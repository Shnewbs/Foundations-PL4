"""Fail-closed offline checks for the independent installed-JAR real-client gate.

These are synthetic-policy regressions, never substitutes for two native clients.
"""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch
import subprocess

import installed_binary_two_clients as gate
from verify_upgrade_evidence import digest, NAMES, FILES


class InstalledTwoClientPolicy(unittest.TestCase):
    def test_candidate_constants_match_both_workflow_provenance_pins(self):
        # A successful beta seed cannot substitute for a newer candidate restore.
        # This failed the installed migration run 40900293347 when only workflow
        # artifact pins changed but this controller retained the older JAR SHA.
        root = Path(__file__).resolve().parents[2]
        for filename in ("installed-binary-two-client-upgrade.yml", "installed-beta-binary-smoke.yml"):
            with self.subTest(workflow=filename):
                workflow = (root / ".github" / "workflows" / filename).read_text()
                self.assertIn("--source-sha " + gate.CANDIDATE_SOURCE, workflow)
                self.assertIn("--jar-sha256 " + gate.CANDIDATE_JAR, workflow)
                self.assertIn("--expected-native-tests 207", workflow)
                self.assertIn("actions/artifacts/12948304091", workflow)
                self.assertIn("--artifact-sha256 766e57bd47aa926f2a8a471930df8f475e579be1c9b8e666b1097a2a386960dd", workflow)
                self.assertNotIn("actions/artifacts/12907239925", workflow)
        self.assertNotEqual(gate.BASELINE_JAR, gate.CANDIDATE_JAR)

    def test_sha_requires_exact_lowercase(self):
        self.assertEqual(gate.exact_sha(gate.BASELINE_JAR), gate.BASELINE_JAR)
        for value in ("", "a" * 63, "A" * 64, True, None, "../evil"):
            with self.subTest(value=value), self.assertRaises(ValueError):
                gate.exact_sha(value)

    def test_source_requires_immutable_commit(self):
        self.assertEqual(gate.exact_source(gate.BASELINE_SOURCE), gate.BASELINE_SOURCE)
        for value in ("main", "v0.2b.0", "a" * 39, "g" * 40):
            with self.subTest(value=value), self.assertRaises(ValueError):
                gate.exact_source(value)

    def test_production_compiles_only_from_installed_binary(self):
        build = gate.build_file("PL4ProbeA", "FoundationsPL4-1.21.1-0.2b.0.jar")
        self.assertIn("compileOnly files('run/mods/FoundationsPL4-1.21.1-0.2b.0.jar')", build)
        self.assertIn("--quickPlayMultiplayer", build)
        self.assertIn("21.1.250", build)
        self.assertNotIn("implementation project(", build)
        self.assertNotIn("net.foundations.pl4.src", build)

    def test_split_package_native_module_failure_is_prevented(self):
        # First real server failure (40834122264): ModLauncher refused the
        # PL4 package being exported by BOTH the installed JAR and probe mod.
        self.assertTrue(gate.SERVER_REL.startswith("src/main/java/net/foundations/pl4probe/"))
        self.assertTrue(gate.CLIENT_REL.startswith("src/main/java/net/foundations/pl4probe/"))
        self.assertNotEqual(gate.SERVER_REL.split("/MultiplayerServerProbe.java")[0],
                            "src/main/java/net/foundations/pl4")
        self.assertNotIn("package net.foundations.pl4;", gate.ENTRY)

    def test_part_kind_constructed_from_installed_jar(self):
        import inspect
        source = inspect.getsource(gate._prepare)
        self.assertIn('Class<?> kind = Class.forName(', source)
        self.assertIn('installedPart(', source)
        self.assertIn('installedPart(', source)

    def test_server_only_binds_loopback(self):
        build = gate.build_file("server", "FoundationsPL4-1.21.1-0.2b.0.jar")
        self.assertIn("server { server()", build)
        self.assertNotIn("--quickPlayMultiplayer", build)

    def test_neighboring_jar_filename_cannot_substitute(self):
        with self.assertRaises(ValueError):
            gate.build_file("PL4ProbeA", "FoundationsPL4-1.20.1-0.2b.0.jar")
        with self.assertRaises(ValueError):
            gate.build_file("PL4ProbeA", "../../some-other.jar")

    def test_seed_rejects_wrong_source_before_runtime(self):
        with self.assertRaisesRegex(ValueError, "immutable"):
            gate.run_phase(".", "x.jar", "x", "seed", gate.CANDIDATE_SOURCE, gate.BASELINE_JAR)

    def test_seed_rejects_wrong_binary_before_runtime(self):
        with self.assertRaisesRegex(ValueError, "immutable"):
            gate.run_phase(".", "x.jar", "x", "seed", gate.BASELINE_SOURCE, gate.CANDIDATE_JAR)

    def test_candidate_rejects_historical_or_neighbor_source(self):
        with self.assertRaisesRegex(ValueError, "qualified"):
            gate.run_phase(".", "x.jar", "x", "restore", gate.BASELINE_SOURCE, gate.CANDIDATE_JAR)

    def test_bounded_timeout_cannot_be_zero(self):
        with self.assertRaisesRegex(ValueError, "bounded"):
            gate.run_phase(".", "x.jar", "x", "seed",
                           gate.BASELINE_SOURCE, gate.BASELINE_JAR, timeout=0)

    def test_missing_native_evidence_cannot_qualify(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises((OSError, KeyError, ValueError)):
                gate.verify_phase(tmp, "seed", gate.BASELINE_SOURCE, gate.BASELINE_JAR)

    def test_native_bootstrap_gate_requires_actual_modlauncher_launch(self):
        with tempfile.TemporaryDirectory() as temp:
            log = Path(temp) / "clientA.log"
            log.write_text(
                "[main/INFO] [EARLYDISPLAY/]: Requested GL version 4.5 got version 4.5\n"
                "[main/INFO] [LaunchServiceHandler/MODLAUNCHER]: "
                "Launching target 'forgeclientdev' with arguments []\n"
            )
            process = Mock()
            process.poll.return_value = None
            with patch.object(gate.time, "sleep") as sleep:
                gate._wait_for_client_native_bootstrap(
                    process, log, gate.time.monotonic() + 20
                )
                sleep.assert_not_called()

    def test_native_bootstrap_gate_rejects_missing_stb_library(self):
        with tempfile.TemporaryDirectory() as temp:
            log = Path(temp) / "clientA.log"
            log.write_text(
                "java.lang.UnsatisfiedLinkError: Failed to locate library: liblwjgl_stb.so"
            )
            process = Mock()
            process.poll.return_value = 1
            with self.assertRaisesRegex(RuntimeError, "liblwjgl_stb.so"):
                gate._wait_for_client_native_bootstrap(
                    process, log, gate.time.monotonic() + 20
                )

    def test_native_bootstrap_gate_rejects_missing_launch_marker(self):
        with tempfile.TemporaryDirectory() as temp:
            log = Path(temp) / "clientA.log"
            log.write_text("Requested GL version 4.5 got version 4.5")
            process = Mock()
            process.poll.return_value = None
            with patch.object(gate.time, "monotonic", side_effect=[0, 2]):
                with patch.object(gate.time, "sleep"):
                    with self.assertRaisesRegex(TimeoutError, "LWJGL/ModLauncher"):
                        gate._wait_for_client_native_bootstrap(process, log, 1)

    def test_installed_clients_start_serial_bootstrap_but_keep_both_real_clients(self):
        import inspect
        source = inspect.getsource(gate.run_phase)
        a = source.index('launch(NAMES[0], "runClient")')
        stage = source.index("_wait_for_client_native_bootstrap(")
        b = source.index('launch(NAMES[1], "runClient")')
        self.assertLess(a, stage)
        self.assertLess(stage, b)
        self.assertIn("if all(p.poll() == 0 for p in clients):", source)

    def test_native_self_shutdown_does_not_attempt_closed_rcon(self):
        server = Mock()
        server.poll.return_value = None
        server.returncode = 0
        with patch.object(gate.smoke, "_stop_with_retry") as stop:
            gate._finish_server(server, Path("disposable/server.properties"))
            stop.assert_not_called()
        server.wait.assert_called_once_with(timeout=30)

    def test_hanging_native_server_uses_bounded_authenticated_rcon(self):
        server = Mock()
        server.poll.return_value = None
        server.returncode = 0
        server.wait.side_effect = [subprocess.TimeoutExpired(["native"], 30), 0]
        with patch.object(gate.smoke, "_stop_with_retry") as stop:
            gate._finish_server(server, Path("disposable/server.properties"))
            stop.assert_called_once_with(Path("disposable/server.properties"))
        self.assertEqual(server.wait.call_count, 2)

    def test_failed_native_exit_remains_rejected(self):
        server = Mock()
        server.poll.return_value = 1
        server.returncode = 1
        with patch.object(gate.smoke, "_stop_with_retry") as stop:
            with self.assertRaisesRegex(RuntimeError, "cleanly"):
                gate._finish_server(server, Path("disposable/server.properties"))
            stop.assert_not_called()

    def test_unavailable_rcon_after_true_hang_remains_error(self):
        server = Mock()
        server.poll.return_value = None
        server.returncode = None
        server.wait.side_effect = subprocess.TimeoutExpired(["native"], 30)
        with patch.object(gate.smoke, "_stop_with_retry", side_effect=RuntimeError("RCON unavailable")):
            with self.assertRaisesRegex(RuntimeError, "RCON unavailable"):
                gate._finish_server(server, Path("disposable/server.properties"))

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name)
        world = {"sha256": "a" * 64, "file_count": 3, "byte_count": 256}
        self.world = world
        self.server = {
            "source_commit": gate.BASELINE_SOURCE, "phase": "seed",
            "node_identity": "19bf6000-1111-2222-3333-010203040506",
            "real_client_names": list(NAMES),
            "real_client_uuids": [
                "f2b21560-1111-2222-3333-010203040501",
                "f2b21560-1111-2222-3333-010203040502",
            ],
            "fake_players": False, "foreign_non_operator_denied": True,
            "operator_access_explicitly_granted": True, "initial_items": 128,
            "items_per_player": [17, 17], "remaining_items": 94,
            "duplicate_network_requests_ignored": True, "saved_world_restore": False,
        }
        self.server["cable_transfer"] = {
            "engine": "net.foundations.pl4.NetworkEngine.tick", "cable_count": 7,
            "connected_endpoints": 2, "severed_endpoints": 1,
            "severed_ticks": 60, "source_before": 1728, "source_after": 1664,
            "sink_before": 0, "sink_after": 64, "delivered": 64,
            "total": 1728, "direct_transfer_invocations": 0, "pending_escrow": 0,
            "identities": ["abcdef01-1111-2222-3333-%012x" % i for i in range(9)],
            "two_real_clients_connected": True,
        }
        self._write("server-multiplayer-proof.json", json.dumps(self.server))
        self._write("server.log", gate.TAG + gate.BASELINE_JAR + "\n" +
                    gate.CABLE_TAG + gate.BASELINE_JAR + ":64\n")
        for index, name in enumerate(NAMES):
            self._write(name + ".log", gate.CLIENT_TAG + gate.BASELINE_JAR + ":" + name)
            self._write(name + "-storage.png", b"\x89PNG\r\n\x1a\n" + b"!" * 1100)
            self._write(name + "-client-proof.json", json.dumps({
                "source_commit": gate.BASELINE_SOURCE, "phase": "seed",
                "name": name, "uuid": self.server["real_client_uuids"][index],
                "items_received": 17, "identical_packets_sent": 2,
            }))
        self._write("saved-world-seed.zip", b"synthetic-not-a-real-archive")
        self._manifest()

    def _write(self, name, data):
        (self.path / name).write_bytes(data if isinstance(data, bytes) else data.encode())

    def _manifest(self):
        record = {
            "schema": 1, "phase": "seed", "source_commit": gate.BASELINE_SOURCE,
            "installed_jar_sha256": gate.BASELINE_JAR, "world": self.world,
            "copied_input_world": None,
            "files": {name: {"sha256": digest(self.path / name),
                             "bytes": (self.path / name).stat().st_size}
                      for name in (*FILES, "saved-world-seed.zip")},
            "client_executed": True, "clean_server_exit": True,
            "clean_client_exits": [True, True],
            "rc_publication_cleared": False,
        }
        self._write("installed-client-phase.json", json.dumps(record))

    def check_fixture(self):
        with patch.object(gate, "check_project"), \
             patch.object(gate, "zip_world_fingerprint", return_value=self.world):
            return gate.verify_phase(self.path, "seed",
                                     gate.BASELINE_SOURCE, gate.BASELINE_JAR)

    def test_mock_policy_fixture_is_not_native_acceptance(self):
        self.assertEqual(self.check_fixture()["world"], self.world)

    def test_fake_player_is_rejected(self):
        self.server["fake_players"] = True
        self._write("server-multiplayer-proof.json", json.dumps(self.server))
        self._manifest()
        with self.assertRaisesRegex(ValueError, "authorization"):
            self.check_fixture()

    def test_foreign_player_permission_is_rejected(self):
        self.server["foreign_non_operator_denied"] = False
        self._write("server-multiplayer-proof.json", json.dumps(self.server))
        self._manifest()
        with self.assertRaisesRegex(ValueError, "authorization"):
            self.check_fixture()

    def test_duplicate_packets_cannot_mint_items(self):
        self.server["remaining_items"] = 128
        self._write("server-multiplayer-proof.json", json.dumps(self.server))
        self._manifest()
        with self.assertRaisesRegex(ValueError, "authorization"):
            self.check_fixture()

    def test_wrong_runtime_jar_fails_before_logs(self):
        record = json.loads((self.path / "installed-client-phase.json").read_text())
        record["installed_jar_sha256"] = gate.CANDIDATE_JAR
        self._write("installed-client-phase.json", json.dumps(record))
        with self.assertRaisesRegex(ValueError, "identity"):
            self.check_fixture()

    def test_no_real_clients_does_not_qualify(self):
        record = json.loads((self.path / "installed-client-phase.json").read_text())
        record["client_executed"] = False
        self._write("installed-client-phase.json", json.dumps(record))
        with self.assertRaisesRegex(ValueError, "identity"):
            self.check_fixture()

    def test_nonzero_native_client_exit_fails(self):
        record = json.loads((self.path / "installed-client-phase.json").read_text())
        record["clean_client_exits"] = [True, False]
        self._write("installed-client-phase.json", json.dumps(record))
        with self.assertRaisesRegex(ValueError, "identity"):
            self.check_fixture()

    def test_tampered_screenshot_fails_sha(self):
        self._write("PL4ProbeB-storage.png", b"not real")
        with self.assertRaisesRegex(ValueError, "artifact"):
            self.check_fixture()


    def _cable_mutation(self, **values):
        self.server["cable_transfer"].update(values)
        self._write("server-multiplayer-proof.json", json.dumps(self.server))
        self._manifest()

    def test_missing_cable_receipt_cannot_qualify(self):
        self.server.pop("cable_transfer")
        self._write("server-multiplayer-proof.json", json.dumps(self.server))
        self._manifest()
        with self.assertRaisesRegex(ValueError, "cable transfer"):
            self.check_fixture()

    def test_short_disconnected_interval_fails(self):
        self._cable_mutation(severed_ticks=1)
        with self.assertRaisesRegex(ValueError, "revocation"):
            self.check_fixture()

    def test_two_client_presence_required_during_delivery(self):
        self._cable_mutation(two_real_clients_connected=False)
        with self.assertRaisesRegex(ValueError, "revocation"):
            self.check_fixture()

    def test_direct_transfer_substitution_fails(self):
        self._cable_mutation(direct_transfer_invocations=1)
        with self.assertRaisesRegex(ValueError, "scheduled-tick"):
            self.check_fixture()

    def test_duplicate_saved_cable_uuid_fails(self):
        self.server["cable_transfer"]["identities"][8] = self.server["cable_transfer"]["identities"][7]
        self._write("server-multiplayer-proof.json", json.dumps(self.server))
        self._manifest()
        with self.assertRaisesRegex(ValueError, "identity"):
            self.check_fixture()

    def test_synthetic_item_creation_fails(self):
        self._cable_mutation(sink_after=65)
        with self.assertRaisesRegex(ValueError, "item conserving"):
            self.check_fixture()

    def test_boolean_cannot_substitute_native_tick_count(self):
        self._cable_mutation(severed_ticks=True)
        with self.assertRaisesRegex(ValueError, "integer count"):
            self.check_fixture()

    def test_missing_native_cable_tick_marker_fails(self):
        self._write("server.log", gate.TAG + gate.BASELINE_JAR)
        self._manifest()
        with self.assertRaisesRegex(ValueError, "marker"):
            self.check_fixture()

    def test_replayed_native_cable_tick_marker_fails(self):
        p = self.path / "server.log"
        p.write_text(p.read_text() + gate.CABLE_TAG + gate.BASELINE_JAR + ":64\n")
        self._manifest()
        with self.assertRaisesRegex(ValueError, "marker"):
            self.check_fixture()

    def test_duplicate_server_marker_is_rejected(self):
        self._write("server.log", (gate.TAG + gate.BASELINE_JAR + "\n") * 2)
        self._manifest()
        with self.assertRaisesRegex(ValueError, "marker"):
            self.check_fixture()


if __name__ == "__main__":
    unittest.main()
