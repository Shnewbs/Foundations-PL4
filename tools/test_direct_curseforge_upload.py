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

    def make_jar(self, metadata=None, class_bytes=None, metadata_path="META-INF/neoforge.mods.toml"):
        if metadata is None:
            metadata = self.metadata("0.97.0.rev1-beta", "1.21.1")
        if class_bytes is None:
            class_bytes = bytes.fromhex("cafebabe000000410001")  # Java 21 classfile header
        with zipfile.ZipFile(self.jar, "w") as archive:
            archive.writestr(metadata_path, metadata)
            archive.writestr("net/foundations/pl4/FoundationsPL4.class", class_bytes)

    def make_receipt(self, **overrides):
        receipt = {
            "project_id": "1716767", "file_id": 12345,
            "minecraft": "1.21.1", "version": "0.97.0.rev1-beta",
            "loader": "NeoForge",
            "sha256": hashlib.sha256(self.jar.read_bytes()).hexdigest(),
            "upload_status": "accepted_by_api_not_moderation_verified",
        }
        receipt.update(overrides)
        self.jar.with_suffix(".curseforge.json").write_text(
            json.dumps(receipt), encoding="utf-8")
        return receipt

    @staticmethod
    def metadata(version, target):
        return (f'[[mods]]\nmodId="foundations_pl4"\nversion="{version}"\n'
                '[[dependencies.foundations_pl4]]\nmodId="neoforge"\nversionRange="[21,)"\n'
                f'[[dependencies.foundations_pl4]]\nmodId="minecraft"\nversionRange="[{target}]"\n')

    def test_accepts_matching_runtime_and_changelog(self):
        self.make_jar()
        self.assertEqual(inspect(self.jar, self.notes), ("1.21.1", "0.97.0.rev1-beta", "NeoForge"))


    def test_valid_receipt_prevents_duplicate_upload_without_claiming_moderation(self):
        self.make_jar()
        self.make_receipt()
        original = Path.cwd()
        try:
            os.chdir(self.root)
            with patch.object(sys, "argv", [
                    "direct_curseforge_upload.py", str(self.jar), str(self.notes), "--upload"]), \
                 patch.object(direct, "request_json") as api, \
                 patch.object(direct, "multipart") as multipart:
                direct.main()
                api.assert_not_called()
                multipart.assert_not_called()
        finally:
            os.chdir(original)

    def test_multipart_uses_inspected_bytes_even_if_disk_jar_changes(self):
        self.make_jar()
        original = self.jar.read_bytes()
        self.jar.write_bytes(b"replacement is not the inspected runtime")
        with patch.object(Path, "read_bytes", side_effect=AssertionError("JAR reread")):
            body, content_type = direct.multipart(
                {"changelog": "verified"}, self.jar, jar_bytes=original)
        self.assertIn(original, body)
        self.assertNotIn(b"replacement is not the inspected runtime", body)
        self.assertIn("multipart/form-data", content_type)

    def test_upload_receipt_hash_matches_bytes_sent_after_disk_replacement(self):
        self.make_jar()
        original = self.jar.read_bytes()
        original_inspect = direct.inspect

        def replace_disk_after_inspection(jar, notes, jar_bytes=None):
            outcome = original_inspect(jar, notes, jar_bytes=jar_bytes)
            self.jar.write_bytes(b"unverified replacement")
            return outcome

        old_cwd = Path.cwd()
        try:
            os.chdir(self.root)
            with patch.object(sys, "argv", [
                    "direct_curseforge_upload.py", str(self.jar), str(self.notes), "--upload"]), \
                 patch.dict(os.environ, {"CURSEFORGE_API_TOKEN": "fixture-token"}), \
                 patch.object(direct, "inspect", side_effect=replace_disk_after_inspection), \
                 patch.object(direct, "request_json", return_value={"id": 54321}) as api:
                direct.main()
                api.assert_called_once()
                sent_body = api.call_args.args[2]
                self.assertIn(original, sent_body)
                self.assertNotIn(b"unverified replacement", sent_body)
        finally:
            os.chdir(old_cwd)
        receipt = json.loads(self.jar.with_suffix(".curseforge.json").read_text(encoding="utf-8"))
        self.assertEqual(receipt["sha256"], hashlib.sha256(original).hexdigest())
        self.assertEqual(receipt["file_id"], 54321)

    def test_rejects_missing_or_invalid_receipt_file_ids(self):
        self.make_jar()
        for file_id in (None, True, False, "12345", 0, -1, [], {}):
            with self.subTest(file_id=file_id):
                self.make_receipt(file_id=file_id)
                with self.assertRaisesRegex(ValueError, "valid file ID"):
                    self.dry_run()

    def test_rejects_receipt_without_api_acceptance_or_matching_artifact(self):
        self.make_jar()
        for status in (None, "", "published", "pending"):
            with self.subTest(status=status):
                self.make_receipt(upload_status=status)
                with self.assertRaisesRegex(ValueError, "API-acceptance status"):
                    self.dry_run()
        self.make_receipt(sha256="0" * 64)
        with self.assertRaisesRegex(ValueError, "differs from the JAR"):
            self.dry_run()

    def test_rejects_too_new_bundled_class_outside_pl4_package(self):
        self.make_jar()
        with zipfile.ZipFile(self.jar, "a") as archive:
            archive.writestr("org/example/BundledHelper.class",
                             bytes.fromhex("cafebabe000000450001"))  # Java 25
        with self.assertRaisesRegex(ValueError, "incompatible PL4 bytecode"):
            inspect(self.jar, self.notes)

    def test_accepts_compatible_bundled_class(self):
        self.make_jar()
        with zipfile.ZipFile(self.jar, "a") as archive:
            archive.writestr("org/example/BundledHelper.class",
                             bytes.fromhex("cafebabe000000410001"))  # Java 21
        self.assertEqual(inspect(self.jar, self.notes),
                         ("1.21.1", "0.97.0.rev1-beta", "NeoForge"))

    def test_rejects_too_new_active_multi_release_class(self):
        self.make_jar()
        with zipfile.ZipFile(self.jar, "a") as archive:
            archive.writestr("META-INF/versions/21/org/example/Helper.class",
                             bytes.fromhex("cafebabe000000450001"))  # Java 25
        with self.assertRaisesRegex(ValueError, "incompatible PL4 bytecode"):
            inspect(self.jar, self.notes)

    def test_ignores_multi_release_class_above_target_java(self):
        self.make_jar()
        with zipfile.ZipFile(self.jar, "a") as archive:
            archive.writestr("META-INF/versions/22/org/example/Helper.class",
                             bytes.fromhex("cafebabe000000450001"))  # Java 25
        self.assertEqual(inspect(self.jar, self.notes),
                         ("1.21.1", "0.97.0.rev1-beta", "NeoForge"))

    def test_legacy_java8_ignores_java9_multi_release_class(self):
        self.jar = self.root / "FoundationsPL4-1.16.5-0.97.0.rev1-beta-java8.jar"
        metadata = self.metadata("0.97.0.rev1-beta", "1.16.5").replace(
            'modId="neoforge"', 'modId="forge"')
        self.make_jar(metadata, class_bytes=bytes.fromhex("cafebabe000000340001"),
                      metadata_path="META-INF/mods.toml")
        with zipfile.ZipFile(self.jar, "a") as archive:
            archive.writestr("META-INF/versions/9/org/example/Helper.class",
                             bytes.fromhex("cafebabe000000350001"))  # Java 9
        self.assertEqual(inspect(self.jar, self.notes),
                         ("1.16.5", "0.97.0.rev1-beta", "Forge"))

    def test_legacy_java8_ignores_root_java9_module_descriptor(self):
        self.jar = self.root / "FoundationsPL4-1.16.5-0.97.0.rev1-beta-java8.jar"
        metadata = self.metadata("0.97.0.rev1-beta", "1.16.5").replace(
            'modId="neoforge"', 'modId="forge"')
        self.make_jar(metadata, class_bytes=bytes.fromhex("cafebabe000000340001"),
                      metadata_path="META-INF/mods.toml")
        with zipfile.ZipFile(self.jar, "a") as archive:
            archive.writestr("module-info.class",
                             bytes.fromhex("cafebabe000000350001"))  # Java 9
        self.assertEqual(inspect(self.jar, self.notes),
                         ("1.16.5", "0.97.0.rev1-beta", "Forge"))

    def test_rejects_malformed_multi_release_class_directory(self):
        self.make_jar()
        with zipfile.ZipFile(self.jar, "a") as archive:
            archive.writestr("META-INF/versions/future/org/example/Helper.class",
                             bytes.fromhex("cafebabe000000410001"))
        with self.assertRaisesRegex(ValueError, "invalid multi-release class path"):
            inspect(self.jar, self.notes)

    def test_rejects_fake_pl4_classfile(self):
        self.make_jar(class_bytes=b"class")
        with self.assertRaisesRegex(ValueError, "invalid PL4 classfile header"):
            inspect(self.jar, self.notes)

    def test_rejects_java25_classfile_for_java21_target(self):
        self.make_jar(class_bytes=bytes.fromhex("cafebabe000000450001"))
        with self.assertRaisesRegex(ValueError, "incompatible PL4 bytecode"):
            inspect(self.jar, self.notes)

    def test_rejects_preview_classfile(self):
        self.make_jar(class_bytes=bytes.fromhex("cafebabeffff00410001"))
        with self.assertRaisesRegex(ValueError, "incompatible PL4 bytecode"):
            inspect(self.jar, self.notes)

    def test_rejects_zero_constant_pool(self):
        self.make_jar(class_bytes=bytes.fromhex("cafebabe000000410000"))
        with self.assertRaisesRegex(ValueError, "invalid PL4 classfile header"):
            inspect(self.jar, self.notes)

    def test_accepts_java25_classfile_for_26_3(self):
        self.jar = self.root / "FoundationsPL4-26.3-0.97.0.rev1-beta.jar"
        self.make_jar(self.metadata("0.97.0.rev1-beta", "26.3"),
                      class_bytes=bytes.fromhex("cafebabe000000450001"))
        self.assertEqual(inspect(self.jar, self.notes),
                         ("26.3", "0.97.0.rev1-beta", "NeoForge"))

    def test_accepts_approved_26_1_2_neoforge_target(self):
        self.jar = self.root / "FoundationsPL4-26.1.2-0.97.0.rev1-beta.jar"
        self.make_jar(self.metadata("0.97.0.rev1-beta", "26.1.2"),
                      class_bytes=bytes.fromhex("cafebabe000000450001"))
        self.assertEqual(inspect(self.jar, self.notes),
                         ("26.1.2", "0.97.0.rev1-beta", "NeoForge"))

    def test_rejects_audit_only_and_unreleased_26x_targets(self):
        for target in ("26.1", "26.2", "26.4", "26.4.1"):
            with self.subTest(target=target):
                self.jar = self.root / f"FoundationsPL4-{target}-0.97.0.rev1-beta.jar"
                self.make_jar(self.metadata("0.97.0.rev1-beta", target),
                              class_bytes=bytes.fromhex("cafebabe000000450001"))
                with self.assertRaisesRegex(ValueError, "not an approved exact NeoForge release target"):
                    inspect(self.jar, self.notes)

    def test_rejects_nonledger_1x_targets_before_publication(self):
        # A correctly named, internally self-consistent JAR is not enough:
        # an exact version must also belong to the approved release ledger.
        for target in ("1.13.2", "1.18.3", "1.20.3", "1.21.2"):
            with self.subTest(target=target):
                self.jar = self.root / f"FoundationsPL4-{target}-0.97.0.rev1-beta.jar"
                self.make_jar(self.metadata("0.97.0.rev1-beta", target))
                with self.assertRaisesRegex(ValueError, "not an approved direct-upload release target"):
                    self.dry_run()

    def test_accepts_published_forge_1_20_4_direct_target(self):
        self.jar = self.root / "FoundationsPL4-1.20.4-0.97.0.rev1-beta.jar"
        metadata = self.metadata("0.97.0.rev1-beta", "1.20.4").replace(
            'modId="neoforge"', 'modId="forge"')
        self.make_jar(metadata, class_bytes=bytes.fromhex("cafebabe0000003d0001"),
                      metadata_path="META-INF/mods.toml")
        self.assertEqual(inspect(self.jar, self.notes),
                         ("1.20.4", "0.97.0.rev1-beta", "Forge"))

    def test_dry_run_rejects_26_4_before_publication(self):
        self.jar = self.root / "FoundationsPL4-26.4-0.97.0.rev1-beta.jar"
        self.make_jar(self.metadata("0.97.0.rev1-beta", "26.4"),
                      class_bytes=bytes.fromhex("cafebabe000000450001"))
        with self.assertRaisesRegex(ValueError, "not an approved exact NeoForge release target"):
            self.dry_run()

    def test_rejects_java21_classfile_for_java17_forge(self):
        self.jar = self.root / "FoundationsPL4-1.20.1-0.97.0.rev1-beta.jar"
        metadata = self.metadata("0.97.0.rev1-beta", "1.20.1").replace(
            'modId="neoforge"', 'modId="forge"')
        self.make_jar(metadata, metadata_path="META-INF/mods.toml")
        with self.assertRaisesRegex(ValueError, "incompatible PL4 bytecode"):
            inspect(self.jar, self.notes)

    def test_accepts_converted_java8_for_1_16_5(self):
        self.jar = self.root / "FoundationsPL4-1.16.5-0.97.0.rev1-beta-java8.jar"
        metadata = self.metadata("0.97.0.rev1-beta", "1.16.5").replace(
            'modId="neoforge"', 'modId="forge"')
        self.make_jar(metadata, class_bytes=bytes.fromhex("cafebabe000000340001"),
                      metadata_path="META-INF/mods.toml")
        self.assertEqual(inspect(self.jar, self.notes),
                         ("1.16.5", "0.97.0.rev1-beta", "Forge"))

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

    def test_rejects_oversized_compressed_jar_snapshot_before_upload(self):
        self.make_jar()
        with patch.object(direct, "MAX_JAR_COMPRESSED_BYTES", self.jar.stat().st_size - 1):
            with self.assertRaisesRegex(ValueError, "compressed archive size limit"):
                self.dry_run()

    def test_accepts_compressed_jar_snapshot_at_exact_limit(self):
        self.make_jar()
        with patch.object(direct, "MAX_JAR_COMPRESSED_BYTES", self.jar.stat().st_size):
            self.dry_run()

    def test_jar_snapshot_remains_immutable_after_disk_replacement(self):
        self.make_jar()
        original = self.jar.read_bytes()
        with patch.object(direct, "MAX_JAR_COMPRESSED_BYTES", len(original)):
            snapshot = direct.read_jar_snapshot(self.jar)
        self.jar.write_bytes(b"later replacement")
        self.assertEqual(snapshot, original)
        self.assertNotEqual(snapshot, self.jar.read_bytes())

    def test_rejects_archive_entry_count_limit(self):
        self.make_jar()
        with patch.object(direct, "MAX_JAR_ENTRIES", 1):
            with self.assertRaisesRegex(ValueError, "archive entry count limit"):
                inspect(self.jar, self.notes)

    def test_rejects_archive_total_uncompressed_size_limit(self):
        self.make_jar()
        with patch.object(direct, "MAX_JAR_UNCOMPRESSED_BYTES", 64):
            with self.assertRaisesRegex(ValueError, "total uncompressed size limit"):
                inspect(self.jar, self.notes)

    def test_rejects_archive_member_size_limit(self):
        self.make_jar()
        with patch.object(direct, "MAX_JAR_MEMBER_BYTES", 64):
            with self.assertRaisesRegex(ValueError, "member size limit"):
                inspect(self.jar, self.notes)

    def test_rejects_excessive_compression_ratio(self):
        self.make_jar()
        with zipfile.ZipFile(self.jar, "a") as archive:
            archive.writestr("assets/pl4/repeated.txt", b"A" * (2 * 1024 * 1024),
                             compress_type=zipfile.ZIP_DEFLATED)
        with patch.object(direct, "MAX_JAR_COMPRESSION_RATIO", 50):
            with self.assertRaisesRegex(ValueError, "compression ratio limit"):
                inspect(self.jar, self.notes)

    def test_rejects_windows_drive_and_ambiguous_archive_paths(self):
        for path in ("C:/outside.txt", "assets//pl4/texture.png"):
            with self.subTest(path=path):
                self.make_jar()
                with zipfile.ZipFile(self.jar, "a") as archive:
                    archive.writestr(path, b"unsafe")
                with self.assertRaisesRegex(ValueError, "unsafe archive entry path"):
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


    def test_receipt_is_created_exclusively_with_private_permissions(self):
        receipt = self.jar.with_suffix(".curseforge.json")
        direct.write_receipt_exclusive(receipt, {"file_id": 54321})
        self.assertEqual(json.loads(receipt.read_text(encoding="utf-8")),
                         {"file_id": 54321})
        if os.name == "posix":
            self.assertEqual(stat.S_IMODE(receipt.stat().st_mode), 0o600)
        with self.assertRaises(FileExistsError):
            direct.write_receipt_exclusive(receipt, {"file_id": 99999})
        self.assertEqual(json.loads(receipt.read_text(encoding="utf-8"))["file_id"],
                         54321)

    def test_rejects_dangling_symlink_receipt_before_upload(self):
        self.make_jar()
        receipt = self.jar.with_suffix(".curseforge.json")
        try:
            receipt.symlink_to(self.root / "nonexistent-receipt")
        except (OSError, NotImplementedError):
            self.skipTest("symlinks unavailable on this platform")
        with self.assertRaisesRegex(ValueError, "symbolic-link"):
            self.dry_run()

    def test_post_upload_receipt_race_preserves_existing_evidence(self):
        self.make_jar()
        receipt = self.jar.with_suffix(".curseforge.json")
        original_cwd = Path.cwd()

        def create_conflicting_receipt(*_args, **_kwargs):
            receipt.write_text("another writer's receipt", encoding="utf-8")
            return {"id": 54321}

        try:
            os.chdir(self.root)
            with patch.object(sys, "argv", [
                    "direct_curseforge_upload.py", str(self.jar),
                    str(self.notes), "--upload"]), \
                 patch.dict(os.environ, {"CURSEFORGE_API_TOKEN": "fixture-token"}), \
                 patch.object(direct, "request_json",
                              side_effect=create_conflicting_receipt) as api:
                with self.assertRaisesRegex(RuntimeError, "accepted file 54321"):
                    direct.main()
                api.assert_called_once()
        finally:
            os.chdir(original_cwd)
        self.assertEqual(receipt.read_text(encoding="utf-8"),
                         "another writer's receipt")

    def test_successful_upload_creates_api_receipt_without_replacement(self):
        self.make_jar()
        original_cwd = Path.cwd()
        try:
            os.chdir(self.root)
            with patch.object(sys, "argv", [
                    "direct_curseforge_upload.py", str(self.jar),
                    str(self.notes), "--upload"]), \
                 patch.dict(os.environ, {"CURSEFORGE_API_TOKEN": "fixture-token"}), \
                 patch.object(direct, "request_json",
                              return_value={"id": 54321}) as api:
                direct.main()
                api.assert_called_once()
        finally:
            os.chdir(original_cwd)
        receipt = self.jar.with_suffix(".curseforge.json")
        self.assertEqual(json.loads(receipt.read_text(encoding="utf-8"))["file_id"],
                         54321)
        self.assertEqual(json.loads(receipt.read_text(encoding="utf-8"))["sha256"],
                         hashlib.sha256(self.jar.read_bytes()).hexdigest())


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


    def test_rejects_duplicate_pl4_mod_table(self):
        metadata = self.metadata("0.97.0.rev1-beta", "1.21.1")
        metadata += '[[mods]]\nmodId="foundations_pl4"\nversion="wrong"\n'
        self.make_jar(metadata)
        with self.assertRaisesRegex(ValueError, "duplicated"):
            inspect(self.jar, self.notes)

    def test_rejects_duplicate_minecraft_dependency(self):
        metadata = self.metadata("0.97.0.rev1-beta", "1.21.1")
        metadata += '[[dependencies.foundations_pl4]]\nmodId="minecraft"\nversionRange="[26.3]"\n'
        self.make_jar(metadata)
        with self.assertRaisesRegex(ValueError, "exactly one Minecraft"):
            inspect(self.jar, self.notes)

    def test_rejects_duplicate_loader_dependency(self):
        metadata = self.metadata("0.97.0.rev1-beta", "1.21.1")
        metadata += '[[dependencies.foundations_pl4]]\nmodId="neoforge"\nversionRange="[26,)"\n'
        self.make_jar(metadata)
        with self.assertRaisesRegex(ValueError, "exactly one Minecraft and loader"):
            inspect(self.jar, self.notes)

    def test_rejects_conflicting_loader_dependency(self):
        metadata = self.metadata("0.97.0.rev1-beta", "1.21.1")
        metadata += '[[dependencies.foundations_pl4]]\nmodId="forge"\nversionRange="[47,)"\n'
        self.make_jar(metadata)
        with self.assertRaisesRegex(ValueError, "conflicting loaders"):
            inspect(self.jar, self.notes)

    def test_rejects_invalid_dependency_shape(self):
        metadata = ('[[mods]]\nmodId="foundations_pl4"\nversion="0.97.0.rev1-beta"\n'
                    '[dependencies]\nfoundations_pl4="not a dependency list"\n')
        self.make_jar(metadata)
        with self.assertRaisesRegex(ValueError, "invalid dependency metadata"):
            inspect(self.jar, self.notes)


if __name__ == "__main__":
    unittest.main()
