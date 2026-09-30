# Foundations PL4 0.0.2a.R1 acceptance

R1 begins the first 0.0.2a display/GSI parity phase with configurable text alignment, line wrapping, and text scale.

## Automated acceptance

- Java 21 clean build, offline checks and native GameTests are required after branch consolidation.
- Text settings default to left alignment, no wrapping, and 1x scale when loaded from legacy NBT or JSON.
- Left/center/right alignment, wrapping and 0.25x-4x text scale survive element NBT persistence, layout JSON encode/decode, canvas resizing and page changes.
- Wrapped lines are clipped to the element's configured height, adjusted for the configured text scale.

## Client acceptance

On a copied world, edit a text element and:

1. Cycle alignment through left, center and right; confirm each change on the live preview and after closing/reopening the editor.
2. Enable wrapping, resize the element to multiple lines, and confirm text wraps at the element width without drawing below its height.
3. Disable wrapping and verify text is clipped to a single line.
4. Cycle text scale from 0.25x through 4x and confirm the live preview redraws at the new size without breaking wrap/clipping.
5. Save/reload the world and confirm the text content, alignment, wrapping and scale remain unchanged.
6. Repeat on a joined Large Display canvas after expanding and shrinking it.

This reconciliation retains the current main release status and hologram fixes. Text-wrapping client acceptance remains a separate visual check.
