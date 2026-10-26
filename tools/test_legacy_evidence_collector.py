import copy,io,json,struct,tempfile,unittest,zipfile,zlib
from pathlib import Path
from collect_legacy_item_evidence import LIMITS,collect,inflate,journal_records,observe,digest_value

class EvidenceCollectorTests(unittest.TestCase):
    token='00000000-0000-0000-0000-000000000001'
    def current(self,**changes):
        row=dict(Token=self.token,Owner='owner',X=3,Y=200,Z=32);row.update(changes)
        return dict(Schema=2,Entries=[row])
    def test_valid_current_target_remains_saved_observation(self):
        r=self.scan(journal=self.current());self.assertEqual(r['matching_saved_records'][0]['original_target'],[3,200,32]);self.assertFalse(r['reconciliation_cleared'])
    def test_malformed_scope_with_coordinates_never_gives_target(self):
        for scope in [7,'future-scope']:
            r=self.scan(journal=self.current(Scope=scope));self.assertTrue(r['opaque_journal']);self.assertIsNone(r['matching_saved_records'][0]['original_target'])
    def test_duplicate_token_with_conflicting_targets_is_ambiguous(self):
        j=self.current();j['Entries'].append(dict(j['Entries'][0],X=99));r=self.scan(journal=j)
        self.assertTrue(r['opaque_journal']);self.assertTrue(all(row['duplicate_token_observation'] and row['original_target'] is None for row in r['matching_saved_records']))
    def test_other_invalid_record_freezes_target_attribution(self):
        j=self.current();j['Entries'].append(dict(Token='bad',Owner='owner',X=1,Y=1,Z=1));r=self.scan(journal=j);self.assertTrue(r['opaque_journal']);self.assertIsNone(r['matching_saved_records'][0]['original_target'])
    def test_bad_owner_height_and_coordinate_types_withhold_target(self):
        for change in [dict(Owner=7),dict(Y=256),dict(X=True)]:
            r=self.scan(journal=self.current(**change));self.assertIsNone(r['matching_saved_records'][0]['original_target']);self.assertTrue(r['matching_saved_records'][0]['record_validation_errors'])
    def test_malformed_container_is_not_silently_discarded(self):
        for j in [7,dict(Schema=2),dict(Schema=2,Entries=[7]),dict(PreviousJournal='lost')]:
            with self.assertRaises(ValueError):journal_records(j,4096)
    def test_legacy_schema_one_remains_opaque(self):
        j=dict(Schema=1,Entries=[dict(Token=self.token,Owner='owner',Scope='legacy-unknown',HostX=64,HostY=200,HostZ=32,Evidence={})]);self.assertTrue(self.scan(journal=j)['opaque_journal'])
    def test_bytes_and_lookalike_compound_hashes_differ(self):
        self.assertNotEqual(digest_value(bytes([1,2])),digest_value(dict(nbt_byte_array_hex='0102')))
    def test_typed_hash_order_and_domain(self):
        self.assertEqual(digest_value(dict(a=1,b=2)),digest_value(dict(b=2,a=1)))
        self.assertNotEqual(digest_value(True),digest_value(1));self.assertNotEqual(digest_value([1,2]),digest_value([2,1]))
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.path=Path(self.temp.name)/'copied-save.zip'
        self.journal=dict(PreviousJournal=dict(Schema=2,Entries=[dict(Token=self.token,Owner='owner',Scope='legacy-unknown',HostX=64,HostY=200,HostZ=32)]),MissingContext=1)
    def scan(self,tiles=(),journal=None,token=None):return observe(tiles,self.journal if journal is None else journal,token or self.token,'owner',0,{(85,200,32)},LIMITS)
    def archive(self,extra=None):
        with zipfile.ZipFile(self.path,'w') as z:
            z.writestr('world/level.dat',b'copied-level')
            if extra:
                for k,v in extra.items():z.writestr(k,v)
    def test_unknown_original_route_is_not_host(self):
        r=self.scan();self.assertIsNone(r['matching_saved_records'][0]['original_target']);self.assertEqual(r['matching_saved_records'][0]['observed_host'],[64,200,32]);self.assertTrue(r['opaque_journal']);self.assertFalse(r['reconciliation_cleared'])
    def test_missing_token_never_clears_fence(self):
        r=self.scan(token='00000000-0000-0000-0000-000000000002');self.assertEqual(r['matching_saved_records'],[]);self.assertFalse(r['reconciliation_cleared'])
    def test_unknown_schema_is_opaque_and_not_interpreted(self):
        r=self.scan(journal=dict(Schema=99,Entries=[dict(Token=self.token,X=1,Y=2,Z=3)]));self.assertTrue(r['opaque_journal']);self.assertEqual(r['matching_saved_records'],[]);self.assertFalse(r['reconciliation_cleared'])
    def test_unlinked_receipt_at_explicit_inventory_is_only_observed(self):
        r=self.scan([dict(x=85,y=200,z=32,Items=[dict(Slot=0,Count=1,tag=dict(ItemTransferFault='old unlinked fault',ItemEscrowCount=8))])]);row=r['observed_receipts'][0];self.assertFalse(row['current_context_token_matches']);self.assertFalse(row['preserved_evidence_token_occurrence']);self.assertFalse(row['original_route_inferred'])
    def test_opaque_evidence_occurrence_is_not_trusted_link(self):
        r=self.scan([dict(x=64,y=200,z=32,ItemLegacyEvidence=dict(OriginalToken=self.token))]);row=r['observed_fault_tiles'][0];self.assertFalse(row['current_context_token_matches']);self.assertTrue(row['preserved_evidence_token_occurrence']);self.assertFalse(row['original_route_inferred'])
    def test_sealed_receipt_does_not_become_spendable(self):
        r=self.scan([dict(x=85,y=200,z=32,Items=[dict(Slot=0,Count=1,tag=dict(ItemLegacyEvidence=dict(OriginalToken=self.token),ItemEscrowCount=8))])]);self.assertFalse(r['observed_receipts'][0]['spendable_claim']);self.assertFalse(r['explicit_inventory_observations'][0]['authorization_proven']);self.assertFalse(r['explicit_inventory_observations'][0]['spendable_total_inferred'])
    def test_journal_depth_bound(self):
        value={}
        for _ in range(10):value=dict(PreviousJournal=value)
        with self.assertRaises(ValueError):journal_records(value,4096)
    def test_decompression_bound(self):
        with self.assertRaises(ValueError):inflate(zlib.compress(b'x'*10000),2,64)
    def test_ambiguous_archive_paths_rejected(self):
        self.archive({'../other':b'x'})
        with self.assertRaises(ValueError):collect(self.path,'1.12.2',self.token,'owner')
    def test_unchanged_input_and_no_false_clearance_without_journal(self):
        self.archive();before=self.path.read_bytes();r=collect(self.path,'1.6.4',self.token,'owner');self.assertEqual(before,self.path.read_bytes());self.assertFalse(r['journal_present']);self.assertIsNone(r['journal_value_sha256']);self.assertEqual(r['value_hash_format'],'pl4-parsed-value-v2');self.assertFalse(r['reconciliation_cleared']);self.assertFalse(r['minecraft_identity_proven_by_save'])
    def test_chunk_bound_is_reported_as_partial(self):
        # Minimal native compound with no tile entities, wrapped in a real region record.
        raw=b'\x0a\x00\x00\x0a\x00\x05Level\x09\x00\x0cTileEntities\x0a\x00\x00\x00\x00\x00\x00'
        compressed=zlib.compress(raw);region=bytearray(12288);region[:4]=struct.pack('>I',2<<8|1);region[8192:8197]=struct.pack('>I',len(compressed)+1)+b'\x02';region[8197:8197+len(compressed)]=compressed
        self.archive({'world/region/r.0.0.mca':region});limits=dict(LIMITS,chunks=0)
        r=collect(self.path,'1.12.2',self.token,'owner',limits=limits);self.assertFalse(r['selected_dimension_scan_complete']);self.assertEqual(r['scan_stop_reason'],'chunk bound');self.assertFalse(r['reconciliation_cleared'])
        r=collect(self.path,'1.12.2',self.token,'owner');self.assertEqual(r['scanned_chunks'],1)
    def test_result_bound_does_not_hide_truncation(self):
        journal=dict(Schema=2,Entries=[dict(Token=self.token,Owner='owner')]*3);r=observe([],journal,self.token,'owner',0,set(),dict(LIMITS,results=1));self.assertEqual(len(r['matching_saved_records']),1);self.assertTrue(r['result_limit_reached']);self.assertFalse(r['reconciliation_cleared'])
    def test_explicit_inventory_priority_crosses_region_boundary(self):
        def text(s):return struct.pack('>H',len(s))+s.encode()
        def region(base):
            value=bytearray(16384)
            for slot in [0,1]:
                tile=b''.join(b'\x03'+text(k)+struct.pack('>i',v) for k,v in [('x',base+slot*16),('y',200),('z',0)])+b'\x08'+text('id')+text('fixture:inventory')+b'\x09'+text('Items')+b'\x0a'+struct.pack('>i',0)+b'\x00'
                raw=b'\x0a\x00\x00\x0a'+text('Level')+b'\x09'+text('TileEntities')+b'\x0a'+struct.pack('>i',1)+tile+b'\x00\x00';compressed=zlib.compress(raw);off=(2+slot)*4096
                value[slot*4:slot*4+4]=struct.pack('>I',(2+slot)<<8|1);value[off:off+5]=struct.pack('>I',len(compressed)+1)+b'\x02';value[off+5:off+5+len(compressed)]=compressed
            return value
        self.archive({'world/region/r.0.0.mca':region(0),'world/region/r.1.0.mca':region(512)})
        r=collect(self.path,'1.12.2',self.token,'owner',inventories=[(0,200,0),(512,200,0)],limits=dict(LIMITS,chunks=2))
        self.assertEqual(sorted(row['position'] for row in r['explicit_inventory_observations']),[[0,200,0],[512,200,0]]);self.assertFalse(r['selected_dimension_scan_complete']);self.assertEqual(r['scanned_chunks'],2)
if __name__=='__main__':unittest.main()
