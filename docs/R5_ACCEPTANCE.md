# R5 acceptance — native build and single-account gameplay

All native/graphical checks below are pending. Automated fake-player fixtures do not require a second Minecraft account.

## Native build

```bat
gradlew.bat clean build runGameTestServer
```

Require success from Java/API compilation, the packaged asset and standalone guards, the source-layer and production-rule guards, and **31 server GameTests**. Review `run`/GameTest output and generated reports rather than treating JAR existence as proof that tests passed. No R5 runtime JAR was assembled in the packaging environment.

## Reproducible offline checks

```bat
python tools/run_offline_checks.py
```

Requires Python 3 and a Java 21 JDK on PATH. No Gradle or third-party Python packages are used. The five production-rule groups compile from the exact source classes used by the mod. The Java parser deliberately does not resolve Minecraft classes. Source/asset guards do not bake models or render pixels.

## Cable acceptance

In a copied/new creative world, place a chest with 17 diamonds. Attach a Node, add a cable centre to the Node's own host, extend cables, and mount an Inventory Reader and Display on that run. Check all six connection directions, corners and branches. The reader/display must show 17, then update when the count changes. Adding a cable or using an Operator on an existing host must not be intercepted by the GUI.

Disable a port and verify both the arm and the data route disappear; re-enable it and verify recovery. Data and redstone cables must not visually or electrically connect. Place several hosts with different arrangements and check that selection/collision does not retain the shape of a previously touched host. Break/remove one component without duplicating other parts or escrow. Save/reload, then unload/reload the relevant chunks and repeat.

## Hammer acceptance

Place with two clear blocks above; check the model from every side, the inset stone skin, leg positions and moving head. Try placement under a solid ceiling: it must fail without replacing that ceiling. Open the GUI from the base, middle and top, with empty and occupied hands. The panel, text, slots and tooltip must stay sharp with blur enabled and at multiple GUI scales.

Insert two stone: one operation should consume one stone and produce four plates, then return the head during cooldown. Test invalid ingredients, blocked output, a component-bearing output, partial-stack shift-click, full player inventory, extraction, hopper insertion/extraction, close/reopen, save/reload and walking out of menu range. Repeat with an old R3/R4 hammer: headroom obstruction must preserve input/output/progress and show the warning.

Break the base and each upper part in separate tests, in Survival and Creative. Verify one intended machine drop in Survival and no duplicated inventory. Check explosion cleanup and no orphan upper cells. Do not downgrade the tested world without a world backup.

## Large-display acceptance

Build 2x1, 1x2 and 2x2 same-owner rectangles. Verify one shared layout, correct front/back outside borders, no interior border seams, and editor access from each tile. Test wall, floor and ceiling orientations and camera frustum edges. Connect a reader to a non-controller tile and verify data at the shared canvas. Disabled cable ports must remain electrically disconnected despite visual joining.

Join tiles with different saved layouts, split them, and confirm each surviving tile retains its own local saved configuration. Remove the controller and test the newly selected top-left tile. Test an L-shape/hole, mixed owners via the server fixture, mismatched faces, and the 16x16 bound. Test save/reload and chunk borders. Text alignment, front/back UV orientation and animated rendering need a real client; resource-path checks cannot establish those results.

## Performance acceptance

Profile representative small and large installations on client and dedicated server. Check graph rebuild counts while idle and during edits; heap/reference retention after chunk/world unload; sampling cost with real item/fluid/energy providers; and client draw cost. R5 caches topology, geometry and render states, but no TPS, MSPT, FPS or memory-budget acceptance measurement is claimed.
