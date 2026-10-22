"""Compile the compact layout and prove non-UI runtime entries match native-tested bytes."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    root = args.root.resolve()
    status = json.loads((root / "BUILD_STATUS.json").read_text())
    mc, version = status["minecraft"], status["version"]
    if status.get("candidate_native_validation") != "PASS":
        raise ValueError("A passing native development baseline is required first")
    run_id = status["candidate_workflow_run"]
    repo = os.environ["GITHUB_REPOSITORY"]
    data = json.loads(subprocess.check_output(["gh", "api", f"repos/{repo}/actions/runs/{run_id}/artifacts?per_page=100"], text=True))
    matches = [x for x in data["artifacts"] if not x["expired"] and
               x["name"].startswith(f"UNRELEASED-RC-development-{mc}-{run_id}-")]
    if not matches:
        raise ValueError("Exact native candidate artifact is absent or expired")
    artifact = max(matches, key=lambda x: x["id"])
    previous = root / "previous-native-baseline"
    subprocess.run(["gh", "run", "download", str(run_id), "--repo", repo, "--name", artifact["name"], "--dir", str(previous)], check=True)
    suffix = "-java8" if status.get("java") == 8 else ""
    name = f"FoundationsPL4-{mc}-{version}{suffix}.jar"
    old = list(previous.rglob(name))
    if len(old) != 1:
        raise ValueError("Missing or ambiguous exact native-tested runtime")
    reports = list(previous.rglob("rc-development-gate.json"))
    if len(reports) != 1:
        raise ValueError("Missing native baseline hash receipt")
    receipt = json.loads(reports[0].read_text())
    digest = hashlib.sha256(old[0].read_bytes()).hexdigest()
    if receipt["artifact_sha256"].get("build/libs/" + name) != digest:
        raise ValueError("Native baseline checksum mismatch")
    logs = root / "verification-logs"
    logs.mkdir(exist_ok=True)
    with (logs / "compact-ui-build.log").open("w") as output:
        result = subprocess.run(["bash", "gradlew", "--no-daemon", "--console=plain", "clean", "build"], cwd=root,
                                stdout=output, stderr=subprocess.STDOUT, timeout=2100)
    print((logs / "compact-ui-build.log").read_text(errors="replace")[-16000:], flush=True)
    if result.returncode:
        raise ValueError("Compact UI build failed")
    current = root / "build/libs" / name
    changes, metadata, compared = [], [], 0
    with zipfile.ZipFile(old[0]) as before, zipfile.ZipFile(current) as after:
        names = set(before.namelist()) | set(after.namelist())
        for entry in sorted(names):
            if entry.endswith("/"):
                continue
            a = before.read(entry) if entry in before.namelist() else None
            b = after.read(entry) if entry in after.namelist() else None
            if a == b:
                compared += 1
                continue
            if entry == "assets/foundations_pl4/guide/en_us.json" and a is not None and b is not None:
                old_book, new_book = json.loads(a), json.loads(b)
                old_book.pop("edition")
                edition = new_book.pop("edition")
                if old_book != new_book or not edition.endswith("Foundations PL4 " + version):
                    raise ValueError("Guide changes exceed the candidate edition metadata")
                metadata.append(entry)
                continue
            if not (entry.startswith("net/foundations/pl4/client/WirelessStorageScreen") and entry.endswith(".class")):
                raise ValueError("A non-UI runtime entry changed; full native revalidation required: " + entry)
            changes.append(entry)
    if not (changes or metadata) or compared < 100:
        raise ValueError("Invalid UI-only comparison or missing production runtime")
    report = {"minecraft": mc, "version": version, "compact_ui_compilation": "PASS",
              "unchanged_runtime_entries_byte_identical_to_native_baseline": compared,
              "guide_edition_metadata_changes": metadata,
              "changed_client_classes": changes, "native_baseline_run": run_id,
              "native_baseline_artifact": artifact["id"], "native_baseline_sha256": digest,
              "updated_runtime_sha256": hashlib.sha256(current.read_bytes()).hexdigest(),
              "actual_client_visual_acceptance": "PENDING", "rc_publication_cleared": False}
    (logs / "compact-ui-parity.json").write_text(json.dumps(report, indent=2) + "\n")
    status.update(compact_storage_ui_source_parity="PASS", compact_ui_compilation="PASS",
                  native_baseline_artifact=artifact["id"], non_ui_runtime_byte_parity="PASS_EXCEPT_VERIFIED_GUIDE_EDITION",
                  ui_parity_workflow_run=os.environ.get("GITHUB_RUN_ID"))
    (root / "BUILD_STATUS.json").write_text(json.dumps(status, indent=2) + "\n")
    print("PASS compact UI compilation; other runtime code matches the native baseline and only guide edition metadata changes")


if __name__ == "__main__":
    main()
