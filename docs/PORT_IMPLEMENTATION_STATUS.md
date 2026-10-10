# New-port implementation and release status

**Historical verification snapshot, not the current release ledger.** This October 9 report documents the original `preview.2` and `0.2a-port.1` runs. Later [legacy `preview.3`](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.12.2-v0.2a-legacy-preview.3), 1.16.5 `port.2` and other exact-target tags are reflected in the [October 10 master roadmap](../FOUNDATIONS_PL4_MASTER_STATUS_PARITY_ROADMAP.md) and [17-target publisher receipts](CURSEFORGE_MULTIVERSION_STATUS.md). Keep the detailed test evidence below, but do not interpret its old versions or Java requirements as today's complete release inventory. Client/installed-API/PL2 parity acceptance remains separate.

Updated October 9, 2026. Minecraft 1.7.10 remains excluded; 1.6.4 remains experimental. The latest completion pass adds real fluid transport to both legacy previews. It does not certify full modern PL4 parity or every optional API.

## Published native builds

| Minecraft / branch | Release version | Forge runtime | Runtime Java | Automated acceptance | Scope |
|---|---|---|---|---|---|
| 1.20.1 / `mc/1.20.1` | `0.2a-port.1` | 47.4.26 | 17 | 191 native GameTests | Existing Forge alpha feature port; unchanged by fluid pass |
| 1.19.2 / `mc/1.19.2` | `0.2a-port.1` | 43.5.2 | 17 | 191 native GameTests | Existing Forge alpha feature port; unchanged by fluid pass |
| 1.18.2 / `mc/1.18.2` | `0.2a-port.2` | 40.3.12 | 17 | 193 native GameTests | Existing Forge alpha feature port; unchanged by fluid pass |
| 1.16.5 / `mc/1.16.5` | `0.2a-port.1` | 36.2.42 | 17 REQUIRED | 191 isolated native server scenarios | Existing Forge alpha feature port; NOT Java-8-compatible |
| 1.12.2 / `mc/1.12.2` | `0.2a-legacy-preview.2` | 14.23.5.2864 | 8 | 52 isolated native server scenarios | Item/fluid transport subset; NOT full modern parity |
| 1.6.4 / `mc/1.6.4` | `0.2a-legacy-preview.2` | 9.11.1.1345 | 7 | 52 isolated native server scenarios | Experimental item/fluid transport subset; NOT full modern parity |

Both new legacy runtimes are published on GitHub with exact sources and checksums. Each passes 50,727 portable fluid-rule assertions plus the previous 38,648 portable item/topology assertions: 89,375 portable assertions per target. Portable checks and native scenarios are distinct; no modern GameTest execution is claimed for legacy versions. Runtime JARs exclude separate test fixtures and do not bundle Minecraft/Forge classes, external core mods or optimization mods.

## New legacy functionality and built-in alternatives

Both legacy versions now include craftable fluid exporters/importers, a built-in 16,000 mB tank, vanilla water/lava bucket interaction and sealed fluid recovery cells. The tank and existing command recipe/inspection paths work without external tank, recipe-viewer or tooltip mods. Recipe commands read actual registered recipes, including the new tank and fluid-node recipes.

The defaults are 250 mB per exporter every 10 ticks. Native configuration controls fluid enablement and rate; global transport enablement, physical network limits, owner separation for PL4 tanks, redstone pause, loaded targets and deduplication remain enforced. Each fluid operation has a shared 64-call budget. Tanks are storage endpoints, not network bridges. Existing item transport retains its separate 1024-provider-call budget.

The fluid layer uses the actual sided Forge fluid capability on 1.12.2 and the native legacy fluid handler on 1.6.4. Declined sides do not fall back to unrestricted access. This is a native API implementation tested with dedicated fixtures, not certification of installed third-party mod combinations.

Fluid identity/NBT stays distinct. Actual provider calls are marked in flight before execution; invalid or throwing execute calls quarantine uncertain state instead of automatically replaying it. Healthy recovery cells can restore fluid, including partial restores. Quarantined cells cannot automatically restore uncertain contents. Unknown/unavailable fluid registrations retain their opaque NBT through serialization and removal. This does not promise atomic crash commits across independently saved external tanks or safety against providers that mutate during simulation.

The earlier item fallback remains: sided inventory/capability adapters, craftable full-block hosts, owner-separated loaded networks, saved item escrow, `/pl4legacy recipes [page]`, `/pl4legacy inspect x y z`, and right-click inspection. These are command/inspection alternatives, not complete graphical JEI/NEI/Waila replacements; native configuration is not a full scripting replacement.

## Current legacy release and verification evidence

- [1.12.2 preview 2](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.12.2-v0.2a-legacy-preview.2), runtime source `243d3838c3ed24bac7d317afb6d940f7c5687da4`; [release run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37993991425). Runtime SHA256: `f0e00f4f9d237d22d0b9b9f6784a5bd6bba7f8e091380c34695803476f5b4ea3`.
- [1.6.4 experimental preview 2](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.6.4-v0.2a-legacy-preview.2), runtime source `17d6aea333fa4ab992cc962602d2f14f3534d519`; [release run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37993712940). Runtime SHA256: `99a07a4839d889d72c070172eb64f5d8f2cdf8272629cc9611860006c3188624`.
- [52-scenario verification of both published runtimes](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37994773456): 1.12.2 verification source `450e9cc858efa84e9d8c01e5d05edf0f14a6f8a3`; 1.6.4 verification source `551a43bc92fe15babcac0a2e8eb58bb52d7f3e27`. Both used the checksum-verified published runtime, passed 52/52 scenarios, passed the fixture cleanup/save guard, and had rebuilt runtime archive entries identical to the published JAR. Supplemental logs, reports, test-only diffs and source snapshots are attached under distinct `validation-...` filenames. Original tags/assets were not overwritten.

The first preview-2 run passed 51 scenarios but left an unregistered test-provider tank in the disposable world at shutdown. That fixture save error was corrected by a final cleanup scenario, and the validator now rejects missing-mapping/save errors. The correction changes tests/tools/documentation, not the published runtime. Original logs remain historical evidence; use the supplemental 52-scenario reports for the corrected harness. The 1.12.2 Forge loader still prints its own missing-signature diagnostic; it is not concealed or reclassified as a PL4 test failure.

Current reproduction is `bash tools/validate_legacy_fluids.sh` on the appropriate branch in a disposable workspace. Dedicated branch workflows now call those scripts, preserve results and refuse to overwrite existing published runtime source with different production code. The one-time fluid-source application workflow was retired after publication; active validation is on the target branches and in `legacy-fluid-recheck.yml`.

## Previous newer-Forge release evidence

These release results are retained from the previous port pass; they were not rerun as new feature ports during the legacy-fluid update.

- [1.20.1 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.20.1-v0.2a-port.1), source `e53c93e895ce49d61597337dab85c8bb08460d3d`; [native run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37928651440).
- [1.19.2 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.19.2-v0.2a-port.1), source `ba21143b159fc9d993b6e404f8f73d1e06f7b639`; [native run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37928651440).
- [1.18.2 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.18.2-v0.2a-port.2), published source `a0140e4f1848fc761742b9e1c7f61c02e08de128`; [successful later verification](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37932187607) at `de2d777fa08cc578b1b9969f6a1ad291f7c199ca`, with no differences under `src/`, `build.gradle`, `settings.gradle` or `gradle.properties`.
- [1.16.5 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.16.5-v0.2a-port.1), source `c481f72c9b809b348df870c3aa2fc93b31b636bc`; [native build/release](https://github.com/Shnewbs/Foundations-PL4/actions/runs/37971853867).

The four newer Forge ports retain the cable, reader, display, network item storage, transfer/escrow, ownership and field-guide behavior with target API adaptations. Automated evidence remains separate from real-client and installed-optional-mod acceptance.

## Build, installation and remaining work

1.12.2 builds/runs on Java 8 against Forge 14.23.5.2864. Its reobfuscated runtime is exercised on installed Forge, avoiding the original development MergeTool/Side conflict. The separate test fixture is reobfuscated with the production class hierarchy.

1.6.4 builds with Java 21 / Gradle 9.5.0 / ForgeGradle 7.0.28, using a Java 8 compiler to emit Java 7 bytecode. Its development API is Forge 9.11.1.960; its tested production runtime is the published Forge 9.11.1.1345 / Java 7. Do not assume a 960 installer exists or confuse the build JDK with the runtime. The released runtime is the SRG-remapped artifact, renamed for installation without changing its bytes.

Install only the runtime JAR matching the Minecraft target; clients and servers must match. Never install source/scenario JARs. Back up worlds and do not downgrade fluid-containing saves. Legacy schema 2 preserves the earlier item fields and adds fluid data, but is not a migration path for modern worlds. On 1.6.4 the default block IDs are 3500 through 3505 and ownership uses normalized player names, not modern authenticated-account UUIDs. Keep obsolete environments isolated or limited to trusted testing; these mod tests do not certify public-server platform security.

**Still unfinished on the legacy tracks:** advanced displays and graphical tank gauges, multipart geometry, wireless network storage, energy transport/adapters, full scripting and installed third-party power/provider integrations. No missing energy API receives guessed conversions. Preliminary full-block models use vanilla textures.

**Still open across targets:** installed optional-provider combinations, real-client visuals, live multiplayer, sustained modpack/performance acceptance and the complete optional-API fallback matrix. The complete multi-version functionality milestone and full PL2 parity are not finished. **Further API testing is still required.**

GitHub publication is verified for the new legacy previews. CurseForge publication/moderation for these new previews was not performed or verified in this pass; existing modern uploads do not imply it.
