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

## Additional functional ports and alternatives

The next multi-version milestone targets 1.20.1, 1.19.2, 1.18.2, 1.16.5 and 1.12.2, with **1.6.4 experimental** and **1.7.10 excluded**. 26.4 is a forward target when a usable toolchain is verified. These additional ports are not yet claimed working. See [the approved plan](docs/MULTIVERSION_PORT_PLAN.md).

Essential PL4 tasks must have a tested compatible optional adapter, an alternative, or a PL4-owned fallback. Recipe lookup, inspection, configuration, native storage and diagnostics must not silently disappear because an optional API is unavailable. Missing foreign energy/mechanical systems are not emulated with guessed units or conversions. Third-party mods remain optional, and not every planned fallback is implemented yet.

## Installation and validation

Use one runtime JAR for the exact Minecraft target; Sonar Core and MCMultiPart are not required. Back up worlds before updating. Host save schema remains 2; payload protocol is 5 and clients/servers must use matching versions. Older Wireless Storage reader/display bindings must be rebound to a Node or Transfer Node.

Offline regressions, native compilation, standalone checks, and 191 registered native GameTests gate the 1.21.1 release. Each 26.x native suite executes 192 PL4 fixtures. Passing these tests does not certify installed optional mods or real-client rendering. ADD / REMOVE transfer routing retains its existing peer rules. See [release targets](docs/RELEASE_TARGETS.md) and [API acceptance](docs/API_TESTING.md).
