"""Synthetic Anvil-only negative cases; these are NOT native installed-JAR evidence."""
import struct
import unittest
import uuid
import zlib

import verify_native_multipart_nbt as gate


def string(value):
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def tag(kind, key, content):
    return bytes([kind]) + string(key) + content


def comp(*fields):
    return b"".join(fields) + b"\0"


def list_of_compounds(values):
    return b"\x0a" + struct.pack(">i", len(values)) + b"".join(values)


def uid(value):
    value = uuid.UUID(value).int
    ints = [(value >> shift) & 0xffffffff for shift in (96, 64, 32, 0)]
    return struct.pack(">i", 4) + struct.pack(">4I", *ints)


def fixture_chunk(parts=True, barrel=64, duplicate_uuid=False, owner=None):
    owner = owner or gate.PART_OWNER
    identity_a = "11111111-2222-4333-8444-555555555555"
    identity_b = identity_a if duplicate_uuid else "66666666-7777-4888-8999-aaaaaaaaaaaa"
    def part(kind, face, identity):
        return comp(
            tag(8, "kind", string(kind)),
            tag(3, "face", struct.pack(">i", face)),
            tag(11, "owner", uid(owner)),
            tag(11, "identity", uid(identity)),
        )
    host = comp(
        tag(8, "id", string("foundations_pl4:host")),
        tag(3, "x", struct.pack(">i", 0)),
        tag(3, "y", struct.pack(">i", 80)),
        tag(3, "z", struct.pack(">i", 0)),
        tag(3, "schema", struct.pack(">i", 2)),
        tag(9, "parts", list_of_compounds([
            part("inventoryreader", 5, identity_a),
            part("datacable", 0, identity_b),
        ] if parts else [])),
    )
    def diamond(slot, count):
        return comp(
            tag(1, "Slot", struct.pack(">b", slot)),
            tag(8, "id", string("minecraft:diamond")),
            tag(3, "count", struct.pack(">i", count)),
        )
    other = comp(
        tag(8, "id", string("minecraft:barrel")),
        tag(3, "x", struct.pack(">i", 1)),
        tag(3, "y", struct.pack(">i", 80)),
        tag(3, "z", struct.pack(">i", 0)),
        tag(9, "Items", list_of_compounds([diamond(0, barrel), diamond(1, 64)])),
    )
    return tag(10, "", comp(tag(9, "block_entities", list_of_compounds([host, other]))))


def fixture_region(chunk):
    packed = zlib.compress(chunk)
    record = struct.pack(">I", len(packed) + 1) + b"\x02" + packed
    count = (len(record) + 4095) // 4096
    region = bytearray((2 + count) * 4096)
    region[:4] = (2).to_bytes(3, "big") + bytes([count])
    region[8192:8192 + len(record)] = record
    return bytes(region)


class NativeMultipartRegionPolicy(unittest.TestCase):
    def test_native_compound_and_uuid_layout(self):
        actual = gate.inspect_chunk(gate.decode_region(fixture_region(fixture_chunk())))
        self.assertEqual(actual["barrel_diamonds"], 128)
        self.assertEqual(actual["host_schema"], 2)
        self.assertEqual([p["slot"] for p in actual["parts"]], [5, 6])
        self.assertEqual(actual["parts"][0]["kind"], "INVENTORY_READER")
        self.assertEqual(actual["parts"][1]["kind"], "DATA_CABLE")

    def test_old_empty_parts_legacy_smoke_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "empty/incomplete"):
            gate.inspect_chunk(gate.decode_region(fixture_region(fixture_chunk(parts=False))))

    def test_duplicate_part_uuid_cannot_pass(self):
        with self.assertRaisesRegex(ValueError, "Duplicate native persisted"):
            gate.inspect_chunk(gate.decode_region(fixture_region(fixture_chunk(duplicate_uuid=True))))

    def test_mutated_owner_cannot_be_accepted(self):
        with self.assertRaisesRegex(ValueError, "part kind/slot/owner"):
            gate.inspect_chunk(gate.decode_region(fixture_region(
                fixture_chunk(owner="aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"))))

    def test_nonconserved_native_barrel_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "not conserved"):
            gate.inspect_chunk(gate.decode_region(fixture_region(fixture_chunk(barrel=63))))

    def test_invalid_compression_or_sector_is_rejected(self):
        region = bytearray(fixture_region(fixture_chunk()))
        region[8192 + 4] = 9
        with self.assertRaisesRegex(ValueError, "compression"):
            gate.decode_region(bytes(region))
        region[0:4] = b"\0\0\0\0"
        with self.assertRaisesRegex(ValueError, "offset"):
            gate.decode_region(bytes(region))



def transfer_fixture(legacy=False):
    """Synthetic parsed NBT only, never proof of a native game execution."""
    source_id = "11111111-2222-4333-8444-555555555555"
    sink_id = "66666666-7777-4888-8999-aaaaaaaaaaaa"
    def integers(value):
        return list(struct.unpack(">4i", uuid.UUID(value).bytes))
    def host(x, mode, identity):
        part = {
            "kind": "transfernode", "face": 5, "transferMode": mode,
            "items": 1, "fluids": 0, "energy": 0, "resourceEscrow": 0,
            "owner": integers(gate.PART_OWNER), "identity": integers(identity),
        }
        if not legacy:
            part["itemEscrowCount"] = 0
            part["itemTransferFault"] = ""
        return {"id": "foundations_pl4:host", "schema": 2, "parts": [part]}
    def barrel(count):
        return {
            "id": "minecraft:barrel",
            "Items": [{"id": "minecraft:diamond", "Slot": i, "count": min(64, count-64*i)}
                      for i in range((count + 63)//64)],
        }
    targets = {
        (0, 80, 2): host(0, 2, source_id),
        (1, 80, 2): barrel(1664),
        (4, 80, 2): host(4, 1, sink_id),
        (5, 80, 2): barrel(64),
    }
    expected = {
        "source_identity": source_id, "sink_identity": sink_id,
        "source_after": 1664, "sink_after": 64,
    }
    return targets, expected


class InstalledTransferNativeNBTPolicy(unittest.TestCase):
    def test_candidate_saved_transfer_inventory_and_uuid_pass(self):
        observed, expected = transfer_fixture()
        result = gate.inspect_transfer_entities(observed, expected)
        self.assertEqual(result["source_diamonds"], 1664)
        self.assertEqual(result["sink_diamonds"], 64)
        self.assertEqual(result["item_total"], 1728)

    def test_published_beta_may_omit_only_new_safe_escrow_keys(self):
        observed, expected = transfer_fixture(legacy=True)
        self.assertEqual(gate.inspect_transfer_entities(
            observed, expected, allow_legacy_missing=True)["item_escrow"], 0)
        with self.assertRaisesRegex(ValueError, "route, fault or escrow"):
            gate.inspect_transfer_entities(observed, expected)

    def test_legacy_missing_keys_does_not_allow_positive_escrow(self):
        observed, expected = transfer_fixture(legacy=True)
        observed[(0, 80, 2)]["parts"][0]["itemEscrowCount"] = 1
        with self.assertRaisesRegex(ValueError, "route, fault or escrow"):
            gate.inspect_transfer_entities(observed, expected, allow_legacy_missing=True)

    def test_wrong_native_source_balance_fails(self):
        observed, expected = transfer_fixture()
        observed[(1, 80, 2)]["Items"][0]["count"] = 63
        with self.assertRaisesRegex(ValueError, "counts"):
            gate.inspect_transfer_entities(observed, expected)

    def test_forged_native_transfer_part_owner_fails(self):
        observed, expected = transfer_fixture()
        observed[(4, 80, 2)]["parts"][0]["owner"] = [0, 0, 0, 0]
        with self.assertRaisesRegex(ValueError, "owner/UUID"):
            gate.inspect_transfer_entities(observed, expected)

    def test_wrong_native_mode_fails(self):
        observed, expected = transfer_fixture()
        observed[(4, 80, 2)]["parts"][0]["transferMode"] = 2
        with self.assertRaisesRegex(ValueError, "route, fault or escrow"):
            gate.inspect_transfer_entities(observed, expected)


def cable_fixture():
    """Synthetic parsed NBT: positive control + malicious mutation cases only."""
    cable_cells = gate.CABLE_CELLS
    source = "aaaa0000-0000-4000-8000-000000000001"
    sink = "bbbb0000-0000-4000-8000-000000000002"
    cable_ids = ["cccc0000-0000-4000-8000-%012d" % i for i in range(1, 8)]

    def ints(value):
        return list(struct.unpack(">4i", uuid.UUID(value).bytes))

    targets = {}
    for i, pos in enumerate(cable_cells):
        parts = [{
            "kind": "datacable", "face": 0, "blockedFaces": 0,
            "owner": ints(gate.PART_OWNER), "identity": ints(cable_ids[i]),
        }]
        if i == 0 or i == 6:
            parts.append({
                "kind": "transfernode", "face": 5, "transferMode": 0,
                "items": 0, "fluids": 0, "energy": 0,
                "owner": ints(gate.PART_OWNER), "identity": ints(source if i == 0 else sink),
            })
        targets[pos] = {"id": "foundations_pl4:host", "schema": 2, "parts": parts}
    return targets, {"source_identity": source, "sink_identity": sink,
                     "cable_identities": cable_ids}


class NativeInstalledCableNBTAudit(unittest.TestCase):
    def test_exact_native_cable_cells_and_endpoints(self):
        targets, receipt = cable_fixture()
        result = gate.inspect_connected_cables(targets, receipt)
        self.assertEqual(result["cable_count"], 7)
        self.assertEqual(result["endpoint_count"], 2)

    def test_missing_loaded_bridge_cable_fails(self):
        targets, receipt = cable_fixture()
        del targets[gate.CABLE_CELLS[3]]
        with self.assertRaisesRegex(ValueError, "Missing actual serialized"):
            gate.inspect_connected_cables(targets, receipt)

    def test_blocked_native_bridge_arm_fails(self):
        targets, receipt = cable_fixture()
        targets[gate.CABLE_CELLS[3]]["parts"][0]["blockedFaces"] = 1 << 5
        with self.assertRaisesRegex(ValueError, "arm-disabled"):
            gate.inspect_connected_cables(targets, receipt)

    def test_duplicate_cable_identity_fails(self):
        targets, receipt = cable_fixture()
        targets[gate.CABLE_CELLS[3]]["parts"][0]["identity"] = (
            targets[gate.CABLE_CELLS[2]]["parts"][0]["identity"])
        with self.assertRaisesRegex(ValueError, "UUID"):
            gate.inspect_connected_cables(targets, receipt)

    def test_forged_endpoint_owner_fails(self):
        targets, receipt = cable_fixture()
        targets[gate.CABLE_CELLS[6]]["parts"][1]["owner"] = [0, 0, 0, 0]
        with self.assertRaisesRegex(ValueError, "passive/owned"):
            gate.inspect_connected_cables(targets, receipt)

    def test_unexpected_extra_multipart_fails(self):
        targets, receipt = cable_fixture()
        targets[gate.CABLE_CELLS[1]]["parts"].append(dict(targets[gate.CABLE_CELLS[1]]["parts"][0]))
        with self.assertRaisesRegex(ValueError, "multipart count"):
            gate.inspect_connected_cables(targets, receipt)

    def test_endpoint_transfer_becoming_active_fails(self):
        targets, receipt = cable_fixture()
        targets[gate.CABLE_CELLS[0]]["parts"][1]["transferMode"] = 2
        with self.assertRaisesRegex(ValueError, "passive/owned"):
            gate.inspect_connected_cables(targets, receipt)



class ScheduledNativeAnvilInventoryPolicy(unittest.TestCase):
    def test_scheduled_barrels_require_actual_saved_quantities(self):
        targets, graph = cable_fixture()
        targets[(1, 80, 5)] = {"id": "minecraft:barrel", "Items": [
            {"id": "minecraft:diamond", "Slot": i, "count": 64} for i in range(26)]}
        targets[(5, 80, 5)] = {"id": "minecraft:barrel", "Items": [
            {"id": "minecraft:diamond", "Slot": 0, "count": 64}]}
        scheduled = {"source_identity": graph["source_identity"],
                     "sink_identity": graph["sink_identity"],
                     "source_after": 1664, "sink_after": 64}
        self.assertEqual(
            gate.inspect_scheduled_transfer_entities(targets, scheduled, graph)["item_total"], 1728)
        targets[(5, 80, 5)]["Items"][0]["count"] = 63
        with self.assertRaisesRegex(ValueError, "scheduled NetworkEngine inventory"):
            gate.inspect_scheduled_transfer_entities(targets, scheduled, graph)

    def test_scheduled_graph_mismatched_endpoint_fails(self):
        targets, graph = cable_fixture()
        expected = {"source_identity": "ffffffff-ffff-4fff-8fff-ffffffffffff",
                    "sink_identity": graph["sink_identity"]}
        with self.assertRaisesRegex(ValueError, "not cable-owned endpoints"):
            gate.inspect_scheduled_transfer_entities(targets, expected, graph)

if __name__ == "__main__":
    unittest.main()

