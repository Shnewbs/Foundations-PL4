"""Safety tests for the destructive, isolated native performance benchmark.

These tests do not run Gradle or qualify native performance/RC releases.
"""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest import mock

MODULE = Path(__file__).with_name("run_native_comparison.py")
spec = importlib.util.spec_from_file_location("pl4_native_comparison", MODULE)
bench = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bench)


class DisposableWorkspaceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.baseline = self.root / "baseline"
        self.candidate = self.root / "candidate"
        self.output = self.root / "results"
        for path in (self.baseline, self.candidate):
            (path / ".git").mkdir(parents=True)

    def validate(self):
        with mock.patch.object(bench.subprocess, "check_output", return_value=""):
            return bench.require_disposable_workspaces(
                self.baseline, self.candidate, self.output)

    def test_fresh_separate_clean_checkouts_are_accepted(self):
        roots, output = self.validate()
        self.assertEqual(roots, [self.baseline, self.candidate])
        self.assertEqual(output, self.output)

    def test_same_checkout_or_existing_results_are_rejected(self):
        with mock.patch.object(bench.subprocess, "check_output", return_value=""):
            with self.assertRaisesRegex(ValueError, "distinct"):
                bench.require_disposable_workspaces(
                    self.baseline, self.baseline, self.output)
        self.output.mkdir()
        with self.assertRaisesRegex(ValueError, "new output"):
            self.validate()

    def test_preexisting_world_is_never_deleted(self):
        world = self.baseline / "run" / "world"
        world.mkdir(parents=True)
        saved = world / "level.dat"
        saved.write_bytes(b"important saved world")
        with self.assertRaisesRegex(ValueError, "pre-existing Minecraft run"):
            self.validate()
        with self.assertRaisesRegex(ValueError, "Unowned Minecraft run"):
            bench.replace_owned_run(self.baseline, 0, "a" * 40)
        self.assertEqual(saved.read_bytes(), b"important saved world")

    def test_dirty_checkout_is_rejected(self):
        with mock.patch.object(bench.subprocess, "check_output",
                               return_value=" M src/main/java/Changed.java\n"):
            with self.assertRaisesRegex(ValueError, "dirty benchmark checkout"):
                bench.require_disposable_workspaces(
                    self.baseline, self.candidate, self.output)

    def test_unowned_or_mismatched_runtime_is_preserved(self):
        runtime = self.baseline / "run"
        runtime.mkdir()
        important = runtime / "level.dat"
        important.write_bytes(b"retained")
        with self.assertRaisesRegex(ValueError, "unowned or changed"):
            bench.replace_owned_run(self.baseline, 1, "a" * 40)
        marker = runtime / ".pl4-benchmark-owned"
        marker.write_text("b" * 40 + ":1\n")
        with self.assertRaisesRegex(ValueError, "unowned or changed"):
            bench.replace_owned_run(self.baseline, 1, "a" * 40)
        self.assertEqual(important.read_bytes(), b"retained")

    def test_exact_prior_trial_can_be_removed(self):
        runtime = self.baseline / "run"
        runtime.mkdir()
        source = "a" * 40
        bench.mark_owned_run(self.baseline, 1, source)
        (runtime / "level.dat").write_bytes(b"disposable fixture")
        bench.replace_owned_run(self.baseline, 1, source)
        self.assertFalse(runtime.exists())
        runtime.mkdir()
        bench.mark_owned_run(self.baseline, 2, source)
        self.assertEqual(
            (runtime / ".pl4-benchmark-owned").read_text(), source + ":2\n")

    def test_symlinked_runtime_is_rejected_without_following(self):
        outside = self.root / "saved"
        outside.mkdir()
        (outside / "level.dat").write_bytes(b"keep")
        (self.baseline / "run").symlink_to(outside, target_is_directory=True)
        with self.assertRaises(ValueError):
            self.validate()
        with self.assertRaises(ValueError):
            bench.replace_owned_run(self.baseline, 1, "a" * 40)
        self.assertEqual((outside / "level.dat").read_bytes(), b"keep")


if __name__ == "__main__":
    unittest.main()
