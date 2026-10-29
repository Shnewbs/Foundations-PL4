"""Adversarial local provenance tests; synthetic ZIPs never certify a native game run."""
import io
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

import exact_native_candidate as gate
from installed_binary_smoke import digest

digest_file = digest

SOURCE = "a" * 40
NAME = "build/libs/FoundationsPL4-1.21.1-0.2.0-rc.1-dev.jar"
NATIVE_LOG = "========= 194 GAME TESTS COMPLETE IN 5.597 s ======================\nAll 194 required tests passed :)\nBUILD SUCCESSFUL\n"


class ExactNativeCandidateTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.artifact = self.root / "native-artifact.zip"
        self.output = self.root / "candidate.jar"
        with io.BytesIO() as data:
            with zipfile.ZipFile(data, "w") as z:
                z.writestr("META-INF/neoforge.mods.toml",
                           'modId="foundations_pl4"\nversionRange="[1.21.1]"\n')
                z.writestr("net/foundations/pl4/FoundationsPL4.class", b"fixture class")
            self.binary = data.getvalue()
        self.verified_jar = self.root / "reference.jar"
        self.verified_jar.write_bytes(self.binary)
        self.jar_sha = digest(self.verified_jar)
        self.write_archive()
        self.art_sha = digest(self.artifact)

    def tearDown(self):
        self.tmp.cleanup()

    def write_archive(self, source=SOURCE, member=NAME, extra=None, data=None):
        with zipfile.ZipFile(self.artifact, "w") as z:
            z.writestr("verification-logs/SOURCE_COMMIT.txt", source + "\n")
            z.writestr("verification-logs/native.log", NATIVE_LOG)
            z.writestr(member, self.binary if data is None else data)
            z.writestr("build/libs/FoundationsPL4-1.21.1-0.2.0-rc.1-dev-sources.jar", b"sources")
            if member != "build/libs/FoundationsPL4-1.21.1-0.2.0-rc.1-dev-native-scenarios.jar":
                z.writestr("build/libs/FoundationsPL4-1.21.1-0.2.0-rc.1-dev-native-scenarios.jar", b"fixture")
            if extra:
                for name, payload in extra:
                    z.writestr(name, payload)

    def metadata(self, sha=SOURCE, digest=None, expired=False):
        artifact_id = 12859271452
        metadata = {
            "id": artifact_id, "name": "verification-" + sha + "-1",
            "expired": expired, "digest": "sha256:" + (digest or digest_file(self.artifact)),
            "archive_download_url": "https://api.github.com/repos/Shnewbs/Foundations-PL4/actions/artifacts/"
                                    + str(artifact_id) + "/zip",
            "workflow_run": {
                "id": 40785212711, "repository_id": 1396808859,
                "head_repository_id": 1396808859,
                "head_branch": "main", "head_sha": sha
            }
        }
        file = self.root / "authenticated-metadata.json"
        file.write_text(json.dumps(metadata))
        return file

    def test_main_artifact_without_embedded_marker_accepts_authenticated_gitHub_source(self):
        with zipfile.ZipFile(self.artifact, "w") as z:
            z.writestr(NAME, self.binary)
            z.writestr("verification-logs/native.log", NATIVE_LOG)
        metadata = self.metadata()
        proof = gate.extract(self.artifact, self.output, digest_file(self.artifact),
                             SOURCE, self.jar_sha, metadata)
        self.assertEqual(proof["artifact_github_metadata_id"], 12859271452)
        self.assertEqual(proof["source_attestation"], "github-validated-artifact-metadata")

    def test_main_artifact_without_source_and_without_metadata_is_rejected(self):
        with zipfile.ZipFile(self.artifact, "w") as z:
            z.writestr(NAME, self.binary)
            z.writestr("verification-logs/native.log", NATIVE_LOG)
        with self.assertRaises(ValueError): self.extract()

    def test_main_artifact_mismatched_metadata_source_rejected(self):
        with zipfile.ZipFile(self.artifact, "w") as z:
            z.writestr(NAME, self.binary)
            z.writestr("verification-logs/native.log", NATIVE_LOG)
        meta = self.metadata(sha="b" * 40)
        with self.assertRaises(ValueError):
            gate.extract(self.artifact, self.output, digest_file(self.artifact),
                         SOURCE, self.jar_sha, meta)

    def test_main_artifact_expired_metadata_rejected(self):
        with zipfile.ZipFile(self.artifact, "w") as z:
            z.writestr(NAME, self.binary)
            z.writestr("verification-logs/native.log", NATIVE_LOG)
        meta = self.metadata(expired=True)
        with self.assertRaises(ValueError):
            gate.extract(self.artifact, self.output, digest_file(self.artifact),
                         SOURCE, self.jar_sha, meta)

    def extract(self, source=SOURCE):
        return gate.extract(self.artifact, self.output, digest(self.artifact), source, self.jar_sha)

    def test_exact_source_artifact_and_one_native_production_jar(self):
        proof = self.extract()
        self.assertEqual(digest(self.output), self.jar_sha)
        self.assertEqual(proof["source_commit"], SOURCE)
        self.assertFalse(proof["installed_candidate_binary_executed"])
        self.assertFalse(proof["rc_publication_cleared"])
        self.assertEqual(json.loads(Path(str(self.output)+".provenance.json").read_text()), proof)

    def test_missing_native_jvm_log_rejected(self):
        with zipfile.ZipFile(self.artifact, "w") as z:
            z.writestr("verification-logs/SOURCE_COMMIT.txt", SOURCE + "\n")
            z.writestr(NAME, self.binary)
        with self.assertRaises(ValueError): self.extract()

    def test_native_failed_game_test_count_rejected(self):
        self.write_archive(extra=[("verification-logs/native-failure-summary.txt", b"failed")])
        with zipfile.ZipFile(self.artifact, "w") as z:
            z.writestr("verification-logs/SOURCE_COMMIT.txt", SOURCE + "\n")
            z.writestr("verification-logs/native.log", "All 193 required tests passed :)\\nBUILD SUCCESSFUL\\n")
            z.writestr(NAME, self.binary)
        with self.assertRaises(ValueError): self.extract()

    def test_exact_195_native_source_requires_195_completion_and_pass(self):
        with zipfile.ZipFile(self.artifact, "w") as z:
            z.writestr("verification-logs/SOURCE_COMMIT.txt", SOURCE + "\n")
            z.writestr("verification-logs/native.log",
                       "========= 195 GAME TESTS COMPLETE IN 5.621 s ======================\n"
                       "All 195 required tests passed :)\nBUILD SUCCESSFUL\n")
            z.writestr(NAME, self.binary)
        receipt = gate.extract(self.artifact, self.output,
                               digest(self.artifact), SOURCE, self.jar_sha,
                               expected_native_tests=195)
        self.assertEqual(receipt["native_log_success_count"], "195/195")
        self.assertEqual(receipt["production_jar_sha256"], self.jar_sha)

    def test_declared_195_rejects_historical_194_native_log(self):
        with self.assertRaisesRegex(ValueError, "195/195"):
            gate.extract(self.artifact, self.output, digest(self.artifact),
                         SOURCE, self.jar_sha, expected_native_tests=195)
        self.assertFalse(self.output.exists())

    def test_native_count_type_and_bounds_fail_closed(self):
        for count in (True, 0, -1, 10001, "195"):
            with self.subTest(count=count):
                with self.assertRaisesRegex(ValueError, "native GameTest count"):
                    gate.extract(self.artifact, self.output,
                                 digest(self.artifact), SOURCE, self.jar_sha,
                                 expected_native_tests=count)
                self.assertFalse(self.output.exists())

    def test_wrong_source_rejected(self):
        self.write_archive(source="b" * 40)
        with self.assertRaises(ValueError): self.extract()

    def test_tampered_outer_artifact_hash_rejected(self):
        with self.assertRaises(ValueError):
            gate.extract(self.artifact, self.output, "0" * 64, SOURCE, self.jar_sha)

    def test_neighbor_version_jar_not_relabelled(self):
        self.write_archive(member=NAME.replace("1.21.1", "1.21.2"))
        with self.assertRaises(ValueError): self.extract()

    def test_native_fixture_and_source_jars_cannot_substitute_for_production(self):
        self.write_archive(member=NAME.replace(".jar", "-native-scenarios.jar"))
        with self.assertRaises(ValueError): self.extract()

    def test_duplicate_production_jars_rejected(self):
        self.write_archive(extra=[(NAME.replace(".jar", "-alternative.jar"), self.binary)])
        with self.assertRaises(ValueError): self.extract()

    def test_changed_inner_binary_rejected(self):
        self.write_archive(data=self.binary + b"unexpected bytes")
        with self.assertRaises(ValueError): self.extract()

    def test_malicious_archive_entry_rejected(self):
        self.write_archive(extra=[("../escape.txt", b"unsafe")])
        with self.assertRaises(ValueError): self.extract()

    def test_existing_output_is_never_overwritten(self):
        self.output.write_bytes(b"existing data")
        with self.assertRaises(FileExistsError): self.extract()
        self.assertEqual(self.output.read_bytes(), b"existing data")


if __name__ == "__main__":
    unittest.main()
