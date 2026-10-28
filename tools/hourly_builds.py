"""Select pinned exact-target builds; successful retained artifacts deduplicate runs."""
import argparse
from datetime import datetime, timedelta, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
from urllib.parse import quote, urlencode

WORKFLOW = ".github/workflows/hourly-builds.yml"


def api(path):
    return json.loads(subprocess.check_output(["gh", "api", path], text=True))


def targets(ledger):
    rows = ledger["targets"]
    identities = [r["minecraft"] for r in rows]
    if len(identities) != 24 or len(set(identities)) != 24 or "1.7.10" in identities:
        raise ValueError("Expected the 24 maintained exact targets, excluding 1.7.10")
    result = []
    for row in rows:
        mc = row["minecraft"]
        if not re.fullmatch(r"(?:1|26)\.\d+(?:\.\d+)?", mc):
            raise ValueError("Invalid Minecraft identity")
        branch = "main" if mc == "1.21.1" else "mc/" + mc
        if row["source_ref"] != branch:
            raise ValueError("Unexpected exact-version branch")
        legacy = mc in {"1.6.4", "1.12.2"}
        java = "25" if mc.startswith("26.") else "21" if mc in {"1.21.1", "1.6.4"} else "8" if mc == "1.12.2" else "17"
        result.append(dict(minecraft=mc, branch=branch, java=java, kind="legacy" if legacy else "modern"))
    return result


def retained_pass(repo, name, lookup, run_cache):
    data = lookup(f"repos/{repo}/actions/artifacts?" + urlencode(dict(name=name, per_page=100)))
    for artifact in data["artifacts"]:
        if artifact["name"] != name or artifact.get("expired"):
            continue
        run_id = artifact.get("workflow_run", {}).get("id")
        if not run_id:
            continue
        if run_id not in run_cache:
            run_cache[run_id] = lookup(f"repos/{repo}/actions/runs/{run_id}")
        run = run_cache[run_id]
        # A successful exact-target job remains valid if another matrix target failed.
        if (run.get("path") == WORKFLOW and run.get("status") == "completed"
                and run.get("head_branch") == "main"
                and run.get("event") in {"push", "schedule", "workflow_dispatch"}
                and run.get("head_repository", {}).get("full_name") == repo):
            return True
    return False


def environment_retry_deferred(ledger, row, sha, now=None):
    """Temporarily suppress a reviewed external outage for only its pinned source.

    A failed native artifact never becomes an accepted build.  This bounded pause
    is restricted to a recorded external cause and must automatically expire.
    A changed source, different branch or explicit --force remains buildable.
    """
    entries = ledger.get("temporary_environment_build_blockers", {})
    if not isinstance(entries, dict):
        raise ValueError("Malformed temporary environment blocker ledger")
    entry = entries.get(row["minecraft"])
    if entry is None:
        return False
    if not isinstance(entry, dict):
        raise ValueError("Malformed exact-target environment blocker")
    if entry.get("source_ref") != row["branch"] or entry.get("source_commit") != sha:
        return False
    if entry.get("kind") not in {"UPSTREAM_LIBRARY_DOWNLOAD_TIMEOUT", "RUNNER_UNAVAILABLE", "BILLING_LIMIT"}:
        raise ValueError("Unreviewed environment blocker cannot defer builds")
    if type(entry.get("failed_workflow_run")) is not int or entry["failed_workflow_run"] < 1:
        raise ValueError("Environment blocker must name an actual failed workflow run")

    def utc(value):
        if not isinstance(value, str) or not value.endswith("Z"):
            raise ValueError("Environment blocker needs explicit UTC timestamps")
        result = datetime.fromisoformat(value[:-1] + "+00:00")
        if result.utcoffset() != timedelta(0):
            raise ValueError("Environment blocker timestamp is not UTC")
        return result

    observed = utc(entry.get("observed_at"))
    retry = utc(entry.get("retry_after"))
    if retry <= observed or retry - observed > timedelta(hours=4):
        raise ValueError("Environment retry deferral must be positive and <= four hours")
    instant = datetime.now(timezone.utc) if now is None else now
    if instant.tzinfo is None:
        raise ValueError("Environment retry comparison requires timezone")
    return observed <= instant < retry


def discover(ledger, repo, force=False, lookup=api, now=None):
    selected = []
    cached_runs = {}
    for row in targets(ledger):
        branch = lookup(f"repos/{repo}/branches/" + quote(row["branch"], safe=""))
        sha = branch["commit"]["sha"]
        if not re.fullmatch(r"[0-9a-f]{40}", sha):
            raise ValueError("Invalid pinned commit")
        name = f"hourly-native-{row['minecraft']}-{sha}"
        if force:
            selected.append(dict(row, sha=sha))
            continue
        if retained_pass(repo, name, lookup, cached_runs):
            continue
        if environment_retry_deferred(ledger, row, sha, now=now):
            continue
        selected.append(dict(row, sha=sha))
    return selected


def validate_restart(root, report, mc, sha, runtime, counts, prefix="item-restart"):
    phases = {"write": 0, "verify": 0, "crash-write": -9, "crash-verify": 0}
    if (report.get("minecraft") != mc or report.get("source_commit") != sha
            or report.get("runtime_sha256") != runtime
            or report.get("clean_restart") != "PASS" or report.get("save_barrier_abrupt_restart") != "PASS"):
        raise ValueError("Restart identity/status disagrees")
    processes = report.get("processes", [])
    rows = {p["phase"]: p for p in processes}
    details = report.get("phase_reports", {})
    if len(processes) != 4 or set(rows) != set(phases) or set(details) != set(phases):
        raise ValueError("Missing independent restart processes")
    ids = [p["pid"] for p in processes]
    if any(type(pid) is not int or pid <= 0 for pid in ids) or len(set(ids)) != 4:
        raise ValueError("Restart PIDs are not distinct")
    logs = root / "verification-logs"
    suffix = '-srg.jar' if mc == '1.6.4' else '.jar'
    fixtures = list((root / 'build/libs').glob('*-restart-scenarios' + suffix))
    if len(fixtures) != 1 or hashlib.sha256(fixtures[0].read_bytes()).hexdigest() != report.get('fixture_sha256'):
        raise ValueError('Restart fixture does not match packaged artifact')
    for phase, code in phases.items():
        p, r = rows[phase], details[phase]
        expected = counts["write" if phase.endswith("write") else "verify"]
        if (type(expected) is not int or expected <= 0 or type(p["exit_code"]) is not int or p["exit_code"] != code
                or r.get("minecraft") != mc or r.get("phase") != phase or r.get("passed") is not True
                or not isinstance(r.get("assertions"), list) or not all(isinstance(x, str) for x in r["assertions"]) or len(r["assertions"]) != expected or not r.get("process", "").startswith(str(p["pid"]) + "@")
                or hashlib.sha256((logs / (prefix + "-" + phase + ".log")).read_bytes()).hexdigest() != p["log_sha256"]):
            raise ValueError("Restart phase proof disagrees: " + phase)
        if json.loads((logs / (prefix + "-" + phase + ".json")).read_text()) != r:
            raise ValueError("Raw restart phase report disagrees: " + phase)
        text = (logs / (prefix + "-" + phase + ".log")).read_text(errors='replace')
        if (not re.search('Starting minecraft server version '+re.escape(mc)+r'\b',text,re.I)
                or 'PL4 ITEM RESTART '+phase+': PASS' not in text
                or ('Stopping server' in text) == (phase == 'crash-write')
                or (phase == 'crash-write' and 'PL4 ITEM RESTART KILL READY' not in text)):
            raise ValueError('Missing native restart phase/shutdown markers: '+phase)
    for a, b in [("write", "verify"), ("crash-write", "crash-verify")]:
        tokens = details[a].get("tokens", [])
        if not isinstance(tokens, list) or not all(isinstance(t, str) and re.fullmatch(r"[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}", t) for t in tokens) or len(set(tokens)) != 2 or tokens != details[b].get("tokens"):
            raise ValueError("Restart token identity changed")
    archives = report.get("world_archives", {})
    if set(archives) != {"clean", "barrier"}:
        raise ValueError("Missing saved-world evidence")
    for mode, a in archives.items():
        if a["file"] != prefix + "-" + mode + "-world.zip" or hashlib.sha256((logs / a["file"]).read_bytes()).hexdigest() != a["sha256"]:
            raise ValueError("Restart world archive changed")


def validate_decode_placements(report):
    for phase in ('write','verify','crash-write','crash-verify'):
        rows=report['phase_reports'][phase].get('placements')
        expected=[64,80,200] if phase.endswith('write') else [200]
        if not isinstance(rows,list) or [r.get('x') for r in rows]!=expected:
            raise ValueError('Missing/duplicate/out-of-order native decode placements: '+phase)
        # When a native fixture supplies stage-specific diagnostics, require the
        # entire four-phase native matrix to supply coherent complete observations.
        # Older pinned sources without stages retain their original exact gate.
        staged=any('stage' in r for p in ('write','verify','crash-write','crash-verify')
                   for r in report['phase_reports'][p].get('placements',[]) if isinstance(r,dict))
        for row in rows:
            if (type(row.get('expected_block')) is not int or row['expected_block']<=0 or
                type(row.get('actual_block')) is not int or row['actual_block']!=row['expected_block'] or
                row.get('before_block')!=0 or row.get('set_block_changed') is not True or
                row.get('y')!=200 or row.get('z')!=32 or row.get('metadata')!=0 or
                row.get('tile_class')!='net.foundations.pl4.legacy.LegacyTile' or row.get('tile_invalid') is not False):
                raise ValueError('Invalid native decode placement observation: '+phase)
            if staged and (row.get('stage')!='complete' or
                    type(row.get('chunk_x')) is not int or row['chunk_x']!=row['x']//16 or
                    type(row.get('chunk_z')) is not int or row['chunk_z']!=2 or
                    row.get('block_exists_before') is not True or row.get('block_exists_after') is not True):
                raise ValueError('Incomplete/stale native decode placement stage: '+phase)

def validate_decode_restart(root, report, mc, sha, runtime, counts, suite):
    """Qualify decode persistence separately from provider and historical scenarios."""
    if suite not in {"decode-raw", "decode-missing", "decode-compound", "decode-owner", "decode-dimension", "decode-scope", "decode-rewrite", "decode-live", "decode-live-context", "decode-live-owner"} or report.get("suite") != suite:
        raise ValueError("Decode suite identity disagrees")
    validate_restart(root, report, mc, sha, runtime, counts, "item-" + suite + "-restart")
    owner = "fae1b877-c6b4-4538-a26a-630190b54205" if mc == "1.12.2" else "pl4owner"
    labels = {"decode-compound": "rejected-token-int71", "decode-owner": "rejected-owner-int71",
              "decode-dimension": "rejected-dimension-string0", "decode-scope": "rejected-scope-compound",
              "decode-rewrite": "rewritten-owner-dimension-host", "decode-live": "rewritten-owner-dimension-host", "decode-live-context": "rewritten-owner-dimension-host", "decode-live-owner": "original-context-host-owner-rewrite"}
    details = report["phase_reports"]
    for first, second in [("write", "verify"), ("crash-write", "crash-verify")]:
        rows = details[first].get("decode_outcomes")
        if not isinstance(rows, list) or len(rows) != 2 or rows != details[second].get("decode_outcomes"):
            raise ValueError("Decode evidence changed across native JVMs")
        if any(details[p].get("suite") != suite for p in [first, second]):
            raise ValueError("Raw decode phase suite disagrees")
        for i, row in enumerate(rows):
            expected = dict(host_x=64 if i == 0 else 80, host_removed=i == 1,
                            original_token=details[first]["tokens"][i], owner=owner, dimension=0,
                            context_kind="compound" if suite in labels else "absent" if suite == "decode-missing" else "int" if i == 0 else "string",
                            context_value=labels[suite] if suite in labels else "" if suite == "decode-missing" else "71" if i == 0 else "current-raw-context",
                            sealed_units=8, evidence_marker="first-raw-payload", original_route_known=False)
            if row != expected or any(type(row.get(k)) is not type(v) for k, v in expected.items()):
                raise ValueError("Decode type, evidence, receipt or original identity disagrees")


def validate_provider_outcomes(report, mc):
    """Do not treat a sealed uncertain claim as spendable provider inventory."""
    owner = "fae1b877-c6b4-4538-a26a-630190b54205" if mc == "1.12.2" else "pl4owner"
    details = report["phase_reports"]
    for first, second in [("write", "verify"), ("crash-write", "crash-verify")]:
        rows = details[first].get("provider_outcomes")
        if not isinstance(rows, list) or len(rows) != 2 or rows != details[second].get("provider_outcomes"):
            raise ValueError("Provider private state changed across JVMs")
        identities = set()
        for index, outcome in enumerate(["insert", "extract"]):
            row = rows[index]
            ident = row.get("provider_id", "")
            if (not isinstance(ident, str) or not re.fullmatch(r"[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}", ident)
                    or ident in identities or row.get("owner") != owner or row.get("outcome") != outcome
                    or row.get("token") != details[first]["tokens"][index]):
                raise ValueError("Provider ownership/operation identity disagrees")
            identities.add(ident)
            extraction = outcome == "extract"
            expected = dict(source_units=32, destination_units=0 if extraction else 8,
                            unaccounted_units=8 if extraction else 0, sealed_uncertain_claim=0 if extraction else 8,
                            mutation_calls=1, removed_units=8 if extraction else 0, accepted_units=0 if extraction else 8)
            if any(type(row.get(k)) is not int or row[k] != v for k, v in expected.items()):
                raise ValueError("Provider conservation/mutation proof disagrees")


def validate_historical_providers(report, mc):
    snapshots=[p['report'].get('provider_outcomes') for p in report['processes']]
    if not snapshots or not isinstance(snapshots[0],list) or len(snapshots[0])!=2 or any(s!=snapshots[0] for s in snapshots):
        raise ValueError('Historical provider state changed across native runtimes')
    owner='fae1b877-c6b4-4538-a26a-630190b54205' if mc=='1.12.2' else 'pl4owner'
    ids=set()
    for row,outcome in zip(snapshots[0],['insert','extract']):
        ident=row.get('provider_id','');extraction=outcome=='extract'
        if not isinstance(ident,str) or not re.fullmatch(r'[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}',ident) or ident in ids or row.get('owner')!=owner or row.get('outcome')!=outcome:
            raise ValueError('Historical provider identity/ownership disagrees')
        ids.add(ident)
        expected=dict(source_units=32,destination_units=0 if extraction else 8,unaccounted_units=8 if extraction else 0,sealed_uncertain_claim=0 if extraction else 8,mutation_calls=1,removed_units=8 if extraction else 0,accepted_units=0 if extraction else 8)
        if any(type(row.get(k)) is not int or row[k]!=v for k,v in expected.items()):raise ValueError('Historical provider conservation disagrees')


def validate_preupgrade(root, report, mc, sha, runtime, provider=False):
    logs = root / "verification-logs"
    suffix = '-srg.jar' if mc == '1.6.4' else '.jar'
    fixtures = list((root / 'build/libs').glob('*-migration-scenarios' + suffix))
    if len(fixtures) != 1 or hashlib.sha256(fixtures[0].read_bytes()).hexdigest() != report.get('fixture_sha256'):
        raise ValueError('Historical migration fixture does not match packaged artifact')
    if (report.get("minecraft") != mc or report.get("source_commit") != sha or report.get("runtime_sha256") != runtime
            or report.get("preupgrade_unlocated_fault_import") != "PASS" or report.get("candidate_restart") != "PASS"):
        raise ValueError("Pre-upgrade candidate identity/status disagrees")
    baseline = report["baseline"]
    old = json.loads((logs / "preupgrade-baseline-native.json").read_text())
    if (old.get("minecraft") != mc or old.get("source_commit") != baseline["source_commit"] or old.get("native_gate") != "PASS"
            or baseline["runtime_sha256"] not in old.get("artifact_sha256", {}).values()
            or hashlib.sha256((logs / "preupgrade-baseline-runtime.jar").read_bytes()).hexdigest() != baseline["runtime_sha256"]):
        raise ValueError("Pre-upgrade historical runtime identity disagrees")
    rows = report.get("processes", [])
    phases = [("fault-write", 16 if provider else 4, baseline["runtime_sha256"]), ("fault-upgrade", 22 if provider else 10, runtime), ("fault-verify", 22 if provider else 10, runtime)]
    if len(rows) != 3 or any(type(p.get("pid")) is not int or p["pid"] <= 0 for p in rows) or len({p["pid"] for p in rows}) != 3:
        raise ValueError("Pre-upgrade processes are not independent")
    for row, (phase, count, selected) in zip(rows, phases):
        proof = row.get("report", {})
        log = logs / ("preupgrade-" + phase + ".log")
        text = log.read_text(errors="replace")
        if (row.get("phase") != phase or type(row.get("exit_code")) is not int or row["exit_code"] != 0 or row.get("runtime_sha256") != selected
                or proof.get("minecraft") != mc or proof.get("phase") != phase or proof.get("passed") is not True or type(proof.get("checks")) is not int or proof["checks"] != count
                or not proof.get("process", "").startswith(str(row["pid"]) + "@")
                or hashlib.sha256(log.read_bytes()).hexdigest() != row["log_sha256"]
                or not re.search("Starting minecraft server version " + re.escape(mc) + r"\b", text, re.I)
                or "PL4 LEGACY MIGRATION " + phase + ": PASS" not in text or "Stopping server" not in text
                or any(marker in text for marker in ["missing a mapping", "It will not persist", "Exception in server tick loop", "UnsupportedClassVersionError"])
                or json.loads((logs / ("preupgrade-" + phase + ".json")).read_text()) != proof):
            raise ValueError("Pre-upgrade phase proof disagrees: " + phase)
    tokens = rows[1]["report"].get("tokens", [])
    if (len(tokens) != 2 or len(set(tokens)) != 2 or any(not isinstance(t, str) or not re.fullmatch(r"[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}", t) for t in tokens)
            or rows[2]["report"].get("tokens") != tokens or rows[0]["report"].get("tokens") != []):
        raise ValueError("Historical unknown-route token proof disagrees")
    archive = report["world_archive"]
    if provider:
        validate_historical_providers(report, mc)
    if archive["file"] != "preupgrade-item-fault-world.zip" or hashlib.sha256((logs / archive["file"]).read_bytes()).hexdigest() != archive["sha256"]:
        raise ValueError("Pre-upgrade saved world changed")


def validate_legacy_counts(root, native, expected):
    observed = json.loads((root / "run-legacy/legacy-scenarios.json").read_text())
    if (observed.get("minecraft") != native["minecraft"]
            or any(type(observed.get(k)) is not int for k in ["total", "passed", "failed"])
            or observed["total"] != expected or observed["passed"] != expected or observed["failed"] != 0):
        raise ValueError("Actual native scenario counters disagree with summary")
    if "results" in observed:
        rows = observed["results"]
        if not isinstance(rows, list) or len(rows) != expected or any(r.get("passed") is not True for r in rows):
            raise ValueError("Native per-case outcomes disagree with counters")
    text = (root / "verification-logs/native.log").read_text(errors="replace")
    if "PL4 LEGACY SCENARIOS: " + str(expected) + "/" + str(expected) not in text:
        raise ValueError("Missing actual native scenario completion marker")


ENDPOINT_DISCOVERY_CASES = {
    # 1.12.2 currently owns this native fixture. Do not borrow labels from a
    # neighboring legacy port or accept aggregate count as equivalent proof.
    '1.12.2': ['handheld blocked root refuses provider discovery',
               'handheld discovery observes later endpoint before provider',
               'handheld discovery healthy provider remains scoped'],
}
# A target-specific 1.12.2 native provider-swap case must execute once.
# Aggregate 392/392 alone cannot prove that the adversarial callback was run.
CAPABILITY_DISCOVERY_CASES = ['wireless capability discovery replacement cannot debit player']

HANDHELD_ADMISSION_CASES = ['handheld readiness owner readmission', 'handheld readiness context readmission', 'handheld readiness observed disconnected alias fence', 'handheld readiness valid scoped context']

LIVE_ADMISSION_CASES = ["live admission owner rewrite rechecks cleared latch","live admission tick catches context rewrite","live admission disconnected alias remains fenced after owner edit tick","live admission valid context remains scoped and idempotent","live admission raw context rewrite preserves rejected payload"]


def validate_live_evidence_cases(root, minecraft, required=None):
    """Require the concrete live-mutation regressions, not only aggregate counters."""
    required = required if required is not None else ["live retained evidence blocks after external fault clearing","live retained evidence rejects face edit with empty escrow","live retained evidence prevents transfer after owner rewrite","live retained evidence reload schedules admission without fault text","live retained evidence reload preserves wide escrow count","live retained evidence rewritten context freezes on reload admission","live retained evidence removal seals after fault clearing","live retained evidence removal rejects rewritten owner context"]
    observed = json.loads((root / "run-legacy/legacy-scenarios.json").read_text())
    if minecraft == "1.12.2":
        rows = observed.get("results", [])
        for name in required:
            matches = [row for row in rows if row.get("name") == name]
            if len(matches) != 1 or matches[0].get("passed") is not True:
                raise ValueError("Missing unique passing live evidence case: " + name)
    elif minecraft == "1.6.4":
        raw_lines = (root / "verification-logs/native.log").read_text(errors="replace").splitlines()
        lines = [re.sub(r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} \[INFO\] \[STDOUT\] ", "", line) for line in raw_lines]
        for name in required:
            if lines.count("PASS " + name) != 1:
                raise ValueError("Missing unique passing live evidence marker: " + name)
    else:
        raise ValueError("Live evidence cases require an exact legacy target")


def validate_live_decode_assertions(report):
    prefixes = ["live cleared fault retains evidence quarantine ",
                "live cleared fault rejects empty escrow face edit ",
                "live cleared fault NBT reload preserves evidence and count "]
    for phase in ["write", "verify", "crash-write", "crash-verify"]:
        names = report["phase_reports"][phase]["assertions"]
        stages = ["retained 64"] + (["seed 64", "seed 80"] if phase.endswith("write") else [])
        for prefix in prefixes:
            for stage in stages:
                if names.count(prefix + stage) != 1:
                    raise ValueError("Missing unique native live decode assertion: " + phase + " " + prefix + stage)


def validate_live_mutation_assertions(report):
    """A new native suite; counts alone do not prove the live path executed."""
    always = ["live mutation persisted owner and physical source",
              "live mutation disconnected alias stays fenced after restart",
              "live mutation alias leaves retained sealed claim unchanged"]
    seeded = ["live mutation starts after scoped admission latch cleared ",
              "live mutation transfer preserves evidence and escrow ",
              "live mutation readmission freezes changed identity "]
    for phase in ["write", "verify", "crash-write", "crash-verify"]:
        names = report["phase_reports"][phase]["assertions"]
        for name in always + ([p + str(x) for p in seeded for x in [64, 80]] if phase.endswith("write") else []):
            if names.count(name) != 1:
                raise ValueError("Missing unique native live mutation assertion: " + phase + " " + name)


def validate_26_native_profiles(root, minecraft, expected, native):
    """Bind every claimed 26.x loader profile to an actual JVM log and its JAR bytes."""
    profiles = {"26.3.0.23-beta", "26.3.0.39-beta"} if minecraft == "26.3" else {"default"}
    observed = native.get("native_coverage")
    if not isinstance(observed, dict) or set(observed) != profiles:
        raise ValueError("Missing coherent 26.x per-profile native count attribution")
    for profile in sorted(profiles):
        row = observed[profile]
        if not isinstance(row, dict):
            raise ValueError("Malformed 26.x native coverage row")
        logfile = root / "verification-logs" / (("native-" + profile + ".log") if minecraft == "26.3" else "native.log")
        transcript = logfile.read_text(errors="replace")
        digest = hashlib.sha256(transcript.encode()).hexdigest()
        if row.get("native_log_sha256") != digest:
            raise ValueError("26.x native log SHA-256 differs from coverage report")
        matches = re.findall(r"Running test environment '([^']+)' batch (\d+) \((\d+) tests\)", transcript)
        batches = [dict(environment=env, batch=int(batch), tests=int(count)) for env, batch, count in matches]
        if len({(b["environment"], b["batch"]) for b in batches}) != len(batches):
            raise ValueError("Duplicate 26.x native environment batch")
        pl4 = sum(b["tests"] for b in batches if b["environment"].startswith("foundations_pl4:"))
        total = sum(b["tests"] for b in batches)
        passed = re.findall(r"All (\d+) required tests passed", transcript)
        if (pl4 != expected or not batches or len(passed) != 1 or int(passed[0]) != total
                or row.get("environment_batches") != batches
                or type(row.get("pl4_fixtures")) is not int or row["pl4_fixtures"] != pl4
                or type(row.get("other_fixtures")) is not int or row["other_fixtures"] != total-pl4
                or type(row.get("total_required_passed")) is not int or row["total_required_passed"] != total):
            raise ValueError("26.x native count attribution disagrees with actual JVM log")
        if minecraft == "26.3":
            staged = root / "verification-logs" / ("artifacts-" + profile)
            packaged = {p.name: p for p in staged.glob("*.jar")}
            declared = row.get("packaged_artifact_sha256")
            if not isinstance(declared, dict) or not packaged or set(declared) != set(packaged):
                raise ValueError("26.3 loader-profile JAR manifest is incomplete or surplus")
            for name, path in packaged.items():
                sha = declared[name]
                if (not isinstance(sha, str) or not re.fullmatch(r"[0-9a-f]{64}", sha)
                        or hashlib.sha256(path.read_bytes()).hexdigest() != sha):
                    raise ValueError("26.3 loader-profile JAR changed after native qualification")


def record(root, expected_mc, expected_sha):
    status = json.loads((root / "BUILD_STATUS.json").read_text())
    actual = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
    if status["minecraft"] != expected_mc or actual != expected_sha:
        raise ValueError("Checked source does not match pinned exact target")
    logs = root / "verification-logs"
    expected = status.get("expected_native_scenarios", status.get("expected_native_tests"))
    if not isinstance(expected, int) or isinstance(expected, bool) or expected <= 0:
        raise ValueError("Missing expected native fixture count")
    if expected_mc in {"1.6.4", "1.12.2"}:
        native = json.loads((logs / "clean-native-summary.json").read_text())
        smoke = json.loads((logs / "production-smoke.json").read_text())
        migration = json.loads((logs / "legacy-migration-summary.json").read_text())
        if not (native["minecraft"] == smoke["minecraft"] == migration["minecraft"] == expected_mc
                and native["verification_commit"] == smoke["source_commit"] == migration["source_commit"] == actual
                and native["total"] == native["passed"] == expected and native["failed"] == 0
                and native["runtime_sha256"] == smoke["runtime_sha256"] == migration["candidate_runtime_sha256"]
                and smoke["production_only_startup"] == smoke["clean_stop"] == "PASS"
                and migration["generated_preview3_world_upgrade"] == "PASS"):
            raise ValueError("Legacy native/production/migration evidence disagrees")
        validate_legacy_counts(root, native, expected)
        if expected_mc == '1.12.2' and expected >= 392:
            validate_live_evidence_cases(root, expected_mc, CAPABILITY_DISCOVERY_CASES)
        if status.get("item_endpoint_discovery_fixture") is True:
            if expected_mc not in ENDPOINT_DISCOVERY_CASES:
                raise ValueError("Unregistered exact-target native endpoint discovery proof: " + expected_mc)
            validate_live_evidence_cases(root, expected_mc, ENDPOINT_DISCOVERY_CASES[expected_mc])
        if status.get("item_handheld_admission_fixture") is True:
            validate_live_evidence_cases(root, expected_mc, HANDHELD_ADMISSION_CASES)
        if status.get("item_live_admission_fixture") is True:
            validate_live_evidence_cases(root, expected_mc, LIVE_ADMISSION_CASES)
        if status.get("item_live_evidence_fixture") is True:
            validate_live_evidence_cases(root, expected_mc)
        gate_files = ["clean-native-summary.json", "production-smoke.json", "legacy-migration-summary.json"]
        if status.get("item_opaque_journal_export"):
            from verify_legacy_journal_export import verify_native_log
            export = verify_native_log((logs / "native.log").read_text(errors="replace"))
            name = "legacy-opaque-export-inspection.json"
            (logs / name).write_text(json.dumps(export, indent=2) + "\n")
            gate_files.append(name)
        if status.get("item_restart_fixture") is True:
            restart = json.loads((logs / "legacy-item-restart-summary.json").read_text())
            validate_restart(root, restart, expected_mc, actual, native["runtime_sha256"], {"write": status["item_restart_write_assertions"], "verify": status["item_restart_verify_assertions"]})
            if status.get("item_provider_fixture") is True:
                validate_provider_outcomes(restart, expected_mc)
            gate_files.append("legacy-item-restart-summary.json")
        if status.get("item_decode_restart_fixture") is True:
            counts = {"write": status["item_decode_restart_write_assertions"], "verify": status["item_decode_restart_verify_assertions"]}
            if status.get("item_live_decode_restart_fixture") is True:
                counts = {"write": counts["write"] + 9, "verify": counts["verify"] + 3}
            inspections = {}
            pids = [p['pid'] for p in restart['processes']] if status.get('item_restart_fixture') is True else []
            for suite in ["decode-raw", "decode-missing"] + (["decode-compound"] if status.get("item_compound_restart_fixture") is True else []) + (["decode-owner", "decode-dimension", "decode-scope", "decode-rewrite"] if status.get("item_field_restart_fixture") is True else []) + (["decode-live"] if status.get("item_live_mutation_restart_fixture") is True else []) + (["decode-live-context"] if status.get("item_context_mutation_restart_fixture") is True else []) + (["decode-live-owner"] if status.get("item_owner_mutation_restart_fixture") is True else []):
                name = "legacy-item-" + suite + "-restart-summary.json"
                decode = json.loads((logs / name).read_text())
                validate_decode_restart(root, decode, expected_mc, actual, native["runtime_sha256"],
                                        {"write": counts["write"]+9, "verify": counts["verify"]+3} if suite in {"decode-live", "decode-live-context", "decode-live-owner"} else {"write": counts["write"]+4, "verify": counts["verify"]+4} if status.get("item_receipt_auth_restart_fixture") is True and suite in {"decode-owner", "decode-dimension", "decode-scope", "decode-rewrite"} else counts, suite)
                if status.get('item_live_decode_restart_fixture') is True:
                    validate_live_decode_assertions(decode)
                if status.get('decode_placement_observation_fixture') is True:
                    validate_decode_placements(decode)
                if suite in {"decode-live", "decode-live-context", "decode-live-owner"}:
                    validate_live_mutation_assertions(decode)
                pids.extend(p['pid'] for p in decode['processes'])
                from inspect_legacy_decode_world import inspect
                inspections[suite] = {mode: inspect(logs / decode['world_archives'][mode]['file'], decode['phase_reports'][phase], expected_mc, suite)
                                      for mode, phase in [('clean', 'verify'), ('barrier', 'crash-verify')]}
                gate_files.append(name)
            if len(set(pids)) != len(pids):
                raise ValueError('Decode/provider suites reused a native process identity')
            name = 'legacy-decode-world-inspection.json'
            (logs / name).write_text(json.dumps(inspections, indent=2) + '\n')
            gate_files.append(name)
        if status.get("preupgrade_item_fault_fixture") is True:
            fault_import = json.loads((logs / "preupgrade-item-fault-summary.json").read_text())
            validate_preupgrade(root, fault_import, expected_mc, actual, native["runtime_sha256"], status.get('historical_provider_fixture') is True)
            gate_files.append("preupgrade-item-fault-summary.json")
    else:
        native = json.loads((logs / "rc-development-gate.json").read_text())
        if not (native["minecraft"] == expected_mc and native["version"] == status["version"]
                and native["source_parent"] == actual and native["native_gate"] == "PASS"
                and native["expected_fixtures"] == expected):
            raise ValueError("Modern exact-source native evidence disagrees")
        if expected_mc.startswith("26."):
            validate_26_native_profiles(root, expected_mc, expected, native)
        # The native runner hashes every packaged JAR. An empty, partial or
        # surplus manifest is not evidence that the exact runtime was tested.
        # Compare the complete build/libs/*.jar set, not a caller-selected subset.
        declared = native.get("artifact_sha256")
        packaged = {p.relative_to(root).as_posix(): p
                    for p in sorted((root / "build/libs").glob("*.jar"))}
        if not isinstance(declared, dict) or not packaged or set(declared) != set(packaged):
            raise ValueError("Modern native artifact manifest must bind every packaged JAR")
        for name, path in packaged.items():
            digest = declared[name]
            if (not isinstance(digest, str) or not re.fullmatch(r"[0-9a-f]{64}", digest)
                    or hashlib.sha256(path.read_bytes()).hexdigest() != digest):
                raise ValueError("Native-verified JAR changed or digest malformed")
        if expected_mc == "26.3":
            # The last profile is also the final build/libs payload; reject a copy
            # from a previous profile or a report from a different binary.
            final = native["native_coverage"]["26.3.0.39-beta"]["packaged_artifact_sha256"]
            if final != {p.name: declared[name] for name, p in packaged.items()}:
                raise ValueError("26.3 final loader-profile JAR differs from packaged runtime")
        gate_files = ["rc-development-gate.json"]
    hashes = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((root / "build/libs").glob("*.jar"))}
    if not hashes:
        raise ValueError("No packaged artifacts")
    if expected_mc in {"1.6.4", "1.12.2"} and native["runtime_sha256"] not in hashes.values():
        raise ValueError("Native-qualified legacy runtime is missing")
    report = dict(minecraft=expected_mc, version=status["version"], source_commit=actual,
                  native_gate="PASS", expected_fixtures=expected, gate_reports=gate_files,
                  artifact_sha256=hashes, workflow_run=os.environ.get("GITHUB_RUN_ID"),
                  publication_performed=False, rc_publication_cleared=False,
                  scope="Exact-source native revalidation only; client/provider/multiplayer/performance acceptance remains separate")
    if native.get("native_coverage"):
        report["native_coverage"] = native["native_coverage"]
    (logs / "hourly-native.json").write_text(json.dumps(report, indent=2) + "\n")
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="action", required=True)
    select = sub.add_parser("discover")
    select.add_argument("--force", action="store_true")
    proof = sub.add_parser("record")
    proof.add_argument("root", type=Path)
    proof.add_argument("minecraft")
    proof.add_argument("sha")
    args = parser.parse_args()
    if args.action == "discover":
        rows = discover(json.loads(Path("docs/RC_DEVELOPMENT_STATUS.json").read_text()), os.environ["GITHUB_REPOSITORY"], args.force)
        if os.environ.get("GITHUB_OUTPUT"):
            with open(os.environ["GITHUB_OUTPUT"], "a") as out:
                out.write("matrix=" + json.dumps({"include": rows}, separators=(",", ":")) + "\ncount=" + str(len(rows)) + "\n")
        print(json.dumps(rows, indent=2))
    else:
        print(json.dumps(record(args.root.resolve(), args.minecraft, args.sha), indent=2))


if __name__ == "__main__":
    main()



