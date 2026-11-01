# Minecraft 26.4 / NeoForge compatibility watch — 2026-10-31

This is a **readiness watch**, not a new PL4 maintained target, build, compatibility claim, or release clearance. The existing 24 exact targets and their branches remain unchanged.

## Official observations

- Mojang's [Minecraft Java 26.4 Snapshot 6](https://www.minecraft.net/fi-fi/article/minecraft-26-4-snapshot-6), published **October 27, 2026**, changes data-pack version to **125.1**, resource-pack version to **101.0**, adds fluid tag `#minecraft:swimmable`, and updates world-generation density functions. PL4's fluid compatibility needs to preserve exact fluid identity and never assume new tags imply a compatible capability.
- Mojang's [Snapshot 5](https://www.minecraft.net/da-dk/article/minecraft-26-4-snapshot-5), published **October 20, 2026**, changes the **login encryption/compression handshake** to explicit client acknowledgements. When a supported loader exists, authenticated client/server packet tests must be rerun; 26.3 network receipts cannot qualify 26.4.
- Mojang's [Snapshot 1](https://www.minecraft.net/da-dk/article/minecraft-26-4-snapshot-1), published **September 22, 2026**, makes **Vulkan the default graphics API**. Client UI, cable and reader rendering require separate Vulkan screenshots and OpenGL fallback review, not inferred acceptance.
- The [official NeoForged Maven version index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) checked **October 30, 2026** lists 26.3 versions through `26.3.0.99` but **no 26.4 build**. This is a point-in-time Maven listing, not proof a loader will never exist. The 26.4 snapshot line is not a stable release.

## Integration and gates

1. Keep 26.4 **unpublished and outside the 24-target RC ledger** until an exact supported Minecraft/NeoForge coordinate exists. Never rename a 26.3 JAR as 26.4 or auto-promote a snapshot.
2. On first official loader availability, create or update an exact version branch only under the owner's branch policy; pin Java 25, exact Minecraft and loader, data/resource pack formats, and protocol. Do not inherit 26.3 native hashes.
3. Add tests for login handshake, fluid capability/tag isolation, provider no-force-load, client rendering under Vulkan, world migration, multiplayer and sustained TPS/heap.
4. Continue current 24 exact-version acceptance work independently; this watch does not clear any existing target or authorize a release.

## October 31 recheck

The [official NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge) was checked again on **October 31, 2026 (UTC)**: no `26.4` build entry was present. Mojang's [October 27 game-drop testing overview](https://www.minecraft.net/en-us/article/drop-4-2026-testing) continues to identify the fourth 2026 drop as preview/snapshot testing. No 26.4 source port, target count change, binary compatibility claim, or release is authorized. Existing snapshot-specific handshake, Vulkan, fluid-tag, and worldgen acceptance gates remain mandatory.

## October 31 fluid-state API migration gate (Snapshot 4)

Mojang's [26.4 Snapshot 4](https://www.minecraft.net/en-us/article/minecraft-26-4-snapshot-4), published **October 13, 2026**, removed the separate `flowing_water` and `flowing_lava` fluid types and added a Boolean `source` fluid-state property alongside the `level` integer. A future exact 26.4 PL4 port must resolve fluid identity from the new state model, test source versus flowing blocks and capability adapters, and verify persisted fluid identifiers and migration before acceptance. Do **not** infer compatible fluid storage/transfer behavior from `#swimmable` alone. Rechecked the [official NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) on October 31: no `26.4` coordinate appears. Snapshot 6 (October 27) remains the latest listed official snapshot; no branch, binary, or release created.

## October 31 protocol watch — Snapshot 1 mod list and intent parameters

Mojang's [26.4 Snapshot 1](https://www.minecraft.net/da-dk/article/minecraft-26-4-snapshot-1) adds a `minecraft:mod_list` configuration-phase payload and permits URI-style parameters in the serverbound `minecraft:intention` host field. These values are **diagnostic/routing inputs, not authentication**; Mojang explicitly warns that the intent packet is unencrypted. On an actual supported 26.4 NeoForge port, validate PL4 channel/mod-list negotiation across vanilla, PL4-only and mismatched mod clients; reject unknown or malformed routing parameters without trusting them for owner identity, permission or secret-bearing tokens. Repeat the Snapshot 5 acknowledged login-handshake tests under authenticated multiplayer. This is a future exact-version acceptance requirement only: the official Maven index still had **no 26.4 loader** at the October 31 check, and no 26.4 branch/JAR/release is created.

## October 31 08:05 UTC recheck

The [official NeoForge Maven directory](https://maven.neoforged.net/releases/net/neoforged/neoforge/) still contains `26.3.0.99` and no `26.4` coordinate; Mojang's [Snapshot 6](https://www.minecraft.net/fi-fi/article/minecraft-26-4-snapshot-6) from October 27 remains the latest listed snapshot. No new exact port or release is authorized; preserve the prior Vulkan, fluid-state, and authenticated handshake gates.

## October 31 latest compatibility recheck

The [official Mojang snapshot list](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) still tops out at **26.4 Snapshot 6 (October 27)**; the [NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) contains no `26.4` coordinate. Preserve Vulkan, fluid-source, mod-list, handshake and migration gates. No new target, branch or release is authorized.

## October 31 11:06 UTC — default-heap performance comparability gate

Mojang's [26.4 Snapshot 6](https://www.minecraft.net/fi-fi/article/minecraft-26-4-snapshot-6) (October 27) now chooses the game's default Java heap from system RAM (up to 25%, capped at 4 GB). For any future exact 26.4 PL4 performance acceptance, **pin explicit `-Xms`/`-Xmx` and report heap, GC pauses, server TPS and JVM version**. Do not compare an auto-sized 26.4 run against a fixed-heap 26.3 baseline or treat a changed default as a PL4 regression. The [official NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) still has **no 26.4 loader entry** at the October 31 check; no 26.4 port or binary is authorized.

## October 31 09:02 PDT — official recheck

The [Mojang snapshot index](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) still lists **26.4 Snapshot 6** as the newest Java snapshot; the [NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) contains **no `26.4` coordinate**, with `26.3.0.99` present. No 26.4 branch, binary or release is authorized. Keep exact NeoForge Minecraft dependency metadata for existing ports and retain the Vulkan, fluid-source/tag, handshake, migration and controlled-heap gates for future 26.4 acceptance.


## October 31 11:05 PDT — official loader and snapshot recheck

The [Mojang Java snapshot index](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) continues to list **26.4 Snapshot 6** (October 27) as newest. The [NeoForge Maven release index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) contains `26.3.0.99` but **no 26.4 coordinate**. Maintain the existing 24 exact targets and do not create a 26.4 branch, publish a JAR or assert compatibility. Keep the documented Vulkan/OpenGL, source/flowing-fluid state, login-handshake/mod-list, migration and pinned-heap acceptance gates; they must be exercised with a real supported loader when available.


## October 31 12:05 PDT — Java 25 classfile packaging preflight

The [Mojang Java snapshot index](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) still lists **26.4 Snapshot 6**, dated **October 27**; the [official NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) still lists **no 26.4 loader**. A shared direct-publisher preflight now checks PL4 classfile headers and rejects bytecode newer than the target Java runtime (26.x: Java 25, class major 69; 1.21.1: Java 21, major 65). This is forward-compatible packaging validation, **not** a 26.4 port or proof of 26.4 compatibility. Retain the Vulkan/OpenGL, fluid-source, handshake, migration and pinned-heap gates before any future 26.4 acceptance.

## October 31 14:05 PDT — compatibility recheck and artifact provenance

The [official Java snapshot changelog index](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) still lists **26.4 Snapshot 6 (October 27)** as the latest Java snapshot. The [official NeoForge Maven directory](https://maven.neoforged.net/releases/net/neoforged/neoforge/) still contains no `26.4` loader coordinate. The direct upload path now binds inspected JAR bytes to the upload and receipt, but this is a shared packaging safety improvement only: it does not create a 26.4 port or qualify any 26.4 binary. Continue the 24 existing targets and preserve Vulkan, fluid-state, authenticated login, world migration, Java 25 and fixed-heap performance gates.

## October 31, 17:05 PDT — official compatibility recheck

The [official Mojang Java snapshot index](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) still lists **26.4 Snapshot 6 (October 27)** as the latest. The [official NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) contains `26.3.0.99` and **no 26.4 loader**. Preserve existing 24 exact targets; no 26.4 branch, release JAR or compatibility clearance. Future 26.4 work still requires Java 25, Vulkan/OpenGL, fluid-source and tag migration, login handshake/mod-list, world migration and pinned-heap native tests. The benchmark world-preservation fix this cycle is shared development tooling only, not a 26.4 port.

## October 31, 18:05 PDT — loader/snapshot recheck and bounded publisher input

The [official Mojang snapshot index](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) continues to list **26.4 Snapshot 6 (October 27)** as latest; the [official NeoForge Maven directory](https://maven.neoforged.net/releases/net/neoforged/neoforge/) still lists `26.3.0.99` and no `26.4` coordinate. A shared publisher input-size safeguard was added this cycle, but it does not establish 26.4 compatibility or permit a 26.4 release. Maintain the 24 existing exact targets and future Vulkan/OpenGL, fluid-source/tag, login handshake, world migration, Java 25 and pinned-heap native acceptance requirements.

## October 31 20:02 PDT — exact-target direct publication gate

Mojang's [26.4 Snapshot 6](https://www.minecraft.net/en-us/article/minecraft-26-4-snapshot-6) (October 27) remains the latest identified official Java snapshot, and the [NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) lists `26.3.0.99` but no `26.4` loader. The modern direct CurseForge uploader now explicitly admits only the 22 currently approved non-legacy release tracks, including `26.1.2` and `26.3`; unsupported `26.4` and adjacent 1.x versions cannot be published by merely presenting internally consistent JAR metadata. The two published legacy tracks `1.6.4` and `1.12.2` retain their separate publisher requirements. This is a release-safety source change, **not** 26.4 runtime compatibility or native RC acceptance. [Evidence](acceptance/rc-dev-direct-upload-exact-ledger-gate-2026-10-31-2002-pdt.json).

## October 31, 21:03 PDT — official 26.4 recheck and native benchmark report sealing

The [official Mojang Java snapshot index](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) still lists **26.4 Snapshot 6 (October 27)** as the latest; the [official NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) lists **26.3.0.99** and **no 26.4 loader** at this check (November 1, 04:03 UTC). The existing 24-target RC ledger and branch policy remain unchanged. The native 1.21.1 performance benchmark now removes and preserves its generated probe report **before** sealing the run/world inventory, so a report written under `run/verification-logs` does not invalidate the next trial's cleanup authorization. This is a shared benchmark reliability fix, **not** 26.4 runtime compatibility, a native performance measurement, or RC acceptance. Keep Vulkan/OpenGL, fluid-source/tag migration, mod-list and authenticated handshake, Java 25, world migration and pinned-heap/TPS gates for a future supported 26.4 port. [Acceptance evidence](acceptance/rc-dev-native-benchmark-report-seal-order-2026-10-31-2103-pdt.json).

## October 31 22:07 PDT — bundled-class bytecode gate and official recheck

The [official Minecraft Java 26.4 Snapshot 6](https://www.minecraft.net/en-us/article/minecraft-26-4-snapshot-6), published **October 27, 2026**, remains the latest identified Java snapshot. The [official NeoForge Maven directory](https://maven.neoforged.net/releases/net/neoforged/neoforge/) still ends at `26.3.0.99` and has **no 26.4 loader coordinate** at this check. Do not create a 26.4 port, branch, release, or compatibility claim; preserve the existing exact-version ledger and Vulkan, fluid-state, login-handshake, world-migration and fixed-heap acceptance gates.

The direct CurseForge publisher now validates classfile versions for **all target-JVM-selectable classes in the JAR**, not only `net/foundations/pl4/`. This catches bundled/shaded helper classes compiled for a newer Java version than the exact Minecraft target. Multi-release entries newer than the target Java runtime are ignored because that JVM cannot select them; active entries are checked. Java 8's ignored root `module-info.class` is also exempt. Seven new repository regression methods cover this policy; **7/7 isolated mirrored scenarios passed and GitHub source readback passed**, but the committed repository suite, native builds and GameTests were **not executed**. This packaging guard is not evidence of 26.4 runtime compatibility or release clearance.

## October 31 23:06 PDT — official 26.4 watch and receipt preservation

The [Mojang Java snapshot index](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) still lists **26.4 Snapshot 6**, released **October 27, 2026**, as the newest Java snapshot. The [official NeoForge Maven directory](https://maven.neoforged.net/releases/net/neoforged/neoforge/) still contains **26.3.0.99** and **no 26.4 loader**. Snapshot 6's [data-pack 125.1 / resource-pack 101.0 and #swimmable fluid-tag changes](https://feedback.minecraft.net/hc/en-us/articles/49936802465805-Minecraft-Java-Edition-26-4-Snapshot-6) remain future compatibility tests, not permission to publish a 26.4 port. The existing 24-target RC ledger and exact-version branch policy remain unchanged.

The direct CurseForge uploader now creates its post-API receipt **exclusively** (no overwrite, private file mode and file synchronization), rejects preexisting symlink receipts, and requires manual destination reconciliation if another writer creates a receipt during upload. This protects local publication evidence but **does not** prove CurseForge moderation/public visibility. Four repository regression tests were committed; 15/15 GitHub source-contract checks and three local isolated receipt-helper scenarios passed. The committed repository suite, native builds, installed-provider tests, multiplayer, migration and TPS/heap acceptance were not executed because this container lacks GitHub/Gradle DNS access, cached dependencies and Java 25. No 26.4 branch, release JAR, upload, or deletion occurred.


## November 1, 2026 00:03 PDT — official watch and concurrent publication guard

- The [official Minecraft Java snapshot changelog index](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) still identifies **26.4 Snapshot 6** (published **October 27, 2026**) as the newest listed Java snapshot. Its [official changelog](https://www.minecraft.net/en-us/article/minecraft-26-4-snapshot-6) remains a preview, not a stable target.
- The [NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) checked November 1 still ends at **26.3.0.99**, with **no 26.4 loader entry**. No new port, branch, compatibility claim, or release was authorized.
- Direct CurseForge upload now uses an exclusive **pre-API reservation** as well as the existing exclusive post-API receipt. Concurrent publishers cannot both call the API when using this version of the uploader. An uncertain API response deliberately retains the reservation until a human reconciles CurseForge and the local receipt; never blindly remove it or retry.
- Three repository regression methods were committed; four **isolated mirrored Python reservation checks** passed. The committed repository suite and native GameTests **were not run** due to unavailable GitHub/Gradle DNS, Gradle dependencies and Java 25. No production artifact was published or deleted; RC publication clearance remains **0/24**.

## November 1, 2026 — official recheck and exact NeoForge dependency gate

Mojang's [26.4 Snapshot 6](https://feedback.minecraft.net/hc/en-us/articles/49936802465805-Minecraft-Java-Edition-26-4-Snapshot-6), dated **October 27**, remains the latest identified Java snapshot. The [official NeoForge Maven directory](https://maven.neoforged.net/releases/net/neoforged/neoforge/) was checked on **November 1** and still contains `26.3.0.99` but **no `26.4` coordinate**. No 26.4 branch, build, or compatibility clearance is authorized.

Shared direct-publication validation now compares the bundled NeoForge dependency range with reviewed exact-branch metadata for **1.21.1, 26.1.2, and 26.3**, and requires both loader and Minecraft dependencies to be mandatory. This prevents a wrongly packaged 26.3 JAR from passing simply because its Minecraft dependency is exact. The change is source-only and **does not** establish a 26.4 port or any RC clearance. Retain the future 26.4 Vulkan/OpenGL, fluid source-state, login handshake, migration, Java 25, and pinned-heap acceptance gates.

## November 1, 2026 02:05 PST — official snapshot/loader recheck

The [official Mojang Java snapshot list](https://feedback.minecraft.net/hc/en-us/sections/360002267532) still ends at **Minecraft 26.4 Snapshot 6 (October 27, 2026)**. The [official NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) still has **no 26.4 loader coordinate**; `26.3.0.99` is listed. Maintain the existing 24 exact targets and branch policy, with **no 26.4 release, new port branch, or compatibility claim**. Preserve the previously identified fluid-state/source, Vulkan, login-handshake, mod-list, world-migration and fixed-heap benchmark gates. This check does not qualify existing targets or authorize publication.

## November 1, 2026 03:03 PST — official 26.4 check and benchmark provenance

Mojang's [Minecraft Java 26.4 Snapshot 6](https://www.minecraft.net/en-us/article/minecraft-26-4-snapshot-6) (published **October 27**) remains the latest identified snapshot. The [official NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) still lists **26.3.0.99** and **no 26.4 loader coordinate**. No 26.4 branch, JAR, release or compatibility claim is authorized; existing 24 target tracks and branch policy are unchanged. Preserve the Java 25, Vulkan/OpenGL, fluid source-state/tag, authenticated handshake/mod-list, migration and pinned-heap performance gates.

The 1.21.1 native benchmark runner now rejects reports outside its two generated `verification-logs` locations and reports reached through linked directories **before** reading or deleting them. This protects unrelated world/report evidence when recursive report discovery encounters unexpected paths. Two repository tests were added; **9/9 committed source/test contract checks** and **6/6 isolated mirrored Python scenarios** passed. The actual repository unittest, Gradle/native benchmark and release acceptance **did not run** due to local DNS/dependency/JDK limitations. This tooling correction is **not** a 26.4 port or a measured performance gain. [Acceptance evidence](acceptance/rc-dev-native-report-provenance-2026-11-01-0303-pst.json).


## November 1, 2026 04:07 PST — official watch and benchmark report selection

The [official Mojang Java snapshot index](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) still lists **26.4 Snapshot 6 (October 27)** as the newest Java snapshot. The [NeoForge official Maven directory](https://maven.neoforged.net/releases/net/neoforged/neoforge/) was rechecked on November 1: **26.3.0.99** remains present and there is **no 26.4 loader coordinate**. No 26.4 source branch, compatibility claim, binary, release or addition to the 24-target ledger is authorized. Continue preserving the exact Java 25, Vulkan/OpenGL, fluid-state/tag, authenticated handshake, world migration and pinned-heap/TPS acceptance gates.

The native 1.21.1 performance comparison runner now looks only in its two generated report directories, rather than recursively scanning unrelated saved worlds or checkout folders. Both exact locations, duplicate/missing reports and symlink rejection are covered by seven new repository regression methods. **12/12 GitHub source-contract checks and 7/7 isolated local helper scenarios passed**, but the committed repository tests and native benchmark were **not executed**. This is benchmark reliability hardening, not a measured performance improvement or RC clearance. [Evidence](acceptance/rc-dev-native-probe-exact-report-selection-2026-11-01-0407-pst.json).

## November 1, 2026 05:05 PST — official 26.4 recheck and stale-probe isolation

The [official Mojang Java snapshot index](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) continues to list **26.4 Snapshot 6**, published **October 27**, as the latest Java snapshot. The [official NeoForge Maven release index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) contains **26.3.0.99** and **no 26.4 loader coordinate** as of this check. Keep 26.4 outside the existing 24-target release ledger; no branch, JAR, runtime compatibility claim or RC clearance is authorized. Future 26.4 acceptance still requires exact Java 25/NeoForge coordinates, Vulkan/OpenGL, source-state and fluid-tag migration, authenticated handshake/mod-list, world migration and pinned-heap native performance testing.

The shared 1.21.1 native performance runner now rejects **pre-existing `verification-logs` directories** and **pre-existing `ReleasePerformanceGameTests.java` fixtures** in either disposable checkout before modifying either checkout. Gitignored stale reports cannot masquerade as a fresh native measurement, and existing fixture sources cannot be overwritten by probe injection. Four repository regressions were added; eight committed-source checks and six isolated mirrored Python scenarios passed, but the actual repository unittest and Gradle/native comparison did **not** execute. This change is benchmark provenance hardening, not a measured performance improvement or 26.4 support. [Evidence](acceptance/rc-dev-benchmark-preexisting-probe-safety-2026-11-01-0505-pst.json).

## November 1, 06:09 PST — official compatibility recheck

The [official NeoForge Maven index](https://maven.neoforged.net/releases/net/neoforged/neoforge/) was checked on **November 1, 2026**: it lists `26.3.0.99` and **no `26.4` loader coordinate**. Mojang's latest identified [26.4 Snapshot 6](https://www.minecraft.net/fi-fi/article/minecraft-26-4-snapshot-6) was published **October 27, 2026**. Preserve the exact 24-target RC ledger and existing branch policies; do not create a 26.4 branch or publish a binary until a supported loader and exact target are approved. Continue carrying forward the Vulkan/OpenGL, fluid-source/tag, configuration/login-handshake, world-migration and explicitly pinned-heap benchmark gates.

## November 1, 2026 07:10 PST — JVM comparability and official version recheck

The [official Minecraft Java snapshot list](https://feedback.minecraft.net/hc/en-us/sections/360002267532-Snapshot-Information-and-Changelogs) still identifies **26.4 Snapshot 6 (October 27)** as the latest snapshot. The [official NeoForge Maven release directory](https://maven.neoforged.net/releases/net/neoforged/neoforge/) lists `26.3.0.99` but **no 26.4 loader**. No 26.4 source branch, JAR, compatibility claim, or release is authorized.

The 1.21.1 native performance comparison now records and requires identical Java runtime, OS, architecture, available processors, initial heap and maximum heap for every interleaved baseline/candidate trial. It rejects changed runtime conditions **before** persisting a timing report or sealing/deleting a generated world. This reduces misleading performance comparisons; it is **not** a 26.4 port, native performance result or RC clearance. Snapshot 6's dynamic default heap makes explicit `-Xms`/`-Xmx` and this runtime comparability gate especially important when a real 26.4 NeoForge loader becomes available. Retain all prior Vulkan/OpenGL, fluid-state, authenticated handshake, migration, and exact-target validation gates.
