# Foundations PL4 0.2a

Standalone NeoForge Practical Logistics 4: Foundations. **Alpha — further API testing is still required.**

## New in 0.2a

Wireless Storage combines accessible item inventories on a physical data network, with search, quantity/name sorting, component-aware item variants, withdrawals across sources, and offhand deposits. Receivers and Entity Nodes gain searchable, server-validated component selection; supported components accept held transceiver links.

Storage actions recheck ownership, topology, loaded targets, binding, configuration, and directional filters. Visual-only reader links do not grant inventory access. The field guide now documents the network workflow.

See [0.2a release notes](docs/releases/0.2a.md), [the field guide](docs/FIELD_GUIDE.md), and [API testing](docs/API_TESTING.md).

## Builds and release targets

The primary build is `FoundationsPL4-1.21.1-0.2a.jar`, for Minecraft 1.21.1, NeoForge 21.1.250, and Java 21. GitHub Releases include the runtime JAR, source JAR, source archive, and checksums after the release gates pass.

Minecraft **1.21.1, 26.1.2, and 26.3** are separate [release targets](docs/RELEASE_TARGETS.md), each requiring its own compilation and tests. **The 26.1.2 and 26.3 builds of 0.2a are pending; the complete three-target rollout is not yet finished.** Never relabel one target's JAR as another target.

## Installation and validation

Use one runtime JAR; Sonar Core and MCMultiPart are not required. Back up worlds before updating. Host save schema remains 2; payload protocol is 5 and clients/servers must use matching versions. Older Wireless Storage reader/display bindings must be rebound to a Node or Transfer Node.

Offline regressions, native compilation, standalone checks, and 191 registered native GameTests gate the 1.21.1 release. Installed optional-mod API acceptance, live multiplayer acceptance, client visual acceptance, and full PL2 parity remain separate from those automated checks. ADD / REMOVE transfer routing retains its existing peer rules.
