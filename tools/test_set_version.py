"""Metadata preparation tests; no network or Minecraft dependencies."""
import json
from pathlib import Path
import tempfile
import unittest
from set_version import plan

class VersionTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        edition = 'Minecraft 1.21.1 / NeoForge / Foundations PL4 0.1b'
        files = {
            'build.gradle': "version = '0.1b'\n",
            'docs/releases/0.2a.md': '# 0.2a\nFurther API testing required.\n',
            'docs/FIELD_GUIDE.md': '# Guide\n' + edition + '\nUnchanged content\n',
            'src/main/resources/assets/foundations_pl4/guide/en_us.json': json.dumps({'edition': edition, 'chapters': ['unchanged']}),
            'src/main/java/net/foundations/pl4/PLPackets.java': 'event.registrar("5");',
            'src/main/java/net/foundations/pl4/PLGameTests.java': 'e.register(PLGameTests.class); @GameTest(',
            'BUILD_STATUS.json': json.dumps({'version': '0.1b', 'further_api_testing_required': False}),
            'README.md': '# PL4 0.1b\n',
        }
        for path, text in files.items():
            target = self.root / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text, encoding='utf-8')

    def test_preflight_does_not_write(self):
        before = (self.root / 'build.gradle').read_text()
        changes = plan(self.root, '0.2a')
        self.assertEqual(before, (self.root / 'build.gradle').read_text())
        status = json.loads(changes[self.root / 'BUILD_STATUS.json'])
        self.assertEqual((status['version'], status['payload_protocol'], status['expected_native_tests']), ('0.2a', '5', 1))
        self.assertTrue(status['further_api_testing_required'])
        self.assertEqual(status['release_channel'], 'alpha')
        guide = json.loads(changes[self.root / 'src/main/resources/assets/foundations_pl4/guide/en_us.json'])
        self.assertEqual(guide['chapters'], ['unchanged'])

    def test_prepared_metadata_is_idempotent(self):
        for path, text in plan(self.root, '0.2a').items():
            path.write_text(text, encoding='utf-8')
        self.assertTrue(all(path.read_text(encoding='utf-8') == text for path, text in plan(self.root, '0.2a').items()))

    def test_unsafe_version_rejected(self):
        for version in ['../0.2a', '0.2a;echo bad', '0.2a\n', 'v0.2a']:
            with self.subTest(version=version), self.assertRaises(ValueError):
                plan(self.root, version)

    def test_missing_notes_rejected(self):
        with self.assertRaises(ValueError):
            plan(self.root, '0.3a')

    def test_drift_rejected_before_writes(self):
        (self.root / 'docs/FIELD_GUIDE.md').write_text('Wrong edition')
        with self.assertRaises(ValueError):
            plan(self.root, '0.2a')
        self.assertIn('0.1b', (self.root / 'build.gradle').read_text())

    def test_guide_patch_updates_both_copies_idempotently(self):
        path = self.root / 'src/main/resources/assets/foundations_pl4/guide/en_us.json'
        guide = json.loads(path.read_text())
        guide['chapters'] = [{'id': 'wireless', 'sections': [{'heading': 'Storage', 'body': 'Unchanged content'}]}]
        path.write_text(json.dumps(guide), encoding='utf-8')
        patch = [{'chapter': 'wireless', 'heading': 'Storage', 'body': 'Updated network storage'}]
        (self.root / 'docs/releases/0.2a-guide.json').write_text(json.dumps(patch), encoding='utf-8')
        changes = plan(self.root, '0.2a')
        self.assertEqual(json.loads(changes[path])['chapters'][0]['sections'][0]['body'], 'Updated network storage')
        self.assertIn('Updated network storage', changes[self.root / 'docs/FIELD_GUIDE.md'])
        for target, text in changes.items():
            target.write_text(text, encoding='utf-8')
        self.assertTrue(all(target.read_text(encoding='utf-8') == text for target, text in plan(self.root, '0.2a').items()))

    def test_unknown_guide_section_rejected_before_writes(self):
        path = self.root / 'src/main/resources/assets/foundations_pl4/guide/en_us.json'
        guide = json.loads(path.read_text())
        guide['chapters'] = []
        path.write_text(json.dumps(guide), encoding='utf-8')
        patch = [{'chapter': 'missing', 'heading': 'Storage', 'body': 'No match'}]
        (self.root / 'docs/releases/0.2a-guide.json').write_text(json.dumps(patch), encoding='utf-8')
        with self.assertRaises(ValueError):
            plan(self.root, '0.2a')
        self.assertIn('0.1b', (self.root / 'build.gradle').read_text())

if __name__ == '__main__':
    unittest.main()
