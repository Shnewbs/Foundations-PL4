# R10 acceptance — execute on the actual Minecraft build

These checks are **pending**, not completed results. Use matching R10 client/server builds, Java 21 and a backed-up/copy world. Begin without resource-pack overrides of PL4 models or the guide; then repeat with the intended pack/shaders.

## Build

Run `gradlew.bat --no-daemon --console=plain clean build runGameTestServer`. All 98 registered GameTests must execute and pass. Confirm the mod metadata/runtime filename reports 0.0.1a.R10. No runtime JAR was built in this package environment.

## Held, dropped and framed items

Use one normal screen, large screen, mini screen, normal/advanced hologram, data cable, reader, Node and wireless dish.

- Main hand and off hand: panels should be compact, centred and not consume the bottom-right quarter of the scene as before. Check lowered/raised item animations and different FOVs. Switch dominant hand to check engine mirroring without needing another account.
- Third person: check both arms and the item's orientation while walking, crouching and using it.
- Drop one item, then a stack: the mesh should be an item-sized object (maximum ground envelope about a quarter block), not an entire full-width panel. Repeated stack copies must also stay compact. Check bobbing, shadows, texture face and pickup.
- Put a panel into an item frame, rotate the frame and inspect it from the side. No old eight-model-unit depth translation should remain.
- Inventory and creative GUI: each part should fit its slot, be readable, and show the correct retained texture. The guide, materials and hammer item icon should not unexpectedly shrink.
- Place the same components: in-world sizes, collision and compact reader/panel mounting must be unchanged. A placed 3x3 board must stay 3x3.

## Monitor presentation

Inspect a normal panel, mini panel, square 3x3 Large Display, and a wide/tall rectangle. AUTO_LIST should start near the physical top with an inset, aligned name/amount columns, whole rows and a footer that reports shown/received rows. It must not start halfway down the square board. Very tall boards stop at 24 rows by design. Amounts retain FE/EU/J/mB units, while item rows omit the redundant `items` suffix.

Keep a named reader and custom colour; both must survive the update. Also test an empty reader and an unavailable reader. No invented zero-energy store or table of unrelated items should appear.

## Real graphical-element check

1. Start with a reader-backed chest containing 17 stone and 3 dirt. Confirm reader data first.
2. Open the display editor while View is AUTO_LIST. The custom preview may initially be blank and the HUD should explain it. Close without editing: the automatic table must return.
3. Reopen, add Type Block model, pick the live stone row and leave Static resource blank. Choose page 4 in properties and save. A successful reply must show CUSTOM, reveal page 4 and display a stone block picture/count rather than the automatic table.
4. Add 5 stone: count should become 22. Switch AUTO_LIST/CUSTOM and leave/reopen; custom elements must remain saved.
5. Move the element to another page in properties; the successful edit must reveal that page. A stale property window/rejected save must not move the page or overwrite the newer layout.
6. Repeat with Item, Inventory grid, Fluid and Bar using compatible data. An ingot for Block model should show a diagnostic, not a fake block or inventory list.
7. Check icon/counter/editor layering at normal and edge-on angles, including the intended shader pack. The editor must not be behind quantities; no large depth gap or global depth-disable is acceptable.

## Field Guide

Open an existing guide with saved bookmarks. Welcome is available in the footer; the first tab is Welcome/tutorials. The Welcome page offers Tutorial > in two-page mode. Compact mode uses Contents to reach the same walkthroughs. Confirm 28 chapters, the numbered chapter styling and unchanged Foundations shell.

Select Reference / KubeJS, then click Welcome or the first side tab. The right page must now show a Start chapter, not KubeJS beside a Start-only list. Search across sections, choose a result, clear search, toggle Bookmarks, change categories and return Home. Bookmarks should persist; the filter should clear on a category/home jump.

Check normal and small GUI widths: footer shortcut, page arrows, chapter counter and Done must not overlap. The counter may be hidden on narrower two-page layouts. Test separate list/body scrolling, track clicks, thumb drags, Page Up/Down, Home with/without search focus, Escape, reopen and resource reload. Text/book should stay crisp even when the world is blurred.

Walk through the first chest tutorial and the live block-picture tutorial using only the guide. The checkpoints are manual instructions, not auto-completing quests.

## Retained functionality

Verify hammer input/output/cooldown; compact reader/display attachment; all six flat-screen orientations; hologram facing; large display expansion/old-root removal/save/reload; same-owner edit access; visual-network isolation; FE/EU/J telemetry; existing KubeJS recipe example in the real installed KubeJS environment. No claim that R10 newly integrated an unsupported provider is made.
