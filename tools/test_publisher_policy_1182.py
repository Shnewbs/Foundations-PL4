"""Exact Minecraft 1.18.2 branch publication safety assertions."""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WF = ROOT / ".github" / "workflows"


class PublisherSafety1182(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.native = (WF / "forge118-native.yml").read_text(encoding="utf-8")
        cls.release = (WF / "release.yml").read_text(encoding="utf-8")
        cls.curseforge = (WF / "curseforge.yml").read_text(encoding="utf-8")
        cls.status = json.loads((ROOT / "BUILD_STATUS.json").read_text(encoding="utf-8"))

    def test_native_source_is_exact_target(self):
        self.assertEqual(self.status["minecraft"], "1.18.2")
        self.assertIn("branches: [mc/1.18.2]", self.native)

    def test_no_publication_clearance_from_native_success(self):
        self.assertIs(self.status["rc_publication_cleared"], False)
        self.assertEqual(self.status["release_channel"], "development")

    def test_native_workflow_is_read_only(self):
        self.assertIn("contents: read", self.native)
        self.assertNotIn("contents: write", self.native)
        self.assertIsNone(re.search(r"\bgit\s+(?:push|commit|tag)\b", self.native))

    def test_old_worldgen_rewrite_is_not_executed(self):
        self.assertNotIn("Wire native world generation", self.native)
        self.assertNotIn("PortWorldgen::register", self.native)
        self.assertNotIn("p.write_text(", self.native)
        self.assertNotIn("prepare new immutable revision", self.native)

    def test_exact_source_attestation(self):
        for token in ('test "$(git rev-parse HEAD)" = "$GITHUB_SHA"',
                      "checked-source.zip", "SOURCE_COMMIT.txt", "SOURCE_ARCHIVE_SHA256SUMS.txt"):
            self.assertIn(token, self.native)

    def test_native_193_tests_not_disabled(self):
        for token in ("clean build runGameTestServer", "All 193 required tests passed",
                      "actions/setup-java@v4", "java-version: '17'"):
            self.assertIn(token, self.native)

    def test_production_jars_and_logs_preserved(self):
        for token in ("JAR_SHA256SUMS.txt", "LOG_SHA256SUMS.txt", "build/libs/",
                      "verification-logs/", "actions/upload-artifact@v4"):
            self.assertIn(token, self.native)

    def test_policy_only_push_does_not_duplicate_native_build(self):
        self.assertIn("paths-ignore:", self.native)
        self.assertIn("'.github/workflows/**'", self.native)
        self.assertIn("'tools/test_publisher_policy_1182.py'", self.native)

    def test_old_generic_release_no_main_or_tag_trigger(self):
        self.assertIn("branches: ['mc/1.18.2']", self.release)
        self.assertNotIn("tags:", self.release)
        self.assertNotIn("branches: [main]", self.release)

    def test_old_publishers_are_read_only(self):
        for body in (self.release, self.curseforge):
            self.assertIn("contents: read", body)
            self.assertIn("unittest", body)

    def test_no_branch_publisher_mutation_or_secrets(self):
        for name, body in (("native", self.native), ("release", self.release),
                           ("curseforge", self.curseforge)):
            for forbidden in ("contents: write", "secrets.", "CURSEFORGE_API_TOKEN",
                              "upload_curseforge.py", "gh release", "gh api",
                              "gh workflow run", "--clobber", "FoundationsPL4-1.21.1-"):
                with self.subTest(name=name, forbidden=forbidden):
                    self.assertNotIn(forbidden, body)


if __name__ == "__main__":
    unittest.main()
