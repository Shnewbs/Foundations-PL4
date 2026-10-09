# Release targets

Maintain separate builds for each supported Minecraft target. A Minecraft version is added to a release only after its own native build and regression tests pass.

| Minecraft | Source branch | Java | NeoForge validation target | Release tag |
|---|---|---|---|---|
| 1.21.1 | `main` (release baseline mirrored to `mc/1.21.1`) | 21 | 21.1.250 | `v<version>` |
| 26.1.2 | `mc/26.1.2` | 25 | 26.1.2.114 | `mc26.1.2-v<version>` |
| 26.3 | `mc/26.3` | 25 | 26.3.0.23-beta and 26.3.0.39-beta | `mc26.3-v<version>` |

## Current 0.2a rollout

GitHub Releases now contains runtime JARs, source JARs, source archives and checksums for all three current tracks. The 26.1.2 and 26.3 builds include the 0.2a network-storage and component-selection changes. The 26.1.2 suite executed all 192 PL4 native fixtures; both pinned 26.3 loader builds independently executed all 192. The 1.21.1 release has its separate 191-fixture suite. See [exact release commits, runs and checksums](releases/0.2a-port-rollout.md).

This is completion of the current-track alpha artifact rollout only. Installed optional-mod APIs, client visuals, real multiplayer and the broader functional-fallback milestone remain pending. CurseForge accepted the new port uploads; moderation approval is not verified.

## Publication policy

Each update must be applied and validated on all three current tracks before that rollout is considered complete. Each branch builds its own `FoundationsPL4-<minecraft>-<version>.jar`; the versioned tag identifies the exact source. The automatic release workflow publishes the validated runtime JAR, sources and checksums to GitHub, then submits the runtime to CurseForge with matching Minecraft version and alpha/beta/release classification. CurseForge approval remains separate from upload acceptance.

Do not mark a 26.3 JAR as compatible with 26.1.2. Client APIs differ, including screen navigation, camera access, block codecs and item drops. Each port preserves the intended shared behavior while adapting those calls. Existing release tags must not be moved to different source; a changed runtime needs a new revision.

## Additional functional-port scope

The [functional ports and fallback plan](MULTIVERSION_PORT_PLAN.md) adds 1.20.1, 1.19.2, 1.18.2, 1.16.5 and 1.12.2, with **1.6.4 experimental**. These additional ports are not yet published or claimed working. **1.7.10 is excluded.** 26.4 is a forward target when a usable toolchain is verified. The existing 26.1 and 26.2 refs remain audit-only, not certified builds. No Fabric support is implied.

**Further API testing is still required.** Installed optional mods, real-client rendering and multiplayer acceptance remain separate from native test results. See [API_TESTING.md](API_TESTING.md). No compatibility claim is made for other Minecraft versions until tested. Essential tasks require a tested version-appropriate alternative or PL4-owned fallback rather than silent loss of functionality.
