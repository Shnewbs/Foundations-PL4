# 26.x port status

Development tracks: main/mc/1.21.1 and mc/26.3. mc/26.1 and mc/26.2 are dormant scaffolds, not separate planned development tracks.

The mc/26.3 port starts from released R1.6, including editor tools, immediate cable geometry, energy conversion and pushed EU input.

Pinned development target: Minecraft 26.3, NeoForge 26.3.0.23-beta, ModDevGradle 2.0.147, Java 25, Gradle 9.2.1, following the official NeoForge 26.3 MDK.

Goal: a common 26.x artifact tested on 26.1, 26.2 and 26.3; 26.4 follows when available. Metadata remains restricted to 26.3 while the native port is being compiled. Broader compatibility must be demonstrated through actual startup, linkage, server and client tests, not inferred by widening version ranges.

Port CI is validation-only and cannot publish a release. The native source compiles and packages successfully. The baseline regression suite has passed on 26.3; an additional ore-drop fixture checks the migrated loot format. Runtime and source JARs are retained in CI artifacts. No public 26.x release is published yet.

Migrated systems include registry IDs, codec persistence, transactional item/fluid/energy transfers, hammer inventory and menus, recipe templates and data, world-generation features, loot modifiers, item model definitions, GUI extraction/input, block-entity render-state submission and native GameTest registration.

Acceptance still outstanding: an in-game client visual/control playthrough, installed external energy providers on 26.3, and explicit 26.1/26.2 compatibility checks. A successful server suite does not establish those claims.

Source: https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle
