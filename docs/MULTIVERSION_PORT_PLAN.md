# PL4 functional ports and optional-API fallback plan

Updated: October 8, 2026 (America/Los_Angeles).

## Approved scope

The next multi-version update must deliver usable ports, not just branches or renamed JARs. Minecraft **1.6.4 is approved as an experimental track**. Minecraft **1.7.10 is excluded**. Experimental means actual implementation and testing are required; it does not mean a working port already exists. The 1.6.4 track must not hold up validated modern releases.

| Minecraft | Branch | Loader | Scope |
|---|---|---|---|
| 1.21.1 | main, mirrored to mc/1.21.1 | NeoForge | Maintain the primary release baseline |
| 26.1.2 | mc/26.1.2 | NeoForge | Complete current feature parity and native validation |
| 26.3 | mc/26.3 | NeoForge | Complete current feature parity and native validation |
| 1.20.1 | mc/1.20.1 | Forge | First additional functional port |
| 1.19.2 | mc/1.19.2 | Forge | Functional port, independently adapted and tested |
| 1.18.2 | mc/1.18.2 | Forge | Functional port, independently adapted and tested |
| 1.16.5 | mc/1.16.5 | Forge | Functional port, including language and API backports |
| 1.12.2 | mc/1.12.2 | Forge | Dedicated legacy implementation |
| 1.6.4 | mc/1.6.4 | Forge | Approved experimental implementation; independent feasibility/build gate |
| 26.4 | mc/26.4 | Verify toolchain | Forward target when a usable toolchain is verified |

These are scope commitments, not a list of completed ports. A branch is created with research/implementation status until its actual build and scenarios pass. Existing mc/26.1 and mc/26.2 remain audit-only unless independently validated; do not delete them or imply binary compatibility. No Fabric support is implied.

## Functional baseline for every port

Preserve cables and multipart placement, network topology, readers, displays/editor, Nodes and Transfer Nodes, sided item/fluid/available-native-energy access, wireless links and item storage, Forging Hammer recipes, configuration, player permissions, resource conservation and reliable persistence. Port the behavior using each target's actual APIs; do not disable a feature merely to obtain a green compile. A missing required function blocks the functional milestone and must be named in the status report.

Use version-specific registration, networking, persistence, inventory/resource interfaces, client rendering, recipe formats and test infrastructure. Share behavioral rules and reviewed fixes where possible. Never merge an entire incompatible version branch into main or lower the Java declaration on unmodified modern source. Back up worlds; preserve published tags and keep save migration separate from unsupported downgrades.

## Required alternatives for unavailable optional APIs

**Optional dependencies stay optional; essential PL4 functions must have an included path.** For each feature, use an installed, target-compatible adapter when available. Otherwise use a verified alternative adapter or implement the PL4-owned fallback below. Unavailable is not a completion state for a required user task. Do not call a proposed fallback implemented or tested until it is.

| User task | External integration candidates | Included fallback required for the functional milestone |
|---|---|---|
| Recipe lookup | JEI, EMI or era-appropriate NEI | PL4 recipe browser/guide path showing actual registered recipes, input/output counts and reload changes; static instructions alone do not certify live recipe lookup |
| Inspect machines, nodes and networks | Jade, HWYLA/Waila or a verified target-specific continuation | PL4 inspector/status view with the data needed to configure and diagnose PL4; no mandatory HUD mod |
| Configure recipes and automation | KubeJS, CraftTweaker or an era-appropriate scripting alternative | PL4 declarative configuration/recipe rules and documented native extension hooks; no claim that configuration replaces arbitrary JavaScript execution |
| Inventory and fluid access | Target loader interfaces and available storage-mod integrations | Native sided inventory/fluid adapters plus PL4's own network storage/transfer behavior; vanilla-compatible tests with no storage mod installed |
| Power monitoring/transfer | FE/RF, appropriate IC2/GregTech EU, Mekanism Joules, BuildCraft or Voltaic APIs where present | Correct target-native power adapters and explicit capability diagnostics. Preserve units, voltage, sidedness and simulation. An absent foreign network is not emulated and never receives guessed conversion ratios |
| Mechanical or storage telemetry | Create, AE2 or the original Applied Energistics generation where available | PL4 native network/transfer information remains accessible; external stress/channels are marked not applicable when that external system does not exist |
| Performance and stability | ModernFix, FerriteCore and verified target-appropriate alternatives | PL4's own bounded scans, topology caching, lifecycle cleanup and performance tests. Do not force-install a stability mod or claim an untested legacy fix is equivalent |

For every candidate, record Minecraft patch, loader, upstream version/revision, checksum, required dependencies, supported feature surface and installed-test outcome. Recipe viewers and HUD replacements are alternatives, not instructions to install incompatible forks together. Where two providers coexist, avoid duplicate registration and record the selected adapter.

Included means PL4-owned fallback code or adapter code is part of the corresponding PL4 build. Third-party mods remain optional installs unless separately approved for distribution and their licenses permit it. Never silently download, bundle, rehost or backport an entire external mod just because its API is missing. No unrelated loader/version JAR can be substituted.

## Verification and release gates

Track source port, native build, required gameplay scenarios, no-optional-mod baseline, alternative/fallback behavior, installed integrations, real-client visuals, multiplayer and performance separately. A synthetic fixture, reflected signature, detected mod ID or green compile is not an installed-integration pass.

Required scenarios include replay/malformed packets, owner changes, missing and replaced providers, full/empty/sided stores, NBT/component variants, simulation without mutation, restart with nonempty escrow, unload/reload without forced chunk loading, cable geometry, GUI scaling and display editing, two-player interactions and reconnects. Test optional APIs absent, compatible alternatives present, incompatible versions present, and failure of one adapter without losing the core functions. On legacy targets use an equivalent server scenario harness instead of claiming modern GameTests ran there.

Use NOT_STARTED, IMPLEMENTING, BUILD_PASS, SCENARIO_PASS and BLOCKED for ports. Record INSTALLED_PASS, INSTALLED_FAIL, NOT_TESTED or NOT_AVAILABLE separately for external integrations. Required fallback status is independently IMPLEMENTING, TESTED_PASS or BLOCKED. A port is functionally complete only when required tasks, the no-optional baseline and the selected alternatives/fallbacks pass. Stable compatibility additionally requires the corresponding client and multiplayer evidence.

Publish each validated target's own runtime JAR, source JAR/archive, checksums, exact version identity and honest release notes on GitHub Releases. Submit CurseForge files with matching Minecraft/loader metadata; upload acceptance and moderation approval remain separate. Keep **Further API testing is still required** while installed-provider acceptance is incomplete. Report partial rollouts explicitly; publishing two 26.x ports does not finish the older ports.

## Implementation order

1. Finish the outstanding 0.2a feature migration on the existing 26.1.2 and 26.3 ports without discarding their native adapters or tests. The reviewed migration tooling is specific to these baselines, not a general port generator.
2. Complete the no-optional baseline and required built-in fallbacks, then the 1.20.1 Forge port and its actual integration profile.
3. Apply the same functional contracts to 1.19.2, 1.18.2 and 1.16.5 with independent toolchains and tests.
4. Implement 1.12.2 separately. Establish the isolated 1.6.4 experimental development environment, registry/ID policy, player identity, persistence and equivalent scenario harness; evaluate legacy integrations without borrowing GTNH/1.7.10 binaries.
5. Create the 26.4 track when verified tooling exists and repeat the same gates. Only mark the complete rollout done when each required track's recorded evidence supports that claim.

## 1.6.4 experiment and references

Forge's official 1.6.4 page lists 9.11.1.1345 with development source and runtime downloads. That is a starting point, not evidence of a successful PL4 build. Record the actual compiler, bytecode and runtime; keep the legacy environment isolated. Investigate NEI/Waila, original Applied Energistics, Thermal Expansion/CoFH, BuildCraft, IC2 and the appropriate GregTech generation individually. Their exact compatible artifacts and APIs remain subject to verification.

Author-maintained references for dependency research; none certifies PL4 integration:

- Forge 1.6.4: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.6.4.html
- JEI: https://github.com/mezz/JustEnoughItems
- EMI: https://github.com/emilyploszaj/emi
- NEI historical project: https://github.com/Chicken-Bones/NotEnoughItems
- Jade author files: https://www.curseforge.com/minecraft/mc-mods/jade/files/all
- ModernFix: https://github.com/embeddedt/ModernFix
- FerriteCore: https://github.com/malte0811/FerriteCore
- Create development status: https://wiki.createmod.net/users/development-status
- GregTech CEu Modern: https://github.com/GregTechCEu/GregTech-Modern
- GregTech CEu legacy: https://github.com/GregTechCEu/GregTech
- BuildCraft: https://mod-buildcraft.com/pages/download.html
- NeoForge 26.1 migration: https://neoforged.net/news/26.1release/
- NeoForge transfer migration: https://neoforged.net/news/21.9-transfer-rework/

See [RELEASE_TARGETS.md](RELEASE_TARGETS.md) for configured release tracks and [API_TESTING.md](API_TESTING.md) for existing acceptance work.
