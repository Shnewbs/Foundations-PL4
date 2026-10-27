import json,tempfile,unittest
from pathlib import Path
from hourly_builds import validate_live_evidence_cases,HANDHELD_ADMISSION_CASES
class HandheldAdmissionTests(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory();self.addCleanup(self.t.cleanup);self.root=Path(self.t.name)
  (self.root/'run-legacy').mkdir();(self.root/'verification-logs').mkdir()
  self.rows=[dict(name=n,passed=True) for n in HANDHELD_ADMISSION_CASES]
  self.lines=['2026-10-26 23:00:00 [INFO] [STDOUT] PASS '+n for n in HANDHELD_ADMISSION_CASES]
 def check(self,mc='1.12.2'):
  (self.root/'run-legacy/legacy-scenarios.json').write_text(json.dumps(dict(results=self.rows)))
  (self.root/'verification-logs/native.log').write_text('\n'.join(self.lines))
  validate_live_evidence_cases(self.root,mc,HANDHELD_ADMISSION_CASES)
 def test_complete112(self):self.check()
 def test_complete164(self):self.check('1.6.4')
 def test_missing(self):
  self.rows.pop()
  with self.assertRaises(ValueError):self.check()
 def test_failed(self):
  self.rows[0]['passed']=False
  with self.assertRaises(ValueError):self.check()
 def test_duplicate(self):
  self.rows.append(self.rows[0])
  with self.assertRaises(ValueError):self.check()
 def test_missing164(self):
  self.lines.pop()
  with self.assertRaises(ValueError):self.check('1.6.4')
if __name__=='__main__':unittest.main()
