# Foundations PL4 1.19 — Forge41 source port

This branch independently targets **Minecraft 1.19 / Forge 41.1.0 / Java 17**
using the separately tested full-feature 1.19.1 source. It is not a
relabelled 1.19.1 JAR. The full multipart network, storage, displays/editor,
energy and fluid routing, ownership and saved transfers remain in source.

A playable alpha needs independent Forge41 compilation, artifact metadata and
reobfuscation checks, and **191 original native GameTests passed**. Real-client,
installed optional APIs, live multiplayer, and sustained modpack performance
remain separate acceptance gates; third-party energy conversions must be
grounded in actual adapters and conservation semantics.

**Further API testing is required.**
