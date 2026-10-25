"""Reject omitted PL4 fixtures and unattributed or ambiguous native success totals."""
import unittest
from ports.native_26_coverage import coverage


def log(pl4=192, other=1, total=193):
    return (f"Running test environment 'foundations_pl4:r5gametests' batch 0 ({pl4} tests)...\n"
            f"Running test environment 'minecraft:default' batch 0 ({other} tests)...\n"
            f'All {total} required tests passed :)\n')


class NativeCoverageTests(unittest.TestCase):
    def test_default_environment_is_separate_from_pl4(self):
        result=coverage(log(),192)
        self.assertEqual((result['pl4_fixtures'],result['other_fixtures'],result['total_required_passed']),(192,1,193))
        self.assertEqual(result['environment_batches'][1]['environment'],'minecraft:default')
        self.assertEqual(len(result['native_log_sha256']),64)

    def test_equal_overall_count_cannot_hide_missing_pl4_test(self):
        with self.assertRaisesRegex(ValueError,'PL4 native coverage'):
            coverage(log(191,2),192)

    def test_unattributed_extra_success_rejected(self):
        with self.assertRaisesRegex(ValueError,'attributed'):
            coverage(log(total=194),192)

    def test_duplicate_batch_and_repeated_runs_rejected(self):
        for text in (log()+log(),log()+"Running test environment 'minecraft:default' batch 0 (1 tests)..."):
            with self.assertRaises(ValueError):coverage(text,192)

    def test_no_success_marker_or_no_pl4_run_rejected(self):
        for text in (log().split('All')[0],log(0,193), ''):
            with self.assertRaises(ValueError):coverage(text,192)

    def test_multiple_batches_and_other_namespaces_attributed(self):
        text=("Running test environment 'foundations_pl4:r5gametests' batch 0 (100 tests)...\n"
              "Running test environment 'foundations_pl4:r5gametests' batch 1 (92 tests)...\n"
              "Running test environment 'loader:smoke' batch 0 (2 tests)...\nAll 194 required tests passed")
        self.assertEqual(coverage(text,192)['other_fixtures'],2)


if __name__=='__main__':unittest.main()
