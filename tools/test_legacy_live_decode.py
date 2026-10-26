import copy,unittest
from hourly_builds import validate_live_decode_assertions
class LiveDecodeTests(unittest.TestCase):
 def setUp(self):
  self.report={"phase_reports":{}}
  for phase in ["write","verify","crash-write","crash-verify"]:
   stages=["retained 64"]+(["seed 64","seed 80"] if phase.endswith("write") else [])
   self.report["phase_reports"][phase]={"assertions":[p+s for p in ["live cleared fault retains evidence quarantine ","live cleared fault rejects empty escrow face edit ","live cleared fault NBT reload preserves evidence and count "] for s in stages]}
 def test_complete(self):validate_live_decode_assertions(self.report)
 def test_missing_after_restart(self):
  self.report["phase_reports"]["verify"]["assertions"].pop()
  with self.assertRaises(ValueError):validate_live_decode_assertions(self.report)
 def test_missing_pre_save(self):
  self.report["phase_reports"]["crash-write"]["assertions"].pop()
  with self.assertRaises(ValueError):validate_live_decode_assertions(self.report)
 def test_duplicate_is_not_coverage(self):
  a=self.report["phase_reports"]["crash-verify"]["assertions"];a.append(a[0])
  with self.assertRaises(ValueError):validate_live_decode_assertions(self.report)
if __name__=="__main__":unittest.main()
