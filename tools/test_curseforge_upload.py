"""Exercise upload retry guards without credentials or external requests."""
import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import upload_curseforge as upload


class UploadTests(unittest.TestCase):
    def test_unconfigured_never_contacts_services(self):
        with patch.dict(os.environ, {}, clear=True), patch.object(upload, 'gh') as gh, patch.object(upload, 'request_json') as api:
            upload.main()
            gh.assert_not_called()
            api.assert_not_called()

    def test_partial_configuration_fails_before_network(self):
        with patch.dict(os.environ, {'CURSEFORGE_PROJECT_ID': '123'}, clear=True), patch.object(upload, 'request_json') as api:
            with self.assertRaises(RuntimeError):
                upload.main()
            api.assert_not_called()

    def test_redirect_does_not_forward_token(self):
        with self.assertRaises(RuntimeError):
            upload.NoRedirect().redirect_request(None, None, 302, '', {}, 'https://example.com')

    def test_receipt_skips_identical_upload_and_rejects_changed_jar(self):
        previous = Path.cwd()
        with tempfile.TemporaryDirectory() as directory:
            os.chdir(directory)
            try:
                Path('BUILD_STATUS.json').write_text(json.dumps({'minecraft': '1.20.2', 'version': '0.2a-port.1'}))
                jar = Path('build/libs/FoundationsPL4-1.20.2-0.2a-port.1.jar')
                jar.parent.mkdir(parents=True)
                jar.write_bytes(b'validated artifact')
                digest = hashlib.sha256(jar.read_bytes()).hexdigest()
                def gh(*args):
                    if args[1] == 'view':
                        return json.dumps({'assets': [{'name': upload.MARKER}]})
                    Path(args[args.index('--dir') + 1], upload.MARKER).write_text(json.dumps({'project_id': '123', 'sha256': digest, 'file_id': 456}))
                    return ''
                env = {'CURSEFORGE_PROJECT_ID': '123', 'CURSEFORGE_API_TOKEN': 'test-only', 'VERSION': '0.2a-port.1', 'RELEASE_TAG': 'mc1.20.2-v0.2a-port.1', 'MINECRAFT_VERSION': '1.20.2', 'GITHUB_REPOSITORY': 'example/repo'}
                with patch.dict(os.environ, env, clear=True), patch.object(upload, 'gh', side_effect=gh), patch.object(upload, 'request_json') as api:
                    upload.main()
                    jar.write_bytes(b'changed artifact')
                    with self.assertRaises(RuntimeError):
                        upload.main()
                    api.assert_not_called()
            finally:
                os.chdir(previous)

    def test_target_mismatch_stops_before_external_requests(self):
        previous = Path.cwd()
        with tempfile.TemporaryDirectory() as directory:
            os.chdir(directory)
            try:
                Path('BUILD_STATUS.json').write_text(json.dumps({'minecraft': '1.20.2', 'version': '0.2a-port.1'}))
                env = {'CURSEFORGE_PROJECT_ID': '123', 'CURSEFORGE_API_TOKEN': 'test-only', 'VERSION': '0.2a-port.1', 'RELEASE_TAG': 'mc1.20.1-v0.2a-port.1', 'MINECRAFT_VERSION': '1.20.1', 'GITHUB_REPOSITORY': 'example/repo'}
                with patch.dict(os.environ, env, clear=True), patch.object(upload, 'gh') as gh, patch.object(upload, 'request_json') as api:
                    with self.assertRaises(RuntimeError):
                        upload.main()
                    gh.assert_not_called()
                    api.assert_not_called()
            finally:
                os.chdir(previous)

    def test_upload_uses_forge_artifact_and_metadata(self):
        previous = Path.cwd()
        with tempfile.TemporaryDirectory() as directory:
            os.chdir(directory)
            try:
                Path('BUILD_STATUS.json').write_text(json.dumps({'minecraft': '1.20.2', 'version': '0.2a-port.1'}))
                jar = Path('build/libs/FoundationsPL4-1.20.2-0.2a-port.1.jar')
                jar.parent.mkdir(parents=True)
                jar.write_bytes(b'validated Forge artifact')
                env = {'CURSEFORGE_PROJECT_ID': '123', 'CURSEFORGE_API_TOKEN': 'test-only', 'VERSION': '0.2a-port.1', 'RELEASE_TAG': 'mc1.20.2-v0.2a-port.1', 'MINECRAFT_VERSION': '1.20.2', 'GITHUB_REPOSITORY': 'example/repo'}
                with patch.dict(os.environ, env, clear=True), patch.object(upload, 'gh', side_effect=['{"assets": []}', '']) as gh, patch.object(upload, 'request_json', return_value={'id': 789}) as api:
                    upload.main()
                    self.assertIn('"gameVersionNames": ["1.20.2", "Forge", "Client", "Server"]', api.call_args.args[2].decode())
                    self.assertIn(jar.name, api.call_args.args[2].decode())
                    self.assertEqual(gh.call_count, 2)
            finally:
                os.chdir(previous)


if __name__ == '__main__':
    unittest.main()
