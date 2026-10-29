"""Isolated server-only smoke for a genuine installed Foundations PL4 JAR.

Do not include the production PL4 source in this dev workspace. This gate is
NOT two-client migration, installed-provider acceptance, or RC clearance.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import zipfile

MODID = "foundations_pl4"
PROBE_MODID = "pl4_binary_probe"
JAVA_REL = "src/main/java/net/foundations/pl4probe/InstalledBinaryProbe.java"
TOML_REL = "src/main/resources/META-INF/neoforge.mods.toml"
MARKER = "PL4_INSTALLED_BINARY_READY:"

BUILD = """plugins {
    id 'java'
    id 'net.neoforged.moddev' version '2.0.147'
}
version = '1.0.0-probe'
group = 'net.foundations.pl4probe'
java { toolchain.languageVersion = JavaLanguageVersion.of(21) }
neoForge {
    enable {
        version = '21.1.250'
        disableRecompilation = true
    }
    mods { pl4_binary_probe { sourceSet sourceSets.main } }
    runs {
        server { server(); programArgument '--nogui'; jvmArguments.addAll('-Xms256m', '-Xmx768m') }
        configureEach { logLevel = org.slf4j.event.Level.INFO }
    }
}
"""

JAVA = r"""package net.foundations.pl4probe;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Independent test mod; NEVER contains or compiles PL4 implementation classes. */
@Mod("pl4_binary_probe")
public final class InstalledBinaryProbe {
    public InstalledBinaryProbe(IEventBus bus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(this::serverStarted);
    }

    private void serverStarted(ServerStartedEvent event) {
        if (!ModList.get().isLoaded("foundations_pl4")) {
            throw new IllegalStateException("Installed production PL4 mod was not loaded");
        }
        String sha = System.getenv("PL4_EXPECTED_JAR_SHA256");
        String output = System.getenv("PL4_INSTALLED_PROOF");
        if (sha == null || !sha.matches("[0-9a-f]{64}") || output == null) {
            throw new IllegalStateException("Missing installed-JAR attestation environment");
        }
        try {
            Path proof = Path.of(output);
            Files.createDirectories(proof.getParent());
            String json = "{\"schema\":1,\"loaded_mod_id\":\"foundations_pl4\","
                + "\"server_started\":true,\"expected_jar_sha256\":\"" + sha + "\"}\n";
            Files.writeString(proof, json, StandardCharsets.UTF_8);
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot preserve installed-server proof", failure);
        }
        System.out.println("PL4_INSTALLED_BINARY_READY:" + sha);
    }
}
"""

TOML = """modLoader="javafml"
loaderVersion="[4,)"
license="MIT"
[[mods]]
modId="pl4_binary_probe"
version="1.0.0-probe"
displayName="Isolated PL4 installed-JAR smoke"
[[dependencies.pl4_binary_probe]]
modId="neoforge"
type="required"
versionRange="[21.1.250,21.2)"
ordering="NONE"
side="BOTH"
[[dependencies.pl4_binary_probe]]
modId="minecraft"
type="required"
versionRange="[1.21.1]"
ordering="NONE"
side="BOTH"
[[dependencies.pl4_binary_probe]]
modId="foundations_pl4"
type="required"
versionRange="[0,)"
ordering="AFTER"
side="BOTH"
"""


def digest(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def validate_binary(path, expected):
    path = Path(path)
    if not re.fullmatch(r"[0-9a-f]{64}", expected):
        raise ValueError("Expected a pinned 64-character SHA256")
    if not path.is_file() or path.is_symlink() or digest(path) != expected:
        raise ValueError("Installed JAR is absent, linked or not byte-identical")
    with zipfile.ZipFile(path) as jar:
        if jar.testzip() is not None:
            raise ValueError("Installed JAR ZIP integrity failure")
        names = jar.namelist()
        if len(names) != len(set(names)) or "META-INF/neoforge.mods.toml" not in names:
            raise ValueError("Installed JAR metadata missing/ambiguous")
        if "net/foundations/pl4/FoundationsPL4.class" not in names:
            raise ValueError("Installed JAR lacks actual PL4 production classes")
        meta = jar.read("META-INF/neoforge.mods.toml").decode("utf-8")
        if 'modId="foundations_pl4"' not in meta or 'versionRange="[1.21.1]"' not in meta:
            raise ValueError("Installed JAR has wrong mod or Minecraft target")


def prepare(checkout, jar, output, expected):
    checkout, jar, output = Path(checkout), Path(jar), Path(output)
    validate_binary(jar, expected)
    if output.exists():
        raise FileExistsError("Refusing to overwrite an existing isolated runtime: " + str(output))
    wrapper = checkout / "gradle" / "wrapper"
    for required in [checkout / "gradlew", wrapper / "gradle-wrapper.jar", wrapper / "gradle-wrapper.properties"]:
        if not required.is_file():
            raise FileNotFoundError("Missing pinned Gradle wrapper: " + str(required))
    output.mkdir(parents=True)
    entries = {
        "build.gradle": BUILD,
        "settings.gradle": "pluginManagement { repositories { gradlePluginPortal(); maven { url='https://maven.neoforged.net/releases' } } }\nrootProject.name = 'PL4-Installed-Binary-Probe'\n",
        "gradle.properties": "org.gradle.jvmargs=-Xmx768m\norg.gradle.daemon=false\norg.gradle.workers.max=1\n",
        JAVA_REL: JAVA,
        TOML_REL: TOML,
        "run/eula.txt": "eula=true\n",
        "run/server.properties": "server-ip=127.0.0.1\nserver-port=25578\nonline-mode=false\nmax-players=1\nlevel-type=minecraft:flat\nview-distance=3\nsimulation-distance=3\n",
    }
    for name, value in entries.items():
        target = output / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(value)
    shutil.copy2(checkout / "gradlew", output / "gradlew")
    shutil.copytree(wrapper, output / "gradle" / "wrapper")
    installed = output / "run" / "mods" / jar.name
    installed.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(jar, installed)
    if digest(installed) != expected:
        raise ValueError("Installed copy does not match pinned input bytes")
    # Vanilla rewrites server.properties/eula.txt during startup. They are
    # runtime evidence, never immutable source inputs; check safety keys later.
    protected = ["gradlew", "gradle/wrapper/gradle-wrapper.jar",
                 "gradle/wrapper/gradle-wrapper.properties",
                 *(name for name in entries if not name.startswith("run/"))]
    files = {name: digest(output / name) for name in protected}
    manifest = {"schema": 1, "expected_jar_sha256": expected, "installed_name": jar.name,
                "isolation": "Only standalone probe sources; PL4 production bytecode exclusively in run/mods",
                "files": files, "runtime_server_executed": False, "rc_publication_cleared": False}
    (output / "prepared-installation.json").write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n")
    return manifest


def verify(output, log):
    output, log = Path(output), Path(log)
    manifest = json.loads((output / "prepared-installation.json").read_text())
    if manifest.get("schema") != 1 or manifest.get("rc_publication_cleared") is not False:
        raise ValueError("Invalid installation manifest or unsafe acceptance status")
    expected = manifest["expected_jar_sha256"]
    files = manifest["files"]
    if not files or any(not (output / n).is_file() or digest(output / n) != sha for n, sha in files.items()):
        raise ValueError("Isolated probe project changed after installation preparation")
    server_props = {}
    for line in (output / "run/server.properties").read_text().splitlines():
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            server_props[key.strip()] = value.strip()
    if any(server_props.get(key) != value for key, value in {
            "server-ip": "127.0.0.1", "server-port": "25578",
            "online-mode": "false", "max-players": "1"}.items()):
        raise ValueError("Native runtime escaped isolated loopback/offline server configuration")
    if "eula=true" not in (output / "run/eula.txt").read_text().splitlines():
        raise ValueError("Native runtime lacks explicit disposable-server EULA")
    sources = sorted(p.relative_to(output).as_posix() for p in (output / "src/main/java").rglob("*.java"))
    if sources != [JAVA_REL]:
        raise ValueError("Runtime contains compiled-from-source PL4 classes or unexpected Java sources")
    mods = sorted((output / "run/mods").glob("*.jar"))
    if len(mods) != 1 or mods[0].name != manifest["installed_name"]:
        raise ValueError("Runtime does not have exactly the pinned installed production JAR")
    validate_binary(mods[0], expected)
    if not log.is_file():
        raise ValueError("Missing actual native NeoForge server log")
    occurrences = [line.strip() for line in log.read_text(errors="replace").splitlines()
                   if MARKER in line]
    if len(occurrences) != 1 or not occurrences[0].endswith(MARKER + expected):
        raise ValueError("Real server never emitted the exact installed-mod startup attestation")
    proof = json.loads((output / "installed-server-proof.json").read_text())
    if (proof != {"schema": 1, "loaded_mod_id": MODID,
                  "server_started": True, "expected_jar_sha256": expected}):
        raise ValueError("Real ServerStartedEvent proof missing or mismatched")
    receipt = {"schema": 1, "installed_jar_sha256": expected,
               "installed_jar_name": mods[0].name, "loaded_mod_id": MODID,
               "server_started": True, "isolation_manifest_sha256": digest(output / "prepared-installation.json"),
               "server_log_sha256": digest(log), "probe_proof_sha256": digest(output / "installed-server-proof.json"),
               "installed_binary_server_smoke": "PASS",
               "installed_binary_client_executed": False,
               "released_to_candidate_world_upgrade_accepted": False,
               "multiplayer_accepted": False, "rc_publication_cleared": False}
    (output / "installed-server-receipt.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    return receipt


def run(output, seconds):
    output = Path(output).resolve()
    manifest = json.loads((output / "prepared-installation.json").read_text())
    expected = manifest["expected_jar_sha256"]
    log_path = output / "installed-server.log"
    proof = output / "installed-server-proof.json"
    env = dict(os.environ, PL4_EXPECTED_JAR_SHA256=expected, PL4_INSTALLED_PROOF=str(proof))
    with log_path.open("w") as log:
        process = subprocess.Popen(["bash", "gradlew", "--no-daemon", "--console=plain", "runServer"],
                                   cwd=output, env=env, stdin=subprocess.PIPE, stdout=log,
                                   stderr=subprocess.STDOUT, text=True)
        ready = False
        try:
            deadline = time.monotonic() + seconds
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    break
                if log_path.exists() and MARKER + expected in log_path.read_text(errors="replace"):
                    ready = True
                    break
                time.sleep(3)
            if ready:
                process.stdin.write("stop\n")
                process.stdin.flush()
                process.wait(timeout=90)
            else:
                raise TimeoutError("Installed production-JAR NeoForge server did not attest startup; inspect retained log")
            if process.returncode != 0:
                raise RuntimeError("Native installed-JAR runner exited unsuccessfully")
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
    return verify(output, log_path)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    sub = p.add_subparsers(dest="action", required=True)
    a = sub.add_parser("prepare")
    a.add_argument("--checkout", required=True)
    a.add_argument("--jar", required=True)
    a.add_argument("--output", required=True)
    a.add_argument("--sha256", required=True)
    b = sub.add_parser("run")
    b.add_argument("--output", required=True)
    b.add_argument("--timeout-seconds", type=int, default=1500)
    c = sub.add_parser("verify")
    c.add_argument("--output", required=True)
    c.add_argument("--log", required=True)
    args = p.parse_args()
    if args.action == "prepare":
        print(json.dumps(prepare(args.checkout, args.jar, args.output, args.sha256), sort_keys=True))
    elif args.action == "run":
        if not 60 <= args.timeout_seconds <= 1800:
            raise ValueError("Native execution timeout outside bounded interval")
        print(json.dumps(run(args.output, args.timeout_seconds), sort_keys=True))
    else:
        print(json.dumps(verify(args.output, args.log), sort_keys=True))


if __name__ == "__main__":
    main()
