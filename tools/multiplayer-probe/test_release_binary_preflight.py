"""Adversarial checks for the real GitHub released-beta JAR preflight."""
import copy
import hashlib
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch
import release_binary_preflight as gate


class PublishedBinaryPreflightTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.path = Path(self.tmp.name) / gate.ASSET
        self.make_jar()
        self.digest = hashlib.sha256(self.path.read_bytes()).hexdigest()
        self.ref = {"ref": "refs/tags/" + gate.TAG,
                    "object": {"sha": gate.SOURCE, "type": "commit"}}
        self.release = {
            "id": gate.RELEASE_ID, "tag_name": gate.TAG,
            "draft": False, "prerelease": True, "published_at": "2026-10-17T22:52:59Z",
            "html_url": "https://github.com/" + gate.REPO + "/releases/tag/" + gate.TAG,
            "assets": [{"id": 901234, "name": gate.ASSET, "state": "uploaded",
                        "digest": "sha256:" + self.digest, "size": self.path.stat().st_size}]
        }

    def tearDown(self):
        self.tmp.cleanup()

    def make_jar(self, metadata=None):
        if metadata is None:
            metadata = ('modId="foundations_pl4"\nversion="0.2b.0"\n'
                        'modId="minecraft"\nversionRange="[1.21.1]"\n')
        with zipfile.ZipFile(self.path, "w") as out:
            out.writestr("META-INF/neoforge.mods.toml", metadata)
            out.writestr("net/foundations/pl4/FoundationsPL4.class", b"\xca\xfe\xba\xbe")

    def accept(self):
        with patch.object(gate, "EXPECTED_SHA256", self.digest), \
             patch.object(gate, "EXPECTED_SIZE", self.release["assets"][0]["size"]):
            return gate.verify(self.release, self.ref, self.path)

    def test_valid_pinned_download_has_no_runtime_clearance(self):
        result = self.accept()
        self.assertEqual(result["asset_sha256"], self.digest)
        self.assertIs(result["installed_binary_executed"], False)
        self.assertIs(result["rc_publication_cleared"], False)

    def test_swapped_tag_commit_rejected(self):
        self.ref["object"]["sha"] = "0" * 40
        with self.assertRaises(ValueError): self.accept()

    def test_wrong_release_identity_rejected(self):
        self.release["id"] += 1
        with self.assertRaises(ValueError): self.accept()

    def test_draft_release_rejected(self):
        self.release["draft"] = True
        with self.assertRaises(ValueError): self.accept()

    def test_duplicate_exact_assets_rejected(self):
        self.release["assets"].append(copy.deepcopy(self.release["assets"][0]))
        with self.assertRaises(ValueError): self.accept()

    def test_altered_remote_digest_rejected(self):
        self.release["assets"][0]["digest"] = "sha256:" + "0"*64
        with self.assertRaises(ValueError): self.accept()

    def test_mutated_jar_rejected(self):
        with self.path.open("ab") as f: f.write(b"tamper")
        with self.assertRaises(ValueError): self.accept()

    def test_wrong_minecraft_target_rejected_even_when_rehashed(self):
        self.make_jar(metadata='modId="foundations_pl4"\nversion="0.2b.0"\nmodId="minecraft"\nversionRange="[1.20.4]"\n')
        self.digest = hashlib.sha256(self.path.read_bytes()).hexdigest()
        self.release["assets"][0]["digest"] = "sha256:" + self.digest
        self.release["assets"][0]["size"] = self.path.stat().st_size
        with self.assertRaises(ValueError): self.accept()

    def test_missing_native_mod_metadata_rejected_even_when_rehashed(self):
        self.make_jar(metadata='not a mod')
        self.digest = hashlib.sha256(self.path.read_bytes()).hexdigest()
        self.release["assets"][0]["digest"] = "sha256:" + self.digest
        self.release["assets"][0]["size"] = self.path.stat().st_size
        with self.assertRaises(ValueError): self.accept()


if __name__ == "__main__":
    unittest.main()
