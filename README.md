# Foundations PL4 1.6.4 experimental native preview

**0.2a-legacy-preview.2 is an item/fluid-transport subset, NOT full modern 0.2a parity. Further API testing is still required.**

Native implementation in `src/legacy`; `src/main` is excluded reference source. The compiler uses the available Forge 9.11.1.960 development API with Java 8 to produce Java 7 bytecode. The build runs with Java 21 / Gradle 9.5.0 / ForgeGradle 7.0.28. Native acceptance is performed separately on the actual published **Forge 9.11.1.1345 / Java 7** runtime; no 960 installer or universal JAR is assumed to exist. Check the release-attached scenario report for completed runtime evidence.

Includes craftable cables and export/import nodes, owner-partitioned loaded physical networks, native sided inventory handling, bounded scans/provider work, redstone pause and saved item-transfer escrow. Full-block host geometry is intentional for this initial legacy subset; it is not the modern multipart model.

Craft eight cables using iron/redstone/iron in a row. Craft exporter using hopper/cable/hopper; shapelessly convert exporter to importer or back. Connect same-owner exporters/importers through cables. Sneak-right-click a node face to choose the adjacent inventory. Right-click inspects the network; `/pl4legacy inspect x y z` is the command alternative. `/pl4legacy recipes [page]` reads actual registered recipes without NEI/Waila. This is not a complete graphical recipe viewer. Default 8 items per exporter per 10 ticks, 256 hosts and 256 slots; native configuration controls limits. Provider calls have a shared 1024-call operation budget.

Legacy block IDs default to 3500/3501/3502/3503/3504/3505 and are checked for collisions before registration. Ownership uses the normalized legacy player name, not the modern authenticated-account UUID contract. Use only isolated test worlds or trusted test servers; this preview is not an assurance that obsolete runtime/loader security or authentication is suitable for public servers.

Build: `bash gradlew clean build` with Java 21 plus a discoverable Java 8 compiler. Only the remapped `-srg.jar` is suitable for runtime installation; the publication workflow names the validated runtime without that classifier. Never install the native-scenarios fixture JAR in a real world. The native workflow installs Forge inside a disposable directory and executes 52 isolated scenarios on the remapped runtime; its test fixture includes the production class hierarchy when remapping inherited Minecraft methods.

Advanced displays, multipart placement, wireless storage, energy transport, complete scripting and installed third-party APIs remain unfinished. No arbitrary power conversions or mismatched newer mods are bundled as substitutes. Client visuals, multiplayer and modpack acceptance are pending. The 1.6.4 branch remains experimental even after its initial native scenarios pass.

## Fluid preview 2

Native fluid endpoints, a 16-bucket tank, vanilla bucket interactions and sealed fluid recovery cells are now included. Crafting and setup, quarantine behavior and limitations are in [the preview 2 release notes](docs/releases/0.2a-legacy-preview.2.md). Vanilla-texture full-block models remain preliminary. Native fluid handlers are supported; installed external mod compatibility is not yet certified.

## Verification-only follow-up

The first preview-2 runtime passed 51 scenarios, but the separate test mod left an unregistered external tank in the disposable world at shutdown. A final cleanup scenario now removes that fixture before saving, and the log guard rejects missing-mapping errors. The suite now requires 52 scenarios. This follow-up changes tests, tools and documentation only; published runtime tags and original release assets remain unchanged. Supplemental verification records the actual installed runtime hash and compares rebuilt archive entries with the published JAR.
