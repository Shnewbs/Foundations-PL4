# Foundations PL4 — Port development checkpoint (October 9–10, 2026)

This checkpoint records evidence from the latest port-development pass without rewriting historical audit snapshots. Project goal: carry the 1.21.1/26.x behavior to all selected Minecraft versions with independent native and optional-provider acceptance. Minecraft 1.7.10 remains excluded; 1.6.4 remains experimental.

## Newly completed: exact Minecraft 1.16.1 release

The independent `mc/1.16.1` source branch now has a published [0.2a-port.1 alpha release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.16.1-v0.2a-port.1). Its native run [38013470863](https://github.com/Shnewbs/Foundations-PL4/actions/runs/38013470863) passed **193 of 193** isolated server scenarios on the exact Minecraft 1.16.1 / Forge 32.0.108 target and a separate clean production-server startup/stop test. These are adapted dedicated-server scenarios, not modern GameTests. Java 8 bytecode/reobfuscation and standalone archive checks also passed.

- Runtime: `FoundationsPL4-1.16.1-0.2a-port.1.jar`, SHA256 `d617c48b04205215f00dc8de31b5e4e0850d86a70e5c6a6c6b4aa20b3698a4ca`.
- Exact source: `FoundationsPL4-1.16.1-0.2a-port.1-source.zip`, SHA256 `a78ba8688a2e17372d05521bcdcc1f36e8d1c41e59defab70d9d384e1dff72f9`.
- Critical fixes: Forge 32 obstruction-aware vanilla chest/double-chest access without bypassing modded denied capabilities; built-in forging alternatives for stone, diamond, redstone, and ender pearl alongside modded tag lookup. The previous 54 failures were reduced to zero rather than excluded.
- IMPORTANT runtime profile: the tested installed-server path uses a pinned ModLauncher 8.1.3 compatibility profile on Java 8; do not assume a stock unmodified Forge 32 launcher was accepted. See `launch-profile.json` and `production-smoke.json` attached to the release.

## In progress: exact Minecraft 1.14.4 target

The new `mc/1.14.4` branch is derived from the fuller-feature 1.15.2 source and targets Forge 28.2.26 / Java 8. Its native branch CI explicitly forbids a release merely because metadata or compilation succeeds. The first mapped Forge 28 pass addresses legacy block properties, event/packet direction checks, registration, tags, worldgen and item behavior, without silently removing displays or multipart features.

The Forge 28 renderer, GUI matrices, and some old entity API differences still need a dedicated backport. Compilation has not been certified, and **no 1.14.4 release is approved**. Candidate-source attempts, even with correct Minecraft version metadata, are not playable builds. See branch `.github/workflows/forge-native.yml` and retained native diagnostics.

## Remaining acceptance

Native test successes do not establish full client graphics, all installed optional mods, public multiplayer, performance under real modpacks, or feature parity with 1.21.1 and 26.x. Optional APIs must have safe PL4-owned core alternatives where their functionality can be implemented natively; foreign power conversion cannot be invented from absent APIs. Maintain separate statuses for each Minecraft patch version. Do not substitute a neighboring-version JAR.

**Further API testing is still required.**
