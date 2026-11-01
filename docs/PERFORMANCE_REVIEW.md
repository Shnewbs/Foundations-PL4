# Cable and sampling performance review

Reviewed against main commit de4c705 (0.0.2a.R1). This pass reduces repeated server-side work and small client-render allocations without changing sampling cadence, cable connectivity, save schema or packet format.

## Findings and changes

- **Idle standalone cable synchronization:** HostEntity previously serialized every persisted setting and synchronized field to NBT on every sampling interval, then compared the result with its previous tag. Now a cable-only host first checks part identity, status, signal and sampled rows. Matching state returns before serialization. Persistent edits, connection-arm updates, external leads and reloads invalidate the shortcut. Multipart hosts retain the full comparison path.
- **Transfer membership:** Each transfer cycle previously filtered the full group to find endpoints and sorted drivers again, including groups with thousands of cables. Membership and driver order are now prepared with the cached topology. Runtime transfer modes, resource settings, filters, permissions and capabilities remain live. Normal topology invalidation rebuilds membership/order when parts or priorities change. The direct-list transfer entry point remains available for fixtures and callers.
- **Inventory/fluid filters:** Each stack or tank comparison previously split and trimmed the filter and parsed its tag identifiers. A weak cache per Part now compiles those tokens once per filter-string change. Values retain neither the Part key nor world/capability objects. Tag membership is evaluated live, so data-pack reloads do not cache tag results. Whitelist inversion remains live; bare IDs retain their previous exact-ID semantics. Server shutdown clears this cache.
- **Cable status allocation:** A sampling group now builds its shared connection-status text once instead of constructing an identical string for each cable/Node/Array in the group.

- **Render-loop allocations:** Cable arm rendering reuses one direction array, external leads select cached models by Kind/material instead of constructing string keys per frame, and reader skins locate their paired panel directly in the 13-slot host map. Existing compact-pair GameTests cover the same six-facing pairing behavior.

## Existing protections retained

Topology is already cached and rebuilt after edits or relevant configuration changes. Connection meshes use cached block states. Cable shapes are cached until geometry changes. The planner uses indexed host cells, checks both ports, and keeps visual reader exports separate from resource-transfer networks. Sampling and transfer rates remain configurable; unloaded targets do not force-load chunks. Transfer simulation, escrow, ownership checks, priority, per-cycle budgets and receive fences are retained.

## Verification and limits

Four additional native GameTests exercise:

- 1,000 unchanged data-cable checks without building additional NBT snapshots, followed by edit, arm, lead, reload and multipart invalidation.
- 1,000 equivalent redstone samples without additional snapshots, while signal, row and error/status changes still synchronize.
- A prepared transfer plan with 4,096 irrelevant cable references, actual item conservation, and a changed live ADD/REMOVE mode.
- 2,000 item/tag matches using one compiled filter, plus whitelist, filter-string, exact-ID, fluid-ID, fluid-tag and blank-filter behavior.

These are operation-count and correctness checks, not FPS or milliseconds-per-tick benchmarks. Full Java 21 compilation, the offline regressions and native GameTests run in the PR workflow. A representative modpack/world profile is still needed to measure the real speedup. Cable-heavy networks should benefit most from synchronization and membership changes; filtered inventory/tank scans benefit from compiled filters. Client mesh batching and incremental graph rebuilding require separate profiling and broader rendering/topology work.


## October 31, 2026 — isolated native benchmark world preservation

The independent `tools/benchmarks/run_native_comparison.py` runner is **not** a production performance result. It intentionally creates disposable Minecraft `run` worlds in separate clean checkouts and must never delete an unrelated or subsequently changed world. Prior cleanup relied on a source/trial marker alone; that marker remained valid even if a user or another tool added a world or edited a file after a trial.

The runner now records a SHA-256 inventory of generated directories, relative paths, file sizes and file contents when a native trial exits. Before removing the prior trial's `run` directory, it requires both the exact source/trial marker and an unchanged complete inventory. It refuses nested symbolic links, special files, unreadable directories, new world files, modified saves and malformed markers. Any refusal preserves the runtime for manual inspection. The existing clean-checkout, nonexisting output, and preexisting-world checks remain mandatory.

Five new repository regression methods cover added worlds, modified saves, nested links, invalid markers and unreadable inventory traversal. GitHub source readback passed 14/14 contract checks; eight local isolated scenarios using the same helper logic passed. **The committed repository test module, Gradle native probe and TPS/heap benchmark have not run in this container.** These protections are not evidence of a speedup or RC qualification; exact-source native comparison remains pending on a dependency-capable external builder.


## November 1, 2026 — fixed-heap native comparison acceptance

The isolated 1.21.1 native comparison now requires the **GameTest server JVM's measured initial heap to equal its measured maximum heap** on every trial, in addition to matching JVM vendor, VM name, collectors, Java version, CPU count, architecture and heap limits across revisions. Two identical *auto-sized* heap profiles are not evidence of a controlled benchmark, particularly as Minecraft 26.4 snapshots change default heap sizing. Configure both GameTest JVMs with equal explicit `-Xms` and `-Xmx` (for example, `-Xms2g -Xmx2g`) and verify that the resulting native JSON reports those exact values; setting Gradle's heap alone does not prove the forked Minecraft JVM is pinned.

A mismatch fails **before** a trial JSON is accepted, before the raw native report is removed, and before the generated world is marked for cleanup. The new regression cases cover equally unpinned baseline/candidate environments and preservation of the world and original report. These are release-gate source changes, **not** measured TPS/heap performance or native RC clearance. The committed regression module and Gradle GameTests still require execution on a dependency-capable builder.
