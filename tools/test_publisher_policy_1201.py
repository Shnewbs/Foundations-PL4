"""Fail-closed release and artifact policy for native Forge 1.20.1."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WF = ROOT / ".github" / "workflows"


class PublisherPolicy1201(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.native = (WF / "forge-port.yml").read_text()
        cls.release = (WF / "release.yml").read_text()
        cls.cf = (WF / "curseforge.yml").read_text()
        cls.status = json.loads((ROOT / "BUILD_STATUS.json").read_text())

    def test_exact_development_source_has_no_clearance(self):
        self.assertEqual(self.status["minecraft"], "1.20.1")
        self.assertEqual(self.status["release_channel"], "development")
        self.assertIs(self.status["rc_publication_cleared"], False)

    def test_native_readonly_and_version_pinned(self):
        self.assertIn("branches: [mc/1.20.1]", self.native)
        self.assertIn("contents: read", self.native)
        self.assertIn("= '1.20.1'", self.native)

    def test_exact_source_identity_and_archive(self):
        for token in ("git rev-parse HEAD", "GITHUB_SHA", "checked-source.zip",
                      "source-commit.txt", "SOURCE_ARCHIVE_SHA256SUMS.txt"):
            self.assertIn(token, self.native)

    def test_native_validation_preserved(self):
        self.assertIn("java-version: '17'", self.native)
        self.assertIn("clean build runGameTestServer", self.native)
        self.assertIn("All 191 required tests passed", self.native)

    def test_native_production_and_log_hashes_retained(self):
        for token in ("JAR_SHA256SUMS.txt", "LOG_SHA256SUMS.txt",
                      "build/libs/", "retention-days: 14"):
            self.assertIn(token, self.native)

    def test_policy_only_edits_skip_native_run(self):
        self.assertIn("paths-ignore:", self.native)
        self.assertIn("'.github/workflows/**'", self.native)
        self.assertIn("test_publisher_policy_1201.py", self.native)

    def test_generic_release_readonly_audit(self):
        self.assertIn("branches: ['mc/1.20.1']", self.release)
        self.assertIn("unittest", self.release)
        self.assertNotIn("tags:", self.release)

    def test_curseforge_readonly_audit(self):
        self.assertIn("branches: ['mc/1.20.1']", self.cf)
        self.assertIn("unittest", self.cf)
        self.assertNotIn("tags:", self.cf)

    def test_no_unsafe_release_operations(self):
        for name, body in (("native", self.native), ("release", self.release),
                           ("curseforge", self.cf)):
            for forbidden in ("contents: write", "GH_TOKEN", "secrets.",
                              "CURSEFORGE_API_TOKEN", "upload_curseforge.py",
                              "gh release", "gh api", "--clobber", "git push",
                              "git tag ", "FoundationsPL4-1.21.1-"):
                with self.subTest(name=name, forbidden=forbidden):
                    self.assertNotIn(forbidden, body)

    def test_native_has_no_legacy_transform(self):
        self.assertNotIn("prepare_patch_port.py", self.native)
        self.assertNotIn("git commit", self.native)


if __name__ == "__main__":
    unittest.main()
