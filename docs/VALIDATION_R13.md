# Foundations PL4 0.0.1a.R13 validation

## Passed offline in this environment

- R13 dependency-free production rules: **64 assertions passed** for shared toolbar geometry, four-corner resizing, shallow editor planes and item-drop persistence policy.
- R13 source guards: **19/19 passed**, including separate toolbar planes, corner-bracket rendering, colored help/keycaps, stackable normal drops, canonical Operator saves, and technical-binder guide wiring.
- Retained dependency-free suites: R5 **1,149,451**, R6 **3,275**, R7 **9,665**, R8 **2,745**, R9 **31,968**, R10 **2,115**, R11 **786**, R12 **41** assertions.
- The R8 and R9 deliberate mutation suites continue to reject the known regressions.
- Java 21 AST syntax parsing: **74 production Java files**. This is syntax only, not NeoForge API type checking.
- Resource/model/JSON and GUI-layer guard suites passed.
- Full offline suite passed from the working R13 source after the guide/editor/NBT changes.

## Native boundary

The native Gradle attempt stopped **before `compileJava`** because this environment cannot resolve `services.gradle.org` while downloading Gradle 9.2.1 (`UnknownHostException`). No R13 runtime JAR is included. The 106 registered Minecraft GameTests were not executed here.

The user's Java 21 Windows environment previously reached native `compileJava` and remains the authoritative compilation/render/GameTest acceptance environment.

## Not claimed

No claim is made that the new toolbar planes/corner handles are graphically accepted, that the Windows updater executed, or that NBT stacking has been verified in a live Minecraft client until the R13 acceptance checklist is completed.
