# Public API references inspected for R6, 2026-09-28

These are branch/source references, not an assertion that every released version matches them. No external mod source code or binaries are bundled by this patch. Bridges use the public getters of an actually registered capability; unsupported signatures fail safely with a provider warning.

- NeoForge 1.21.1 BlockCapability: https://raw.githubusercontent.com/NeoForged/NeoForge/1.21.1/src/main/java/net/neoforged/neoforge/capabilities/BlockCapability.java
- NeoForge 1.21.1 capability name/type/context access: https://raw.githubusercontent.com/NeoForged/NeoForge/1.21.1/src/main/java/net/neoforged/neoforge/capabilities/BaseCapability.java
- Mekanism 1.21.x IStrictEnergyHandler: https://raw.githubusercontent.com/mekanism/Mekanism/1.21.x/src/api/java/mekanism/api/energy/IStrictEnergyHandler.java
- Mekanism strict capability identifier: https://raw.githubusercontent.com/mekanism/Mekanism/1.21.x/src/main/java/mekanism/common/capabilities/Capabilities.java
- Mekanism target version metadata (Minecraft 1.21.1): https://raw.githubusercontent.com/mekanism/Mekanism/1.21.x/gradle.properties
- GTCEu 1.21 capability identifiers: https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern/1.21/src/main/java/com/gregtechceu/gtceu/api/capability/GTCapability.java
- GTCEu IEnergyContainer getters: https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern/1.21/src/main/java/com/gregtechceu/gtceu/api/capability/IEnergyContainer.java
- GTCEu IEnergyInfoProvider record/getter: https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern/1.21/src/main/java/com/gregtechceu/gtceu/api/capability/IEnergyInfoProvider.java
- KubeJS recipe event and custom JSON guidance: https://kubejs.com/wiki/tutorials/recipes

The KubeJS example additionally follows PL4's local CoreRecipes registration and ForgingRecipe serializer, not another mod's JSON schema.
