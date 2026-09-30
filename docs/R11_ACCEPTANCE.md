# Foundations PL4 R11 — In-game acceptance checklist

Use a **copied/backed-up world** and matching R11 client/server builds.

## 1. Existing R10 board migration
1. Open a copied R10 world containing a configured joined Large Display.
2. Record the element's approximate relative location/size before upgrade.
3. Start R11 and let the board load.
4. Confirm the element remains in the same relative visual region and the board is not reduced to a narrow 248x120 island.
5. Save/quit/reload and confirm it does not migrate again or drift.

## 2. Wide-board workspace
1. Build a valid 2x2 Large Display and enter editor mode.
2. Extend it horizontally to 6x2.
3. Confirm the editable workspace grows across the new tiles instead of stretching the old surface.
4. Drag an element into the far-right third; close/reopen editor and verify its position.

## 3. Tall board
Build 2x6 and repeat the move/resize test near the lower area. Confirm cursor mapping remains correct at different viewing angles.

## 4. Toolbar placement
Confirm + / E / X / C / forward / backward / snap / settings are on the **visible left edge of the screen**, not outside beside cables or reader blocks. Verify clicking the controls does not select/remove neighboring multipart pieces.

## 5. Resize and bounds
Move an element to every edge and resize it. It must remain inside the joined canvas. Test snap on/off.

## 6. AUTO_LIST vs CUSTOM
AUTO_LIST should use the joined surface cleanly. Entering editor previews CUSTOM. Saving a graphical item/block/fluid/bar should not fall back to inventory-list presentation.

## 7. Regression checks
- Reader + attached display compact mounting.
- Large-screen edge expansion.
- Hologram orientation.
- Item/block/fluid pictures and quantity layering.
- Hammer GUI and animation.
- Field Guide Welcome/tutorial pages.
- Held and dropped item scale.

## Pass condition
R11 passes graphical acceptance when joined boards use their full physical surface, legacy layouts migrate once without visual jumps, editor picking matches the cursor across the board, controls stay on-screen, save/reload is stable, and no R4-R10 regression appears.
