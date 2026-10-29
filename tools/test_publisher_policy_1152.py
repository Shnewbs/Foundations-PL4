"""Minecraft 1.15.2 Forge31 branch-local publishing regression gates."""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WF = ROOT / ".github" / "workflows"


class PublisherSafety1152(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.native = (WF / "forge-native.yml").read_text(encoding="utf-8")
        cls.release = (WF / "release.yml").read_text(encoding="utf-8")
        cls.curseforge = (WF / "curseforge.yml").read_text(encoding="utf-8")
        cls.status = json.loads((ROOT / "BUILD_STATUS.json").read_text(encoding="utf-8"))

    def test_native_exact_version(self):
        self.assertEqual(self.status["minecraft"], "1.15.2")
        self.assertIn("branches: [mc/1.15.2]", self.native)

    def test_rc_clearance_remains_false(self):
        self.assertIs(self.status["rc_publication_cleared"], False)
        self.assertEqual(self.status["release_channel"], "development")

    def test_no_auto_source_transform_or_push(self):
        for forbidden in ("tools/prepare_1152.py", "tools/backport_1152_api.py",
                          "git commit", "git push", "git tag"):
            self.assertNotIn(forbidden, self.native)

    def test_read_only_native_evidence(self):
        self.assertIn("contents: read", self.native)
        self.assertNotIn("contents: write", self.native)
        for required in ("checked-source.zip", "SOURCE_COMMIT.txt",
                         "SOURCE_ARCHIVE_SHA256SUMS.txt", "JAR_SHA256SUMS.txt",
                         "LOG_SHA256SUMS.txt"):
            self.assertIn(required, self.native)

    def test_java8_exact_native_checks_remain(self):
        for required in ("bash gradlew --no-daemon --console=plain clean build",
                         "tools/java8/runtime.py native", "report['total']==194",
                         "report['passed']==194", "report['failed']==0"):
            self.assertIn(required, self.native)

    def test_real_production_jar_smoke_is_required(self):
        self.assertIn("tools/production_smoke_1152.py", self.native)
        self.assertLess(self.native.index("runtime.py native"),
                        self.native.index("production_smoke_1152.py"))

    def test_failure_diagnostics_and_retention_preserved(self):
        for required in ("tools/inspect_1152_api.py", "if: failure()",
                         "actions/upload-artifact@v4", "retention-days: 14",
                         "run-java8/crash-reports/"):
            self.assertIn(required, self.native)

    def test_policy_edits_do_not_start_duplicate_native_jobs(self):
        self.assertIn("paths-ignore:", self.native)
        self.assertIn("'.github/workflows/**'", self.native)
        self.assertIn("'tools/test_publisher_policy_1152.py'", self.native)

    def test_release_audit_scoped_to_exact_branch(self):
        self.assertIn("branches: ['mc/1.15.2']", self.release)
        self.assertNotIn("tags:", self.release)
        self.assertNotIn("branches: [main]", self.release)

    def test_curseforge_audit_scoped_to_exact_branch(self):
        self.assertIn("branches: ['mc/1.15.2']", self.curseforge)
        self.assertIn("unittest", self.curseforge)

    def test_no_publisher_or_secret_remains(self):
        for name, body in (("native", self.native), ("release", self.release),
                           ("curseforge", self.curseforge)):
            for forbidden in ("contents: write", "secrets.", "upload_curseforge.py",
                              "CURSEFORGE_API_TOKEN", "gh release", "gh api",
                              "--clobber", "FoundationsPL4-1.21.1-"):
                with self.subTest(name=name, forbidden=forbidden):
                    self.assertNotIn(forbidden, body)


if __name__ == "__main__":
    unittest.main()
