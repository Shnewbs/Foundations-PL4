# Foundations PL4 / Minecraft 1.13.2 — Forge25 port

**Unreleased native port in progress.** This branch derives from the 1.14.4
full-feature implementation and independently targets Minecraft 1.13.2,
Forge 25.0.223 and Java 8. Forge's official download list identifies
25.0.223 as the latest 1.13.2 build (check source below).

The inherited full-feature code still needs target-specific API adaptation
for forge registries, world/recipe data, GUI rendering, block interaction,
worldgen, networking and capabilities. Existing source presence is NOT proof
that any of those features run on installed Forge25.

Native gates: independent compiler/reobfuscation, Java8 archive verification,
194 exact-target installed-server scenarios, no-fixture production startup
and stop, then real-client, optional API and multiplayer acceptance. A binary
from another Minecraft target must NEVER be renamed and published as 1.13.2.

**Further API testing is required.** No versioned release has been certified.
Official loader: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.13.2.html
