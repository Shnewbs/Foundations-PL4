# Foundations PL4 0.0.1a.R11 — final stabilization hotfix

This hotfix keeps the runtime version at `0.0.1a.R11`. It is intended to close the two client-side defects reported after the R11 dynamic-canvas pass before the `0.0.1a` line is frozen.

## Fixed in source

### Editor toolbar hit testing

R11 moved the visible toolbar onto the monitor face but its click path still tested the pre-R11 outside-screen rectangle. Rendering, hover text and click handling now share one `TOOLBAR_X/Y/W/H/STEP` geometry definition. The old negative-X hit rectangle is removed.

### Monitor z-fighting

No custom filled element geometry is intentionally rendered directly on the physical display plane anymore. The shallow stack is now:

1. physical display surface;
2. element/background geometry;
3. item/block/fluid pictures and ordinary text;
4. quantity/overlay text;
5. selection/editor controls.

The existing scale-normalized hundredth-model-unit spacing is retained, so the fix does not push the editor visibly far away from the monitor when seen from the side.

## Preserved

Dynamic joined-screen canvas sizing and R10 layout migration are unchanged. Save schema remains 2, multipart slots remain 13, and payload protocol remains 4. No recipe, texture, registry ID, energy-provider, hammer, wiring or Field Guide behavior is changed by this hotfix.

## Acceptance before freezing 0.0.1a

Build with Java 21, enter editor mode on a joined Large Display and verify every visible toolbar button hovers/clicks at its drawn position. Check filled bars/backgrounds, icons/models, quantity text and the selection overlay from front and shallow side angles for no flicker. Then save/reload the world and run the registered server GameTests before archiving the final `0.0.1a` baseline.
