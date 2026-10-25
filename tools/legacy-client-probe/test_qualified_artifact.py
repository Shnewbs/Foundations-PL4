import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
from qualified_artifact import runtime_from_archive


class QualifiedArtifactTests(unittest.TestCase):
    def check_archive(self, changed=None, bad_source=False, bad_archive=False):
        payload=b'production runtime'
        native=dict(minecraft='1.12.2',verification_commit='a'*40,total=168,passed=168,failed=0,runtime_sha256=hashlib.sha256(payload).hexdigest())
        native.update(changed or {})
        with tempfile.TemporaryDirectory() as d:
            path=Path(d)/'native.zip'
            with zipfile.ZipFile(path,'w') as archive:
                archive.writestr('verification-logs/SOURCE_COMMIT.txt',('b' if bad_source else 'a')*40)
                archive.writestr('verification-logs/clean-native-summary.json',json.dumps(native))
                archive.writestr('build/libs/runtime.jar',payload)
            digest='0'*64 if bad_archive else hashlib.sha256(path.read_bytes()).hexdigest()
            return runtime_from_archive(path,digest,'1.12.2','a'*40,168,'runtime.jar')

    def test_exact_source_count_and_production_payload(self):
        self.assertEqual(self.check_archive()[0],b'production runtime')

    def test_archive_digest_required(self):
        with self.assertRaises(ValueError): self.check_archive(bad_archive=True)

    def test_archive_source_required(self):
        with self.assertRaises(ValueError): self.check_archive(bad_source=True)

    def test_summary_source_required(self):
        with self.assertRaises(ValueError): self.check_archive({'verification_commit':'c'*40})

    def test_neighboring_target_rejected(self):
        with self.assertRaises(ValueError): self.check_archive({'minecraft':'1.6.4'})

    def test_empty_or_failing_suites_rejected(self):
        for fields in ({'total':0,'passed':0},{'passed':167,'failed':1}):
            with self.subTest(fields=fields), self.assertRaises(ValueError): self.check_archive(fields)

    def test_production_hash_required(self):
        with self.assertRaises(ValueError): self.check_archive({'runtime_sha256':'0'*64})

if __name__=='__main__': unittest.main()
