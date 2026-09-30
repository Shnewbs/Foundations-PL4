# R8 in-game acceptance checklist

Status at packaging: NOT RUN in Minecraft. These are expected results, not passed tests. Use a copied world, Java 21, and matching R8 client/server builds. Preserve the original R7 world backup.

## Build and prior behavior

Run `gradlew.bat --no-daemon --console=plain clean build runGameTestServer`. All 73 native GameTests must pass before treating this candidate as validated. Confirm R4 reader/guide blur order, R5 hammer inventory/progress/upper structure and R7 compact reader+panel still work. Compare a charged FE battery and any installed Mekanism/GT battery with its own GUI. Those real-mod checks are separate from generic fixtures.

## Side expansion

Configure a one-tile Large Display with a unique reader name, label, color and text/bar element. Hold another Large Display and click each thin edge in turn; repeat using the outer fifth of its front surface. The new tile must sit in the same plane/front and join without a new perpendicular panel. Repeat on floor, ceiling and all four walls. Expansion to the left or above must retain configured content even when top-left changes. Compare physical outline to drawn panel and connector.

Center-click must consume no item and display a hint. Sneak-place can make independent arrangements. Test an occupied destination, solid block, foreign owner, protected canvas member and target across an unloaded boundary: no replacements, no item loss, no force-loaded chunks. A stored panel with a different old front/layout must adopt the board when explicitly used to extend it. Break the original controller, reload the world and ensure surviving tiles retain the active shared layout.

A complete 2x2 rectangle joins. A three-tile L remains independent; filling its last tile joins. A 16x16 complete screen is the maximum; adding outside that plane's limit must fail. Different owners/fronts/planes remain distinct. Connect two isolated machine networks to two parts of a shared screen and confirm visual joining does not permit cross-network item/energy transfer. Merge two configured canvases and check the documented newer-layout precedence. Split again: shared layout persists, not R7's former hidden local layouts.

Flip a board's flat Front into a matching neighboring front: retain the active board configuration at the new root. Denied neighbor access must reject the flip before overwriting a layout.

## Holograms

Test normal and advanced projectors on all six faces. Floor/ceiling text is upright and projects away from the base. Walk around both sides: text remains readable, never horizontally mirrored, and does not jump to a different base. Edge-on it naturally becomes thin; it is not an omnidirectional billboard. Cycle View on floor/ceiling and verify the base yaw, outline and projection rotate together. The view axis on wall projectors is fixed by the wall and its control is disabled. Rotation into another part must be rejected. Save/reload and test an R7 projector lacking the new field; its data and mounting slot must remain intact.

## Field Guide

Use an existing PL Guide, craft a book + sapphire-tag gem, or run `/give @s foundations_pl4:plguide`. Confirm the new name/item skin and right-click to open. Two-page leather/parchment shell, four icon side tabs, cyan technical section plates, safe center binding, native-sized text and themed scrollbars should be present. No recipe, item ID or external guide dependency changes are required.

Read all 22 chapters. Search by a term in body text (for example 'KubeJS' or 'hologram'), clear the search, bookmark a chapter, use Saved, close/reopen and verify bookmark/last chapter persistence. Drag both scrollbar thumbs and click tracks; scrolling one pane must not move the other. Check a long chapter at top/middle/bottom, native tooltips, Escape while search is focused, Page Up/Down, previous/next, and Done.

Repeat fullscreen/windowed, GUI scales 1/2/3/Auto and narrow viewports. The compact Contents/Read page must remain reachable without fractional glyph scaling. World blur must not blur the book/text after opening or changing tabs. Text and controls must not cross the gutter, footer or clip boundary. Test a resource-pack guide override; malformed/oversized content should show recovery information, not crash. Changes are visible after reopening the guide following resource reload.

## Report regressions

Include the R8 build output and first error from `.foundations_build_logs`, or a client `latest.log` plus screenshot and exact mounting/front/View settings for visual problems. A passed offline parser or rule suite is not evidence that this checklist passed.
