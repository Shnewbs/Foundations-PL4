# R6 local acceptance (single account)

Run against a copied world, retaining the R5 source backup and old runtime mod separately. Install the same successful R6 runtime build on client and server. Do not install a source or Gradle wrapper JAR as a mod.

## Native build

Run `gradlew.bat clean build runGameTestServer`. Require compilation, JAR assembly, build guards and all 35 GameTests to pass. Archive the full build log and exact mod versions. A successful process exit without the expected R6 runtime JAR is not acceptance.

## Display fronts

Use an existing R5 2x2 display with a named reader and custom label/layout. Open a member tile and choose Settings -> Front: outward. Check the useful side is outward and the back has no text. Check the front frame and lettering agree, the full rectangle shares the same front, and the selected reader/label/elements survive the controller moving. Flip back and repeat after save/reload. Cable ports must not move.

Test single normal, mini and joined 1x2, 2x1, 2x2 displays on all six mounting directions. Test new cable-host placement and solid-backed placement separately. Test a member tile rather than only the controller. Verify expanding/flipping near another screen cannot overwrite a protected or foreign layout. Breaking/splitting and cross-chunk unload/reload must not orphan the canvas. Keep R4's crisp GUI background layering.

## Energy

Name a real Energy Reader `power_main`, connect a Node to an energy-exposing face, and first read the reader's Data tab. Then point a display at `power_main`; do not leave it selected to Inventory Reader.

Compare stored/capacity with the machine GUI for an FE battery, a Mekanism cube and a GTCEu buffer. Record their exact versions. Test AUTO and explicit FE/J/EU. Charge/discharge without PL4 and verify updates. An empty valid battery is zero; unsupported/blocked sides are a diagnostic, not invented zero storage. Unsupported selection on an FE-only battery should not pretend EU/J support.

Connect duplicate Nodes to the same physical battery and confirm no duplicate total. Connect FE/EU/J stores together and verify three separate totals, not a converted sum or native-plus-FE duplicate. For multiblocks use one authoritative energy port: global equivalence across distinct casing positions is not established. Test side configuration changes, chunk unload, reconnect and server restart. Verify all optional mods can be absent. Inspect server logs for provider errors. Providers are read-only; Transfer Nodes remain FE transport.

## KubeJS

Install a compatible 1.21.1 NeoForge KubeJS build and dependencies. On a test server, copy the bundled `.js.example` to `kubejs/server_scripts/pl4_recipes.js`. Reload, require no script/codec errors, confirm the original stone recipe is removed and the custom recipe creates four plates from two tagged stones with 120 processing and 80 cooldown ticks. Validate recipe synchronization with the client and reload while the hammer is idle and while processing. Restore/remove the example afterwards unless the change is desired.

## Report a failed provider

Include the exact mod/version and battery/machine registry ID, Node side, Energy Reader selection/status, machine GUI charge, and relevant log excerpt. Inventory reaching the display does not prove the energy provider is supported.
