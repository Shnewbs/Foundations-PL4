# Foundations PL4 0.0.1a.R16 freeze gate

R16 is not automatically the frozen alpha merely because the source package exists.

Freeze 0.0.1a only after:

1. Java 21 native `clean build` passes.
2. All 114 registered GameTests pass.
3. R15 normal/Advanced hologram checks pass on wall, floor and ceiling mounts.
4. R14 display editor, grid/palette, block-preview and z-fighting regressions stay fixed.
5. R16 REMOVE -> ADD, normal Node -> ADD and REMOVE -> normal Node item paths pass in-game.
6. At least one real NeoForge fluid tank passes both passive-source and passive-destination transfer.
7. At least one real FE battery/machine passes both passive-source and passive-destination transfer.
8. A full/blocked destination never deletes resources and persisted escrow retries once after reload.

EU/J remain telemetry-only and are not part of the transport freeze gate.
