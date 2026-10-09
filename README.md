# Foundations PL4 — Minecraft 1.18.2 / Forge

Development port **0.2a-port.2**, Forge **40.3.12**, Java **17**.

Native Forge source port, not a renamed NeoForge JAR. Retains multipart cables, readers, displays, network item storage, transfer escrow, ownership checks and the built-in field guide. Forge SimpleChannel networking and sided capabilities replace NeoForge interfaces. Native NBT preserves item and fluid variants on this Minecraft generation.

Build with `bash gradlew build runGameTestServer`. Java compilation is not client, multiplayer or installed-provider acceptance. **Further API testing is still required.** See BUILD_STATUS.json and docs/releases/0.2a-port.2.md.

Back up worlds; use matching client/server builds. Worlds and JARs are not interchangeable between Minecraft versions. Unavailable third-party power APIs remain disabled rather than using guessed units or conversion ratios.
