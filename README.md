# Foundations PL4 0.2a

Standalone NeoForge Practical Logistics 4: Foundations. **Alpha — further API testing is still required.**

## New in 0.2a

Wireless Storage combines accessible item inventories on a physical data network, with search, quantity/name sorting, component-aware item variants, withdrawals across sources, and offhand deposits. Receivers and Entity Nodes gain searchable, server-validated component selection; supported components accept held transceiver links.

Storage actions recheck ownership, topology, loaded targets, binding, configuration, and directional filters. Visual-only reader links do not grant inventory access. The field guide documents the network workflow.

See [0.2a release notes](docs/releases/0.2a.md), [completed port rollout evidence](docs/releases/0.2a-port-rollout.md), [the field guide](docs/FIELD_GUIDE.md), and [API/fallback testing](docs/API_TESTING.md). The original release notes describe the state when the 1.21.1 release was published; the rollout evidence records the subsequently completed 26.x builds.

## Published current-track builds

| Minecraft | Runtime JAR | Java | Release |
|---|---|---|---|
| 1.21.1 | `FoundationsPL4-1.21.1-0.2a.jar` | 21 | [v0.2a](https://github.com/Shnewbs/Foundations-PL4/releases/tag/v0.2a) |
| 26.1.2 | `FoundationsPL4-26.1.2-0.2a.jar` | 25 | [mc26.1.2-v0.2a](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc26.1.2-v0.2a) |
| 26.3 | `FoundationsPL4-26.3-0.2a.jar` | 25 | [mc26.3-v0.2a](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc26.3-v0.2a) |

Each release contains the runtime JAR, source JAR, full source archive and checksums. Native validation covers NeoForge 21.1.250, 26.1.2.114, and separate 26.3.0.23-beta / 26.3.0.39-beta builds. The 26.3 published artifact is built against 26.3.0.23-beta. Do not relabel one target's JAR as another target.

The current three-track alpha artifact rollout is complete. **This does not mean all future ports, optional integrations or functional fallbacks are complete.** Installed optional-mod APIs, live multiplayer, client visual acceptance, sustained-load acceptance and full PL2 parity remain separate work.

## Multi-version status and current roadmap (October 10, 2026)

**[Master PL4 roadmap and live acceptance backlog](FOUNDATIONS_PL4_MASTER_STATUS_PARITY_ROADMAP.md)** — use this for current target coverage and incomplete features. The original [multi-version implementation plan](docs/MULTIVERSION_PORT_PLAN.md) is retained as a scope contract, not a current publication ledger.

The [cross-version CurseForge upload report](docs/CURSEFORGE_MULTIVERSION_STATUS.md) records **17 latest tagged Minecraft targets**: fourteen additional API-accepted uploads, plus three previously submitted NeoForge releases. These include the original 1.21.1/26.1.2/26.3 tracks, Forge 1.14.4–1.20.1 target releases for specific versions, and 1.12.2 / experimental 1.6.4 legacy-preview releases. **Upload acceptance is not moderator approval or proof of a working client, full PL2 parity, optional integration or large-modpack performance.** Every exact Minecraft target has its own JAR and acceptance status.

1.16.4 is its own released tag, **not** equivalent to 1.16.5, but requires a special Forge-35/ModLauncher library-precedence profile; standard client-launcher acceptance remains open. 1.13.2 remains **blocked** pending Forge-25 native API/validation work. 1.8.9, 1.11.2, additional final patch gaps and future 26.4 still need independent toolchain/port work. **1.7.10 is excluded**; 1.6.4 remains experimental.

Essential PL4 tasks require target-appropriate compatible optional adapters, alternatives or included PL4-owned fallbacks. Recipe lookup, inspection, configuration, native storage and diagnostics must not silently disappear because an optional API is absent. Unavailable foreign energy/mechanical networks must not be simulated using guessed units/conversions. Stability mods are optional; not every planned fallback is already implemented or tested.

See [version-gap policy](docs/VERSION_COVERAGE.md), [dated coverage audit](docs/VERSION_COVERAGE_CHECKPOINT.md), [newer Forge/legacy release history](docs/PORT_IMPLEMENTATION_STATUS.md), and [exact installed API acceptance](docs/API_TESTING.md).

## Installation and validation

Use one runtime JAR for the exact Minecraft target; Sonar Core and MCMultiPart are not required. Back up worlds before updating. Host save schema remains 2; payload protocol is 5 and clients/servers must use matching versions. Older Wireless Storage reader/display bindings must be rebound to a Node or Transfer Node.

Offline regressions, native compilation, standalone checks, and 191 registered native GameTests gate the 1.21.1 release. Each 26.x native suite executes 192 PL4 fixtures. Passing these tests does not certify installed optional mods or real-client rendering. ADD / REMOVE transfer routing retains its existing peer rules. See [release targets](docs/RELEASE_TARGETS.md) and [API acceptance](docs/API_TESTING.md).
