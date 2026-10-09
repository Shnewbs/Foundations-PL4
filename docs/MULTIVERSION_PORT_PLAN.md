# PL4 multi-version port and API stability plan

Updated: October 8, 2026 (America/Los_Angeles).

## Next-build direction

Expand Foundations PL4 to the important modded Minecraft version families, with separate source branches and independently tested loader/API combinations. This document records the next-build plan; it does not create branches, publish new builds, or certify compatibility. Finish the current release's outstanding work without replacing it with branch scaffolding.

Selection is based on relevant mod ecosystems and dependency availability, not a claim to a measured global popularity ranking. Recheck author-maintained release listings before pinning any dependency. An upstream release being available is not the same as continued upstream support or PL4 acceptance.

## Version targets and branch policy

Retain the existing names for established tracks. New names below are proposed Forge tracks; add a loader suffix if a second loader is later implemented for the same Minecraft version.

| Minecraft | Branch | Intended loader | Next-build priority / status |
|---|---|---|---|
| 1.21.1 | main, mirrored to mc/1.21.1 | NeoForge | Maintain the primary release baseline |
| 26.1.2 | mc/26.1.2 | NeoForge | Complete and maintain the existing parallel track |
| 26.3 | mc/26.3 | NeoForge | Complete and maintain the existing forward-port track |
| 1.20.1 | mc/1.20.1 | Forge | First additional port candidate |
| 1.19.2 | mc/1.19.2 | Forge | Additional established-version candidate |
| 1.18.2 | mc/1.18.2 | Forge | Additional established-version candidate |
| 1.16.5 | mc/1.16.5 | Forge | Additional established-version candidate |
| 1.12.2 | mc/1.12.2 | Forge | Dedicated legacy feasibility and implementation track |
| 1.7.10 | mc/1.7.10 | Forge | Dedicated legacy feasibility and implementation track |
| 26.4 | mc/26.4 | Verify available toolchain | Create when a usable target toolchain is verified; validate independently |

The existing mc/26.1 and mc/26.2 refs remain audit-only until their actual build settings and acceptance are verified. Do not relabel them as working ports or delete them as part of this plan. Evaluate other versions only when their mod ecosystems justify another maintained track; do not create every intervening point release automatically. No Fabric compatibility is inferred from a Forge or NeoForge port.

Current configured release tracks and naming are documented in [RELEASE_TARGETS.md](RELEASE_TARGETS.md). Preserve published tags and artifact names. A new branch starts as research/scaffold, not as supported. The first native build must use that target's actual Minecraft, loader, mappings, Java, Gradle, resource formats and APIs. Older tracks require deliberate language/API backports rather than lowering the version declaration on Java 21 source.

## Evidence for the initial shortlist

Create's author-maintained development table lists continued support for 1.21.1; available releases for 1.20.1, 1.19.2, 1.18.2 and 1.16.5; a 26.1 port in progress; and several intervening releases as skipped. That supports investigating these version families, but does not certify any PL4/Create pairing.

GregTech CEu Modern explicitly identifies Forge 1.20.1 and NeoForge 1.21.1+ in its project documentation. The separate GregTech CEu project targets the 1.12 generation. GTNH maintains its own 1.7.10 NEI continuation and modern-Java runtime work. These are distinct integration contracts, not interchangeable editions of one API.

## Per-target API and mod matrix

For every target, record the exact Minecraft patch, loader build, build JDK, runtime JDK, dependency version, source revision, checksum and tested feature surface. Record each result as NOT_AVAILABLE, NOT_TESTED, COMPILE_VERIFIED, INSTALLED_PASS, INSTALLED_FAIL or BLOCKED. Never convert a source-signature check or synthetic fixture into an installed-mod pass.

| Area | Candidate APIs/mods to audit | Required evidence |
|---|---|---|
| Base runtime | Target loader registries, packets, saved data, rendering, recipes, item/fluid/energy interfaces | Independent compilation, dedicated-server boot and native scenario tests |
| Power and machines | FE; legacy RF where applicable; Mekanism Joules; the appropriate GregTech EU API; IC2-specific EU where a maintained target exists; Electrodynamics/Voltaic | Real sided providers, simulation agreement, bounded transfer, explicit conversion policy and resource conservation |
| Mechanical and storage telemetry | Create and AE2 versions actually available for the target | Installed network lifecycle, unavailable/removed providers, deduplication and documented read-only versus transfer behavior |
| Recipe viewers | JEI and EMI where available; NEI for the relevant legacy track | Forging Hammer category, ingredients/results/counts, recipe reload and client-only isolation |
| Tooltips and scripting | Jade or target-appropriate Waila/HWYLA; KubeJS and CraftTweaker where available | Target-specific APIs, server-safe loading, documented examples and feature-specific tests |
| Stability and performance profiles | ModernFix and FerriteCore where supported; evaluate target-appropriate renderer and legacy fixes separately | Clean baseline comparison, one-mod-at-a-time tests and combined-profile tests; no universal compatibility claim |

Core PL4 must remain loadable without optional integration mods. A missing optional API disables only its adapter and is reported clearly. Do not bundle performance/stability mods as mandatory dependencies merely because they are in a test profile. Mod presence, readable telemetry, resource transfer, and power conversion are separate capabilities and must be reported separately.

Pin a verified loader/dependency baseline and test upgrade candidates in a separate CI lane before changing it. Include loader security advisories in that review: NeoForge's May 11, 2026 advisory identifies network-allocation fixes in 21.1.229 and 26.1.2.44-beta, and describes mitigation for older affected NeoForge installations. Do not generalize that advisory's remedy to unrelated loaders or every legacy version.

## Implementation sequence

1. Close the current release blockers on 1.21.1, 26.1.2 and 26.3. Preserve the source changes and tests already under development.
2. Define shared behavioral contracts for topology, placement, ownership, storage, transfer/escrow, display layouts and provider sampling. Keep loader and Minecraft adapters separate; share fixes through reviewed ports instead of merging whole version branches blindly.
3. Start 1.20.1 first. Establish the actual Forge toolchain, then migrate serialization, networking, capabilities, recipes and rendering. Create the other planned tracks with explicit research/scaffold status and evaluate their blockers independently.
4. On 1.19.2, 1.18.2 and 1.16.5, reproduce the same behavioral tests with the correct APIs. Treat 1.12.2 and 1.7.10 as separate legacy implementation work, including appropriate JVM bytecode and test infrastructure. Modern Java runtime compatibility for a legacy pack is not automatically compatibility with an unmodified legacy installation.
5. Install and exercise the selected real integration mods, first alone and then in each recorded combined test profile. Keep unavailable APIs explicit rather than substituting a mismatched build.
6. Publish each successful target's runtime JAR, sources and checksums to GitHub Releases with its exact Minecraft/loader identity. Submit to CurseForge using the matching target metadata; upload acceptance and moderation approval remain separate. Never publish renamed binaries for untested targets.

## Acceptance and rollout reporting

Maintain separate progress fields for source port, build, native scenarios, installed APIs, client visuals, multiplayer and performance. On versions without the modern GameTest framework, use equivalent automated dedicated-server scenarios and record that harness; do not claim modern GameTests ran there.

Required scenarios include packet replay and malformed requests; owner/permission changes; absent and replaced providers; empty/full/sided stores; component/NBT variants; simulation without mutation; restart with nonempty escrow; chunk unload/reload without forced loading; cable placement and geometry; display editing and GUI scaling; two-player interactions and reconnects; and sustained network tick/allocation profiling.

Successful automated gates may justify a clearly labelled alpha while manual acceptance remains open. A stable compatibility claim requires the corresponding installed-provider, real-client and multiplayer evidence. Keep the release notice **Further API testing is still required** wherever those checks remain pending. Report partial rollouts per target; a successful modern build does not complete the legacy ports or the full multi-version rollout.

## Author-maintained references reviewed

- Create development status: https://wiki.createmod.net/users/development-status
- GregTech CEu Modern: https://github.com/GregTechCEu/GregTech-Modern
- GregTech CEu Modern documentation: https://gregtechceu.github.io/GregTech-Modern/
- GregTech CEu legacy: https://github.com/GregTechCEu/GregTech
- GTNH NEI: https://github.com/GTNewHorizons/NotEnoughItems
- GTNH LWJGL3ify / modern Java: https://github.com/GTNewHorizons/lwjgl3ify
- ModernFix: https://github.com/embeddedt/ModernFix
- FerriteCore: https://github.com/malte0811/FerriteCore
- NeoForge network security advisory: https://neoforged.net/news/mitigating-vulnerabilities-network/
- NeoForge 26.1 migration background: https://neoforged.net/news/26.1release/
- NeoForge transfer migration background: https://neoforged.net/news/transfer-rework/
