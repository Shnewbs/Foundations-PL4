# R9 native acceptance checklist — not executed here

Use Java 21 JDK, a copied world and matching R9 client/server builds. Full native compilation and graphical tests must pass before treating this candidate as an accepted release.

## Build

Run `gradlew.bat --no-daemon --console=plain clean build runGameTestServer`. Expect 93 native GameTests registered. None were executed in the packaging environment. The output should be `build/libs/FoundationsPL4-1.21.1-0.0.1a.R9.jar` only after successful completion. Keep `.foundations_build_logs` and the game's log if it fails. Do not install a source ZIP, a sources JAR or gradle-wrapper.jar as a mod.

## Visible type selection (first acceptance gate)

Connect a chest with stone64+33, a diamond, an ingot and two differently named/dyed items to a Node and Inventory Reader; mount a display on the reader. Open reader Data first. Place the same graph on all six mount faces, plus a large rectangle and each hologram type.

Right-click the display, +, Type Block model, Pick stone, Add. Expect an actual block picture and97, not an inventory list. Select an ingot for Block: expect an incompatibility diagnostic, not a block or a silently unchanged list. Change the element to Item: expect the ingot item model. Try a modded block and a component/custom-model item; inspect glint/tint and clipping. Native model invocation is implemented but pack-specific renderers are not validated here.

Add Inventory grid with at least two columns; change rows/columns/offset, enable names/quantities, and move/resize it. Actual variants must not collapse into one base picture except documented oversized-metadata fallbacks. Static `minecraft:stone` is a sample1, not live storage. An explicit exact variant key must survive save/reload. A numeric STORAGE row cannot become an item picture.

## Mode, page and edit lifecycle

Add/edit must enter CUSTOM. Switch View to AUTO_LIST: the legacy list appears by explicit choice. Switch back: the typed layout survives. Delete the last custom element: blank screen, not the list. Use eight pages, duplicate/reorder/remove, drag/move and corner/resize, snap on/off. Expected screen coordinates follow the camera projection with GUI scales, aspect ratios and all supported mounts. Moving a big canvas/hologram must not invert dragging. Inspect all controls from ordinary viewing distance; their frustum bounds include the left toolbar.

Close with Escape immediately after save or source inspection. A delayed reply must not reopen the screen. Removing the clicked tile, unloading it or moving out of range must end editing. Existing R8 layouts migrate; a bare new panel remains AUTO_LIST until an element is added.

## Counters, editor controls and rendering state

Stand head-on, very close, far away and almost edge-on. Expect surface, icon, counter and editor layers in that order. Counter text must not bury left controls or selection handles. A16x16 canvas must not have16times the layer depth. Overlapping element ordering is within icon/text categories; counters remain above icons. Confirm no GUI-like .03-block float, excessive shadows, doubled text, z-fighting, backwards glyphs, missing textures or leaked render state after closing. Test with/without installed shader/render mods. There is no graphical pass claimed until this is observed.

## Fluid and energy

Use real fluid tanks and a Fluid Reader. A water500/1000 sample should be half-filled with native water sprite/tint. Combine same-fluid tanks and check summed nonempty tank capacity; STORAGE includes empty capacity. Test lava and one modded tinted fluid. Fluid grid displays samples/amounts, not an invented extraction inventory. A graphic bar uses a numeric row with real capacity; missing capacity must not imply full. Select FE/J/EU readings from the existing Energy Reader and confirm distinct units and no conversion. Real optional providers remain an independent pack-compatibility test.

## Ownership / stale changes (single-account compatible)

Run the native FakePlayer fixtures: they cover owner/non-owner, stale revision, replaced/distant anchor, invisible source injection, migration and component snapshots without a second Minecraft account. For manual conflict testing, open a property form, change the canvas through a server/test action, then submit the stale form. It must reject and show the current layout, not overwrite. No source outside the existing visual network may be bound by guessing a UUID. Protection checks apply across joined tiles.

## Existing features / continuity

Grow a configured large rectangle left/up using R8 edge expansion; typed elements, mode and page must follow. Remove the old controller, split, save/reload and rejoin. Splits retain the shared layout as R8 documents. Confirm compact reader/panel mounting, disabled cable ports, separate visual/machine networks, hologram viewing controls, hammer GUI/progress/automation, energy readers and recipe-level KubeJS examples remain intact. Run in integrated and dedicated server modes.

## Field Guide

Open the existing `foundations_pl4:plguide`. Check the two-page shell and native text at windowed/fullscreen GUI scales. Four tab glyphs must be legible/centered. Hover chapter entries: summary below list, no huge tooltip over the right page. Search must show one unshadowed empty hint; Bookmarks highlights its filter. Verify no partial header/body line at viewport boundaries, scrolling/drags/track clicks, compact Contents/Read, keyboard focus, Escape, preferences and resource reload/recovery. The book's content must match actual accepted R9 behavior; report discrepancies instead of hiding them.
