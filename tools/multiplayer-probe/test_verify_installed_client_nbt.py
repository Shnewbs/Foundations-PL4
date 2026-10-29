"""Policy-only negative cases; real native world attestation is separate CI gate."""
import copy
import unittest
from verify_installed_client_nbt import inspect_player, inspect_native_chunk

OWNER='c41a920d-c5eb-3c57-aa93-453b2df1da52'
NODE='88a3d2e0-9d5d-446f-ad40-afd04a23df4a'
CABLE='cb4b99d8-d061-4514-8c03-5caafbab3ca2'

def arr(value):
    bits=__import__('uuid').UUID(value).int
    return [((bits>>(96-i*32))&0xffffffff) if ((bits>>(96-i*32))&0x80000000)==0 else ((bits>>(96-i*32))&0xffffffff)-0x100000000 for i in range(4)]

def player():
    return {'UUID':arr(OWNER),'Inventory':[
        {'Slot':0,'count':1,'id':'foundations_pl4:wirelessstorage',
         'components':{'minecraft:custom_data':{'pl_link':{'dimension':'minecraft:overworld','side':5,'pos':80,'part':arr(NODE)}}}},
        {'Slot':1,'count':17,'id':'minecraft:diamond'}]}

def chunk():
    return {'xPos':0,'zPos':0,'block_entities':[
        {'id':'foundations_pl4:host','schema':2,'x':0,'y':80,'z':0,'parts':[
            {'kind':'node','face':5,'identity':arr(NODE),'owner':arr(OWNER),'resourceEscrow':0,'itemEscrowCount':0,'itemTransferFault':''},
            {'kind':'datacable','face':0,'identity':arr(CABLE),'owner':arr(OWNER),'resourceEscrow':0,'itemEscrowCount':0,'itemTransferFault':''}]},
        {'id':'minecraft:barrel','x':1,'y':80,'z':0,'Items':[
            {'id':'minecraft:diamond','Slot':0,'count':30},{'id':'minecraft:diamond','Slot':1,'count':64}]}]}

class NativeClientNBTPolicy(unittest.TestCase):
    def test_valid_exact_player_and_host_fixture(self):
        self.assertEqual(inspect_player(player(),OWNER,NODE)['diamonds'],17)
        self.assertEqual(inspect_native_chunk(chunk(),OWNER,NODE,True)['barrel_diamonds'],94)

    def test_forged_client_uuid_fails(self):
        sample=player();sample['UUID']=arr(CABLE)
        with self.assertRaises(ValueError):inspect_player(sample,OWNER,NODE)

    def test_duplicate_inventory_slot_fails(self):
        sample=player();sample['Inventory'][1]['Slot']=0
        with self.assertRaises(ValueError):inspect_player(sample,OWNER,NODE)

    def test_player_item_mint_fails(self):
        sample=player();sample['Inventory'][1]['count']=18
        with self.assertRaises(ValueError):inspect_player(sample,OWNER,NODE)

    def test_wireless_tool_forged_binding_fails(self):
        sample=player();sample['Inventory'][0]['components']['minecraft:custom_data']['pl_link']['part']=arr(CABLE)
        with self.assertRaises(ValueError):inspect_player(sample,OWNER,NODE)

    def test_native_host_uuid_swap_fails(self):
        sample=chunk();sample['block_entities'][0]['parts'][0]['identity']=arr(CABLE)
        with self.assertRaises(ValueError):inspect_native_chunk(sample,OWNER,NODE,True)

    def test_native_owner_swap_fails(self):
        sample=chunk();sample['block_entities'][0]['parts'][1]['owner']=arr(NODE)
        with self.assertRaises(ValueError):inspect_native_chunk(sample,OWNER,NODE,True)

    def test_native_escrow_fault_fails(self):
        sample=chunk();sample['block_entities'][0]['parts'][0]['itemEscrowCount']=3
        with self.assertRaises(ValueError):inspect_native_chunk(sample,OWNER,NODE,True)

    def test_native_barrel_count_tamper_fails(self):
        sample=chunk();sample['block_entities'][1]['Items'][1]['count']=63
        with self.assertRaises(ValueError):inspect_native_chunk(sample,OWNER,NODE,True)

    def test_candidate_missing_modern_escrow_fields_fails(self):
        sample=chunk();del sample['block_entities'][0]['parts'][0]['itemEscrowCount']
        with self.assertRaises(ValueError):inspect_native_chunk(sample,OWNER,NODE,True)
        # Immutable beta legitimately predates this zero-valued field.
        self.assertEqual(inspect_native_chunk(sample,OWNER,NODE,False)['barrel_diamonds'],94)

    def test_duplicate_block_entity_fails(self):
        sample=chunk();sample['block_entities'].append(copy.deepcopy(sample['block_entities'][1]))
        with self.assertRaises(ValueError):inspect_native_chunk(sample,OWNER,NODE,True)

if __name__=='__main__':unittest.main()
