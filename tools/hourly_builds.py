"""Select pinned exact-target builds; successful retained artifacts deduplicate runs."""
import argparse
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


def discover(ledger, repo, force=False, lookup=api):
    selected = []
    cached_runs = {}
    for row in targets(ledger):
        branch = lookup(f"repos/{repo}/branches/" + quote(row["branch"], safe=""))
        sha = branch["commit"]["sha"]
        if not re.fullmatch(r"[0-9a-f]{40}", sha):
            raise ValueError("Invalid pinned commit")
        name = f"hourly-native-{row['minecraft']}-{sha}"
        if force or not retained_pass(repo, name, lookup, cached_runs):
            selected.append(dict(row, sha=sha))
    return selected


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
        gate_files = ["clean-native-summary.json", "production-smoke.json", "legacy-migration-summary.json"]
    else:
        native = json.loads((logs / "rc-development-gate.json").read_text())
        if not (native["minecraft"] == expected_mc and native["version"] == status["version"]
                and native["source_parent"] == actual and native["native_gate"] == "PASS"
                and native["expected_fixtures"] == expected):
            raise ValueError("Modern exact-source native evidence disagrees")
        if expected_mc.startswith("26."):
            profiles = {"26.3.0.23-beta", "26.3.0.39-beta"} if expected_mc == "26.3" else {"default"}
            observed = native.get("native_coverage", {})
            if set(observed) != profiles or any(row["pl4_fixtures"] != expected
                    or row["total_required_passed"] != row["pl4_fixtures"] + row["other_fixtures"]
                    for row in observed.values()):
                raise ValueError("Missing coherent 26.x per-profile native count attribution")
        for name, digest in native["artifact_sha256"].items():
            path = (root / name).resolve()
            if not path.is_relative_to((root / "build/libs").resolve()) or hashlib.sha256(path.read_bytes()).hexdigest() != digest:
                raise ValueError("Native-verified JAR changed")
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
