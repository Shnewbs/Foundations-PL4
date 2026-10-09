"""Dependency-free tests for fail-closed migration helpers, not native port tests."""
from pathlib import Path
import unittest
from prepare_02a_ports import between, plan, replace_once, server_api


class MigrationTests(unittest.TestCase):
    def test_replace_exactly_one(self):
        self.assertEqual(replace_once('before OLD after', 'OLD', 'NEW'), 'before NEW after')

    def test_missing_anchor_rejected(self):
        with self.assertRaises(ValueError):
            replace_once('before after', 'OLD', 'NEW')

    def test_ambiguous_anchor_rejected(self):
        with self.assertRaises(ValueError):
            replace_once('OLD OLD', 'OLD', 'NEW')

    def test_section_preserves_unrelated_text(self):
        text = 'prefix START payload END suffix'
        self.assertEqual(between(text, 'START', 'END'), 'START payload ')
        self.assertEqual(replace_once(text, between(text, 'START', 'END'), 'UPDATED '), 'prefix UPDATED END suffix')

    def test_invalid_sections_rejected(self):
        for text in ['END START', 'START START END', 'START only']:
            with self.subTest(text=text), self.assertRaises(ValueError):
                between(text, 'START', 'END')

    def test_only_reviewed_server_accessors_changed(self):
        self.assertEqual(server_api('p.serverLevel();w.dimension().location();x.location();'), 'p.level();w.dimension().identifier();x.location();')

    def test_unreviewed_targets_rejected_before_io(self):
        for version in ['1.6.4', '1.7.10', '1.20.1', '26.4']:
            with self.subTest(version=version), self.assertRaises(ValueError):
                plan(Path('missing-source'), Path('missing-target'), version)


if __name__ == '__main__':
    unittest.main()
