# Foundations PL4 0.0.2a.R1 acceptance

R1 begins the first 0.0.2a display/GSI parity phase with configurable text alignment and line wrapping.

## Automated acceptance

- Java 21 clean build and all 114 native GameTests pass.
- Text settings default to left alignment and no wrapping when loaded from legacy NBT or JSON.
- Left/center/right alignment and wrapping survive element NBT persistence, layout JSON encode/decode, canvas resizing and page changes.
- Wrapped lines are clipped to the element's configured height.

## Client acceptance

On a copied world, edit a text element and:

1. Cycle alignment through left, center and right; confirm each change on the live preview and after closing/reopening the editor.
2. Enable wrapping, resize the element to multiple lines, and confirm text wraps at the element width without drawing below its height.
3. Disable wrapping and verify text is clipped to a single line.
4. Save/reload the world and confirm the text content, alignment and wrapping remain unchanged.
5. Repeat on a joined Large Display canvas after expanding and shrinking it.

This revision does not close the outstanding 0.0.1a hologram visual, live item/fluid/FE transfer, escrow restart, or dedicated-server acceptance gates.
