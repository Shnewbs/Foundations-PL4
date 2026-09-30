# R14 block preview hotfix validation

- User-provided in-game screenshot showed z-fighting on a Block model element while the R12/R13/R14 monitor/bar/editor planes were otherwise stable.
- Source inspection identified the block preview Z compression in `DisplayCanvas.item(...)` as the direct cause.
- Old block-preview world Z scale: `0.000001 / 16 = 0.0000000625` block, effectively coplanar after isometric projection.
- New block-preview local Z scale: `0.0006` block, bounded between neighboring display layers for the centred default block model.
- Java 21 AST syntax parsing: PASS, 76 production Java files.
- Retained R5-R14 dependency-free regression assertions: PASS through the point exercised in this environment; remaining source/mutation guards were run separately and pass.
- R9-R14 source/mutation guards: PASS.
- Native Minecraft/NeoForge rendering after this hotfix: NOT RUN in this environment. User's Windows Java 21 client is the authoritative visual acceptance environment.
- Save schema: unchanged (2).
- Multipart slots: unchanged (13).
- Payload protocol: unchanged (4).
- Runtime version: unchanged (`0.0.1a.R14`).
