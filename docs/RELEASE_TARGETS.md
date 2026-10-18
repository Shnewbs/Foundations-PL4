# Release targets

Maintain separate builds for each supported Minecraft target. A Minecraft version is added to a release only after its own native build and regression tests pass.

| Minecraft | Source branch | Java | NeoForge validation target | Release tag |
|---|---|---|---|---|
| 1.21.1 | `main` (release baseline mirrored to `mc/1.21.1`) | 21 | 21.1.250 | `v<version>` |
| 26.1.2 | `mc/26.1.2` | 25 | 26.1.2.114 | `mc26.1.2-v<version>` |
| 26.3 | `mc/26.3` | 25 | 26.3.0.23-beta and 26.3.0.39-beta | `mc26.3-v<version>` |

## Current performance beta and historical alpha rollout

NeoForge 1.21.1, 26.1.2 and 26.3 have exact-target `0.2b.0` beta releases. Forge 1.20.1, 1.20.2 and 1.20.4 have exact-target `0.2b-port.1` beta releases. The three NeoForge native suites each ran 192 PL4 fixtures; each Forge 1.20.x beta suite ran 191. All six runtime/source/checksum release sets were published on GitHub and their runtime JARs accepted by the CurseForge upload API as beta. The [publication report](CURSEFORGE_MULTIVERSION_STATUS.md) records receipt IDs and the [master roadmap](../FOUNDATIONS_PL4_MASTER_STATUS_PARITY_ROADMAP.md) tracks the other 18 published targets, which remain at their earlier revisions.

The earlier `0.2a` rollout published the first three NeoForge tracks; [its dated evidence](releases/0.2a-port-rollout.md) is retained for historical source and checksum details. Installed optional-mod APIs, client visuals, real multiplayer and the broader functional-fallback milestone remain pending. CurseForge moderation approval and public listing are not established by upload receipts.

## Publication policy

A cross-port rollout is complete only for the exact targets whose branch changes pass their own native build and regression gates. Each branch builds its own `FoundationsPL4-<minecraft>-<version>.jar`; the versioned tag identifies the exact source. Each exact-target release publishes its validated runtime JAR, sources and checksums to GitHub; the CurseForge workflow submits the matching runtime with its Minecraft version and alpha/beta/release classification. CurseForge approval remains separate from upload acceptance.

Do not mark a 26.3 JAR as compatible with 26.1.2. Client APIs differ, including screen navigation, camera access, block codecs and item drops. Each port preserves the intended shared behavior while adapting those calls. Existing release tags must not be moved to different source; a changed runtime needs a new revision.

## Additional functional-port scope

**Status reconciliation (October 18, 2026):** This document's three-target matrix describes the **NeoForge current tracks**, not the entire Minecraft catalog. The [master roadmap](../FOUNDATIONS_PL4_MASTER_STATUS_PARITY_ROADMAP.md) lists **24 published exact targets with CurseForge upload receipts**, including independently versioned Forge and legacy releases; six currently have a performance beta. See [the CurseForge publication report](CURSEFORGE_MULTIVERSION_STATUS.md). The earlier [functional-port scope plan](MULTIVERSION_PORT_PLAN.md) remains the feature/fallback contract, not proof every published port has full PL2 parity.

Minecraft **1.13.2** still has a blocked source/native port. Minecraft **1.16.4** has a released Java-8-compatible binary but depends on a documented special Forge-35 launcher profile; ordinary client compatibility is unverified. Minecraft **1.6.4** remains experimental, **1.7.10** remains excluded, and **26.4** remains a forward target pending its exact toolchain. `mc/26.1` and `mc/26.2` remain audit-only without independent validation. No Fabric support is implied.

CurseForge API receipt is not moderation approval or an installed-game certification. Each exact target needs its own runtime/source/checksums and native, real-client, multiplayer, optional-provider and fallback acceptance evidence.

**Further API testing is still required.** Installed optional mods, real-client rendering and multiplayer acceptance remain separate from native test results. See [API_TESTING.md](API_TESTING.md). No compatibility claim is made for other Minecraft versions until tested. Essential tasks require a tested version-appropriate alternative or PL4-owned fallback rather than silent loss of functionality.
