"""Fail-closed Forge 1.16.1 branch publisher policy regression tests."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WF = ROOT / ".github" / "workflows"


class PublisherSafety1161(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.native = (WF / "forge-native.yml").read_text(encoding="utf-8")
        cls.release = (WF / "release.yml").read_text(encoding="utf-8")
        cls.cf = (WF / "curseforge.yml").read_text(encoding="utf-8")
        cls.status = json.loads((ROOT / "BUILD_STATUS.json").read_text(encoding="utf-8"))

    def test_exact_version_and_uncleared_dev(self):
        self.assertEqual(self.status["minecraft"], "1.16.1")
        self.assertEqual(self.status["release_channel"], "development")
        self.assertIs(self.status["rc_publication_cleared"], False)

    def test_native_only_exact_branch(self):
        self.assertIn("branches: ['mc/1.16.1']", self.native)
        self.assertIn("TARGET\" = '1.16.1'", self.native)

    def test_immutability_and_read_only(self):
        for token in ("contents: read", "checked-source.zip",
                      "SOURCE_COMMIT.txt", "SOURCE_ARCHIVE_SHA256SUMS.txt",
                      'test "$(git rev-parse HEAD)" = "$GITHUB_SHA"'):
            self.assertIn(token, self.native)

    def test_native_conversion_and_exact_193_checks(self):
        for token in ("clean build", "tools/java8/runtime.py native",
                      "report['total']==193", "report['passed']==193", "report['failed']==0"):
            self.assertIn(token, self.native)

    def test_pinned_launcher_production_smoke_retained(self):
        self.assertIn("tools/production_smoke.py", self.native)
        self.assertLess(self.native.index("runtime.py native"), self.native.index("production_smoke.py"))

    def test_retained_evidence(self):
        for token in ("JAR_SHA256SUMS.txt", "LOG_SHA256SUMS.txt",
                      "run-java8-production/crash-reports/", "retention-days: 14"):
            self.assertIn(token, self.native)

    def test_no_old_alignment_run(self):
        self.assertNotIn("tools/prepare_patch_port.py", self.native)
        self.assertNotIn("git commit", self.native)
        self.assertNotIn("git push", self.native)

    def test_policy_only_edits_do_not_trigger_native(self):
        self.assertIn("paths-ignore:", self.native)
        self.assertIn("'.github/workflows/**'", self.native)

    def test_generic_release_audit_only(self):
        self.assertIn("branches: ['mc/1.16.1']", self.release)
        self.assertNotIn("tags:", self.release)
        self.assertIn("unittest", self.release)

    def test_direct_curseforge_audit_only(self):
        self.assertIn("branches: ['mc/1.16.1']", self.cf)
        self.assertIn("unittest", self.cf)

    def test_forbidden_mutation_or_secret(self):
        for name, body in (("native", self.native), ("release", self.release), ("cf", self.cf)):
            for forbidden in ("contents: write", "secrets.", "GH_TOKEN",
                              "upload_curseforge.py", "gh release", "git tag ",
                              "CURSEFORGE_API_TOKEN", "FoundationsPL4-1.21.1-", "--clobber"):
                with self.subTest(name=name, forbidden=forbidden):
                    self.assertNotIn(forbidden, body)


if __name__ == "__main__":
    unittest.main()
