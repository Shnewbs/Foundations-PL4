# Foundations PL4 R3 validation record

## Executed

- Java 21 compilation against Minecraft 1.21.1 / NeoForge 21.1.250.
- Mod JAR and sources JAR generation.
- Server boot, registry initialization, recipes and data-resource loading in NeoForge GameTestServer.
- Sixteen required GameTests passed on the final source state.
- Packaged-JAR client asset validation passed: 62 root models, 86 model files, 42 referenced sprites. Checks include model parents, inherited texture aliases, PNG decoding and membership in Minecraft 1.21.1's block/item atlas directories.
- The same validator was run against the released R2 JAR and correctly failed on its legacy texture paths. The intentional failure log is included as `validation/r2-texture-regression-expected-failure.log`.
- Regenerating resources from the pinned upstream source reproduced all 86 model files and 114 texture/texture-metadata files exactly.
- Updater verified with PowerShell 7.4.6 on Linux: successful R2-to-R3 application and backup hashes; modified-file rejection without changes; wrong-version rejection without changes; simulated file-write failure followed by complete rollback. An unrelated user file was preserved. Windows BAT execution and the native folder picker were not exercised in this environment.

## Passed tests

1. `hammerConservesItemsAndBlocksFullOutput` — one input produces the exact result; blocked output cannot consume another input.
2. `hammerAutomationCannotExtractInput` — the item capability enforces input/output direction.
3. `persistentEscrowRoundTrip` — item/fluid/energy buffers, identity, owner and links survive NBT serialization.
4. `transferItemsConservesSeventeenDiamonds` — exactly 17 diamonds move from source to destination.
5. `fullDestinationDoesNotExtract` — a full destination leaves the source intact.
6. `restoredEscrowRetriesWithoutReextracting` — an existing buffer is delivered without removing more source items.
7. `readerCountsAndFilters` — allow/exclude filters produce the expected counts.
8. `networkUpdatesLiveDisplay` — a reader reaches a display through the actual ticking network.
9. `upstreamCraftingRecipesLoad` — all 29 original crafting recipe IDs are present in the server recipe manager.
10. `wirelessOwnerLinkCarriesData` — disconnected wired networks share data through a same-owner wireless link.
11. `wirelessRejectsDifferentOwner` — a forged different-owner link does not join networks.
12. `readingUnloadedTargetDoesNotLoadChunk` — reading a distant unloaded target does not load it.
13. `removedNodeItemRetainsAllEscrow` — dropped component item data retains pending resources.
14. `internalForgingRecipesLoadAndMatchTags` — all six internal recipes load and produce their upstream outputs through tag matching.
15. `internalRecipeCodecPreservesCountsAndComponents` — ingredient counts, timing and result components survive JSON codec serialization; invalid zero-cost input is rejected.
16. `changingForgingRecipeResetsProgress` — swapping the ingredient to a different recipe resets progress and produces the correct new output.

The `verifyStandalone` build gate passed. It scans the actual JAR for references to the old Sonar Core/MCMultiPart packages, nested JARs, legacy dependency declarations, and the Foundations PL4 mod identity. All tests ran without Sonar Core or MCMultiPart installed.

These tests require no second Minecraft account. They use simulated owner UUIDs and server-side fixtures.

## Not established

- Client startup/rendering, model appearance, screen text orientation, small-window interaction and all six attachment orientations.
- Full original feature or visual parity.
- Long-running server stability or multiplayer operation.
- Actual server stop/restart recovery. The serialization tests are not a substitute for that lifecycle test.
- Fluid and FE transfer behavior against real modded blocks; only buffer serialization was tested for those resources.
- Complete ownership/packet/protection-mod security review.
- Original worlds or third-party addons loading.

A virtual-display launch was attempted, but the display server failed to create its sockets. No graphical-client pass is claimed. The test log also contains an external Minecraft authentication-key fetch failure caused by unavailable DNS/network access; local GameTests still completed and passed. This is not evidence of an authenticated multiplayer test.

R3 fixes the concrete missing-texture defect shown in the user's screenshot. The earlier file-existence check was insufficient: PNGs existed, but their directories were excluded from the actual client atlas. R3's build check covers that distinction. Texture contents are unchanged from R2; the resource paths changed. Registry IDs and NBT formats are unchanged from R2.

The build environment required an external NeoForm runtime executable-discovery fallback for an empty `ProcessHandle.Info.command()` result. This only supplied the Java executable path to the build tool; it did not change the mod, Minecraft/NeoForge APIs, or test results. That host-specific override is not part of the user's project and is not expected on a normal Windows JDK installation.

## Single-account manual acceptance still required

- Install the built JAR in a separate 1.21.1 / NeoForge 21.1.250 client and server test instance.
- Place every registered component; inspect all attachment directions and inventory models for missing textures.
- Run the README chest/reader/display setup at windowed/full-screen sizes and multiple GUI scales.
- Move exactly 17 diamonds; fill the destination; disconnect/reconnect cables; remove/re-place the transfer node; save, quit and reload.
- Repeat using a real fluid tank and FE storage block.
- Exercise same-dimension and cross-dimension wireless links with target chunks both loaded and unloaded.
- Test filtered items with distinct components/NBT, combined inventories, and more than one reader/network.
- Compare every outstanding row in PARITY.md against the upstream game; passing this checklist alone does not fill missing code.
