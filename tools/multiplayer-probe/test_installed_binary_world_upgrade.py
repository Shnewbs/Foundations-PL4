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
        log.write_text(smoke.MARKER + sha + "\n" + gate.WORLD_MARKER + phase.upper() + ":" + sha + "\n")
        (root / "installed-server-proof.json").write_text(json.dumps({
            "schema": 1, "loaded_mod_id": "foundations_pl4",
            "server_started": True, "expected_jar_sha256": sha,
        }))
        smoke.verify(root, log)  # Synthetic log tests only.
        (root / "installed-world-proof.json").write_text(json.dumps({
            "schema": 1, "phase": phase, "expected_jar_sha256": sha,
            "host": gate.HOST, "host_entity": gate.ENTITY,
            "barrel_diamonds": 128, "server_started": True,
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


if __name__ == "__main__":
    unittest.main()
