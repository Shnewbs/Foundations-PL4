# Foundations PL4: one functional runtime module

## What was pulled and checked

The original PL2 source has 124 distinct `sonar.core` import declarations: 123 explicit symbols and one wildcard import. The pulled [Sonar Core repository](https://github.com/SonarSonic/Sonar-Core) is pinned to `f5b64e033e2c07af55c2d5f474052fb83fba5ca1`, version 5.0.19 for Minecraft 1.12.2. PL2 originally declared a 5.0.18 dependency. Every explicit imported symbol was resolved in the pulled source; `SONAR_CORE_AUDIT.json` contains the callers and source hashes. The wildcard resolves to the `sonar/core/network/sync` directory rather than one class.

## How the combined mod works

The current implementation is one mod: **Foundations PL4**, with mod ID `foundations_pl4` and Java package `net.foundations.pl4`. There is no separate `sonarcore` mod entrypoint, no nested old Sonar JAR, and no dependency on a separate Foundations Framework or Calculator build. This also avoids loading obsolete Forge 1.12 classes into NeoForge 1.21.1.

| Original Core responsibility | Internal PL4 implementation | Coverage |
|---|---|---|
| SonarRegister, registry helpers | `FoundationsPL4` and NeoForge deferred registration | All currently registered content |
| NBTHelper, SyncTagType, sync inventories | `Part`, `HostEntity`, `HammerEntity` and registry-aware NBT | Implemented part settings, owners, links, buffers, inventory and progress |
| Sonar packet registration and tile sync | `PLPackets`, NeoForge payload registration, block entity update tags | Current configuration/editor and sampled data |
| SonarInventory / sided inventory wrappers | Internal handlers using NeoForge `IItemHandler` | Current hammer and transfer implementation |
| Core item/fluid/energy access | `DataSampler`, `TransferEngine`, standard NeoForge capabilities | General item, fluid and FE interfaces; custom legacy providers remain pending |
| DefinedRecipeHelper, RecipeOreStack, RecipeHelperV2 | `core.CoreRecipes` and `core.ForgingRecipe` | Six hammer conversions, tags, counts, component-aware output, timings, reload/sync |
| Core GUI / font / rendering helpers | Current native 1.21.1 screens, font and block rendering | Current simple UI only; full original GSI/editor remains unfinished |
| Core multipart wrapper | `HostBlock`, `HostEntity`, `PartItem` | Current internal multipart host; complete original geometry/rules remain pending |

“Internal” here describes working code packaged in the PL4 JAR, not an empty wrapper around an unavailable Core mod. It does not claim every old Core API or every original PL2 feature is finished. `PARITY.md` remains authoritative about the missing features.

## Internal forging recipe format

Recipe type: `foundations_pl4:forging_hammer`.

Example datapack path: `data/your_pack/recipe/forging/custom_plate.json`.

```json
{
  "type": "foundations_pl4:forging_hammer",
  "ingredient": { "tag": "c:gems/diamond" },
  "input_count": 3,
  "result": { "id": "foundations_pl4:etchedplate", "count": 4 },
  "processing_ticks": 100,
  "cooldown_ticks": 200
}
```

`ingredient` uses standard Minecraft/NeoForge ingredient syntax and accepts an item or tag. Results use the 1.21.1 item stack codec, including supported item components. Input count must be 1–64; result stacks must fit the output slot; processing duration must be 1–72000 and cooldown 0–72000 ticks. Defaults preserve the original hammer's 100-step processing counter and 200-tick cooldown. As in the original, a fresh operation completes on the update after the counter reaches the configured value.

Custom recipes are loaded through the normal recipe manager and are synchronized by Minecraft. A recipe/data reload is therefore authoritative; there is no second hidden hard-coded recipe table. If a recipe changes while processing, the machine rechecks its input/output constraints. Changing the active recipe resets progress.

This uses standard recipe JSON so a datapack can provide recipes. A dedicated CraftTweaker/JEI integration or a KubeJS-specific Java API is not claimed.

## Build guard

`gradlew build` runs `verifyStandalone` on the generated JAR. It fails on legacy Sonar Core/MCMultiPart class references, nested JARs, old dependency declarations, or a missing `foundations_pl4` identity. The earlier R3 record reports that guard and its 16 server GameTests passing. R5 adds fifteen tests and updates the wiring fixtures, but none of the 31 native R5 GameTests has been run here. R5 source-only rule/asset checks are separate evidence.

## Packaging and compatibility

R5 is supplied as source only. After a successful Java 21 build, install only `build/libs/FoundationsPL4-1.21.1-0.0.1a.R5.jar` on client/server, replacing the older PL4 runtime JAR. Do not install the sources JAR. The supplied ZIP does not contain a compiled R5 mod. The mod requires Minecraft 1.21.1, NeoForge 21.1.250 or a compatible 21.1.x release, and Java 21; the source target is specifically 21.1.250; native R5 testing is pending.

The earlier development checkpoint used the `practicallogistics2` registry namespace. R2 changes it to `foundations_pl4`; it is not a save-compatible replacement for that namespace. This build provides no automatic old-save conversion. The old namespace and the original Sonar APIs are not claimed as compatibility shims.
