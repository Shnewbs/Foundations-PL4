# Foundations PL4 R8 — Validation record

**Distribution: SOURCE ONLY. Native Minecraft/NeoForge compilation has not passed. No R8 runtime JAR is included.** This record is specific to R8; older revision reports are historical evidence.

## Executed in this environment

| Check | Observed result | Boundary |
|---|---|---|
| R8 production rules | PASS, 2,750 assertions | Actual dependency-free DisplayPlacement, CanvasContinuity, HologramProjection, GuideLayout and GuideBook methods; no Minecraft execution |
| Deliberately reintroduced R8 bugs | PASS, all 3 mutants rejected | Wrong-axis side placement, mirrored rear hologram frame, older layout winning; mutants compile and fail the executable assertions |
| Retained R5/R6/R7 rules | PASS: 1,149,451 / 3,275 / 9,665 assertions | R6 optional energy handlers are synthetic fixtures, not real mod installations |
| Java 21 AST parsing | PASS, all 54 production Java files | Syntax only, not Minecraft/NeoForge API type checking |
| Existing GUI layer tests | PASS, 11 checks including deliberate bad render order | Source-order guards, not rendering |
| Resources and current guide | PASS: 370 models,312 roots,482 JSONs;216 explicit front variants;60 paired-reader states;18 endpoint leads;22 chapters | Resource references/geometry/content checks, not Minecraft baking/graphics |
| Guide asset generation | PASS | Deterministic R5 -> R6 -> R7 -> R8 generators reproduce exact existing candidate bytes |
| Original content preservation | PASS | All 113 original PNGs and 35 recipes byte-identical to exact R7; 3 new PNGs; 116 total PNGs |
| KubeJS example | PASS with Node.js event stub | Not a KubeJS/Rhino or native recipe reload test |
| Source/updater ZIP and manifest | SHA-256 and archive checks, plus Python filesystem test log supplied | Windows BAT/PowerShell have not been executed |

The full source archive is extracted and its offline runner is executed again from the packaged bytes. See the outer validation/offline-packaged.log and package-audit.json. These checks do not convert the source candidate into a native-tested release.

## Native attempt

Command: `bash gradlew --no-daemon --console=plain clean build runGameTestServer` under Java21.

Observed: exit1 while downloading Gradle9.2.1, `java.net.UnknownHostException: services.gradle.org`. This is **before** Gradle plugin/dependency resolution and Minecraft/NeoForge compilation. There is no runtime mod JAR; the included gradle-wrapper.jar is build tooling only.

## Not executed / unverified

All **73 native GameTests** (58 retained,15 added for R8), native API compilation, a packaged runtime launch, ordinary dedicated-server acceptance, actual in-game GUI/hologram/large-display visuals, real Mekanism/GTCEu integration, KubeJS runtime/reload, and the Windows updater are unverified. The inherited R5 split-layout native test was deliberately updated to R8's documented mirrored-layout contract; it is not counted as an R8 native pass.

An internal PIL layout review used the shipped book textures at wide and compact sizes. It uses substitute fonts and code geometry; it is not a Minecraft screenshot and does not validate widget/event execution or exact Minecraft glyph metrics. No performance benchmark or leak-free guarantee is claimed.

## Acceptance still required

Build with Java21 and Internet access. Run all native tests, install the resulting matching R8 client/server JARs on a copied world, then follow R8_ACCEPTANCE.md. Check all six mounting orientations, growth/removal/reload, protected members, the active-layout merge/split contract, two-sided hologram text, and Field Guide scrolling/search/bookmarks/GUI scaling.

The host keeps schema2/13slots with additive metadata, but payload protocol3 requires matching R8 client/server. Joining canvases mirrors settings to members; restore a matching older world backup to undo those layout changes. Source rollback alone does not restore world data.
