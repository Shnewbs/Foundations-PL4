from pathlib import Path
import hashlib
import json
import os
import stat
import warnings
import sys
import tempfile
import unittest
import zipfile
from unittest.mock import patch

import direct_curseforge_upload as direct
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

    def test_rejects_duplicate_jar_metadata(self):
        self.make_jar()
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(self.jar, "a") as archive:
                archive.writestr("META-INF/neoforge.mods.toml",
                                 self.metadata("0.97.0.rev1-beta", "1.21.1"))
        with self.assertRaisesRegex(ValueError, "duplicate archive entry"):
            inspect(self.jar, self.notes)

    def test_rejects_unsafe_jar_entry_path(self):
        self.make_jar()
        with zipfile.ZipFile(self.jar, "a") as archive:
            archive.writestr("../outside.txt", "not a runtime entry")
        with self.assertRaisesRegex(ValueError, "unsafe archive entry path"):
            inspect(self.jar, self.notes)

    def test_rejects_jar_symlink(self):
        self.make_jar()
        link = zipfile.ZipInfo("net/foundations/pl4/alias.class")
        link.create_system = 3
        link.external_attr = (stat.S_IFLNK | 0o777) << 16
        with zipfile.ZipFile(self.jar, "a") as archive:
            archive.writestr(link, "FoundationsPL4.class")
        with self.assertRaisesRegex(ValueError, "symbolic-link archive entry"):
            inspect(self.jar, self.notes)

    def test_rejects_corrupt_class_crc(self):
        self.make_jar()
        with zipfile.ZipFile(self.jar) as archive:
            entry = archive.getinfo("net/foundations/pl4/FoundationsPL4.class")
            payload_offset = (entry.header_offset + 30
                              + len(entry.filename.encode("utf-8")) + len(entry.extra))
        data = bytearray(self.jar.read_bytes())
        data[payload_offset] ^= 0x01
        self.jar.write_bytes(data)
        with self.assertRaisesRegex(ValueError, "corrupt archive entry"):
            inspect(self.jar, self.notes)

    def test_rejects_unconverted_legacy_runtime(self):
        self.jar = self.root / "FoundationsPL4-1.16.5-0.2a-port.2.jar"
        with self.assertRaisesRegex(ValueError, "converted"):
            inspect(self.jar, self.notes)


    def dry_run(self):
        original = Path.cwd()
        try:
            os.chdir(self.root)
            with patch.object(sys, "argv", ["direct_curseforge_upload.py", str(self.jar), str(self.notes)]), \
                 patch.object(direct, "request_json") as api:
                try:
                    direct.main()
                finally:
                    api.assert_not_called()
        finally:
            os.chdir(original)

    def test_rejects_neoforge_minecraft_version_range(self):
        metadata = self.metadata("0.97.0.rev1-beta", "1.21.1")
        self.make_jar(metadata.replace('versionRange="[1.21.1]"',
                                       'versionRange="[1.21.1,1.22)"'))
        with self.assertRaisesRegex(ValueError, "exact Minecraft target"):
            inspect(self.jar, self.notes)

    def test_rejects_development_even_with_matching_receipt(self):
        self.jar = self.root / "FoundationsPL4-1.21.1-0.2.0-rc.1-dev.jar"
        self.make_jar(self.metadata("0.2.0-rc.1-dev", "1.21.1"))
        digest = hashlib.sha256(self.jar.read_bytes()).hexdigest()
        self.jar.with_suffix(".curseforge.json").write_text(json.dumps({
            "project_id": "1716767", "minecraft": "1.21.1",
            "version": "0.2.0-rc.1-dev", "loader": "NeoForge",
            "sha256": digest, "file_id": 123
        }))
        with self.assertRaisesRegex(ValueError, "Development snapshots"):
            self.dry_run()

    def test_rc_requires_exact_target_status_and_clearance(self):
        self.jar = self.root / "FoundationsPL4-1.21.1-0.2.0-rc.1.jar"
        self.make_jar(self.metadata("0.2.0-rc.1", "1.21.1"))
        with self.assertRaisesRegex(ValueError, "BUILD_STATUS.json"):
            self.dry_run()
        status = {"version": "0.2.0-rc.1", "minecraft": "1.21.1",
                  "release_channel": "beta", "rc_publication_cleared": False}
        (self.root / "BUILD_STATUS.json").write_text(json.dumps(status))
        with self.assertRaisesRegex(ValueError, "not been explicitly cleared"):
            self.dry_run()
        status["rc_publication_cleared"] = True
        status["minecraft"] = "26.3"
        (self.root / "BUILD_STATUS.json").write_text(json.dumps(status))
        with self.assertRaisesRegex(ValueError, "exact Minecraft target disagree"):
            self.dry_run()
        status["minecraft"] = "1.21.1"
        (self.root / "BUILD_STATUS.json").write_text(json.dumps(status))
        self.dry_run()


if __name__ == "__main__":
    unittest.main()
