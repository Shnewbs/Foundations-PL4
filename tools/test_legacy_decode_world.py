import copy,unittest
from inspect_legacy_decode_world import validate

class DecodeWorldTests(unittest.TestCase):
    def field_suite(self,suite,mc='1.12.2'):
        owner='fae1b877-c6b4-4538-a26a-630190b54205' if mc=='1.12.2' else 'pl4owner'
        for x,tag,row in [(64,self.tiles[0],self.phase['decode_outcomes'][0]),(80,self.tiles[1]['Items'][0]['tag'],self.phase['decode_outcomes'][1])]:
            tag['ItemLegacyEvidence']['ObservedOwner']=owner
            r=dict(Token=row['original_token'],Owner=owner,Dimension=0,Scope='legacy-unknown',HostX=x,HostY=200,HostZ=32,RejectedPayload=dict(Bytes=bytes([4,5,6])))
            if suite=='decode-owner':r['Owner']=71
            if suite=='decode-dimension':r['Dimension']='0'
            if suite=='decode-scope':r['Scope']=dict(Marker=71,Bytes=bytes([7,8,9]))
            if suite=='decode-rewrite':r.update(Owner='rewritten-owner',Dimension=7,HostX=x+128)
            tag['ItemJournalContext']=r
        self.journal['ExistingLegacyEvidence']=copy.deepcopy(self.tiles[0]['ItemLegacyEvidence'])
        self.journal['RejectedContext']=copy.deepcopy(self.tiles[0]['ItemJournalContext'])
        for row in self.journal['PreviousJournal']['Entries']:row['Owner']=owner
        if mc=='1.6.4':self.journal=dict(Schema=-1,Opaque=self.journal)
    def assert_field_ports(self,suite):
        for mc in ['1.12.2','1.6.4']:
            self.setUp();self.field_suite(suite,mc)
            self.assertEqual(self.validate(suite,mc)['result'],'PASS')
    def test_owner_field_worlds_coherent(self):self.assert_field_ports('decode-owner')
    def test_dimension_field_worlds_coherent(self):self.assert_field_ports('decode-dimension')
    def test_scope_field_worlds_coherent(self):self.assert_field_ports('decode-scope')
    def test_rewritten_context_worlds_coherent(self):self.assert_field_ports('decode-rewrite')
    def test_owner_scalar_coercion_rejected(self):
        self.field_suite('decode-owner');self.tiles[0]['ItemJournalContext']['Owner']='71'
        with self.assertRaises(ValueError):self.validate('decode-owner')
    def test_dimension_string_coercion_rejected(self):
        self.field_suite('decode-dimension');self.tiles[1]['Items'][0]['tag']['ItemJournalContext']['Dimension']=0
        with self.assertRaises(ValueError):self.validate('decode-dimension')
    def test_scope_nested_bytes_replacement_rejected(self):
        self.field_suite('decode-scope');self.tiles[0]['ItemJournalContext']['Scope']['Bytes']=bytes([7,8,0])
        with self.assertRaises(ValueError):self.validate('decode-scope')
    def test_rewritten_route_omission_rejected(self):
        self.field_suite('decode-rewrite');self.tiles[0]['ItemJournalContext']['HostX']=64
        with self.assertRaises(ValueError):self.validate('decode-rewrite')
    def test_field_cannot_be_reported_as_prior_compound(self):
        self.field_suite('decode-owner')
        with self.assertRaises(ValueError):self.validate('decode-compound')
    def test_unknown_world_suite_rejected(self):
        with self.assertRaises(ValueError):self.validate('decode-unknown')
    def compound(self):
        owner='fae1b877-c6b4-4538-a26a-630190b54205'
        def rejected(x):return dict(Token=71,Owner=owner,Dimension=0,Scope='legacy-unknown',HostX=x,HostY=200,HostZ=32,RejectedPayload=dict(Bytes=bytes([4,5,6])))
        self.tiles[0]['ItemJournalContext']=rejected(64);self.tiles[1]['Items'][0]['tag']['ItemJournalContext']=rejected(80);self.journal['RejectedContext']=rejected(64)
    def test_compound_world_coherent(self):
        self.compound();self.assertEqual(self.validate('decode-compound')['result'],'PASS')
    def test_compound_nested_payload_changed_rejected(self):
        self.compound();self.tiles[1]['Items'][0]['tag']['ItemJournalContext']['RejectedPayload']['Bytes']=bytes([4,5,7])
        with self.assertRaises(ValueError):self.validate('decode-compound')
    def setUp(self):
        owner='fae1b877-c6b4-4538-a26a-630190b54205';tokens=['00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002']
        def tag(x,token):
            return dict(ItemTransferFault='uncertain',ItemEscrowCount=8,ItemJournalContext=71 if x==64 else 'current-raw-context',ItemLegacyEvidence=dict(Fault='Original decode fixture uncertainty',ObservedOwner=owner,ObservedDimension=0,HostX=x,HostY=200,HostZ=32,OriginalToken=token,MalformedContext='first-raw-payload',OriginalPayload=dict(Sentinel=12345,Bytes=bytes([1,2,3]))))
        live=tag(64,tokens[0]);live.update(x=64,y=200,z=32)
        receipt=tag(80,tokens[1]);self.tiles=[live,dict(x=85,y=200,z=32,Items=[dict(Slot=0,Count=1,id='foundations_pl4:legacy_item_cell',tag=receipt)])]
        self.journal=dict(ExistingLegacyEvidence=copy.deepcopy(live['ItemLegacyEvidence']),RejectedContext=71,PreviousJournal=dict(Schema=2,Entries=[dict(Token=t,Owner=owner,Scope='legacy-unknown') for t in tokens]))
        self.phase=dict(decode_outcomes=[dict(original_token=t) for t in tokens])
    def validate(self,suite='decode-raw',mc='1.12.2'):return validate(self.tiles,self.journal,self.phase,mc,suite)
    def test_raw_world_coherent(self):self.assertEqual(self.validate()['result'],'PASS')
    def test_changed_original_bytes_rejected(self):
        self.tiles[0]['ItemLegacyEvidence']['OriginalPayload']['Bytes']=bytes([1,2,4])
        with self.assertRaises(ValueError):self.validate()
    def test_receipt_current_context_replacement_rejected(self):
        self.tiles[1]['Items'][0]['tag']['ItemJournalContext']='first-raw-payload'
        with self.assertRaises(ValueError):self.validate()
    def test_receipt_disappeared_rejected(self):
        self.tiles[1]['Items']=[]
        with self.assertRaises(ValueError):self.validate()
    def test_invented_prior_route_rejected(self):
        self.journal['PreviousJournal']['Entries'][0]['X']=64
        with self.assertRaises(ValueError):self.validate()
    def test_missing_world_keeps_absence(self):
        del self.tiles[0]['ItemJournalContext'];del self.tiles[1]['Items'][0]['tag']['ItemJournalContext']
        del self.journal['RejectedContext'];self.journal['MissingContext']=1
        self.assertEqual(self.validate('decode-missing')['result'],'PASS')
        self.tiles[0]['ItemJournalContext']={}
        with self.assertRaises(ValueError):self.validate('decode-missing')
if __name__=='__main__':unittest.main()
