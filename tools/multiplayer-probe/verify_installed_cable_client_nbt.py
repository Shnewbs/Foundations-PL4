"""Read-only raw Anvil acceptance: installed two-client 1.21.1 cable delivery.

Separate from the native server JSON and source/JAR/run receipt gates. No
provider is invoked, no world is mutated, no RC clearance is implied.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile

import installed_binary_two_clients as gate
from verify_installed_client_nbt import REGION, _bounded_zip_read
from verify_native_multipart_nbt import decode_region, uuid_int_array

CELLS = ((0,80,5),(0,80,6),(1,80,6),(2,80,6),
         (3,80,6),(4,80,6),(4,80,5))
BARRELS = ((1,80,5),(5,80,5))


def _diamonds(entity):
    if not isinstance(entity, dict) or entity.get("id") != "minecraft:barrel":
        raise ValueError("Actual saved native cable barrel is missing")
    items = entity.get("Items")
    if not isinstance(items, list):
        raise ValueError("Saved cable inventory Items NBT is absent")
    total, slots = 0, set()
    for item in items:
        if not isinstance(item, dict) or item.get("id") != "minecraft:diamond":
            raise ValueError("Unexpected native cable provider inventory item")
        slot = item.get("Slot")
        count = item.get("count", item.get("Count"))
        if (type(slot) is not int or not 0 <= slot < 27 or slot in slots
                or type(count) is not int or not 1 <= count <= 64):
            raise ValueError("Malformed saved native cable item slot/count")
        slots.add(slot)
        total += count
    return total


def inspect_cable_chunk(chunk, server, phase):
    if phase not in ("seed","restore") or chunk.get("xPos") != 0 or chunk.get("zPos") != 0:
        raise ValueError("Wrong saved native cable chunk/phase")
    proof = server.get("cable_transfer")
    if not isinstance(proof, dict) or len(proof.get("identities", [])) != 9:
        raise ValueError("Missing installed cable native receipt")
    ids = proof["identities"]
    expected_owner = server["real_client_uuids"][0]
    if not isinstance(expected_owner, str) or not re.fullmatch(
            r"[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}", expected_owner):
        raise ValueError("Unbound native installed cable owner")
    found = {}
    entities = chunk.get("block_entities")
    if not isinstance(entities, list):
        raise ValueError("Saved cable chunk has no real native block entities")
    positions = set(CELLS) | set(BARRELS)
    for entity in entities:
        if not isinstance(entity, dict):
            raise ValueError("Malformed native cable chunk block entity")
        xyz = (entity.get("x"), entity.get("y"), entity.get("z"))
        if xyz in positions:
            if xyz in found:
                raise ValueError("Duplicate or substituted native cable block entity")
            found[xyz] = entity
    if set(found) != positions:
        raise ValueError("Missing saved native seven-centre cable or barrel")
    for i, xyz in enumerate(CELLS):
        h = found[xyz]
        if h.get("id") != "foundations_pl4:host" or h.get("schema") != 2:
            raise ValueError("Actual PL4 host missing from native cable world")
        parts = h.get("parts")
        if not isinstance(parts,list) or len(parts) != (2 if i in (0,6) else 1):
            raise ValueError("Saved cable multipart host part count changed")
        kinds = {}
        for p in parts:
            if not isinstance(p,dict) or p.get("kind") not in ("datacable","transfernode"):
                raise ValueError("Unexpected native saved cable part kind")
            k=p["kind"]
            if k in kinds:
                raise ValueError("Duplicate native saved cable part kind")
            kinds[k]=p
        cable=kinds.get("datacable")
        if not isinstance(cable,dict) or cable.get("face") != 0:
            raise ValueError("Saved native centre DATA_CABLE missing")
        expected_cable=ids[i+2]
        if uuid_int_array(cable.get("owner")) != expected_owner or uuid_int_array(cable.get("identity")) != expected_cable:
            raise ValueError("Native cable owner/UUID changed across installed binaries")
        if type(cable.get("blockedFaces",0)) is not int or cable.get("blockedFaces",0) != 0:
            raise ValueError("Saved cable remained disconnected after native rejoin")
        if i in (0,6):
            endpoint=kinds.get("transfernode")
            if not isinstance(endpoint,dict) or endpoint.get("face") != 5:
                raise ValueError("Saved physical transfer endpoint face changed")
            if (uuid_int_array(endpoint.get("owner")) != expected_owner
                    or uuid_int_array(endpoint.get("identity")) != ids[0 if i==0 else 1]):
                raise ValueError("Native transfer endpoint identity/owner changed")
            if (type(endpoint.get("transferMode")) is not int
                    or endpoint["transferMode"] != 0
                    or endpoint.get("items") != 0):
                raise ValueError("Native automatic transfer driver was not safely disarmed")
        elif "transfernode" in kinds:
            raise ValueError("Unexpected native transfer endpoint on intermediate cable")
        for p in parts:
            if (p.get("resourceEscrow",0) != 0 or p.get("itemEscrowCount",0) != 0
                    or p.get("itemTransferFault","") != "" or "pendingItem" in p):
                raise ValueError("Native cable/endpoint retained unresolved escrow or fault")
            if phase == "restore" and ("itemEscrowCount" not in p or "itemTransferFault" not in p):
                raise ValueError("Candidate native cable part lacks escrow safety serialization")
    source=_diamonds(found[BARRELS[0]])
    sink=_diamonds(found[BARRELS[1]])
    if (source != proof.get("source_after") or sink != proof.get("sink_after")
            or source+sink != 1728):
        raise ValueError("Raw saved native cable barrel quantities do not conserve inventory")
    return {"identities":ids,"owner":expected_owner,
            "source_diamonds":source,"sink_diamonds":sink,
            "total_diamonds":1728,"cable_centres":7,"endpoints":2,
            "unresolved_escrow":0}


def inspect_world(folder, phase, source, jar):
    folder=Path(folder)
    native_log=(folder/"server.log").read_text(errors="replace")
    server=json.loads((folder/"server-multiplayer-proof.json").read_text())
    gate.verify_cable_receipt(server,native_log,phase,jar)
    if server.get("source_commit") != source or server.get("phase") != phase:
        raise ValueError("Saved native cable source provenance differs from client/server receipt")
    archive=folder/("saved-world-"+phase+".zip")
    with zipfile.ZipFile(archive) as z:
        entries=z.namelist()
        if len(entries)!=len(set(entries)) or z.testzip() is not None:
            raise ValueError("Corrupt or duplicated native installed cable world ZIP")
        raw=_bounded_zip_read(z,REGION)
    result=inspect_cable_chunk(decode_region(raw),server,phase)
    return {"phase":phase,"source":source,"jar_sha256":jar,
            "archive_sha256":hashlib.sha256(archive.read_bytes()).hexdigest(),
            "region_sha256":hashlib.sha256(raw).hexdigest(),"native":result}


def verify_upgrade(seed,restore):
    # First require actual production-JAR plus two graphical clients in both phases.
    gate.verify_upgrade(seed,restore)
    a=inspect_world(seed,"seed",gate.BASELINE_SOURCE,gate.BASELINE_JAR)
    b=inspect_world(restore,"restore",gate.CANDIDATE_SOURCE,gate.CANDIDATE_JAR)
    if (a["native"]["identities"] != b["native"]["identities"]
            or a["native"]["owner"] != b["native"]["owner"]
            or a["native"]["source_diamonds"] <= b["native"]["source_diamonds"]
            or a["native"]["sink_diamonds"] >= b["native"]["sink_diamonds"]):
        raise ValueError("Native two-client cable migration identity or delivery is not preserved")
    report={"schema":1,"result":"PASS_SCOPED_NATIVE_CABLE_TWO_CLIENT_NBT",
            "seed":a,"restore":b,"online_authenticated":False,
            "installed_optional_providers":False,"arbitrary_saved_world":False,
            "sustained_performance":False,"rc_publication_cleared":False}
    (Path(restore)/"native-installed-cable-client-nbt-proof.json").write_text(
        json.dumps(report,indent=2,sort_keys=True)+"\n")
    return report


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--seed",type=Path,required=True)
    p.add_argument("--restore",type=Path,required=True)
    args=p.parse_args()
    result=verify_upgrade(args.seed,args.restore)
    print(json.dumps({"result":result["result"],
                      "seed_source_items":result["seed"]["native"]["source_diamonds"],
                      "restored_sink_items":result["restore"]["native"]["sink_diamonds"]},
                     sort_keys=True))


if __name__=="__main__":
    main()
