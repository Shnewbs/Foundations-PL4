# Foundations PL4 0.0.1a.R6 — Display front, native energy telemetry, KubeJS recipe examples

**SOURCE CANDIDATE. No R6 runtime JAR is included.** Minecraft 1.21.1 / NeoForge 21.1.250 / Java 21.

## What the R5 screenshot establishes

The user supplied an in-game R5 screenshot with connected cables and an inventory-backed 2x2 display. This is user-side evidence that those paths are running, not evidence of every R5/R6 acceptance test. The text shown is `inventoryreader`. That reader does not provide power values. A display must select a named Energy Reader to show energy instead.

R5 places glyphs on the inward/cable-facing side for every display. Its original normal/mini model front and glyph plane are also inconsistent. For a screen mounted on a cable face, outward is normally the useful viewing direction; a display mounted against a solid wall needs the inward surface of its host cell instead. Merely rotating the letters 180 degrees would not fix the texture/front-plane mismatch.

## Display correction

R6 distinguishes the mounting slot from the viewing front. New displays mounted directly on an existing host face outward toward the side clicked. New displays placed in a cell against a backing block face away from that block. Operator-removed saved items retain their explicit front setting.

**Existing R5 screens are not silently flipped.** Their missing `displayOutward` setting defaults to the old inward direction, preserving the saved controller/layout. Open any tile's Settings and change **Front: inward** to **Front: outward** to fix an existing cable-mounted screen. The button flips its joined rectangular canvas, not its cable ports or block coordinates. Inward remains available for solid-backed screens.

The same six-face coordinate basis is used by the font, rectangle controller selection, and render bounds. Normal, mini and joined large displays have explicit oriented model variants for both front choices. The front texture and glyph plane are on the same side; glyphs are not drawn from the reverse viewing side. Opposite-facing screens do not form a shared canvas. A joined-canvas flip transfers the active label, selected reader, color and elements to its new top-left controller. Both the old group and any new joined group are checked for edit/spawn-protection permission before copying settings. Other local tile layouts remain saved except the new controller, which receives the active layout by design.

This is not new GSI feature parity. Holographic text shares the new orientation control, but full hologram geometry/projection parity is still unverified. Existing large-screen rectangle limits, ownership and electrical-network isolation remain.

## Energy Reader

R5 only queries the attached side's NeoForge FE storage capability and labels every successful sample FE. That is not native support for arbitrary mod energy systems.

R6 source adds these **read-only, optional providers**:

| Selection | Capability/access | Unit |
|---|---|---|
| FE | NeoForge `Capabilities.EnergyStorage.BLOCK` | FE |
| J | Mekanism `mekanism:strict_energy_handler`, container count/stored/max getters | J |
| EU | GTCEu `gtceu:energy_info_provider` (including BigInteger telemetry) or `gtceu:energy_container` | EU |
| AUTO | Prefer a usable native provider; otherwise try FE | Native unit or FE |

The reader's Settings now have **Energy: AUTO / FE / EU / J**. Server configuration adds `energyReader.mekanismJoules`, `energyReader.gregtechEU`, and `energyReader.maximumContainersPerHandler` (default 1024). Missing optional mods require no dependency JAR. The actual registered capability and public interface methods are resolved once per server session; no arbitrary block NBT keys or reflective setters are used.

A target with both a native provider and an FE bridge is counted once, not twice. Multiple Nodes facing the same physical block try their actual connected sides but do not multiply its total. There is no automatic null-side/internal or unconnected-side access to bypass the machine's side configuration. Provider failures are isolated, flagged in the reader status, and logged once per provider. Missing/blocked/unloaded storage is not invented as a `0 FE` battery. Getter counts are bounded; an oversized handler is rejected, not silently truncated.

Totals stay separate: FE uses the existing `storage` data key, EU uses `storage:eu`, and Joules use `storage:j`. No arbitrary EU/FE/J conversion constants are applied. These providers do not enable EU/J transfer: Transfer Nodes still use their existing FE transport implementation. RF-named mods are readable through FE only when they actually expose that capability; changing an RF label alone does not integrate a private API. This revision does not add AE2-native grid telemetry, IC2-specific energy, Create torque/stress/RPM, or every historical integration.

### Wiring acceptance

1. Connect a Node to an exposed energy face of the actual battery/machine. For multiblocks use a supported energy port/controller, not an arbitrary casing block.
2. Put an Energy Reader on that data network and give it a unique name, for example `power_main`.
3. Inspect its Data tab first. Select AUTO or the desired native system. A charged battery should show nonzero stored/capacity in its actual unit; an empty valid battery may legitimately show zero.
4. Set the display's Reader name to `power_main`. An Inventory Reader named `inventoryreader` continues to show inventory, not energy.
5. Compare a charged FE battery, Mekanism cube and GT energy buffer with their own GUIs; change the charge and confirm updates. Test the actual installed mod versions before treating a provider as accepted.

Native providers currently use the existing double-valued row protocol: values above 2^53 can lose integer precision, even though they are not truncated to 32-bit FE limits. Values beyond finite double range are rejected. Separate multiblock casing blocks that expose the same underlying store are not globally deduplicated; use one Node at the authoritative port. Storage/capacity is not a measurement of generation, consumption or instantaneous transfer rate.

## KubeJS compatibility — actual boundary

The existing registered items, tags, crafting recipes and `foundations_pl4:forging_hammer` JSON recipe codec are compatible at the data/recipe layer. KubeJS `ServerEvents.recipes` can add custom JSON recipes and remove existing ones by ID, without a dedicated PL4 KubeJS addon. R6 includes an opt-in example at:

`examples/kubejs/server_scripts/pl4_recipes.js.example`

Copy it as `kubejs/server_scripts/pl4_recipes.js` only in the instance/server where you deliberately want the example change, then `/reload`. It replaces the stone forging recipe with 2 tagged stones -> 4 plates and different timings. The updater does not install or enable scripts and does not change your recipes. The 1.21.1 result object uses `id`, not the old `item` field.

There is **no PL4 KubeJS plugin, `PL4Events` event group, recipe-builder DSL, scriptable display controller, script-registered energy provider API, or permission bypass added here**. Generic remove/add JSON is supported by the data design; KubeJS runtime/reload acceptance has not been run in this environment. A Node.js stub test of the example is not a KubeJS/Rhino test.

## Build and verification boundary

R6 adds 3,275 assertions against three new dependency-free production classes, retains the 1,149,451 R5 rule assertions and 11 GUI-layer regression tests, and checks 216 oriented display variants. Java 21 syntax parsing is not Minecraft API compilation. Four new native GameTests bring the total to 35; none were executed for R6.

The native build attempt failed before compilation because `services.gradle.org` could not be resolved. Native Java/NeoForge API compilation, runtime-JAR assembly, graphical acceptance, live Mekanism/GT compatibility, KubeJS reload and Windows updater execution are all pending. No claim of completed 1:1 parity follows from the offline checks.
