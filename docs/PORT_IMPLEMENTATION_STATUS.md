# New-port implementation and release status

Verified October 9, 2026. This implementation ledger supersedes earlier roadmap statements that these six tracks were only planned or scaffolded. Minecraft 1.7.10 remains excluded. Minecraft 1.6.4 remains experimental.

## Published native builds

| Minecraft / branch | Release version | Forge runtime | Runtime Java | Completed automated acceptance | Scope |
|---|---|---|---|---|---|
| 1.20.1 / `mc/1.20.1` | `0.2a-port.1` | 47.4.26 | 17 | 191 native GameTests passed | Forge alpha feature port |
| 1.19.2 / `mc/1.19.2` | `0.2a-port.1` | 43.5.2 | 17 | 191 native GameTests passed | Forge alpha feature port |
| 1.18.2 / `mc/1.18.2` | `0.2a-port.2` | 40.3.12 | 17 | 193 native GameTests passed | Forge alpha feature port |
| 1.16.5 / `mc/1.16.5` | `0.2a-port.1` | 36.2.42 | 17 REQUIRED | 191 isolated native server scenarios passed | Forge alpha feature port; NOT Java-8-compatible |
| 1.12.2 / `mc/1.12.2` | `0.2a-legacy-preview.1` | 14.23.5.2864 | 8 | 21 isolated native server scenarios passed | Initial legacy item-transport subset; NOT full modern 0.2a parity |
| 1.6.4 / `mc/1.6.4` | `0.2a-legacy-preview.1` | 9.11.1.1345 | 7 | 21 isolated native server scenarios passed | Experimental initial legacy item-transport subset; NOT full modern 0.2a parity |

All six have their own published GitHub prerelease, native runtime JAR, source JAR, exact source ZIP, checksums and native test log. The two legacy previews additionally run the same portable production-rule suite: 38,648 assertions. These are separate algorithm assertions, not additional native Minecraft scenarios. Verification downloads were checksum-checked; runtime bytecode levels were checked as Java 17 for the four newer Forge ports, Java 8 for 1.12.2 and Java 7 for 1.6.4. Runtime JARs do not bundle Minecraft/Forge classes, external core mods or the separate legacy scenario mods.

The first three Forge alpha ports were already implemented when this completion pass resumed. This pass fixed and released the 1.16.5 port, implemented the two legacy subsets, repaired their native validation pipelines, and collected verified builds/source/evidence for all six. No branch scaffold or renamed modern binary is counted as a native port.

## Exact release references

- [1.20.1 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.20.1-v0.2a-port.1), source `e53c93e895ce49d61597337dab85c8bb08460d3d`; [native build run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37928651440).
- [1.19.2 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.19.2-v0.2a-port.1), source `ba21143b159fc9d993b6e404f8f73d1e06f7b639`; [native build run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37928651440).
- [1.18.2 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.18.2-v0.2a-port.2), source `de2d777fa08cc578b1b9969f6a1ad291f7c199ca`; [native build run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37932187607).
- [1.16.5 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.16.5-v0.2a-port.1), source `c481f72c9b809b348df870c3aa2fc93b31b636bc`; [native build and release run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37971853867).
- [1.12.2 preview](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.12.2-v0.2a-legacy-preview.1), source `0dc98abb3a052ae4ac5a05c1d8da1e90759dd14e`; [installed-server build and release run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37975679196).
- [1.6.4 experimental preview](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.6.4-v0.2a-legacy-preview.1), source `8797700ebf50f3216c276f57ed785e81f7d6e4f9`; [installed-server build and release run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37976371723).

## Feature and fallback boundaries

The 1.20.1, 1.19.2, 1.18.2 and 1.16.5 source ports retain cable, reader, display, network item storage, transfer/escrow, ownership and field-guide behavior using their target Forge APIs. Passing automated tests is not a real-client rendering or installed-optional-mod acceptance claim. Not every proposed optional-API fallback is complete.

The two native legacy previews implement craftable full-block cables/export/import nodes, owner-separated loaded physical networks, sided item transport, bounded inventory/provider work, redstone pause, native configuration, saved escrow and buffered-item drops on host removal. The operation budget bounds provider calls across all destinations, not merely moved item counts.

Built-in `/pl4legacy recipes [page]` reads actual registered recipes; `/pl4legacy inspect x y z` and right-click provide owner-checked inspection. 1.12.2 also has native inventory-wrapper alternatives to item capabilities; 1.6.4 implements the era's inventory/sided-inventory contracts directly. These keep the initial subset independent of mandatory recipe-viewer or tooltip mods. They are command/inspection fallbacks, not complete graphical replacements for JEI/NEI/Waila. Native configuration is not a complete scripting replacement.

Legacy advanced displays, multipart geometry, wireless network storage, fluid/energy transport, full scripting and third-party power/provider integrations still need implementation and acceptance. No unavailable energy API receives a guessed conversion ratio. No 1.7.10 dependencies or mandatory performance mods are substituted.

## Toolchain and acceptance corrections

1.16.5: corrected an accidental visible `Inventory` -> `PlayerInventory` API-renaming side effect and replaced an incompatible runtime underline-style call. All 191 adapted native scenarios then passed. These are dedicated-server scenarios, not modern GameTests.

1.12.2: acceptance uses the exact reobfuscated runtime on an installed official Forge server, rather than the development launch path that exhibited a MergeTool/Side conflict. The separate scenario JAR is remapped with the production class hierarchy. The exact-once buffer-drop assertion counts only live entities, excluding discarded objects from earlier fixtures. All 21 scenarios passed. For native reproduction, follow `.github/workflows/legacy112-native.yml`; the initial source README's development `runServer` example is superseded by that installed-server workflow. `bash gradlew clean build` remains the native build command with Java 8.

1.6.4: the reproducible compiler baseline uses the available Forge 9.11.1.960 development API; it is NOT a published 960 installer/universal claim. The exact remapped JAR passed all 21 scenarios on the separately installed, published Forge 9.11.1.1345 runtime and Java 7. Build Gradle runs on Java 21; Java 8 compiles Java 7 bytecode. The legacy installer must execute inside its destination directory. The native-scenarios JAR is separate and never included in the runtime. Native report fields record both build API and actual runtime versions.

## Still open and installation limits

**Further API testing is still required.** Installed optional-provider combinations, actual client visuals, live multiplayer, extended modpack/performance acceptance and the full fallback matrix remain open. The complete multi-version functionality milestone is not finished merely because the six initial native releases exist. No stable or full PL2 parity certification is made.

Use only the runtime JAR matching the Minecraft target, loader and Java requirements. Clients and servers must match. Do not install source or scenario JARs. Back up worlds; modern and legacy world/save formats are not interchangeable. The 1.6.4 preview uses normalized player-name ownership and configurable legacy block IDs (defaults 3500/3501/3502); it is not the modern authenticated-account UUID contract. Keep obsolete runtimes in isolated/trusted test environments; mod scenario tests do not certify legacy platform security for public servers.

GitHub publication is verified for these six ports. This pass did not verify CurseForge publication or moderation for the six new releases; do not infer that from the existing modern 0.2a uploads.
