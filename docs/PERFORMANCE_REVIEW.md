# Cable and sampling performance review

Reviewed against main commit de4c705 (0.0.2a.R1). This pass reduces repeated server-side work without changing sampling cadence, cable connectivity, save schema or packet format.

## Findings and changes

- **Idle standalone cable synchronization:** HostEntity previously serialized every persisted setting and synchronized field to NBT on every sampling interval, then compared the result with its previous tag. Now a cable-only host first checks part identity, status, signal and sampled rows. Matching state returns before serialization. Persistent edits, connection-arm updates, external leads and reloads invalidate the shortcut. Multipart hosts retain the full comparison path.
- **Transfer membership:** Each transfer cycle previously filtered the full group to find endpoints and sorted drivers again, including groups with thousands of cables. Membership and driver order are now prepared with the cached topology. Runtime transfer modes, resource settings, filters, permissions and capabilities remain live. Normal topology invalidation rebuilds membership/order when parts or priorities change. The direct-list transfer entry point remains available for fixtures and callers.
- **Inventory/fluid filters:** Each stack or tank comparison previously split and trimmed the filter and parsed its tag identifiers. A weak cache per Part now compiles those tokens once per filter-string change. Values retain neither the Part key nor world/capability objects. Tag membership is evaluated live, so data-pack reloads do not cache tag results. Whitelist inversion remains live; bare IDs retain their previous exact-ID semantics. Server shutdown clears this cache.
- **Cable status allocation:** A sampling group now builds its shared connection-status text once instead of constructing an identical string for each cable/Node/Array in the group.

## Existing protections retained

Topology is already cached and rebuilt after edits or relevant configuration changes. Connection meshes use cached block states. Cable shapes are cached until geometry changes. The planner uses indexed host cells, checks both ports, and keeps visual reader exports separate from resource-transfer networks. Sampling and transfer rates remain configurable; unloaded targets do not force-load chunks. Transfer simulation, escrow, ownership checks, priority, per-cycle budgets and receive fences are retained.

## Verification and limits

Four additional native GameTests exercise:

- 1,000 unchanged data-cable checks without building additional NBT snapshots, followed by edit, arm, lead, reload and multipart invalidation.
- 1,000 equivalent redstone samples without additional snapshots, while signal, row and error/status changes still synchronize.
- A prepared transfer plan with 4,096 irrelevant cable references, actual item conservation, and a changed live ADD/REMOVE mode.
- 2,000 item/tag matches using one compiled filter, plus whitelist, filter-string, exact-ID, fluid-ID, fluid-tag and blank-filter behavior.

These are operation-count and correctness checks, not FPS or milliseconds-per-tick benchmarks. Full Java 21 compilation, the offline regressions and native GameTests run in the PR workflow. A representative modpack/world profile is still needed to measure the real speedup. Cable-heavy networks should benefit most from synchronization and membership changes; filtered inventory/tank scans benefit from compiled filters. Client mesh batching and incremental graph rebuilding require separate profiling and broader rendering/topology work.
