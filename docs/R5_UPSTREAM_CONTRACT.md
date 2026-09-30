# R5 upstream contract and deliberate boundaries

Reference: Practical Logistics 2, commit `4772196103d35c78c33f03c288a7b47aac267197` (3.0.8 / Minecraft 1.12.2). MIT attribution is retained in LICENSE and NOTICE. This is not a claim that all PL1 branches or unfinished PL3 features have been ported.

The linked Java files were read through the public source interface. Raw repository downloads and Gradle dependency downloads were unavailable in the packaging runtime. The existing R4 package already contained the retained PL2 textures/models and the pinned upstream manifest.

## Hammer sources

- [ModelHammer](https://github.com/SonarSonic/Practical-Logistics-2/blob/4772196103d35c78c33f03c288a7b47aac267197/src/main/java/sonar/logistics/core/tiles/misc/hammer/render/ModelHammer.java): sixteen box dimensions, UV origins, pivots and leg angles; 128x64 model skin. `core/HammerGeometry` records those values and `client/HammerModel` bakes them with modern APIs.
- [RenderHammer](https://github.com/SonarSonic/Practical-Logistics-2/blob/4772196103d35c78c33f03c288a7b47aac267197/src/main/java/sonar/logistics/core/tiles/misc/hammer/render/RenderHammer.java): 1.625-block travel and pedestal item; R5 uses bounded tick interpolation and a normal modern coordinate transform instead of the old GL state helper.
- [BlockHammer](https://github.com/SonarSonic/Practical-Logistics-2/blob/4772196103d35c78c33f03c288a7b47aac267197/src/main/java/sonar/logistics/core/tiles/misc/hammer/BlockHammer.java) and the same directory's `BlockHammerAir.java`: base plus two upper occupied cells and GUI forwarding. R5 adds preflight checks, safe old-hammer headroom behavior and explicit no-loot upper blocks.
- [ContainerHammer](https://github.com/SonarSonic/Practical-Logistics-2/blob/4772196103d35c78c33f03c288a7b47aac267197/src/main/java/sonar/logistics/core/tiles/misc/hammer/ContainerHammer.java) and [GuiHammer](https://github.com/SonarSonic/Practical-Logistics-2/blob/4772196103d35c78c33f03c288a7b47aac267197/src/main/java/sonar/logistics/core/tiles/misc/hammer/GuiHammer.java): two machine slots, original player inventory coordinates, 176x143 GUI and 23-pixel progress arrow. R5 uses native server-side menus and keeps the original GUI image.

## Cable sources and limits

The pinned manifest locates `core/tiles/connections/data/handling/CableConnectionHelper.java`, `core/tiles/connections/data/tiles/TileDataCable.java`, and `api/core/tiles/connections/EnumCableRenderSize.java`. Their centre/connector model family, port disabling and render-size distinctions informed this revision.

R5 uses one explicit centre cable per host, compatible face devices in that host, and unobstructed matching external cable ports. It does not implement all upstream multipart provider, reader-subnetwork/channel, cover, edge/corner attachment or addon API rules. Collision is generated from retained static model meshes, not a promise that every original multipart placement exception is reproduced. The earlier adjacent-host approximation is removed, but **full multipart/network 1:1 parity remains open**.

## Large displays

The retained `large_screen` and `large_screen_back` texture families are used for sixteen edge masks. R5 groups loaded same-owner/same-face coplanar rectangles up to 16x16 with a deterministic visual top-left controller. This is a bounded implementation over the existing text/bar renderer; it does not reproduce the entire original connected-display/GSI system, arbitrary shapes, icon/grid widgets or editor actions. Native visual orientation is unverified.

## NeoForge reference APIs

[1.21.1 menus](https://docs.neoforged.net/docs/1.21.1/gui/menus/), [interaction pipeline](https://docs.neoforged.net/docs/1.21.1/items/interactionpipeline/), and the official NeoForge 1.21.1 `IBlockEntityRendererExtension`, `IBlockExtension`, `SlotItemHandler` and `FakePlayerFactory` sources were checked. R5's protocol/menu/renderer code still needs full compilation against the pinned NeoForge dependency; API review is not a substitute for that build.
