# Foundations PL4 feature parity status

**Requested outcome: stable 1:1 port. Current outcome: R16 transfer-endpoint hardening on the `0.0.1a` freeze candidate; incomplete historical parity and native acceptance pending.**

Registering a block/item name, retaining a texture, or passing compilation does not establish gameplay or visual parity. The table distinguishes implemented basics from the outstanding original behavior. The primary comparison is PL2 3.0.8; this does not claim that every historical PL1 branch or unfinished PL3 subsystem has been reproduced.

| Area | Present in R16 source (native acceptance pending) | Outstanding parity / verification |
|---|---|---|
| Platform | Java 21, Minecraft 1.21.1, NeoForge 21.1.250 source target | R16 native compilation, all 114 server tests, dedicated soak, graphical startup and runtime-JAR installation |
| Content registration | Original component IDs, sapphire ore, hammer, items; internal hammer_air and model carriers | Registry acceptance in a native R14 build; registered names do not establish full behavior |
| Crafting | 29 explicit PL2 crafting recipes plus six internal forging recipes retained from R3/R4; common tags | R13 recipe/GameTest execution, JEI hammer category and full external recipe integration |
| Hammer | Original 16-piece model geometry/skin, animated head, three-block ownership, native two-slot GUI/shift-click, automation, saved inventory/progress, safe headroom upgrade | Native gameplay and visual acceptance; JEI/external recipe-viewer integration; no assertion of pixel-perfect runtime parity |
| Sapphire | Original texture, drops/fortune/silk-touch data, Overworld placement at Y 1–29 | Original randomized per-chunk vein sizes; XP behavior; full original configurable ore settings |
| Item presentation | 23 centred item-only part meshes; eight bounded item transforms each; no placed-model/UV/texture changes | Native first/third-person, GUI, dropped stacks, frames, FOV, handedness and shader acceptance |
| Multipart blocks | Separate ordinary/display face slots, original paired-reader skin and collision, held-item placement, covered-reader access, kind/face save reindexing, dynamic endpoint leads | Original placement exceptions, edge/corner/cover/provider rules, rotation tools, external multipart API parity and native collision tests |
| Wired networks | Cached production planner; typed reader NETWORK input/VISUAL output, compatible exposed rear endpoints, explicit visual-only remote telemetry, port/family/obstruction checks | Complete original reader/channel/subnetwork semantics, external providers, native load/unload tests and large-network performance acceptance |
| Inventory Reader | Component-aware bounded item pictures with independent total quantities; filters/sorting/data modes retained | Native variants/custom models and inventory deduplication acceptance; full historical channel/mode UI; oversized visual fallback may merge variants |
| Fluid Reader | Bounded typed fluid pictures, nonempty per-fluid aggregate capacity and native still/tint graphical renderer; storage rows retained | Native modded fluids/providers and per-tank/channel fidelity; screen extraction controls not implemented |
| Energy Reader | FE plus optional read-only Mekanism strict J and GTCEu energy info/container EU; unit selection, per-unit totals, same-block deduplication, sided diagnostics | Actual installed-mod acceptance; generation/consumption rates; exact very-large integer row values; equivalence across distinct multiblock casing positions; native AE2/IC2 and other historical providers |
| Info Reader | Coordinates, redstone, light, weather/time, hardness, numeric block properties, crop growth, furnace progress, entity health/armor/food/XP/speed | Complete upstream reflection registry and selectable four-field/progress bindings; every vanilla and mod provider |
| Network Reader | Basic host/target counts | Original network details/channels/debug information |
| Transfer Nodes | PASSIVE/ADD/REMOVE/ADD-REMOVE, priority, item/fluid/FE switches and filters; explicit Transfer peers plus PL2-style passive normal-Node endpoints; simulate-before-extract, persistent driver escrow and same-cycle re-extraction fence | Historical directional channel/filter GUI, deeper fairness/saturation testing, live third-party fluid/FE acceptance, cross-dimension transfer test, restart/crash stress tests; EU/J remain telemetry-only |
| Wireless emitters/receivers | Explicit same-owner links, graph joining, loaded-world resolution, configuration switches | Original emitter discovery/selection GUI, permissions/public sharing semantics, live cross-dimension acceptance |
| Array | Eight saved block/entity links, remote observation | Original eight transceiver inventory slots, card removal lifecycle, original GUI |
| Entity Node / transceiver | UUID binding to living entities; basic entity info; local count fallback | Original entity selection/scanning UI and item/entity inventory interactions |
| Wireless Storage | Bound-target read-only snapshot | Live subscriptions, original interactive remote item/fluid/inventory management and GUI |
| Redstone | Basic network signal propagation, single numeric comparison signaller, sampled clock pulse | Full statement lists/AND/OR logic, original clock editor/hand animation, per-face output fidelity and feedback-loop tests |
| Mini/normal displays | Explicit front models and typed item/block/grid/fluid/text/bar paths, separate AUTO_LIST/CUSTOM, optional quantities/names, eight pages; physical-top-aligned automatic name/amount table capped at 24 rows | Native visual/interactive acceptance, all original elements/styles/actions, live remote block-entity renderers |
| Large connected displays | Same-owner/plane/front rectangles <=16x16; edge/rim extension; permission preflight; deterministic layout revisions mirrored across tiles through growth, root removal and reload | Native placement/visual/cross-chunk acceptance; arbitrary shapes and full GSI. Splits keep shared layout rather than hidden local tile settings |
| Holographic displays | Separate mounting/projector/text axes; upright floor/ceiling text; camera-side readable planar back view; four floor/ceiling View directions; R15 projects normal/advanced canvases 0.90/1.20 blocks clear of the mounting surface | Native graphics and placement acceptance; complete original advanced/volumetric geometry and GSI features; no joined hologram canvas |
| GSI display editor | Left-edge world-space controls, captured-MVP pointer projection, selection/move/resize/snap, properties/data picker, duplicate/delete/order and revisioned server transactions; CUSTOM editor preview independent of saved view; successful add/edit reveals element page | Full PL2 GSI containers/actions/links/extraction/export/sharing; original pixel-perfect UI; native rendering, hit mapping and shader acceptance |
| Guide/languages | Foundations book retained, schematic tabs, unobtrusive chapter summaries, single search hint, Bookmarks filter, whole-line clipping, numbered parchment chapter rows, corrected section navigation, Welcome and six tutorials within 28 current chapters | Native GUI-scale/window/readability acceptance; chapter translations and complete interactive recipe illustrations |
| Operator | Configure devices, remove parts, direction-aware cable port toggle with persistent mask | Original additional modes/overlays/configuration cloning and full rotation behavior |
| Mod integration | General item/fluid/FE plus optional Mekanism J / GTCEu EU reader bridges | Live mod versions unverified; other AE2/Calculator/IC2/JEI and historical provider semantics remain outstanding; native EU/J transfer not added |
| KubeJS | Existing JSON forging codec, ordinary recipe/tag data and opt-in custom recipe example | Native KubeJS runtime/reload tests; no PL4 plugin, builder DSL, event group, scriptable network/display API or custom provider registration |
| PL3 additions | Source was inspected and pinned as a reference | ImGui/node graph, data-address/method graph architecture, PL3-specific editor features; no claim of a completed PL3 port |
| Existing saves | Schema2/13 slots retained; additive typed element/mode/page state; legacy text/bar migration; protocol4 retained; use matching R16 on both ends | Native save/reload acceptance; R8 and earlier cannot preserve typed layouts; exact matching world backup required for downgrade |

## Required work before a stable 1:1 designation

1. Lock a detailed behavioral contract against the actual released PL2 build and identify historical PL1-only behavior that must be retained; do not treat commented-out registrations as released functionality.
2. Complete original multipart/subnetwork rules and run native acceptance of the R5 connections, hammer structure, animation and UI.
3. Port the complete GSI element/editor/action system and complete/verify connected-display behavior beyond the bounded R5 rectangle implementation.
4. Restore the original reader modes, channel UI, transceiver/card inventories, and remote inventory interactions.
5. Restore or explicitly map the original integration providers to available 1.21.1 APIs. Generic FE/item/fluid support is not a substitute for those providers.
6. Complete automated fluid/energy transfer, failed-insertion, chunk unload/reload, cross-dimension, server restart, packet authorization, and performance stress tests.
7. Run graphical checks at multiple GUI scales/resolutions and perform single-account gameplay acceptance on a client and dedicated server.


## R14 editor usability follow-up (source-only)

R14 moves contextual editor help/keycaps off the physical monitor and into the editing player's HUD while retaining the world-space side toolbar. Right mouse now acts as Back through editor/property/picker/palette screens. Inventory/fluid grids default to three columns; Columns remains a real renderer input and gains a mini preview. A 24-swatch PL4 palette fills the existing hex color field without changing persisted color format. The R13 four-corner resize, NBT stackability, technical-binder guide, dynamic canvas, and earlier fixes are retained. Offline production/source/resource checks pass; native API compilation, graphical acceptance, Windows updater execution and all 106 GameTests remain pending.


## R16 Transfer Node endpoint hardening (source-only)

R16 restores ordinary Nodes as passive resource endpoints for the bounded transfer engine. REMOVE / EXPORT nodes prefer explicit ADD peers and can then export into normal Nodes; ADD / IMPORT nodes can pull from explicit REMOVE peers or normal Nodes. Normal Node to normal Node never moves resources by itself. ADD / REMOVE is explicit-peer only until PL2 directional channel/filter semantics are restored, preventing blind passive-network ping-pong. Simulate-before-extract and persistent driver escrow remain; a per-cycle receive fence blocks immediate re-extraction. Eight native item-path GameTests raise the registered total to 114. Items, compatible fluids and FE use the same routing rules; native EU/J remain read-only telemetry. Native API compilation, live third-party fluid/FE acceptance and all GameTests remain freeze gates.

## R15 final alpha polish (source-only)

R15 moves the normal and Advanced hologram planes farther away from their projector/cable hardware while retaining the fixed two-sided readable coordinate frames. The six onboarding tutorials are rewritten into a more natural teaching voice while preserving deterministic checks such as 17/22 stone. No save, multipart slot, packet, recipe or network format changes are introduced. Native graphical acceptance and all 106 GameTests remain the freeze gates.

## R13 final alpha stabilization (source-only)

R13 separates editor-button background/border/glyph/hover planes after the user confirmed R12 fixed the bar but not toolbar shimmer. It adds four large corner resize hit targets rendered as cyan brackets, contextual colored keycaps, stackable ordinary part drops with canonical Operator-saved configuration, and the requested Calculator-style technical-binder Field Guide presentation while keeping the established guide footprint and PL4 graphite/cyan palette. Three native NBT GameTests raise the registered total to 106. Offline production/source/resource checks pass; native compilation, framebuffer acceptance, live NBT stacking, Windows updater execution and all GameTests remain pending.

## R10 item presentation, guide onboarding and view polish (source-only)

Item-only meshes/transforms are normalised without shrinking placed blocks. Automatic tables use the full board's usable aspect ratio; custom editor preview is separate from the resting view, and successful element edits reveal their page. Welcome, six tutorial chapters and category/navigation/row polish keep the Foundations guide identity. Pure production rules, resource-envelope tests and deliberately reintroduced regressions pass. Native API compilation, all 98 GameTests, graphics, real integration and Windows execution remain unverified. No full historical visual/GSI parity row is closed by these source checks.

## R9 PL2-style typed displays and editor (source-only)

Distinct item/block/inventory/fluid/grid/bar/text elements, shallow counter/editor layering, on-world selection/drag/resize and revision-fenced edits are implemented. The Field Guide is cleaned up and updated for R9. Executable pure render-plan/transaction/picking tests passed; this is not native graphics. All93 native GameTests, full API compilation, real integrations and Windows updater execution remain unverified. The full original GSI parity rows stay open.

## R8 display continuity, holograms and Field Guide (source-only)

Same-plane smart panel extension, persisted shared-layout mirroring and independent readable hologram frames are implemented. The old PL2 guide screen is replaced by a re-authored Foundations-style Field Guide documenting the actual port. Five new pure production classes have 2,750 checks; three reintroduced regressions are rejected. All 73 native GameTests, Minecraft API compilation, client graphics, real-mod integration and Windows updater execution remain unverified. R8 does not close the full historical multipart/GSI parity rows.

## R7 compact reader/display connections (source-only)

Restores compact flat-panel mounting and typed visual output in the standalone host. Exposed endpoints no longer universally require a same-block cable centre. Same-host centre precedence, obstruction rules and remaining historical differences are explicit in R7_MULTIPART_CONNECTIONS.md and R7_UPSTREAM_CONTRACT.md. New pure production tests pass; all 58 native GameTests and graphical/API acceptance remain unexecuted. Original PNGs/recipes and R6 energy/KubeJS code remain unchanged. Do not downgrade an R7 world to R6 without its matching world backup.

## R6 facing and native energy telemetry (source-only)

Adds explicit display-front control, corrected shared font/model/layout axes, same-front rectangle grouping, optional read-only native J/EU capability bridges with unit-separated totals, and an opt-in KubeJS recipe example. Preserves old R5 front until the user explicitly flips it. 3,275 additional pure production/accessor assertions and oriented-model guards pass, alongside retained offline checks. Native API compilation, all 35 GameTests, graphical checks, real Mekanism/GTCEu, KubeJS runtime and Windows updater remain unexecuted. See R6_DISPLAY_ENERGY_KUBEJS.md, KUBEJS.md and VALIDATION.md. No full-parity claim is made.

## R5 connections, hammer and rectangles (source-only)

R5 implements the reported paths in source, preserving the R4 GUI-layer correction. Five dependency-free production rule groups, Java syntax parsing, resource/source guards and eleven GUI-layer tests pass offline. Native API compilation, all 31 GameTests, graphics and Windows launcher execution are pending; the Gradle download failed before compilation. No R5 runtime JAR is included. See R5_CONNECTIONS_HAMMER_DISPLAYS.md for the explicit wiring and rectangle limits.

## R4 GUI layer correction (source-only)

The component screen and guide now draw content after the inherited native background pass and before widgets. Row tooltips are drawn after widgets and cleared per frame. A JDK parser guard passes 11 checks and rejects the actual R3 pattern. Full mod compilation, server tests and in-game rendering were not run for R4 because the build environment could not download Gradle. No full-parity feature row is closed by this source-only change. R3 resources, registry IDs, server logic and save formats are unchanged.

## R3 texture loading correction

Moved the original block, model and item textures into the directories stitched by the Minecraft 1.21.1 block/item atlas. Model references and the upstream resource converter now use those paths. Texture pixels, registry IDs and saved-data formats are unchanged from R2. A build gate checks all packaged model roots and texture references, and detects the exact R2 missing-texture regression. Client visual parity and the remaining features below are still outstanding.

## R2 core consolidation

Foundations PL4 now owns its registration, part state, network sync, capabilities, and forging recipe service in one runtime JAR. Sonar Core source was pulled and all 123 explicit imported symbols in PL2 were located; the 124th import is a wildcard package import. The source audit is in `SONAR_CORE_AUDIT.json`. The absence of an external Core dependency does not complete the still-missing PL2 GUI/GSI/provider features above.

None of the outstanding rows has been silently counted as complete. This file is the acceptance ledger for subsequent work.
