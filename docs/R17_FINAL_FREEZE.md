# Foundations PL4 0.0.1a.R17 - frozen baseline

R17 is the accepted `0.0.1a` freeze, following the roadmap's recommendation to fix the hologram emitter anchor without adding new feature scope.

## Hologram geometry

- Normal projection starts at the physical emitter bar.
- Advanced floor/ceiling projection starts at the top emitter; wall projection starts at the selected physical projection panel.
- Projection follows the mount axis: above floor and wall emitters, and below ceiling emitters. Readable View is independent of this axis.
- The mount anchor, emitter anchor, projection axis and projection distance are modeled separately.
- Both canvases have a .25 block centre offset from their emitter edge: .21 block half-height plus a .04 block gap. This places the panel immediately above the bar instead of floating far away.
- Configured view, readable front/back frames and camera-side origin stability are retained.

## Compatibility and scope

R16 transfer routing, save schema 2, 13 multipart slots, payload protocol 4 and recipes are unchanged. The Java 21 clean build, asset/standalone-JAR checks, emitter-anchor rule suite and all 114 R17 native GameTests passed. Host edits register immediately for synchronous topology rebuilds, and joined canvases preserve their settings with proportional layout migration when their size changes.

## Freeze gate - accepted

- Normal and Advanced hologram visual checks on wall, floor and ceiling mounts - accepted.
- R14 display/editor and R16 item/fluid/FE transfer acceptance - accepted.
- Escrow save/reload, blocked-destination conservation and dedicated-server smoke test - accepted.

The user has confirmed the complete gate passes. `0.0.1a` is locked as the immutable base for `0.0.2a`; repeat the native checks when publishing the frozen artifact.
