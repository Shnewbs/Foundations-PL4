"""Offline negative tests for the independent installed provider evidence gate.

These do not claim a native run, JAR compilation, or RC acceptance.
"""
import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile

MODULE = Path(__file__).with_name("run_provider_probe.py")
spec = importlib.util.spec_from_file_location("run_provider_probe", MODULE)
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)

EXPECTED = "a" * 64
NONCE = "b" * 32


def valid_proof():
    return {
        "schema": 1, "target": "1.21.1", "api_version": 1,
        "installed_pl4_sha256": EXPECTED, "run_nonce": NONCE,
        "replacement": {"first": 1, "later": 0, "stale_rows": 0},
        "registration_close": {"first": 1, "later": 0, "first_rows": 1},
        "exception_quarantine": {"failing": 1, "healthy": 1, "partial_rows": 0},
        "unloaded": {"callbacks": 0, "rows": 0,
                     "before_loaded": False, "after_loaded": False},
        "registration_leaks": 0, "rc_publication_cleared": False,
    }


class ProviderReceiptTests(unittest.TestCase):
    def test_exact_proof_passes(self):
        probe.validate_proof(valid_proof(), EXPECTED, NONCE)

    def test_unloaded_chunk_rejection_is_mandatory(self):
        for field, value in [("callbacks", 1), ("rows", 1),
                             ("before_loaded", True), ("after_loaded", True)]:
            with self.subTest(field=field):
                evidence = valid_proof()
                evidence["unloaded"][field] = value
                with self.assertRaises(ValueError):
                    probe.validate_proof(evidence, EXPECTED, NONCE)

    def test_provider_callback_and_partial_row_rejections(self):
        for section, key, value in [
            ("replacement", "later", 1),
            ("replacement", "stale_rows", 1),
            ("registration_close", "later", 1),
            ("exception_quarantine", "partial_rows", 1),
            ("exception_quarantine", "healthy", 0),
        ]:
            with self.subTest(section=section, key=key):
                evidence = valid_proof()
                evidence[section][key] = value
                with self.assertRaises(ValueError):
                    probe.validate_proof(evidence, EXPECTED, NONCE)

    def test_wrong_jar_and_unsafe_release_claim_rejected(self):
        with self.assertRaises(ValueError):
            probe.validate_proof(valid_proof(), "b" * 64, NONCE)
        evidence = valid_proof()
        evidence["rc_publication_cleared"] = True
        with self.assertRaises(ValueError):
            probe.validate_proof(evidence, EXPECTED, NONCE)

    def test_wrong_or_missing_run_nonce_rejected(self):
        for value in ("c" * 32, "", "invalid"):
            with self.subTest(value=value):
                with self.assertRaises(ValueError):
                    probe.validate_proof(valid_proof(), EXPECTED, value)
        evidence = valid_proof()
        del evidence["run_nonce"]
        with self.assertRaises(ValueError):
            probe.validate_proof(evidence, EXPECTED, NONCE)

    def test_existing_native_evidence_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            probe.require_fresh_run(root)
            for name in ("provider-server.log", "provider-proof.json", "provider-probe-receipt.json"):
                path = root / name
                path.write_text("retained native evidence")
                with self.assertRaises(ValueError):
                    probe.require_fresh_run(root)
                path.unlink()

    def test_receipt_is_immutable_and_reverification_idempotent(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "receipt.json"
            probe.write_immutable_receipt(path, {"status": "PENDING", "source": "pinned"})
            original = path.read_bytes()
            probe.write_immutable_receipt(path, {"status": "PENDING", "source": "pinned"})
            self.assertEqual(path.read_bytes(), original)
            with self.assertRaises(ValueError):
                probe.write_immutable_receipt(path, {"status": "PASS", "source": "different"})
            self.assertEqual(path.read_bytes(), original)

    def test_probe_jar_cannot_embed_pl4_implementation(self):
        with tempfile.TemporaryDirectory() as temp:
            jar = Path(temp) / "probe.jar"
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr("net/foundations/pl4providerprobe/InstalledProviderProbe.class", b"probe")
                archive.writestr("META-INF/neoforge.mods.toml",
                    'modId="pl4_provider_probe"\nmodId="foundations_pl4"\n'
                    'versionRange="[1.21.1]"\n')
            self.assertEqual(len(probe.verify_probe_jar(jar)), 64)
            with zipfile.ZipFile(jar, "a") as archive:
                archive.writestr("net/foundations/pl4/InfoProviders.class", b"forbidden")
            with self.assertRaises(ValueError):
                probe.verify_probe_jar(jar)


if __name__ == "__main__":
    unittest.main()
