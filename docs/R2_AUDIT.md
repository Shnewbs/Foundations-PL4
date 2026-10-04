# R2 correctness audit — 0.0.2a.R2.1

## Resolved findings

| Finding | Resolution | Executable coverage |
|---|---|---|
| A pin beyond the first 64 entries looked disconnected | Independent selected label; searched pages of 64 | 130 endpoints, last page, hidden pin, actual removal and saved aliases |
| Choice list could be stale when accepting edits | Validate against current server-owned network endpoints | Native topology suite plus edit rejection fixture |
| Repeated/combined storage inflated telemetry | Per-sample identity sets and canonical vanilla inventories | Actual paired chest contents/capacity |
| Old close handle could remove a new registration of the same callback | Unique entry token per registration | Close/re-register/close old handle |
| Registry copied its TreeMap on every sample | Immutable snapshot refreshed on registry edits | Production provider suite |
| 26.3 furnace keys were obsolete | Current save fields on 26.3; preserve 1.21.1 keys | Actual burning/smelting furnace |
| Info/network cleanup | Removed unused imports/counter/redundant condition; bounded diagnostic rows | Full compile and regression suites |

## Deliberate boundaries

The separate Minecraft branches contain necessary API differences, not competing runtime implementations. No broad refactor was made merely to reduce line count. There is no proof of zero bugs or zero redundancy across the entire mod.

Deduplication does not merge inventories based on equal contents. Distinct modded wrappers can overlap without advertising shared identity, and need provider-specific integration. Installed Create/AE2/Mekanism/GT/Electrodynamics acceptance, client visual checks and live two-player play have not been performed in this audit environment. Existing native conversion, rollback, placement, display-lighting, persistence and ownership regression suites remain release gates.

Furnace field migration reference: https://www.minecraft.net/en-us/article/minecraft-1-21-4-pre-release-2

The first native run exposed a faulty test assumption (64 items per slot) and a 26.3 adapter inconsistency: general slot capacity must be requested with ItemResource.EMPTY, not the currently stored item. The regression now sums the native general-capacity API directly and requires exact agreement after deduplication. STORAGE also skips redundant visual aggregation/serialization.
