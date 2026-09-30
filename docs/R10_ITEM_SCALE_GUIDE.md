# Foundations PL4 0.0.1a.R10 — Item scale, monitor polish and guided start

**SOURCE CANDIDATE — no compiled R10 runtime mod JAR is included.**

Minecraft 1.21.1 / NeoForge 21.1.250 / Java 21. Baseline: the exact delivered R9 PL2-Display-Editor source. The supplied screenshots show the user's guide and large-display setup; they are not a claim that this revision has been run in a graphical client here.

## 1. Oversized held and dropped components

The R9 part item JSONs only named a world-model parent. Some of those parents carry legacy transforms; large and normal displays had no ground scale, so their full 16-model-unit surface was reused as a dropped item. Several contexts used large translations intended for an uncentred face-mounted mesh. The placed panel's orientation/position is not an appropriate item origin.

R10 gives all **23 multipart part items** an item-only copy of the pinned mesh and UVs, translated to centre their actual bounds. Each has explicit GUI, ground, fixed, head, first-person right/left and third-person right/left transforms. The paired hand definitions let Minecraft apply its handedness mirror rather than hard-coding another mirror into the geometry.

The fitted maximum extent, after each item-pose rotation, is approximately:

| Context | Maximum model-space extent | Block-equivalent geometric envelope |
|---|---:|---:|
| First-person hands | 5.2 / 16 | 0.325 |
| Third-person hands | 5.0 / 16 | 0.3125 |
| Ground | 4.0 / 16 | 0.25 |
| Item frame / fixed | 8.0 / 16 | 0.5 |
| Head | 5.6 / 16 | 0.35 |
| GUI | 13.0 / 16 | 0.8125 |

These are geometry bounds under item transforms, **not measured percentages of the player's viewport**. Camera FOV, equipment positioning, bobbing, shaders and renderer context still require in-game acceptance. Normal/large panels no longer inherit their unscaled ground pose. Smaller parts use larger scale factors to fit the same compact envelope; a factor above 1 on a four-unit cable node is not a full-block-sized item.

The item generator centres rotation pivots with the mesh and keeps the original UV coordinates and texture references. Panel GUI icons use front lighting and a front-facing pose; other components retain a three-dimensional presentation. The guide, hammer icon, Operator, transceivers, materials and other generated flat items keep their existing item models.

**Placed block models, multipart geometry, collision, connections, panel frame thickness and all 117 existing PNGs are unchanged.** There is no blanket scale applied to the block-entity renderer and no shrinking of installed monitor walls. No new texture is introduced. `tools/r10_item_assets.py` runs last after the earlier resource generators; `docs/R10_ITEM_PRESENTATIONS.json` records the per-item centring and envelope audit.

## 2. Monitor presentation

AUTO_LIST now uses a small inset heading, a fine divider and aligned NAME / AMOUNT columns instead of a raw `name: value items` paragraph at the canvas origin. Item rows no longer print the grammatically incorrect `1 items` suffix. Energy/fluid units remain visible and separate. Existing user labels and colour selections are retained; known internal reader names receive readable automatic headings.

Large displays anchor that automatic table at the **physical upper edge** of their available surface. R9's fixed 248x120 custom canvas was centred in a square screen, which placed automatic text partway down the board. R10 uses the board's available aspect ratio for automatic presentation without resizing or rewriting saved custom elements. Mini displays likewise use their square available surface for automatic viewing. Normal/holographic displays keep the existing logical region.

The automatic table shows as many whole rows as fit, capped at 24. A footer reports `shown / received rows / AUTO LIST`. A shorter panel may show fewer than the previous eight because the heading and footer have reserved space. The full sampled list remains available in Data. This is not automatic scrolling, an unbounded table, or a change to reader sampling limits.

The physical screen fronts and back textures are the retained PL2 assets. This pass intentionally polishes layout and item presentation rather than inventing a replacement frame design or claiming pixel-perfect PL2 equivalence.

## 3. Graphical custom elements and visible edits

The world-space editor now previews **CUSTOM** even when the saved resting view is AUTO_LIST. An old inventory table therefore does not cover the elements being edited. Opening or closing the editor alone changes no saved display mode. An empty custom preview has an explicit HUD hint to use +; it is not a broken empty inventory.

Adding or saving a typed element successfully:

- selects CUSTOM;
- reveals the page assigned to that element;
- preserves all other elements/pages;
- still uses the same server ownership, identity, distance, loaded-state and expected-revision checks.

A stale/rejected edit does not move the page or replace the current layout. The UI describes the current preview separately from the saved view. Leaving an unmodified editor returns to the saved AUTO_LIST or CUSTOM view.

Existing real Item / Block / Inventory / Fluid / Fluid grid / Bar / Text render dispatch is retained. A stone block element still produces a block-picture command plus its separate quantity; an ingot is not silently represented as a block. Layer order remains display surface, picture, quantity, editor controls, with the shallow scale-normalized R9 depths. No depth-test-disable workaround is added.

This revision does **not** prove the native renderer behaves correctly with every item model, shader or mod. The smoke test is to bind an actual stone row, add a Block model element and confirm the block picture replaces the resting automatic list after a successful edit. An untested renderer is not counted as full GSI parity.

## 4. Welcome and tutorials

The Field Guide now has **28 chapters**, retaining every previous chapter ID so existing bookmarks remain meaningful. `start` is now **Welcome to Foundations PL4**. Six sequential walkthroughs follow it:

1. First inventory monitor — chest, Node, cable, named Inventory Reader and compact panel; 17 stone + 5 must become 22.
2. From list to block icon — choose the live stone row, leave Static resource blank, add a block model/count, then try an inventory grid.
3. Your first energy monitor — validate the machine side and Energy Reader first, then build a capacity-backed bar with the correct FE/EU/J unit.
4. Grow a large display — same-plane edge extension, filled rectangle rules, saved layout continuity and reload checks.
5. Hologram walkthrough — connect, choose View, add content and check both sides.
6. Hammer walkthrough — two clear cells above the base, actual two-slot GUI, default stone plates, progress/cooldown and automation.

These are **manual walkthroughs**, not automatically completed quests or proof that native tests passed. Instructions describe the implemented port and its limitations, not legacy features that are still missing. The status and configuration chapters reflect the new automatic table, typed layouts and validation boundary.

The two-page book has a **Welcome** footer shortcut. On the welcome page it becomes **Tutorial >**. In compact mode the same chapters are reached through Contents and the Welcome/tutorial side tab. Home returns to Welcome when the search box is not focused. Returning home clears search/bookmark filters; it does not erase bookmarks.

Existing saved last-chapter preferences are respected, so an upgraded guide can reopen where the user left it. Use Welcome or the first side tab to reach the new onboarding. Fresh preferences start on Welcome.

## 5. Field Guide polish and navigation correction

The Foundations leather/parchment shell remains. Chapter rows now use numbered index markers, restrained parchment-compatible fills, a fine separator and a clear selected accent instead of a wall of identical dark rectangles. Actual component specimen icons use the corrected GUI item presentation.

Selecting another side section clears search/bookmark filters and selects a chapter **in that section**. This fixes the state seen in the supplied screenshot where the left pane displayed Start chapters while the right pane still showed KubeJS. Search can still return chapters from all sections; selecting one aligns the section highlight with its actual category.

The reading-page subheading now names its section. The footer shortcut reserves its own space; the global chapter counter is hidden at narrower two-page widths rather than overlapping it. Existing independent scrollbars, search, bookmarks, native integer-coordinate text, Escape behavior and background-before-book order remain.

The `foundations_pl4:plguide` item, book+sapphire recipe, resource-pack guide path and personal preference file are unchanged. The guide does not require Soil, Framework, Patchouli or a new dependency. `docs/FIELD_GUIDE_R10.md` is exported from the same JSON shipped to the client.

## 6. Preserved behavior / limits

R4 GUI layering; R5 hammer; R6 read-only native energy telemetry; R7 compact multipart connections; R8 monitor growth, mirroring and holograms; R9 typed elements, world editor and authorization are retained. All 35 crafting/forging recipes are unchanged. Energy sampling/transfers, machine capabilities and KubeJS integration are not expanded here. KubeJS remains the documented recipe/data-level compatibility, not a PL4 events/editor API.

Host schema 2, 13 slots and payload protocol 4 remain. Item presentation is client resource data; page reveal occurs on a successful server edit using the existing page field. Use matching R10 client/server builds for consistent behavior. A resource pack overriding the affected item JSONs or guide JSON can override these changes; test the bundled resources first before reporting that the old appearance remains.

This is not the full original GSI system. Screen extraction, nested containers/actions, layout sharing, arbitrary joined shapes, live remote block-entity pictures and universal energy/provider integrations remain unfinished. Source rollback does not restore a world or undo edited layouts.

## 7. Build / install

Extract the complete updater ZIP and run `UPDATE-FoundationsPL4-R9-to-R10.bat`. Select the exact R9 source directory containing `build.gradle` and `gradlew.bat`, not your mods folder or the outer ZIP directory. The launcher can stay in Downloads/Desktop. It verifies affected source/payload hashes, creates checked backups under `.foundations_update_backups`, and offers build/test options. Unrelated local files are left alone; edits to affected files are rejected for deliberate merging.

Choose **3 — Build and all 98 server tests**, with Java 21 JDK and first-build Internet access. After a successful native build the intended runtime is:

```
build/libs/FoundationsPL4-1.21.1-0.0.1a.R10.jar
```

Only the Gradle wrapper JAR is bundled. The native build here stopped before compilation at the Gradle distribution download with `UnknownHostException: services.gradle.org`. Do not install the ZIP, wrapper, source JAR or an older build as R10. The updater does not copy mods into instances, change configs or alter worlds. Test a copied world.

See `VALIDATION.md` for executed checks and `R10_ACCEPTANCE.md` for the remaining client/server acceptance.
