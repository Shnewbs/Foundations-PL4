"""Adversarial offline regression gates for actual two-client migration receipts."""
import json
import tempfile
import unittest
from pathlib import Path
from verify_upgrade_evidence import (record_phase, verify_upgrade, world_fingerprint)

BASE='a'*40
CAND='b'*40
NAMES=('PL4ProbeA','PL4ProbeB')
IDS=['11111111-1111-1111-1111-111111111111','22222222-2222-2222-2222-222222222222']


def phase(out, world, source, stage, copied=None):
    out.mkdir()
    (out/'server.log').write_text('native server successfully exited')
    server={'source_commit':source,'phase':stage,'node_identity':'node-id-123',
            'real_client_names':list(NAMES),'real_client_uuids':IDS,'fake_players':False,
            'foreign_non_operator_denied':True,'operator_access_explicitly_granted':True,
            'saved_world_restore':stage=='restore','initial_items':128,'items_per_player':[17,17],
            'remaining_items':94,'duplicate_network_requests_ignored':stage=='seed'}
    (out/'server-multiplayer-proof.json').write_text(json.dumps(server))
    for name,uid in zip(NAMES,IDS):
        (out/(name+'.log')).write_text('real client exited normally')
        (out/(name+'-storage.png')).write_bytes(b'\x89PNG\r\n\x1a\n'+b'a'*1500)
        (out/(name+'-client-proof.json')).write_text(json.dumps({
            'source_commit':source,'phase':stage,'name':name,'uuid':uid,'items_received':17,
            'identical_packets_sent':2 if stage=='seed' else 0}))
    record_phase(out,world,source,'f'*64,stage,copied)


class UpgradeEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(); root=Path(self.tmp.name)
        self.world=root/'world';self.world.mkdir();(self.world/'level.dat').write_bytes(b'NBT-FIXTURE')
        (self.world/'region').mkdir();(self.world/'region/r.0.0.mca').write_bytes(b'storage')
        self.seed=root/'seed';self.restore=root/'restore'
        phase(self.seed,self.world,BASE,'seed')
        phase(self.restore,self.world,CAND,'restore',world_fingerprint(self.world))

    def tearDown(self):self.tmp.cleanup()

    def accept(self):return verify_upgrade(self.seed,self.restore,BASE,CAND)

    def test_valid_scoped_upgrade(self):
        receipt=self.accept();self.assertEqual(receipt['baseline'],BASE)
        self.assertEqual(receipt['candidate'],CAND)

    def test_mismatched_source_rejected(self):
        with self.assertRaises(ValueError):verify_upgrade(self.seed,self.restore,BASE,'c'*40)

    def test_incorrect_seed_copy_rejected(self):
        m=self.restore/'phase-evidence.json';obj=json.loads(m.read_text());obj['copied_input_world']['sha256']='0'*64;m.write_text(json.dumps(obj))
        with self.assertRaises(ValueError):self.accept()

    def test_modified_saved_world_zip_rejected(self):
        with (self.seed/'saved-world-seed.zip').open('ab') as f:f.write(b'append')
        with self.assertRaises(ValueError):self.accept()

    def test_mutated_client_receipt_rejected(self):
        p=self.restore/'PL4ProbeA-client-proof.json';v=json.loads(p.read_text());v['items_received']=19;p.write_text(json.dumps(v))
        with self.assertRaises(ValueError):self.accept()

    def test_missing_screenshot_rejected(self):
        (self.restore/'PL4ProbeB-storage.png').unlink()
        with self.assertRaises(ValueError):self.accept()

    def test_missing_world_level_dat_rejected(self):
        (self.world/'level.dat').unlink()
        with self.assertRaises(ValueError):world_fingerprint(self.world)

    def test_wrong_operator_authority_rejected_even_if_rehashed(self):
        p=self.seed/'server-multiplayer-proof.json';v=json.loads(p.read_text());v['foreign_non_operator_denied']=False;p.write_text(json.dumps(v))
        record_phase(self.seed,self.world,BASE,'f'*64,'seed')
        with self.assertRaises(ValueError):self.accept()


if __name__=='__main__':unittest.main()
