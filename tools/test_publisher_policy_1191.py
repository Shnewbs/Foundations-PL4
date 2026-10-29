"""Release regression gate for the exact Forge 1.19.1 development branch."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WF = ROOT / ".github" / "workflows"


class PublisherPolicy1191(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.native = (WF / "forge1191-native.yml").read_text(encoding="utf-8")
        cls.release = (WF / "release.yml").read_text(encoding="utf-8")
        cls.cf = (WF / "curseforge.yml").read_text(encoding="utf-8")
        cls.status = json.loads((ROOT / "BUILD_STATUS.json").read_text(encoding="utf-8"))

    def test_exact_target_development_uncleared(self):
        self.assertEqual(self.status["minecraft"], "1.19.1")
        self.assertEqual(self.status["release_channel"], "development")
        self.assertIs(self.status["rc_publication_cleared"], False)

    def test_no_full_acceptance_or_publication_claim(self):
        self.assertFalse(self.status["release_published"])
        self.assertTrue(self.status["further_api_testing_required"])
        self.assertFalse(self.status["full_pl2_parity"])

    def test_audits_are_readonly(self):
        for body in (self.release, self.cf):
            self.assertIn("contents: read", body)
            self.assertNotIn("contents: write", body)

    def test_audits_pin_exact_branch(self):
        for body in (self.release, self.cf):
            self.assertIn("branches: ['mc/1.19.1']", body)
            self.assertIn("unittest", body)

    def test_no_release_or_curseforge_mutation_paths(self):
        for name, body in (("native", self.native), ("release", self.release), ("curseforge", self.cf)):
            for forbidden in ("contents: write", "GH_TOKEN", "secrets.", "CURSEFORGE_API_TOKEN",
                              "upload_curseforge.py", "gh release", "gh api",
                              "git push", "git tag ", "--clobber",
                              "FoundationsPL4-1.21.1-", "tags:"):
                with self.subTest(name=name, forbidden=forbidden):
                    self.assertNotIn(forbidden, body)

    def test_native_ref_and_source_provenance(self):
        self.assertIn("if: github.ref == 'refs/heads/mc/1.19.1'", self.native)
        self.assertIn("git rev-parse HEAD", self.native)
        self.assertIn("SOURCE_COMMIT.txt", self.native)
        self.assertIn("SOURCE_ARCHIVE_SHA256SUMS.txt", self.native)

    def test_native_forge42_java17_retained(self):
        self.assertIn("java-version: '17'", self.native)
        self.assertIn("clean build", self.native)
        self.assertIn("tools/run_1191_native_tests.py", self.native)

    def test_native_artifact_hashes_retained(self):
        for token in ("JAR_SHA256SUMS.txt", "LOG_SHA256SUMS.txt", "retention-days: 14", "build/libs/"):
            self.assertIn(token, self.native)

    def test_native_not_an_obsolete_source_transform(self):
        for forbidden in ("prepare_patch_port.py", "git commit", "set_version.py"):
            self.assertNotIn(forbidden, self.native)

    def test_no_direct_release_from_native(self):
        self.assertNotIn("release-publication", self.native)
        self.assertNotIn("Publish verified", self.native)


if __name__ == "__main__":
    unittest.main()
