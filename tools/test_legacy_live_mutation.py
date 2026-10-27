import copy,unittest
from hourly_builds import validate_live_mutation_assertions
from inspect_legacy_decode_world import validate
import test_legacy_decode_world as worlds

class LiveMutationTests(unittest.TestCase):
 def setUp(self):
  self.report={'phase_reports':{}}
  for phase in ['write','verify','crash-write','crash-verify']:
   names=['live mutation persisted owner and physical source','live mutation disconnected alias stays fenced after restart','live mutation alias leaves retained sealed claim unchanged']
   if phase.endswith('write'):
    names += [p+str(x) for p in ['live mutation starts after scoped admission latch cleared ','live mutation transfer preserves evidence and escrow ','live mutation readmission freezes changed identity '] for x in [64,80]]
   self.report['phase_reports'][phase]={'assertions':names}
 def test_complete_names(self):validate_live_mutation_assertions(self.report)
 def test_missing_pre_save(self):
  self.report['phase_reports']['write']['assertions'].pop()
  with self.assertRaises(ValueError):validate_live_mutation_assertions(self.report)
 def test_missing_restart(self):
  self.report['phase_reports']['crash-verify']['assertions'].pop()
  with self.assertRaises(ValueError):validate_live_mutation_assertions(self.report)
 def test_duplicate_is_not_coverage(self):
  a=self.report['phase_reports']['verify']['assertions'];a.append(a[0])
  with self.assertRaises(ValueError):validate_live_mutation_assertions(self.report)
 def fixture(self,mc):
  f=worlds.DecodeWorldTests();f.setUp();f.field_suite('decode-rewrite',mc)
  f.tiles[0]['Owner' if mc=='1.12.2' else 'OwnerName']='live-owner'
  f.tiles += [dict(x=x,y=199,z=32,Items=[dict(Slot=0,Count=32,id='minecraft:iron_ingot' if mc=='1.12.2' else 265)]) for x in [64,80]]
  return f
 def test_exact_native_world_ports(self):
  for mc in ['1.12.2','1.6.4']:
   f=self.fixture(mc);self.assertEqual(validate(f.tiles,f.journal,f.phase,mc,'decode-live')['result'],'PASS')
 def test_context_only_owner_preserved_ports(self):
  for mc in ['1.12.2','1.6.4']:
   f=self.fixture(mc);f.tiles[0]['Owner' if mc=='1.12.2' else 'OwnerName']='fae1b877-c6b4-4538-a26a-630190b54205' if mc=='1.12.2' else 'pl4owner'
   self.assertEqual(validate(f.tiles,f.journal,f.phase,mc,'decode-live-context')['result'],'PASS')
 def test_context_only_cannot_claim_changed_owner(self):
  f=self.fixture('1.12.2')
  with self.assertRaises(ValueError):validate(f.tiles,f.journal,f.phase,'1.12.2','decode-live-context')
 def test_source_loss_rejected(self):
  f=self.fixture('1.12.2');f.tiles[-1]['Items'][0]['Count']=31
  with self.assertRaises(ValueError):validate(f.tiles,f.journal,f.phase,'1.12.2','decode-live')
 def test_owner_reversion_rejected(self):
  f=self.fixture('1.12.2');f.tiles[0]['Owner']='other'
  with self.assertRaises(ValueError):validate(f.tiles,f.journal,f.phase,'1.12.2','decode-live')
 def test_neighboring_owner_schema_rejected(self):
  f=self.fixture('1.6.4');f.tiles[0]['Owner']=f.tiles[0].pop('OwnerName')
  with self.assertRaises(ValueError):validate(f.tiles,f.journal,f.phase,'1.6.4','decode-live')
 def test_retained_alias_rejected(self):
  f=self.fixture('1.6.4');f.tiles.append(dict(x=64,y=198,z=32))
  with self.assertRaises(ValueError):validate(f.tiles,f.journal,f.phase,'1.6.4','decode-live')
if __name__=='__main__':unittest.main()
