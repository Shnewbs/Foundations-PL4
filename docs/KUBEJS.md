# PL4 and KubeJS on Minecraft 1.21.1

PL4 has recipe/data-level compatibility, not a dedicated KubeJS plugin.

## Example

Place the following in `kubejs/server_scripts/pl4_recipes.js` on a test instance/server. It deliberately replaces the bundled stone forging recipe; use only when that recipe change is desired. Reload recipes with `/reload`.

```js
ServerEvents.recipes(event => {
  event.remove({ id: 'foundations_pl4:forging/stone_plate' })
  event.custom({
    type: 'foundations_pl4:forging_hammer',
    ingredient: { tag: 'c:stones' },
    input_count: 2,
    result: { id: 'foundations_pl4:stoneplate', count: 4 },
    processing_ticks: 120,
    cooldown_ticks: 80
  }).id('kubejs:pl4/stone_plate')
})
```

`ingredient` accepts the native nonempty Ingredient representation, including `item` and `tag`. Input count is 1..64. Result must fit one output slot; `id` is the 1.21.1 ItemStack field and components are handled by the native stack codec. Processing ticks are 1..72000; cooldown ticks are 0..72000. PL4 reads the recipe manager, so there is no duplicate static recipe table to update.

Removal by exact ID plus re-add is the documented approach for this custom type. Do not assume generic `replaceInput`/`replaceOutput` will understand every custom field without a registered schema. Install a KubeJS build matching Minecraft/NeoForge 1.21.1 and its required dependencies; PL4 does not package KubeJS.

The sample is opt-in and was syntax/stub-event tested using Node.js. Native KubeJS/Rhino execution, recipe reload while processing, and client/server recipe synchronization still require in-game validation.

There is no `PL4Events`, `event.recipes.foundations_pl4.*` builder, JavaScript energy-provider registration, or scripting interface for screen layout/network ownership in this revision.
