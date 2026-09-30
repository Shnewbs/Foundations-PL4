# Foundations PL4 0.0.1a.R17

**SOURCE ONLY. No compiled R17 runtime mod JAR is included.** Minecraft 1.21.1 / NeoForge 21.1.250 / Java 21. Standalone: no Sonar Core or MCMultiPart runtime dependency.

R17 is a narrow final-freeze candidate. It keeps R16's hardened Transfer Node behavior and anchors hologram canvases to their actual emitter geometry: the normal projector's bar, the Advanced top emitter on floor/ceiling mounts, or its selected wall panel.

The post-freeze R1 continuation now carries per-element text scale and left/centre/right alignment through the editor, renderer, NBT, and element JSON while retaining legacy defaults.

R16 ordinary Nodes remain passive transfer endpoints. ADD / REMOVE remains explicit-peer only in this alpha until PL2's directional channel/filter editor is restored. Simulated transfers, persistent driver escrow and the same-cycle receive fence are retained. Items, compatible fluids and FE are transported; native EU and Mekanism Joules remain read-only telemetry.

Save schema 2, 13 multipart slots, payload protocol 4, recipes and transfer rules are unchanged from R16.

Version tags of the form `v<Gradle version>` trigger a Java 21 build and publish a GitHub Release with the runtime JAR, sources JAR and SHA-256 checksums. See [docs/RELEASING.md](docs/RELEASING.md) for the release gate and steps.

## Build

```bat
gradlew.bat --no-daemon --console=plain clean build
```

Expected artifact after a successful Java 21 build:

```text
build/libs/FoundationsPL4-1.21.1-0.0.1a.R17.jar
```

Run native GameTests with:

```bat
gradlew.bat --no-daemon --console=plain runGameTestServer
```

R17 retains R16's 114 registered GameTests. Use a copied world until native build, all GameTests, hologram wall/floor/ceiling visuals and the R16 transfer acceptance checklist pass.

Hologram placement hotfix: floor and wall canvases sit directly above the emitter edge with a 0.04-block gap; ceiling canvases sit below it. View controls readable orientation, without projecting the canvas horizontally away.
