# Foundations PL4 — Master Status, Parity Audit, Roadmap, and Expansion Plan

**Project:** Foundations PL4  
**Current source candidate:** `0.0.2a.R3` — editor organization, routing and component controls. See `docs/R3_IMPLEMENTATION.md`.
**Minecraft:** 1.21.1  
**Loader:** NeoForge 21.1.250 target  
**Java:** 21  
**Current validation:** R3 source/offline checks pass; publication is gated by native builds and GameTests on each track. Client visuals, live multiplayer and installed optional integrations remain pending. Earlier validation notes below describe historical versions.
**Layer workflow:** R1.3 published; 134 native tests passed on Linux and Windows; client visual acceptance pending.
**Cable placement patch:** local client/server geometry reconciliation implemented; 131 native tests passed on Linux and Windows; user accepted much faster/snappy placement in the modpack.
**Primary visual/gameplay reference:** Practical Logistics 2 (`3.0.8`, Minecraft 1.12.2)  
**Status:** `0.0.1a` frozen by user acceptance; `0.0.2a` is now the active development line.

---

## R3 reconciliation

R2 delivered named/searchable/pinned reader targets, provider registration and network diagnostics. R3 source adds editor organization/styles/page links, page names, transfer channels and cycle-based fairness. Historical checkboxes below are not release evidence; native and client acceptance remain distinct. The component, directional-filter and installed-mod parity backlog remains open.

## 1. Purpose of this document

This is the working master roadmap for Foundations PL4. It is intended to prevent the project from drifting between “ported,” “registered,” “looks similar,” and “actually behaves like Practical Logistics 2.”

It records:

- what has already been implemented;
- what has been verified in-game by the current tester;
- what is implemented in source but still needs native acceptance;
- what remains missing compared with released Practical Logistics 2;
- which source files should be revisited when parity work resumes;
- how the remaining work should be divided into versions/phases;
- what should deliberately wait until after the first stable PL2-parity line;
- what Foundations PL4 can eventually become beyond PL2.

This file should be updated whenever a revision closes a roadmap item. **A feature is not considered complete merely because an item/block is registered, a texture exists, or an offline rule test passes.**

---

# 2. Source-of-truth references

## 2.1 Current Foundations PL4 source

Historical audited R16 package:

```text
FoundationsPL4-0.0.1a.R16-TRANSFER-HARDENING-SOURCE.zip
```

Current R17 source retains these paths:

```text
src/main/java/net/foundations/pl4/
src/main/java/net/foundations/pl4/client/
src/main/java/net/foundations/pl4/core/
src/main/resources/assets/foundations_pl4/
src/main/resources/data/foundations_pl4/
docs/
```

Historical R16 source size:

| Item | Count |
|---|---:|
| Production Java files | 78 |
| Resource files | 605 |
| PNG textures | 117 |
| Model JSON files | 370 |
| Blockstate JSON files | 58 |
| Recipes | 35 |
| Field Guide chapters | 28 |
| Registered native GameTests | 114 |
| Multipart/content `Kind` IDs | 23 |

## 2.2 Pinned upstream references

These commits are already recorded in the project’s `docs/UPSTREAM.md` and `UPSTREAM_MANIFEST.json` and should remain the reproducible reference points unless deliberately superseded.

### Practical Logistics 2 — primary reference

```text
Repository: https://github.com/SonarSonic/Practical-Logistics-2
Commit:     4772196103d35c78c33f03c288a7b47aac267197
Version:    3.0.8 / Minecraft 1.12.2
```

This is the **main visual and gameplay reference** for PL4.

The pinned manifest contains 562 PL2 Java files. Important areas include:

```text
core/tiles/connections/
core/tiles/readers/
core/tiles/nodes/
core/tiles/displays/
core/tiles/displays/gsi/
core/tiles/wireless/
core/tiles/misc/
base/channels/
base/filters/
integration/
core/items/guide/
```

### Practical Logistics 1 — historical behavior reference only

```text
Repository: https://github.com/SonarSonic/Practical-Logistics
Commit:     4b82002021cba1788b59b3279a7ade97ebca2357
```

PL1 should only be used when confirming a feature that was genuinely released and later disappeared or changed. Commented-out registrations are not proof of released gameplay.

### Practical Logistics 3 — future design research, not the parity target

```text
Repository: https://github.com/SonarSonic/Practical-Logistics-3
Commit:     bfd78dee190fef0964470549c3e023ddff3a2a61
Status:     unfinished / in development
```

PL3 is useful for ideas such as data addresses, method graphs, advanced GSI components, styled text, layouts, interactions, and node graphs. It must **not** replace PL2 as the visual or feature-completion target.

### Sonar Core — audited and internalized

```text
Repository: https://github.com/SonarSonic/Sonar-Core
Commit:     f5b64e033e2c07af55c2d5f474052fb83fba5ca1
```

PL4 is intentionally standalone. Old Sonar Core and MCMultiPart are not runtime dependencies. Their responsibilities are being reimplemented internally using Minecraft 1.21.1 / NeoForge APIs.

---

# 3. Evidence levels used in this roadmap

Every status should be interpreted using these evidence levels.

| Level | Meaning |
|---|---|
| **USER VERIFIED** | The current tester reported the feature working in an actual client/world. |
| **NATIVE TESTED** | Native Java/NeoForge build and automated GameTests passed for that exact revision. |
| **SOURCE IMPLEMENTED** | Code exists and offline/source tests pass, but native acceptance is incomplete. |
| **PARTIAL** | Some of the original behavior exists, but important parity is missing. |
| **MISSING** | The feature is not meaningfully implemented. |
| **EXPANSION** | New PL4 functionality beyond the released PL2 target. |

Do not silently promote **SOURCE IMPLEMENTED** to **complete**.

---

# 4. Current R16 architecture

## 4.1 Standalone runtime

Foundations PL4 is a single mod:

```text
mod id: foundations_pl4
package: net.foundations.pl4
```

No separate Sonar Core, MCMultiPart, Calculator, or Framework JAR is required for the current runtime design.

## 4.2 Multipart host model

The current multipart system is owned by:

```text
HostBlock.java
HostEntity.java
Part.java
PartItem.java
MultipartShapes.java
PartShapes.java
MultipartTopology.java
ConnectionRules.java
```

Current save contract:

```text
Host schema: 2
Multipart slots: 13
ordinary faces: 0..5
cable center: 6
display faces: 7..12
```

This is an internal replacement for old MCMultiPart behavior, not binary/API compatibility with MCMultiPart.

## 4.3 Network model

Main source:

```text
NetworkEngine.java
MultipartTopology.java
DisjointSets.java
```

Important current behavior:

- network traversal is server-authoritative;
- loaded host parts are cached into groups;
- topology rebuilds only when marked dirty or configuration changes;
- chunks are not force-loaded;
- data and redstone networks remain separate;
- reader `NETWORK` input and `VISUAL` output are distinct;
- a visual export exposes telemetry without electrically merging the two machine networks;
- same-owner wireless emitter/receiver pairs can union network groups;
- cross-dimension wireless can be allowed by config;
- oversized loaded networks fail safely at the configured host limit.

## 4.4 Display model

Main source:

```text
DisplayNetworks.java
DisplayCanvas.java
DisplayPainter.java
DisplayEditorScreen.java
DisplayPropertiesScreen.java
DisplayPickerScreen.java
DisplayColorPickerScreen.java
DisplayElements.java
DisplayLayout.java
DynamicCanvasLayout.java
LayoutTransactions.java
EditorChrome.java
DisplayPicking.java
```

Current typed display elements:

```text
TEXT
ITEM
BLOCK
INVENTORY
FLUID
FLUID_GRID
BAR
```

Current limits:

```text
32 elements
8 pages
128 rendered icons per canvas
up to 8 explicitly bound visible readers
logical canvas max 4096 x 4096
large displays: rectangular joined canvases up to 16 x 16 physical panels
```

## 4.5 Transfer model

Main source:

```text
TransferEngine.java
TransferRules.java
R16GameTests.java
```

R16 defines normal Nodes as passive resource endpoints while Transfer Nodes are active movers.

Supported transported resources:

```text
Items
NeoForge fluids
FE
```

Read-only telemetry only:

```text
GregTech EU
Mekanism Joules
```

Default per-cycle budgets:

```text
Items:  64
Fluid:  1000 mB
Energy: 10000 FE
```

## 4.6 Reader/data model

Main source:

```text
DataSampler.java
EnergyReader.java
VisualSamples.java
ReflectiveEnergyAccess.java
EnergyValues.java
```

Current readers:

```text
Inventory Reader
Fluid Reader
Energy Reader
Info Reader
Network Reader
```

## 4.7 Guide

The current Field Guide has 28 chapters and is maintained as Foundations PL4 documentation for Minecraft 1.21.1 rather than exposing stale original PL2 text to the player.

Primary files:

```text
GuideScreen.java
GuideResources.java
GuideNavigation.java
GuideLayout.java
GuideBook.java
assets/foundations_pl4/guide/en_us.json
```

---

# 5. Current content inventory

`Kind.java` currently preserves these PL2 IDs:

```text
datacable
redstonecable
node
inforeader
inventoryreader
fluidreader
energyreader
networkreader
array
entitynode
transfernode
redstonenode
clock
redstonesignaller
displayscreen
minidisplay
largedisplayscreen
holographicdisplay
advancedholographicdisplay
dataemitter
datareceiver
redstoneemitter
redstonereceiver
```

Additional registered content includes:

```text
sapphireore
hammer
hammer_air (internal structure block)
sapphire
sapphiredust
stoneplate
etchedplate
signallingplate
wirelessplate
operator
transceiver
entitytransceiver
wirelessstorage
plguide
```

---

# 6. What has been done — revision history

## R1 — initial 1.21.1 reconstruction

Initial port baseline: content registration, basic host/network/data concepts, recipes/assets, and early gameplay scaffolding.

## R2 — Core consolidation

- pulled/audited Sonar Core source;
- confirmed 123 explicit PL2 Sonar Core imports plus one wildcard package;
- moved required responsibilities into the PL4 runtime;
- removed the need for a separate legacy Core binary;
- established `foundations_pl4` identity.

## R3 — texture/resource correction

- corrected 1.21.1 texture atlas locations;
- retained original texture pixels;
- added resource/model path validation.

## R4 — GUI render ordering

- fixed custom PL4 screens being blurred/dimmed by inherited background rendering;
- established background -> PL4 content -> widgets -> tooltip ordering.

## R5 — connections, hammer, and first connected displays

- original cable connector mesh family restored;
- stronger multipart connection rules;
- Forging Hammer restored as a three-block animated machine;
- original-style two-slot hammer GUI;
- automation support;
- first bounded connected Large Display implementation.

## R6 — display facing and native energy telemetry

- explicit display-front orientation;
- corrected flat-screen readable side;
- optional read-only Mekanism J telemetry;
- optional read-only GTCEu EU telemetry;
- FE/J/EU totals kept separate;
- KubeJS/data recipe example for the Forging Hammer.

## R7 — compact reader/display connectivity

- separate ordinary and display face slots;
- compact Reader + Display same-face mounting restored;
- visual output no longer requires a cable detour;
- reader `NETWORK` input vs `VISUAL` output separation;
- visual telemetry no longer merges machine networks.

## R8 — display continuity, hologram orientation, Field Guide

- smart Large Display side expansion;
- shared layout continuity across joined displays;
- initial hologram orientation work;
- Foundations-style PL4 Field Guide created and updated for the actual port.

## R9 — PL2-style typed display elements/editor

- real item icons;
- block previews;
- inventory grids;
- fluid graphics/grids;
- progress/energy bars;
- text/value elements;
- world-space editor controls;
- selection/move/resize/snap;
- duplicate/delete/order;
- data picker;
- revision-fenced server-side transactions.

## R10 — item transforms, display presentation, onboarding

- corrected oversized held/dropped multipart item presentations;
- improved automatic monitor table layout;
- added Welcome and guided tutorials;
- improved guide navigation.

## R11 — dynamic joined-display canvas

- Large Display logical workspace now grows with physical screen dimensions;
- old typed layouts migrate proportionally;
- toolbar/editor geometry moved onto the actual display workspace.

## R12 — display plane stabilization

- separated bar background/fill/frame/text/editor planes;
- eliminated major progress/energy-bar z-fighting.

## R13 — editor stabilization and stackable part drops

- button background/border/glyph/hover planes separated;
- four-corner resizing;
- larger cyan resize affordances;
- canonical part item serialization;
- ordinary identical drops stack again;
- configured Operator-removed parts keep meaningful settings/escrow;
- technical-binder Field Guide visual pass.

## R14 — editor UX and block-preview stabilization

- editing help moved to local player HUD;
- world toolbar remains visible to the editing player;
- right-click Back behavior through edit screens;
- 3-column default for grid elements;
- actual column preview;
- visual color palette plus hex support;
- block preview given real shallow 3D depth so its own faces do not z-fight.

## R15 — final-alpha polish attempt

- hologram projection moved farther from projector hardware;
- guide tutorial prose rewritten into a more natural teaching voice.

## R16 — Transfer Node endpoint hardening

- normal Nodes restored as passive transfer endpoints;
- ADD Transfer Nodes can import from passive normal Nodes;
- REMOVE Transfer Nodes can export to passive normal Nodes;
- explicit Transfer Node peers take priority;
- same-cycle receive/re-extract fence added;
- deterministic equal-priority rotation;
- persistent escrow retained;
- ADD/REMOVE is deliberately explicit-peer-only until fuller PL2 channel/filter semantics return.

## R17 — final-freeze emitter anchor

- normal hologram projection begins at the model's physical emitter bar;
- Advanced projection begins at the selected physical projection panel;
- projection clearance is measured along the configured view normal from that emitter anchor;
- R16 transfer behavior, save schema, multipart slots, protocol and recipes remain unchanged;
- native build, all 114 GameTests and real in-game transfer/visual acceptance remain required.

---

# 7. Current user-reported in-game acceptance evidence

The following has been reported working in actual gameplay during the current alpha line. This is useful evidence and should be preserved as regression targets.

## Verified by user

- Forging Hammer model/machine works.
- Forging Hammer GUI works.
- Forging Hammer recipes were tested with KubeJS.
- Hammer automation was tested with hoppers/piping.
- Multiple energy sources are readable, including FE and EU telemetry.
- Large Displays connect and form working canvases.
- Multipart hosts/cables on displays work.
- Display/editor z-fighting was reported fixed after the later render-depth hotfixes.
- Display element resizing became usable/easy after four-corner resize changes.
- Field Guide exists and is usable.
- Typed display content is visible in-game.

## Still not accepted / explicitly open

- R16 passive normal-Node Transfer endpoint behavior has not yet been reported accepted in-game.
- Fluid passive-source/passive-destination transfer still needs real handler acceptance.
- FE passive-source/passive-destination transfer still needs real machine acceptance after R16.
- R17 source now anchors projection geometry at the normal emitter bar / Advanced projection panel; in-game wall/floor/ceiling visual acceptance remains outstanding.
- Full 114 GameTest run for the R17 candidate remains required.

---

# 8. Immediate freeze blockers for `0.0.1a`

The first alpha should not be frozen until every item in this section is closed.

## P0-1 — Fix the hologram emitter anchor

### Current source

```text
core/HologramProjection.java
client/HostRenderer.java
```

R15/R16 projection center logic was based on:

```text
MOUNT_SURFACE_OFFSET
NORMAL_PROJECTION_CLEARANCE
ADVANCED_PROJECTION_CLEARANCE
```

The R17 `geometry()` contract now keeps the mount anchor, emitter anchor, projection normal and distance separate. Normal holograms use the center of the physical emitter bar; Advanced holograms select the physical projection panel facing opposite the configured view.

Offline geometry checks cover all six mount faces, four requested view directions, both projector variants and front/back camera sides. Native rendering still requires the acceptance matrix below.

### PL2 source to revisit

```text
core/tiles/displays/tiles/holographic/BlockHolographicDisplay.java
core/tiles/displays/tiles/holographic/BlockAdvancedHolographicDisplay.java
core/tiles/displays/tiles/holographic/EntityHolographicDisplay.java
core/tiles/displays/tiles/holographic/TileAbstractHolographicDisplay.java
core/tiles/displays/tiles/holographic/TileHolographicDisplay.java
core/tiles/displays/tiles/holographic/TileAdvancedHolographicDisplay.java
core/tiles/displays/tiles/render/RenderHolographicDisplay.java
core/tiles/displays/tiles/holographic/GuiHolographicRescaling.java
network/packets/PacketHolographicDisplayScaling.java
```

### Recommended correction

Introduce an explicit projector geometry contract, for example:

```text
mount anchor
emitter-bar anchor
projection normal
projection distance
projection size/scale
```

`HologramProjection.centre()` should start from the **emitter-bar anchor**, not a generic host-face surface.

Normal and Advanced projectors may use different emitter offsets.

Acceptance matrix:

- wall mount, all horizontal directions;
- floor mount, four View directions;
- ceiling mount, four View directions;
- normal and Advanced variants;
- front and back readable camera sides;
- no intersection with base, bar, cables, or neighboring multipart geometry.

## P0-2 — Native R16 build and all GameTests

Required:

```bat
gradlew.bat --no-daemon --console=plain clean build runGameTestServer
```

Target:

```text
114 / 114 GameTests PASS
```

Do not freeze based only on the offline rule suites.

## P0-3 — R16 real transfer acceptance

### Items

Verify all three useful routing forms:

```text
REMOVE Transfer Node -> ADD Transfer Node
normal Node -> ADD Transfer Node
REMOVE Transfer Node -> normal Node
```

Also verify:

- full destination does not extract;
- item/tag filters;
- priorities;
- equal-priority rotation;
- escrow save/reload;
- Operator removal with pending escrow.

### Fluids

Use at least one real NeoForge tank and test:

```text
normal Node source -> ADD
REMOVE -> normal Node destination
full tank
filter
save/reload escrow
```

### FE

Use at least one real FE battery/machine and test both passive directions and configured rate limits.

EU/J are **not** part of the 0.0.1a transport gate.

## P0-4 — final graphical regression sweep

At minimum:

- GUI scales: small/normal/large where practical;
- windowed and fullscreen;
- normal Display;
- Mini Display;
- joined Large Display;
- normal Hologram;
- Advanced Hologram;
- block icon;
- item icon;
- fluid display;
- grid;
- bar;
- quantities;
- editor toolbar;
- color palette;
- four-corner resize;
- front and shallow side camera views.

## P0-5 — archive the frozen baseline

When the above passes, archive:

```text
exact source ZIP
exact built JAR
SHA256SUMS
R16/R17 final validation log
114 GameTest output
final screenshots
final parity ledger
updater from previous release
```

R17 is the narrow final-freeze revision for the hologram anchor; do not add new feature work before the freeze gate passes.

---

# 9. Detailed subsystem parity audit

| Subsystem | Current R16 state | Evidence | PL2 source to revisit | Remaining work | Planned phase |
|---|---|---|---|---|---|
| Core/runtime | Standalone NeoForge mod; legacy Core responsibilities internalized | Source + earlier local builds | Sonar Core audit; PL2 entrypoints | Public extension API, final packaging acceptance | 0.0.2a+ |
| Multipart host | 13-slot host; cable center + ordinary/display faces | User partial + source | PL2 multipart slots / connection helpers | Remaining placement exceptions, covers, external multipart/API parity | 0.0.2a |
| Data cable | Original connector family, dynamic arms/ports; cached per-group sample inputs | User verified + R1 source optimization | `connections/data/*` | Large network perf/load-unload acceptance and native TPS/memory profile | 0.0.2a hardening |
| Redstone cable | Separate graph and signal propagation | Source | `connections/redstone/*` | Full per-face semantics, feedback loops, wireless parity | 0.0.2a |
| Node | Samples target; R16 passive transfer endpoint | R16 source | `nodes/node/*` | Native transfer acceptance, original GUI details | freeze/0.0.2a |
| Inventory Reader | Counts, filters, sorting, component-aware visuals | User display evidence + source | `readers/items/*` | Historical channel modes, slot/position details, deduplication | 0.0.2a |
| Fluid Reader | NeoForge tank sampling + graphical fluids | Source | `readers/fluids/*` | Per-tank/channel fidelity, modded handlers acceptance | 0.0.2a |
| Energy Reader | FE + optional J/EU read-only telemetry | User FE/EU verified | `readers/energy/*`, old integrations | rates, richer machine semantics, more providers | 0.0.2a/0.0.3a |
| Info Reader | Several vanilla/world/entity metrics | Source | `readers/info/*`, `MasterInfoRegistry` | Restore provider registry breadth and selectable bindings | 0.0.2a |
| Network Reader | Basic host/part counts | Source | `readers/network/*` | Channels, network diagnostics, route details | 0.0.2a |
| Transfer Node | Items/fluids/FE; passive normal endpoints in R16; escrow | Source; native acceptance pending | `nodes/transfer/*`, `base/channels/*`, `base/filters/*` | Directional channels, full filter GUI, deep fairness/recovery tests | 0.0.2a |
| Array | Eight saved links / remote observation | Source | `nodes/array/*` | Restore 8 transceiver inventory slots, GUI, card lifecycle | 0.0.2a |
| Entity Node | UUID binding, entity info/basic scan | Source | `nodes/entity/*` | selection UI, inventory interactions, reconnect semantics | 0.0.2a |
| Data emitter/receiver | Explicit same-owner links and network joining | Source | `wireless/emitters/*`, `wireless/receivers/*` | discovery GUI, channel selection, sharing permissions | 0.0.2a |
| Redstone emitter/receiver | Basic wireless redstone graph joining | Source | same + redstone integration | GUI, statement/channel semantics, tests | 0.0.2a |
| Wireless Storage | Bound target opens read-only-ish current target UI | Partial source | `items/wirelessstoragereader/*` | emitter list, remote inventory/fluid management, permissions | 0.0.2a |
| Transceiver | block/entity link item | Partial source | `items/transceiver/*`, API transceiver interfaces | original inventories/range/selection behavior | 0.0.2a |
| Redstone Signaller | one numeric comparison -> output | Source | `misc/signaller/*` | statement list, AND/OR logic, richer conditions | 0.0.2a |
| Clock | sampled pulse + world-time row | Source | `misc/clock/*` | original editor/visual hand animation parity | 0.0.2a |
| Forging Hammer | 3-block animated machine, GUI, recipes, automation | USER VERIFIED | `misc/hammer/*` | JEI/EMI recipe category, final visual parity | 0.0.2a integration |
| Standard/Mini Display | typed renderers + editor | USER VERIFIED/partial | PL2 display/GSI trees | full styles/actions/containers, pixel-perfect polish | 0.0.2a |
| Large Display | rectangular shared dynamic canvas <=16x16 | USER VERIFIED | connected display files | arbitrary shapes if desired, cross-chunk soak, complete GSI | 0.0.2a |
| Holograms | readable frame and clearance logic | OPEN visual defect | holographic display files | emitter-bar anchor, scale controls, advanced/volumetric parity | freeze + 0.0.2a |
| GSI editor | move/resize, 4 corners, palette, pages, typed elements | USER VERIFIED partial | 35 PL2 GSI classes | styled text, groups, hyperlinks/actions, extraction, sharing/export | 0.0.2a |
| Field Guide | 28 current PL4 chapters, search/bookmarks/tutorials | USER VERIFIED partial | original guide only as content reference | final prose/art/readability, recipe illustrations | 0.0.2a polish |
| Operator | configure/remove/toggle cable ports | Source/user use | `items/operator/*` | clone settings, overlays, rotation modes | 0.0.2a |
| KubeJS | recipe JSON compatibility | USER VERIFIED hammer recipes | CraftTweaker old integration as design reference | real PL4 KubeJS plugin/events/provider registration | 0.0.2a/0.0.3a |
| JEI/EMI | no full hammer category | MISSING | PL2 JEI integration | hammer category, recipe display, focus support | 0.0.2a |
| Save migration | current schema 2 / protocol 4 | Source | n/a | explicit future migrations, downgrade guards | every version |

---

# 10. Important source-audit findings to revisit

These are technical observations from the current R16 source that should guide future implementation.

## 10.1 `Part` is currently a monolithic state container

File:

```text
Part.java
```

One `Part` contains fields for readers, displays, transfers, redstone, wireless links, energy settings, canvas state, pending escrow, and more.

This was efficient for alpha development, but long-term it has costs:

- unrelated fields exist on every part;
- migrations become harder;
- addon/API extension is difficult;
- validation is distributed across packet handlers and UI.

**Recommendation:** do not refactor this during the freeze. During a later API phase, consider versioned per-kind state records/components while maintaining a migration layer from schema 2.

## 10.2 `Kind` is a fixed enum

File:

```text
Kind.java
```

This is safe for a closed mod but blocks external addons from registering new PL4 component kinds.

**Long-term option:** introduce a data/provider registry around a stable internal ID while keeping built-in `Kind` entries compatible.

## 10.3 Generic string-based edit packets will become limiting

File:

```text
PLPackets.java
```

Current `Edit` and `LayoutEdit` packets are bounded and permission checked, which is good, but they use string field/action payloads.

For more complex GSI/actions/scripting, use typed codecs for new protocol families rather than continuing to add arbitrary field names.

## 10.4 Display editor is functional but not full PL2 GSI

Files:

```text
DisplayElements.java
LayoutTransactions.java
DisplayEditorScreen.java
DisplayPropertiesScreen.java
DisplayPickerScreen.java
DisplayPainter.java
```

Current transaction actions are only roughly:

```text
add
update
delete
forward
backward
clear
mode
page
```

PL2’s GSI source has dedicated concepts for:

```text
selection modes
grid modes
create-info modes
click interactions
hyperlinks
element containers
saved element data
look interactions
multiple style/alignment types
styled text
```

This should be the first major parity target in `0.0.2a`.

## 10.5 Hologram position still uses a generic mount reference

Files:

```text
HologramProjection.java
HostRenderer.java
```

The current math knows the face, requested view, normal vs Advanced clearance, and camera side. It does not encode the actual emitter-bar geometry.

This is the current best explanation for the remaining visual mismatch and should be corrected before freezing `0.0.1a`.

## 10.6 Transfer routing is intentionally bounded

File:

```text
TransferEngine.java
```

R16 is intentionally safer than blindly matching incomplete historical semantics:

- normal Nodes are passive endpoints;
- explicit Transfer peers win ties;
- `ADD_REMOVE` does not blindly interact with the passive pool;
- same-cycle re-extraction is blocked;
- no chunk force-loading;
- Energy readers do not convert EU/J. R1.5 adds explicit, server-controlled Transfer Node conversion; telemetry remains native.

This is a good alpha safety boundary. The correct next step is to port **channels and filters**, not to loosen the safety rules blindly.

## 10.7 `NetworkEngine` needs measured performance acceptance

File:

```text
NetworkEngine.java
```

Good current properties:

- cached graph;
- dirty invalidation;
- loaded-only traversal;
- bounded network host count;
- no force-loading.

Still required:

- measured 100/500/1000/4096-host rebuild cost;
- repeated chunk unload/reload testing;
- multiple dimensions;
- many independent networks;
- wireless-heavy networks;
- display-heavy networks;
- stress with item/fluid/energy transfers at the same time.

Future optimization should be driven by profiles, not assumptions.

## 10.8 Reader/provider architecture should become extensible

Current files:

```text
DataSampler.java
EnergyReader.java
ReflectiveEnergyAccess.java
```

The current system works for built-ins and a few optional native energy APIs. PL2 had a broader info/provider registry and many integration classes.

A future `PL4DataProvider` API would let:

- Foundations modules;
- KubeJS;
- third-party mods;
- server packs

register monitored values without editing `DataSampler` itself.

## 10.9 Current configuration is intentionally small

R1.4 adds optional aggregate item/mB/FE delivery caps per connected data network per cycle, including escrow retries. Existing per-node settings remain; shared caps default to zero (uncapped). See `docs/TRANSFER_CONFIG.md`.


File:

```text
PLConfig.java
```

Current config includes network tick rate, host limits, transfer limits, wireless/cross-dimension toggles, entity scan radius, and optional energy readers.

Future versions should consider configurable:

- display limits;
- maximum canvas dimensions;
- wireless range/policy;
- permissions/public links;
- transfer fairness mode;
- per-resource transfer rates;
- provider enable/disable;
- display sync limits;
- diagnostics/logging levels;
- history/graph retention if time-series features are added.

## 10.10 Wireless tools are functional scaffolding, not full UX

File:

```text
ToolItem.java
```

Current transceivers store direct links and receivers validate owner/type. This is safe and simple, but PL2 had dedicated emitter/receiver UIs and Wireless Storage screens.

Do not mistake “link stored in item CustomData” for full wireless parity.

---

# 11. High-value upstream PL2 files to keep open during parity work

## Network/multipart

```text
api/core/tiles/connections/*
core/tiles/connections/data/handling/AbstractConnectionHandler.java
core/tiles/connections/data/handling/CableConnectionHandler.java
core/tiles/connections/data/handling/CableConnectionHelper.java
core/tiles/connections/data/network/LogisticsNetwork.java
core/tiles/connections/data/network/LogisticsNetworkHandler.java
core/tiles/connections/data/network/NetworkHelper.java
core/tiles/connections/redstone/*
```

## Readers/channels

```text
core/tiles/readers/base/TileAbstractReader.java
core/tiles/readers/base/TileAbstractListReader.java
core/tiles/readers/base/TileAbstractLogicReader.java
core/tiles/readers/items/*
core/tiles/readers/fluids/*
core/tiles/readers/energy/*
core/tiles/readers/info/*
core/tiles/readers/network/*
base/channels/ChannelList.java
base/channels/GuiChannelSelection.java
base/channels/ContainerChannelSelection.java
base/channels/handling/*
```

PL2’s `TileAbstractReader` explicitly treats the cable-facing side as `NETWORK` and the opposite side as `VISUAL`; R7 correctly moved PL4 back toward this contract.

## Transfer Nodes

```text
core/tiles/nodes/transfer/TileTransferNode.java
core/tiles/nodes/transfer/handling/NetworkItemHandler.java
core/tiles/nodes/transfer/handling/TransferNetworkChannels.java
base/filters/*
base/channels/*
```

PL2 `TileTransferNode` includes:

- priority;
- transfer mode;
- filters;
- `ChannelList`;
- connection toggle;
- item/fluid/energy toggles;
- selected block/entity channel data.

R16 restores endpoint behavior but not this entire selection/channel UX.

## Displays/GSI

```text
core/tiles/displays/gsi/DisplayGSI.java
core/tiles/displays/gsi/gui/*
core/tiles/displays/gsi/interaction/*
core/tiles/displays/gsi/modes/*
core/tiles/displays/gsi/packets/*
core/tiles/displays/gsi/render/GSIOverlays.java
core/tiles/displays/gsi/storage/*
core/tiles/displays/info/elements/*
core/tiles/displays/info/types/*
```

Important missing concepts include:

```text
clickable elements
hyperlinks/actions
element containers
selection/grid modes
styled text
alignment/fill types
saved layouts
look interactions
richer progress/list/grid editors
```

## Large connected displays

```text
base/events/types/ConnectedDisplayEvent.java
base/utils/worlddata/ConnectedDisplayData.java
core/tiles/displays/tiles/connected/ConnectedDisplay.java
core/tiles/displays/tiles/connected/ConnectedDisplayChange.java
core/tiles/displays/tiles/connected/BlockLargeDisplay.java
core/tiles/displays/tiles/connected/TileLargeDisplayScreen.java
network/packets/PacketConnectedDisplayRemove.java
network/packets/PacketConnectedDisplayUpdate.java
```

## Holographic displays

```text
core/tiles/displays/tiles/holographic/*
core/tiles/displays/tiles/render/RenderHolographicDisplay.java
network/packets/PacketHolographicDisplayScaling.java
```

## Wireless

```text
core/tiles/wireless/emitters/*
core/tiles/wireless/receivers/*
core/tiles/wireless/base/*
api/core/tiles/wireless/*
core/items/transceiver/*
core/items/wirelessstoragereader/*
```

## Array / Entity Node

```text
core/tiles/nodes/array/*
core/tiles/nodes/entity/*
```

## Redstone logic

```text
core/tiles/misc/signaller/*
api/core/tiles/misc/signaller/*
core/tiles/misc/clock/*
```

## Integrations

PL2 includes explicit integration source for:

```text
AE2
Calculator
CraftTweaker
Ender IO
Extra Utilities
Extreme Reactors
Flux Networks
IC2
Immersive Engineering
JEI
Mekanism
Vanilla providers
```

Do not blindly port old APIs; map each historical integration to a current 1.21.1 equivalent or explicitly retire it.

---

# 12. Recommended phase plan

# Phase 0 — Freeze `0.0.1a` (complete)

**Goal:** finish the foundation without adding another large subsystem.

### Completed freeze gate

- R17 emitter-relative hologram anchor and wall/floor/ceiling visual matrix accepted.
- Native Java 21 build and all 114 GameTests accepted.
- R16 item/fluid/FE transfer, escrow/reload, blocked-destination conservation, display/editor regression, and dedicated-server smoke checks accepted.
- Source/JAR/logs/screenshots/checksums archived by the release operator.

### Exit condition

`0.0.1a` is the immutable base for `0.0.2a`. The acceptance above is user-reported release evidence; repeat the native checks when producing the public frozen artifact.

---

# Phase 1 — `0.0.2a.R1`: Full PL2 display/GSI parity foundation

This should be the first major next-alpha work because displays are the feature users see constantly and the current editor is already a solid base.

## Add/restore

### Element styling

- text scale; **implemented in the R1 continuation** (25%–400% bounded data model; separate scale and alignment controls);
- text alignment; **implemented in the R1 continuation** (left, centre, right);
- width/height alignment;
- fill/background styles;
- opacity;
- border styles;
- icon scale;
- quantity position;
- bar style/direction;
- per-element formatting.

### Styled text

Use PL2/PL3 research for:

- wrapped text; **reconciled from the older R1 branch, with persisted wrap controls and element-height clipping**;
- titles;
- line breaks;
- style spans;
- formatting shortcuts;
- alignment.

### Editor workflow

- element list panel; **implemented in R1.3: frontmost-first current-page list, Shift-toggle and Ctrl+A selection, bounded list navigation**;
- multi-select; **extended in R1.1 with Shift-toggle, Ctrl+A, box selection, rigid multi-element movement and atomic multi-delete**;
- box/lasso select; **box selection implemented in R1.1 with page-local overlap, reverse drag and Shift-add; freeform lasso remains planned**;
- alignment tools; **implemented in R1: left/right/top/bottom/centre, using canvas bounds for one element and selection bounds for multiple elements**;
- distribute evenly; **implemented in R1: horizontal/vertical equal edge gaps for three or more current-page elements, preserving outer anchors**;
- copy/paste; **extended in R1.1 to an entire page-local selection, preserving styles/spacing, with one revision-fenced paste**;
- duplicate with deterministic offset; **extended in R1.1: Ctrl+D duplicates the entire selection with a shared 4px offset clamped to canvas edges**;
- group/ungroup;
- lock element;
- hide/show element;
- undo/redo; **implemented in the R1 continuation: bounded client-side history of whole-layout snapshots, restored server-side through a new revision-fenced `replace` transaction (Ctrl+Z / Ctrl+Y, Ctrl+Shift+Z)**;
- page names;
- page duplicate/delete; **implemented in R1.4: duplicate current page into an empty slot with fresh element IDs; undoable clear of current page contents; eight stable slots retained**;
- keyboard shortcuts;
- clearer selected-layer order; **implemented in R1.3: stable page-local multi-selection forward/backward/front/back, one revision and one undo snapshot**.

### Containers/actions

Port PL2 concepts for:

- element containers;
- clickable elements;
- display actions;
- hyperlinks/page links;
- safe player interactions.

### Layout storage

- export/import layout JSON;
- named templates;
- copy layout with Operator;
- optional server-provided templates;
- versioned layout schema.

### Acceptance

A user should be able to rebuild representative PL2 screens without editing NBT or scripts.

---

# Phase 2 — `0.0.2a.R2`: Reader channels and data-provider parity

## Channels

Restore a real modern equivalent of PL2 `ChannelList` behavior:

- single/unlimited channel types;
- node selection;
- block/entity targets;
- named channels where useful;
- reader channel-selection UI;
- selected target feedback;
- safe unloaded-target state;
- permissions.

## Inventory Reader

- per-slot view;
- storage/stack/list modes;
- exact component-aware grouping;
- filter UI;
- sorting UI;
- duplicate combined-inventory handling;
- channel selection.

## Fluid Reader

- per-tank view;
- aggregate view;
- channel selection;
- filters;
- capacity/fill data;
- third-party tank acceptance.

## Energy Reader

Keep units native and add, where APIs can provide them honestly:

- stored;
- capacity;
- fill percentage;
- input rate;
- output rate;
- GT voltage/amperage/tier;
- Mekanism native rate fields;
- FE rate sampling.

Do **not** invent cross-unit conversions just to simplify displays.

## Info Reader

Rebuild a provider registry instead of continuing to hard-code every property into `DataSampler`.

Potential provider categories:

- block state;
- furnace/process progress;
- redstone;
- comparator output;
- crops;
- world time/weather;
- entity health/armor/food/xp;
- machine progress;
- mod-specific values.

## Network Reader

Upgrade from basic counts to:

- host count;
- part count;
- channel count;
- resource endpoints;
- wireless edges;
- network health;
- rebuild count/debug data when permissions allow.

---

# Phase 3 — `0.0.2a.R3`: Transfer Node parity

R16 provides a safe base. R3 should add the historical selection/routing model rather than removing safeguards.

## Restore

- ChannelList-style source/destination selection;
- rich filter GUI;
- item + tag filters;
- fluid filters;
- component-sensitive filter options;
- directional behavior;
- connection toggle;
- priority UI;
- transfer rate settings where desired;
- explicit ADD/REMOVE/BOTH semantics based on selected channels.

## Reliability

Test:

- full destinations;
- partial insertion;
- simulation disagreement;
- source changes mid-cycle;
- chunk unload;
- server stop during escrow;
- reload;
- cross-dimension wireless;
- many equal-priority sinks;
- saturation/backpressure;
- duplicate endpoints to the same combined inventory.

## Native energy transport

R1.5 implements user-requested optional native Mekanism J, GTCEu EU and Electrodynamics/Voltaic Joule transport plus explicit node A → B conversion. See `docs/ENERGY_TRANSFER.md`. Native FE remains the default; this does not complete Phase 3 transfer parity.

Recommended default:

- FE transport remains core;
- native EU/J transport is optional integration-specific behavior only when the external API has clear safe transaction semantics;
- never auto-convert merely because the reader can display another unit.

---

# Phase 4 — `0.0.2a.R4`: Wireless, Arrays, Entity Nodes, and remote storage

## Data/Redstone Emitters and Receivers

Add:

- emitter discovery screen;
- receiver pairing screen;
- names;
- channels/frequencies;
- public/private/team policy;
- permission checks;
- range/config policy;
- live link status;
- cross-dimension status.

## Array

Restore the original concept more faithfully:

- eight physical/logical transceiver slots;
- insert/remove link items;
- GUI;
- slot status;
- loaded/unloaded target state;
- item lifecycle when broken.

## Entity Node

Add:

- entity selection/scanning UI;
- display target name/type;
- inventory/equipment data where safe;
- reconnect to UUID after unload/reload;
- dimension changes;
- permission rules for players/entities.

## Wireless Storage

PL2 reference:

```text
ItemWirelessStorageReader.java
GuiWirelessStorageEmitterList.java
GuiWirelessStorageReader.java
ContainerEmitterList.java
ContainerStorageViewer.java
PacketWirelessStorage.java
```

PL4 target:

- emitter list;
- remote inventory view;
- search;
- fluid view;
- safe deposit/withdraw if enabled;
- server-authoritative transactions;
- range/dimension/permission rules;
- no client-trusted inventory manipulation.

---

# Phase 5 — `0.0.2a.R5`: Redstone logic and automation

## Redstone Signaller

Current one-comparison behavior should become a full statement system.

Port concepts from:

```text
RedstoneSignaller.java
RedstoneSignallerStatement.java
GuiStatementList.java
SignallerModes.java
```

Target features:

- multiple statements;
- AND/OR group logic;
- `>`, `<`, `>=`, `<=`, `=`, `!=`;
- named data source selection;
- per-face output;
- invert;
- pulse/level modes;
- diagnostics explaining why output is on/off.

## Clock

- original-style editor;
- interval/duty cycle;
- phase offset;
- redstone output side;
- restore/modernize clock hand animation if still visually desirable.

---

# Phase 6 — `0.0.2a.R6`: Integrations, JEI/EMI, KubeJS, public API

## Recipe viewers

Add:

- JEI Forging Hammer category;
- EMI category;
- input/output/timing display;
- recipe focus support;
- guide links to recipe viewers where available.

## Jade inspection

Required integration requested by the user:

- identify the targeted multipart component rather than treating every host as a generic cable;
- show component/network connection status and configured update interval;
- expose useful reader/transfer diagnostics through existing permission checks;
- reuse bounded existing state instead of scanning the whole network per tooltip frame;
- remain optional at runtime so the standalone mod still starts without Jade.

## Modern integration matrix

Evaluate current equivalents for historical PL2 integrations:

| Historical PL2 integration | Modern PL4 target |
|---|---|
| AE2 | storage/network/crafting telemetry using current AE2 APIs |
| Calculator | native Foundations Calculator telemetry |
| CraftTweaker | modern CraftTweaker recipe/provider hooks if demand exists |
| Ender IO | current machine progress/energy/storage APIs |
| Extra Utilities | no direct port if no current equivalent; map only where relevant |
| Extreme Reactors | current reactor/turbine telemetry |
| Flux Networks | current network/buffer telemetry |
| IC2 | only if a maintained 1.21.1 equivalent exists |
| Immersive Engineering | machine process/energy/fluid telemetry |
| Mekanism | richer native J/machine telemetry |
| JEI | hammer recipe category |

Also consider modern ecosystems absent from old PL2:

```text
Create
Modern Industrialization
GregTech CEu Modern
Powah
Industrial Foregoing
Thermal-series equivalents if available
Refined Storage / RS2 if applicable
CC:Tweaked
```

## KubeJS

Move beyond recipe JSON.

Potential API shape:

```js
PL4Events.dataProviders(event => { ... })
PL4Events.displayElements(event => { ... })
PL4Events.networkActions(event => { ... })
```

Possible scriptable capabilities:

- register custom reader rows;
- register custom machine providers;
- register safe custom display element types;
- create named formatters;
- create display templates;
- observe transfer events;
- add custom conditions.

Keep server authority and validation mandatory.

## Public Java API

Create a small versioned API rather than exposing internal `Part` directly.

Possible interfaces:

```text
PL4DataProvider
PL4DisplayElementProvider
PL4NetworkObserver
PL4TransferEndpoint
PL4PermissionService
```

---

# Phase 7 — `0.0.2a.R7`: Performance, security, multiplayer, release hardening

## Performance benchmarks

Test at least:

```text
100 hosts
500 hosts
1000 hosts
4096 hosts
```

Scenarios:

- simple cable line;
- dense branching;
- many readers;
- many large displays;
- many item icons;
- many wireless links;
- transfers enabled;
- multiple independent networks;
- chunk unload/reload churn.

Record:

```text
server tick cost
network rebuild time
sampling time
packet volume
client frame time
display render batches
memory growth
```

## Multiplayer/security

Test:

- unauthorized edit packets;
- stale layout revisions;
- player disconnect while editing;
- two players editing same canvas;
- link ownership;
- spawn protection;
- remote target permissions;
- malformed element payloads;
- packet spam/rate limits;
- huge NBT/components;
- broken/unloaded links.

The current packet revision fencing and rate limiting are good foundations; expand tests instead of weakening them.

## Save/recovery

Test:

- world save during transfer escrow;
- server kill/restart;
- chunk unload with pending transfer;
- display controller removal;
- large display split/rejoin;
- wireless target missing/reappearing;
- schema migration from frozen `0.0.1a`.

## Release gate for `0.0.2a`

- clean Java 21 build;
- full GameTest suite;
- dedicated server soak;
- graphical acceptance;
- integration matrix;
- migration from frozen 0.0.1a world copy;
- no known item loss/duplication;
- no silent permission bypass.

---

# 13. `0.0.3a` and beyond — expansion beyond PL2

Once PL2 parity is in good shape, PL4 can become more than a port.

These are **expansion ideas**, not promises for 0.0.2a.

## 13.1 PL3-inspired data addresses and method graph

PL3 contains unfinished but interesting architecture:

```text
server/address/*
server/data/*
server/data/methods/*
client/nodes/Graph.java
client/nodes/AdvancedNodeGraph.java
client/nodes/DataSourceNode.java
client/gsi/*
```

Potential PL4 evolution:

- every monitored value has a typed data address;
- data can be transformed through methods;
- method nodes can perform arithmetic/formatting/logic;
- display elements bind to graph outputs rather than directly to reader rows;
- graph editor is optional and advanced, not required for normal PL2-style usage.

This should be additive. The simple PL2 workflow must remain easy.

## 13.2 Time-series graphs and history

New display elements:

- line graph;
- area graph;
- sparkline;
- min/max/average;
- delta/rate;
- historical bar chart.

Potential monitored data:

- energy usage;
- generation;
- storage fill;
- item throughput;
- fluid throughput;
- machine progress;
- server/network health.

Use bounded retention and server-configurable sampling. Do not synchronize unbounded history.

## 13.3 Alerts and notifications

Allow conditions such as:

```text
battery < 20%
chest > 90% full
machine stopped
fluid tank empty
network disconnected
transfer escrow stuck
```

Actions could include:

- display warning;
- redstone output;
- player toast/chat;
- sound;
- Foundations HUD notification;
- optional webhook/plugin integration later.

## 13.4 Network diagnostics mode

Operator or Network Reader could expose:

- topology overlay;
- cable port states;
- network ID;
- endpoint count;
- wireless edges;
- data vs visual edges;
- transfer route;
- rebuild reason;
- overloaded network warnings.

This would make complicated builds much easier to troubleshoot.

## 13.5 Layout/template library

Add:

- save named display template;
- copy/paste between worlds/servers where allowed;
- server template folder;
- KubeJS/datapack-provided templates;
- import/export JSON;
- template version/migration.

Example templates:

```text
Battery dashboard
Reactor dashboard
Storage wall
Fluid plant
Machine status board
Server/network status
```

## 13.6 Team/permission model

Move beyond owner-only links with explicit policies:

```text
Private
Team
Public read-only
Public interactive
Admin
```

Integrate with common team/claim APIs where available, but keep a native fallback.

## 13.7 Handheld diagnostic terminal

Expand Wireless Storage / Operator into a configurable handheld terminal:

- network overview;
- reader data;
- remote display preview;
- alerts;
- search;
- transfer diagnostics;
- admin-only topology details.

Do not let the client directly mutate remote inventories without server validation.

## 13.8 Foundations ecosystem integration

Potential optional integrations with other Foundations modules:

- Economy balances/market/stock telemetry on PL4 displays;
- Calculator machine dashboards;
- Soil sensor dashboards;
- Cluster/server health dashboards for server operators;
- shared Foundations Field Guide navigation;
- Framework theme hooks while keeping PL4 standalone when Framework is absent.

These should be optional bridges, never hard dependencies unless a future architecture decision explicitly changes that.

## 13.9 Computer integration

Potential CC:Tweaked/peripheral style API:

- read reader rows;
- read network status;
- set display page;
- push safe text/layout updates;
- inspect alerts.

Write operations should be permission-gated and rate-limited.

## 13.10 Data-driven custom readers

Allow datapacks/KubeJS/API providers to define:

- labels;
- units;
- numeric properties;
- capacity pairs;
- formatting;
- machine detection;
- icon/colour metadata.

This could make PL4 useful across mods without shipping a hardcoded Java bridge for every machine.

---

# 14. Things PL4 should deliberately avoid

These rules protect the project from repeating old problems.

1. **Do not make PL3 the visual target.** PL2 remains the in-world visual/interaction reference.
2. **Do not reintroduce Sonar Core as a required runtime JAR.** Keep the combined modern implementation.
3. **Do not claim generic FE support equals every historical energy integration.**
4. **Do not convert EU/J/FE silently.** Native units should stay honest.
5. **Do not force-load chunks just to keep dashboards alive.** Make unloaded state explicit.
6. **Do not let client packets grant access to data the server topology does not expose.**
7. **Do not use unique transient NBT on ordinary dropped parts.** Equivalent default parts must remain stackable.
8. **Do not fix z-fighting by pushing UI geometry visibly far off the screen.** Maintain shallow ordered render planes.
9. **Do not hide parity gaps by registering placeholder content.**
10. **Do not change save schema casually.** Every schema change needs migration and downgrade guidance.
11. **Do not make advanced graph/scripting features mandatory for basic monitoring.** PL2-style simple workflows should stay simple.

---

# 15. Testing strategy

## 15.1 Single-account constraint

Testing must continue to assume one Minecraft account is available.

Where multiplayer behavior is required, use:

- GameTest fake/server players where possible;
- admin/test commands;
- simulated second owner UUIDs;
- deterministic permission fixtures;
- dedicated server/client reconnect tests.

Do not make acceptance depend on owning a second account.

## 15.2 Automated test groups

Maintain separate GameTest groups for:

```text
multipart placement
network connectivity
visual-only reader exports
wireless links
reader sampling
transfer routing
item conservation
fluid conservation
FE conservation
escrow persistence
hammer processing
display continuity
layout migration
NBT stackability
permissions/packet validation
save/reload
```

## 15.3 Graphical test matrix

Test:

```text
GUI scales: small / normal / large
windowed
fullscreen
first-person right hand
first-person left hand
third person
dropped items
item frames
front-on displays
shallow-angle displays
shader off
common shader/render mods where practical
```

## 15.4 Mod compatibility acceptance matrix

For each supported integration, record:

```text
exact mod version
exact API path used
machine/block tested
reader values
transfer behavior if applicable
restart behavior
known warnings
```

Do not advertise “supports Mod X” based only on reflection code existing.

## 15.5 Transfer conservation tests

Every transfer implementation must prove:

```text
before source + destination + escrow
=
after source + destination + escrow
```

for items, fluids, and energy within the unit’s exact representable type.

Test partial insertion and deliberately hostile/misbehaving handlers.

---

# 16. Version and packaging policy

Use the existing Foundations revision policy:

```text
0.0.1a
0.0.1a.R2
...
0.0.1a.R16
0.0.1a.R17 — narrow final-freeze emitter anchor
```

Keep the same base alpha while finishing that alpha’s agreed scope.

After the freeze gate:

```text
0.0.1a = frozen foundation baseline
0.0.2a = PL2 parity expansion line
0.0.3a = post-parity / PL3-inspired expansion line
```

For each revision, prefer:

- complete source ZIP;
- portable updater ZIP;
- BAT launcher usable outside the project folder;
- Windows folder picker + manual fallback;
- expected baseline hash verification;
- backup of changed files;
- optional build;
- optional GameTest run;
- validation markdown;
- acceptance markdown;
- SHA-256 list.

If save schema changes, also include:

- schema number;
- automatic migration path;
- downgrade warning;
- copy-world acceptance plan.

---

# 17. Recommended next-version sequence

## If R16 still has the hologram anchor issue

Use:

```text
0.0.1a.R17 — Hologram anchor + final freeze only
```

Do **not** add new feature work to R17.

Then freeze `0.0.1a`.

## Begin `0.0.2a`

Recommended sequence:

```text
0.0.2a.R1  Advanced PL2 GSI/editor parity
0.0.2a.R2  Reader channels + provider registry
0.0.2a.R3  Transfer Node channel/filter parity
0.0.2a.R4  Wireless / Array / Entity Node / Wireless Storage
0.0.2a.R5  Redstone Signaller + Clock parity
0.0.2a.R6  JEI/EMI + integrations + KubeJS/public API
0.0.2a.R7  Performance / multiplayer / migration / release hardening
```

The exact number of revisions may change, but the dependency order should stay close to this sequence.

---

# 18. Priority backlog

## P0 — before freezing `0.0.1a`

- [x] R17 source anchors normal holograms at the emitter bar and Advanced holograms at the selected projection panel.
- [x] R17 normal/Advanced holograms visually accepted on wall, floor and ceiling mounts.
- [x] Exact freeze source builds under Java 21.
- [x] 114/114 GameTests pass.
- [x] R16 item passive-endpoint transfers accepted in-game.
- [x] R16 real fluid passive-endpoint transfers accepted.
- [x] R16 real FE passive-endpoint transfers accepted.
- [x] Full destination cannot delete resources.
- [x] Escrow survives restart and delivers once.
- [x] R14/R15 display/editor render fixes stay fixed.
- [x] Final save/reload and dedicated-server smoke.
- [x] Frozen source/JAR/checksums/logs archived.

## P1 — first half of `0.0.2a`

- [ ] Full GSI element/style/action roadmap implemented.
- [ ] Styled text and alignment.
- [ ] Multi-select/group/copy/paste/undo.
- [ ] Layout import/export/templates.
- [ ] Reader ChannelList equivalent.
- [ ] Full reader mode/filter GUIs.
- [ ] Provider registry.
- [ ] Network Reader diagnostics.
- [ ] Transfer Node channel/filter parity.
- [ ] Transfer fairness/recovery stress tests.

## P2 — second half of `0.0.2a`

- [ ] Wireless emitter/receiver selection GUI.
- [ ] Array transceiver slots and GUI.
- [ ] Entity Node selection/interactions.
- [ ] Wireless Storage remote GUI/actions.
- [ ] Redstone Signaller statement lists.
- [ ] Clock editor/visual parity.
- [ ] Jade multipart inspection and permission-aware status.
- [ ] JEI/EMI hammer integration.
- [ ] Modern mod provider matrix.
- [ ] KubeJS provider/event API.
- [ ] Public Java API.
- [ ] Large-network profiling and optimization.
- [ ] Security/multiplayer stress.

## P3 — `0.0.3a+` expansion

- [ ] PL3-inspired data-address/method graph.
- [ ] Time-series charts/history.
- [ ] Alerts/notifications.
- [ ] Network topology debugger.
- [ ] Layout/template library.
- [ ] Team permission model.
- [ ] Handheld diagnostic terminal.
- [ ] Foundations ecosystem dashboards.
- [ ] Computer/peripheral integration.
- [ ] Data-driven custom readers/providers.

---

# 19. Definition of “PL2 parity” for PL4

PL4 should only claim strong Practical Logistics 2 parity when all of the following are true:

1. The major released PL2 components exist and behave recognizably like PL2.
2. The multipart placement/connectivity rules support normal PL2 build patterns.
3. Reader channel selection and modes are restored or intentionally modernized with equivalent capability.
4. Displays support the important original information types and editor interactions.
5. Connected displays behave predictably through expansion, split, reload, and chunk boundaries.
6. Holographic displays look and behave correctly in every mount orientation.
7. Transfer Nodes have proper filters/channels/routing and cannot lose or duplicate resources.
8. Wireless/Array/Entity/Wireless Storage systems have real UIs and lifecycle behavior, not only raw stored links.
9. Redstone Signaller/Clock functionality reaches useful original parity.
10. Important historical integrations are either mapped to modern APIs or explicitly documented as retired.
11. A complete native automated test suite passes.
12. Client visual acceptance passes across common GUI scales and view angles.
13. Dedicated-server and save/restart tests pass.
14. No known critical item/fluid/energy loss, duplication, permission bypass, or save corruption issue remains.

PL4 does **not** need to reproduce old Forge/Sonar APIs byte-for-byte. The target is behavioral and visual parity on a safe modern architecture.

---

# 20. Final direction

The project has moved well beyond a registration-only port. The current alpha has a real internal multipart system, server network graph, typed displays, large canvases, reader telemetry, a working hammer, a modern Field Guide, and a conservative transfer engine.

The next mistake to avoid is broadening scope before the foundation is frozen.

Recommended order from here:

1. **Visually accept the R17 hologram anchor.**
2. **Native-test R16 transfers and all 114 GameTests.**
3. **Freeze `0.0.1a`.**
4. **Use the frozen alpha as the migration baseline.**
5. **Spend `0.0.2a` completing PL2 parity, especially GSI, channels, transfers, wireless, and integrations.**
6. **Only after that, use PL3 and new Foundations ideas to expand beyond PL2.**

That gives Foundations PL4 a clear identity:

> **PL2’s recognizable logistics/monitoring experience, rebuilt safely for Minecraft 1.21.1, then extended into a modern programmable information and automation platform.**

---

# Appendix A — Current R16 config surface

Current server configuration is intentionally small:

```text
network.updateTicks
network.maximumHosts
network.maximumRows
network.wirelessEnabled
network.crossDimensionWireless
network.entityScanRadius
transfer.enabled
transfer.itemsPerCycle
transfer.millibucketsPerCycle
transfer.fePerCycle
energyReader.mekanismJoules
energyReader.gregtechEU
energyReader.maximumContainersPerHandler
```

This is adequate for `0.0.1a` but should grow carefully as new systems become real.

---

# Appendix B — Current display element contract

```text
TEXT
ITEM
BLOCK
INVENTORY
FLUID
FLUID_GRID
BAR
```

Current editor limits:

```text
MAX_ELEMENTS = 32
MAX_PAGES = 8
MAX_ICONS = 128
MAX_CANVAS = 4096
```

These limits are safety bounds, not necessarily permanent UX limits.

---

# Appendix C — Current native test count by revision source file

```text
PLGameTests.java   16
R5GameTests.java   15
R6GameTests.java    4
R7GameTests.java   23
R8GameTests.java   15
R9GameTests.java   20
R10GameTests.java   5
R11GameTests.java   5
R13GameTests.java   3
R16GameTests.java   8
--------------------
TOTAL             114
```

R12/R14/R15 primarily added source/offline/render guards rather than additional native GameTest classes.

---

# Appendix D — Upstream provenance reminders

Primary parity reference:

```text
Practical Logistics 2
https://github.com/SonarSonic/Practical-Logistics-2
commit 4772196103d35c78c33f03c288a7b47aac267197
```

Historical reference:

```text
Practical Logistics
https://github.com/SonarSonic/Practical-Logistics
commit 4b82002021cba1788b59b3279a7ade97ebca2357
```

Future design research only:

```text
Practical Logistics 3
https://github.com/SonarSonic/Practical-Logistics-3
commit bfd78dee190fef0964470549c3e023ddff3a2a61
```

Legacy Core audit:

```text
Sonar Core
https://github.com/SonarSonic/Sonar-Core
commit f5b64e033e2c07af55c2d5f474052fb83fba5ca1
```

Maintain these pins in the project so future refactors can always answer “which original code were we comparing against?”

## R1.10 combined display workflow update

Implemented on both active tracks: schema-1 JSON whole-layout export/import, client-local named templates, eight-page spatial previews, fit-to-screen bounds, fresh imported identities, reader-binding clearing, revision-fenced and undoable apply, and item/fluid/energy starter boards. See the current field guide. Optional server templates, Operator-held layout copying, element lock/hide/grouping and the remaining integration backlog are still pending.


## 0.0.2a.R2 — first reader/provider batch

Implemented: persisted endpoint pins independent of data mode; bounded target cycling and reset; no fallback for missing pins; fluid tank SLOT selection; held-item filter shortcut; trimmed Info Reader metric keys; read-only provider registry with bounded namespaced results, failure isolation and unregister handles. Vanilla information sampling now goes through the registry.

Still open in Phase 2: user-named channels and richer searchable selection, combined-inventory deduplication, full per-slot/per-tank presentation, richer network diagnostics, third-party machine providers and installed-mod acceptance. This batch does not mark the entire R2 parity phase complete.


## 0.0.2a.R2.1 — reader correctness closeout

Completed: paginated/searchable endpoint selection; saved per-reader aliases; correct pin status beyond 64 endpoints; current-network validation of target edits; exact handler and vanilla combined-inventory deduplication; network availability diagnostics; current 26.3 furnace keys; registration generation safety; immutable provider snapshot reuse; dead-code cleanup.

Release acceptance remains explicitly open for real-client visuals, two-player gameplay and installed optional power mods. Generic overlapping modded storage views require provider-specific identities; no heuristic equality-based deduplication was added. Broader machine providers, richer per-slot/per-tank presentation and full Phase 2 parity remain follow-up work.


## 0.0.2a.R2.2 — final audit performance fix

Reader-only browsing controls no longer invalidate network topology. Native packet coverage verifies unchanged topology build count across successful search/page edits. This completes the current correctness cleanup, while the client/multiplayer/installed-mod acceptance boundaries listed above remain open.
