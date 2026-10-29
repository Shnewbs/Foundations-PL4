"""Independent archived NBT acceptance for exact installed two-client 1.21.1 upgrade.

Never treats runtime JSON as saved-world proof, mutates the world, or grants RC
clearance. Scoped to the pinned private offline client fixture only.
"""
import argparse
import gzip
import hashlib
import io
import json
from pathlib import Path
import re
import uuid
import zipfile

from verify_native_multipart_nbt import decode_region, parse_chunk, uuid_int_array

REGION = "region/r.0.0.mca"
MAX_ENTRY = 16 * 1024 * 1024


def _bounded_zip_read(archive, name):
    found = [entry for entry in archive.infolist() if entry.filename == name]
    if len(found) != 1 or found[0].file_size > MAX_ENTRY or found[0].is_dir():
        raise ValueError("Missing, duplicate or oversized saved native NBT: " + name)
    return archive.read(found[0])


def _saved_player_nbt(compressed):
    with gzip.GzipFile(fileobj=io.BytesIO(compressed)) as stream:
        raw = stream.read(MAX_ENTRY + 1)
    if len(raw) > MAX_ENTRY:
        raise ValueError("Saved player NBT exceeds bounded decoded size")
    return parse_chunk(raw)


def inspect_player(player, expected_uuid, expected_node):
    if uuid_int_array(player.get("UUID")) != expected_uuid:
        raise ValueError("Saved player UUID conflicts with actual client identity")
    inventory = player.get("Inventory")
    if not isinstance(inventory, list) or len(inventory) != 2:
        raise ValueError("Saved client inventory must have exactly one link and one stack")
    slots, diamonds, links = set(), 0, 0
    for item in inventory:
        if not isinstance(item, dict):
            raise ValueError("Malformed saved client item entry")
        slot, count, identifier = item.get("Slot"), item.get("count"), item.get("id")
        if type(slot) is not int or not 0 <= slot < 36 or slot in slots or type(count) is not int:
            raise ValueError("Invalid or duplicate saved client inventory slot")
        slots.add(slot)
        if identifier == "minecraft:diamond" and count == 17:
            diamonds += count
        elif identifier == "foundations_pl4:wirelessstorage" and count == 1:
            link = item.get("components", {}).get("minecraft:custom_data", {}).get("pl_link")
            if not isinstance(link, dict) or link.get("dimension") != "minecraft:overworld" or link.get("side") != 5 or link.get("pos") != 80 or uuid_int_array(link.get("part")) != expected_node:
                raise ValueError("Saved wireless tool binding targets wrong node")
            links += 1
        else:
            raise ValueError("Unexpected native item, altered stack count or unsafe saved link")
    if diamonds != 17 or links != 1 or slots != {0, 1}:
        raise ValueError("Saved client item counts or wireless binding slots differ")
    return {"player_uuid": expected_uuid, "diamonds": diamonds,
            "bound_node_uuid": expected_node, "wireless_tools": links}


def inspect_native_chunk(chunk, expected_owner, expected_node, require_candidate_fields):
    if chunk.get("xPos") != 0 or chunk.get("zPos") != 0:
        raise ValueError("Native saved region origin chunk coordinates invalid")
    entities = chunk.get("block_entities")
    if not isinstance(entities, list):
        raise ValueError("Native saved origin chunk has no block entities")
    found = {}
    for entity in entities:
        if not isinstance(entity, dict):
            raise ValueError("Malformed native saved entity")
        position = tuple(entity.get(key) for key in ("x", "y", "z"))
        if position in ((0, 80, 0), (1, 80, 0)):
            if position in found:
                raise ValueError("Ambiguous duplicate native fixture block entity")
            found[position] = entity
    host, barrel = found.get((0, 80, 0)), found.get((1, 80, 0))
    if not isinstance(host, dict) or host.get("id") != "foundations_pl4:host" or host.get("schema") != 2:
        raise ValueError("Actual saved PL4 multipart host missing")
    parts = host.get("parts")
    if not isinstance(parts, list) or len(parts) != 2:
        raise ValueError("Native multipart host lost its two saved parts")
    observed = {}
    for part in parts:
        if not isinstance(part, dict):
            raise ValueError("Malformed saved PL4 multipart part")
        kind, face = part.get("kind"), part.get("face")
        if kind not in ("node", "datacable") or type(face) is not int or face != (5 if kind == "node" else 0) or kind in observed:
            raise ValueError("Native saved PL4 part face/kind/slot mismatch")
        if uuid_int_array(part.get("owner")) != expected_owner:
            raise ValueError("Saved PL4 part changed ownership")
        identity = uuid_int_array(part.get("identity"))
        if kind == "node" and identity != expected_node:
            raise ValueError("Saved native wireless node identity changed")
        if (part.get("resourceEscrow") != 0 or part.get("itemEscrowCount", 0) != 0
                or part.get("itemTransferFault", "") != "" or "pendingItem" in part):
            raise ValueError("Saved native multipart escrow/fault is not clear")
        if require_candidate_fields and ("itemEscrowCount" not in part or "itemTransferFault" not in part):
            raise ValueError("Candidate native part lacks modern escrow serialization")
        observed[kind] = identity
    if len(observed) != 2 or len(set(observed.values())) != 2:
        raise ValueError("Native saved PL4 part identities missing or duplicated")
    if not isinstance(barrel, dict) or barrel.get("id") != "minecraft:barrel":
        raise ValueError("Native saved item provider barrel missing")
    items = barrel.get("Items")
    if not isinstance(items, list) or len(items) != 2:
        raise ValueError("Saved barrel must contain exactly two diamond stacks")
    slots = {}
    for item in items:
        if not isinstance(item, dict) or item.get("id") != "minecraft:diamond":
            raise ValueError("Unexpected saved native barrel item")
        slot, count = item.get("Slot"), item.get("count", item.get("Count"))
        if type(slot) is not int or type(count) is not int or slot in slots or not 1 <= count <= 64:
            raise ValueError("Malformed native saved barrel stack")
        slots[slot] = count
    if slots != {0: 30, 1: 64}:
        raise ValueError("Native saved barrel did not retain exactly 94 diamonds")
    return {"node_uuid": observed["node"], "cable_uuid": observed["datacable"],
            "owner_uuid": expected_owner, "barrel_diamonds": 94, "item_escrow": 0}


def inspect_world(archive_path, phase, source, jar_sha, server):
    archive_path = Path(archive_path)
    if phase not in ("seed", "restore") or not re.fullmatch(r"[0-9a-f]{40}", source) or not re.fullmatch(r"[0-9a-f]{64}", jar_sha):
        raise ValueError("Unpinned native two-client source/JAR/phase")
    if server.get("source_commit") != source or server.get("phase") != phase or server.get("real_client_names") != ["PL4ProbeA", "PL4ProbeB"]:
        raise ValueError("Saved-world source or client receipt identity mismatch")
    ids = server.get("real_client_uuids")
    if not isinstance(ids, list) or len(ids) != 2 or len(set(ids)) != 2 or any(not isinstance(v, str) or not re.fullmatch(r"[0-9a-f-]{36}", v) for v in ids):
        raise ValueError("Invalid or duplicate saved player identities")
    node = server.get("node_identity")
    if not isinstance(node, str) or not re.fullmatch(r"[0-9a-f-]{36}", node):
        raise ValueError("Invalid native saved wireless node identity")
    with zipfile.ZipFile(archive_path) as z:
        entries = z.namelist()
        if len(set(entries)) != len(entries) or z.testzip() is not None:
            raise ValueError("Corrupt or ambiguous saved native world ZIP")
        live = {n for n in entries if n.startswith("playerdata/") and n.endswith(".dat")}
        expected = {"playerdata/" + v + ".dat" for v in ids}
        if live != expected:
            raise ValueError("Missing, extra or substituted saved client playerdata")
        clients = [inspect_player(_saved_player_nbt(_bounded_zip_read(z, "playerdata/" + ident + ".dat")), ident, node) for ident in ids]
        region = _bounded_zip_read(z, REGION)
    native = inspect_native_chunk(decode_region(region), ids[0], node, phase == "restore")
    if native["barrel_diamonds"] + sum(player["diamonds"] for player in clients) != 128:
        raise ValueError("Raw saved client and barrel inventories are not conserved")
    return {"phase": phase, "source_commit": source, "jar_sha256": jar_sha,
            "archive_sha256": hashlib.sha256(archive_path.read_bytes()).hexdigest(),
            "native_region_sha256": hashlib.sha256(region).hexdigest(),
            "players": clients, "native": native, "total_diamonds": 128,
            "rc_publication_cleared": False}


def verify_upgrade(seed, restore, baseline_source, baseline_jar, candidate_source, candidate_jar):
    # The installed-binary runtime gate has already validated both phase
    # manifests, actual GUI client process exits, screenshot and log hashes.
    from installed_binary_two_clients import verify_phase
    from verify_upgrade_evidence import digest
    before = verify_phase(seed, "seed", baseline_source, baseline_jar)
    after = verify_phase(restore, "restore", candidate_source, candidate_jar)
    if after.get("copied_input_world") != before.get("world"):
        raise ValueError("Candidate native world was not copied from validated baseline")
    first = inspect_world(Path(seed) / "saved-world-seed.zip", "seed", baseline_source, baseline_jar,
                          json.loads((Path(seed) / "server-multiplayer-proof.json").read_text()))
    second = inspect_world(Path(restore) / "saved-world-restore.zip", "restore", candidate_source, candidate_jar,
                           json.loads((Path(restore) / "server-multiplayer-proof.json").read_text()))
    if (first["players"] != second["players"] or first["native"] != second["native"]):
        raise ValueError("Native playerdata/host/barrel UUIDs or balances changed across candidate upgrade")
    result = {"schema": 1, "result": "PASS_SCOPED_NATIVE_PLAYERDATA_AND_MULTIPART_NBT",
              "seed": first, "restore": second, "installed_two_clients_each_phase": True,
              "installed_optional_providers": False, "online_authentication": False,
              "arbitrary_world_migration": False, "rc_publication_cleared": False}
    out = Path(restore) / "native-installed-client-nbt-proof.json"
    out.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n")
    return result


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--seed", required=True, type=Path)
    p.add_argument("--restore", required=True, type=Path)
    args = p.parse_args()
    from installed_binary_two_clients import BASELINE_SOURCE, BASELINE_JAR, CANDIDATE_SOURCE, CANDIDATE_JAR
    result = verify_upgrade(args.seed, args.restore, BASELINE_SOURCE, BASELINE_JAR,
                            CANDIDATE_SOURCE, CANDIDATE_JAR)
    print(json.dumps({"result": result["result"], "seed_region": result["seed"]["native_region_sha256"],
                      "restore_region": result["restore"]["native_region_sha256"]}, sort_keys=True))


if __name__ == "__main__":
    main()
