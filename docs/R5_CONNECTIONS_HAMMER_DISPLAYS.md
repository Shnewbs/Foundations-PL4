# Foundations PL4 0.0.1a.R5 — Connections, Forging Hammer and Joined Displays

**SOURCE CANDIDATE — no compiled R5 runtime JAR is included.**

Target: Minecraft 1.21.1, NeoForge 21.1.250, Java 21. Starting source: recovered R4 GUI-layer candidate; the portable updater also accepts the recovered R3 TextureFix source. No separate Sonar Core, MCMultiPart, Calculator or Framework JAR is required.

## The reported connection problem

Three code paths contributed to the problem. Host GUI interaction could consume a click before the held PartItem/Operator was invoked. Cable arms were stretched copies of the centre model. Host geometry did not opt into dynamic block-entity-dependent shapes, while the server graph connected adjacent host blocks without checking their actual cable ports.

R5 passes held PL4 parts/tools through to their item interaction, fixes placement orientation when adding a component to an existing host, and uses the original PL2 centre/external/internal/short connector meshes. The server computes six authoritative connection states used by both rendering and collision. No render-frame neighbor scan is used. Model states are precomputed by the renderer.

The supported wiring contract is explicit: a device connects to a compatible cable centre **in the same block**. Cable centres connect across adjacent blocks only when both ports are enabled, both cables are the same family, and neither face is obstructed by another component. Data and redstone cables remain separate. Face devices by themselves are not cable relays. Model-matched collision rejects overlapping new attachments; existing saved parts are not deleted.

The Operator toggles the selected cable arm/port. A side hit on an arm selects that arm's direction, not the clicked polygon normal. A matching editable neighboring cable is toggled on the corresponding port. Sneak-use still removes one component and preserves its saved state/escrow in the drop.

The server now caches a part-level graph, invalidating it on placement/removal/configuration and load changes, rather than rebuilding every sample. Sampling still follows the configured interval. No performance numbers or large-server acceptance are claimed.

### Reconnect an existing R3/R4 setup

1. Sneak-place a Node against a chest or other supported inventory.
2. Click the Node with a Data Cable to place a cable centre in the **same** host block.
3. Extend the cable run. Mount the Inventory Reader and Display onto cable hosts, rather than relying on uncabled devices merely touching.
4. Check the six port arms. An Operator can disable/re-enable ports; a device occupying an external face blocks a through-cable connection on that face.

Existing placements that depended on R3/R4's overly broad adjacent-host graph may need cable centres added. This is an intentional behavior correction, not a save-data reset.

## Forging Hammer

The placeholder table model is replaced by a baked model using the pinned PL2 hammer's 16 boxes, UV offsets, pivots and leg angles. The original 128x64 stone skin is used as an entity-model texture, not stretched over block faces. The head moves through the original 26-model-pixel stroke; cooldown returns it upward. The renderer includes a three-block bounding volume and displays the input/output item on the pedestal.

The machine now owns its base plus two invisible upper structure cells. New placement requires two clear air blocks above. Existing R3/R4 hammers retain their input, output, recipe/progress and cooldown; they acquire the upper cells only when both positions are clear. An obstructed existing hammer pauses and displays a headroom warning instead of overwriting blocks. Removing an upper cell dismantles the base; the upper cells have no item loot of their own. Piston movement is blocked for the structure.

Right-click the base or either upper part to open the inventory. The GUI uses the original 176x143 texture/layout: input at (53,24), output at (107,24), player inventory and hotbar, a 23-pixel progress arrow, status text, and progress/cooldown tooltip. Output insertion is rejected. Shift-click uses server-side container transactions with slot dirty notifications. A menu closes when the player moves out of range or the base is replaced. R4's background-before-content rendering order is preserved.

The six forging recipes, component-aware output checks, inventory/progress persistence and automation restrictions remain: automation inserts input and extracts output. The old hand-insert/hand-extract interaction is replaced by the GUI.

## Connected large displays

Same-owner, same-face, same-plane **rectangles up to 16x16 tiles** form one canvas. A visual top-left controller renders the shared text/bar layout once; the render bounds cover the entire canvas. Original PL2 front/back texture families remove internal frame seams. Clicking a member tile addresses the shared editor while retaining distance and identity checks against the clicked tile. Readers connected to any member's cable network may supply the selected reader.

Joining screens does **not** electrically bridge disconnected cable networks. Each tile keeps its local saved layout; joining chooses the top-left layout, and splitting restores the surviving tiles' local configurations. The join itself is derived/sync-only state, rebuilt from loaded parts. L-shapes, holes, over-limit rectangles, different owners and different planes remain independent displays. Keep the configured top-left tile when extending a screen unless you deliberately want a different saved layout to become the controller.

This is bounded rectangular joining over the existing basic text/bar editor, not the full original GSI editor, item/fluid icon system or arbitrary large-display parity.

## Build and install

Extract the **entire** updater ZIP and run `UPDATE-FoundationsPL4-R3-R4-to-R5.bat`. The folder picker/manual fallback asks for the R3 or R4 **source directory containing build.gradle and gradlew.bat**. The updater may stay in Downloads/Desktop. It validates the exact baseline hashes, backs up changed files under `.foundations_update_backups`, applies only the listed source delta, and offers a local build or build plus server GameTests.

A successful native build should produce:

```text
build/libs/FoundationsPL4-1.21.1-0.0.1a.R5.jar
```

Install that runtime JAR on client/server and remove the older PL4 runtime JAR. Do not install `-sources.jar`, the ZIP, or `gradle-wrapper.jar` as a mod. The updater does not copy anything into a Minecraft instance and does not edit worlds or live server configuration. A build failure retains the updated source and backup; it does not claim a usable new runtime exists.

Use a backup or copy of the world for this candidate. R5 introduces upper hammer block IDs; rolling source files back is not the same as rolling a world back.

## Validation boundary

The offline runner compiled and executed the five dependency-free production classes, with 1,149,451 assertions across port masks, graph partitions, hammer timing/geometry/output limits and rectangular canvases. Java 21 parsing and asset/source-wiring guards passed, as did the existing 11 GUI-layer regression tests. These are **not native Minecraft execution or graphical validation**.

The native Gradle attempt failed at the distribution download with `UnknownHostException: services.gradle.org`. There are no Minecraft/NeoForge dependency caches available here. Full R5 API compilation, packaged runtime startup, all 31 server GameTests (16 retained/updated plus 15 new), graphical acceptance and Windows updater execution remain **not run**. R3's recorded server passes are historical evidence only.

See `VALIDATION.md`, `R5_ACCEPTANCE.md`, `R5_UPSTREAM_CONTRACT.md` and `PARITY.md` for the precise remaining work.
