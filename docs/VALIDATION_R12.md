# Foundations PL4 0.0.1a.R12 — validation

## Passed offline

- R12 production plane-separation test: 41 assertions.
- R12 source guards: 11/11.
- Retained R5 assertions: 1,149,451.
- Retained R6 assertions: 3,275.
- Retained R7 assertions: 9,665.
- Retained R8 assertions: 2,750.
- Retained R9 assertions: 31,968 plus four deliberate mutations rejected.
- Retained R10 assertions: 2,115 plus four retained regressions rejected.
- Retained R11 dynamic-canvas assertions: 786.
- Java 21 AST parsing: 71 production files.
- Screen-layer guards: 11/11.
- Resource/model/JSON checks and retained R5–R11 source guards passed.

R12 explicitly checks that overlapping filled BAR quads cannot share a plane, and that the five content/editor planes remain strictly ordered and shallow.

## Native build attempt in this environment

`./gradlew --no-daemon --console=plain clean build` stopped before `compileJava` because `services.gradle.org` could not resolve (`UnknownHostException`) while fetching Gradle 9.2.1. This is an environment/network blocker, not a successful native build.

## Not executed here

- Minecraft/NeoForge API type-checking;
- runtime JAR assembly;
- framebuffer/in-game graphics acceptance;
- all 103 native GameTests;
- Windows updater execution.

The user's Windows/Java 21 machine previously reached native `compileJava` and remains the authoritative native acceptance environment.

## Final package checks

- The final full-source ZIP was extracted and the complete offline suite passed from the packaged bytes.
- A native build attempt from the extracted package hit the same `services.gradle.org` DNS failure before `compileJava`.
- ZIP CRC checks passed for both the source and updater archives.
- The exact 24-file R11→R12 updater manifest was applied in a clean filesystem simulation; every before/payload/after SHA-256 matched, and the resulting after-state was recognized exactly. This is not execution of the PowerShell updater on Windows.
