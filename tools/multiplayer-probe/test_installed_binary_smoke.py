"""Negative policy tests only; synthetic marker must never be called a real CI startup."""
import hashlib
import json
from pathlib import Path
import struct
from unittest.mock import patch
import tempfile
import unittest
import zipfile

import installed_binary_smoke as gate


class InstalledBinaryIsolationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.checkout = self.root / "checkout"
        wrapper = self.checkout / "gradle/wrapper"
        wrapper.mkdir(parents=True)
        (self.checkout / "gradlew").write_text("#!/bin/sh\necho stub\n")
        (wrapper / "gradle-wrapper.jar").write_bytes(b"test-only-wrapper")
        (wrapper / "gradle-wrapper.properties").write_text("distributionUrl=local-fixture\n")
        self.jar = self.root / "FoundationsPL4-1.21.1-0.2b.0.jar"
        self.write_jar()
        self.sha = gate.digest(self.jar)
        self.target = self.root / "isolated"
        self.log = self.target / "installed-server.log"

    def tearDown(self):
        self.temp.cleanup()

    def write_jar(self, metadata=None):
        if metadata is None:
            metadata = 'modId="foundations_pl4"\nversionRange="[1.21.1]"\n'
        with zipfile.ZipFile(self.jar, "w") as z:
            z.writestr("META-INF/neoforge.mods.toml", metadata)
            z.writestr("net/foundations/pl4/FoundationsPL4.class", b"production-only-fixture")

    def prepare(self):
        return gate.prepare(self.checkout, self.jar, self.target, self.sha)

    def pretend_started(self):
        """Simulates a proof to test verifier policy, never real native qualification."""
        self.log.write_text("Server startup passed\n" + gate.MARKER + self.sha + "\n")
        (self.target / "installed-server-proof.json").write_text(json.dumps({
            "schema": 1, "loaded_mod_id": "foundations_pl4",
            "server_started": True, "expected_jar_sha256": self.sha
        }))

    def test_only_the_probe_compiles_and_installed_jar_is_pinned(self):
        m = self.prepare()
        self.assertFalse(m["runtime_server_executed"])
        self.assertFalse(m["rc_publication_cleared"])
        sources = list((self.target / "src/main/java").rglob("*.java"))
        self.assertEqual(len(sources), 1)
        self.assertEqual(sources[0].relative_to(self.target).as_posix(), gate.JAVA_REL)
        self.assertNotIn("net.foundations.pl4;", sources[0].read_text())
        self.assertEqual(gate.digest(self.target / "run/mods" / self.jar.name), self.sha)

    def test_synthetic_policy_contract_never_grants_upgrade_clearance(self):
        self.prepare()
        self.pretend_started()
        proof = gate.verify(self.target, self.log)
        self.assertEqual(proof["installed_binary_server_smoke"], "PASS")
        self.assertIs(proof["released_to_candidate_world_upgrade_accepted"], False)
        self.assertIs(proof["installed_binary_client_executed"], False)
        self.assertIs(proof["rc_publication_cleared"], False)

    def test_missing_native_runtime_marker_fails(self):
        self.prepare()
        self.pretend_started()
        self.log.write_text("server started but the PL4 mod was not confirmed\n")
        with self.assertRaises(ValueError): gate.verify(self.target, self.log)

    def test_duplicate_native_runtime_marker_fails(self):
        self.prepare()
        self.pretend_started()
        self.log.write_text((gate.MARKER + self.sha + "\n") * 2)
        with self.assertRaises(ValueError): gate.verify(self.target, self.log)

    def test_swapped_installed_jar_fails(self):
        self.prepare()
        self.pretend_started()
        with (self.target / "run/mods" / self.jar.name).open("ab") as f:
            f.write(b"forged")
        with self.assertRaises(ValueError): gate.verify(self.target, self.log)

    def test_another_mod_jar_fails_closed(self):
        self.prepare()
        self.pretend_started()
        (self.target / "run/mods/another.jar").write_bytes(b"unexpected")
        with self.assertRaises(ValueError): gate.verify(self.target, self.log)

    def test_source_injection_fails_closed(self):
        self.prepare()
        self.pretend_started()
        rogue = self.target / "src/main/java/net/foundations/pl4/FoundationsPL4.java"
        rogue.parent.mkdir(parents=True)
        rogue.write_text("package net.foundations.pl4;")
        with self.assertRaises(ValueError): gate.verify(self.target, self.log)

    def test_post_prepare_gradle_change_fails_closed(self):
        self.prepare()
        self.pretend_started()
        with (self.target / "build.gradle").open("a") as f:
            f.write("\n// unapproved runtime modification\n")
        with self.assertRaises(ValueError): gate.verify(self.target, self.log)

    def test_forged_missing_server_event_fails(self):
        self.prepare()
        self.pretend_started()
        (self.target / "installed-server-proof.json").write_text("{}")
        with self.assertRaises(ValueError): gate.verify(self.target, self.log)

    def test_bad_target_metadata_fails_even_with_matching_sha(self):
        self.write_jar(metadata='modId="foundations_pl4"\nversionRange="[1.20.4]"\n')
        self.sha = gate.digest(self.jar)
        with self.assertRaises(ValueError): self.prepare()

    def test_vanilla_server_properties_rewrite_is_not_false_source_tamper(self):
        self.prepare()
        self.pretend_started()
        with (self.target / "run/server.properties").open("a") as f:
            f.write("motd=A disposable server\\n")
        result = gate.verify(self.target, self.log)
        self.assertEqual(result["installed_binary_server_smoke"], "PASS")

    def test_runtime_cannot_escape_loopback(self):
        self.prepare()
        self.pretend_started()
        config = self.target / "run/server.properties"
        config.write_text(config.read_text().replace("server-ip=127.0.0.1", "server-ip=0.0.0.0"))
        with self.assertRaises(ValueError): gate.verify(self.target, self.log)

    def test_overwriting_existing_runtime_fails(self):
        self.prepare()
        with self.assertRaises(FileExistsError): self.prepare()

    def test_rcon_shutdown_requires_ephemeral_authenticated_loopback(self):
        self.prepare()
        config = self.target / "run/server.properties"
        self.assertIn("enable-rcon=true", config.read_text())
        self.assertIn("rcon.port=25579", config.read_text())
        config.write_text(config.read_text().replace("server-ip=127.0.0.1", "server-ip=0.0.0.0"))
        with self.assertRaises(ValueError):
            gate.stop_server_rcon(config)

    def test_rcon_authenticated_stop_sends_command_only_after_auth(self):
        self.prepare()
        def packet(request_id, packet_type):
            body = struct.pack("<ii", request_id, packet_type) + b"\0\0"
            return struct.pack("<i", len(body)) + body
        class FakeSocket:
            def __init__(self):
                self.reply = packet(194, 0) + packet(194, 2)
                self.sent = []
            def __enter__(self): return self
            def __exit__(self, *unused): return False
            def settimeout(self, unused): pass
            def recv(self, size):
                chunk, self.reply = self.reply[:size], self.reply[size:]
                return chunk
            def sendall(self, data): self.sent.append(data)
        fake = FakeSocket()
        with patch.object(gate.socket, "create_connection", return_value=fake) as connect:
            gate.stop_server_rcon(self.target / "run/server.properties")
        connect.assert_called_once_with(("127.0.0.1", 25579), timeout=5)
        self.assertEqual(len(fake.sent), 2)
        self.assertIn(b"stop\0\0", fake.sent[1])
        self.assertIn(struct.pack("<ii", 195, 2), fake.sent[1])

    def test_rcon_authentication_failure_never_sends_stop(self):
        self.prepare()
        class FailedAuthSocket:
            def __init__(self):
                body = struct.pack("<ii", -1, 2) + b"\0\0"
                self.reply = struct.pack("<i", len(body)) + body
                self.sent = []
            def __enter__(self): return self
            def __exit__(self, *unused): return False
            def settimeout(self, unused): pass
            def recv(self, size):
                chunk, self.reply = self.reply[:size], self.reply[size:]
                return chunk
            def sendall(self, data): self.sent.append(data)
        fake = FailedAuthSocket()
        with patch.object(gate.socket, "create_connection", return_value=fake):
            with self.assertRaises(PermissionError):
                gate.stop_server_rcon(self.target / "run/server.properties")
        self.assertEqual(len(fake.sent), 1)


if __name__ == "__main__":
    unittest.main()
