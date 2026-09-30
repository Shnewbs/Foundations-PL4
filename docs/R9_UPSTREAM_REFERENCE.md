# R9 implementation reference

Primary visual target: released Practical Logistics2 (1.12.2-3.0.8), not unfinished PL3. Existing original asset/license notices remain untouched. No new upstream textures were downloaded/replaced for R9.

Inspected primary references:
- https://www.curseforge.com/minecraft/mc-mods/practical-logistics-2
- https://raw.githubusercontent.com/SonarSonic/Practical-Logistics-2/4772196103d35c78c33f03c288a7b47aac267197/src/main/java/sonar/logistics/core/tiles/displays/info/types/items/NetworkItemElement.java
- https://raw.githubusercontent.com/SonarSonic/Practical-Logistics-2/4772196103d35c78c33f03c288a7b47aac267197/src/main/java/sonar/logistics/core/tiles/displays/gsi/gui/GuiAbstractEditElements.java
- https://raw.githubusercontent.com/SonarSonic/Practical-Logistics-2/4772196103d35c78c33f03c288a7b47aac267197/src/main/java/sonar/logistics/core/tiles/displays/info/types/fluids/ElementNetworkFluid.java
- https://docs.neoforged.net/docs/1.21.1/gui/screens/
- https://raw.githubusercontent.com/NeoForged/NeoForge/1.21.1/src/main/java/net/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions.java
- https://raw.githubusercontent.com/NeoForged/NeoForge/1.21.1/src/main/java/net/neoforged/neoforge/fluids/FluidStack.java

The code is a bounded implementation in the port's existing architecture. Original PL2 has a much larger GSI element/action/container model. Left-side editing, distinct graphical elements and separately layered quantities are restoration targets; arbitrary combinations/actions and pixel-perfect equivalence are not established by this source pass. Native API/renderer signatures and real gameplay still require the build/client acceptance listed in VALIDATION.md.
