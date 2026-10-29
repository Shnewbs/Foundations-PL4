"""Fail-closed safety checks for the retired Minecraft 1.13.2 branch publishers.

Only native validation and evidence retention remain active; no branch-local
publishing is permitted until independently reviewed exact-tag RC acceptance.
"""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WF = ROOT / ".github" / "workflows"


def workflow(filename):
    return (WF / filename).read_text(encoding="utf-8")


class PublisherSafety1132(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.native = workflow("forge-native.yml")
        cls.release = workflow("release.yml")
        cls.curseforge = workflow("curseforge.yml")
        cls.status = json.loads((ROOT / "BUILD_STATUS.json").read_text(encoding="utf-8"))

    def test_exact_version_and_development_maturity(self):
        self.assertEqual(self.status["minecraft"], "1.13.2")
        self.assertNotEqual(self.status.get("rc_publication_cleared"), True)
        self.assertEqual(self.status["release_channel"], "alpha")

    def test_native_job_is_read_only(self):
        self.assertIn("contents: read", self.native)
        self.assertNotIn("contents: write", self.native)
        self.assertIn("branches: [mc/1.13.2]", self.native)

    def test_no_unreviewed_source_transformation_or_push(self):
        for forbidden in ("tools/prepare_1132.py", "tools/backport_1132_mcp_names.py"):
            self.assertNotIn(forbidden, self.native)
        self.assertIsNone(re.search(r"\bgit\s+(?:commit|push|reset|checkout\s+-B)\b", self.native))

    def test_exact_source_provenance_retained(self):
        for expected in ("git archive --format=zip", "checked-source.zip",
                         "SOURCE_COMMIT.txt", "SOURCE_ARCHIVE_SHA256SUMS.txt",
                         'test "$(git rev-parse HEAD)" = "$GITHUB_SHA"'):
            self.assertIn(expected, self.native)

    def test_native_compile_and_scenarios_still_required(self):
        for expected in ("bash gradlew --no-daemon --console=plain clean build",
                         "tools/java8/runtime.py native", "report['total']==194",
                         "report['passed']==194", "report['failed']==0"):
            self.assertIn(expected, self.native)

    def test_native_artifacts_and_hashes_retained(self):
        for expected in ("actions/upload-artifact@v4", "JAR_SHA256SUMS.txt",
                         "LOG_SHA256SUMS.txt", "build/libs/*.jar", "retention-days: 14"):
            self.assertIn(expected, self.native)

    def test_no_new_build_for_workflow_only_changes(self):
        self.assertIn("paths-ignore:", self.native)
        self.assertIn("'.github/workflows/**'", self.native)

    def test_old_release_is_not_tag_or_main_triggered(self):
        self.assertIn("branches: ['mc/1.13.2']", self.release)
        self.assertNotIn("tags:", self.release)
        self.assertNotIn("branches: [main]", self.release)

    def test_old_release_is_read_only_and_audit_only(self):
        self.assertIn("contents: read", self.release)
        self.assertIn("unittest", self.release)

    def test_old_curseforge_is_read_only_and_audit_only(self):
        self.assertIn("contents: read", self.curseforge)
        self.assertIn("unittest", self.curseforge)
        self.assertIn("branches: ['mc/1.13.2']", self.curseforge)

    def test_no_branch_local_publication_or_secret_access(self):
        for name, body in (("native", self.native),
                           ("release", self.release),
                           ("curseforge", self.curseforge)):
            for forbidden in ("contents: write", "secrets.", "upload_curseforge.py",
                              "gh release", "gh api", "--clobber",
                              "CURSEFORGE_API_TOKEN", "FoundationsPL4-1.21.1-"):
                with self.subTest(workflow=name, forbidden=forbidden):
                    self.assertNotIn(forbidden, body)


if __name__ == "__main__":
    unittest.main()
