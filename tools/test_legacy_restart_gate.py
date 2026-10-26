import copy,hashlib,json,tempfile,unittest
from pathlib import Path
import hourly_builds as builds
class RestartProofTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name);logs=self.root/'verification-logs';logs.mkdir()
        self.mc='1.12.2';self.sha='a'*40;self.runtime='b'*64
        self.report=dict(minecraft=self.mc,source_commit=self.sha,runtime_sha256=self.runtime,clean_restart='PASS',save_barrier_abrupt_restart='PASS',processes=[],phase_reports={},world_archives={})
        for i,(phase,code) in enumerate([('write',0),('verify',0),('crash-write',73),('crash-verify',0)]):
            pid=100+i;log=logs/('item-restart-'+phase+'.log');log.write_text(phase)
            self.report['processes'].append(dict(phase=phase,pid=pid,exit_code=code,log_sha256=hashlib.sha256(log.read_bytes()).hexdigest()))
            tokens=['00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002']
            self.report['phase_reports'][phase]=dict(minecraft=self.mc,phase=phase,passed=True,assertions=['check']*(36 if phase.endswith('write') else 26),process=str(pid)+'@runner',tokens=tokens)
        for mode in ['clean','barrier']:
            name='item-restart-'+mode+'-world.zip';p=logs/name;p.write_bytes(b'world-fixture')
            self.report['world_archives'][mode]=dict(file=name,sha256=hashlib.sha256(p.read_bytes()).hexdigest())
    def validate(self):builds.validate_restart(self.root,self.report,self.mc,self.sha,self.runtime,dict(write=36,verify=26))
    def test_coherent(self):self.validate()
    def test_wrong_source(self):
        self.report['source_commit']='c'*40
        with self.assertRaises(ValueError):self.validate()
    def test_reused_pid(self):
        self.report['processes'][1]['pid']=100
        with self.assertRaises(ValueError):self.validate()
    def test_missing_process(self):
        self.report['processes'].pop()
        with self.assertRaises(ValueError):self.validate()
    def test_clean_exit_cannot_prove_abrupt_halt(self):
        self.report['processes'][2]['exit_code']=0
        with self.assertRaises(ValueError):self.validate()
    def test_changed_tokens(self):
        self.report['phase_reports']['verify']['tokens']=['00000000-0000-0000-0000-000000000003']*2
        with self.assertRaises(ValueError):self.validate()
    def test_changed_world_archive(self):
        (self.root/'verification-logs/item-restart-clean-world.zip').write_bytes(b'changed')
        with self.assertRaises(ValueError):self.validate()
    def test_incomplete_assertions(self):
        self.report['phase_reports']['verify']['assertions'].pop()
        with self.assertRaises(ValueError):self.validate()
if __name__=='__main__':unittest.main()
