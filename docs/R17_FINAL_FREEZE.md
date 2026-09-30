# Foundations PL4 0.0.1a.R17 — final-freeze candidate

R17 is a narrow continuation of the R16 source candidate, following the roadmap's recommendation to fix the hologram emitter anchor without adding new feature scope.

## Hologram geometry

- Normal projection starts at the physical emitter bar.
- Advanced projection starts at the selected physical projection panel.
- The mount anchor, emitter anchor, projection normal and projection distance are modeled separately.
- Normal and Advanced clearances remain 0.90 and 1.20 blocks, measured from their emitter geometry.
- Configured view, readable front/back frames and camera-side origin stability are retained.

## Compatibility and scope

R16 transfer routing, save schema 2, 13 multipart slots, payload protocol 4 and recipes are unchanged. The Java 21 clean build, asset/standalone-JAR checks, emitter-anchor rule suite and all 114 native GameTests pass. Host edits register immediately for synchronous topology rebuilds, and joined canvases preserve their settings with proportional layout migration when their size changes. Do not treat this candidate as accepted until the remaining freeze checks pass.

## Remaining freeze gate

- Normal and Advanced hologram visual checks on wall, floor and ceiling mounts.
- R14 display/editor and R16 item/fluid/FE transfer acceptance.
- Escrow save/reload, blocked-destination conservation and dedicated-server smoke test.

Do not freeze `0.0.1a` until the complete gate passes.
