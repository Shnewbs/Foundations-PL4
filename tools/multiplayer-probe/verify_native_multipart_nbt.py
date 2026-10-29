"""Independent native Anvil region-NBT acceptance for installed PL4 binary upgrades.

Read-only: inspect both retained world ZIPs, never mutate the installed world,
invoke providers, invent inventory movement, or grant RC clearance.
"""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import struct
import uuid
import zipfile
import zlib

from installed_binary_world_upgrade import check_world_proof, PART_LAYOUT, PART_OWNER

MAX_NBT_BYTES = 16 * 1024 * 1024
MAX_ELEMENTS = 262144
REGION = "region/r.0.0.mca"


class Reader:
    def __init__(self, raw):
        if len(raw) > MAX_NBT_BYTES:
            raise ValueError("Native Anvil chunk exceeds bounded NBT limit")
        self.raw = raw
        self.pos = 0

    def read(self, amount):
        if amount < 0 or self.pos + amount > len(self.raw):
            raise ValueError("Truncated native Anvil NBT payload")
        result = self.raw[self.pos:self.pos + amount]
        self.pos += amount
        return result

    def number(self, code):
        return struct.unpack(">" + code, self.read(struct.calcsize(">" + code)))[0]

    def string(self):
        count = self.number("H")
        if count > 32767:
            raise ValueError("Oversized NBT string")
        return self.read(count).decode("utf-8", errors="strict")

    def payload(self, kind, depth=0):
        if depth > 64:
            raise ValueError("Native NBT nesting exceeds limit")
        if kind == 1:
            return self.number("b")
        if kind == 2:
            return self.number("h")
        if kind == 3:
            return self.number("i")
        if kind == 4:
            return self.number("q")
        if kind == 5:
            return self.number("f")
        if kind == 6:
            return self.number("d")
        if kind in (7, 11, 12):
            count = self.number("i")
            width = {7: 1, 11: 4, 12: 8}[kind]
            if count < 0 or count > MAX_ELEMENTS or count * width > MAX_NBT_BYTES:
                raise ValueError("Malformed native NBT array length")
            raw = self.read(count * width)
            if kind == 7:
                return raw
            return list(struct.unpack(">" + str(count) + ("i" if kind == 11 else "q"), raw))
        if kind == 8:
            return self.string()
        if kind == 9:
            item_kind = self.number("B")
            count = self.number("i")
            if count < 0 or count > MAX_ELEMENTS or (item_kind == 0 and count):
                raise ValueError("Malformed native NBT list")
            return [self.payload(item_kind, depth + 1) for _ in range(count)]
        if kind == 10:
            fields = {}
            while True:
                item_kind = self.number("B")
                if item_kind == 0:
                    return fields
                if item_kind > 12:
                    raise ValueError("Unsupported native NBT tag")
                name = self.string()
                if name in fields:
                    raise ValueError("Duplicate native NBT compound name")
                fields[name] = self.payload(item_kind, depth + 1)
        raise ValueError("Invalid native NBT tag type")


def parse_chunk(raw):
    reader = Reader(raw)
    if reader.number("B") != 10:
        raise ValueError("Native Anvil root is not a compound")
    reader.string()
    result = reader.payload(10)
    if reader.pos != len(raw):
        raise ValueError("Extra native Anvil NBT bytes after root")
    return result


def decode_region(raw):
    if len(raw) < 8192 or len(raw) % 4096:
        raise ValueError("Native region is not sector-aligned")
    sector = int.from_bytes(raw[:3], "big")
    count = raw[3]
    if sector < 2 or count < 1:
        raise ValueError("Native origin chunk has no valid region offset")
    start = sector * 4096
    if start + count * 4096 > len(raw):
        raise ValueError("Native origin chunk points outside Anvil region")
    size = struct.unpack_from(">I", raw, start)[0]
    if size < 2 or size > count * 4096 - 4:
        raise ValueError("Native origin chunk has invalid compressed length")
    codec = raw[start + 4]
    payload = raw[start + 5:start + 4 + size]
    if codec == 1:
        unpacked = gzip.decompress(payload)
    elif codec == 2:
        unpacked = zlib.decompress(payload)
    elif codec == 3:
        unpacked = payload
    else:
        raise ValueError("Unsupported or external native Anvil chunk compression")
    return parse_chunk(unpacked)


def uuid_int_array(values):
    if not isinstance(values, list) or len(values) != 4 or any(type(n) is not int for n in values):
        raise ValueError("Native PL4 part UUID missing or malformed")
    bits = 0
    for value in values:
        bits = (bits << 32) | (value & 0xffffffff)
    return str(uuid.UUID(int=bits))


def _native_diamonds(entity, xyz):
    if entity.get("id") != "minecraft:barrel":
        raise ValueError("Wrong installed native transfer barrel identity at " + str(xyz))
    items = entity.get("Items")
    if not isinstance(items, list):
        raise ValueError("Native installed transfer barrel lacks its saved inventory")
    slots = set()
    total = 0
    for item in items:
        if not isinstance(item, dict) or item.get("id") != "minecraft:diamond":
            raise ValueError("Unexpected item in native installed transfer inventory")
        slot = item.get("Slot")
        count = item.get("count", item.get("Count"))
        if (type(slot) is not int or not 0 <= slot < 27 or slot in slots
                or type(count) is not int or not 1 <= count <= 64):
            raise ValueError("Malformed persisted native installed transfer item stack")
        slots.add(slot)
        total += count
    return total


def inspect_transfer_entities(targets, expected, allow_legacy_missing=False):
    """Require direct saved Anvil NBT, not only the runtime JSON transfer receipt."""
    if not isinstance(expected, dict):
        raise ValueError("Native installed transfer proof is missing")
    identities = []
    for position, mode, field in (((0, 80, 2), 2, "source_identity"),
                                  ((4, 80, 2), 1, "sink_identity")):
        entity = targets.get(position)
        if (not isinstance(entity, dict)
                or entity.get("id") != "foundations_pl4:host"
                or entity.get("schema") != 2):
            raise ValueError("Missing real persisted native installed transfer host")
        parts = entity.get("parts")
        if not isinstance(parts, list) or len(parts) != 1:
            raise ValueError("Missing/ambiguous native saved transfer part")
        part = parts[0]
        if (not isinstance(part, dict)
                or part.get("kind") != "transfernode"
                or type(part.get("face")) is not int or part["face"] != 5
                or type(part.get("transferMode")) is not int or part["transferMode"] != mode
                or part.get("items") != 1 or part.get("fluids") != 0 or part.get("energy") != 0
                or part.get("resourceEscrow") != 0
                or part.get("itemEscrowCount", 0) != 0
                or part.get("itemTransferFault", "") != ""
                or (not allow_legacy_missing and (
                    "itemEscrowCount" not in part or "itemTransferFault" not in part))
                or "pendingItem" in part):
            raise ValueError("Native saved transfer part route, fault or escrow changed")
        owner = uuid_int_array(part.get("owner"))
        identity = uuid_int_array(part.get("identity"))
        if owner != PART_OWNER or identity != expected.get(field):
            raise ValueError("Native installed transfer owner/UUID does not match active proof")
        identities.append(identity)
    if identities[0] == identities[1]:
        raise ValueError("Native installed transfer part identities duplicated")
    source = _native_diamonds(targets.get((1, 80, 2), {}), (1, 80, 2))
    sink = _native_diamonds(targets.get((5, 80, 2), {}), (5, 80, 2))
    if (source != expected.get("source_after")
            or sink != expected.get("sink_after")
            or source + sink != 1728):
        raise ValueError("Raw saved native transferred item counts do not match real engine output")
    return {"source_diamonds": source, "sink_diamonds": sink,
            "source_identity": identities[0], "sink_identity": identities[1],
            "item_total": 1728, "item_escrow": 0}



# Seven genuinely placed cable centres and two attached passive transfer ports.
# All cells are in origin Anvil chunk 0,0; never inspect a manufactured JSON row
# in place of the saved region data.
CABLE_CELLS = ((0, 80, 5), (0, 80, 6), (1, 80, 6),
               (2, 80, 6), (3, 80, 6), (4, 80, 6), (4, 80, 5))


def inspect_connected_cables(targets, expected):
    """Independently require nine exact installed-parts in native saved NBT."""
    if not isinstance(expected, dict):
        raise ValueError("Native installed DATA_CABLE topology proof is missing")
    cable_ids = expected.get("cable_identities")
    if not isinstance(cable_ids, list) or len(cable_ids) != len(CABLE_CELLS):
        raise ValueError("Native DATA_CABLE proof needs seven distinct part UUIDs")
    observed = []
    identities = set()
    for index, position in enumerate(CABLE_CELLS):
        host = targets.get(position)
        if (not isinstance(host, dict) or host.get("id") != "foundations_pl4:host"
                or host.get("schema") != 2):
            raise ValueError("Missing actual serialized native DATA_CABLE host: " + str(position))
        parts = host.get("parts")
        endpoint_index = 0 if index == 0 else 1 if index == len(CABLE_CELLS) - 1 else None
        expected_count = 2 if endpoint_index is not None else 1
        if not isinstance(parts, list) or len(parts) != expected_count:
            raise ValueError("Invalid physical cable centre/endpoint multipart count")
        cables = [p for p in parts if isinstance(p, dict) and p.get("kind") == "datacable"]
        if len(cables) != 1:
            raise ValueError("Missing or ambiguous serialized native DATA_CABLE")
        cable = cables[0]
        if (type(cable.get("face")) is not int or cable["face"] != 0
                or type(cable.get("blockedFaces")) is not int or cable["blockedFaces"] != 0
                or uuid_int_array(cable.get("owner")) != PART_OWNER):
            raise ValueError("Native saved cable face/owner/arm-disabled mismatch")
        identity = uuid_int_array(cable.get("identity"))
        if identity != cable_ids[index] or identity in identities:
            raise ValueError("Native DATA_CABLE part UUID differs from installed proof")
        identities.add(identity)
        if endpoint_index is not None:
            endpoints = [p for p in parts if isinstance(p, dict)
                         and p.get("kind") == "transfernode"]
            if len(endpoints) != 1:
                raise ValueError("Missing passive installed transfer-node endpoint")
            endpoint = endpoints[0]
            expected_id = expected.get("source_identity" if endpoint_index == 0 else "sink_identity")
            if (type(endpoint.get("face")) is not int or endpoint["face"] != 5
                    or type(endpoint.get("transferMode")) is not int or endpoint["transferMode"] != 0
                    or any(endpoint.get(k) != 0 for k in ("items", "fluids", "energy"))
                    or uuid_int_array(endpoint.get("owner")) != PART_OWNER):
                raise ValueError("Saved native topology endpoint is not passive/owned")
            endpoint_id = uuid_int_array(endpoint.get("identity"))
            if endpoint_id != expected_id or endpoint_id in identities:
                raise ValueError("Native topology endpoint UUID was replaced or reused")
            identities.add(endpoint_id)
        observed.append({"position": list(position), "cable_identity": identity,
                         "has_endpoint": endpoint_index is not None})
    if len(identities) != 9:
        raise ValueError("Native saved DATA_CABLE graph has missing/repeated identities")
    return {"cable_count": len(CABLE_CELLS), "endpoint_count": 2,
            "cable_identities": cable_ids,
            "source_identity": expected["source_identity"],
            "sink_identity": expected["sink_identity"],
            "native_cells": observed}


def inspect_chunk(chunk, expected_transfer=None, allow_legacy_missing=False, expected_topology=None):
    entities = chunk.get("block_entities")
    if not isinstance(entities, list):
        raise ValueError("Native chunk has no block_entities list")
    targets = {}
    for entity in entities:
        if not isinstance(entity, dict):
            raise ValueError("Malformed native block entity compound")
        xyz = tuple(entity.get(k) for k in ("x", "y", "z"))
        if xyz in ((0, 80, 0), (1, 80, 0), (0, 80, 2),
                    (1, 80, 2), (4, 80, 2), (5, 80, 2)) + CABLE_CELLS:
            if xyz in targets:
                raise ValueError("Duplicate native fixture block entity position")
            targets[xyz] = entity
    host = targets.get((0, 80, 0))
    barrel = targets.get((1, 80, 0))
    if (host is None or barrel is None
            or host.get("id") != "foundations_pl4:host"
            or barrel.get("id") != "minecraft:barrel"
            or host.get("schema") != 2):
        raise ValueError("Native installed PL4 host/barrel identity or schema missing")
    native_parts = host.get("parts")
    if not isinstance(native_parts, list) or len(native_parts) != len(PART_LAYOUT):
        raise ValueError("Native saved-world PL4 part list is empty/incomplete")
    by_slot = {}
    for part in native_parts:
        if not isinstance(part, dict):
            raise ValueError("Native PL4 part is not a compound")
        kind, face = part.get("kind"), part.get("face")
        if type(face) is not int:
            raise ValueError("Native PL4 part has invalid face")
        slot = 6 if kind == "datacable" else face
        if slot in by_slot:
            raise ValueError("Duplicate native PL4 multipart slot")
        by_slot[slot] = {
            "slot": slot,
            "kind": kind,
            "face": face,
            "owner": uuid_int_array(part.get("owner")),
            "identity": uuid_int_array(part.get("identity")),
        }
    parts = []
    for slot, kind, face in PART_LAYOUT:
        record = by_slot.get(slot)
        expected_face = {"EAST": 5, "DOWN": 0}[face]
        if (record is None or record["kind"] != kind.lower().replace("_", "")
                or record["face"] != expected_face or record["owner"] != PART_OWNER):
            raise ValueError("Native persisted PL4 part kind/slot/owner does not match fixture")
        parts.append({
            "slot": slot, "kind": kind, "face": face,
            "owner": record["owner"], "identity": record["identity"],
        })
    if len({p["identity"] for p in parts}) != len(parts):
        raise ValueError("Duplicate native persisted PL4 part identity")
    stacks = barrel.get("Items")
    if not isinstance(stacks, list) or len(stacks) != 2:
        raise ValueError("Native barrel did not preserve exactly two stacks")
    items = {}
    for item in stacks:
        if not isinstance(item, dict) or item.get("id") != "minecraft:diamond":
            raise ValueError("Unexpected native barrel inventory item")
        slot = item.get("Slot")
        quantity = item.get("count", item.get("Count"))
        if type(slot) is not int or type(quantity) is not int or slot in items:
            raise ValueError("Invalid native barrel slot or item quantity")
        items[slot] = quantity
    if items != {0: 64, 1: 64}:
        raise ValueError("Native barrel inventory was not conserved")
    result = {"parts": parts, "barrel_diamonds": 128, "host_schema": 2}
    if expected_transfer is not None:
        result["transfer_nbt"] = inspect_transfer_entities(
            targets, expected_transfer, allow_legacy_missing=allow_legacy_missing)
    if expected_topology is not None:
        result["topology_nbt"] = inspect_connected_cables(targets, expected_topology)
    return result


def inspect_archive(phase_dir, phase, expected_sha):
    phase_dir = Path(phase_dir)
    proof = check_world_proof(phase_dir, phase, expected_sha)
    archive = phase_dir / ("saved-world-" + phase + ".zip")
    if not archive.is_file() or archive.is_symlink():
        raise ValueError("Missing actual saved native-world archive")
    with zipfile.ZipFile(archive) as z:
        if z.testzip() is not None or z.namelist().count(REGION) != 1:
            raise ValueError("Missing, duplicate or corrupt native region archive")
        data = z.read(REGION)
    observed = inspect_chunk(decode_region(data), expected_transfer=proof["transfer"],
                             allow_legacy_missing=(phase == "seed"),
                             expected_topology=proof["topology"])
    if observed["parts"] != proof["parts"] or observed["barrel_diamonds"] != proof["barrel_diamonds"]:
        raise ValueError("Raw saved native region NBT differs from installed-JAR runtime proof")
    return {
        "phase": phase, "archive_sha256": hashlib.sha256(archive.read_bytes()).hexdigest(),
        "region_sha256": hashlib.sha256(data).hexdigest(), **observed,
    }


def verify_native_upgrade(seed, restore, baseline_sha, candidate_sha):
    before = inspect_archive(seed, "seed", baseline_sha)
    after = inspect_archive(restore, "restore", candidate_sha)
    if before["parts"] != after["parts"]:
        raise ValueError("Actual serialized native PL4 multipart UUID/owner not retained across binaries")
    if before["topology_nbt"] != after["topology_nbt"]:
        raise ValueError("Native persisted DATA_CABLE/endpoint UUIDs changed between exact-JAR runs")
    src, dst = before["transfer_nbt"], after["transfer_nbt"]
    if (src["source_identity"] != dst["source_identity"]
            or src["sink_identity"] != dst["sink_identity"]
            or dst["source_diamonds"] >= src["source_diamonds"]
            or dst["sink_diamonds"] <= src["sink_diamonds"]):
        raise ValueError("Raw saved installed transfer inventories/identities did not progress")
    return {
        "schema": 1, "baseline_jar_sha256": baseline_sha,
        "candidate_jar_sha256": candidate_sha,
        "seed_native": before, "restore_native": after,
        "result": "PASS_NATIVE_SAVED_NBT_MULTIPART_AND_EXPLICIT_REF_TRANSFER",
        "client_executed": False, "live_item_transfer_tested": True,
        "live_network_topology_tested": True,
        "native_raw_cable_nbt_verified": True,
        "native_automatic_tick_routing_tested": False,
        "installed_optional_providers_tested": False,
        "rc_publication_cleared": False,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--seed", required=True, type=Path)
    parser.add_argument("--restore", required=True, type=Path)
    parser.add_argument("--baseline-sha256", required=True)
    parser.add_argument("--candidate-sha256", required=True)
    args = parser.parse_args()
    for value in (args.baseline_sha256, args.candidate_sha256):
        if len(value) != 64 or any(c not in "0123456789abcdef" for c in value):
            raise ValueError("Exact 64-character lowercase JAR digest required")
    result = verify_native_upgrade(
        args.seed, args.restore, args.baseline_sha256, args.candidate_sha256)
    output = args.restore / "native-multipart-nbt-proof.json"
    output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n")
    print(json.dumps(result, sort_keys=True))


if __name__ == "__main__":
    main()
