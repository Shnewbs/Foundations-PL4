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


## Additional October 9–10 Forge release results

### Two newly published full-feature-derived Forge alphas

**Minecraft 1.19.1 / Forge 42.0.9 / Java 17:** Published [`mc1.19.1-v0.2a-port.1`](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.19.1-v0.2a-port.1), with **191/191 native GameTests**, standalone build/reobfuscation and SHA256 assets verified in [successful CI](https://github.com/Shnewbs/Foundations-PL4/actions/runs/38024845699). Runtime JAR SHA256 `b3d6e69960fb83fdf1221b084cb7dba269a2db1bffd0647c418f933c5249c527`; exact source ZIP SHA256 `a0ef55cf2fbbcb530159239b1940015bead8ffbdeeac4c870adb5b5585273f50`. Forge42 uses sided `CapabilityItemHandler`, `CapabilityFluidHandler` and `CapabilityEnergy` rather than newer `ForgeCapabilities`.

**Minecraft 1.19.3 / Forge 44.1.23 / Java 17:** Published [`mc1.19.3-v0.2a-port.1`](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.19.3-v0.2a-port.1), with **191/191 native GameTests** and checksum assets verified in [successful CI](https://github.com/Shnewbs/Foundations-PL4/actions/runs/38024939431). Runtime JAR SHA256 `a2a8e87752d79b0e36d13f5c8d427cf701b16e02ddd198dd7ee67dc623e927d5`; matching source ZIP SHA256 `0224d5d4cfc24c4a2f12df6d8e90cb1c65b8adec217e425b9b5e6148b23f92b2`. The port migrates JOML display transforms, real creative-tab registration (Forge44's three-argument generator), native registry keys, and private widget positions. A failed workflow originally pushed the immutable tag without a release because of malformed `GH_TOKEN` configuration; the corrected release workflow verified identical production sources, preserved the original tag, and published without changing its source revision.

Both are **alphas** rather than installed-mod, real-client, multiplayer or modpack stability certifications. The independent source branches carry the full-feature-derived PL4 code; remaining API alternatives and parity acceptance remain separate.

### Earlier-API tracks still under development

**Minecraft 1.18 / Forge 38.0.17 / Java17:** New branch `mc/1.18` derived from 1.18.1, with 193 retained tests and native source/pack metadata. This early Forge38 predates the standard Forge39 automated GameTest-server launcher. Compilation alone is not an acceptable release gate. An independent dedicated-server `/test runall` harness has been added; publication remains blocked until all 193 tests are genuinely exercised and independently reported. A prior migration script had committed its status flag before the Java source migration; the guard now verifies the actual committed GameTest source before allowing a no-op. **No 1.18 JAR is approved or uploaded.**

**Minecraft 1.13.2 / Forge 25.0.223 / Java8:** Exact mapping setup was corrected to historical MCP snapshot mappings. A first legacy class-name migration is committed, but extensive method/renderer/registry incompatibilities still hit the compiler's 1,000-error reporting cap. This target needs an independent native API backport and then gameplay, client and installed-mod acceptance. **No 1.13.2 JAR is approved or uploaded.**

The [cross-version CurseForge publishing workflow](../.github/workflows/curseforge.yml) checks exact tags, checksums, loader identity, release channel and existing upload receipts; it must never submit an untested source branch or claim that CurseForge moderation has approved a file. It is scheduled hourly and also accepts manual release tags.

**Further API testing is still required.**


## Forge 38 native command transport — verification checkpoint

The exact Minecraft **1.18 / Forge 38.0.17** native source compiles, but the original Forge39-only `runGameTestServer` task is unavailable on this loader. The branch `mc/1.18` now has a dedicated-server test runner with **localhost-only authenticated RCON** to send the actual `test runall` command. It checks the true 193-test completion marker and refuses publication on missing/unsupported commands, timeout, or an unclean shutdown. The first stdin-based attempts did not confirm acceptance. The RCON update is committed at `d19f9ecef5dffd81a92b4c17458d3fe6808c38a2`; CI must pass before a 1.18 runtime or CurseForge upload is claimed.

The 1.19.1 and 1.19.3 alphas both passed **191/191 installed Forge GameTests** and have public GitHub runtime/source/checksum releases and version-specific CurseForge upload receipts. CurseForge moderation and real-client/API/multiplayer acceptance are separate. Minecraft 1.13.2 still has significant native MCP-era source work, and 1.6.4 stays experimental. **No 1.7.10 port.**
