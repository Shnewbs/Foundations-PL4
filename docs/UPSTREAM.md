# Upstream references

| Repository | Pinned source | Inspected baseline |
|---|---|---|
| [Practical Logistics](https://github.com/SonarSonic/Practical-Logistics) | `4b82002021cba1788b59b3279a7ade97ebca2357` | Default branch properties target Minecraft 1.10.2, mod 2.0.0. README directs users to PL2. |
| [Practical Logistics 2](https://github.com/SonarSonic/Practical-Logistics-2) | `4772196103d35c78c33f03c288a7b47aac267197` | Minecraft 1.12.2, mod 3.0.8. Main behavior and asset reference. |
| [Practical Logistics 3](https://github.com/SonarSonic/Practical-Logistics-3) | `bfd78dee190fef0964470549c3e023ddff3a2a61` | README explicitly says in development for 1.15.2. Includes node-graph/editor work. |

The audit counted 240 Java files in PL1, 562 in PL2, and 260 in PL3 at these exact checkouts. Counts are provenance, not a percentage-completion metric. `UPSTREAM_MANIFEST.json` records the inspected Java/resource paths and content hashes so future work can compare against the same source.

Key PL2 reference files: `PL2Blocks.java`, `PL2Items.java`, `PL2Multiparts.java`, `PL2Crafting.java`, `PL2Config.java`, `core/tiles/misc/hammer/HammerRecipes.java`, `core/items/guide/GuidePageRegistry.java`, the `core/tiles/displays/gsi` tree, and the `integration` tree.

PL1's `LogisticsBlocks.java` contains a large commented-out legacy registration block (channelled cable, data modifier, info creator, channel selector, item router, digital signs and others). Those names cannot be treated as working content in the default checkout merely because a text search finds them. Historical released-version parity needs a separate branch/tag/release comparison.

PL2's in-game guide also describes some unfinished or broader capabilities. The checkpoint follows inspected code where the guide conflicts with it. For example, the actual PL2 hammer recipes convert sapphire ore to two sapphire dust; sapphire gems normally come from mining the ore.

The PL1 and PL2 repositories include the MIT license by Ollie Lansdell. The retained PL2 assets and derived work include that notice in `LICENSE` and the mod JAR. No PL3 source/assets were copied into this checkpoint. The NeoForge MDK was used to establish the 1.21.1 build configuration.

## Sonar Core audit added in Foundations PL4 R2

[Sonar Core](https://github.com/SonarSonic/Sonar-Core) was pulled at commit `f5b64e033e2c07af55c2d5f474052fb83fba5ca1`, version 5.0.19 targeting Minecraft 1.12.2. The original PL2 properties reference Core 5.0.18; the pulled default Core source itself references PL2 3.0.8. All 123 explicit Core imports in PL2 resolve in this source, plus one wildcard package import. See `SONAR_CORE_AUDIT.json` and `CORE_INTEGRATION.md` for scope. No separate Core binary is needed by the current mod.
