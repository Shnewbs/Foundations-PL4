from pathlib import Path
import tempfile
import unittest
import zipfile

from direct_curseforge_upload import inspect


class DirectUploadValidationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.notes = self.root / "changes.md"
        self.notes.write_text("# Verified changes\n", encoding="utf-8")
        self.jar = self.root / "FoundationsPL4-1.21.1-0.97.0.rev1-beta.jar"

    def make_jar(self, metadata=None):
        if metadata is None:
            metadata = self.metadata("0.97.0.rev1-beta", "1.21.1")
        with zipfile.ZipFile(self.jar, "w") as archive:
            archive.writestr("META-INF/neoforge.mods.toml", metadata)
            archive.writestr("net/foundations/pl4/FoundationsPL4.class", b"class")

    @staticmethod
    def metadata(version, target):
        return (f'[[mods]]\nmodId="foundations_pl4"\nversion="{version}"\n'
                '[[dependencies.foundations_pl4]]\nmodId="neoforge"\nversionRange="[21,)"\n'
                f'[[dependencies.foundations_pl4]]\nmodId="minecraft"\nversionRange="[{target}]"\n')

    def test_accepts_matching_runtime_and_changelog(self):
        self.make_jar()
        self.assertEqual(inspect(self.jar, self.notes), ("1.21.1", "0.97.0.rev1-beta", "NeoForge"))

    def test_rejects_wrong_target_or_version(self):
        self.make_jar(self.metadata("0.2a", "1.21.1"))
        with self.assertRaisesRegex(ValueError, "release version"):
            inspect(self.jar, self.notes)

    def test_rejects_neighboring_patch_target(self):
        self.make_jar(self.metadata("0.97.0.rev1-beta", "1.21.10"))
        with self.assertRaisesRegex(ValueError, "target Minecraft"):
            inspect(self.jar, self.notes)

    def test_rejects_source_only_archive(self):
        self.jar = self.root / "FoundationsPL4-1.21.1-0.97.0.rev1-beta-sources.jar"
        self.make_jar()
        with self.assertRaisesRegex(ValueError, "runtime"):
            inspect(self.jar, self.notes)

    def test_rejects_empty_changelog(self):
        self.make_jar()
        self.notes.write_text("   ", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "changelog"):
            inspect(self.jar, self.notes)

    def test_rejects_unconverted_legacy_runtime(self):
        self.jar = self.root / "FoundationsPL4-1.16.5-0.2a-port.2.jar"
        with self.assertRaisesRegex(ValueError, "converted"):
            inspect(self.jar, self.notes)


if __name__ == "__main__":
    unittest.main()
