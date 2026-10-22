"""Run the exact port's native gate and retain artifact hashes, without publishing."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    root = args.root.resolve()
    logs = root / "verification-logs"
    logs.mkdir(exist_ok=True)
    status_path = root / "BUILD_STATUS.json"
    status = json.loads(status_path.read_text())
    mc = status["minecraft"]
    expected = status.get("expected_native_scenarios", status.get("expected_native_tests"))
    if not isinstance(expected, int) or expected <= 0:
        raise ValueError("No exact-target native fixture count")

    def run(command, name, timeout=2100):
        print("RUN", " ".join(command), flush=True)
        path = logs / name
        with path.open("w") as output:
            result = subprocess.run(command, cwd=root, stdout=output, stderr=subprocess.STDOUT, timeout=timeout)
        text = path.read_text(errors="replace")
        print(text[-16000:], flush=True)
        if result.returncode:
            for server_log in logs.glob("installed-*-native.log"):
                if server_log != path:
                    print("SERVER DIAGNOSTICS", server_log.name, flush=True)
                    print(server_log.read_text(errors="replace")[-18000:], flush=True)
            raise ValueError(f"{name}: process exited {result.returncode}")
        return text

    gradle = ["bash", "gradlew", "--no-daemon", "--console=plain"]
    if mc in {"1.21.1", "26.1.2", "26.3"}:
        run([sys.executable, "tools/run_offline_checks.py"], "offline.log")
    profiles = [None] if mc != "26.3" else ["26.3.0.23-beta", "26.3.0.39-beta"]
    installed_runners = {"1.19": "1190", "1.19.1": "1191", "1.19.3": "1193", "1.19.4": "1194",
                         "1.20.1": "1201", "1.20.2": "1202", "1.20.4": "1204"}
    for profile in profiles:
        suffix = ("-" + profile) if profile else ""
        command = gradle + (["-PneoForgeVersion=" + profile] if profile else [])
        run(command + ["clean", "build"], "build" + suffix + ".log")
        if status.get("java") == 8:
            text = run([sys.executable, "tools/java8/runtime.py", "native"], "native-java8.log")
            # The installed runner verifies scenario counts, exact runtime/test entries and clean production boot.
            report = json.loads((logs / "java8-summary.json").read_text())
            if report.get("minecraft") != mc:
                raise ValueError("Installed Java 8 runner used another Minecraft target")
        elif mc == "1.17.1":
            run([sys.executable, "tools/verify_installed117.py"], "native-installed.log")
        elif mc == "1.18.1":
            run([sys.executable, "tools/run_1181_native_tests.py"], "native-installed.log")
        elif mc in installed_runners:
            # These ports isolate their native fixtures from the production mod.
            # Their stock development GameTest task can complete with zero tests.
            run([sys.executable, "tools/run_" + installed_runners[mc] + "_native_tests.py"], "native-installed.log")
        else:
            log_name = "native" + suffix + ".log"
            text = run(command + ["runGameTestServer"], log_name)
            if mc.startswith("26."):
                run([sys.executable, "tools/verify_native_port_run.py", str(logs / log_name)], "fixture-check" + suffix + ".log")
            elif not re.search(r"All\s+" + str(expected) + r"\s+required tests passed", text, re.I):
                raise ValueError(f"Missing exact {expected}-fixture native success marker")
        if profile:
            saved = logs / ("artifacts-" + profile)
            saved.mkdir()
            for path in (root / "build/libs").glob("*.jar"):
                shutil.copy2(path, saved / path.name)
    hashes = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
              for p in (root / "build/libs").glob("*.jar")}
    if not hashes:
        raise ValueError("No candidate JARs")
    sources = {name: hashlib.sha256((root / "src/main/java/net/foundations/pl4" / name).read_bytes()).hexdigest()
               for name in ["NetworkEngine.java", "TransferEngine.java"]}
    report = {"minecraft": mc, "version": status["version"], "native_gate": "PASS",
              "expected_fixtures": expected, "validated_loader_profiles": profiles,
              "source_parent": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip(),
              "workflow_run": os.environ.get("GITHUB_RUN_ID"), "artifact_sha256": hashes,
              "performance_source_sha256": sources, "rc_publication_cleared": False,
              "remaining_acceptance": ["installed optional providers", "actual client UI", "live multiplayer",
                                       "world migration", "measured large-network performance", "cross-port feature parity"]}
    (logs / "rc-development-gate.json").write_text(json.dumps(report, indent=2) + "\n")
    status.update(port_status="RC_DEVELOPMENT_NATIVE_GATE_PASSED", native_compilation="PASS_FOR_THIS_CANDIDATE",
                  candidate_native_validation="PASS", candidate_workflow_run=os.environ.get("GITHUB_RUN_ID"),
                  candidate_item_safety_validation="PASS_IN_EXACT_TARGET_NATIVE_FIXTURES",
                  rc_publication_cleared=False)
    if mc == "1.21.1" and (root / "src/portTest").is_dir():
        status["native_test_isolation"] = "PASS"
    status_path.write_text(json.dumps(status, indent=2) + "\n")
    print("PASS native candidate gate; RC publication acceptance remains open", flush=True)


if __name__ == "__main__":
    main()
