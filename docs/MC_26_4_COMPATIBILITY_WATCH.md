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
