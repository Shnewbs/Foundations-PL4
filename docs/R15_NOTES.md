# Foundations PL4 0.0.1a.R15 — final alpha polish

R15 is intentionally small. User acceptance of R14 confirmed the hammer, KubeJS forging, hopper/pipe automation, FE/EU energy visibility, joined Large Displays, multipart display hosts and fixed display z-fighting. The remaining visible issue was holographic canvases sitting too close to their projector/cable hardware; the Field Guide tutorials also read more mechanically than desired.

## Holograms

The projection origin is now measured from the actual mounting surface. Normal Holographic Displays leave 0.90 blocks of clearance; Advanced leave 1.20. This moves the data plane in front of its own multipart hardware without making it a camera billboard. The projection origin remains fixed and the existing readable front/rear frames, wall mount behavior and four floor/ceiling View directions are retained.

## Field Guide

The same 28-chapter graphite/cyan technical binder remains. The six tutorials were rewritten into a more natural teaching voice: shorter reasons, clearer checks and less instruction-list phrasing. Deterministic examples such as 17 stone -> 22 stone remain so users can confirm live behavior.

## Compatibility

Host schema 2, 13 multipart slots, payload protocol 4, all existing recipes and persisted display fields are unchanged from R14. No world migration is required.

## Freeze intent

R15 is intended to close 0.0.1a if native build, all 106 GameTests and the R15 acceptance checklist pass.
