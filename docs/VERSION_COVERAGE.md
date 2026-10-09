# Upstream-gap coverage policy

User-directed scope, October 9, 2026: Foundations PL4 must cover the Minecraft versions Practical Logistics did not reach, not only the initial popular-version shortlist. **1.18.2 is explicitly included; 1.16.4 is its own target, not shorthand for 1.16.5.** This supersedes shortlist-only wording in MULTIVERSION_PORT_PLAN.md. It does not mean all gaps already have working builds.

## Upstream evidence and boundaries

The original author's Practical Logistics file page lists 1.7.10. Practical Logistics 2 lists final versions 1.9.4, 1.10, 1.10.1, 1.10.2, 1.12, 1.12.1 and 1.12.2, plus snapshot tags. These are published file-version labels, not certifications of ongoing support, feature parity or every unofficial fork. A version missing from these original project listings is an upstream coverage gap for this audit.

The requested exclusion of 1.7.10 remains. The specifically approved 1.6.4 track remains experimental. Retain the existing 1.12.2 work even though PL2 has an upstream release for it; do not discard user-approved ports. Snapshot and pre-release targets are not interchangeable with final Minecraft releases. A future 26.4 target still requires verified toolchain availability.

## Immediate implementation and existing coverage

| Target | Current action / evidence |
|---|---|
| 1.16.4 | New `mc/1.16.4` branch; independently target Forge 35.1.37, exact Minecraft 1.16.4 metadata and distinct payload identity. Native build/scenario acceptance required before release. The inherited full-feature source currently requires Java 17; ordinary installed-launcher/client acceptance is separate. |
| 1.16.5 | Retain `mc/1.16.5` and its existing `0.2a-port.1` release; it is not a substitute for 1.16.4 verification. |
| 1.18.2 | Retain `mc/1.18.2`, published `0.2a-port.2`, and the 193-test evidence in PORT_IMPLEMENTATION_STATUS.md. The runtime release was checked again in this scope correction; no new 1.18.2 runtime change is claimed. |
| 1.19.2, 1.20.1 | Retain the independently built Forge ports and their acceptance records. |
| 1.21.1, 26.1.2, 26.3 | Retain separate NeoForge targets, native tests and exact-target release identities. |
| 1.12.2, experimental 1.6.4 | Preserve ongoing legacy functionality work and existing preview releases. Legacy subsets must not be described as complete modern PL4 parity. |

## Remaining gap backlog - not completed ports

Prioritize the additional anchor releases **1.8.9, 1.11.2, 1.13.2, 1.14.4, 1.15.2 and 1.17.1**, alongside the explicit 1.16.4 request. Their original-project coverage gap is established, but their PL4 implementations and API/stability combinations are not yet completed.

Do not silently omit other final point releases. Audit the intervening 1.7.2, 1.8/1.8.8, 1.9-family gaps, 1.11, 1.13, 1.14.1-1.14.3, 1.15/1.15.1, 1.16.1-1.16.3, 1.17, 1.18/1.18.1, 1.19/1.19.1/1.19.3/1.19.4, remaining 1.20.x and 1.21.x patches, and 26.x gaps against exact official Minecraft and loader metadata. This is an audit queue, not an assertion that Forge/NeoForge published a toolchain for every patch. Versions without a suitable toolchain require an explicit BLOCKED/ALTERNATIVE_LOADER_RESEARCH status rather than a relabelled neighboring JAR.

Existing `mc/26.1` and `mc/26.2` refs remain audit-only until their actual build settings and acceptance are verified. A Git ref alone is not release coverage. Other planned branches must likewise remain PORTING/NOT_TESTED until their own compilation, runtime, resources and behavioral tests pass.

## Required functionality and optional API alternatives

Each target must eventually deliver the cable/network, reader/display, storage, item/fluid/energy routing, ownership/save, recipe lookup, inspection and configuration behaviors appropriate to the project milestone. Passing a transport subset does not complete the full port.

For every optional integration, record the exact target-compatible provider, actual implemented feature, and installed-mod test status. When a provider API is absent, include a compatible alternative or PL4-owned implementation for core recipe lookup, inspection, storage and configuration. A command fallback is explicitly a command fallback, not graphical JEI/NEI/Jade parity. A configuration file or Java extension point is not a complete scripting replacement. Do not report absent power units as converted or infer RF/EU/J compatibility from a working FE/internal buffer. Stability mods are optional test profiles, not mandatory bundled dependencies.

Maintain separate statuses for SOURCE_PORT, NATIVE_BUILD, SCENARIOS, INSTALLED_RUNTIME, INSTALLED_APIS, CLIENT_VISUALS, MULTIPLAYER, PERFORMANCE and RELEASE. A successful neighboring version or unavailable dependency does not satisfy the missing target's gate. Preserve matching runtime/source/checksum assets on GitHub Releases. **Further API testing is still required** wherever installed integration acceptance is incomplete.

## Reference pages reviewed

- Original Practical Logistics files: https://www.curseforge.com/minecraft/mc-mods/practical-logistics/files/all
- Original Practical Logistics 2 files: https://www.curseforge.com/minecraft/mc-mods/practical-logistics-2/files/all
- Forge exact-version index, including 1.16.4 / 35.1.37: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.16.4.html
- Existing 1.18.2 release: https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.18.2-v0.2a-port.2
- Per-release evidence: PORT_IMPLEMENTATION_STATUS.md
