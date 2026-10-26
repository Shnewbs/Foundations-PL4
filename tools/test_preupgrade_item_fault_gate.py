import copy,hashlib,json,tempfile,unittest
from pathlib import Path
import hourly_builds as builds

class PreupgradeProofTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name);self.logs=self.root/'verification-logs';self.logs.mkdir()
        self.mc='1.12.2';self.source='a'*40;self.runtime='b'*64
        old=hashlib.sha256(b'old-runtime').hexdigest();(self.logs/'preupgrade-baseline-runtime.jar').write_bytes(b'old-runtime')
        (self.logs/'preupgrade-baseline-native.json').write_text(json.dumps(dict(minecraft=self.mc,source_commit='c'*40,native_gate='PASS',artifact_sha256={'old.jar':old})))
        self.report=dict(minecraft=self.mc,source_commit=self.source,runtime_sha256=self.runtime,baseline=dict(source_commit='c'*40,runtime_sha256=old),preupgrade_unlocated_fault_import='PASS',candidate_restart='PASS',processes=[],world_archive={})
        fixtures=self.root/'build/libs';fixtures.mkdir(parents=True);self.fixture=fixtures/'candidate-migration-scenarios.jar';self.fixture.write_bytes(b'fixture')
        self.report['fixture_sha256']=hashlib.sha256(self.fixture.read_bytes()).hexdigest()
        for i,(phase,count,runtime) in enumerate([('fault-write',4,old),('fault-upgrade',10,self.runtime),('fault-verify',10,self.runtime)]):
            pid=100+i;proof=dict(minecraft=self.mc,phase=phase,passed=True,checks=count,process=str(pid)+'@runner',tokens=[] if not i else ['00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002'])
            p=self.logs/('preupgrade-'+phase+'.log');p.write_text('Starting minecraft server version '+self.mc+'\nPL4 LEGACY MIGRATION '+phase+': PASS\nStopping server\n');(self.logs/('preupgrade-'+phase+'.json')).write_text(json.dumps(proof))
            self.report['processes'].append(dict(phase=phase,pid=pid,exit_code=0,runtime_sha256=runtime,log_sha256=hashlib.sha256(p.read_bytes()).hexdigest(),report=proof))
        p=self.logs/'preupgrade-item-fault-world.zip';p.write_bytes(b'world');self.report['world_archive']=dict(file=p.name,sha256=hashlib.sha256(p.read_bytes()).hexdigest())
    def validate(self):builds.validate_preupgrade(self.root,self.report,self.mc,self.source,self.runtime)
    def test_coherent(self):self.validate()
    def test_changed_migration_fixture(self):
        self.fixture.write_bytes(b'changed')
        with self.assertRaises(ValueError):self.validate()
    def test_missing_migration_fixture(self):
        self.fixture.unlink()
        with self.assertRaises(ValueError):self.validate()
    def test_wrong_candidate(self):
        self.report['source_commit']='d'*40
        with self.assertRaises(ValueError):self.validate()
    def test_changed_historical_jar(self):
        (self.logs/'preupgrade-baseline-runtime.jar').write_bytes(b'other')
        with self.assertRaises(ValueError):self.validate()
    def test_reused_pid(self):
        self.report['processes'][2]['pid']=100
        with self.assertRaises(ValueError):self.validate()
    def test_changed_phase_report(self):
        (self.logs/'preupgrade-fault-upgrade.json').write_text('{}')
        with self.assertRaises(ValueError):self.validate()
    def test_missing_phase(self):
        self.report['processes'].pop()
        with self.assertRaises(ValueError):self.validate()
    def test_invented_baseline_token(self):
        self.report['processes'][0]['report']['tokens']=['00000000-0000-0000-0000-000000000001']
        (self.logs/'preupgrade-fault-write.json').write_text(json.dumps(self.report['processes'][0]['report']))
        with self.assertRaises(ValueError):self.validate()
    def test_changed_world(self):
        (self.logs/'preupgrade-item-fault-world.zip').write_bytes(b'other')
        with self.assertRaises(ValueError):self.validate()
    def changed_log(self,text):
        p=self.logs/'preupgrade-fault-upgrade.log';p.write_text(text);self.report['processes'][1]['log_sha256']=hashlib.sha256(p.read_bytes()).hexdigest()
    def test_wrong_native_minecraft_even_with_matching_digest(self):
        self.changed_log('Starting minecraft server version 1.6.4\nPL4 LEGACY MIGRATION fault-upgrade: PASS\nStopping server\n')
        with self.assertRaises(ValueError):self.validate()
    def test_missing_native_pass_even_with_matching_digest(self):
        self.changed_log('Starting minecraft server version 1.12.2\nStopping server\n')
        with self.assertRaises(ValueError):self.validate()
if __name__=='__main__':unittest.main()

