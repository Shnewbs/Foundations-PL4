"""Offline negative tests for the independent installed provider evidence gate.

These do not claim a native run, JAR compilation, or RC acceptance.
"""
import importlib.util
import inspect
from pathlib import Path
import tempfile
import unittest
from unittest import mock
import zipfile

MODULE = Path(__file__).with_name("run_provider_probe.py")
spec = importlib.util.spec_from_file_location("run_provider_probe", MODULE)
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)

EXPECTED = "a" * 64
NONCE = "b" * 32


def valid_proof():
    return {
        "schema": 2, "target": "1.21.1", "api_version": 1,
        "installed_pl4_sha256": EXPECTED, "run_nonce": NONCE,
        "replacement": {"first": 1, "later": 0, "stale_rows": 0},
        "registration_close": {"first": 1, "later": 0, "first_rows": 1},
        "self_close": {"calls": 1, "rows": 0, "healthy_calls": 1},
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
            ("self_close", "calls", 0),
            ("self_close", "rows", 1),
            ("self_close", "healthy_calls", 0),
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

    def test_native_log_is_exclusively_created(self):
        with tempfile.TemporaryDirectory() as temp:
            log = Path(temp) / "provider-server.log"
            with probe.open_immutable_log(log) as stream:
                stream.write("retained original")
            with self.assertRaises(FileExistsError):
                with probe.open_immutable_log(log):
                    pass
            self.assertEqual(log.read_text(), "retained original")

    def test_receipt_symlink_cannot_overwrite_external_file(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            external = root / "external.json"
            external.write_text("protected external content")
            link = root / "receipt.json"
            link.symlink_to(external)
            with self.assertRaises(ValueError):
                probe.write_immutable_receipt(link, {"status": "PASS"})
            self.assertEqual(external.read_text(), "protected external content")

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


    def test_preflight_rejects_tampering_before_native_execution(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for name in ["gradlew", "gradle/wrapper/gradle-wrapper.jar",
                         "gradle/wrapper/gradle-wrapper.properties", *probe.PROJECT]:
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(b"pinned test fixture")
            mods = root / "run/mods"
            mods.mkdir(parents=True)
            (mods / "PL4-1.21.1.jar").write_bytes(b"mocked binary")
            props = root / "run/server.properties"
            props.write_text("server-ip=127.0.0.1\nserver-port=25588\n"
                             "online-mode=false\nmax-players=1\n"
                             "enable-rcon=true\nrcon.port=25589\n")
            (root / "run/eula.txt").write_text("eula=true\n")
            protected = ["gradlew", "gradle/wrapper/gradle-wrapper.jar",
                         "gradle/wrapper/gradle-wrapper.properties", *probe.PROJECT]
            manifest = {
                "schema": 1, "target": "1.21.1", "run_nonce": NONCE,
                "installed_pl4_sha256": EXPECTED, "installed_pl4_name": "PL4-1.21.1.jar",
                "pl4_source_commit": "c" * 40, "rc_publication_cleared": False,
                "protected_files": {name: probe.digest(root / name) for name in protected},
            }
            with mock.patch.object(probe.binary_smoke, "validate_binary"):
                self.assertEqual(probe.validate_prepared_installation(root, manifest),
                                 (EXPECTED, NONCE))
                (root / "gradlew").write_bytes(b"tampered wrapper")
                with self.assertRaises(ValueError):
                    probe.validate_prepared_installation(root, manifest)
                (root / "gradlew").write_bytes(b"pinned test fixture")
                props.write_text(props.read_text().replace("online-mode=false",
                                                           "online-mode=true"))
                with self.assertRaises(ValueError):
                    probe.validate_prepared_installation(root, manifest)
                props.write_text(props.read_text().replace("online-mode=true",
                                                           "online-mode=false"))
                (mods / "unapproved.jar").write_bytes(b"extra mod")
                with self.assertRaises(ValueError):
                    probe.validate_prepared_installation(root, manifest)
                (mods / "unapproved.jar").unlink()
                manifest["protected_files"].pop("gradlew")
                with self.assertRaises(ValueError):
                    probe.validate_prepared_installation(root, manifest)

    def test_gradle_jar_is_checked_before_server_launch(self):
        source = inspect.getsource(probe.run)
        self.assertLess(source.index('"verifyProbeIsolation"'),
                        source.index('"runServer"'))
        self.assertLess(source.index("validate_prepared_installation("),
                        source.index("subprocess.Popen("))



if __name__ == "__main__":
    unittest.main()
