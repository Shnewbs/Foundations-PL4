"""Synthetic negative-policy fixtures; these are NEVER native-world qualifications."""
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

import installed_binary_smoke as smoke
import installed_binary_world_upgrade as gate


class InstalledWorldUpgradePolicy(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.checkout = self.root / "checkout"
        wrapper = self.checkout / "gradle/wrapper"
        wrapper.mkdir(parents=True)
        (self.checkout / "gradlew").write_text("#!/bin/sh\nexit 0\n")
        (wrapper / "gradle-wrapper.jar").write_bytes(b"synthetic-wrapper")
        (wrapper / "gradle-wrapper.properties").write_text("distributionUrl=local\n")
        self.beta = self.root / "beta.jar"
        self.candidate = self.root / "candidate.jar"
        for path, suffix in ((self.beta, "beta"), (self.candidate, "candidate")):
            with zipfile.ZipFile(path, "w") as z:
                z.writestr("META-INF/neoforge.mods.toml",
                           'modId="foundations_pl4"\nversionRange="[1.21.1]"\n# ' + suffix)
                z.writestr("net/foundations/pl4/FoundationsPL4.class", b"real-bytecode-not-used-" + suffix.encode())
        self.beta_sha = smoke.digest(self.beta)
        self.candidate_sha = smoke.digest(self.candidate)
        self.seed = self.root / "seed"
        self.restore = self.root / "restore"

    def configured_parts(self):
        return [
            {"slot": 5, "kind": "INVENTORY_READER", "face": "EAST",
             "owner": gate.PART_OWNER, "identity": "11111111-2222-4333-8444-555555555555"},
            {"slot": 6, "kind": "DATA_CABLE", "face": "DOWN",
             "owner": gate.PART_OWNER, "identity": "66666666-7777-4888-8999-aaaaaaaaaaaa"},
        ]

    def transfer(self, phase):
        if phase == "seed":
            before, after, sink_before, sink_after = 1728, 1664, 0, 64
        else:
            before, after, sink_before, sink_after = 1664, 1600, 64, 128
        return {
            "engine": "net.foundations.pl4.TransferEngine",
            "source_before": before, "source_after": after,
            "sink_before": sink_before, "sink_after": sink_after,
            "delivered": 64, "total": 1728, "pending_escrow": 0,
            "source_identity": "12345678-1234-4234-8234-123456789abc",
            "sink_identity": "abcdefab-abcd-4bcd-8bcd-abcdefabcdef",
        }

    def topology(self):
        return {
            "network_engine": "net.foundations.pl4.NetworkEngine",
            "graph": "PHYSICAL_DATA_CABLE",
            "connected_endpoints": 2, "severed_endpoints": 1,
            "restored_endpoints": 2, "cable_count": 7,
            "source_identity": "10000000-0000-4000-8000-000000000001",
            "sink_identity": "10000000-0000-4000-8000-000000000002",
            "cable_identities": [
                "10000000-0000-4000-8000-%012d" % i for i in range(3, 10)
            ],
            "automatic_tick_tested": True,
        }

    def scheduled(self, phase):
        if phase == "seed":
            source_before, sink_before = 1728, 0
        else:
            source_before, sink_before = 1664, 64
        return {
            "engine": "net.foundations.pl4.NetworkEngine.tick",
            "source_before": source_before, "source_after": source_before - 64,
            "sink_before": sink_before, "sink_after": sink_before + 64,
            "delivered": 64, "total": 1728, "sample_ticks_observed": 20,
            "direct_transfer_invocations": 0, "pending_escrow": 0,
            "source_identity": self.topology()["source_identity"],
            "sink_identity": self.topology()["sink_identity"],
        }

    def phase(self, root, phase, jar, sha, copy=None):
        smoke.prepare(self.checkout, jar, root, sha)
        if copy is None:
            world = root / "run/world"
            world.mkdir(parents=True)
            (world / "level.dat").write_bytes(b"synthetic-world-level")
            (world / "region").mkdir()
            (world / "region/r.0.0.mca").write_bytes(b"synthetic-chunk")
        else:
            import shutil
            shutil.copytree(copy / "run/world", root / "run/world")
        log = root / "installed-server.log"
        log.write_text(smoke.MARKER + sha + "\n" + gate.WORLD_MARKER + phase.upper() + ":" + sha + "\n" + "PL4_INSTALLED_SCHEDULED_TICK:" + sha + ":64\n")
        (root / "installed-server-proof.json").write_text(json.dumps({
            "schema": 1, "loaded_mod_id": "foundations_pl4",
            "server_started": True, "expected_jar_sha256": sha,
        }))
        smoke.verify(root, log)  # Synthetic log tests only.
        (root / "installed-world-proof.json").write_text(json.dumps({
            "schema": 1, "phase": phase, "expected_jar_sha256": sha,
            "host": gate.HOST, "host_entity": gate.ENTITY,
            "barrel_diamonds": 128, "server_started": True,
            "parts": self.configured_parts(),
            "transfer": self.transfer(phase),
            "topology": self.topology(),
            "scheduled_transfer": self.scheduled(phase),
        }))
        copied = gate.world_fingerprint(copy / "run/world") if copy else None
        return gate.capture_phase(root, phase, sha, copied)

    def pair(self):
        self.phase(self.seed, "seed", self.beta, self.beta_sha)
        self.phase(self.restore, "restore", self.candidate, self.candidate_sha, self.seed)

    def test_distinct_binaries_and_world_fingerprint_policy(self):
        self.pair()
        result = gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)
        self.assertEqual(result["installed_beta_to_candidate_world_smoke"], "PASS")
        self.assertIs(result["client_executed"], False)
        self.assertIs(result["rc_publication_cleared"], False)
        self.assertIn("Server-only", result["scope"])

    def test_same_binary_never_counts_as_migration(self):
        self.pair()
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.beta_sha)

    def test_world_phase_proof_is_required(self):
        self.pair()
        (self.restore / "installed-world-proof.json").unlink()
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_wrong_host_identity_fails(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        p.write_text(p.read_text().replace("foundations_pl4:multipart_host", "minecraft:air"))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_repeated_phase_marker_fails(self):
        self.pair()
        log = self.restore / "installed-server.log"
        log.write_text(log.read_text() + gate.WORLD_MARKER + "RESTORE:" + self.candidate_sha + "\n")
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_replaced_candidate_binary_fails(self):
        self.pair()
        install = self.restore / "run/mods" / self.candidate.name
        with install.open("ab") as f:
            f.write(b"fake-tampering")
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_mutated_retained_world_archive_fails(self):
        self.pair()
        with (self.seed / "saved-world-seed.zip").open("ab") as f:
            f.write(b"changed")
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_invented_candidate_world_copy_fails(self):
        self.pair()
        path = self.restore / "world-phase-evidence.json"
        evidence = json.loads(path.read_text())
        evidence["copied_input_world"]["sha256"] = "f" * 64
        path.write_text(json.dumps(evidence))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_missing_candidate_world_manifest_fails(self):
        self.pair()
        (self.restore / "world-phase-evidence.json").unlink()
        with self.assertRaises(FileNotFoundError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_rogue_java_source_is_detected(self):
        self.pair()
        path = self.restore / "src/main/java/net/foundations/pl4/FoundationsPL4.java"
        path.parent.mkdir(parents=True)
        path.write_text("package net.foundations.pl4;")
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_wrong_maturity_flag_never_passes(self):
        self.pair()
        path = self.restore / "world-phase-evidence.json"
        evidence = json.loads(path.read_text())
        evidence["rc_publication_cleared"] = True
        path.write_text(json.dumps(evidence))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)


    def test_no_parts_cannot_qualify_multipart_migration(self):
        self.pair()
        path = self.restore / "installed-world-proof.json"
        proof = json.loads(path.read_text())
        proof["parts"] = []
        path.write_text(json.dumps(proof))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_part_identity_must_survive_actual_restore(self):
        self.pair()
        path = self.restore / "installed-world-proof.json"
        proof = json.loads(path.read_text())
        proof["parts"][0]["identity"] = "bbbbbbbb-cccc-4ddd-8eee-ffffffffffff"
        path.write_text(json.dumps(proof))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_duplicate_part_identity_is_rejected(self):
        self.pair()
        path = self.seed / "installed-world-proof.json"
        proof = json.loads(path.read_text())
        proof["parts"][1]["identity"] = proof["parts"][0]["identity"]
        path.write_text(json.dumps(proof))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_wrong_owner_fails_closed(self):
        self.pair()
        path = self.restore / "installed-world-proof.json"
        proof = json.loads(path.read_text())
        proof["parts"][0]["owner"] = "00000000-0000-4000-8000-000000000000"
        path.write_text(json.dumps(proof))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_wrong_slot_fails_closed(self):
        self.pair()
        path = self.restore / "installed-world-proof.json"
        proof = json.loads(path.read_text())
        proof["parts"][0]["slot"] = 12
        path.write_text(json.dumps(proof))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_missing_real_transfer_proof_fails(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        del receipt["transfer"]
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_zero_movement_does_not_qualify(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["transfer"].update({"source_after": 1664, "sink_after": 64, "delivered": 0})
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_double_counted_diamonds_fail(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["transfer"]["sink_after"] = 129
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_booleans_are_not_native_item_counts(self):
        self.pair()
        p = self.seed / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["transfer"]["delivered"] = True
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_transfer_part_identity_must_survive_restore(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["transfer"]["sink_identity"] = "bbbbbbbb-cccc-4ddd-8eee-ffffffffffff"
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_saved_transfer_quantities_cannot_be_rewritten(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["transfer"]["source_before"] = 1663
        receipt["transfer"]["sink_before"] = 65
        receipt["transfer"]["source_after"] = 1599
        receipt["transfer"]["sink_after"] = 129
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_unsupported_transfer_implementation_fails(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["transfer"]["engine"] = "net.minecraft.world.Container"
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)


    def test_fake_connected_network_does_not_qualify(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["topology"]["connected_endpoints"] = 1
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_missing_severed_cable_control_fails_closed(self):
        self.pair()
        p = self.seed / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["topology"]["severed_endpoints"] = 2
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_rewritten_saved_cable_identity_is_rejected(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["topology"]["cable_identities"][3] = "f0000000-0000-4000-8000-000000000000"
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_duplicate_native_cable_uuid_is_rejected(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["topology"]["cable_identities"][3] = receipt["topology"]["source_identity"]
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_automatic_tick_must_not_be_claimed_without_execution(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["topology"]["automatic_tick_tested"] = True
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)


if __name__ == "__main__":
    unittest.main()

    def test_missing_scheduled_transfer_fails(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        del receipt["scheduled_transfer"]
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_synthetic_direct_transfer_cannot_substitute_for_tick(self):
        self.pair()
        p = self.seed / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["scheduled_transfer"]["direct_transfer_invocations"] = 1
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_scheduled_inventory_zero_or_forged_balance_fails(self):
        self.pair()
        p = self.seed / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["scheduled_transfer"]["delivered"] = 0
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_scheduled_native_endpoint_not_in_graph_fails(self):
        self.pair()
        p = self.seed / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["scheduled_transfer"]["source_identity"] = "ffffffff-ffff-4fff-8fff-ffffffffffff"
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_missing_native_scheduled_log_marker_fails(self):
        self.pair()
        p = self.seed / "installed-server.log"
        p.write_text(p.read_text().replace("PL4_INSTALLED_SCHEDULED_TICK:", "FAKE_TICK:"))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_scheduled_tick_boolean_cannot_count_as_duration(self):
        self.pair()
        p = self.seed / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["scheduled_transfer"]["sample_ticks_observed"] = True
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)

    def test_scheduled_restored_inventory_cannot_reset(self):
        self.pair()
        p = self.restore / "installed-world-proof.json"
        receipt = json.loads(p.read_text())
        receipt["scheduled_transfer"]["source_before"] = 1728
        receipt["scheduled_transfer"]["sink_before"] = 0
        receipt["scheduled_transfer"]["source_after"] = 1664
        receipt["scheduled_transfer"]["sink_after"] = 64
        p.write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            gate.verify_upgrade(self.seed, self.restore, self.beta_sha, self.candidate_sha)
