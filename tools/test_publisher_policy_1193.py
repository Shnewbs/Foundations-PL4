"""Branch release-safety regression for exact Forge 1.19.3."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WF = ROOT / ".github" / "workflows"


class PublisherSafety1193(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.status = json.loads((ROOT / "BUILD_STATUS.json").read_text())
        cls.release = (WF / "release.yml").read_text()
        cls.cf = (WF / "curseforge.yml").read_text()
        cls.native = (WF / "forge1193-native.yml").read_text()

    def test_status_is_exact_development_and_uncleared(self):
        self.assertEqual(self.status["minecraft"], "1.19.3")
        self.assertEqual(self.status["release_channel"], "development")
        self.assertIs(self.status["rc_publication_cleared"], False)

    def test_audits_are_readonly(self):
        for source in (self.release, self.cf):
            self.assertIn("contents: read", source)
            self.assertNotIn("contents: write", source)

    def test_audits_target_exact_version_branch(self):
        for source in (self.release, self.cf):
            self.assertIn("branches: ['mc/1.19.3']", source)
            self.assertIn("unittest", source)

    def test_no_tag_or_mutation_paths(self):
        for source in (self.release, self.cf):
            for forbidden in ("tags:", "gh release", "gh api", "git push",
                              "git tag ", "GH_TOKEN", "secrets.", "--clobber",
                              "CURSEFORGE_API_TOKEN", "upload_curseforge.py",
                              "FoundationsPL4-1.21.1-"):
                self.assertNotIn(forbidden, source)

    def test_native_validation_unchanged(self):
        self.assertIn("contents: read", self.native)
        self.assertIn("clean build", self.native)
        self.assertIn("tools/run_1193_native_tests.py", self.native)
        self.assertIn("retention-days: 14", self.native)

    def test_no_acceptance_or_release_claims(self):
        self.assertTrue(self.status["further_api_testing_required"])
        self.assertFalse(self.status["full_pl2_parity"])
        self.assertFalse(self.status["release_published"])


if __name__ == "__main__":
    unittest.main()
