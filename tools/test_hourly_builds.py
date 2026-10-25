"""Scheduled build identity/deduplication and RC publication failure cases; no network."""
import base64
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

    def fixture(self, root, legacy=False):
        logs=root/'verification-logs';logs.mkdir();libs=root/'build/libs';libs.mkdir(parents=True)
        jar=libs/'runtime.jar';jar.write_bytes(b'qualified native runtime');digest=hashlib.sha256(jar.read_bytes()).hexdigest()
        mc='1.6.4' if legacy else '1.21.1'
        def save(name, value):(logs/name).write_text(json.dumps(value))
        (root/'BUILD_STATUS.json').write_text(json.dumps(dict(minecraft=mc,version='0.2.0-rc.1-dev',expected_native_scenarios=156)))
        if legacy:
            save('clean-native-summary.json',dict(minecraft=mc,verification_commit=SHA,total=156,passed=156,failed=0,runtime_sha256=digest))
            save('production-smoke.json',dict(minecraft=mc,source_commit=SHA,runtime_sha256=digest,production_only_startup='PASS',clean_stop='PASS'))
            save('legacy-migration-summary.json',dict(minecraft=mc,source_commit=SHA,candidate_runtime_sha256=digest,generated_preview3_world_upgrade='PASS'))
        else:
            save('rc-development-gate.json',dict(minecraft=mc,version='0.2.0-rc.1-dev',source_parent=SHA,native_gate='PASS',expected_fixtures=156,artifact_sha256={'build/libs/runtime.jar':digest}))
        return mc,jar

    def test_native_proof_stays_unpublished_for_both_runtime_kinds(self):
        for legacy in (False,True):
            with tempfile.TemporaryDirectory() as directory:
                root=Path(directory);mc,_=self.fixture(root,legacy)
                with patch.object(builds.subprocess,'check_output',return_value=SHA+'\n'):
                    report=builds.record(root,mc,SHA)
                self.assertEqual(report['native_gate'],'PASS')
                self.assertFalse(report['publication_performed'])
                self.assertFalse(report['rc_publication_cleared'])

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
