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
    # Python treats True == 1 and False == 0; JSON numeric evidence must not
    # be satisfied by booleans or integral-looking floating point values.
    require(type(proof.get("schema")) is int and
            type(proof.get("api_version")) is int and
            type(proof.get("registration_leaks")) is int,
            "Native proof schema/API/leak counters must be exact integers")
    for section, keys in (
        ("replacement", ("first", "later", "stale_rows")),
        ("registration_close", ("first", "later", "first_rows")),
        ("self_close", ("calls", "rows", "healthy_calls")),
        ("exception_quarantine", ("failing", "healthy", "partial_rows")),
        ("unloaded", ("callbacks", "rows")),
    ):
        values = proof.get(section)
        require(type(values) is dict and
                all(type(values.get(key)) is int for key in keys),
                "Native proof counters must be exact integers: " + section)
    unloaded = proof.get("unloaded")
    require(type(unloaded) is dict and
            all(type(unloaded.get(key)) is bool
                for key in ("before_loaded", "after_loaded")),
            "Native chunk residency must be boolean, not a numeric surrogate")
    require(NONCE.fullmatch(nonce) is not None and
            proof.get("run_nonce") == nonce and
            proof.get("schema") == 2 and proof.get("target") == "1.21.1" and
            proof.get("api_version") == 1 and
            proof.get("installed_pl4_sha256") == expected and
            proof.get("rc_publication_cleared") is False,
            "Proof has wrong schema/target/API/installed JAR or unsafe RC claim")
    require(proof.get("replacement") == {"first": 1, "later": 0, "stale_rows": 0},
            "Installed provider replacement proof failed")
    require(proof.get("registration_close") == {"first": 1, "later": 0, "first_rows": 1},
            "Installed provider registration-revocation proof failed")
    require(proof.get("self_close") ==
            {"calls": 1, "rows": 0, "healthy_calls": 1},
            "Installed provider self-revocation proof failed")
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
                 "gradle/wrapper/gradle-wrapper.properties", *PROJECT,
                 "run/server.properties", "run/eula.txt"]
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


def parse_strict_properties(path, label):
    """Accept only unambiguous Minecraft-generated Java properties.

    java.util.Properties also accepts colon/whitespace separators, escaped
    keys and line continuations. A dict made from lines containing "=" would
    silently ignore those aliases and could approve unsafe runtime settings.
    Minecraft's generated files use plain key=value and comment lines.
    """
    result = {}
    for line_number, line in enumerate(Path(path).read_text(encoding="utf-8").splitlines(), 1):
        if not line or line.startswith(("#", "!")):
            continue
        match = re.fullmatch(r"([A-Za-z0-9_.-]+)=(.*)", line)
        require(match is not None,
                f"Unsafe {label} property syntax at line {line_number}")
        key, value = match.groups()
        require(key not in result, "Duplicate " + label + " property key: " + key)
        result[key] = value
    return result


def validate_prepared_installation(output, manifest, allow_runtime_config_changes=False):
    """Fail closed on local input mutation BEFORE invoking Gradle or a native server.

    Before execution every input must match its prepared SHA-256. Minecraft may
    rewrite server.properties/eula.txt during normal startup; after execution
    those two runtime files may differ in bytes, but must remain regular files,
    retain safe settings, and be pinned by the clean-exit attestation.
    """
    output = Path(output)
    expected = manifest.get("installed_pl4_sha256", "")
    nonce = manifest.get("run_nonce", "")
    require(NONCE.fullmatch(nonce) is not None, "Missing/invalid per-preparation nonce")
    require(manifest.get("schema") == 1 and manifest.get("target") == "1.21.1" and
            manifest.get("rc_publication_cleared") is False and
            SHA.fullmatch(expected) is not None and
            re.fullmatch(r"[0-9a-f]{40}", manifest.get("pl4_source_commit", "")) is not None,
            "Unsafe/invalid prepared installation or source attribution")
    protected = ["gradlew", "gradle/wrapper/gradle-wrapper.jar",
                 "gradle/wrapper/gradle-wrapper.properties", *PROJECT,
                 "run/server.properties", "run/eula.txt"]
    hashes = manifest.get("protected_files")
    require(isinstance(hashes, dict) and set(hashes) == set(protected),
            "Incomplete/extra protected probe input manifest")
    runtime_config = {"run/server.properties", "run/eula.txt"}
    for name in protected:
        path = output / name
        require(path.is_file() and not path.is_symlink() and
                SHA.fullmatch(hashes[name]) is not None and
                (allow_runtime_config_changes and name in runtime_config or
                 digest(path) == hashes[name]),
                "Probe source, wrapper or prepared configuration changed: " + name)
    require(sorted(p.relative_to(output).as_posix()
                   for p in (output / "src/main/java").rglob("*.java")) == [JAVA],
            "Unexpected PL4/other source in independent probe")
    # A *.jar glob silently ignores nested mods, disabled archives and other
    # unapproved files. The independent acceptance runtime must have exactly
    # one regular installed mod, not merely one matching JAR.
    run_dir, mods_dir = output / "run", output / "run/mods"
    require(output.is_dir() and not output.is_symlink() and
            run_dir.is_dir() and not run_dir.is_symlink() and
            mods_dir.is_dir() and not mods_dir.is_symlink(),
            "Probe runtime or mods directory is missing/untrusted")
    mods = sorted(mods_dir.iterdir())
    require(len(mods) == 1 and mods[0].is_file() and not mods[0].is_symlink() and
            mods[0].suffix == ".jar" and
            mods[0].name == manifest.get("installed_pl4_name"),
            "Expected exactly one regular installed PL4 production JAR and no extra mod entries")
    binary_smoke.validate_binary(mods[0], expected)
    props_path = output / "run/server.properties"
    require(props_path.is_file() and not props_path.is_symlink(),
            "Missing/untrusted loopback server configuration")
    props = parse_strict_properties(props_path, "server")
    for key, value in {"server-ip": "127.0.0.1", "server-port": "25588",
                       "online-mode": "false", "max-players": "1",
                       "enable-rcon": "true", "rcon.port": "25589"}.items():
        require(props.get(key) == value, "Server network isolation changed: " + key)
    eula = output / "run/eula.txt"
    require(eula.is_file() and not eula.is_symlink(),
            "Disposable server EULA missing/untrusted")
    require(parse_strict_properties(eula, "EULA") == {"eula": "true"},
            "Disposable server EULA is missing, ambiguous or unaccepted")
    return expected, nonce


def validate_exit_attestation(exit_data, manifest, log, proof_path):
    """Require a runner-observed clean native shutdown before accepting offline proof.

    A native PASS marker alone is insufficient: the server may subsequently
    crash, be killed, or fail to persist the test world. This attestation is
    emitted only after the local runner observes process exit code zero.
    """
    fields = {"schema", "run_nonce", "pl4_source_commit", "installed_pl4_sha256",
              "exit_code", "native_server_log_sha256", "native_provider_proof_sha256",
              "runtime_server_properties_sha256", "runtime_eula_sha256"}
    require(type(exit_data) is dict and set(exit_data) == fields,
            "Missing/extra native process-exit attestation fields")
    require(type(exit_data["schema"]) is int and exit_data["schema"] == 1 and
            type(exit_data["exit_code"]) is int and exit_data["exit_code"] == 0 and
            exit_data["run_nonce"] == manifest["run_nonce"] and
            exit_data["pl4_source_commit"] == manifest["pl4_source_commit"] and
            exit_data["installed_pl4_sha256"] == manifest["installed_pl4_sha256"] and
            exit_data["native_server_log_sha256"] == digest(log) and
            exit_data["native_provider_proof_sha256"] == digest(proof_path) and
            exit_data["runtime_server_properties_sha256"] == digest(log.parent / "run/server.properties") and
            exit_data["runtime_eula_sha256"] == digest(log.parent / "run/eula.txt"),
            "Native process did not attest clean exit and exact retained evidence")


def verify(output, log):
    output, log = Path(output), Path(log)
    require(log.resolve() == (output / "provider-server.log").resolve() and
            not log.is_symlink(),
            "Native log must be the prepared runtime server log, not an external file")
    manifest = json.loads((output / "provider-probe-prepared.json").read_text())
    expected, nonce = validate_prepared_installation(
        output, manifest, allow_runtime_config_changes=True)
    proof_path = output / "provider-proof.json"
    require(proof_path.is_file() and not proof_path.is_symlink() and
            log.is_file() and not log.is_symlink(),
            "Missing/untrusted native proof or server log")
    proof = json.loads(proof_path.read_text())
    validate_proof(proof, expected, nonce)
    exit_path = output / "provider-exit.json"
    require(exit_path.is_file() and not exit_path.is_symlink(),
            "Missing/untrusted native clean-exit attestation")
    validate_exit_attestation(json.loads(exit_path.read_text()), manifest, log, proof_path)
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
        "native_exit_attestation_sha256": digest(exit_path),
        "runtime_server_properties_sha256": digest(output / "run/server.properties"),
        "runtime_eula_sha256": digest(output / "run/eula.txt"),
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
    try:
        # Exclusive create closes the exists-check/write race between writers.
        with path.open("x", encoding="utf-8") as stream:
            stream.write(content)
    except FileExistsError:
        require(path.is_file() and not path.is_symlink() and
                path.read_text() == content,
                "Refusing to overwrite a different or untrusted provider receipt")


def require_fresh_run(output):
    """Never overwrite a prior native attempt or its receipt; use a new prepared directory."""
    for name in ("provider-server.log", "provider-proof.json", "provider-exit.json", "provider-probe-receipt.json"):
        require(not (Path(output) / name).exists(),
                "Refusing to overwrite existing native evidence: " + name)


def open_immutable_log(path):
    """Exclusive creation prevents a second native attempt from truncating evidence."""
    return Path(path).open("x", encoding="utf-8")


def run(output, timeout):
    output = Path(output).resolve()
    manifest = json.loads((output / "provider-probe-prepared.json").read_text())
    expected, nonce = validate_prepared_installation(output, manifest)
    installed = output / "run/mods" / manifest["installed_pl4_name"]
    log = output / "provider-server.log"
    proof = output / "provider-proof.json"
    require_fresh_run(output)
    env = dict(os.environ, PL4_PROVIDER_RUN_NONCE=nonce,
               PL4_EXPECTED_JAR_SHA256=expected,
               PL4_INSTALLED_JAR=str(installed), PL4_PROVIDER_PROOF=str(proof))
    with open_immutable_log(log) as stream:
        process = subprocess.Popen(["bash", "gradlew", "--no-daemon", "--console=plain",
                                    "verifyProbeIsolation", "runServer"],
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
    # Vanilla may normalize server.properties/eula.txt on startup. Check their
    # safety-critical values after shutdown, then bind their final bytes to the
    # clean-exit evidence instead of requiring the prepared bytes to survive.
    validate_prepared_installation(output, manifest, allow_runtime_config_changes=True)
    # Written only after the native server has returned zero; a marker/proof
    # from a process that later crashed must never become an accepted receipt.
    write_immutable_receipt(output / "provider-exit.json", {
        "schema": 1, "run_nonce": nonce,
        "pl4_source_commit": manifest["pl4_source_commit"],
        "installed_pl4_sha256": expected, "exit_code": process.returncode,
        "native_server_log_sha256": digest(log),
        "native_provider_proof_sha256": digest(proof),
        "runtime_server_properties_sha256": digest(output / "run/server.properties"),
        "runtime_eula_sha256": digest(output / "run/eula.txt"),
    })
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
