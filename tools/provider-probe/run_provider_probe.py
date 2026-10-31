#!/usr/bin/env python3
"""Build and run an independent installed InfoProvider acceptance probe.

No GitHub Actions, release publishing, PL4 source compilation, or API secrets.
The only PL4 bytecode admitted is the caller-pinned production JAR in run/mods.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import signal
import subprocess
import sys
import time
import zipfile

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "multiplayer-probe"))
import installed_binary_smoke as binary_smoke

JAVA = "src/main/java/net/foundations/pl4providerprobe/InstalledProviderProbe.java"
TOML = "src/main/resources/META-INF/neoforge.mods.toml"
PROJECT = ["build.gradle", "settings.gradle", "gradle.properties", JAVA, TOML]
MARKER = "PL4_INSTALLED_PROVIDER_PASS:"
SHA = re.compile(r"[0-9a-f]{64}\Z")
NONCE = re.compile(r"[0-9a-f]{32}\Z")


def digest(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def require(condition, message):
    if not condition:
        raise ValueError(message)


def verify_probe_jar(path):
    with zipfile.ZipFile(path) as archive:
        require(archive.testzip() is None, "Probe JAR failed CRC validation")
        names = archive.namelist()
        require(len(names) == len(set(names)), "Probe JAR has duplicate entries")
        require("net/foundations/pl4providerprobe/InstalledProviderProbe.class" in names,
                "Independent provider entrypoint missing")
        require(not any(name.startswith("net/foundations/pl4/") or
                        "GameTests" in name or name.endswith(".jar") for name in names),
                "Probe JAR includes PL4/native/embedded binary content")
        meta = archive.read("META-INF/neoforge.mods.toml").decode("utf-8")
        require('modId="pl4_provider_probe"' in meta and
                'modId="foundations_pl4"' in meta and
                'versionRange="[1.21.1]"' in meta,
                "Probe metadata does not pin installed PL4 / Minecraft 1.21.1")
    return digest(path)


def validate_proof(proof, expected, nonce):
    require(NONCE.fullmatch(nonce) is not None and
            proof.get("run_nonce") == nonce and
            proof.get("schema") == 1 and proof.get("target") == "1.21.1" and
            proof.get("api_version") == 1 and
            proof.get("installed_pl4_sha256") == expected and
            proof.get("rc_publication_cleared") is False,
            "Proof has wrong schema/target/API/installed JAR or unsafe RC claim")
    require(proof.get("replacement") == {"first": 1, "later": 0, "stale_rows": 0},
            "Installed provider replacement proof failed")
    require(proof.get("registration_close") == {"first": 1, "later": 0, "first_rows": 1},
            "Installed provider registration-revocation proof failed")
    require(proof.get("exception_quarantine") ==
            {"failing": 1, "healthy": 1, "partial_rows": 0},
            "Installed provider partial-row quarantine proof failed")
    require(proof.get("unloaded") ==
            {"callbacks": 0, "rows": 0, "before_loaded": False, "after_loaded": False},
            "Installed provider unloaded-chunk/no-force-load proof failed")
    require(proof.get("registration_leaks") == 0,
            "Installed provider registry leaked a registration")


def prepare(checkout, jar, output, expected, source_sha):
    checkout, jar, output = Path(checkout), Path(jar), Path(output)
    require(SHA.fullmatch(expected) is not None, "Expected exact PL4 SHA-256")
    require(re.fullmatch(r"[0-9a-f]{40}", source_sha) is not None, "Expected exact source commit")
    binary_smoke.validate_binary(jar, expected)
    if output.exists():
        raise FileExistsError("Refusing to overwrite any existing probe runtime: " + str(output))
    wrapper = checkout / "gradle" / "wrapper"
    required = [checkout / "gradlew", wrapper / "gradle-wrapper.jar",
                wrapper / "gradle-wrapper.properties", *(HERE / p for p in PROJECT)]
    for path in required:
        require(path.is_file() and not path.is_symlink(), "Missing/untrusted required input: " + str(path))
    output.mkdir(parents=True)
    for relative in PROJECT:
        destination = output / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(HERE / relative, destination)
    shutil.copy2(checkout / "gradlew", output / "gradlew")
    shutil.copytree(wrapper, output / "gradle" / "wrapper")
    mods = output / "run" / "mods"
    mods.mkdir(parents=True)
    installed = mods / jar.name
    shutil.copyfile(jar, installed)
    require(digest(installed) == expected, "Installed copy changed during preparation")
    (output / "run" / "eula.txt").write_text("eula=true\n")
    (output / "run" / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25588\nonline-mode=false\n"
        "max-players=1\nlevel-type=minecraft:flat\nview-distance=3\nsimulation-distance=3\n"
        "enable-rcon=true\nrcon.port=25589\nrcon.password=" + secrets.token_urlsafe(32) + "\n"
    )
    protected = ["gradlew", "gradle/wrapper/gradle-wrapper.jar",
                 "gradle/wrapper/gradle-wrapper.properties", *PROJECT]
    manifest = {
        "schema": 1, "target": "1.21.1", "pl4_source_commit": source_sha,
        "pl4_source_attribution": "caller supplied; corroborate against retained build artifact",
        "installed_pl4_sha256": expected, "installed_pl4_name": jar.name,
        "run_nonce": secrets.token_hex(16),
        "protected_files": {p: digest(output / p) for p in protected},
        "rc_publication_cleared": False, "runtime_executed": False,
    }
    (output / "provider-probe-prepared.json").write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n")
    return manifest


def verify(output, log):
    output, log = Path(output), Path(log)
    manifest = json.loads((output / "provider-probe-prepared.json").read_text())
    expected = manifest["installed_pl4_sha256"]
    nonce = manifest.get("run_nonce", "")
    require(NONCE.fullmatch(nonce) is not None, "Missing/invalid per-preparation nonce")
    require(manifest.get("schema") == 1 and manifest.get("target") == "1.21.1" and
            manifest.get("rc_publication_cleared") is False and
            SHA.fullmatch(expected) is not None,
            "Unsafe/invalid prepared installation")
    for name, sha in manifest["protected_files"].items():
        require((output / name).is_file() and digest(output / name) == sha,
                "Probe source or wrapper changed: " + name)
    require(sorted(p.relative_to(output).as_posix()
                   for p in (output / "src/main/java").rglob("*.java")) == [JAVA],
            "Unexpected PL4/other source in independent probe")
    mods = sorted((output / "run/mods").glob("*.jar"))
    require(len(mods) == 1 and mods[0].name == manifest["installed_pl4_name"],
            "Expected exactly one installed PL4 production JAR")
    binary_smoke.validate_binary(mods[0], expected)
    props = dict(line.split("=", 1) for line in
                 (output / "run/server.properties").read_text().splitlines()
                 if "=" in line and not line.startswith("#"))
    for key, value in {"server-ip": "127.0.0.1", "server-port": "25588",
                       "online-mode": "false", "max-players": "1",
                       "enable-rcon": "true", "rcon.port": "25589"}.items():
        require(props.get(key) == value, "Server network isolation changed: " + key)
    require("eula=true" in (output / "run/eula.txt").read_text().splitlines(),
            "Disposable server EULA missing")
    proof_path = output / "provider-proof.json"
    require(proof_path.is_file() and log.is_file(), "Missing native proof or server log")
    proof = json.loads(proof_path.read_text())
    validate_proof(proof, expected, nonce)
    matching = [line for line in log.read_text(errors="replace").splitlines()
                if MARKER in line]
    require(len(matching) == 1 and matching[0].endswith(MARKER + expected + ":" + nonce),
            "Missing/duplicate/mismatched native installed provider pass marker")
    jars = sorted((output / "build/libs").glob("PL4-Installed-Provider-Probe-*.jar"))
    jars = [p for p in jars if not p.name.endswith("-sources.jar")]
    require(len(jars) == 1, "Missing/ambiguous compiled independent provider JAR")
    probe_hash = verify_probe_jar(jars[0])
    receipt = {
        "schema": 1, "status": "INSTALLED_PROVIDER_ONLY_PASS",
        "target": "1.21.1", "pl4_source_commit": manifest["pl4_source_commit"],
        "pl4_source_attribution": manifest["pl4_source_attribution"],
        "pl4_production_jar_sha256": expected,
        "independent_probe_jar_sha256": probe_hash,
        "native_server_log_sha256": digest(log),
        "native_provider_proof_sha256": digest(proof_path),
        "prepared_manifest_sha256": digest(output / "provider-probe-prepared.json"),
        "scenarios": proof, "rc_publication_cleared": False,
        "not_tested": ["authenticated multiplayer", "arbitrary-world migration",
                       "sustained TPS/heap", "other Minecraft versions", "client screenshots"],
    }
    write_immutable_receipt(output / "provider-probe-receipt.json", receipt)
    return receipt


def write_immutable_receipt(path, receipt):
    """A repeat verification may confirm, but never replace, a retained receipt."""
    path = Path(path)
    content = json.dumps(receipt, indent=2, sort_keys=True) + "\n"
    if path.exists():
        require(path.is_file() and path.read_text() == content,
                "Refusing to overwrite a different retained provider receipt")
    else:
        path.write_text(content)


def require_fresh_run(output):
    """Never overwrite a prior native attempt or its receipt; use a new prepared directory."""
    for name in ("provider-server.log", "provider-proof.json", "provider-probe-receipt.json"):
        require(not (Path(output) / name).exists(),
                "Refusing to overwrite existing native evidence: " + name)


def run(output, timeout):
    output = Path(output).resolve()
    manifest = json.loads((output / "provider-probe-prepared.json").read_text())
    expected = manifest["installed_pl4_sha256"]
    nonce = manifest.get("run_nonce", "")
    require(NONCE.fullmatch(nonce) is not None, "Missing/invalid per-preparation nonce")
    installed = output / "run/mods" / manifest["installed_pl4_name"]
    require(digest(installed) == expected, "PL4 JAR changed before launch")
    log = output / "provider-server.log"
    proof = output / "provider-proof.json"
    require_fresh_run(output)
    env = dict(os.environ, PL4_PROVIDER_RUN_NONCE=nonce,
               PL4_EXPECTED_JAR_SHA256=expected,
               PL4_INSTALLED_JAR=str(installed), PL4_PROVIDER_PROOF=str(proof))
    with log.open("w") as stream:
        process = subprocess.Popen(["bash", "gradlew", "--no-daemon", "--console=plain",
                                    "runServer", "verifyProbeIsolation"],
                                   cwd=output, env=env, stdin=subprocess.DEVNULL,
                                   stdout=stream, stderr=subprocess.STDOUT,
                                   start_new_session=True)
        try:
            deadline = time.monotonic() + timeout
            ready = False
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    break
                if log.exists() and MARKER + expected + ":" + nonce in log.read_text(errors="replace"):
                    ready = True
                    break
                time.sleep(3)
            require(ready, "Independent installed provider server did not attest PASS")
            binary_smoke._stop_with_retry(output / "run/server.properties")
            try:
                process.wait(timeout=90)
            except subprocess.TimeoutExpired as exc:
                raise RuntimeError("Installed server did not stop cleanly") from exc
            require(process.returncode == 0, "Native installed server exited unsuccessfully")
        finally:
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()
    return verify(output, log)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="action", required=True)
    p = sub.add_parser("prepare")
    for key in ("checkout", "jar", "output", "sha256", "source-sha"):
        p.add_argument("--" + key, required=True)
    p = sub.add_parser("run")
    p.add_argument("--output", required=True)
    p.add_argument("--timeout-seconds", type=int, default=1500)
    p = sub.add_parser("verify")
    p.add_argument("--output", required=True)
    p.add_argument("--log", required=True)
    args = parser.parse_args()
    if args.action == "prepare":
        result = prepare(args.checkout, args.jar, args.output, args.sha256, args.source_sha)
    elif args.action == "run":
        require(60 <= args.timeout_seconds <= 1800, "Timeout outside bounded range")
        result = run(args.output, args.timeout_seconds)
    else:
        result = verify(args.output, args.log)
    print(json.dumps(result, sort_keys=True))


if __name__ == "__main__":
    main()
