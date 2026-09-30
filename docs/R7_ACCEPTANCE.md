# R7 acceptance — run on a copied world

## Build

Use Java 21 JDK and run `gradlew.bat --no-daemon --console=plain clean build runGameTestServer` from the R7 source folder. This must pass compilation, resource gates, pure-rule gates, JAR verification and all 58 server GameTests. Expected runtime output after success: `build/libs/FoundationsPL4-1.21.1-0.0.1a.R7.jar`. Replace the prior runtime mod on both client/server; do not install a sources JAR or wrapper.

## The screenshot blocker

Use one chest containing 17 diamonds, a Node, a data cable run and an Inventory Reader. Before adding the screen, verify the reader Data tab contains 17 diamonds. Click the reader's OUTWARD face with a normal/mini/large panel. It must occupy the same host face, without a new host, a cable branch in front or loss of the reader. Keep Reader name blank (or the attached reader's name). The panel must show 17 and update when the chest count changes.

Repeat DOWN/UP/NORTH/SOUTH/WEST/EAST. Check text/front orientation, reader-with-display texture, no z-fighting, accurate outlines/collision, no gaps/stretched cable cubes, and correct item count after successful/failed placement. Test placing the reader onto an existing same-face panel as well. Use a fresh panel and an Operator-removed saved panel with a known front setting.

Right-click the screen for its editor. Shift-right-click empty-handed for the reader underneath. Sneak-Operator on the front removes only the screen and restores the reader's original model. Permission failures must not remove/place/edit anything.

## Exposed endpoints and separate visual bus

Test a reader with no centre cable in its own host, fed by a cable in the next block behind it. It must read without creating a free cable item/centre. Verify the actual stem and selection/collision reach that cable. Disable the cable's matching port: source data and connector must disappear on update. Side contact and wrong cable family must not work.

Feed a separate cable run from the reader's exposed visual front. Select that reader on a remote display. Put a different chest/Node/Reader on the output run: the input reader must not aggregate the output chest, and Transfer Nodes must not transport between the two runs. Disable the front cable port and confirm remote display rows clear instead of freezing. Re-enable/reload and check recovery. Do not test merely the letters: verify inventory counts on both sides.

## Persistence and prior features

Load a copied R6 world; verify front settings, same selected reader, layouts, owners, part UUID links, cable port masks, hammer inventory/progress and transfer escrow. Save/reload with paired parts; no display or reader may vanish. Rebuild a rectangle using paired readers/panels; check shared editor, front flipping, controller selection and split layouts. Check an L-shape remains individual panels, not blank/stale.

Exercise chunk unload/reload without forced chunk loading, including a reader at a chunk boundary and a remote display in another loaded chunk. Verify joins and wires recover on return. Open guide, reader/settings/layout and hammer UI at multiple scales: the old blur-over-content defect must stay fixed.

Recheck the exact installed FE/Mekanism/GTCEu devices using the Energy Reader Data tab before the display. Existing KubeJS recipe compatibility still needs its own native reload test. A green pure-rule test is not this acceptance.

## Native fixture inventory

35 retained R3/R5/R6 fixtures, adapted only where display slots changed, plus 23 R7 fixtures: six compact orientations, six exposed-back orientations, six visual-isolation orientations, R6 save migration, thirteen-slot codec retention, paired geometry/hit/under-reader access, disconnect clearing, and real PartItem same-host placement. None was executed during packaging.
