"""Exact-JAR NeoForge world-byte upgrade (server-side acceptance only).

Seed a disposable Minecraft 1.21.1 world under the immutable published beta
binary, shut it down cleanly, then reopen an exact byte copy with a separately
native-qualified candidate production JAR. PL4 implementation classes are never
compiled from source in this test. This does NOT test clients or clear any RC.
"""
import argparse
import json
from pathlib import Path
import re
import shutil

import installed_binary_smoke as smoke
from verify_upgrade_evidence import (
    digest, snapshot_world, world_fingerprint, zip_world_fingerprint,
)

HOST = "foundations_pl4:multipart_host"
ENTITY = "foundations_pl4:host"
WORLD_MARKER = "PL4_INSTALLED_WORLD_"
PART_OWNER = "3d83227f-3df1-4c66-a188-a2f271bb86b3"
PART_LAYOUT = ((5, "INVENTORY_READER", "EAST"), (6, "DATA_CABLE", "DOWN"))


def require_sha(value):
    if not re.fullmatch(r"[0-9a-f]{64}", value):
        raise ValueError("Required pinned 64-character lowercase SHA-256")
    return value


def check_world_proof(output, phase, expected_sha):
    """Require an actual ServerStartedEvent fixture and independently logged phase."""
    require_sha(expected_sha)
    if phase not in ("seed", "restore"):
        raise ValueError("Only explicit seed/restore world phases are supported")
    output = Path(output)
    native_log = output / "installed-server.log"
    expected_marker = WORLD_MARKER + phase.upper() + ":" + expected_sha
    if not native_log.is_file():
        raise ValueError("Missing installed production-binary native server log")
    markers = [line.strip() for line in native_log.read_text(errors="replace").splitlines()
               if WORLD_MARKER in line]
    if len(markers) != 1 or not markers[0].endswith(expected_marker):
        raise ValueError("Missing, repeated or wrong installed-JAR world-phase native marker")
    expected_proof = {
        "schema": 1, "phase": phase, "expected_jar_sha256": expected_sha,
        "host": HOST, "host_entity": ENTITY, "barrel_diamonds": 128,
        "server_started": True,
    }
    evidence = output / "installed-world-proof.json"
    if not evidence.is_file() or evidence.is_symlink():
        raise ValueError("Missing native installed-JAR configured multipart proof")
    proof = json.loads(evidence.read_text())
    if (set(proof) != set(expected_proof) | {"parts"}
            or any(proof.get(name) != value for name, value in expected_proof.items())):
        raise ValueError("Native installed-JAR block entity/inventory proof is absent or altered")
    parts = proof["parts"]
    if not isinstance(parts, list) or len(parts) != len(PART_LAYOUT):
        raise ValueError("Native installed PL4 world has no configured multipart pieces")
    identities = set()
    for part, (slot, kind, face) in zip(parts, PART_LAYOUT):
        if (not isinstance(part, dict)
                or set(part) != {"slot", "kind", "face", "owner", "identity"}
                or part["slot"] != slot or type(part["slot"]) is not int
                or part["kind"] != kind or part["face"] != face
                or part["owner"] != PART_OWNER
                or not isinstance(part["identity"], str)
                or not re.fullmatch(r"[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}", part["identity"])
                or part["identity"] in identities):
            raise ValueError("Native installed PL4 part kind/face/owner/identity changed")
        identities.add(part["identity"])
    return proof


def verify_phase(output, phase, expected_sha):
    output = Path(output)
    require_sha(expected_sha)
    manifest = json.loads((output / "world-phase-evidence.json").read_text())
    if (manifest.get("schema") != 1 or manifest.get("phase") != phase
            or manifest.get("installed_jar_sha256") != expected_sha
            or manifest.get("rc_publication_cleared") is not False
            or manifest.get("client_executed") is not False):
        raise ValueError("Wrong phase, exact-JAR identity or unsafe clearance")
    smoke_receipt = smoke.verify(output, output / "installed-server.log")
    if (smoke_receipt.get("installed_jar_sha256") != expected_sha
            or smoke_receipt.get("installed_binary_server_smoke") != "PASS"):
        raise ValueError("Native installed-server smoke receipt did not pass")
    check_world_proof(output, phase, expected_sha)
    required = {
        "prepared-installation.json", "installed-server.log",
        "installed-server-proof.json", "installed-server-receipt.json",
        "installed-world-proof.json", "saved-world-" + phase + ".zip",
    }
    if set(manifest.get("files", {})) != required:
        raise ValueError("Incomplete exact native-phase evidence manifest")
    for name, record in manifest["files"].items():
        file = output / name
        if (not file.is_file() or file.is_symlink()
                or set(record) != {"sha256", "bytes"}
                or record["sha256"] != digest(file)
                or record["bytes"] != file.stat().st_size):
            raise ValueError("Missing or altered installed-JAR world evidence: " + name)
    saved_archive = output / ("saved-world-" + phase + ".zip")
    if zip_world_fingerprint(saved_archive) != manifest.get("world"):
        raise ValueError("Retained native world ZIP differs from recorded saved NBT bytes")
    copied = manifest.get("copied_input_world")
    if phase == "seed" and copied is not None:
        raise ValueError("Seed world cannot claim a pre-existing source world")
    if phase == "restore" and (not isinstance(copied, dict)
                               or set(copied) != {"sha256", "file_count", "byte_count"}):
        raise ValueError("Candidate has no independently hashed baseline input world")
    return manifest


def capture_phase(output, phase, jar_sha, copied_world=None):
    """Record only after real native clean exit; never certify synthetic marker tests."""
    output = Path(output)
    check_world_proof(output, phase, jar_sha)
    world = world_fingerprint(output / "run/world")
    archive = output / ("saved-world-" + phase + ".zip")
    if snapshot_world(output / "run/world", archive) != world:
        raise ValueError("Native world changed during deterministic archive capture")
    files = {}
    for name in (
        "prepared-installation.json", "installed-server.log",
        "installed-server-proof.json", "installed-server-receipt.json",
        "installed-world-proof.json", archive.name,
    ):
        f = output / name
        if not f.is_file() or f.is_symlink() or not f.stat().st_size:
            raise ValueError("Required native phase output missing: " + name)
        files[name] = {"sha256": digest(f), "bytes": f.stat().st_size}
    result = {
        "schema": 1, "phase": phase, "installed_jar_sha256": jar_sha,
        "world": world, "copied_input_world": copied_world,
        "files": files, "client_executed": False,
        "rc_publication_cleared": False,
    }
    (output / "world-phase-evidence.json").write_text(
        json.dumps(result, indent=2, sort_keys=True) + "\n"
    )
    verify_phase(output, phase, jar_sha)
    return result


def seed_world(checkout, jar, output, jar_sha, timeout=1500):
    output = Path(output)
    require_sha(jar_sha)
    smoke.prepare(checkout, jar, output, jar_sha)
    smoke.run(output, timeout, world_phase="seed")
    return capture_phase(output, "seed", jar_sha)


def restore_world(checkout, jar, seed, output, beta_sha, candidate_sha, timeout=1500):
    seed, output = Path(seed), Path(output)
    require_sha(beta_sha)
    require_sha(candidate_sha)
    if beta_sha == candidate_sha:
        raise ValueError("Published baseline and development candidate must be distinct JARs")
    original = verify_phase(seed, "seed", beta_sha)
    if not (seed / "run/world").is_dir():
        raise ValueError("Missing native world directory; cannot restore from receipts alone")
    before = world_fingerprint(seed / "run/world")
    if before != original["world"]:
        raise ValueError("Seed world's live bytes differ from its retained native archive")
    smoke.prepare(checkout, jar, output, candidate_sha)
    destination = output / "run/world"
    if destination.exists():
        raise ValueError("Refusing to overlay an existing candidate world")
    shutil.copytree(seed / "run/world", destination, symlinks=False)
    if world_fingerprint(destination) != before:
        raise ValueError("Candidate's pre-launch saved-world copy is not byte-identical")
    smoke.run(output, timeout, world_phase="restore")
    result = capture_phase(output, "restore", candidate_sha, copied_world=before)
    verify_upgrade(seed, output, beta_sha, candidate_sha)
    return result


def verify_upgrade(seed, restore, beta_sha, candidate_sha):
    require_sha(beta_sha)
    require_sha(candidate_sha)
    if beta_sha == candidate_sha:
        raise ValueError("Cannot count the same production JAR as a binary-to-binary upgrade")
    seed = Path(seed)
    restore = Path(restore)
    before = verify_phase(seed, "seed", beta_sha)
    after = verify_phase(restore, "restore", candidate_sha)
    beta_parts = check_world_proof(seed, "seed", beta_sha)["parts"]
    candidate_parts = check_world_proof(restore, "restore", candidate_sha)["parts"]
    if beta_parts != candidate_parts:
        raise ValueError("Installed PL4 native multipart kind/slot/owner/UUID did not persist across JARs")
    if before["world"] != after["copied_input_world"]:
        raise ValueError("Exact saved baseline world was not the candidate's copied input")
    receipt = {
        "schema": 1, "published_beta_jar_sha256": beta_sha,
        "candidate_jar_sha256": candidate_sha,
        "source_world_sha256": before["world"]["sha256"],
        "candidate_world_sha256": after["world"]["sha256"],
        "beta_phase_evidence_sha256": digest(seed / "world-phase-evidence.json"),
        "candidate_phase_evidence_sha256": digest(restore / "world-phase-evidence.json"),
        "installed_beta_to_candidate_world_smoke": "PASS",
        "configured_multipart_parts_persisted": len(beta_parts),
        "configured_part_identities": [part["identity"] for part in beta_parts],
        "scope": "Two real installed production-JAR dedicated-server lifecycles, two configured PL4 parts with identical owner/UUIDs across world save and restore, and saved 128-item barrel. Server-only: no clients, live item transfer, installed optional providers, arbitrary worlds or RC clearance.",
        "client_executed": False,
        "rc_publication_cleared": False,
    }
    (restore / "installed-world-upgrade-receipt.json").write_text(
        json.dumps(receipt, indent=2, sort_keys=True) + "\n"
    )
    return receipt


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    for action in ("seed", "restore"):
        command = commands.add_parser(action)
        command.add_argument("--checkout", required=True, type=Path)
        command.add_argument("--jar", required=True, type=Path)
        command.add_argument("--output", required=True, type=Path)
        command.add_argument("--sha256", required=True)
        command.add_argument("--timeout-seconds", type=int, default=1500)
        if action == "restore":
            command.add_argument("--seed", required=True, type=Path)
            command.add_argument("--baseline-sha256", required=True)
    verify = commands.add_parser("verify")
    verify.add_argument("--seed", required=True, type=Path)
    verify.add_argument("--restore", required=True, type=Path)
    verify.add_argument("--baseline-sha256", required=True)
    verify.add_argument("--candidate-sha256", required=True)
    args = parser.parse_args()
    if args.command in ("seed", "restore") and not 60 <= args.timeout_seconds <= 1800:
        raise ValueError("Native runtime timeout must remain bounded to 60..1800 seconds")
    if args.command == "seed":
        result = seed_world(args.checkout, args.jar, args.output,
                            args.sha256, args.timeout_seconds)
    elif args.command == "restore":
        result = restore_world(args.checkout, args.jar, args.seed, args.output,
                               args.baseline_sha256, args.sha256, args.timeout_seconds)
    else:
        result = verify_upgrade(args.seed, args.restore,
                                args.baseline_sha256, args.candidate_sha256)
    print(json.dumps(result, sort_keys=True))


if __name__ == "__main__":
    main()
