# Foundations PL4 R13 acceptance — `0.0.1a` freeze gate

Use a copied/backed-up world and matching R13 client/server. Do not freeze the alpha until every required item below passes.

## Native build

1. Java 21: `gradlew.bat --no-daemon --console=plain clean build`
2. Expected JAR: `build\libs\FoundationsPL4-1.21.1-0.0.1a.R13.jar`
3. Run: `gradlew.bat --no-daemon --console=plain runGameTestServer`
4. All 106 registered GameTests must pass.

## Monitor editor

- Re-test the exact R12 toolbar angle that previously shimmered. Button backgrounds, borders and glyphs must be stable from straight-on and shallow side views.
- The already-fixed energy/progress bar must remain stable.
- Every visible toolbar button must hover/click where drawn.
- Select an element: all four cyan corner brackets are obvious. Hover each; it should brighten. Drag each corner independently and confirm the opposite corner remains anchored.
- Resize near min size and all canvas edges; no inversion, disappearing handle or out-of-bounds element.
- Check the corner help/keycaps do not obscure active content.
- Save, close, reopen and restart the world; dynamic-canvas element positions/sizes remain unchanged.

## Item stacking / NBT

- Break two default Data Cables (and two default readers/screens) normally. Identical drops must stack.
- Place, break and restack repeatedly; stackability must remain.
- Operator-remove two equivalently configured readers; they should stack after runtime identity/orientation is canonicalized.
- Operator-remove two differently configured readers; they may remain separate.
- Break a Transfer Node while it contains pending item/fluid/energy escrow and confirm the escrow survives replacement.

## Field Guide

- Overall guide size remains the same as R12 at normal desktop GUI scale.
- Graphite/cyan technical binder, pale pages, dark search, right-edge section tabs and two-pane content are crisp and aligned.
- START/NET/DISP/REF tabs change both the chapter list and reading page.
- Search, Saved, scrollbars, bookmarks, Welcome/tutorial jump, previous/next and Done work.
- Test at multiple GUI scales/window sizes; compact mode still works.

## Freeze

If all of the above pass, archive the exact R13 source, built JAR, updater, test logs and world backup as the final `0.0.1a` baseline. Begin new feature work as `0.0.2a`.
