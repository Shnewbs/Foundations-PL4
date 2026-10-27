import json,tempfile,unittest
from pathlib import Path
from verify_legacy_item_restart import retain_phase_report,digest,require_legacy_decode_placements

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
class NativeDecodePlacementAdmissionTests(unittest.TestCase):
    def good(self,x):
        return dict(x=x,y=200,z=32,stage='complete',placement_flags=2,
            set_block_changed=True,expected_block=3501,actual_block=3501,
            tile_class='net.foundations.pl4.legacy.LegacyTile',tile_invalid=False,
            block_exists_after=True)
    def test_exact_hosts_accepted_without_retries(self):
        report=dict(suite='decode-raw',placements=[self.good(64),self.good(80),dict(x=200)])
        self.assertIsNone(require_legacy_decode_placements(report,'write','1.6.4'))
        self.assertIsNone(require_legacy_decode_placements(report,'verify','1.6.4'))
        self.assertIsNone(require_legacy_decode_placements(report,'write','1.12.2'))
    def test_gravel_overwrite_rejected_even_after_set_block_reports_success(self):
        other=self.good(80);other['stage']='placement_assertion'
        other['actual_block']=13;other['tile_class']=None
        report=dict(suite='decode-raw',placements=[self.good(64),other])
        with self.assertRaisesRegex(ValueError,'x=80'):
            require_legacy_decode_placements(report,'write','1.6.4')
    def test_missing_duplicate_or_wrong_flags_rejected(self):
        for rows in ([self.good(64)],[self.good(64),self.good(64)],
                     [dict(self.good(64),placement_flags=3),self.good(80)],
                     [dict(self.good(64),tile_invalid=True),self.good(80)]):
            with self.subTest(rows=rows),self.assertRaises(ValueError):
                require_legacy_decode_placements(dict(suite='decode-raw',placements=rows),'crash-write','1.6.4')

if __name__=='__main__':unittest.main()
