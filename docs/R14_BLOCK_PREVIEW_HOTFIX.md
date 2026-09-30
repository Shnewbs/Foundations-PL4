# Foundations PL4 0.0.1a.R14 — block preview depth hotfix

This hotfix addresses z-fighting **inside 3D block previews rendered on PL4 displays**. It does not change the monitor/bar/editor plane stack, save schema, payload protocol, recipes, networking, or world data.

## Root cause

`DisplayCanvas.item(...)` placed BlockItem previews on display plane 2, then compressed the preview's entire model-space Z axis to essentially zero (`0.000001 / 16` block after scale normalization). After the 30°/45° isometric rotations, separate faces of the block model became nearly coplanar. The monitor layers could be correctly separated while the block's own faces still fought each other.

## Fix

Block previews now retain a bounded local Z scale of **0.0006 block** before the isometric transform. For the centred unit cube used by the default block renderer, the existing 30° X / 45° Y rotation produces about **0.00103 block total depth**. Centred on display plane 2, that remains between the neighboring picture-background/text planes while giving the depth buffer real separation between the block's faces.

Flat item icons keep their prior near-flat behavior. The fix does not push the whole element outward and does not use the rejected 0.03-block workaround.

## Acceptance

Check a Block model element from straight on and from a shallow side angle. The block's top/interior/side faces should remain stable without stippling or shimmer. Also confirm the quantity text and editor outline remain above the block preview and the preview does not visibly float off the monitor.
