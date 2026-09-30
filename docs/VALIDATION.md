# Foundations PL4 0.0.2a.R1 validation

## Completed

- Native `compileJava` and the clean packaged build passed under Java 21, including asset and standalone-JAR verification.
- R17 emitter-anchor rules passed: 342 assertions across all six mount faces, four requested views, normal/Advanced projectors and both camera sides.
- R16 transfer routing rules passed: 41 assertions.
- Gradle's retained R5-R14 rule tasks and screen-layer checks passed.
- R14, R16 and R17 source guards passed.
- Java 21 AST parsing passed for 78 production Java files.
- `runGameTestServer` executed all 114 registered tests: **114 passed, 0 failed**. Host edits now register newly created parts before synchronous topology rebuilds; joined-canvas tests verify proportional layout migration when a canvas shrinks.
- R1 cached network processing order, reader list and target links during topology rebuild; a native regression verifies priority changes invalidate that cache.
- Current `0.0.2a.R1` clean build packages matching Gradle/mod metadata, and all **115 GameTests pass**.

## Freeze checks - user accepted

- Native hologram visual acceptance - accepted by the user.
- R14 display/editor and R16 real item/fluid/FE transfer acceptance - accepted by the user.
- Escrow save/reload, blocked-destination conservation and dedicated-server smoke test - accepted by the user.

The native build and GameTest gates pass. With the user acceptance above, R17 is frozen and `0.0.2a` is the active development line.
