# Foundations PL4 Field Guide

Minecraft 1.21.1 | NeoForge | R9

This text matches the bundled R9 guide. Native graphical and gameplay acceptance remains separate.

## Start here

Read a chest, then put the result on a screen.

### FOUNDATIONS PL4

A data network connects your machines to readers. Readers turn inventories, fluids, energy or world information into rows. Displays show those rows. This guide describes Foundations PL4 for Minecraft 1.21.1 and NeoForge, not an older Practical Logistics release.

### YOUR FIRST NETWORK

1. Attach a Node to a chest.
2. Add a Data Cable to the Node host and extend the cable.
3. Mount an Inventory Reader on the cable.
4. Right-click the reader and check its Data tab.
5. Click the reader outward face with a normal, mini or large display. Reader and panel share the same block.

### READ THE RIGHT SOURCE

Leave Reader name blank for automatic selection, or give the reader a unique Name and enter that name in the display settings. An Inventory Reader does not report machine energy; use an Energy Reader for that.

### OPENING THIS GUIDE

Craft a book with a sapphire from the common sapphire gem tag. Existing PL4 Guide items now open this Field Guide. The item identifier remains foundations_pl4:plguide. Right-click in the air to read it.

## Using the Field Guide

Search, tabs, bookmarks and readable pages.

### NAVIGATION

The four side tabs group chapters into Start, Networks, Displays and Reference. Select a chapter on the left; its contents appear on the right. The chapter list and detail page scroll separately.

### SEARCH AND BOOKMARKS

Search matches chapter titles, summaries and body text across all chapters. Clear the search to return to the selected section. The star bookmarks the selected chapter; Bookmarks highlights when only saved chapters are shown. The last chapter and bookmarks are stored on this client only. Hover a chapter to read its summary below the list, without a tooltip covering either page.

### SMALL WINDOWS

At narrow GUI widths, Contents switches between the chapter list and reading page instead of shrinking the text. Drag a scrollbar thumb, click its track or use the mouse wheel. Escape closes the guide, including while Search has focus.

## Reader + display

A compact same-face assembly without a cable detour.

### MOUNT THE PANEL

Click the outward face of a reader with a normal, mini or large display. It uses a separate display slot, leaving the reader in place. The screen should be viewed from that outward side.

### ACCESS THE READER

Right-click opens the panel settings. Shift-right-click with an empty hand opens the reader underneath. Sneak-use the Operator on the front panel to remove that component without deliberately breaking the entire host.

### INPUT IS NOT OUTPUT

A reader takes machine-network input behind it. Its front carries visual output. A cable on the front may feed remote displays, but is not a bridge for machine access or item/fluid/energy transfers.

### NO EXTRA BRANCH REQUIRED

A same-face reader/panel pair does not require a cable loop around its front. A bare reader can use an exposed compatible cable in the adjacent block behind it. A disabled or obstructed local cable port cannot be bypassed by that external connection.

## Nodes and targets

Attach the network to a real machine interface.

### MACHINE CONNECTION

A Node identifies the block on its mounted side as a data target. Attach it to the chest, tank, battery or machine face you intend to inspect, then connect the Node to Data Cable.

### SIDED ACCESS

The connected face matters. A modded machine may expose inventory, fluid or energy only on particular sides or ports. PL4 does not secretly scan every side or bypass a disabled side configuration.

### MULTIBLOCKS

Use a supported port or controller rather than an arbitrary casing. Two different casing blocks can expose the same underlying store; one Node at the authoritative port avoids double counting.

### UNLOADED TARGETS

PL4 observes loaded targets and does not force-load chunks. Restore the loaded connection before expecting live rows.

## Cables and ports

Directional multipart wiring and operator controls.

### TWO CABLE FAMILIES

Data Cable carries the machine data network. Redstone Cable carries its redstone network. The families do not connect interchangeably.

### LOCAL AND EXTERNAL PORTS

A compatible cable centre can share a host with face components. Exposed compatible external component ports may also connect to an adjacent cable. Merely touching sideways is not a port. Reader network input and visual output are separate.

### OPERATOR

Right-click a cable arm with the Operator to disable or re-enable that port. When permitted, the corresponding neighbor cable port is toggled too. Sneak-right-click removes one selected component; its settings and pending transfer state stay with its dropped item.

### BLOCKING AND OWNERSHIP

An occupied face, disabled port, different cable family, overlapping part or protected position can reject placement or connection. Do not defeat this by routing a cable through the same blocked face.

## Inventory Reader

Counts, filters and inventory views.

### DATA FIRST

Open Data on the Inventory Reader before configuring a display. It lists supported item-handler inventories reached through Nodes. Click a data row to copy its key for a display element.

### SETTINGS

Name identifies this reader to a display. Filter IDs / tags accepts the port filter syntax; allow/exclude selects the filter direction. Sort chooses high or low values first. Data selects LIST, STACK, SLOT, POS, STORAGE or CHANNEL; index selects a slot or channel where applicable. This controls the data, not whether a display draws a block, item, grid or bar.

### CURRENT LIMITS

R9 groups bounded visual component variants separately and includes a one-item picture with each sampled item row. Quantities remain separate from that picture. Nested container inventories and block-entity data are excluded. Oversized visual metadata falls back to the base item; this can merge those fallback variants. Multiple access points to a combined inventory may duplicate it; use one authoritative target when validating totals.

### PICTURES AND QUANTITIES

In the display editor choose Item icon, Block model or Inventory grid. Pick the reader and actual row key. A key with a variant suffix selects that specific visual variant; a plain item ID matches the first such variant. Block model requires an actual block item. A STORAGE row is numeric and has no item picture. Exact and compact quantities are optional overlays, not ItemStack sizes limited to 64.

## Fluid Reader

Read compatible tanks in millibuckets.

### CONNECT A TANK

Put a Node on the tank fluid-handler face. Connect a Fluid Reader to the data network and inspect Data. Tank amounts, capacity and fluid IDs are available when exposed by the target.

### FILTER AND DISPLAY

Filter by fluid IDs/tags as needed. Choose Fluid tank for a tinted still-texture fill, Fluid grid for one icon per sampled fluid, or Progress / energy bar for a numeric storage row. Tanks fill bottom-up from stored/capacity; grids show a fluid sample rather than a fullness meter. Per-fluid capacity sums the nonempty exposed tanks that contain that fluid; STORAGE includes all reported tank capacity. This is not a per-tank selection editor.

### READING IS NOT PUMPING

A Fluid Reader only observes. Actual movement requires configured Transfer Nodes and a source/destination that allow extraction/insertion. Real modded tank behavior still needs validation in your pack.

## Energy Reader

Keep FE, EU and Joules in their native units.

### SELECT ENERGY

Connect a Node to an exposed energy face. Add an Energy Reader, give it a unique name such as power_main, and inspect Data. Point the display Reader name to power_main; an Inventory Reader will not supply power values.

### PROVIDERS

AUTO prefers a usable optional native provider, then FE. FE uses the NeoForge storage capability. J queries an exposed Mekanism strict-energy handler. EU queries GTCEu energy information/container interfaces. Optional native providers are read-only and depend on the installed mod version.

### DATA KEYS

FE total: storage.
EU total: storage:eu.
Joule total: storage:j.
Totals are separate; PL4 does not add unlike energy units or assume conversion constants.

### CHECK THE MACHINE

A supported empty battery can report zero. Missing, unloaded, blocked or unsupported storage is not invented as a zero-capacity battery. Compare stored/capacity with the machine own GUI. These values are not generation or consumption per tick.

### BOUNDARIES

Transfer Nodes still transport FE only. This is not universal energy support: AE2-native grids, IC2-specific power and Create stress/RPM are not added here. Very large telemetry above 2^53 may lose integer precision.

## Info and Network Readers

World, entity and network information.

### INFO READER

The current sampler covers selected numeric properties such as position, redstone, time/weather, hardness, growth and selected machine/entity values. Supported rows depend on the target. Not every mod property has a provider.

### NETWORK READER

Use the Network Reader for current host/component and target counts. It is a diagnostic view, not the full historical channel editor.

### DISPLAY A VALUE

Use Data to discover actual row keys. Copy the key, select this reader on your display, then add a text or bar element.

## Displays and the element editor

Choose actual item, block, grid, fluid, text or bar elements.

### CHOOSE A PANEL

Mini, normal and large flat panels use separate display mounting slots. Normal and mini displays stay individual. Large panels form a rectangular shared canvas. The same typed elements also draw on holographic displays. Joining screens does not join machine or transfer networks.

### OPEN THE EDITOR

Right-click a display to open its world-space editor. The left-edge controls belong to the monitor, not a full-screen inventory list. The world remains visible but mouse look is released for editing. Escape closes the editor. Empty-hand Shift-right-click a paired panel still opens the reader underneath.

### ADD A BLOCK OR ITEM

Click + on the left. Cycle Type to Block model or Item icon. Pick Reader (or keep the display default), then Pick the actual data key. Set position, size, caption and optional Quantity. Add element saves the typed element and changes the display to CUSTOM. Block model requires a BlockItem, so an ingot cannot silently become a block. A missing or incompatible sample shows a diagnostic, not the old inventory list.

### PRESENTATION TYPES

Text / value draws a caption and optional live numeric row. Item icon uses the item GUI model. Block model draws the block default-state model. Inventory grid lays out actual item pictures with columns, offset, optional names and quantities. Fluid tank draws a tinted graphical fill. Fluid grid draws fluid samples. Progress / energy bar draws a horizontal or vertical value/capacity fill. Numeric storage rows are intended for Text or Bar.

### READERS AND STATIC PICTURES

Each element may use the display reader or a specifically selected reader visible on that display network. The picker does not search private or unrelated networks. Up to eight explicit reader bindings are supported; explicit previews contain at most 64 rows each and 256 rows combined. An optional static item/fluid ID draws a fixed sample of one, not a live inventory quantity. Selecting a data key clears the static ID.

### MOVE RESIZE EDIT

Click an element to select it. Drag to move; drag its lower-right handle to resize. # or G toggles a four-pixel snap. E edits properties; X or Delete removes the selected element; C or Ctrl+D duplicates it. ^ and v reorder elements inside the icon/text layer categories. The canvas is 248 by 120 logical pixels and bounds are clamped. Quantities remain above icons; editing controls remain above both.

### PAGES AND AUTOMATIC MODE

The bottom < and > controls select one of eight saved pages. Properties can assign an element to another page. Up to 32 elements are stored across all pages and up to 128 item/fluid pictures are drawn on one canvas at a time. ? opens Data / Settings; View switches between AUTO_LIST and CUSTOM. AUTO_LIST is the explicit up-to-eight-row legacy view, not a fallback. An empty custom layout stays blank. Switching modes retains saved elements.

### SAFE SAVING

Drag updates are local until mouse release. The server checks the clicked tile, identity, edit permission, distance and canvas membership, then commits a revisioned change. A stale edit is rejected instead of overwriting another editor or a newer joined layout. Reopen properties after a conflict and review the latest settings. This is a bounded PL2-style editor pass, not the complete historical GSI action/container system.

### VIEWING FRONT

? then Settings > Front changes inward/outward viewing without moving mounting slots or cable ports. A new reader-mounted panel faces outward. Older panels retain their explicit front. For floor/ceiling holograms use View instead; their text remains upright and readable from either side.

## Expand a large screen

Side-click extension that preserves plane and layout.

### EXTEND THE EDGE

Hold another Large Display and click the thin side edge of an existing Large Display. You can also aim near the rim of its front/back surface. The new tile is placed beside that edge with the same mounting face and viewing front.

### AIM AT THE RIM

A click in the centre is not an extension direction. Aim within the outer fifth of the panel, or at the physical side. Sneak-place to use ordinary independent placement instead of edge extension.

### JOIN RULES

Joined panels must share owner, type, plane, mount and viewing front. A complete rectangle may be at most 16 tiles wide and 16 high. There must be no holes. During an L-shaped intermediate build, tiles remain individually usable until the rectangle is complete.

### KEEP YOUR CONTENT

The active selection, colour and layout are mirrored to members of a valid canvas. Extending left/up, removing the old top-left controller, saving or reloading no longer intentionally replaces that layout with a blank tile. Split pieces keep the last shared settings.

### MERGING CONFIGURED SCREENS

When two configured canvases merge, the newest stored layout revision wins. Deterministic ties prefer an existing controller then the new top-left. Back up distinct layouts before joining; a canvas has one shared layout.

### REJECTED PLACEMENT

Solid blocks, conflicting host parts, ownership/protection, world limits and unloaded positions reject extension without consuming the display. A saved display item adopts the existing canvas plane/front/layout when used to extend it.

## Holographic displays

Readable projections instead of backwards flat-screen text.

### SEPARATE PROJECTOR AND TEXT

A holographic display is a projector, not a flat panel. Its base stays mounted on the surface while its text plane projects away from that surface. Floor and ceiling projectors now present upright text, not a horizontal monitor skin.

### READ FROM EITHER SIDE

The projection renders one readable view toward the observer on either side of its text plane. The rear view is drawn in a new coordinate frame, rather than showing mirrored letters. It is not a free-spinning camera billboard.

### VIEW DIRECTION

Floor/ceiling projectors initially face the placing player. Settings > View rotates their readable plane through the four horizontal directions. Wall projection is derived from its mount and is also readable from either side. Legacy projectors use a deterministic default; View can adjust a floor/ceiling one.

### DATA AND LIMITS

Select a reader and use the same basic text/bar layout. Normal and advanced holographic bases are supported. Holograms do not join into large flat-screen rectangles. This is an orientation repair, not full historical advanced projection/editor parity.

## Remote displays

Export reader telemetry without joining machine networks.

### VISUAL OUTPUT

The reader outward side can feed a separate compatible visual cable run. A display on that run can select the exporting reader. This is useful when the screen is not directly attached to the reader.

### ISOLATION

That visual export must not authorize an attached Transfer Node to access the source machine inventory, tank or power. It only exposes reader rows. Keep machine-network cabling and visual export intent distinct.

### NO ROWS

Check the reader Data tab, output direction, cable family and disabled ports. Check the display selected reader name. A larger screen with no supported source is still an empty screen.

## Forging Hammer

Three-block machine with a native inventory.

### PLACE THE MACHINE

Leave two clear air blocks above the base. The hammer occupies a three-block-tall structure. Placement must not replace a roof or other obstruction. An obstructed older hammer pauses and preserves inventory.

### USE THE INVENTORY

Right-click the base or either upper part. Put an ingredient in the left slot; collect the result at the right. Shift-click moves compatible stacks. The arrow shows processing progress; cooldown returns the hammer before another stroke.

### AUTOMATION AND REMOVAL

Automation inserts input and extracts output. The result slot rejects insertion. Removing an upper part dismantles the base; take normal precautions with machine inventory. The upper structure parts have no separate item drops.

### RECIPE TIMES

Bundled recipes use 100 processing ticks and 200 cooldown ticks. A datapack or KubeJS custom recipe can change those durations. The currently active recipe data is authoritative.

## Materials and recipes

Sapphires, plates and the guide recipe.

### SAPPHIRE

The port registers sapphire ore, sapphire and sapphire dust. Overworld generation is configured in its datapack world-generation resources; it is not a promise to match every historical vein rule.

### BUNDLED FORGING

1 tagged stone -> 4 Stone Plates.
1 tagged diamond -> 4 Etched Plates.
1 tagged redstone dust -> 4 Signalling Plates.
1 tagged ender pearl -> 4 Wireless Plates.
1 tagged sapphire -> 1 Sapphire Dust.
1 tagged sapphire ore -> 2 Sapphire Dust.

### FIELD GUIDE

Combine a book and a tagged sapphire gem in a crafting grid. The result is the Foundations PL4 Field Guide. Pack recipes can override these defaults; consult the active recipe data.

## Wireless links and Arrays

Bind explicit targets without an external core.

### EMITTER TO RECEIVER

Sneak-right-click one of your data emitters with a Transceiver to bind it, then right-click a matching receiver. Redstone emitters/receivers use their matching family. Ownership and configured wireless permissions still apply.

### ARRAY

Sneak-right-click a block target to bind its position/side, then use the bound tool on an Array. The port supports up to eight saved links. It does not reproduce the original eight physical transceiver inventory slots.

### ENTITY LINK

Use the Entity Transceiver on a living entity to bind its identity, then add a supported link to an Entity Node or Array. An unlinked Entity Node can supply a nearby-entity count.

### LOADED WORLDS

Cross-dimension wireless is configurable. Linked targets still need to be loaded; PL4 does not force-load them. Clear links through component Settings when replacing a target.

### WIRELESS STORAGE

Wireless Storage currently opens a bound-target read-only view. It is not a complete remote inventory management system.

## Transfer Nodes

Deliberate source/destination routing.

### SAFE START

Transfer Nodes start PASSIVE. Set a source to REMOVE, a destination to ADD, or explicitly choose ADD / REMOVE. Enable only the Items, Fluids or Energy paths you intend to use.

### SUPPORTED TRANSPORT

Transfers run on the server and respect the exposed target side. Items, compatible fluid handlers and FE are supported in the port. EU and Joule telemetry providers do not make those units transportable.

### FILTERS AND PRIORITY

Use filters and priority to limit eligible transfers. A visual-only reader connection is not a transfer route. Provider failures and unsuccessful insertion must not become item duplication.

### PERSISTENCE

Pending transfers are retained in source part state. Remove a component with the Operator rather than deleting its state externally. Crash/restart behavior against arbitrary third-party providers still needs pack-specific testing.

## Redstone and clocks

Signal networks and simple numeric conditions.

### SIGNAL NETWORK

Redstone Nodes/emitters sample supported redstone inputs. Redstone Cable and receivers propagate sampled signals on their separate network.

### SIGNALLER

The Redstone Signaller evaluates a selected numeric reader row using its comparison and threshold. A true condition produces a signal. This is a single condition, not the full original multi-statement logic editor.

### CLOCK

A clock uses the Interval setting in ticks. The port samples/pulses with the network update cadence; very short requested timing is bounded. World-time rows are not wall-clock time.

## Troubleshooting

Start with data, then connections, then presentation.

### READER IS EMPTY

Check the Node target and exposed side. Inspect cable family, occupied faces, blocked ports and loaded chunks. Confirm the correct reader type. Test a simple chest or known FE battery before a complex multiblock.

### DISPLAY IS EMPTY

Read the reader Data tab first. Check Reader name or use automatic selection. Confirm the compact pair or correctly sided visual route. In CUSTOM mode check the selected page and element bindings; an empty custom layout intentionally stays blank. In AUTO_LIST mode only the legacy rows are shown. Large-screen adjacency does not supply data by itself.

### SCREEN FACES THE WRONG WAY

For flat panels change Front in Settings. For floor/ceiling holograms use View. Do not rotate the entire cable network to compensate for a viewing-side setting.

### SCREEN WILL NOT EXPAND

Use a Large Display on the side/rim of another Large Display without sneaking. Match owner, plane and front. Complete the rectangle; remove holes. Stay within 16x16. A protected or occupied destination is intentionally rejected.

### HAMMER DOES NOTHING

Check two-block headroom, a valid active forging recipe, room in the output slot and cooldown. Recipe overrides can change ingredients, counts and processing time.

### REPORTING A BUG

Record PL4 revision, Minecraft/NeoForge versions, relevant other-mod versions and exact machine/side. Include a screenshot of the wiring, reader Data/Settings and the relevant log exception.

### WRONG DISPLAY TYPE

Reader Data mode chooses which rows are sampled. Display element Type chooses the graphical renderer. Use + > Type > Pick data > Add element, not just a reader mode change. Block model requires a block item; Item needs an item picture; Fluid needs a fluid sample; Bar requires positive capacity. STORAGE is numeric and cannot become an inventory picture. An unavailable selection is not silently replaced with an inventory list.

### EDIT REJECTED

A different player, screen expansion or settings change may advance the layout revision while properties are open. Close/reopen properties to review the newer layout before retrying. Disabled editing can also mean ownership, protection, distance, an unloaded or replaced tile, or a busy edit limit. No source/network access is granted by entering an arbitrary reader name.

## KubeJS and datapacks

Data recipes are supported; custom PL4 events are not.

### SUPPORTED LAYER

Registered items, tags, crafting JSON and the foundations_pl4:forging_hammer recipe codec can be configured through datapacks. KubeJS ServerEvents.recipes with event.custom can submit that same recipe JSON.

### HAMMER FIELDS

Use type, ingredient, input_count, result, processing_ticks and cooldown_ticks. In 1.21.1 the result uses id and count. The included examples/kubejs/server_scripts/pl4_recipes.js.example shows a deliberate recipe replacement.

### OPT IN

The updater does not install or enable KubeJS scripts. Copy and rename the example only when deliberately changing that instance/server recipe. Reload and test using your installed KubeJS version.

### NOT A PL4 SCRIPTING API

There is no PL4Events event group, scripted display controller, custom energy-provider registration API or dedicated recipe-builder DSL. Do not treat generic custom-recipe support as those missing integrations.

## Server configuration

Bounded sampling and optional providers.

### NETWORK

updateTicks sets the sample/transfer interval; default 20 ticks. maximumHosts defaults to 4096 and stops an oversized network safely. maximumRows defaults to 128 synchronized reader rows. The automatic world display shows at most eight rows; inspect Data for the full received list.

### TRANSFERS

Default per-cycle limits are 64 items, 1000 millibuckets and 10000 FE. The enabled option can disable transfers. These are caps, not measured throughput guarantees.

### OPTIONAL READERS

energyReader.mekanismJoules and energyReader.gregtechEU enable optional native read-only providers. maximumContainersPerHandler bounds a handler traversal; excessive providers are rejected rather than partially counted.

### GUIDE CONTENT

Client resource packs may replace assets/foundations_pl4/guide/en_us.json. Content is bounded and validated when the guide opens. A malformed replacement displays a recovery page and logs the error instead of crashing the screen.

### DISPLAY BUDGETS

Typed layouts are limited to 32 elements, eight pages, eight explicit visible reader bindings, and 128 rendered item/fluid pictures per canvas. Preview metadata excludes nested inventories and uses a 4096-byte per-picture and 32768-byte component budget per sampled reader, plus small base-ID fallback records. The limits are safety caps, not performance benchmark results. Different energy units remain separate; R9 does not convert or transfer EU/J.

## Port status and credits

PL2-style display restoration, current limits and credits.

### ALPHA IMPLEMENTATION

Foundations PL4 is an in-development port for Minecraft 1.21.1 / NeoForge. It bundles its own required core functionality. A separate Sonar Core or MCMultiPart runtime is not required.

### CURRENT BOUNDARY

This is not a completed stable 1:1 release. R9 adds typed item/block/fluid/bar renderers, a bounded world-space selection/move/resize/properties editor and eight pages. The full original GSI actions, nested containers, extraction controls, links, arbitrary large-screen shapes and historical integrations remain unfinished. Block pictures use the default block state, not a live remote block-entity renderer. Custom mod renderers still require pack-level visual acceptance.

### R9 VALIDATION

R9 retains R8 expansion, layout continuity, holograms and the Field Guide while restoring distinct presentation types and left-side screen controls. Source tests and render-plan tests do not prove a Minecraft build or in-game rendering. Read the release validation report for executed checks. Test on a copied world; older versions cannot preserve R9 typed layouts.

### CREDITS

Original Practical Logistics and relevant original assets: SonarSonic / Ollie Lansdell. Foundations PL4 port changes and Field Guide presentation are identified in the included NOTICE. Original MIT license notices remain in the source and built mod.

