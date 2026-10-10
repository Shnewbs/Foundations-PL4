"""Offline identity, maturity, backfill and checksum checks; no API token needed."""
import hashlib
from pathlib import Path
import tempfile
import unittest

from upload_curseforge import parse_tag, release_type
from curseforge_sync import latest_per_target, validate_checksum


class CurseForgePublishTests(unittest.TestCase):
    def test_exact_tag_targets(self):
        for tag, expected in {
            "v0.2a": ("1.21.1", "0.2a", "NeoForge"),
            "mc26.1.2-v0.2a": ("26.1.2", "0.2a", "NeoForge"),
            "mc26.3-v0.2a": ("26.3", "0.2a", "NeoForge"),
            "mc1.14.4-v0.2a-port.1": ("1.14.4", "0.2a-port.1", "Forge"),
            "mc1.16.1-v0.2a-port.1": ("1.16.1", "0.2a-port.1", "Forge"),
            "mc1.18-v0.2a-port.1": ("1.18", "0.2a-port.1", "Forge"),
            "mc1.18.1-v0.2a-port.1": ("1.18.1", "0.2a-port.1", "Forge"),
            "mc1.19.1-v0.2a-port.1": ("1.19.1", "0.2a-port.1", "Forge"),
            "mc1.19.3-v0.2a-port.1": ("1.19.3", "0.2a-port.1", "Forge"),
            "mc1.6.4-v0.2a-legacy-preview.3": ("1.6.4", "0.2a-legacy-preview.3", "Forge")
        }.items():
            with self.subTest(tag=tag):
                self.assertEqual(parse_tag(tag), expected)

    def test_invalid_tags_and_excluded_version(self):
        for tag in ("", "mc1.14.4", "v", "mc1.7.10-v0.2a",
                    "mc1.14.4-v0.2a/../bad", "../v1.0", "mc1.14.4-v0.2a!"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                parse_tag(tag)

    def test_release_types(self):
        for v in ("0.2a", "0.2a-port.1", "0.2a-legacy-preview.3", "0.0.2a.R1"):
            self.assertEqual(release_type(v), "alpha")
        for v in ("0.1b", "0.1b-beta", "1.0.0-rc1"):
            self.assertEqual(release_type(v), "beta")
        self.assertEqual(release_type("1.0.0"), "release")

    def test_latest_released_target_only(self):
        versions = [
            {"tagName": "v0.0.1a.R1", "publishedAt": "2026-09-28T01:00:00Z", "isDraft": False},
            {"tagName": "v0.2a", "publishedAt": "2026-10-09T01:00:00Z", "isDraft": False},
            {"tagName": "mc1.14.4-v0.2a-port.1", "publishedAt": "2026-10-10T01:00:00Z", "isDraft": False},
            {"tagName": "mc1.14.4-v0.2a-port.2", "publishedAt": "2026-10-12T01:00:00Z", "isDraft": True},
            {"tagName": "mc1.16.1-v0.2a-port.1", "publishedAt": "2026-10-10T00:00:00Z", "isDraft": False},
            {"tagName": "wrong-version", "publishedAt": "2026-10-13T01:00:00Z", "isDraft": False}
        ]
        self.assertEqual(latest_per_target(versions), [
            ("1.14.4", "mc1.14.4-v0.2a-port.1"),
            ("1.16.1", "mc1.16.1-v0.2a-port.1"),
            ("1.21.1", "v0.2a")
        ])

    def test_exact_jar_checksum(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            jar = folder / "FoundationsPL4-1.14.4-0.2a-port.1.jar"
            jar.write_bytes(b"exact verified native jar")
            expected = hashlib.sha256(jar.read_bytes()).hexdigest()
            sums = folder / "SHA256SUMS.txt"
            sums.write_text(f"{expected}  {jar.name}\n")
            self.assertEqual(validate_checksum(jar, sums), expected)
            jar.write_bytes(b"modified jar")
            with self.assertRaises(RuntimeError):
                validate_checksum(jar, sums)
            sums.write_text("not an actual release checksum\n")
            with self.assertRaises(RuntimeError):
                validate_checksum(jar, sums)


if __name__ == "__main__":
    unittest.main()
