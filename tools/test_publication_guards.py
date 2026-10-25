"""Exercise actual generated workflow readers and preserve native validation."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import yaml
from ports.guard_development_publication import guarded, READ, GUARD

FIXTURE = '''name: Native
on:
  push:
    branches: [mc/1.20.1]
jobs:
  verify:
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - name: Validate
        run: bash gradlew build runGameTestServer
      - name: Publish
        if: ${{ success() }}
        run: gh release create "$tag" release-files/*
'''

class PublicationGuardTests(unittest.TestCase):
    def test_native_steps_and_existing_condition_preserved(self):
        result = guarded(FIXTURE)
        self.assertIn('run: bash gradlew build runGameTestServer', result)
        self.assertIn(GUARD + ' && (success())', result)
        self.assertEqual(guarded(result), result)
        steps = yaml.safe_load(result)['jobs']['verify']['steps']
        self.assertEqual(len(steps), 4)
        self.assertTrue(steps[0]['with']['fetch-depth'] == 0)

    def test_frozen_publisher_requires_manual_review(self):
        with self.assertRaises(ValueError):
            guarded("      - name: Frozen\n        run: gh release create old-tag\n")

    def test_actual_embedded_reader(self):
        script = yaml.safe_load('steps:\n'+READ)['steps'][0]['run']
        cases = [
            ({'version':'0.2.0-rc.1-dev'}, False),
            ({'version':'1.0-SNAPSHOT'}, False),
            ({'version':'1.0-nightly.2'}, False),
            ({'version':'1.0-rc.1'}, False),
            ({'version':'1.0-rc1','rc_publication_cleared':'true'}, False),
            ({'version':'1.0-rc1','rc_publication_cleared':True}, True),
            ({'version':'1.0-rc1','rc_publication_cleared':True,'release_channel':'development'}, False),
            ({'version':'0.2b-port.1'}, True),
        ]
        for status, allowed in cases:
            with self.subTest(status=status), tempfile.TemporaryDirectory() as d:
                root=Path(d)
                (root/'BUILD_STATUS.json').write_text(json.dumps(status))
                env=dict(os.environ,GITHUB_ENV=str(root/'env'))
                subprocess.run(['bash','-e','-c',script],cwd=root,env=env,check=True,capture_output=True)
                self.assertEqual((root/'env').read_text(), 'PL4_ALLOW_PUBLICATION='+str(allowed).lower()+'\n')

    def test_upgrade_existing_weak_reader(self):
        weak = FIXTURE.replace('      - name: Validate', READ.replace("    require_publication(status)", "    allowed = True")+'      - name: Validate')
        result=guarded(weak)
        self.assertEqual(result.count('name: Read publication maturity'), 1)
        self.assertIn('    require_publication(status)', result)

if __name__ == '__main__':
    unittest.main()
