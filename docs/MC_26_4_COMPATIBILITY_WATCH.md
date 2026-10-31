# Minecraft 26.4 / NeoForge compatibility watch — 2026-10-30

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
