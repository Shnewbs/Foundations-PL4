# Foundations PL4 0.0.1a.R15 validation

## Scope

R15 is the final narrow 0.0.1a polish candidate: hologram projection clearance and guide-tone cleanup only. It does not change network semantics, save schema, multipart slots, payload protocol or recipes.

## Offline production checks

- R15 hologram projection rules: 223 assertions pass across all six mounting faces, normal/advanced projectors, four requested view directions and both camera sides.
- Normal projection clearance is exactly 0.90 blocks from the mounting surface.
- Advanced projection clearance is exactly 1.20 blocks from the mounting surface.
- The camera-facing readable frame changes without moving the projection origin.
- All retained R5-R14 rule suites pass.
- Java 21 AST syntax parsing passes all 76 production Java files.
- Existing resource/model/GUI/mutation guards pass.
- R15 source guards pass 10/10.

These are not Minecraft framebuffer tests.

## Native boundary

The model environment still cannot resolve the Gradle distribution host, so a native NeoForge build, graphical hologram acceptance and the 106 GameTests must be run on the user's Java 21 Windows environment. The user's R14 environment previously reached `compileJava`, providing a stronger native baseline than this container.

## Freeze gate

Do not freeze 0.0.1a until both normal and Advanced holograms are visually clear of their own projector/cables on wall, floor and ceiling mounts; front/rear text stays readable; existing display/hammer/network regressions remain clean; and all 106 GameTests pass.
