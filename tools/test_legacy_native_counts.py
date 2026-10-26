import json,tempfile,unittest
from pathlib import Path
from hourly_builds import validate_legacy_counts
class CountsTests(unittest.TestCase):
    def setUp(self):
        self.t=tempfile.TemporaryDirectory();self.addCleanup(self.t.cleanup);self.root=Path(self.t.name);(self.root/'run-legacy').mkdir();(self.root/'verification-logs').mkdir();self.report=dict(minecraft='1.12.2',total=2,passed=2,failed=0,results=[dict(passed=True),dict(passed=True)]);self.log='PL4 LEGACY SCENARIOS: 2/2';self.native=dict(minecraft='1.12.2')
    def validate(self):
        (self.root/'run-legacy/legacy-scenarios.json').write_text(json.dumps(self.report));(self.root/'verification-logs/native.log').write_text(self.log);validate_legacy_counts(self.root,self.native,2)
    def test_per_case_native(self):self.validate()
    def test_old_counter_only_native(self):
        self.report.pop('results');self.report['minecraft']=self.native['minecraft']='1.6.4';self.validate()
    def test_summary_cannot_hide_actual_count(self):
        self.report['total']=3
        with self.assertRaises(ValueError):self.validate()
    def test_boolean_is_not_a_counter(self):
        self.report['failed']=False
        with self.assertRaises(ValueError):self.validate()
    def test_counter_cannot_hide_failed_case(self):
        self.report['results'][1]['passed']=False
        with self.assertRaises(ValueError):self.validate()
    def test_missing_log_marker(self):
        self.log='no native completion'
        with self.assertRaises(ValueError):self.validate()
    def test_wrong_minecraft(self):
        self.report['minecraft']='1.6.4'
        with self.assertRaises(ValueError):self.validate()
if __name__=='__main__':unittest.main()
