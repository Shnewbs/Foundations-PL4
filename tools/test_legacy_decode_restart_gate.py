import hashlib,json,unittest
import hourly_builds as builds
import test_legacy_restart_gate as common

class DecodeRestartTests(unittest.TestCase):
    def compound(self):
        self.suite='decode-compound';self.report['suite']=self.suite
        logs=self.root/'verification-logs'
        for p in list(logs.iterdir()):p.rename(logs/p.name.replace('decode-raw','decode-compound'))
        for row in self.report['world_archives'].values():row['file']=row['file'].replace('decode-raw','decode-compound')
        for r in self.report['phase_reports'].values():
            r['suite']=self.suite
            for row in r['decode_outcomes']:row.update(context_kind='compound',context_value='rejected-token-int71')
    def test_compound_coherent(self):self.compound();self.validate()
    def test_compound_cannot_be_reported_as_string(self):
        self.compound();self.change_all('context_kind','string')
        with self.assertRaises(ValueError):self.validate()
    def test_compound_lost_payload_report_rejected(self):
        self.compound();self.change_all('context_value','')
        with self.assertRaises(ValueError):self.validate()
    def setUp(self):
        common.RestartProofTests.setUp(self)
        self.suite='decode-raw'
        logs=self.root/'verification-logs'
        for p in list(logs.iterdir()):p.rename(logs/p.name.replace('item-restart','item-decode-raw-restart'))
        for row in self.report['world_archives'].values():row['file']=row['file'].replace('item-restart','item-decode-raw-restart')
        self.report['suite']=self.suite
        for phase,r in self.report['phase_reports'].items():
            r['suite']=self.suite
            r['decode_outcomes']=[dict(host_x=64 if i==0 else 80,host_removed=i==1,original_token=r['tokens'][i],owner='fae1b877-c6b4-4538-a26a-630190b54205',dimension=0,context_kind='int' if i==0 else 'string',context_value='71' if i==0 else 'current-raw-context',sealed_units=8,evidence_marker='first-raw-payload',original_route_known=False) for i in range(2)]
        self.sync()
    def sync(self):
        for phase,r in self.report['phase_reports'].items():
            (self.root/'verification-logs'/('item-'+self.suite+'-restart-'+phase+'.json')).write_text(json.dumps(r))
    def validate(self):
        self.sync()
        builds.validate_decode_restart(self.root,self.report,self.mc,self.sha,self.runtime,dict(write=36,verify=26),self.suite)
    def change_all(self,key,value):
        for r in self.report['phase_reports'].values():r['decode_outcomes'][0][key]=value
    def test_raw_coherent(self):self.validate()
    def test_missing_coherent(self):
        self.suite='decode-missing';self.report['suite']=self.suite
        logs=self.root/'verification-logs'
        for p in list(logs.iterdir()):p.rename(logs/p.name.replace('decode-raw','decode-missing'))
        for row in self.report['world_archives'].values():row['file']=row['file'].replace('decode-raw','decode-missing')
        for r in self.report['phase_reports'].values():
            r['suite']=self.suite
            for row in r['decode_outcomes']:row.update(context_kind='absent',context_value='')
        self.validate()
    def test_consistent_raw_type_coercion_rejected(self):
        self.change_all('context_kind','string')
        with self.assertRaises(ValueError):self.validate()
    def test_consistent_original_payload_replacement_rejected(self):
        self.change_all('evidence_marker','second-raw-payload')
        with self.assertRaises(ValueError):self.validate()
    def test_consistent_invented_route_rejected(self):
        self.change_all('original_route_known',True)
        with self.assertRaises(ValueError):self.validate()
    def test_boolean_cannot_replace_integer(self):
        self.change_all('dimension',False)
        with self.assertRaises(ValueError):self.validate()
    def test_changed_native_readback_rejected(self):
        self.report['phase_reports']['verify']['decode_outcomes'][1]['sealed_units']=0
        with self.assertRaises(ValueError):self.validate()
    def test_unchanged_rows_with_replaced_token_rejected(self):
        self.change_all('original_token','00000000-0000-0000-0000-000000000099')
        with self.assertRaises(ValueError):self.validate()
    def test_raw_phase_from_other_suite_rejected(self):
        self.report['phase_reports']['verify']['suite']='decode-missing'
        with self.assertRaises(ValueError):self.validate()
    def test_native_fixture_digest_still_required(self):
        self.fixture.write_bytes(b'changed-decode-fixture')
        with self.assertRaises(ValueError):self.validate()
if __name__=='__main__':unittest.main()
