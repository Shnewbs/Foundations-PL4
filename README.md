# Foundations PL4 0.0.2a development line

**SOURCE REPOSITORY. No compiled 0.0.2a.R1.6 runtime mod JAR is committed.** This branch targets Minecraft 26.3 / NeoForge 26.3.0.23-beta / Java 25. The maintained 1.21.1 source remains on `main` and `mc/1.21.1`. Standalone: no Sonar Core or MCMultiPart runtime dependency.

`0.0.1a.R17` is frozen by user acceptance. This branch is now the `0.0.2a` development line, retaining R16's hardened Transfer Node behavior and R17's emitter-anchored holograms.

The post-freeze R1 continuation now carries per-element text scale, left/centre/right alignment and bounded line wrapping through the editor, renderer, NBT, and element JSON while retaining legacy defaults. Use **Arrange [A]** in the world display editor for current-page alignment and equal spacing; Shift-click adds elements to the selection.

R16 ordinary Nodes remain passive transfer endpoints. ADD / REMOVE remains explicit-peer only in this alpha until PL2's directional channel/filter editor is restored. Simulated transfers, persistent driver escrow and the same-cycle receive fence are retained. Items, compatible fluids and FE are transported; native EU and Mekanism Joules remain read-only telemetry.

Save schema 2, 13 multipart slots, payload protocol 4, recipes and transfer rules are unchanged from R16.

The `mc/26.3` workflow builds and runs native GameTests and retains JARs as CI artifacts. Port publication is disabled during acceptance. Existing `v<version>` release tags belong to the 1.21.1 track; use separate target tags when enabling 26.x publication. See [docs/PORT_26_STATUS.md](docs/PORT_26_STATUS.md).

## Build

```bat
gradlew.bat --no-daemon --console=plain clean build
```

Expected artifact after a successful Java 25 build:

```text
build/libs/FoundationsPL4-26.3-0.0.2a.R1.6-port.1.jar
```

Run native GameTests with:

```bat
gradlew.bat --no-daemon --console=plain runGameTestServer
```

The frozen R17 baseline has 114 GameTests; R1 adds a topology-priority invalidation test, for 115 total. Use a copied world for ongoing 0.0.2a acceptance.

Hologram placement hotfix: floor and wall canvases sit directly above the emitter edge with a 0.04-block gap; ceiling canvases sit below it. View controls readable orientation, without projecting the canvas horizontally away.

R1.1 selection: drag empty monitor space for box selection; Shift adds a box or toggles an element. Ctrl+A selects the current page. Drag a selected element to move the selection together; corner handles resize the active element. Ctrl+C/V and Ctrl+D copy, paste and duplicate the whole selection in one server edit. Pasting onto another page retains relative positions and styles.
