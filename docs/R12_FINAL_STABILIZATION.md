# Foundations PL4 0.0.1a.R12 — final display-plane stabilization

R12 is the final `0.0.1a` stabilization revision candidate. It follows the user-verified R11 toolbar hitbox fix and targets the remaining in-game z-fighting shown on filled monitor elements.

## Root cause corrected

The R11 hotfix moved categories away from the physical display surface, but a progress/energy bar still emitted its dark base and coloured fill as two overlapping filled quads on the same content plane. The depth buffer therefore had no deterministic winner over the filled portion of the bar. Merely pushing the whole editor farther off the monitor would not fix that same-plane conflict and would recreate visible side-angle floating.

R12 gives generated box geometry an explicit shallow plane:

1. element/background plate;
2. active fill, item/block/fluid picture;
3. frame and ordinary text;
4. quantity/overlay text;
5. editor controls and selection handles.

The bar background is plane 1 and the coloured fill is plane 2, so overlapping filled quads no longer share depth. Fluid wells use the same base/content/frame separation. Automatic-list rules are also moved off the physical panel plane.

The existing step remains `0.01` model units per plane (`0.000625` block before the bounded element-order bias). The highest editor plane is only `0.003125` block from the canvas, below the previously rejected `0.03`-block visible offset.

## Overlapping elements

The existing bounded per-element order bias now applies to planes 1–4. This makes intentionally overlapping custom elements deterministic while keeping the editor on plane 5 above all saved content. The bias is smaller than one full plane step, so it cannot cross category ordering.

## Preserved

- R11 dynamic joined-screen canvas dimensions and migration;
- the corrected shared toolbar render/hover/click geometry;
- R9 typed item/block/inventory/fluid/bar display elements;
- R8 monitor expansion, hologram orientation and Field Guide;
- R7 compact reader/display wiring;
- R6 native read-only FE/J/EU sampling and recipe-level KubeJS support;
- R5 forging hammer and multipart work.

No host save-schema, slot-count or payload-protocol change is introduced by R12. No recipe or existing PNG asset is changed.

## Native acceptance required

The source-only environment cannot download Gradle, so framebuffer behavior cannot be claimed accepted here. On the user's Java 21 machine, build R12 and retest the exact EU progress bar from front, shallow side and editor views. Then run all 103 existing GameTests before freezing `0.0.1a`.
