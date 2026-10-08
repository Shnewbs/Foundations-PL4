# Integration status — 0.1b

**Further API testing is still required.** The implementation inventory below retains the R1.9 adapter scope; installed-provider acceptance is pending. See [API_TESTING.md](API_TESTING.md).

Current GitHub branches are the baseline; the older R16 archive is historical.

| System | Implemented | Remaining / acceptance requirement |
|---|---|---|
| NeoForge FE | Sided pull and pushed input, simulation/transaction handling, bounded persistent escrow | Actual modpack tests with generators/cables and reloads |
| Mekanism J | Optional native telemetry and transfer/conversion | Installed-version fixtures and richer machine telemetry |
| GregTech CEu EU | Optional native telemetry, transfer/conversion, pushed input and voltage rules | Installed-version packet/amperage acceptance tests |
| Electrodynamics/Voltaic J | Optional native transfer/conversion | Native telemetry and installed-version tests |
| Create | R1.9 RPM, theoretical RPM, stress/capacity, overstress; dedicated model/texture | Shaft-connected generator/motor, stress accounting, configurable electrical conversion, rotation animation and actual Create client/server fixtures |
| AE2 | R1.9 native grid energy/capacity, average usage/injection, powered state, identity deduplication; dedicated model/texture | Explicit AE grid transfer policy/API, storage/crafting telemetry and actual AE2 fixtures |
| IC2-specific EU | Not implemented; CEu EU is a separate API | Identify a maintained compatible mod/API for the target versions; official IC2 Classic files currently list 1.19.2 Forge rather than either PL4 target |
| Other FE tech mods | Existing standard FE path, provided the contacted side exposes the capability | Each mod/version still needs machine/side tests; dedicated telemetry for Immersive Engineering, Powah, Flux Networks, Industrial Foregoing, Ender IO and reactors remains unfinished |
| Recipe/data ecosystems | Existing integration work stays in place | Scriptable provider API, richer KubeJS/CraftTweaker hooks and missing Jade/JEI feature parity should follow the master roadmap |

The master roadmap remains the complete feature backlog. This release does not declare all unfinished products complete.

## Use

Craft the existing Energy Reader. Set its Energy selector to CREATE or AE2. The model changes automatically, including when a display covers the reader. Link Nodes to the actual loaded target; unloaded chunks are never force-loaded. Telemetry is read-only and keeps RPM/SU/AE separate from FE/EU/J.

## Supported contracts and version boundaries

Create: `com.simibubi.create.content.kinetics.base.KineticBlockEntity`: public speed/theoretical-speed/overstress getters and protected float network stress/capacity fields. Resolution checks the exact signatures; access failure disables the adapter. PL4 never calls `getOrCreateNetwork`, stress-calculation methods, speed setters or kinetic network mutation methods.

AE2: `IInWorldGridNodeHost#getGridNode(Direction)` → `IGridNode#getGrid()` → `IGrid#getEnergyService()`. Only read-only service getters are invoked. Grid identity is held only during the sample. This does not bypass AE2 inventory security or expose item/crafting storage.

API contract tests use synthetic providers. Native PL4 tests check model registration and persisted selection. CI does not install Create or AE2, and client visuals/live integrations remain unverified. The 26.3 adapter is dormant without the expected API; no official Create 26.3 branch was present in the upstream branch inventory reviewed on 2026-10-01 UTC.

Sources reviewed:
- https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/content/kinetics/base/KineticBlockEntity.java
- https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/1.21.1/src/main/java/appeng/api/networking
- https://www.curseforge.com/minecraft/mc-mods/ic2-classic/files/all
