"""Static regression: unqualified Forge 1.20.2 source must never publish or mutate tags."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WF = ROOT / ".github" / "workflows"


class PublisherPolicy1202(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.native = (WF / "forge1202-native.yml").read_text(encoding="utf-8")
        cls.release = (WF / "release.yml").read_text(encoding="utf-8")
        cls.cf = (WF / "curseforge.yml").read_text(encoding="utf-8")
        cls.status = json.loads((ROOT / "BUILD_STATUS.json").read_text(encoding="utf-8"))

    def test_exact_target_development_not_cleared(self):
        self.assertEqual(self.status["minecraft"], "1.20.2")
        self.assertEqual(self.status["release_channel"], "development")
        self.assertIs(self.status["rc_publication_cleared"], False)

    def test_no_claim_of_completed_acceptance(self):
        self.assertFalse(self.status["release_published"])
        self.assertTrue(self.status["further_api_testing_required"])
        self.assertFalse(self.status["full_pl2_parity"])

    def test_native_still_source_bound_and_read_only(self):
        self.assertIn("branches: [mc/1.20.2]", self.native)
        self.assertIn("contents: read", self.native)
        self.assertIn("test \"$(git rev-parse HEAD)\" = \"${GITHUB_SHA}\"", self.native)
        self.assertIn("SOURCE_COMMIT.txt", self.native)
        self.assertIn("source-at-build.zip", self.native)

    def test_native_java_and_game_test_count_preserved(self):
        self.assertIn("java-version: '17'", self.native)
        self.assertIn("clean build runGameTestServer", self.native)
        self.assertIn("All 191 required tests passed", self.native)
        self.assertIn("retention-days: 14", self.native)

    def test_native_real_hash_receipts(self):
        for token in ("SOURCE_ARCHIVE_SHA256SUMS.txt", "LOG_SHA256SUMS.txt", "JAR_SHA256SUMS.txt", "sha256sum -c"):
            with self.subTest(token=token):
                self.assertIn(token, self.native)

    def test_native_policy_only_edits_skip_native_run(self):
        self.assertIn("paths-ignore:", self.native)
        self.assertIn("'.github/workflows/**'", self.native)
        self.assertIn("tools/test_publisher_policy_1202.py", self.native)

    def test_two_readonly_audits_target_exact_branch(self):
        for source in (self.release, self.cf):
            self.assertIn("branches: ['mc/1.20.2']", source)
            self.assertIn("contents: read", source)
            self.assertIn("unittest", source)

    def test_no_direct_upload_release_or_tag_modification(self):
        for name, source in (("native", self.native), ("release", self.release), ("curseforge", self.cf)):
            for token in ("contents: write", "GH_TOKEN", "secrets.", "CURSEFORGE_API_TOKEN",
                          "upload_curseforge.py", "gh release", "gh api", "git push",
                          "git tag ", "--clobber", "FoundationsPL4-1.21.1-", "tags:"):
                with self.subTest(workflow=name, token=token):
                    self.assertNotIn(token, source)

    def test_no_obsolete_source_transforms(self):
        for token in ("prepare_patch_port.py", "git commit", "set_version.py"):
            self.assertNotIn(token, self.native)

    def test_central_reconciliation_remains_separate(self):
        self.assertNotIn("Upload release", self.cf)
        self.assertNotIn("Publish verified", self.native)


if __name__ == "__main__":
    unittest.main()
