# Foundations PL4 0.0.1a.R13 — final alpha stabilization

R13 is intentionally narrow: it closes the remaining R12 editor-chrome z-fighting, improves resize affordances, fixes broken-part stackability, and applies the requested Field Guide visual cleanup. It does not expand the PL2 feature scope.

## Editor chrome

R12 fixed the progress/energy bar by splitting background/fill/frame/text planes. The user then confirmed that the bar was stable while the toolbar buttons still shimmered. R13 therefore separates editor chrome itself: button background plane 5, border plane 6, glyph/help plane 7, and hover/corner accents plane 8. These are still shallow 0.01 model-unit steps divided by 16, not a visible 0.03-block projection.

The render and hit-test rectangles still come from one `EditorChrome` geometry source, preserving the R11 hitbox fix.

## Four-corner resize

A selected element now has a 10x10 logical hit target at all four corners. The visual is an L-shaped cyan PL4 bracket rather than a filled square. Hover brightens the bracket and adds a restrained cyan interior glow; an idle pulse makes corners discoverable without covering content. Each corner anchors the opposite corner during resize, and all paths keep the existing 8x9 minimum and dynamic-canvas bounds.

The lower monitor corner contains contextual help. Key hints are colored keycaps (Edit blue, Delete red, Snap green, Escape amber). Toolbar controls also have functional color families rather than identical monochrome boxes.

## Broken-part NBT / stacking

Normal block breaking now uses a stackable item path. A default PL4 part does not receive `pl_part` CustomData just because it had a runtime UUID, owner, ticks or network state. This makes ordinary identical drops stack again.

Transfer escrow is the exception: if a Transfer Node contains pending item/fluid/energy escrow, that payload is retained so breaking the part cannot destroy in-flight value.

Operator removal deliberately uses a saved-configuration path. It keeps meaningful configuration but strips runtime-only identity, owner, signal, ticks and layout revision and normalizes the saved face, allowing equivalent saved configurations to stack while genuinely different configurations remain distinct.

## Field Guide presentation

The Guide keeps the same established maximum 620x360 screen footprint and all 28 current PL4/1.21.1 chapters/tutorials. The shell is restyled toward the user's Calculator Field Guide reference without copying its colors: graphite technical binder, pale reference pages, cyan edge rails, dark top search, compact right-edge START/NET/DISP/REF section tabs, and the existing PL4 dark-cyan section plates.

## Compatibility

World schema 2, 13 multipart slots, dynamic-canvas persistence and packet protocol 4 are unchanged. No recipe or existing PNG asset is changed by R13. The item-form serialization policy changes intentionally.
