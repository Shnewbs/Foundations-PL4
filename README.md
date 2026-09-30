# Foundations PL4 0.0.1a.R16

**SOURCE ONLY. No compiled R16 runtime mod JAR is included.** Minecraft 1.21.1 / NeoForge 21.1.250 / Java 21. Standalone: no Sonar Core or MCMultiPart runtime dependency.

R16 hardens Transfer Nodes before the 0.0.1a freeze. Ordinary Nodes now participate as passive transfer endpoints: ADD / IMPORT Transfer Nodes can pull from normal Nodes on the same data network, while REMOVE / EXPORT Transfer Nodes can push into them. Existing REMOVE -> ADD Transfer Node pairs still work and are preferred over passive endpoints on equal priority.

ADD / REMOVE remains explicit-peer only in this alpha until PL2's directional channel/filter editor is restored. This prevents a bidirectional node from blindly pumping the passive network back into itself. Transfers are simulated before extraction; partial real insertion is retained in persistent driver escrow. Resources received in one cycle are fenced from immediate re-extraction during that cycle. Items, compatible fluids and FE are transported; native EU and Mekanism Joules remain read-only telemetry.

The R15 hologram projection and guide-tone work, R14 editor UX and block-preview hotfix, R13 stackability/editor polish, and all earlier wiring/display/hammer work remain. Save schema 2, 13 multipart slots and payload protocol 4 are unchanged.

## Build

```bat
gradlew.bat --no-daemon --console=plain clean build
```

Expected artifact after a successful Java 21 build:

```text
build/libs/FoundationsPL4-1.21.1-0.0.1a.R16.jar
```

Run native GameTests with:

```bat
gradlew.bat --no-daemon --console=plain runGameTestServer
```

R16 registers 114 GameTests total (8 new transfer endpoint tests). Use a copied world until native build, all GameTests and the transfer acceptance checklist pass.
