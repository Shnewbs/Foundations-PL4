# Foundations PL4 0.0.1a.R14 — editor UX follow-up

## HUD-only contextual help
The world-space help panel added in R13 is removed from the monitor plane. `DisplayEditorScreen` now draws the contextual line and colored keycaps in the player's 2D GUI only when the local editor is editable. The monitor toolbar remains world-space and visible for the duration of edit mode.

## Right-click Back
Right mouse acts as Back through the display editor stack: editor -> gameplay; element properties -> editor; reader/data picker -> properties; color palette -> properties. In `PartScreen`, right mouse returns Settings to Data and then closes from Data.

## Columns
Inventory and fluid grids default to three columns. `Spec.columns` still clamps to 1..16 and is consumed by the renderer; R14 adds a mini preview next to the field so changing it visibly changes the layout expectation before saving.

## Colour palette
Hex entry remains authoritative. The Palette button opens a visual set of 24 deterministic swatches plus the PL4 default and a large current-colour preview. Choosing a colour fills the existing hex field; no new persisted colour format is introduced.

## Retained work
R13 four-corner resize, editor depth separation, canonical item-drop NBT, R12 bar-plane fix, R11 dynamic canvas, R10 item transforms/tutorials, and earlier connection/hammer/energy work are unchanged.
