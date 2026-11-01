"""Safety tests for the destructive, isolated native performance benchmark.

These tests do not run Gradle or qualify native performance/RC releases.
"""
import importlib.util
import copy
import json
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
        with self.assertRaisesRegex(ValueError, "invalid benchmark ownership marker"):
            bench.replace_owned_run(self.baseline, 1, "a" * 40)
        self.assertEqual(important.read_bytes(), b"retained")

    def test_exact_prior_trial_can_be_removed(self):
        runtime = self.baseline / "run"
        runtime.mkdir()
        source = "a" * 40
        (runtime / "level.dat").write_bytes(b"disposable fixture")
        bench.mark_owned_run(self.baseline, 1, source)
        bench.replace_owned_run(self.baseline, 1, source)
        self.assertFalse(runtime.exists())
        runtime.mkdir()
        bench.mark_owned_run(self.baseline, 2, source)
        marker = json.loads((runtime / ".pl4-benchmark-owned").read_text())
        self.assertEqual(marker["source"], source)
        self.assertEqual(marker["trial"], 2)
        self.assertEqual(marker["inventory_sha256"], bench.inventory_digest(runtime))


    def test_added_world_after_trial_is_preserved(self):
        runtime = self.baseline / "run"
        runtime.mkdir()
        source = "a" * 40
        (runtime / "server.properties").write_text("generated")
        bench.mark_owned_run(self.baseline, 1, source)
        saved = runtime / "world" / "level.dat"
        saved.parent.mkdir()
        saved.write_bytes(b"player world added after the benchmark")
        with self.assertRaisesRegex(ValueError, "changed after its trial"):
            bench.replace_owned_run(self.baseline, 1, source)
        self.assertEqual(saved.read_bytes(), b"player world added after the benchmark")

    def test_modified_generated_file_after_trial_is_preserved(self):
        runtime = self.baseline / "run"
        runtime.mkdir()
        source = "a" * 40
        saved = runtime / "level.dat"
        saved.write_bytes(b"original benchmark bytes")
        bench.mark_owned_run(self.baseline, 1, source)
        saved.write_bytes(b"new player world bytes")
        with self.assertRaisesRegex(ValueError, "changed after its trial"):
            bench.replace_owned_run(self.baseline, 1, source)
        self.assertEqual(saved.read_bytes(), b"new player world bytes")

    def test_nested_symlink_added_after_trial_is_preserved(self):
        runtime = self.baseline / "run"
        runtime.mkdir()
        source = "a" * 40
        bench.mark_owned_run(self.baseline, 1, source)
        outside = self.root / "retained"
        outside.mkdir()
        saved = outside / "level.dat"
        saved.write_bytes(b"outside")
        (runtime / "linked-world").symlink_to(outside, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "linked content"):
            bench.replace_owned_run(self.baseline, 1, source)
        self.assertEqual(saved.read_bytes(), b"outside")

    def test_unreadable_runtime_is_never_marked(self):
        runtime = self.baseline / "run"
        runtime.mkdir()
        def unreadable_walk(*args, **kwargs):
            kwargs["onerror"](OSError("permission denied"))
            yield None
        with mock.patch.object(bench.os, "walk", side_effect=unreadable_walk):
            with self.assertRaisesRegex(ValueError, "Cannot inspect complete"):
                bench.mark_owned_run(self.baseline, 1, "a" * 40)
        self.assertFalse((runtime / ".pl4-benchmark-owned").exists())

    def test_invalid_marker_never_authorizes_deletion(self):
        runtime = self.baseline / "run"
        runtime.mkdir()
        source = "a" * 40
        saved = runtime / "level.dat"
        saved.write_bytes(b"keep")
        bench.mark_owned_run(self.baseline, 1, source)
        (runtime / ".pl4-benchmark-owned").write_text("{invalid")
        with self.assertRaisesRegex(ValueError, "invalid benchmark ownership marker"):
            bench.replace_owned_run(self.baseline, 1, source)
        self.assertEqual(saved.read_bytes(), b"keep")

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




class NativePreflightTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.roots = [self.root / 'baseline', self.root / 'candidate']
        self.registry = []
        self.original = ('public static void register(RegisterGameTestsEvent e)'
                         '{e.register(PLGameTests.class);}')
        for root in self.roots:
            (root / '.git').mkdir(parents=True)
            (root / 'BUILD_STATUS.json').write_text(
                json.dumps({'minecraft': '1.21.1'}), encoding='utf-8')
            path = root / 'src/portTest/java/net/foundations/pl4/PLGameTests.java'
            path.parent.mkdir(parents=True)
            path.write_text(self.original, encoding='utf-8')
            self.registry.append(path)

    def preflight(self, shas=None):
        shas = shas or ['a' * 40, 'b' * 40]
        def git_head(command, cwd, text):
            self.assertEqual(command, ['git', 'rev-parse', 'HEAD'])
            return shas[self.roots.index(cwd)] + '\n'
        with mock.patch.object(bench.subprocess, 'check_output', side_effect=git_head):
            return bench.preflight_native_checkouts(self.roots)

    def assert_untouched(self):
        for path in self.registry:
            self.assertEqual(path.read_text(encoding='utf-8'), self.original)
            self.assertFalse(path.with_name('ReleasePerformanceGameTests.java').exists())

    def test_distinct_exact_revisions_are_planned_without_writing(self):
        plans = self.preflight()
        self.assertEqual([p['source'] for p in plans], ['a' * 40, 'b' * 40])
        self.assertTrue(all('ReleasePerformanceGameTests.class' in
                            p['registration'] for p in plans))
        self.assert_untouched()

    def test_same_source_is_rejected_before_any_checkout_mutation(self):
        with self.assertRaisesRegex(ValueError, 'distinct source commits'):
            self.preflight(['a' * 40, 'a' * 40])
        self.assert_untouched()

    def test_bad_second_target_does_not_dirty_first_checkout(self):
        (self.roots[1] / 'BUILD_STATUS.json').write_text(
            json.dumps({'minecraft': '26.3'}), encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'Exact Minecraft 1.21.1'):
            self.preflight()
        self.assert_untouched()

    def test_ambiguous_second_registration_does_not_dirty_first(self):
        self.registry[1].write_text(self.original + '\n' + self.original,
                                    encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'ambiguous isolated registration'):
            self.preflight()
        self.assertEqual(self.registry[0].read_text(encoding='utf-8'), self.original)
        self.assertFalse(self.registry[0].with_name(
            'ReleasePerformanceGameTests.java').exists())

    def test_missing_second_registry_does_not_dirty_first(self):
        self.registry[1].unlink()
        with self.assertRaisesRegex(ValueError, 'Missing isolated GameTest'):
            self.preflight()
        self.assert_untouched_first()

    def test_existing_second_probe_report_is_preserved_before_mutation(self):
        logs = self.roots[1] / 'verification-logs'
        logs.mkdir()
        stale = logs / 'performance-probe.json'
        stale.write_text('{"stale": true}', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'Pre-existing native benchmark report directory'):
            self.preflight()
        self.assertEqual(stale.read_text(encoding='utf-8'), '{"stale": true}')
        self.assert_untouched_first()

    def test_symlinked_second_probe_directory_is_not_followed(self):
        outside = self.root / 'retained-logs'
        outside.mkdir()
        retained = outside / 'performance-probe.json'
        retained.write_text('retained', encoding='utf-8')
        (self.roots[1] / 'verification-logs').symlink_to(
            outside, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, 'Pre-existing native benchmark report directory'):
            self.preflight()
        self.assertEqual(retained.read_text(encoding='utf-8'), 'retained')
        self.assert_untouched_first()

    def test_existing_second_fixture_is_preserved_before_mutation(self):
        fixture = self.registry[1].with_name('ReleasePerformanceGameTests.java')
        fixture.write_text('// retained user fixture', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'Existing performance fixture'):
            self.preflight()
        self.assertEqual(fixture.read_text(encoding='utf-8'), '// retained user fixture')
        self.assert_untouched_first()

    def test_symlinked_second_fixture_is_not_overwritten(self):
        retained = self.root / 'retained-fixture.java'
        retained.write_text('// retained source', encoding='utf-8')
        fixture = self.registry[1].with_name('ReleasePerformanceGameTests.java')
        fixture.symlink_to(retained)
        with self.assertRaisesRegex(ValueError, 'Existing performance fixture'):
            self.preflight()
        self.assertEqual(retained.read_text(encoding='utf-8'), '// retained source')
        self.assert_untouched_first()

    def test_linked_second_registry_does_not_overwrite_external_source(self):
        outside = self.root / 'retained-registration.java'
        outside.write_text(self.original, encoding='utf-8')
        self.registry[1].unlink()
        self.registry[1].symlink_to(outside)
        with self.assertRaisesRegex(ValueError, 'Linked native benchmark checkout path'):
            self.preflight()
        self.assertEqual(outside.read_text(encoding='utf-8'), self.original)
        self.assert_untouched_first()

    def test_linked_second_registry_parent_does_not_overwrite_external_source(self):
        parent = self.registry[1].parent
        outside = self.root / 'retained-registry'
        parent.rename(outside)
        parent.symlink_to(outside, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, 'Linked native benchmark checkout path'):
            self.preflight()
        self.assertEqual((outside / 'PLGameTests.java').read_text(encoding='utf-8'),
                         self.original)
        self.assert_untouched_first()

    def test_ambiguous_registration_locations_rejected_before_mutation(self):
        extra = self.roots[1] / 'src/main/java/net/foundations/pl4/PLGameTests.java'
        extra.parent.mkdir(parents=True)
        extra.write_text(self.original, encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'Ambiguous isolated GameTest registration locations'):
            self.preflight()
        self.assertEqual(extra.read_text(encoding='utf-8'), self.original)
        self.assert_untouched_first()

    def test_linked_build_status_cannot_supply_target_identity(self):
        outside = self.root / 'retained-build-status.json'
        outside.write_text(json.dumps({'minecraft': '1.21.1'}), encoding='utf-8')
        status = self.roots[1] / 'BUILD_STATUS.json'
        status.unlink()
        status.symlink_to(outside)
        with self.assertRaisesRegex(ValueError, 'Linked native benchmark checkout path'):
            self.preflight()
        self.assertEqual(json.loads(outside.read_text())['minecraft'], '1.21.1')
        self.assert_untouched_first()

    def assert_untouched_first(self):
        self.assertEqual(self.registry[0].read_text(encoding='utf-8'), self.original)
        self.assertFalse(self.registry[0].with_name(
            'ReleasePerformanceGameTests.java').exists())

def sample_native_report():
    return {
        "source_commit": "a" * 40, "trial": "1",
        "java": "25.0.5", "java_vendor": "Eclipse Adoptium",
        "vm_name": "OpenJDK 64-Bit Server VM",
        "gc_collectors": ["G1 Old Generation", "G1 Young Generation"],
        "os": "Linux", "os_arch": "amd64",
        "processors": 8, "heap_initial_bytes": 2147483648,
        "heap_max_bytes": 2147483648,
        "workloads": [{
            "network": "wired", "host_count": 4096, "endpoint_count": 128,
            "measurements": [{
                "operation": "lookup", "operations_per_round": 10000,
                "warmup_operations": 2000,
                "elapsed_ns": [300, 400, 500],
                "cpu_ns": [100, 200, 300],
                "allocated_bytes": [0, 16, 32],
            }],
        }],
    }


class NativeReportValidationTests(unittest.TestCase):
    def test_valid_native_report_matches_across_trials(self):
        report = sample_native_report()
        schema = bench.validate_native_report(report, "a" * 40, 1)
        self.assertEqual(schema, bench.validate_native_report(report, "a" * 40, 1, schema))

    def test_malformed_native_reports_are_rejected(self):
        edits = [
            ("wrong source", lambda r: r.update(source_commit="b" * 40)),
            ("wrong trial", lambda r: r.update(trial="2")),
            ("missing workloads", lambda r: r.update(workloads=[])),
            ("duplicate network", lambda r: r["workloads"].append(copy.deepcopy(r["workloads"][0]))),
            ("missing operation", lambda r: r["workloads"][0]["measurements"][0].update(operation="")),
            ("duplicate operation", lambda r: r["workloads"][0]["measurements"].append(copy.deepcopy(r["workloads"][0]["measurements"][0]))),
            ("bool operations", lambda r: r["workloads"][0]["measurements"][0].update(operations_per_round=True)),
            ("zero operations", lambda r: r["workloads"][0]["measurements"][0].update(operations_per_round=0)),
            ("bool hosts", lambda r: r["workloads"][0].update(host_count=True)),
            ("missing cpu", lambda r: r["workloads"][0]["measurements"][0].pop("cpu_ns")),
            ("short cpu", lambda r: r["workloads"][0]["measurements"][0].update(cpu_ns=[1])),
            ("unequal rounds", lambda r: r["workloads"][0]["measurements"][0].update(cpu_ns=[1, 2, 3, 4])),
            ("negative allocations", lambda r: r["workloads"][0]["measurements"][0].update(allocated_bytes=[0, -1, 2])),
            ("bool cpu", lambda r: r["workloads"][0]["measurements"][0].update(cpu_ns=[1, True, 2])),
            ("zero elapsed", lambda r: r["workloads"][0]["measurements"][0].update(elapsed_ns=[1, 0, 3])),
        ]
        for name, edit in edits:
            with self.subTest(case=name):
                report = sample_native_report()
                edit(report)
                with self.assertRaises(ValueError):
                    bench.validate_native_report(report, "a" * 40, 1)

    def test_cross_revision_measurement_schema_must_match(self):
        schema = bench.validate_native_report(sample_native_report(), "a" * 40, 1)
        report = sample_native_report()
        report["workloads"][0]["measurements"][0]["operations_per_round"] = 100
        with self.assertRaisesRegex(ValueError, "differ"):
            bench.validate_native_report(report, "a" * 40, 1, schema)

class NativeTrialFinalizationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.runtime = self.root / "run"
        self.runtime.mkdir()
        (self.runtime / "level.dat").write_bytes(b"benchmark-generated world")
        self.source = "a" * 40

    def write_report(self, parent):
        parent.mkdir(parents=True, exist_ok=True)
        path = parent / "performance-probe.json"
        path.write_text(json.dumps(sample_native_report()), encoding="utf-8")
        return path

    def test_report_inside_run_is_removed_before_inventory_is_sealed(self):
        report_path = self.write_report(self.runtime / "verification-logs")
        result = self.root / "trial.json"
        report, schema = bench.finish_native_trial(
            self.root, report_path, result, self.source, 1, "baseline", "f" * 64)
        self.assertEqual(report["label"], "baseline")
        self.assertIn(("wired", "lookup"), schema)
        self.assertFalse(report_path.exists())
        self.assertTrue(result.is_file())
        marker = json.loads((self.runtime / ".pl4-benchmark-owned").read_text())
        self.assertEqual(marker["inventory_sha256"], bench.inventory_digest(self.runtime))
        bench.replace_owned_run(self.root, 1, self.source)
        self.assertFalse(self.runtime.exists())

    def test_report_outside_run_still_seals_and_preserves_results(self):
        report_path = self.write_report(self.root / "verification-logs")
        result = self.root / "trial.json"
        bench.finish_native_trial(
            self.root, report_path, result, self.source, 1, "candidate", "f" * 64)
        self.assertFalse(report_path.exists())
        self.assertEqual(json.loads(result.read_text())["label"], "candidate")
        bench.replace_owned_run(self.root, 1, self.source)
        self.assertTrue(result.exists())

    def test_invalid_report_never_seals_or_deletes_generated_world(self):
        report_path = self.write_report(self.runtime / "verification-logs")
        malformed = sample_native_report()
        malformed["trial"] = "2"
        report_path.write_text(json.dumps(malformed))
        with self.assertRaisesRegex(ValueError, "source/trial mismatch"):
            bench.finish_native_trial(
                self.root, report_path, self.root / "trial.json", self.source,
                1, "baseline", "f" * 64)
        self.assertTrue(report_path.exists())
        self.assertTrue((self.runtime / "level.dat").exists())
        self.assertFalse((self.runtime / ".pl4-benchmark-owned").exists())
        self.assertFalse((self.root / "trial.json").exists())

    def test_linked_report_is_rejected_without_reading_or_deleting_target(self):
        outside = self.root / "retained-report.json"
        outside.write_text(json.dumps(sample_native_report()))
        link_dir = self.runtime / "verification-logs"
        link_dir.mkdir()
        report_path = link_dir / "performance-probe.json"
        report_path.symlink_to(outside)
        with self.assertRaisesRegex(ValueError, "linked native benchmark report"):
            bench.finish_native_trial(
                self.root, report_path, self.root / "trial.json", self.source,
                1, "baseline", "f" * 64)
        self.assertTrue(outside.exists())
        self.assertTrue(report_path.is_symlink())
        self.assertFalse((self.runtime / ".pl4-benchmark-owned").exists())


    def test_unrelated_nested_report_is_preserved(self):
        report_path = self.write_report(
            self.root / "other-world" / "verification-logs")
        with self.assertRaisesRegex(ValueError, "outside the expected probe directories"):
            bench.finish_native_trial(
                self.root, report_path, self.root / "trial.json", self.source,
                1, "baseline", "f" * 64)
        self.assertTrue(report_path.is_file())
        self.assertFalse((self.root / "trial.json").exists())
        self.assertFalse((self.runtime / ".pl4-benchmark-owned").exists())

    def test_symlinked_report_parent_is_preserved(self):
        retained = self.root / "retained-reports"
        outside = self.write_report(retained)
        linked_dir = self.root / "verification-logs"
        linked_dir.symlink_to(retained, target_is_directory=True)
        report_path = linked_dir / "performance-probe.json"
        with self.assertRaisesRegex(ValueError, "Linked native benchmark report"):
            bench.finish_native_trial(
                self.root, report_path, self.root / "trial.json", self.source,
                1, "baseline", "f" * 64)
        self.assertEqual(outside.read_text(encoding="utf-8"),
                         json.dumps(sample_native_report()))
        self.assertTrue(linked_dir.is_symlink())
        self.assertFalse((self.runtime / ".pl4-benchmark-owned").exists())


class NativeReportLocatorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def report(self, relative):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(sample_native_report()), encoding="utf-8")
        return path

    def test_exact_generated_run_report_is_selected(self):
        path = self.report("run/verification-logs/performance-probe.json")
        self.assertEqual(bench.locate_native_probe_report(self.root), path)

    def test_exact_checkout_report_is_selected(self):
        path = self.report("verification-logs/performance-probe.json")
        self.assertEqual(bench.locate_native_probe_report(self.root), path)

    def test_unrelated_nested_world_report_is_ignored_and_preserved(self):
        valid = self.report("run/verification-logs/performance-probe.json")
        retained = self.report("saved-world/verification-logs/performance-probe.json")
        self.assertEqual(bench.locate_native_probe_report(self.root), valid)
        self.assertTrue(retained.is_file())

    def test_two_generated_reports_are_ambiguous_and_preserved(self):
        first = self.report("run/verification-logs/performance-probe.json")
        second = self.report("verification-logs/performance-probe.json")
        with self.assertRaisesRegex(ValueError, "Expected one native probe report, found 2"):
            bench.locate_native_probe_report(self.root)
        self.assertTrue(first.is_file())
        self.assertTrue(second.is_file())

    def test_missing_report_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Expected one native probe report, found 0"):
            bench.locate_native_probe_report(self.root)

    def test_symlinked_generated_report_parent_is_rejected(self):
        retained = self.report("retained/performance-probe.json")
        (self.root / "verification-logs").symlink_to(
            retained.parent, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "Linked native benchmark report"):
            bench.locate_native_probe_report(self.root)
        self.assertTrue(retained.is_file())

    def test_symlinked_generated_report_is_rejected(self):
        retained = self.report("retained.json")
        linked = self.root / "verification-logs" / "performance-probe.json"
        linked.parent.mkdir()
        linked.symlink_to(retained)
        with self.assertRaisesRegex(ValueError, "Linked native benchmark report"):
            bench.locate_native_probe_report(self.root)
        self.assertTrue(retained.is_file())



class NativeEnvironmentComparabilityTests(unittest.TestCase):
    def test_matching_native_jvm_and_heap_across_trials(self):
        report = sample_native_report()
        environment = bench.validate_native_environment(report, copy.deepcopy(report))
        self.assertEqual(environment["heap_max_bytes"], 2147483648)
        self.assertEqual(environment["heap_initial_bytes"], environment["heap_max_bytes"])
        self.assertEqual(environment["processors"], 8)

    def test_unpinned_equal_heap_profiles_cannot_pass_native_gate(self):
        report = sample_native_report()
        report["heap_initial_bytes"] = 536870912
        # Comparing two equally auto-sized heaps is not fixed-heap evidence.
        self.assertEqual(
            bench.validate_native_environment(report, copy.deepcopy(report))["heap_initial_bytes"],
            536870912)
        with self.assertRaisesRegex(ValueError, "fixed heap"):
            bench.validate_native_environment(
                report, copy.deepcopy(report), require_fixed_heap=True)

    def test_unpinned_heap_preserves_world_and_original_report(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            logs = root / "run" / "verification-logs"
            logs.mkdir(parents=True)
            world = root / "run" / "level.dat"
            world.write_bytes(b"preserve native test world")
            report = sample_native_report()
            report["heap_initial_bytes"] = 536870912
            native_report = logs / "performance-probe.json"
            native_report.write_text(json.dumps(report), encoding="utf-8")
            result = root / "trial.json"
            with self.assertRaisesRegex(ValueError, "fixed heap"):
                bench.finish_native_trial(
                    root, native_report, result, "a" * 40, 1, "candidate", "f" * 64,
                    reference_environment=copy.deepcopy(report))
            self.assertEqual(world.read_bytes(), b"preserve native test world")
            self.assertEqual(json.loads(native_report.read_text()), report)
            self.assertFalse(result.exists())
            self.assertFalse((root / "run" / ".pl4-benchmark-owned").exists())

    def test_different_runtime_conditions_fail_comparability(self):
        for field, changed in (
                ("java", "25.0.6"), ("java_vendor", "Different Vendor"),
                ("vm_name", "Different VM"), ("gc_collectors", ["ZGC Cycles", "ZGC Pauses"]),
                ("os", "Windows"), ("os_arch", "aarch64"),
                ("processors", 4), ("heap_initial_bytes", 268435456),
                ("heap_max_bytes", 1073741824)):
            with self.subTest(field=field):
                reference = sample_native_report()
                current = copy.deepcopy(reference)
                current[field] = changed
                with self.assertRaisesRegex(ValueError, "environment differs across trials"):
                    bench.validate_native_environment(current, reference)

    def test_missing_boolean_or_invalid_heap_environment_fails(self):
        for field, value in (
                ("java", ""), ("java_vendor", ""), ("vm_name", None),
                ("gc_collectors", []), ("gc_collectors", ["G1 Young Generation", "G1 Old Generation"]),
                ("gc_collectors", ["G1 Old Generation", "G1 Old Generation"]),
                ("gc_collectors", ["", "G1 Young Generation"]),
                ("gc_collectors", "G1"), ("os_arch", None), ("processors", True),
                ("heap_initial_bytes", 0), ("heap_max_bytes", -1),
                ("heap_max_bytes", 1)):
            with self.subTest(field=field, value=value):
                report = sample_native_report()
                report[field] = value
                with self.assertRaises(ValueError):
                    bench.validate_native_environment(report)

    def test_mismatched_environment_preserves_world_and_report(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            runtime = root / "run"
            logs = runtime / "verification-logs"
            logs.mkdir(parents=True)
            world = runtime / "level.dat"
            world.write_bytes(b"retained generated world")
            path = logs / "performance-probe.json"
            report = sample_native_report()
            path.write_text(json.dumps(report), encoding="utf-8")
            reference = copy.deepcopy(report)
            reference["heap_max_bytes"] *= 2
            result = root / "trial.json"
            with self.assertRaisesRegex(ValueError, "environment differs across trials"):
                bench.finish_native_trial(
                    root, path, result, "a" * 40, 1, "candidate", "f" * 64,
                    reference_environment=reference)
            self.assertEqual(world.read_bytes(), b"retained generated world")
            self.assertTrue(path.is_file())
            self.assertFalse(result.exists())
            self.assertFalse((runtime / ".pl4-benchmark-owned").exists())


    def test_gc_mismatch_preserves_world_and_unmodified_native_report(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            logs = root / "run" / "verification-logs"
            logs.mkdir(parents=True)
            world = root / "run" / "level.dat"
            world.write_bytes(b"retained world")
            native_report = logs / "performance-probe.json"
            report = sample_native_report()
            native_report.write_text(json.dumps(report), encoding="utf-8")
            reference = copy.deepcopy(report)
            reference["gc_collectors"] = ["ZGC Cycles", "ZGC Pauses"]
            with self.assertRaisesRegex(ValueError, "gc_collectors"):
                bench.finish_native_trial(
                    root, native_report, root / "trial.json", "a" * 40,
                    1, "candidate", "f" * 64, reference_environment=reference)
            self.assertEqual(world.read_bytes(), b"retained world")
            self.assertEqual(json.loads(native_report.read_text()), report)
            self.assertFalse((root / "trial.json").exists())
            self.assertFalse((root / "run" / ".pl4-benchmark-owned").exists())


class NativeTrialEnvironmentTests(unittest.TestCase):
    def test_default_heap_is_pinned_for_native_game_jvm(self):
        env = bench.native_trial_environment("a" * 40, 1, {})
        self.assertEqual(env["JAVA_TOOL_OPTIONS"], "-Xms2048m -Xmx2048m")
        self.assertEqual(env["PL4_BENCHMARK_SOURCE"], "a" * 40)
        self.assertEqual(env["PL4_BENCHMARK_TRIAL"], "1")

    def test_configured_heap_and_unrelated_options_are_preserved(self):
        inherited = {"PL4_BENCHMARK_HEAP_MB": "1024",
                     "JAVA_TOOL_OPTIONS": "-Duser.language=en"}
        env = bench.native_trial_environment("b" * 40, 3, inherited)
        self.assertEqual(env["JAVA_TOOL_OPTIONS"],
                         "-Duser.language=en -Xms1024m -Xmx1024m")
        self.assertNotIn("PL4_BENCHMARK_HEAP_MB", env)
        self.assertEqual(inherited["PL4_BENCHMARK_HEAP_MB"], "1024")

    def test_invalid_heap_sizes_are_rejected(self):
        for value in ("", "0", "-1", "511", "16385", "1g", "100.0", " 1024"):
            with self.subTest(value=value), self.assertRaisesRegex(ValueError, "HEAP_MB"):
                bench.native_trial_environment("a" * 40, 1,
                                               {"PL4_BENCHMARK_HEAP_MB": value})

    def test_conflicting_inherited_heap_flags_are_rejected(self):
        for key in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
            for flag in ("-Xms1g", "-Xmx2048m",
                         "-XX:InitialHeapSize=1024m", "-XX:MaxRAMPercentage=25"):
                with self.subTest(key=key, flag=flag), self.assertRaisesRegex(
                        ValueError, "Conflicting inherited JVM heap option"):
                    bench.native_trial_environment("a" * 40, 1, {key: flag})

    def test_unrelated_inherited_environment_is_not_modified(self):
        inherited = {"JAVA_TOOL_OPTIONS": "-Dfoo=bar",
                     "PL4_BENCHMARK_HEAP_MB": "512"}
        bench.native_trial_environment("a" * 40, 1, inherited)
        self.assertEqual(inherited, {"JAVA_TOOL_OPTIONS": "-Dfoo=bar",
                                     "PL4_BENCHMARK_HEAP_MB": "512"})



class NativeEnvironmentPreflightTests(unittest.TestCase):
    def plans(self):
        return [{"label": "baseline", "source": "a" * 40},
                {"label": "candidate", "source": "b" * 40}]

    def test_all_six_environments_are_prepared_before_native_execution(self):
        inherited = {"PL4_BENCHMARK_HEAP_MB": "1024",
                     "JAVA_TOOL_OPTIONS": "-Duser.language=en"}
        environments = bench.preflight_native_trial_environments(self.plans(), inherited)
        self.assertEqual(len(environments), 6)
        for trial in range(1, 4):
            for label, source in (("baseline", "a" * 40), ("candidate", "b" * 40)):
                env = environments[(label, trial)]
                self.assertEqual(env["PL4_BENCHMARK_SOURCE"], source)
                self.assertEqual(env["PL4_BENCHMARK_TRIAL"], str(trial))
                self.assertEqual(env["JAVA_TOOL_OPTIONS"],
                                 "-Duser.language=en -Xms1024m -Xmx1024m")
        self.assertEqual(inherited["PL4_BENCHMARK_HEAP_MB"], "1024")
        environments[("baseline", 1)]["JAVA_TOOL_OPTIONS"] = "changed"
        self.assertNotEqual(environments[("candidate", 1)]["JAVA_TOOL_OPTIONS"],
                            "changed")

    def test_bad_heap_preflight_does_not_mutate_checkout_or_create_results(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            baseline, candidate, output = (root / "baseline", root / "candidate",
                                           root / "results")
            for checkout in (baseline, candidate):
                checkout.mkdir()
                (checkout / "PLGameTests.java").write_text("original source")
            plans = [
                {"label": "baseline", "source": "a" * 40,
                 "root": baseline, "registry": baseline / "PLGameTests.java",
                 "registration": "modified source"},
                {"label": "candidate", "source": "b" * 40,
                 "root": candidate, "registry": candidate / "PLGameTests.java",
                 "registration": "modified source"},
            ]
            with (mock.patch("sys.argv", ["benchmark", str(baseline),
                                         str(candidate), str(output)]),
                  mock.patch.object(bench, "require_disposable_workspaces",
                                    return_value=([baseline, candidate], output)),
                  mock.patch.object(bench, "preflight_native_checkouts",
                                    return_value=plans),
                  mock.patch.dict(bench.os.environ,
                                  {"PL4_BENCHMARK_HEAP_MB": "not-a-number"}, clear=True)):
                with self.assertRaisesRegex(ValueError, "HEAP_MB"):
                    bench.main()
            self.assertFalse(output.exists())
            for checkout in (baseline, candidate):
                self.assertEqual((checkout / "PLGameTests.java").read_text(),
                                 "original source")
                self.assertFalse((checkout / "ReleasePerformanceGameTests.java").exists())
                self.assertFalse((checkout / "verification-logs").exists())

    def test_conflicting_inherited_java_options_fail_all_trial_preflight(self):
        for key in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
            with self.subTest(key=key), self.assertRaisesRegex(
                    ValueError, "Conflicting inherited JVM heap option"):
                bench.preflight_native_trial_environments(
                    self.plans(), {key: "-Xmx512m"})

    def test_invalid_heap_size_rejects_preflight_without_mutating_input(self):
        inherited = {"PL4_BENCHMARK_HEAP_MB": "16385"}
        with self.assertRaisesRegex(ValueError, "HEAP_MB"):
            bench.preflight_native_trial_environments(self.plans(), inherited)
        self.assertEqual(inherited, {"PL4_BENCHMARK_HEAP_MB": "16385"})


if __name__ == "__main__":
    unittest.main()
