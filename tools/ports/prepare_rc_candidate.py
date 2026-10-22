"""Prepare an explicitly unpublished RC development snapshot for a modern port."""
from pathlib import Path
import argparse
import json
import re
from align_performance import network, transfers

VERSION = "0.2.0-rc.1-dev"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("minecraft")
    args = parser.parse_args()
    root = args.root
    status_path = root / "BUILD_STATUS.json"
    status = json.loads(status_path.read_text())
    if status["minecraft"] != args.minecraft or (root / "src/legacy/java").is_dir():
        raise ValueError("Wrong target or excluded legacy runtime")
    java = root / "src/main/java/net/foundations/pl4"
    edits = {java / name: transform((java / name).read_text()) for name, transform in
             [("NetworkEngine.java", network), ("TransferEngine.java", transfers)]}
    build_path = root / "build.gradle"
    build, count = re.subn(r"^version\s*=\s*'[^']+'", "version = '" + VERSION + "'", build_path.read_text(), count=1, flags=re.M)
    if count != 1:
        raise ValueError("Missing Gradle version")
    edits[build_path] = build
    old = dict(status)
    old.pop("previous_release_evidence", None)
    if status["version"] != VERSION:
        status["previous_release_evidence"] = old
    # Previous binary hashes and PASS fields cannot certify newly changed source.
    for key in list(status):
        if key.startswith("verified_") or key.startswith("local_native_") or key in {
                "native_scenarios", "native_game_tests", "production_smoke", "native_test_result",
                "native_test_evidence", "production_artifact_result", "production_boot_result",
                "native_verification_run", "native_compilation", "native_startup", "native_reobfuscation",
                "runtime_packaging", "executed_native_scenarios", "production_jar_test_isolation"}:
            del status[key]
    status.update(version=VERSION, release_channel="development", release_published=False,
                  port_status="RC_DEVELOPMENT_NATIVE_VALIDATION_PENDING",
                  installed_runtime_acceptance="PENDING_FOR_THIS_CANDIDATE",
                  installed_optional_provider_acceptance="PENDING_FOR_THIS_CANDIDATE",
                  client_visual_acceptance="PENDING_FOR_THIS_CANDIDATE",
                  live_multiplayer_acceptance="PENDING_FOR_THIS_CANDIDATE",
                  measured_performance_acceptance="PENDING_FOR_THIS_CANDIDATE",
                  migration_acceptance="PENDING_FOR_THIS_CANDIDATE",
                  further_api_testing_required=True,
                  release_notes=f"docs/releases/{VERSION}.md",
                  performance_baseline="indexed-network-membership-and-cycle-endpoint-keys")
    edits[status_path] = json.dumps(status, indent=2) + "\n"
    edits[root / "release-version.txt"] = VERSION + "\n"
    notes = root / "docs/releases" / (VERSION + ".md")
    edits[notes] = (
        f"# Foundations PL4 {args.minecraft} — RC development snapshot\n\n"
        "Unpublished development build preparing a common performance baseline. This is not a stable release or a cleared release candidate.\n\n"
        "Includes topology-owned network membership/storage indexes and per-cycle endpoint identity reuse. "
        "Target-specific loader APIs are retained. Gameplay routing, rates, filters, ownership and saved data are unchanged.\n\n"
        "Native build/scenario evidence must identify this exact source and artifact. Installed optional providers, actual client UI, "
        "live multiplayer, world migration and repeatable large-network performance acceptance remain open. "
        "No measured TPS/MSPT or memory improvement is claimed. Legacy subset ports do not inherit these changes or their results.\n")
    for path, content in edits.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)
    print(f"Prepared {args.minecraft} {VERSION}; publication remains blocked on RC acceptance")


if __name__ == "__main__":
    main()
