# Foundations PL4 0.0.1a.R15 — final alpha polish

R15 is intentionally narrow. It moves normal and Advanced hologram canvases farther clear of projector/cable geometry and rewrites the six onboarding tutorials into a more natural teaching voice.

## Hologram clearance

- Normal: 0.90 blocks from the mounting surface.
- Advanced: 1.20 blocks from the mounting surface.
- The projection origin remains fixed; walking behind it switches to a right-handed readable frame rather than moving the plane or mirroring text.
- Floor/ceiling View rotation and wall-mount orientation remain unchanged.

## Compatibility

Host schema 2, 13 multipart slots, payload protocol 4, recipes and persisted display fields are unchanged. No migration is required from R14.

## Alpha freeze

Freeze 0.0.1a only after native Java 21 build, all 106 GameTests, and visual checks of both hologram types on wall/floor/ceiling mounts.
