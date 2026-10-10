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

The full-feature Minecraft 1.14.4 / Forge 28.2.26 / Java 8 port is now **published as an alpha**, after independently verifying both the 194/194 scenario suite and a clean production-only Forge28 startup/shutdown. The Forge28-native fixed-function renderer retains multi-part cables, text/picture/fluid display canvases, the GUI editor and animated hammer. See [published mc1.14.4-v0.2a-port.1 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.14.4-v0.2a-port.1) and [successful gated release run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/38018971199). Runtime JAR SHA256 `091c205a1dece3399221a8aaee055ccf61a5f28309bc33097f67a01bf4a5057e`. Exact source ZIP SHA256 `f94c3c415ed80430bef5b39d59711dd61160a057e4cdbd311fce512e2d082ac1`. Real-client visuals, multiplayer, installed third-party API combinations and sustained modpack performance remain unverified; do not interpret a server-native alpha as complete stable parity. Candidate-source attempts, even with correct Minecraft version metadata, are not playable builds. See branch `.github/workflows/forge-native.yml` and retained native diagnostics.

## Remaining acceptance

Native test successes do not establish full client graphics, all installed optional mods, public multiplayer, performance under real modpacks, or feature parity with 1.21.1 and 26.x. Optional APIs must have safe PL4-owned core alternatives where their functionality can be implemented natively; foreign power conversion cannot be invented from absent APIs. Maintain separate statuses for each Minecraft patch version. Do not substitute a neighboring-version JAR.

**Further API testing is still required.**


## Additional independently started version-gap ports

**1.18.1:** created `mc/1.18.1`, targeting exact Minecraft 1.18.1 / Forge 39.1.2 / Java 17. It is derived from the separately tested full-feature 1.18.2 branch, with its own mod metadata, Gradle target, protocol identity and 193-test native release gate. Development starts at [source setup commit](https://github.com/Shnewbs/Foundations-PL4/commit/219acc3791640d47076ced615732125d4d67e47a). Native acceptance and release are distinct and must be verified; branch presence alone does not mean a working runtime.

**1.13.2:** created `mc/1.13.2` targeting exact Minecraft 1.13.2 / Forge 25.0.223 / Java 8. Initial native build [38019426280](https://github.com/Shnewbs/Foundations-PL4/actions/runs/38019426280) failed because Mojang official ProGuard mappings are unavailable for 1.13.2. Updated Gradle to archived MCP `snapshot:20180921-1.13`; [native build 38019579683](https://github.com/Shnewbs/Foundations-PL4/actions/runs/38019579683) got past mapping setup but revealed substantial MCP-era class/API naming incompatibilities inherited from the 1.14.4 feature source. **1.13.2 native compilation and scenarios are NOT complete; no runtime was published.** This will require a dedicated MCP source backport, not another loader version-string change. Keep Java 8 and the correct native test harness separate from modern GameTests.

Both tracks retain full-feature parity as a goal, rather than claiming it or substituting a compiled neighboring Minecraft JAR. Optional installed-provider testing, visuals, multiplayer and performance remain independently open.
