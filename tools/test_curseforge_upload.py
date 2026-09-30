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
                jar = Path('build/libs/FoundationsPL4-1.21.1-0.0.2a.R1.jar')
                jar.parent.mkdir(parents=True)
                jar.write_bytes(b'validated artifact')
                digest = hashlib.sha256(jar.read_bytes()).hexdigest()
                def gh(*args):
                    if args[1] == 'view':
                        return json.dumps({'assets': [{'name': upload.MARKER}]})
                    Path(args[args.index('--dir') + 1], upload.MARKER).write_text(json.dumps({'project_id': '123', 'sha256': digest, 'file_id': 456}))
                    return ''
                env = {'CURSEFORGE_PROJECT_ID': '123', 'CURSEFORGE_API_TOKEN': 'test-only', 'VERSION': '0.0.2a.R1', 'RELEASE_TAG': 'v0.0.2a.R1', 'GITHUB_REPOSITORY': 'example/repo'}
                with patch.dict(os.environ, env, clear=True), patch.object(upload, 'gh', side_effect=gh), patch.object(upload, 'request_json') as api:
                    upload.main()
                    jar.write_bytes(b'changed artifact')
                    with self.assertRaises(RuntimeError):
                        upload.main()
                    api.assert_not_called()
            finally:
                os.chdir(previous)


if __name__ == '__main__':
    unittest.main()
