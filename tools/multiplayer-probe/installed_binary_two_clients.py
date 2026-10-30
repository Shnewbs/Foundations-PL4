"""Run TWO real Minecraft clients against each exact installed PL4 production JAR.

This is a separate, nonpublishing acceptance harness.  Only the probe's three
Java test classes are compiled; production PL4 code comes from the pinned JAR
installed under run/mods and supplied read-only to javac as compileOnly.
It does not establish online-auth, arbitrary-world, provider or RC clearance.
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
import time

import installed_binary_smoke as smoke
from verify_upgrade_evidence import (
    NAMES, FILES, digest, snapshot_world, world_fingerprint, zip_world_fingerprint,
)

SERVER_REL = "src/main/java/net/foundations/pl4probe/MultiplayerServerProbe.java"
CLIENT_REL = "src/main/java/net/foundations/pl4probe/client/MultiplayerClientProbe.java"
ENTRY_REL = "src/main/java/net/foundations/pl4probe/InstalledTwoClientProbe.java"
SOURCE_FILES = (SERVER_REL, CLIENT_REL, ENTRY_REL)
TAG = "PL4_INSTALLED_TWO_CLIENT_SERVER_READY:"
CLIENT_TAG = "PL4_INSTALLED_TWO_CLIENT_READY:"
CABLE_TAG = "PL4_INSTALLED_CABLE_CLIENT_TICK:"
BASELINE_SOURCE = "7c7c07ff5ae0bce4990bf2f0a739841161ce8f8c"
CANDIDATE_SOURCE = "411401924e35ea1c7e085db4afdcd7cc241eeb33"
BASELINE_JAR = "7fa14351e1bbf039846afba040e4b66ce16b98db64103f1b33fc8ce14df0c229"
CANDIDATE_JAR = "8e30b7da03de049e1f0342c515db5f3dabaf9d4d7629efb088b3b47dc927c2bc"
ENTRY = """package net.foundations.pl4probe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
/** Test-only probe mod: PL4 production classes are external installed JAR bytes. */
@Mod("pl4_binary_probe")
public final class InstalledTwoClientProbe {
    public InstalledTwoClientProbe(IEventBus bus, ModContainer container) {}
}
"""


def exact_sha(value):
    if not isinstance(value, str) or not re.fullmatch(r"[0-9a-f]{64}", value):
        raise ValueError("A complete exact SHA-256 is required")
    return value


def exact_source(value):
    if not isinstance(value, str) or not re.fullmatch(r"[0-9a-f]{40}", value):
        raise ValueError("A complete immutable Git source SHA is required")
    return value


def build_file(name, jar):
    # The installed production JAR is the only PL4 compile input; it remains a
    # run/mods artifact at execution.  No original production source is copied.
    if not re.fullmatch(r"FoundationsPL4-1\.21\.1-[\w.-]+\.jar", jar):
        raise ValueError("Unexpected exact-target runtime JAR name")
    run = ("server { server(); programArgument '--nogui'; "
           "jvmArguments.addAll('-Xms256m','-Xmx768m') }" if name == "server"
           else "client { client(); programArguments.addAll('--username','" + name
           + "','--quickPlayMultiplayer','127.0.0.1:25576','--width','960','--height','720'); "
             "jvmArguments.addAll('-Xms256m','-Xmx896m') }")
    return """plugins {
    id 'java'
    id 'net.neoforged.moddev' version '2.0.147'
}
version = '1.0.0-isolated-clients'
group = 'net.foundations.pl4probe'
java { toolchain.languageVersion = JavaLanguageVersion.of(21) }
neoForge {
    enable { version = '21.1.250'; disableRecompilation = true }
    mods { pl4_binary_probe { sourceSet sourceSets.main } }
    runs {
        """ + run + """
        configureEach { logLevel = org.slf4j.event.Level.INFO }
    }
}
// Only the exact already-verified production JAR supplies PL4 classes to javac.
// It must be installed separately under run/mods and must not be repackaged.
dependencies { compileOnly files('run/mods/""" + jar + """') }
"""


def _prepare(checkout, jar, output, phase, source, jar_sha, copy_from=None):
    checkout, jar, output = Path(checkout), Path(jar), Path(output)
    if phase not in ("seed", "restore"):
        raise ValueError("Unrecognized installed two-client migration phase")
    exact_source(source)
    exact_sha(jar_sha)
    smoke.validate_binary(jar, jar_sha)
    if output.exists():
        raise FileExistsError("Refusing to replace existing installed-client evidence")
    fixture = checkout / "tools/multiplayer-probe"
    wrapper = checkout / "gradle/wrapper"
    if not (checkout / "gradlew").is_file() or not (wrapper / "gradle-wrapper.jar").is_file():
        raise ValueError("Missing authenticated test checkout Gradle wrapper")
    output.mkdir(parents=True)
    copied = None
    if phase == "restore":
        if copy_from is None:
            raise ValueError("Restore requires saved installed-beta world evidence")
        seed = Path(copy_from)
        prior = verify_phase(seed, "seed", BASELINE_SOURCE, BASELINE_JAR)
        source_dir = seed / "server/run/world"
        if world_fingerprint(source_dir) != prior["world"]:
            raise ValueError("Live released-beta save differs from its validated ZIP")
        copied = prior["world"]
    elif copy_from is not None:
        raise ValueError("Seed must not copy an existing world")
    for name in ("server", *NAMES):
        root = output / name
        root.mkdir()
        shutil.copy2(checkout / "gradlew", root / "gradlew")
        shutil.copytree(wrapper, root / "gradle/wrapper")
        (root / "settings.gradle").write_text(
            "pluginManagement { repositories { gradlePluginPortal(); "
            "maven { url='https://maven.neoforged.net/releases' } } }\n"
            "rootProject.name = 'PL4-Isolated-Two-Client-Probe'\n"
        )
        (root / "gradle.properties").write_text(
            "org.gradle.jvmargs=-Xmx768m\norg.gradle.daemon=false\n"
            "org.gradle.parallel=false\norg.gradle.workers.max=1\n"
        )
        (root / "build.gradle").write_text(build_file(name, jar.name))
        src = root / "src/main/java/net/foundations/pl4probe"
        (src / "client").mkdir(parents=True)
        server_java = (fixture / "MultiplayerServerProbe.java").read_text().replace(
            "package net.foundations.pl4;",
            "package net.foundations.pl4probe;\nimport net.foundations.pl4.*;"
        ).replace(
            "@EventBusSubscriber(modid=FoundationsPL4.ID",
            '@EventBusSubscriber(modid="pl4_binary_probe"'
        )
        client_java = (fixture / "MultiplayerClientProbe.java").read_text().replace(
            "package net.foundations.pl4.client;",
            "package net.foundations.pl4probe.client;\nimport net.foundations.pl4.*;"
        ).replace(
            "@EventBusSubscriber(modid=FoundationsPL4.ID",
            '@EventBusSubscriber(modid="pl4_binary_probe"'
        )
        if 'modid="pl4_binary_probe"' not in server_java or 'modid="pl4_binary_probe"' not in client_java:
            raise ValueError("Could not bind both actual-client probes to isolated probe mod")
        # A separate probe module MUST NOT declare the installed PL4 Java package.
        # Construct public Part through the loaded installed JAR's Kind enum by
        # reflection; Kind is intentionally package-private production API.
        server_java = server_java.replace(
            "new Part(Kind.NODE,Direction.EAST,uuid(\"PL4ProbeA\"))",
            'installedPart("NODE",Direction.EAST,uuid("PL4ProbeA"))'
        ).replace(
            "new Part(Kind.DATA_CABLE,Direction.DOWN,node.owner)",
            'installedPart("DATA_CABLE",Direction.DOWN,node.owner)'
        ).replace(
            'new Part(Kind.DATA_CABLE,Direction.DOWN,uuid("PL4ProbeA"))',
            'installedPart("DATA_CABLE",Direction.DOWN,uuid("PL4ProbeA"))'
        ).replace(
            'new Part(Kind.TRANSFER_NODE,Direction.EAST,uuid("PL4ProbeA"))',
            'installedPart("TRANSFER_NODE",Direction.EAST,uuid("PL4ProbeA"))'
        ).replace(
            "public final class MultiplayerServerProbe {",
            """public final class MultiplayerServerProbe {
    private static Part installedPart(String id, Direction face, UUID owner) {
        try {
            Class<?> kind = Class.forName("net.foundations.pl4.Kind");
            Object selected = java.util.Arrays.stream(kind.getEnumConstants())
                .filter(value -> ((Enum<?>) value).name().equals(id))
                .findFirst().orElseThrow();
            return (Part) Part.class.getConstructor(kind, Direction.class, UUID.class)
                .newInstance(selected, face, owner);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Installed PL4 native part construction unavailable", ex);
        }
    }
"""
        )
        server_java = server_java.replace(
            "var level=event.getServer().overworld();",
            'if (!net.neoforged.fml.ModList.get().isLoaded("foundations_pl4")) '
            'throw new IllegalStateException("Actual PL4 production JAR not loaded");'
            'System.out.println("PL4_INSTALLED_TWO_CLIENT_SERVER_READY:" '
            '+ System.getenv("PL4_INSTALLED_JAR_SHA256"));'
            "var level=event.getServer().overworld();"
        )
        client_java = client_java.replace(
            'Files.writeString(out.resolve(name+"-client-proof.json"),',
            'if (!net.neoforged.fml.ModList.get().isLoaded("foundations_pl4")) '
            'throw new IllegalStateException("Installed PL4 missing from real client");'
            'System.out.println("PL4_INSTALLED_TWO_CLIENT_READY:" + '
            'System.getenv("PL4_INSTALLED_JAR_SHA256") + ":" + name);'
            'Files.writeString(out.resolve(name+"-client-proof.json"),'
        )
        (src / "MultiplayerServerProbe.java").write_text(server_java)
        (src / "client/MultiplayerClientProbe.java").write_text(client_java)
        entry = root / ENTRY_REL
        entry.parent.mkdir(parents=True, exist_ok=True)
        entry.write_text(ENTRY)
        toml = root / smoke.TOML_REL
        toml.parent.mkdir(parents=True, exist_ok=True)
        toml.write_text(smoke.TOML)
        runtime = root / "run"
        runtime.mkdir()
        (runtime / "options.txt").write_text(
            "onboardAccessibility:false\npauseOnLostFocus:false\nmaxFps:30\nguiScale:2\n"
        )
        installed = runtime / "mods" / jar.name
        installed.parent.mkdir(parents=True)
        shutil.copyfile(jar, installed)
        if digest(installed) != jar_sha:
            raise ValueError("Modified installed production JAR copy")
        if name == "server":
            (runtime / "eula.txt").write_text("eula=true\n")
            (runtime / "server.properties").write_text(
                "server-ip=127.0.0.1\nserver-port=25576\nonline-mode=false\n"
                "max-players=2\nspawn-protection=0\nlevel-type=minecraft:flat\n"
                "view-distance=3\nsimulation-distance=3\nenforce-secure-profile=false\n"
                "enable-rcon=true\nrcon.port=25579\nrcon.password="
                + secrets.token_urlsafe(32) + "\n"
            )
            if copied is not None:
                shutil.copytree(Path(copy_from) / "server/run/world", runtime / "world", symlinks=False)
                if world_fingerprint(runtime / "world") != copied:
                    raise ValueError("Restored installed-beta world copy changed before candidate boot")
                for receipt in ("probe-node-identity.txt", "probe-cable-identities.txt"):
                    shutil.copy2(Path(copy_from) / "server/run" / receipt,
                                 runtime / receipt)
        sources = sorted(p.relative_to(root).as_posix()
                         for p in (root / "src/main/java").rglob("*.java"))
        if sources != sorted(SOURCE_FILES):
            raise ValueError("Source isolation failed: unexpected Java implementation classes")
        protected = ("build.gradle", "settings.gradle", "gradle.properties",
                     "gradlew", "gradle/wrapper/gradle-wrapper.jar",
                     "gradle/wrapper/gradle-wrapper.properties", smoke.TOML_REL, *SOURCE_FILES)
        manifest = {
            "schema": 1, "phase": phase, "source_commit": source,
            "installed_jar_sha256": jar_sha, "installed_jar_name": jar.name,
            "classes": sources, "file_hashes": {p: digest(root / p) for p in protected},
            "rc_publication_cleared": False,
            "isolation": "PL4 compiled only from the single installed exact production JAR; probe Java only",
        }
        (root / "installed-client-project.json").write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n")
    return copied


def check_project(root, source, jar_sha):
    manifest = json.loads((root / "installed-client-project.json").read_text())
    if (manifest.get("source_commit") != source
            or manifest.get("installed_jar_sha256") != jar_sha
            or manifest.get("rc_publication_cleared") is not False
            or manifest.get("classes") != sorted(SOURCE_FILES)):
        raise ValueError("Not a single-source isolated installed-binary test runtime")
    sources = sorted(p.relative_to(root).as_posix()
                     for p in (root / "src/main/java").rglob("*.java"))
    if sources != sorted(SOURCE_FILES):
        raise ValueError("Unexpected Java source; production substitution is forbidden")
    for p, digest_value in manifest["file_hashes"].items():
        if not (root / p).is_file() or digest(root / p) != digest_value:
            raise ValueError("Isolated Java test code or build instructions changed")
    installed = sorted((root / "run/mods").glob("*.jar"))
    if len(installed) != 1 or installed[0].name != manifest["installed_jar_name"]:
        raise ValueError("Missing or duplicate installed production mod")
    smoke.validate_binary(installed[0], jar_sha)



def verify_cable_receipt(server, native_log, phase, jar_sha):
    """Fail closed on native physical sever/reconnect and automatic scheduled transfer.

    Fixture JSON is not independent native proof: its exact source, compiled-test
    identity and log are separately bound by the installed phase manifest.
    """
    transfer = server.get("cable_transfer")
    keys = {"engine", "cable_count", "connected_endpoints", "severed_endpoints",
            "severed_ticks", "source_before", "source_after", "sink_before",
            "sink_after", "delivered", "total", "direct_transfer_invocations",
            "pending_escrow", "identities", "two_real_clients_connected"}
    if not isinstance(transfer, dict) or set(transfer) != keys:
        raise ValueError("Native two-client physical cable transfer proof missing or incomplete")
    counts = ("cable_count", "connected_endpoints", "severed_endpoints", "severed_ticks",
              "source_before", "source_after", "sink_before", "sink_after",
              "delivered", "total", "direct_transfer_invocations", "pending_escrow")
    if any(type(transfer[k]) is not int or transfer[k] < 0 for k in counts):
        raise ValueError("Invalid native cable integer count or boolean substitution")
    ids = transfer["identities"]
    if (not isinstance(ids, list) or len(ids) != 9
            or any(not isinstance(v, str) or not re.fullmatch(
                r"[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}", v) for v in ids)
            or len(set(ids)) != 9):
        raise ValueError("Invalid or repeated native installed cable/endpoint identity")
    if (transfer["engine"] != "net.foundations.pl4.NetworkEngine.tick"
            or transfer["cable_count"] != 7 or transfer["connected_endpoints"] != 2
            or transfer["severed_endpoints"] != 1
            or not 60 <= transfer["severed_ticks"] <= 100
            or transfer["two_real_clients_connected"] is not True
            or transfer["direct_transfer_invocations"] != 0
            or transfer["pending_escrow"] != 0 or transfer["total"] != 1728):
        raise ValueError("Native two-client topology/revocation/scheduled-tick proof is insufficient")
    before = transfer["source_before"] + transfer["sink_before"]
    after = transfer["source_after"] + transfer["sink_after"]
    if (before != 1728 or after != 1728 or not 0 < transfer["delivered"] < 1728
            or transfer["source_before"] - transfer["source_after"] != transfer["delivered"]
            or transfer["sink_after"] - transfer["sink_before"] != transfer["delivered"]
            or transfer["source_after"] <= 0):
        raise ValueError("Native real-client scheduled network delivery is not item conserving")
    if ((phase == "seed" and
         (transfer["source_before"], transfer["sink_before"]) != (1728, 0))
            or (phase == "restore" and transfer["sink_before"] <= 0)):
        raise ValueError("No correctly seeded/restored native cable provider inventory")
    marker = CABLE_TAG + jar_sha + ":" + str(transfer["delivered"])
    matched = [line for line in native_log.splitlines() if CABLE_TAG in line]
    if len(matched) != 1 or not matched[0].strip().endswith(marker):
        raise ValueError("Missing/ambiguous native installed client-and-cable server tick marker")
    return transfer


def verify_phase(output, phase, source, jar_sha):
    output = Path(output)
    exact_source(source)
    exact_sha(jar_sha)
    manifest = json.loads((output / "installed-client-phase.json").read_text())
    if (manifest.get("schema") != 1 or manifest.get("phase") != phase
            or manifest.get("source_commit") != source
            or manifest.get("installed_jar_sha256") != jar_sha
            or manifest.get("rc_publication_cleared") is not False
            or manifest.get("client_executed") is not True
            or manifest.get("clean_server_exit") is not True
            or manifest.get("clean_client_exits") != [True, True]):
        raise ValueError("Native installed-client phase/exits/binary identity not accepted")
    names = set(FILES) | {"saved-world-" + phase + ".zip"}
    if set(manifest.get("files", {})) != names:
        raise ValueError("Missing independent real-client logs/screenshots/world evidence")
    for name, proof in manifest["files"].items():
        f = output / name
        if (not f.is_file() or f.is_symlink() or f.stat().st_size != proof.get("bytes")
                or digest(f) != proof.get("sha256")):
            raise ValueError("Altered real-client installed-binary artifact: " + name)
    if zip_world_fingerprint(output / ("saved-world-" + phase + ".zip")) != manifest["world"]:
        raise ValueError("Saved native client-world ZIP hash mismatch")
    if manifest.get("copied_input_world") is not None:
        if phase != "restore" or not isinstance(manifest["copied_input_world"], dict):
            raise ValueError("Only restored candidate may copy the saved world")
    elif phase == "restore":
        raise ValueError("Candidate missing exact saved-world copy fingerprint")
    for name in ("server", *NAMES):
        check_project(output / name, source, jar_sha)
    server_log = (output / "server.log").read_text(errors="replace")
    if len([s for s in server_log.splitlines() if TAG + jar_sha in s]) != 1:
        raise ValueError("Native installed production JAR server startup marker absent/ambiguous")
    server = json.loads((output / "server-multiplayer-proof.json").read_text())
    if (server.get("source_commit") != source or server.get("phase") != phase
            or server.get("real_client_names") != list(NAMES)
            or len(set(server.get("real_client_uuids", []))) != 2
            or server.get("fake_players") is not False
            or server.get("foreign_non_operator_denied") is not True
            or server.get("operator_access_explicitly_granted") is not True
            or server.get("initial_items") != 128
            or server.get("items_per_player") != [17, 17]
            or server.get("remaining_items") != 94
            or server.get("duplicate_network_requests_ignored") is not (phase == "seed")
            or server.get("saved_world_restore") is not (phase == "restore")):
        raise ValueError("Actual installed server multiplayer/authorization proof failed")
    verify_cable_receipt(server, server_log, phase, jar_sha)
    for index, name in enumerate(NAMES):
        png = output / (name + "-storage.png")
        if png.stat().st_size <= 1000 or png.read_bytes()[:8] != b"\x89PNG\r\n\x1a\n":
            raise ValueError("No actual native client screenshot for " + name)
        p = json.loads((output / (name + "-client-proof.json")).read_text())
        if (p.get("source_commit") != source or p.get("phase") != phase
                or p.get("name") != name or p.get("uuid") != server["real_client_uuids"][index]
                or p.get("items_received") != 17
                or p.get("identical_packets_sent") != (2 if phase == "seed" else 0)):
            raise ValueError("Real installed client identity/items/packet receipt mismatch")
        client_log = (output / (name + ".log")).read_text(errors="replace")
        if len([s for s in client_log.splitlines() if CLIENT_TAG + jar_sha + ":" + name in s]) != 1:
            raise ValueError("Real installed client never attested production-mod receipt")
    if (not isinstance(server.get("node_identity"), str)
            or not re.fullmatch(r"[0-9a-f-]{36}", server["node_identity"])):
        raise ValueError("Persisted installed PL4 wireless node identity absent")
    return manifest


def _capture(output, phase, source, jar_sha, copied, server_exit, client_exits):
    world = world_fingerprint(output / "server/run/world")
    archive = output / ("saved-world-" + phase + ".zip")
    if snapshot_world(output / "server/run/world", archive) != world:
        raise ValueError("Native world changed during final saved-world ZIP capture")
    files = {}
    for name in (*FILES, archive.name):
        f = output / name
        if not f.is_file() or f.is_symlink() or not f.stat().st_size:
            raise ValueError("Native installed-client evidence missing: " + name)
        files[name] = {"bytes": f.stat().st_size, "sha256": digest(f)}
    record = {
        "schema": 1, "phase": phase, "source_commit": source,
        "installed_jar_sha256": jar_sha, "world": world,
        "copied_input_world": copied, "files": files,
        "client_executed": True, "clean_server_exit": server_exit == 0,
        "clean_client_exits": [v == 0 for v in client_exits],
        "rc_publication_cleared": False,
    }
    (output / "installed-client-phase.json").write_text(json.dumps(record, indent=2, sort_keys=True) + "\n")
    return verify_phase(output, phase, source, jar_sha)



def _finish_server(server, properties):
    """Prefer clean native self-stop over RCON while retaining fail-closed exit."""
    if server.poll() is None:
        try:
            # Native ServerTickEvent shutdown may finish after both clients
            # have closed, even though RCON is already offline.
            server.wait(timeout=30)
        except subprocess.TimeoutExpired:
            if server.poll() is None:
                smoke._stop_with_retry(properties)
                server.wait(timeout=100)
    if server.returncode != 0:
        raise RuntimeError("Installed server did not exit cleanly after two client sessions")



def _wait_for_client_native_bootstrap(process, log_file, deadline):
    """Serialize LWJGL early-native initialization, not game/client sessions.

    Two simultaneous Gradle runClient launches can race on shared NeoForm /
    LWJGL extraction caches. Require the actual first client to finish
    ModLauncher early-display initialization before starting the second.
    Both genuine clients still connect and remain online together.
    """
    marker = "Launching target 'forgeclientdev'"
    log_file = Path(log_file)
    while time.monotonic() < deadline:
        if process.poll() is not None:
            tail = log_file.read_text(errors="replace")[-4000:] if log_file.is_file() else ""
            raise RuntimeError("First installed client exited before native bootstrap: " + tail)
        if log_file.is_file() and marker in log_file.read_text(errors="replace"):
            return
        time.sleep(2)
    raise TimeoutError("First installed client did not finish LWJGL/ModLauncher bootstrap")


def run_phase(checkout, jar, output, phase, source, jar_sha, seed=None, timeout=1500):
    output = Path(output).resolve()
    jar = Path(jar).resolve()
    if phase == "seed" and (source != BASELINE_SOURCE or jar_sha != BASELINE_JAR):
        raise ValueError("Seed must install immutable verified released-beta JAR/source")
    if phase == "restore" and (source != CANDIDATE_SOURCE or jar_sha != CANDIDATE_JAR):
        raise ValueError("Candidate must install exact previously qualified JAR/source")
    if not 120 <= timeout <= 1800:
        raise ValueError("Installed two-client phase timeout must remain bounded")
    copied = _prepare(checkout, jar, output, phase, source, jar_sha, seed)
    for name in ("server", *NAMES):
        check_project(output / name, source, jar_sha)
    env = dict(os.environ, PL4_PROBE_PHASE=phase, PL4_PROBE_SOURCE=source,
               PL4_PROBE_OUTPUT=str(output), PL4_INSTALLED_JAR_SHA256=jar_sha)
    processes, handles = {}, []
    try:
        def launch(name, task):
            handle = (output / (name + ".log")).open("w")
            handles.append(handle)
            process = subprocess.Popen(
                ["bash", "gradlew", "--no-daemon", "--console=plain", task],
                cwd=output / name, env=env, stdin=subprocess.DEVNULL,
                stdout=handle, stderr=subprocess.STDOUT,
                start_new_session=True,
            )
            processes[name] = process
            return process
        server = launch("server", "runServer")
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if server.poll() is not None:
                raise RuntimeError("Actual installed server stopped before two clients connected; native log tail: " + (output / "server.log").read_text(errors="replace")[-12000:])
            if TAG + jar_sha in (output / "server.log").read_text(errors="replace"):
                break
            time.sleep(2)
        else:
            raise TimeoutError("Actual installed-JAR server did not attest startup")
        # Real client processes must overlap for native multiplayer assertions,
        # but their Gradle/LWJGL bootstrap must not race on the same shared cache.
        clients = [launch(NAMES[0], "runClient")]
        _wait_for_client_native_bootstrap(
            clients[0], output / (NAMES[0] + ".log"), deadline
        )
        clients.append(launch(NAMES[1], "runClient"))
        while time.monotonic() < deadline:
            if any(p.poll() not in (None, 0) for p in clients):
                raise RuntimeError("A real installed production-JAR client exited unsuccessfully")
            if all(p.poll() == 0 for p in clients):
                break
            time.sleep(2)
        else:
            raise TimeoutError("Two actual installed NeoForge clients did not complete")
        if not (output / "server-multiplayer-proof.json").is_file():
            raise ValueError("Installed binary real clients exited without server permissions/items proof")
        # The server fixture self-terminates after both native clients report.
        # Its RCON socket closes before Gradle's wrapper process necessarily
        # exits; attempting RCON immediately races that clean native shutdown.
        # Allow bounded natural completion first, then use authenticated RCON
        # only for a genuinely still-running server.
        _finish_server(server, output / "server/run/server.properties")
    finally:
        for p in processes.values():
            if p.poll() is None:
                os.killpg(p.pid, signal.SIGTERM)
                try:
                    p.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    os.killpg(p.pid, signal.SIGKILL)
                    p.wait()
        for h in handles:
            h.close()
    return _capture(output, phase, source, jar_sha, copied,
                    processes["server"].returncode,
                    [processes[name].returncode for name in NAMES])


def verify_upgrade(seed, restore):
    a = verify_phase(seed, "seed", BASELINE_SOURCE, BASELINE_JAR)
    b = verify_phase(restore, "restore", CANDIDATE_SOURCE, CANDIDATE_JAR)
    sa = json.loads((Path(seed) / "server-multiplayer-proof.json").read_text())
    sb = json.loads((Path(restore) / "server-multiplayer-proof.json").read_text())
    ca = verify_cable_receipt(sa, (Path(seed) / "server.log").read_text(errors="replace"),
                              "seed", BASELINE_JAR)
    cb = verify_cable_receipt(sb, (Path(restore) / "server.log").read_text(errors="replace"),
                              "restore", CANDIDATE_JAR)
    if (ca["identities"] != cb["identities"]
            or ca["source_after"] != cb["source_before"]
            or ca["sink_after"] != cb["sink_before"]):
        raise ValueError("Migrated physical cable UUIDs or saved native inventories changed")
    if (a["world"] != b["copied_input_world"]
            or sa["node_identity"] != sb["node_identity"]
            or sa["real_client_uuids"] != sb["real_client_uuids"]
            or a["installed_jar_sha256"] == b["installed_jar_sha256"]):
        raise ValueError("Independent real-client migration provenance/identity failed")
    result = {
        "schema": 1, "seed_source": BASELINE_SOURCE, "candidate_source": CANDIDATE_SOURCE,
        "published_beta_jar_sha256": BASELINE_JAR, "candidate_jar_sha256": CANDIDATE_JAR,
        "seed_saved_world_sha256": a["world"]["sha256"],
        "restored_saved_world_sha256": b["world"]["sha256"],
        "seed_receipt_sha256": digest(Path(seed) / "installed-client-phase.json"),
        "restore_receipt_sha256": digest(Path(restore) / "installed-client-phase.json"),
        "real_installed_clients_each_phase": 2,
        "real_client_screenshots": 4,
        "network_request_replay_denied": True,
        "two_client_physical_cable_auto_transfer": True,
        "native_cable_severed_ticks_each_phase": [
            ca["severed_ticks"], cb["severed_ticks"]],
        "native_cable_delivery_each_phase": [ca["delivered"], cb["delivered"]],
        "installed_binary_two_client_migration": "PASS",
        "scope": "Two real offline-mode clients in each installed production-JAR phase with saved migration, NetworkEngine scheduled DATA_CABLE transfer and 60+ ticks of physical route denial. No online authentication, owner-revocation after provider callback, optional installed providers, arbitrary worlds, sustained performance, other exact versions or RC clearance.",
        "rc_publication_cleared": False,
    }
    (Path(restore) / "installed-two-client-upgrade-receipt.json").write_text(
        json.dumps(result, indent=2, sort_keys=True) + "\n"
    )
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    actions = parser.add_subparsers(dest="command", required=True)
    for cmd in ("seed", "restore"):
        p = actions.add_parser(cmd)
        p.add_argument("--checkout", required=True, type=Path)
        p.add_argument("--jar", required=True, type=Path)
        p.add_argument("--output", required=True, type=Path)
        p.add_argument("--timeout-seconds", type=int, default=1500)
        if cmd == "restore":
            p.add_argument("--seed", required=True, type=Path)
    p = actions.add_parser("verify")
    p.add_argument("--seed", required=True, type=Path)
    p.add_argument("--restore", required=True, type=Path)
    args = parser.parse_args()
    if args.command == "seed":
        result = run_phase(args.checkout, args.jar, args.output, "seed",
                           BASELINE_SOURCE, BASELINE_JAR, timeout=args.timeout_seconds)
    elif args.command == "restore":
        result = run_phase(args.checkout, args.jar, args.output, "restore",
                           CANDIDATE_SOURCE, CANDIDATE_JAR, seed=args.seed,
                           timeout=args.timeout_seconds)
    else:
        result = verify_upgrade(args.seed, args.restore)
    print(json.dumps(result, sort_keys=True))


if __name__ == "__main__":
    main()
