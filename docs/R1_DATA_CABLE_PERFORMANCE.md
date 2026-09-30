# 0.0.2a.R1 - data cable topology review

## Change

Network groups now build their stable processing order, distinct target list, and reader list once during topology rebuild. Sampling reuses those immutable lists instead of sorting every group's parts and rebuilding target/reader collections on every sample interval. Part priority, links, port masks, placement, load/unload, and configuration changes continue to invalidate topology through the existing host lifecycle.

This removes recurring `O(P log P)` sorting and per-sample list/set allocation from each group, where `P` is the group's part count. Provider sampling, entity scans, and resource transfers remain dynamic and are not cached.

## Review findings

- Data and redstone cables remain separated by the topology planner's compatibility check.
- Cable links require both facing ports enabled; device attachments and exposed back-cable leads retain their existing obstruction checks.
- Topology scans only tracked loaded hosts and checks chunk-loaded state; it does not force-load chunks.
- No additional functional cable defect was confirmed by source review. Existing native GameTests cover disabled-port disconnection, cable-family separation, stale collision arms, unloaded-host invalidation, and unchanged-topology reuse.

This is a source-level optimization, not a claim of measured TPS/MSPT or memory improvement. Profile large networks on a dedicated server before setting a performance budget.
