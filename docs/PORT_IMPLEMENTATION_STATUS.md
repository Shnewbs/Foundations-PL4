# New-port implementation and release status

Verified October 9, 2026. This ledger supersedes earlier statements that the six tracks were only planned/scaffolded. Minecraft 1.7.10 remains excluded; 1.6.4 remains experimental.

## Published native builds

| Minecraft / branch | Release version | Forge runtime | Runtime Java | Completed automated acceptance | Scope |
|---|---|---|---|---|---|
| 1.20.1 / `mc/1.20.1` | `0.2a-port.1` | 47.4.26 | 17 | 191 native GameTests passed | Forge alpha feature port |
| 1.19.2 / `mc/1.19.2` | `0.2a-port.1` | 43.5.2 | 17 | 191 native GameTests passed | Forge alpha feature port |
| 1.18.2 / `mc/1.18.2` | `0.2a-port.2` | 40.3.12 | 17 | 193 native GameTests passed | Forge alpha feature port |
| 1.16.5 / `mc/1.16.5` | `0.2a-port.1` | 36.2.42 | 17 REQUIRED | 191 isolated native server scenarios passed | Forge alpha feature port; NOT Java-8-compatible |
| 1.12.2 / `mc/1.12.2` | `0.2a-legacy-preview.1` | 14.23.5.2864 | 8 | 21 isolated native server scenarios passed | Initial legacy item-transport subset; NOT full modern 0.2a parity |
| 1.6.4 / `mc/1.6.4` | `0.2a-legacy-preview.1` | 9.11.1.1345 | 7 | 21 isolated native server scenarios passed | Experimental initial legacy item-transport subset; NOT full modern 0.2a parity |

All six have published GitHub prereleases with native runtime JARs, source JARs, exact source ZIPs, checksums and native test logs. Each legacy preview also passes 38,648 portable production-rule assertions, separate from native scenarios. Downloaded artifacts were checksum-checked. Runtime bytecode was checked as Java 17 for the four newer Forge ports, Java 8 for 1.12.2 and Java 7 for 1.6.4. Runtime JARs do not bundle Minecraft/Forge classes, external core mods or separate legacy scenario mods.

The first three Forge ports were already implemented when this completion pass resumed. This pass fixed/released 1.16.5, implemented both native legacy subsets, repaired their native validation pipelines, and collected verified builds/source/evidence for all six. Scaffolds and renamed modern binaries are not counted as ports.

## Exact release evidence

- [1.20.1 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.20.1-v0.2a-port.1), source `e53c93e895ce49d61597337dab85c8bb08460d3d`; [native run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37928651440).
- [1.19.2 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.19.2-v0.2a-port.1), source `ba21143b159fc9d993b6e404f8f73d1e06f7b639`; [native run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37928651440).
- [1.18.2 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.18.2-v0.2a-port.2), published source `a0140e4f1848fc761742b9e1c7f61c02e08de128`; [later successful native verification](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37932187607) at `de2d777fa08cc578b1b9969f6a1ad291f7c199ca`. The published source and later verification checkout were compared: no differences under `src/`, `build.gradle`, `settings.gradle` or `gradle.properties`. Delivered files retain the published release's original hashes and source archive.
- [1.16.5 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.16.5-v0.2a-port.1), source `c481f72c9b809b348df870c3aa2fc93b31b636bc`; [native build/release](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37971853867).
- [1.12.2 preview](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.12.2-v0.2a-legacy-preview.1), source `0dc98abb3a052ae4ac5a05c1d8da1e90759dd14e`; [installed-server build/release](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37975679196).
- [1.6.4 experimental preview](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.6.4-v0.2a-legacy-preview.1), source `8797700ebf50f3216c276f57ed785e81f7d6e4f9`; [installed-server build/release](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37976371723).

## Features and built-in alternatives

The four newer Forge source ports retain cable, reader, display, network item storage, transfer/escrow, ownership and field-guide behavior with their target APIs. Automated tests are not a client-rendering or installed-optional-mod acceptance claim. Not every planned optional-API fallback is complete.

Both legacy previews implement craftable full-block cables/export/import nodes, owner-separated loaded physical networks, sided item transport, bounded inventory/provider work, redstone pause, native configuration, saved escrow and buffered-item drops on host removal. Each operation has a shared 1024-provider-call limit across destinations.

Built-in `/pl4legacy recipes [page]` reads actual registered recipes; `/pl4legacy inspect x y z` and right-click provide owner-checked inspection. 1.12.2 has native inventory-wrapper alternatives to item capabilities; 1.6.4 directly implements its inventory/sided-inventory contracts. These initial fallbacks need no mandatory viewer/tooltip mod. They are command/inspection alternatives, not full graphical JEI/NEI/Waila replacements. Native configuration is not a complete scripting replacement.

**Legacy features still unfinished:** advanced displays, multipart geometry, wireless network storage, fluid/energy transport, full scripting and third-party power/provider integrations. No unavailable energy API receives guessed conversions. No 1.7.10 dependencies or mandatory optimization mods are substituted.

## Validation corrections and build notes

1.16.5: fixed the accidental visible `Inventory` -> `PlayerInventory` rename and an incompatible runtime underline-style call. All 191 adapted dedicated-server scenarios passed. These are NOT modern GameTests.

1.12.2: the exact reobfuscated runtime is tested on installed official Forge, avoiding a development MergeTool/Side conflict. The separate scenario JAR is remapped with the production class hierarchy. The exact-once buffer-drop assertion counts live entities, excluding discarded earlier fixtures. All 21 scenarios passed. Native reproduction follows `.github/workflows/legacy112-native.yml`; this supersedes the initial source README's development `runServer` example. `bash gradlew clean build` remains the build command with Java 8.

1.6.4: the compiler uses the available Forge 9.11.1.960 development API, NOT a fabricated 960 installer/universal. Its exact remapped JAR passed all 21 scenarios on the published Forge 9.11.1.1345 / Java 7 runtime. Gradle runs on Java 21; Java 8 compiles Java 7 bytecode. The old installer must execute inside its destination directory. The native report records build API and actual runtime separately. Scenario JARs are not production mods.

## Remaining acceptance and installation limits

**Further API testing is still required.** Installed optional-provider combinations, real-client visuals, live multiplayer, extended modpack/performance tests and the full fallback matrix remain open. Six initial native releases do not finish the complete multi-version functionality milestone. No stable or full PL2 parity certification is made.

Install only the runtime JAR matching Minecraft, Forge and Java requirements; clients and servers must match. Do not install source/scenario JARs. Back up worlds; modern and legacy saves are not interchangeable. 1.6.4 uses normalized player-name ownership and configurable block IDs (defaults 3500/3501/3502), not modern authenticated-account UUID ownership. Keep obsolete runtimes in isolated/trusted test environments; mod scenarios do not certify legacy platform security for public servers.

GitHub publication is verified for all six ports. This pass did not verify CurseForge publication or moderation for these six releases; do not infer it from existing modern 0.2a uploads.
