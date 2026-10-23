# Foundations PL4

Standalone NeoForge Practical Logistics 4: Foundations. **Performance beta — further API testing is still required.**

## RC development source

The maintained ports share **`0.2.0-rc.1-dev`**, an unpublished development version. All 24 have independently passing native gates; their feature scope and publication clearance remain separate. The common update includes indexed network lookups, reused transfer endpoint keys, channel-aware transfer status, the compact Wireless Storage layout and matching guide editions. [Native build evidence](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39713811633) and [installed Forge follow-up](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39716131269) cover exact targets; the 1.21.1 runtime also excludes its 192 native fixtures from the production JAR.

**A stable RC has not been released.** The 1.12.2 and 1.6.4 development ports now add bounded item/fluid/energy/network readers and four-line displays that start private to their transport subsets. Both passed 120 native scenarios, production-only boots and generated preview.3 world upgrades. Advanced legacy editor/multipart/wireless-storage parity remains incomplete. Actual client UI, installed optional providers, live multiplayer, migration and measured performance acceptance remain open. See the [current RC roadmap](FOUNDATIONS_PL4_MASTER_STATUS_PARITY_ROADMAP.md). The tables below describe published builds, which remain separate from development snapshots.

The current hardening quarantines uncertain item/fluid/FE/native-energy mutations across saves and part removal. Wireless Storage stages offhand deposits in node escrow before foreign insertion and fences uncertain player delivery. [The exact-target ledger](docs/RC_DEVELOPMENT_STATUS.json) records native gates separately from RC clearance. The 1.21.1 candidate also has [actual client UI snapshots](docs/acceptance/rc-dev-1.21.1-client-visual.json), [two actual client storage sessions](docs/acceptance/rc-dev-1.21.1-real-client-storage.json), [a generated released-beta-source world upgrade](docs/acceptance/rc-dev-1.21.1-generated-beta-world-upgrade.json) and [native operation measurements with mixed results](docs/acceptance/rc-dev-1.21.1-native-performance.json). These narrow results do not certify installed providers, full gameplay or another port.

## New in 0.2b.0

The six current beta targets reuse each endpoint's physical target key during transfer candidate checks and eligible-peer counting, reducing repeated identifier allocation in larger networks. Routing order, rates, filters and saved data remain unchanged. This is a source-level performance change; server TPS and memory improvements have not been measured.

The earlier 1.21.1 revision indexed network membership and storage-node lookups, adjusted the Wireless Storage screen for a 320×240 scaled window, and added a two-session conservation scenario. The server scenario uses fake players; live multiplayer and client visuals still require installed testing.

## Included from 0.2a

Wireless Storage combines accessible item inventories on a physical data network, with search, quantity/name sorting, component-aware item variants, withdrawals across sources, and offhand deposits. Receivers and Entity Nodes gain searchable, server-validated component selection; supported components accept held transceiver links.

Storage actions recheck ownership, topology, loaded targets, binding, configuration, and directional filters. Visual-only reader links do not grant inventory access. The field guide documents the network workflow.

See [0.2b.0 release notes](docs/releases/0.2b.0.md), [0.2a.1 release notes](docs/releases/0.2a.1.md), [0.2a release notes](docs/releases/0.2a.md), [completed port rollout evidence](docs/releases/0.2a-port-rollout.md), [the field guide](docs/FIELD_GUIDE.md), and [API/fallback testing](docs/API_TESTING.md). The original release notes describe the state when the 1.21.1 release was published; the rollout evidence records the subsequently completed 26.x builds.

## Published current-track builds

| Minecraft | Runtime JAR | Java | Release |
|---|---|---|---|
| 1.21.1 | `FoundationsPL4-1.21.1-0.2b.0.jar` | 21 | [v0.2b.0](https://github.com/Shnewbs/Foundations-PL4/releases/tag/v0.2b.0) |
| 26.1.2 | `FoundationsPL4-26.1.2-0.2b.0.jar` | 25 | [mc26.1.2-v0.2b.0](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc26.1.2-v0.2b.0) |
| 26.3 | `FoundationsPL4-26.3-0.2b.0.jar` | 25 | [mc26.3-v0.2b.0](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc26.3-v0.2b.0) |

Each release contains the runtime JAR, source JAR, full source archive and checksums. Native validation covers NeoForge 21.1.250, 26.1.2.114, and separate 26.3.0.23-beta / 26.3.0.39-beta builds. The 26.3 published artifact is built against 26.3.0.23-beta. Do not relabel one target's JAR as another target.

The three current NeoForge tracks and three Forge 1.20.x targets have performance beta releases. Eighteen other published exact targets remain at earlier alpha or experimental revisions. **Optional integrations, functional fallbacks and full cross-port beta parity remain incomplete.** Installed optional-mod APIs, live multiplayer, client visual acceptance, sustained-load acceptance and full PL2 parity remain separate work.

## Multi-version status and current roadmap (October 18, 2026)

**[Master PL4 roadmap and live acceptance backlog](FOUNDATIONS_PL4_MASTER_STATUS_PARITY_ROADMAP.md)** — use this for current target coverage and incomplete features. The original [multi-version implementation plan](docs/MULTIVERSION_PORT_PLAN.md) is retained as a scope contract, not a current publication ledger.

The [cross-version CurseForge upload report](docs/CURSEFORGE_MULTIVERSION_STATUS.md) records matching upload receipts for the latest release of **24 exact Minecraft targets**, with zero submission failures in the October 18 reconciliation. Six of those targets have the performance beta: NeoForge 1.21.1, 26.1.2, 26.3 and Forge 1.20.1, 1.20.2, 1.20.4. The other 18 retain their prior releases, including legacy previews. **Upload acceptance is not moderator approval or proof of a working client, full PL2 parity, optional integration or large-modpack performance.** Each exact target has its own JAR and acceptance status.

1.16.4 is its own released tag, **not** equivalent to 1.16.5, but requires a special Forge-35/ModLauncher library-precedence profile; standard client-launcher acceptance remains open. 1.13.2 remains **blocked** pending Forge-25 native API/validation work. 1.8.9, 1.11.2, additional final patch gaps and future 26.4 still need independent toolchain/port work. **1.7.10 is excluded**; 1.6.4 remains experimental.

Essential PL4 tasks require target-appropriate compatible optional adapters, alternatives or included PL4-owned fallbacks. Recipe lookup, inspection, configuration, native storage and diagnostics must not silently disappear because an optional API is absent. Unavailable foreign energy/mechanical networks must not be simulated using guessed units/conversions. Stability mods are optional; not every planned fallback is already implemented or tested.

See [version-gap policy](docs/VERSION_COVERAGE.md), [dated coverage audit](docs/VERSION_COVERAGE_CHECKPOINT.md), [newer Forge/legacy release history](docs/PORT_IMPLEMENTATION_STATUS.md), and [exact installed API acceptance](docs/API_TESTING.md).

## Installation and validation

Use one runtime JAR for the exact Minecraft target; Sonar Core and MCMultiPart are not required. Back up worlds before updating. Host save schema remains 2; payload protocol is 5 and clients/servers must use matching versions. Older Wireless Storage reader/display bindings must be rebound to a Node or Transfer Node.

Offline regressions, native compilation and 192 registered native GameTests gate the 1.21.1 beta. Each 26.x native suite executes 192 PL4 fixtures. The three Forge 1.20.x beta builds each passed 191 native GameTests. Passing these tests does not certify installed optional mods or real-client rendering. ADD / REMOVE transfer routing retains its existing peer rules. See [release targets](docs/RELEASE_TARGETS.md) and [API acceptance](docs/API_TESTING.md).
