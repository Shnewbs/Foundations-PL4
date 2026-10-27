import copy,unittest
from hourly_builds import validate_decode_placements
class PlacementTests(unittest.TestCase):
    def setUp(self):
        self.report={'phase_reports':{p:{'placements':[dict(x=x,y=200,z=32,before_block=0,set_block_changed=True,expected_block=2000,actual_block=2000,metadata=0,tile_class='net.foundations.pl4.legacy.LegacyTile',tile_invalid=False) for x in ([64,80,200] if p.endswith('write') else [200])]} for p in ('write','verify','crash-write','crash-verify')}}
    def check(self):validate_decode_placements(self.report)
    def test_complete(self):self.check()
    def test_missing_or_duplicate_or_reordered(self):
        for rows in ([],self.report['phase_reports']['write']['placements']*2,list(reversed(self.report['phase_reports']['write']['placements']))):
            with self.subTest(rows=rows):
                report=copy.deepcopy(self.report);report['phase_reports']['write']['placements']=rows
                with self.assertRaises(ValueError):validate_decode_placements(report)
    def test_bad_native_observations(self):
        for key,value in (('set_block_changed',False),('actual_block',0),('tile_class',None),('tile_invalid',True),('expected_block',True),('y',201),('before_block',1)):
            with self.subTest(key=key):
                report=copy.deepcopy(self.report);report['phase_reports']['write']['placements'][0][key]=value
                with self.assertRaises(ValueError):validate_decode_placements(report)
    def test_staged_observations_complete(self):
        for report in self.report['phase_reports'].values():
            for row in report['placements']:
                row.update(stage='complete',chunk_x=row['x']//16,chunk_z=2,block_exists_before=True,block_exists_after=True)
        self.check()
        for key,value in (('stage','tile_lookup'),('chunk_x',99),('chunk_z',0),('block_exists_before',False),('block_exists_after',None)):
            with self.subTest(key=key):
                report=copy.deepcopy(self.report);report['phase_reports']['write']['placements'][0][key]=value
                with self.assertRaises(ValueError):validate_decode_placements(report)
    def test_staged_observations_mixed_or_missing(self):
        self.report['phase_reports']['write']['placements'][0].update(stage='complete',chunk_x=4,chunk_z=2,block_exists_before=True,block_exists_after=True)
        with self.assertRaises(ValueError):self.check()
    def test_omitted_diagnostic_not_silent_pass(self):
        del self.report['phase_reports']['verify']['placements']
        with self.assertRaises(ValueError):self.check()
if __name__=='__main__':unittest.main()
