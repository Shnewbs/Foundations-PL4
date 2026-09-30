# Foundations PL4 0.0.1a.R14 validation

- R14 dependency-free production checks: **12 assertions passed** for three-column defaults, real grid wrapping, and palette normalization/parsing.
- R14 source guards: **15/15 passed** for HUD-gated help, persistent side toolbar, right-click Back, palette wiring, colour preview, columns preview/render use, guide/version wiring.
- Retained suites passed: R5 1,149,451; R6 3,275; R7 9,665; R8 2,745; R9 31,898; R10 2,115; R11 786; R12 41; R13 64 assertions, plus retained mutation/source/resource/GUI-layer checks.
- Java 21 AST parsing: **76 production Java files passed**. This is syntax parsing, not Minecraft/NeoForge API type-checking.
- Resource checks: 370 models, 312 roots, 72 atlas sprites, 482 JSON files parsed.
- Native `compileJava` attempt: **not reached**. Gradle wrapper download failed with `UnknownHostException: services.gradle.org`.
- Registered server GameTests: **106**; executed here: **0**.
- Windows updater execution: pending; updater payload/hash simulation is performed separately when packaging.
