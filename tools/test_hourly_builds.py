"""Scheduled build identity/deduplication and RC publication failure cases; no network."""
import base64
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import hourly_builds as builds
from publication_policy import require_publication, release_type
import curseforge_sync
import upload_curseforge

ROOT = Path(__file__).resolve().parents[1]
SHA = "a" * 40
REPO = "Shnewbs/Foundations-PL4"


class HourlyBuildTests(unittest.TestCase):
    def setUp(self):
        self.ledger = json.loads((ROOT / "docs/RC_DEVELOPMENT_STATUS.json").read_text())

    def test_each_exact_track_and_native_java_profile(self):
        rows = {r['minecraft']: r for r in builds.targets(self.ledger)}
        self.assertEqual(len(rows), 24)
        self.assertEqual(rows['1.21.1']['branch'], 'main')
        self.assertEqual(rows['1.6.4']['java'], '21')
        self.assertEqual(rows['1.12.2']['java'], '8')
        self.assertEqual(rows['26.3']['java'], '25')
        self.assertNotEqual(rows['1.16.4']['branch'], rows['1.16.5']['branch'])
        self.assertNotIn('1.7.10', rows)

    def test_duplicate_or_neighboring_version_branch_fails(self):
        for mutate in (lambda r: r['targets'].append(r['targets'][0]),
                       lambda r: r['targets'][0].update(source_ref='mc/1.15.2')):
            data = json.loads(json.dumps(self.ledger));mutate(data)
            with self.assertRaises(ValueError):
                builds.targets(data)

    def test_retained_completed_target_artifact_skips_unchanged_source(self):
        def lookup(path):
            if '/branches/' in path:return {'commit': {'sha': SHA}}
            if '/artifacts?' in path:
                from urllib.parse import parse_qs, urlsplit
                name = parse_qs(urlsplit(path).query)['name'][0]
                return {'artifacts': [{'name': name, 'expired': False, 'workflow_run': {'id': 1}}]}
            return dict(path=builds.WORKFLOW, status='completed', head_branch='main', event='schedule',
                        head_repository={'full_name': REPO}, conclusion='failure')
        self.assertEqual(builds.discover(self.ledger, REPO, lookup=lookup), [])

    def test_missing_or_changed_source_builds_all_pinned_targets(self):
        def lookup(path):
            if '/branches/' in path:return {'commit': {'sha': SHA}}
            return {'artifacts': [{'name': 'hourly-native-1.21.1-' + 'b'*40, 'expired': False}]}
        rows = builds.discover(self.ledger, REPO, lookup=lookup)
        self.assertEqual(len(rows), 24)
        self.assertTrue(all(r['sha'] == SHA for r in rows))

    def test_expired_incomplete_foreign_and_wrong_workflow_cannot_skip(self):
        valid = dict(path=builds.WORKFLOW, status='completed', head_branch='main', event='push', head_repository={'full_name': REPO})
        for change in ({'path': '.github/workflows/verify.yml'}, {'status': 'in_progress'},
                       {'head_branch': 'other'}, {'event': 'pull_request'}, {'head_repository': {'full_name': 'other/repo'}}):
            run = dict(valid, **change)
            def lookup(path):
                return {'artifacts': [{'name':'wanted','expired':False,'workflow_run':{'id':1}}]} if '/artifacts?' in path else run
            self.assertFalse(builds.retained_pass(REPO, 'wanted', lookup, {}))
        self.assertFalse(builds.retained_pass(REPO, 'wanted', lambda _: {'artifacts':[{'name':'wanted','expired':True}]}, {}))

    def test_force_rebuild_does_not_reuse_artifact(self):
        def lookup(path):
            self.assertIn('/branches/', path)
            return {'commit': {'sha': SHA}}
        self.assertEqual(len(builds.discover(self.ledger, REPO, force=True, lookup=lookup)), 24)

    def test_reviewed_network_failure_defers_only_matching_source_for_three_hours(self):
        report = dict(source_ref="mc/1.12.2", source_commit=SHA,
                      kind="UPSTREAM_LIBRARY_DOWNLOAD_TIMEOUT",
                      failed_workflow_run=40705534234,
                      observed_at="2026-10-28T14:25:03Z",
                      retry_after="2026-10-28T17:25:03Z")
        self.ledger["temporary_environment_build_blockers"] = {"1.12.2": report}
        row = dict(minecraft="1.12.2", branch="mc/1.12.2")
        at = datetime(2026, 10, 28, 15, 0, tzinfo=timezone.utc)
        self.assertTrue(builds.environment_retry_deferred(self.ledger, row, SHA, at))
        self.assertFalse(builds.environment_retry_deferred(self.ledger, row, "b"*40, at))
        self.assertFalse(builds.environment_retry_deferred(self.ledger, dict(row, branch="main"), SHA, at))
        self.assertFalse(builds.environment_retry_deferred(self.ledger, row, SHA, datetime(2026, 10, 28, 17, 26, tzinfo=timezone.utc)))
        self.assertFalse(builds.environment_retry_deferred(self.ledger, row, SHA, datetime(2026, 10, 28, 14, 24, tzinfo=timezone.utc)))

        def lookup(path):
            if "/branches/" in path: return {"commit": {"sha": SHA}}
            return {"artifacts": []}
        rows = builds.discover(self.ledger, REPO, lookup=lookup, now=at)
        self.assertEqual(len(rows), 23)
        self.assertNotIn("1.12.2", [row["minecraft"] for row in rows])
        self.assertEqual(len(builds.discover(self.ledger, REPO, force=True, lookup=lookup, now=at)), 24)
        self.assertEqual(len(builds.discover(self.ledger, REPO, lookup=lookup, now=datetime(2026, 10, 28, 17, 26, tzinfo=timezone.utc))), 24)

    def test_reviewed_forge_installer_connection_reset_deferred_only_for_pinned_164(self):
        report = dict(source_ref="mc/1.6.4", source_commit=SHA,
                      kind="UPSTREAM_CONNECTION_RESET", failed_workflow_run=40752183303,
                      observed_at="2026-10-28T19:15:14Z",
                      retry_after="2026-10-28T22:15:14Z")
        self.ledger.setdefault("temporary_environment_build_blockers", {})["1.6.4"] = report
        row = dict(minecraft="1.6.4", branch="mc/1.6.4")
        at = datetime(2026, 10, 28, 20, 0, tzinfo=timezone.utc)
        self.assertTrue(builds.environment_retry_deferred(self.ledger, row, SHA, at))
        self.assertFalse(builds.environment_retry_deferred(self.ledger, row, "b"*40, at))
        self.assertFalse(builds.environment_retry_deferred(self.ledger, dict(row, branch="mc/1.12.2"), SHA, at))
        self.assertFalse(builds.environment_retry_deferred(self.ledger, row, SHA, datetime(2026, 10, 28, 22, 16, tzinfo=timezone.utc)))

        def lookup(path):
            if "/branches/" in path: return {"commit": {"sha": SHA}}
            return {"artifacts": []}
        pending = builds.discover(self.ledger, REPO, lookup=lookup, now=at)
        self.assertEqual(len(pending), 23)
        self.assertNotIn("1.6.4", [item["minecraft"] for item in pending])
        self.assertEqual(len(builds.discover(self.ledger, REPO, force=True, lookup=lookup, now=at)), 24)
        self.assertEqual(len(builds.discover(self.ledger, REPO, lookup=lookup, now=datetime(2026, 10, 28, 22, 16, tzinfo=timezone.utc))), 24)

    def test_environment_deferral_rejects_unreviewed_indefinite_or_bad_receipts(self):
        row = dict(minecraft="1.12.2", branch="mc/1.12.2")
        base = dict(source_ref=row["branch"], source_commit=SHA, kind="UPSTREAM_LIBRARY_DOWNLOAD_TIMEOUT",
                    failed_workflow_run=40705534234, observed_at="2026-10-28T14:25:03Z", retry_after="2026-10-28T17:25:03Z")
        for changes in (dict(kind="GAMEPLAY_FAILURE"), dict(failed_workflow_run=True),
                        dict(retry_after="2026-10-29T17:25:03Z"),
                        dict(retry_after="2026-10-28T14:25:03Z"),
                        dict(observed_at="2026-10-28T14:25:03")):
            modified = dict(base, **changes)
            ledger = {"temporary_environment_build_blockers": {"1.12.2": modified}}
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                builds.environment_retry_deferred(ledger, row, SHA)

    def fixture(self, root, legacy=False):
        logs=root/'verification-logs';logs.mkdir();libs=root/'build/libs';libs.mkdir(parents=True)
        jar=libs/'runtime.jar';jar.write_bytes(b'qualified native runtime');digest=hashlib.sha256(jar.read_bytes()).hexdigest()
        mc='1.6.4' if legacy else '1.21.1'
        def save(name, value):(logs/name).write_text(json.dumps(value))
        (root/'BUILD_STATUS.json').write_text(json.dumps(dict(minecraft=mc,version='0.2.0-rc.1-dev',expected_native_scenarios=156)))
        if legacy:
            (root/'run-legacy').mkdir()
            (root/'run-legacy/legacy-scenarios.json').write_text(json.dumps(dict(minecraft=mc,total=156,passed=156,failed=0)))
            (logs/'native.log').write_text('PL4 LEGACY SCENARIOS: 156/156\n')
            save('clean-native-summary.json',dict(minecraft=mc,verification_commit=SHA,total=156,passed=156,failed=0,runtime_sha256=digest))
            save('production-smoke.json',dict(minecraft=mc,source_commit=SHA,runtime_sha256=digest,production_only_startup='PASS',clean_stop='PASS'))
            save('legacy-migration-summary.json',dict(minecraft=mc,source_commit=SHA,candidate_runtime_sha256=digest,generated_preview3_world_upgrade='PASS'))
        else:
            save('rc-development-gate.json',dict(minecraft=mc,version='0.2.0-rc.1-dev',source_parent=SHA,native_gate='PASS',expected_fixtures=156,artifact_sha256={'build/libs/runtime.jar':digest}))
        return mc,jar

    def test_endpoint_discovery_proof_uses_112_native_case_names(self):
        expected = ['handheld blocked root refuses provider discovery',
                    'handheld discovery observes later endpoint before provider',
                    'handheld discovery healthy provider remains scoped']
        self.assertEqual(builds.ENDPOINT_DISCOVERY_CASES['1.12.2'], expected)
        self.assertNotIn('1.6.4', builds.ENDPOINT_DISCOVERY_CASES)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'run-legacy').mkdir()
            report = root / 'run-legacy/legacy-scenarios.json'
            accepted = [{'name': name, 'passed': True} for name in expected]
            report.write_text(json.dumps({'minecraft': '1.12.2', 'results': accepted}))
            builds.validate_live_evidence_cases(root, '1.12.2', expected)
            # Old invented label, false, and duplicate proof must each fail.
            variants = [
                [{'name': 'handheld discovery observes root before provider', 'passed': True}] + accepted[1:],
                [dict(accepted[0], passed=False)] + accepted[1:],
                accepted + [accepted[0]],
            ]
            for rows in variants:
                report.write_text(json.dumps({'minecraft': '1.12.2', 'results': rows}))
                with self.assertRaisesRegex(ValueError, 'Missing unique passing'):
                    builds.validate_live_evidence_cases(root, '1.12.2', expected)

    def test_native_proof_stays_unpublished_for_both_runtime_kinds(self):
        for legacy in (False,True):
            with tempfile.TemporaryDirectory() as directory:
                root=Path(directory);mc,_=self.fixture(root,legacy)
                with patch.object(builds.subprocess,'check_output',return_value=SHA+'\n'):
                    report=builds.record(root,mc,SHA)
                self.assertEqual(report['native_gate'],'PASS')
                self.assertFalse(report['publication_performed'])
                self.assertFalse(report['rc_publication_cleared'])

    def test_26_native_receipt_requires_real_log_and_count_attribution(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); self.fixture(root)
            status = root / "BUILD_STATUS.json"
            data = json.loads(status.read_text()); data["minecraft"] = "26.1.2"
            status.write_text(json.dumps(data))
            gate = root / "verification-logs/rc-development-gate.json"
            data = json.loads(gate.read_text()); data["minecraft"] = "26.1.2"
            gate.write_text(json.dumps(data))
            with patch.object(builds.subprocess, "check_output", return_value=SHA):
                with self.assertRaisesRegex(ValueError, "count attribution"):
                    builds.record(root, "26.1.2", SHA)
            transcript = ("Running test environment 'foundations_pl4:fixtures' batch 0 (156 tests)\n"
                          "Running test environment 'minecraft:other' batch 1 (2 tests)\n"
                          "All 158 required tests passed\n")
            (root / "verification-logs/native.log").write_text(transcript)
            data["native_coverage"] = {"default": dict(
                pl4_fixtures=156, other_fixtures=2, total_required_passed=158,
                environment_batches=[
                    dict(environment="foundations_pl4:fixtures", batch=0, tests=156),
                    dict(environment="minecraft:other", batch=1, tests=2)],
                native_log_sha256=hashlib.sha256(transcript.encode()).hexdigest())}
            gate.write_text(json.dumps(data))
            with patch.object(builds.subprocess, "check_output", return_value=SHA):
                report = builds.record(root, "26.1.2", SHA)
            self.assertEqual(report["native_coverage"], data["native_coverage"])
            for bad in ("All 157 required tests passed\n", "All 158 required tests passed\nAll 158 required tests passed\n"):
                (root / "verification-logs/native.log").write_text(transcript.replace("All 158 required tests passed\n", bad))
                with patch.object(builds.subprocess, "check_output", return_value=SHA):
                    with self.assertRaises(ValueError):
                        builds.record(root, "26.1.2", SHA)

    def test_263_two_native_loader_profiles_require_each_log_and_jar(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); _, jar = self.fixture(root)
            status = root / "BUILD_STATUS.json"
            data = json.loads(status.read_text()); data["minecraft"] = "26.3"
            status.write_text(json.dumps(data))
            gate = root / "verification-logs/rc-development-gate.json"
            data = json.loads(gate.read_text()); data["minecraft"] = "26.3"
            coverage = {}
            versions = ("26.3.0.23-beta", "26.3.0.39-beta")
            for index, profile in enumerate(versions):
                transcript = ("Running test environment 'foundations_pl4:fixtures' batch 0 (156 tests)\n"
                              "Running test environment 'minecraft:other' batch 1 (2 tests)\n"
                              "All 158 required tests passed\n")
                (root / "verification-logs" / ("native-" + profile + ".log")).write_text(transcript)
                directory = root / "verification-logs" / ("artifacts-" + profile)
                directory.mkdir()
                binary = b"profile native runtime " + bytes([index])
                (directory / "runtime.jar").write_bytes(binary)
                coverage[profile] = dict(pl4_fixtures=156, other_fixtures=2, total_required_passed=158,
                    environment_batches=[
                        dict(environment="foundations_pl4:fixtures", batch=0, tests=156),
                        dict(environment="minecraft:other", batch=1, tests=2)],
                    native_log_sha256=hashlib.sha256(transcript.encode()).hexdigest(),
                    packaged_artifact_sha256={"runtime.jar": hashlib.sha256(binary).hexdigest()})
            jar.write_bytes(b"profile native runtime " + bytes([1]))
            data["artifact_sha256"] = {"build/libs/runtime.jar": hashlib.sha256(jar.read_bytes()).hexdigest()}
            data["native_coverage"] = coverage
            gate.write_text(json.dumps(data))
            with patch.object(builds.subprocess, "check_output", return_value=SHA):
                self.assertEqual(builds.record(root, "26.3", SHA)["native_coverage"], coverage)
            first = root / "verification-logs" / ("artifacts-" + versions[0]) / "runtime.jar"
            first.write_bytes(b"replaced after native pass")
            with patch.object(builds.subprocess, "check_output", return_value=SHA):
                with self.assertRaisesRegex(ValueError, "loader-profile JAR changed"):
                    builds.record(root, "26.3", SHA)
            first.write_bytes(b"profile native runtime " + bytes([0]))
            data["native_coverage"][versions[1]]["packaged_artifact_sha256"] = {"runtime.jar": hashlib.sha256(b"wrong").hexdigest()}
            gate.write_text(json.dumps(data))
            with patch.object(builds.subprocess, "check_output", return_value=SHA):
                with self.assertRaisesRegex(ValueError, "loader-profile JAR changed"):
                    builds.record(root, "26.3", SHA)

    def test_modern_native_manifest_covers_every_packaged_jar(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            mc, jar = self.fixture(root)
            gate = root / 'verification-logs/rc-development-gate.json'
            original = json.loads(gate.read_text())
            second = root / 'build/libs/other.jar'
            second.write_bytes(b'an additional native build artifact')
            with patch.object(builds.subprocess, 'check_output', return_value=SHA):
                with self.assertRaisesRegex(ValueError, 'artifact manifest'):
                    builds.record(root, mc, SHA)
            attested = dict(original['artifact_sha256'],
                            **{'build/libs/other.jar': hashlib.sha256(second.read_bytes()).hexdigest()})
            gate.write_text(json.dumps(dict(original, artifact_sha256=attested)))
            with patch.object(builds.subprocess, 'check_output', return_value=SHA):
                report = builds.record(root, mc, SHA)
            self.assertEqual(set(report['artifact_sha256']), {'runtime.jar', 'other.jar'})
            for changed in ({}, {'build/libs/runtime.jar': attested['build/libs/runtime.jar']},
                            dict(attested, **{'build/libs/unbuilt.jar': 'a' * 64}),
                            dict(attested, **{'build/libs/other.jar': 'not-a-sha256'}),
                            dict(attested, **{'build/libs/other.jar': '0' * 64})):
                gate.write_text(json.dumps(dict(original, artifact_sha256=changed)))
                with self.subTest(manifest=changed), patch.object(builds.subprocess, 'check_output', return_value=SHA):
                    with self.assertRaises(ValueError):
                        builds.record(root, mc, SHA)

    def test_changed_runtime_or_wrong_source_never_get_success_receipt(self):
        for legacy in (False,True):
            with tempfile.TemporaryDirectory() as directory:
                root=Path(directory);mc,jar=self.fixture(root,legacy)
                with patch.object(builds.subprocess,'check_output',return_value='b'*40):
                    with self.assertRaises(ValueError):builds.record(root,mc,SHA)
                jar.write_bytes(b'different binary')
                with patch.object(builds.subprocess,'check_output',return_value=SHA):
                    with self.assertRaises(ValueError):builds.record(root,mc,SHA)
                self.assertFalse((root/'verification-logs/hourly-native.json').exists())


class PublicationTests(unittest.TestCase):
    def status(self, **changes):
        return dict(dict(minecraft='1.21.1', version='0.2.0-rc.1', release_channel='beta',rc_publication_cleared=True),**changes)

    def test_rc_requires_explicit_boolean_clearance(self):
        for flag in (False,None,'true',1):
            with self.assertRaisesRegex(ValueError,'cleared'):
                require_publication(self.status(rc_publication_cleared=flag))
        self.assertEqual(require_publication(self.status()),'beta')

    def test_identity_channel_and_development_aliases_rejected(self):
        for version in ('0.2.0-rc.1-dev','1.0.0_SNAPSHOT','1.0.0-nightly.4'):
            with self.assertRaises(ValueError):require_publication(self.status(version=version))
        with self.assertRaises(ValueError):require_publication(self.status(release_channel='development'))
        with self.assertRaises(ValueError):require_publication(self.status(),'1.16.4','0.2.0-rc.1')
        with self.assertRaises(ValueError):require_publication(self.status(),'1.21.1','0.2.0-rc.2')
        self.assertEqual(release_type('0.2b.0'),'beta')
        self.assertEqual(release_type('0.2a-legacy-preview.3'),'alpha')

    def test_uncleared_tagged_rc_rejected_before_downloading_any_jar(self):
        metadata={'content':base64.b64encode(json.dumps(self.status(rc_publication_cleared=False)).encode()).decode()}
        with tempfile.TemporaryDirectory() as directory, patch.object(curseforge_sync,'gh',return_value=json.dumps(metadata)) as gh, patch.object(curseforge_sync.subprocess,'run') as download:
            with self.assertRaisesRegex(ValueError,'cleared'):
                curseforge_sync.stage_release('v0.2.0-rc.1',REPO,Path(directory))
            self.assertIn('ref=v0.2.0-rc.1', gh.call_args.args[1])
            download.assert_not_called()

    def test_main_release_publisher_never_overwrites_or_edits_existing_assets(self):
        source = (Path(__file__).resolve().parents[1] / ".github/workflows/release.yml").read_text()
        self.assertNotIn("--clobber", source)
        self.assertNotIn("gh release upload", source)
        self.assertNotIn("gh release edit", source)
        self.assertIn("gh release create", source)
        self.assertIn("Existing GitHub release $tag", source)

    def test_main_release_preflight_fails_closed_before_tag_creation(self):
        source = (Path(__file__).resolve().parents[1] / ".github/workflows/release.yml").read_text()
        release_step = source.split("- name: Publish validated GitHub Release", 1)[1]
        self.assertIn('gh api "repos/$env:GITHUB_REPOSITORY/releases/tags/$tag"', release_step)
        self.assertIn("if ($releaseCheck -notmatch 'HTTP 404')", release_step)
        self.assertIn('Cannot prove GitHub release $tag is absent', release_step)
        self.assertLess(release_step.index('$releaseCheck ='), release_step.index('$remoteTag ='))
        self.assertLess(release_step.index('$releaseCheck ='), release_step.index('gh api "repos/$env:GITHUB_REPOSITORY/git/refs"'))

    def test_direct_rc_upload_requires_local_exact_status_before_network(self):
        env=dict(CURSEFORGE_PROJECT_ID='123',CURSEFORGE_API_TOKEN='test-only',RELEASE_TAG='v0.2.0-rc.1',GITHUB_REPOSITORY=REPO)
        with tempfile.TemporaryDirectory() as directory:
            status=Path(directory)/'BUILD_STATUS.json';status.write_text(json.dumps(self.status(rc_publication_cleared=False)))
            old=Path.cwd()
            try:
                os.chdir(directory)
                with patch.dict(os.environ,env,clear=True),patch.object(upload_curseforge,'gh') as gh,patch.object(upload_curseforge,'request_json') as api:
                    with self.assertRaisesRegex(ValueError,'cleared'):upload_curseforge.main()
                    gh.assert_not_called();api.assert_not_called()
            finally:os.chdir(old)


if __name__ == '__main__':
    unittest.main()
