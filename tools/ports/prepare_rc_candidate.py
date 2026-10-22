"""Prepare an explicitly unpublished RC development snapshot for a modern port."""
from pathlib import Path
import argparse
import json
import re
import subprocess
from align_performance import network, transfers
from align_storage_screen import screen, guide_edits
from align_channel_status import engine as channel_engine, rules as channel_rules, regressions as channel_regressions

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
    ui_path = java / "client/WirelessStorageScreen.java"
    edits[ui_path] = screen(ui_path.read_text())
    edits[java / "TransferEngine.java"] = channel_engine(edits[java / "TransferEngine.java"])
    edits[java / "core/TransferRules.java"] = channel_rules((java / "core/TransferRules.java").read_text())
    edits[root / "tools/R16RegressionTests.java"] = channel_regressions((root / "tools/R16RegressionTests.java").read_text())
    owned_fixtures = []
    if args.minecraft in {"1.20.1", "1.20.2", "1.20.4"}:
        bootstrap = root / "src/portTest/java/net/foundations/pl4/PLPortTestMod.java"
        text = bootstrap.read_text()
        condition = 'if (!Boolean.getBoolean("foundations_pl4.isolatedGameTestConfig")) {'
        if condition not in text and "if (net.minecraftforge.fml.loading.FMLEnvironment.production) {" not in text:
            raise ValueError("Unreviewed isolated native test bootstrap")
        # Memory-backed config is independent of whether Forge's installed
        # environment needs explicit fixture registration and ticker dispatch.
        edits[bootstrap] = text.replace(condition, "if (net.minecraftforge.fml.loading.FMLEnvironment.production) {")
        owned_fixtures.append(bootstrap)
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
    status["candidate_native_validation"] = "PENDING"
    edits[status_path] = json.dumps(status, indent=2) + "\n"
    edits[root / "release-version.txt"] = VERSION + "\n"
    notes = root / "docs/releases" / (VERSION + ".md")
    edits[notes] = (
        f"# Foundations PL4 {args.minecraft} — RC development snapshot\n\n"
        "Unpublished development build preparing a common performance baseline. This is not a stable release or a cleared release candidate.\n\n"
        "Includes topology-owned network membership/storage indexes and per-cycle endpoint identity reuse. "
        "The compact Wireless Storage layout fix is carried forward. "
        "Transfer status uses the same channel/direction decision as actual routing. "
        "Target-specific loader APIs are retained. Gameplay routing, rates, filters, ownership and saved data are unchanged.\n\n"
        "Native build/scenario evidence must identify this exact source and artifact. Installed optional providers, actual client UI, "
        "live multiplayer, world migration and repeatable large-network performance acceptance remain open. "
        "No measured TPS/MSPT or memory improvement is claimed. Legacy subset ports do not inherit these changes or their results.\n")
    for path, content in edits.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)
    guides = guide_edits(root)
    for path, content in guides.items():
        path.write_text(content)
    # Older retained controllers stage Java/status paths explicitly. Keep the
    # versioned resources they compile in their exact-source archive as well.
    if (root / ".git").exists():
        subprocess.run(["git", "add", "--"] + [str(p.resolve()) for p in [*guides, *owned_fixtures]], cwd=root, check=True)
    print(f"Prepared {args.minecraft} {VERSION}; publication remains blocked on RC acceptance")


if __name__ == "__main__":
    main()
