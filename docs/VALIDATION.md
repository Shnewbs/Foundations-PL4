# Foundations PL4 0.0.1a.R17 validation

## Completed

- Native `compileJava` passed under Java 21.
- R17 emitter-anchor rules passed: 342 assertions across all six mount faces, four requested views, normal/Advanced projectors and both camera sides.
- R16 transfer routing rules passed: 41 assertions.
- Gradle's retained R5-R14 rule tasks and screen-layer checks passed.
- R14, R16 and R17 source guards passed.
- Java 21 AST parsing passed for 78 production Java files.

## Blocked / not run

- `clean build` exits at `verifyClientAssets` because six existing Large Display back textures are missing: `large_display_three_n.png`, `large_display_opposite_1.png`, `large_display_three_e.png`, `large_display_three_w.png`, `large_display_opposite_2.png`, and `large_display_three_s.png`.
- The full offline script stops at the same missing texture set.
- No runtime JAR was produced by a successful full build.
- `runGameTestServer` executed all 114 registered tests: 94 passed and 20 failed. Failures are in existing multipart/display coverage, not the R17 anchor-rule suite:
  `editedsharedlayoutsurvivesreload`, `barerearsouth`, `barerearup`, `barerearnorth`, `barereardown`, `edgeextensionup`, `mergeusesneweststoredsettings`, `barerearwest`, `edgeextensionwest`, `edgeextensiondown`, `edgeextensioneast`, `explicitflipkeepsactivelayoutwhenjoiningnewerneighbor`, `typedjoinedsettingssurviverootremoval`, `joineddisplayspreservesharedlayoutwhensplit`, `collisionandoutlinefollowconnections`, `cablearmsrespectdomains`, `barereareast`, `removedcontrollerkeepssharedlayout`, `edgeextensionnorth`, and `edgeextensionsouth`.
- Native hologram visual acceptance, real item/fluid/FE transfer checks, escrow reload and dedicated-server smoke test remain pending.

The source candidate is not a frozen alpha until the complete R17 freeze gate passes.
