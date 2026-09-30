# Validation — Foundations PL4 0.0.1a.R9

**SOURCE CANDIDATE. No runtime R9 mod JAR.** The Gradle download failed before native compilation with `UnknownHostException: services.gradle.org` (exit1). Java syntax and standalone production-rule tests are not Minecraft API compilation, native server startup or graphical validation.

## Checks actually executed

| Check | Evidence / result |
|---|---|
| R9 production render planner, revisioned transactions and cursor unprojection | Java21 compilation/execution: **31,968 assertions passed**. Tests inspect actual typed draw operations, 4,000 bounds/movement cases, mode/page/source/element limits, stale revisions, layer ordering, 1,000 affine inversions and 500 arbitrary-rotation perspective projections. |
| Deliberately reintroduced R9 regressions | **4 rejected** by executable tests: block-as-item, stale edit accepted, counter over editor, wrong cursor origin. |
| Retained production suites | R5 **1,149,451**; R6 **3,275** (includes synthetic accessor fixtures, not real mods); R7 **9,665**; R8 **2,750** assertions passed. |
| Retained R8 mutations | **3 rejected**: perpendicular extension, mirrored hologram back, older layout wins. |
| Java AST syntax | **66 production Java files parsed** using Java21; does not resolve Minecraft/NeoForge symbols. |
| GUI-layer guard | **11 tests passed**, including deliberately wrong order/stale-tooltip cases. |
| Resource references | **370 models**,312 roots,72 atlas sprites; **482 JSON files** parsed. Oriented216 front/back variants,60 paired-reader states and18 endpoint leads validated geometrically, not Minecraft baking. |
| Guide and data wiring |22 chapters,3,653 body words, current type/mode controls, chapter summaries/bookmarks/search, bounded resources and actual server/planner call paths checked. Source guards are structural, not GUI screenshots. |
| Preserved assets/recipes |All116 R8 PNGs (113 original+3guide) and35 recipes byte-identical. One1x1 rendering primitive PNG added. |
| Generators |R5→R6→R7→R8 resource generators rerun: **605 resource files byte-identical**. |
| KubeJS example |Retained example executed with a Node.js stub. NOT KubeJS/Rhino or a game recipe reload. |
| Packaged source |See `validation/offline-packaged.log` for the complete final-source extraction rerun. |
| Updater behavior |See `validation/updater-filesystem-tests.json`: independent Python filesystem model using the actual manifest/payload. NOT execution of PowerShell, BAT or Windows dialogs. |
| Package integrity |ZIP CRCs, entry SHA256, exact delta/payload hashes, no unsafe/duplicate paths and no runtime JAR checked in `validation/package-audit.json`. |

## Not executed / acceptance gates

- Full Minecraft/NeoForge API compilation and runtime-JAR assembly; build attempt is in `validation/native-packaged-build-attempt.log`.
- All **93 native GameTests** (73 retained+20 R9). R9 native tests cover typed NBT/JSON, visual component/quantity snapshots, fluid metadata, nested inventory exclusion, owner/distance/identity/stale/source checks and joined layout continuity. Registered source fixtures are not passes.
- In-game rendering, framebuffer/depth behavior, all monitor orientations, actual mouse picking, shader compatibility, guide readability and input controls.
- Real Mekanism/GTCEu/other mod providers, native KubeJS runtime, dedicated-server/network stress, heap/frame-time/leak profiling.
- Windows updater or launcher execution. A filesystem simulation does not establish the PowerShell script or picker ran.

## Native command

```bat
gradlew.bat --no-daemon --console=plain clean build runGameTestServer
```

Expected artifact **only after successful local build**: `build/libs/FoundationsPL4-1.21.1-0.0.1a.R9.jar`. Client and server require matching protocol4. Test copied worlds and keep the pre-R9 snapshot. Older versions cannot preserve typed layouts; rolling back source is not a world rollback.

The source implementation is intended to restore distinct PL2-style render/editor paths. It does not establish full GSI or visual parity. Use `R9_ACCEPTANCE.md` for runtime checks and `PARITY.md` for outstanding features. Previous recorded revisions are historical evidence only.
