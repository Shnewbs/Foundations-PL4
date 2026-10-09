# Release targets

Maintain separate builds for each supported Minecraft target. A Minecraft version is added to a release only after its own native build and regression tests pass.

| Minecraft | Source branch | Java | NeoForge validation target | Release tag |
|---|---|---|---|---|
| 1.21.1 | `main` (mirrored to `mc/1.21.1`) | 21 | 21.1.250 | `v<version>` |
| 26.1.2 | `mc/26.1.2` | 25 | 26.1.2.114 | `mc26.1.2-v<version>` |
| 26.3 | `mc/26.3` | 25 | 26.3.0.23-beta and 26.3.0.39-beta | `mc26.3-v<version>` |

Each update must be applied and validated on all three tracks before the rollout is considered complete. Each branch builds its own `FoundationsPL4-<minecraft>-<version>.jar`; the versioned tag identifies the exact source. The automatic release workflow publishes the validated runtime JAR, sources and checksums to GitHub, then submits the runtime to CurseForge with the matching Minecraft version and alpha/beta/release classification. CurseForge approval remains separate from upload acceptance.

Do not mark a 26.3 JAR as compatible with 26.1.2. Client APIs differ, including screen navigation, camera access, block codecs and item drops. The separate 26.1.2 backport keeps the 0.1b feature set and public API contract while adapting those calls.

**Further API testing is still required.** Installed optional mods, real-client rendering and multiplayer acceptance remain separate from native test results. See [API_TESTING.md](API_TESTING.md). No compatibility claim is made for other 26.x versions, including 26.4 until tested.
