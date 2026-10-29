"""Read-only publisher-safety regression suite for exact Forge 1.17.1."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WF = ROOT / ".github" / "workflows"


class PublisherPolicy1171(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.native = (WF / "forge117-native.yml").read_text()
        cls.release = (WF / "release.yml").read_text()
        cls.curseforge = (WF / "curseforge.yml").read_text()
        cls.status = json.loads((ROOT / "BUILD_STATUS.json").read_text())

    def test_exact_unqualified_development_source(self):
        self.assertEqual(self.status["minecraft"], "1.17.1")
        self.assertEqual(self.status["release_channel"], "development")
        self.assertIs(self.status["rc_publication_cleared"], False)

    def test_readonly_permissions_in_all_paths(self):
        for source in (self.native, self.release, self.curseforge):
            self.assertIn("contents: read", source)
            self.assertNotIn("contents: write", source)

    def test_native_pinned_to_exact_branch(self):
        self.assertIn("branches: [mc/1.17.1]", self.native)
        self.assertIn("refs/heads/mc/1.17.1", self.native)
        self.assertIn("= '1.17.1'", self.native)

    def test_native_immutable_source_retained(self):
        for token in ("git rev-parse HEAD", "GITHUB_SHA", "checked-source.zip",
                      "source-commit.txt", "SOURCE_ARCHIVE_SHA256SUMS.txt"):
            self.assertIn(token, self.native)

    def test_native_compile_and_installed_validation_preserved(self):
        self.assertIn("java-version: '17'", self.native)
        self.assertIn("clean build", self.native)
        self.assertIn("tools/verify_installed117.py", self.native)
        self.assertLess(self.native.index("clean build"), self.native.index("verify_installed117.py"))

    def test_production_artifact_and_log_receipts(self):
        for token in ("JAR_SHA256SUMS.txt", "LOG_SHA256SUMS.txt",
                      "build/libs/*.jar", "run-installed-117/port-scenarios.json",
                      "retention-days: 14"):
            self.assertIn(token, self.native)

    def test_no_native_source_transforms_or_automatic_push(self):
        for forbidden in ("git commit", "git push", "prepare_patch_port.py",
                          "prepare_02a_ports.py", "git tag "):
            self.assertNotIn(forbidden, self.native)

    def test_workflow_only_edits_dont_run_expensive_native_matrix(self):
        self.assertIn("paths-ignore:", self.native)
        self.assertIn("'.github/workflows/**'", self.native)
        self.assertIn("test_publisher_policy_1171.py", self.native)

    def test_generic_publisher_is_only_audit(self):
        self.assertIn("branches: ['mc/1.17.1']", self.release)
        self.assertNotIn("tags:", self.release)
        self.assertIn("unittest", self.release)

    def test_curseforge_is_only_audit(self):
        self.assertIn("branches: ['mc/1.17.1']", self.curseforge)
        self.assertNotIn("tags:", self.curseforge)
        self.assertIn("unittest", self.curseforge)

    def test_no_secret_or_asset_mutation_anywhere(self):
        for name, source in (("native", self.native), ("release", self.release),
                             ("curseforge", self.curseforge)):
            for forbidden in ("GH_TOKEN", "secrets.", "CURSEFORGE_API_TOKEN",
                              "upload_curseforge.py", "gh release",
                              "gh api", "--clobber", "FoundationsPL4-1.21.1-",
                              "git push", "git tag "):
                with self.subTest(name=name, forbidden=forbidden):
                    self.assertNotIn(forbidden, source)


if __name__ == "__main__":
    unittest.main()
