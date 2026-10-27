import json,tempfile,unittest
from pathlib import Path
from verify_legacy_item_restart import retain_phase_report,digest

class PhaseRetentionTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name);self.work=self.root/'work';self.logs=self.root/'logs'
        self.work.mkdir();self.logs.mkdir();self.log=self.logs/'native.log';self.log.write_text('native failure\n')
        self.source=self.work/'item-decode-missing-restart-write.json'
    def retain(self):return retain_phase_report(self.work,self.logs,'item-decode-missing-restart','write',71,1,self.log)
    def observation(self):return json.loads((self.logs/'item-decode-missing-restart-write-process.json').read_text())
    def test_failed_phase_preserves_exact_raw_bytes(self):
        raw=b'{ "passed": false, "assertions": [], "placements": [{"x":64,"tile_class":null}] }\n'
        self.source.write_bytes(raw);self.assertIs(self.retain()['passed'],False)
        saved=self.logs/self.source.name;self.assertEqual(saved.read_bytes(),raw)
        self.assertEqual(self.observation(),dict(phase='write',pid=71,exit_code=1,log_sha256=digest(self.log),report_present=True,report_sha256=digest(saved)))
    def test_invalid_json_is_retained_before_parse_failure(self):
        self.source.write_bytes(b'{broken')
        with self.assertRaises(json.JSONDecodeError):self.retain()
        self.assertEqual((self.logs/self.source.name).read_bytes(),b'{broken')
        self.assertIs(self.observation()['report_present'],True)
    def test_absent_report_preserves_process_facts(self):
        with self.assertRaisesRegex(ValueError,'Missing restart phase report'):self.retain()
        self.assertIs(self.observation()['report_present'],False);self.assertIsNone(self.observation()['report_sha256'])
    def test_success_report_is_preserved_without_granting_clearance(self):
        self.source.write_text('{"passed":true,"assertions":["one"]}')
        self.assertEqual(self.retain(),dict(passed=True,assertions=['one']))
        self.assertEqual(self.observation()['exit_code'],1)
if __name__=='__main__':unittest.main()
