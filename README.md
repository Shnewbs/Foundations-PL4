# Foundations PL4 — Minecraft 1.19.4 / Forge 45

Native source baseline: full-feature 1.19.3, independently retargeted to
Minecraft 1.19.4, Forge 45.4.5, Java 17. Not a renamed 1.19.3 JAR.

Complete PL4 multipart networks/displays/editors, wireless storage,
item/fluid/energy transfers, escrow and guides remain the intended baseline.
The alpha is released only if it compiles against its own APIs and all
191 exact-target GameTests succeed. An absent optional mod must not prevent
the tested built-in PL4 functionality; foreign energy conversion is never
guessed from missing APIs.

**Further API testing is required** for graphics, optional installed mods,
live multiplayer and modpack-scale performance.
