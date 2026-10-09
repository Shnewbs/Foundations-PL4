# Foundations PL4 1.12.2 native legacy preview

**0.2a-legacy-preview.1 is an initial item-transport subset, NOT full modern 0.2a parity. Further API testing is still required.**

Native Forge 14.23.5.2864 / Java 8 implementation in `src/legacy`. `src/main` is excluded modern reference source only. Never relabel its binaries as legacy builds.

Includes craftable owner-separated cable/export/import hosts, loaded physical network traversal, sided native item capabilities and inventory-wrapper alternatives, redstone pause, configurable rates/scan limits, and NBT-persisted transfer escrow. Inventory provider calls share a 1024-call budget across destination scans.

Built-in `/pl4legacy recipes [page]` lists actual registered PL4 recipes. `/pl4legacy inspect x y z` and right-click provide owner-checked inspection without JEI, NEI or Waila. This is a command fallback, not a complete graphical recipe viewer. Use a bound node's face to select adjacent inventory by sneak-right-clicking that face.

Craft cable using iron/redstone/iron in a row; craft exporter using hopper/cable/hopper. Shapelessly convert exporter to importer or back. Connect exporters and importers through cables belonging to the same player. Defaults: 8 items per exporter every 10 ticks; 256 network hosts and 256 scanned inventory slots. Powered nodes pause.

Build: `bash gradlew clean build` using Java 8. Native scenario run: `bash gradlew clean build runServer -PlegacyScenarios` only in the workflow-created disposable test world. Test fixtures never belong in a production mod folder.

Advanced displays, multipart placement, wireless storage, fluid/energy transport, complete scripting and installed third-party API integration remain pending. Real-client visuals, multiplayer and modpack acceptance also remain pending. No arbitrary energy conversion is supplied and no performance mods are bundled.

The new `legacy_*` block IDs and legacy schema 1 are not a migration path for modern worlds. Use a fresh test world or backups. Publication is gated by a native build, reobfuscation, portable rule tests and 21 isolated native scenarios; these are not the modern port's 191 GameTests.
