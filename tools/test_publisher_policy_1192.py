"""Safety regression against obsolete wrong-target 1.19.2 publication paths."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WF = ROOT / ".github" / "workflows"


class PublisherPolicy1192(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.status = json.loads((ROOT / "BUILD_STATUS.json").read_text())
        cls.release = (WF / "release.yml").read_text()
        cls.cf = (WF / "curseforge.yml").read_text()
        cls.verify = (WF / "verify.yml").read_text()

    def test_exact_development_version_held(self):
        self.assertEqual(self.status["minecraft"], "1.19.2")
        self.assertEqual(self.status["release_channel"], "development")
        self.assertIs(self.status["rc_publication_cleared"], False)

    def test_both_audits_read_only(self):
        for source in (self.release, self.cf):
            self.assertIn("contents: read", source)
            self.assertNotIn("contents: write", source)

    def test_branch_local_audits_are_exact(self):
        self.assertIn("branches: ['mc/1.19.2']", self.release)
        self.assertIn("branches: ['mc/1.19.2']", self.cf)

    def test_no_tag_or_release_push_triggers(self):
        for source in (self.release, self.cf):
            self.assertNotIn("tags:", source)
            self.assertIn("unittest", source)

    def test_no_direct_release_mutations_or_wrong_jar(self):
        for name, source in (("release", self.release), ("curseforge", self.cf)):
            for forbidden in ("gh release", "gh api", "git tag ",
                              "git push", "GH_TOKEN", "secrets.",
                              "upload_curseforge.py", "FoundationsPL4-1.21.1-",
                              "--clobber", "CURSEFORGE_API_TOKEN"):
                with self.subTest(name=name, forbidden=forbidden):
                    self.assertNotIn(forbidden, source)

    def test_source_native_validation_is_preserved(self):
        self.assertIn("clean build runGameTestServer", self.verify)
        self.assertIn("build/libs/*.jar", self.verify)
        self.assertIn("retention-days: 14", self.verify)

    def test_no_new_acceptance_claims(self):
        self.assertTrue(self.status["further_api_testing_required"])
        self.assertFalse(self.status["full_pl2_parity"])
        self.assertFalse(self.status["release_published"])


if __name__ == "__main__":
    unittest.main()
