# Foundations PL4 0.0.2a.R1

Minecraft 1.21.1 / NeoForge 21.1.250 / Java 21. Standalone: no Sonar Core or MCMultiPart runtime dependency.

R1 begins the 0.0.2a display/GSI parity phase. Text elements now support left/center/right alignment, optional wrapped lines, and 0.25x-4x text scale. The unfinished 0.0.1a visual and live-transfer acceptance gates remain open; this is a development build, not a frozen alpha.

R16 ordinary Nodes remain passive transfer endpoints. ADD / REMOVE remains explicit-peer only in this alpha until PL2's directional channel/filter editor is restored. Simulated transfers, persistent driver escrow and the same-cycle receive fence are retained. Items, compatible fluids and FE are transported; native EU and Mekanism Joules remain read-only telemetry.

Save schema 2, 13 multipart slots, payload protocol 4, recipes and transfer rules are unchanged from R17. Element text alignment, wrapping and scale are optional per-element fields; older saves default to left alignment, no wrapping, and 1x scale.

Version tags of the form `v<Gradle version>` trigger a Java 21 build and publish a GitHub Release with the runtime JAR, sources JAR and SHA-256 checksums. See [docs/RELEASING.md](docs/RELEASING.md) for the release gate and steps.

## Build

```bat
gradlew.bat --no-daemon --console=plain clean build
```

Expected artifact after a successful Java 21 build:

```text
build/libs/FoundationsPL4-1.21.1-0.0.2a.R1.jar
```

Run native GameTests with:

```bat
gradlew.bat --no-daemon --console=plain runGameTestServer
```

R1 retains the existing 114 registered GameTests. Use a copied world until the remaining hologram visual and R16 live-transfer acceptance gates pass.
