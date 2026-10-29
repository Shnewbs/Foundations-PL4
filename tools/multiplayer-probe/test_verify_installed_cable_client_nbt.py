"""Synthetic adversarial tests, separate from installed-native execution receipts."""
import copy
import uuid
import unittest
from verify_installed_cable_client_nbt import inspect_cable_chunk, CELLS, BARRELS

OWNER="c41a920d-c5eb-3c57-aa93-453b2df1da52"
IDS=[str(uuid.UUID(int=100+i)) for i in range(9)]


def arr(value):
    bits=uuid.UUID(value).int
    out=[]
    for i in range(4):
        n=(bits>>(96-i*32))&0xffffffff
        out.append(n if n<2**31 else n-2**32)
    return out


def setup():
    record={"real_client_uuids":[OWNER,str(uuid.UUID(int=300))],
            "cable_transfer":{"identities":IDS,"source_after":1664,"sink_after":64}}
    chunk={"xPos":0,"zPos":0,"block_entities":[]}
    for i,xyz in enumerate(CELLS):
        cable={"kind":"datacable","face":0,"owner":arr(OWNER),
               "identity":arr(IDS[i+2]),"blockedFaces":0,"resourceEscrow":0,
               "itemEscrowCount":0,"itemTransferFault":""}
        parts=[cable]
        if i in (0,6):
            parts.append({"kind":"transfernode","face":5,
                          "owner":arr(OWNER),"identity":arr(IDS[0 if i==0 else 1]),
                          "transferMode":0,"items":0,"resourceEscrow":0,
                          "itemEscrowCount":0,"itemTransferFault":""})
        x,y,z=xyz
        chunk["block_entities"].append({"x":x,"y":y,"z":z,
                                        "id":"foundations_pl4:host","schema":2,"parts":parts})
    for xyz,count in zip(BARRELS,(1664,64)):
        x,y,z=xyz
        items=[{"id":"minecraft:diamond","Slot":slot,"count":64}
               for slot in range(count//64)]
        chunk["block_entities"].append(
            {"x":x,"y":y,"z":z,"id":"minecraft:barrel","Items":items})
    return chunk,record


class NativeCableClientNBTPolicy(unittest.TestCase):
    def test_valid_exact_7_plus_2_fixture(self):
        chunk,record=setup()
        report=inspect_cable_chunk(chunk,record,"restore")
        self.assertEqual(report["source_diamonds"],1664)
        self.assertEqual(report["sink_diamonds"],64)

    def test_missing_terrain_cable_is_rejected(self):
        chunk,record=setup()
        chunk["block_entities"].pop(3)
        with self.assertRaises(ValueError):inspect_cable_chunk(chunk,record,"restore")

    def test_rewritten_owner_is_rejected(self):
        chunk,record=setup()
        chunk["block_entities"][0]["parts"][0]["owner"]=arr(IDS[0])
        with self.assertRaises(ValueError):inspect_cable_chunk(chunk,record,"restore")

    def test_rewritten_saved_uuid_is_rejected(self):
        chunk,record=setup()
        chunk["block_entities"][4]["parts"][0]["identity"]=arr(IDS[2])
        with self.assertRaises(ValueError):inspect_cable_chunk(chunk,record,"restore")

    def test_duplicate_host_entity_is_rejected(self):
        chunk,record=setup()
        chunk["block_entities"].append(copy.deepcopy(chunk["block_entities"][0]))
        with self.assertRaises(ValueError):inspect_cable_chunk(chunk,record,"restore")

    def test_reconnected_cable_cannot_stay_blocked(self):
        chunk,record=setup()
        chunk["block_entities"][3]["parts"][0]["blockedFaces"]=1<<5
        with self.assertRaises(ValueError):inspect_cable_chunk(chunk,record,"restore")

    def test_pending_escrow_rejects_rc_evidence(self):
        chunk,record=setup()
        chunk["block_entities"][6]["parts"][1]["itemEscrowCount"]=1
        with self.assertRaises(ValueError):inspect_cable_chunk(chunk,record,"restore")

    def test_scheduled_driver_must_be_frozen_before_save(self):
        chunk,record=setup()
        chunk["block_entities"][0]["parts"][1]["transferMode"]=2
        with self.assertRaises(ValueError):inspect_cable_chunk(chunk,record,"restore")

    def test_source_inventory_mint_is_rejected(self):
        chunk,record=setup()
        chunk["block_entities"][-2]["Items"].append(
            {"id":"minecraft:diamond","Slot":26,"count":64})
        with self.assertRaises(ValueError):inspect_cable_chunk(chunk,record,"restore")

    def test_candidate_missing_modern_escrow_is_rejected(self):
        chunk,record=setup()
        del chunk["block_entities"][0]["parts"][0]["itemEscrowCount"]
        with self.assertRaises(ValueError):inspect_cable_chunk(chunk,record,"restore")
        self.assertEqual(inspect_cable_chunk(chunk,record,"seed")["total_diamonds"],1728)

    def test_fake_slot_boolean_is_rejected(self):
        chunk,record=setup()
        chunk["block_entities"][-1]["Items"][0]["Slot"]=True
        with self.assertRaises(ValueError):inspect_cable_chunk(chunk,record,"restore")


if __name__=="__main__":
    unittest.main()
