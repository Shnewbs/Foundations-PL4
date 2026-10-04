# Second R2 correctness audit — 0.0.2a.R2.3

## Confirmed findings and fixes

| Finding | Fix | Regression evidence |
|---|---|---|
| ADD extraction was not staged before destination insertion | Persist item/fluid/legacy FE buffers before external insertion; decrement after reported acceptance | Native chest fixture throws before mutation, checks persistence and exactly-once retry; existing fluid/energy conservation suites |
| Fallback previews could exceed the shared budget | Charge every full/fallback preview against the remaining budget; omit exhausted previews | 256 distinct named stacks, summed encoded preview bytes <=32768, distinct stable keys, preserved counts |
| Layout snapshots decoded excessive element arrays | Reject above DisplayElements.MAX_ELEMENTS before per-element decoding | 32 accepted, 33 rejected |

## Coverage

Reviewed transfer execution/escrow, preview serialization, reader sampling and provider paths, host persistence, placement/removal, packet ownership/distance/revision checks, templates and release configuration. Source/resource checks cover model references, cable geometry, screen planes, guide/version consistency and registered native fixtures. CI runs dependency-free Java regressions, native compilation and server GameTests on 1.21.1/21.1.250 and 26.3/26.3.0.23-beta plus 26.3.0.39-beta. Workflow run conclusions are the authoritative execution results.

No local Java compiler is available; native and Java-only execution is delegated to CI. Static checks are not graphical acceptance. Installed Create/AE2/Mekanism/GT/Electrodynamics acceptance, real client dark-screen/placement visuals and live two-player sessions remain unverified. Future Minecraft/NeoForge releases require their own build and gameplay validation.

Conservation assumes providers report mutations correctly. A provider that accepts resources and then throws without returning its accepted amount cannot be recovered generically. Oversized component data may use sanitized base previews; stable component keys are guaranteed by this change only for individually bounded previews. The preview budget excludes row labels and numeric metadata; it is not a total packet-size limit.

No claim of flawless code or exhaustive proof is made. Separate API branches and the legacy FE fast path are intentional; no speculative consolidation was applied.
