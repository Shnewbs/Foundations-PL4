# Foundations PL4 0.0.1a.R8 — Displays and Field Guide

**SOURCE CANDIDATE. No compiled R8 runtime JAR is included.** Target: Minecraft 1.21.1, NeoForge 21.1.250, Java 21. Baseline: exact recovered R7 Multipart-Connections source. The native Gradle attempt failed before Minecraft API compilation with `UnknownHostException: services.gradle.org`.

## Large-display edge expansion

Hold another Large Display and right-click a placed Large Display's thin side edge. R8 extends one block along that edge while preserving the existing mounting face, viewing front and owner. Clicking the outer fifth of the front/back surface also extends toward the nearest rim. At an exact corner, the horizontal screen direction wins. A center click does not guess a perpendicular placement: it shows an edge-placement hint and consumes nothing. Sneak-placement retains the independent placement path.

Smart expansion works in all six mounting orientations. A removed/saved panel used to extend a canvas explicitly adopts that canvas's plane/front and active settings; its old saved layout does not override the board. Solid blocks, occupied display slots, overlapping components, inaccessible/protected areas, unloaded target chunks, world bounds and a connected plane larger than 16 by 16 are rejected before placement/item consumption. No blocks are force-loaded or silently replaced. A permitted placement consumes one item outside Creative.

Same-owner, same-front, same-plane Large Displays still join only when the connected shape forms a complete rectangle no larger than 16 by 16. Intermediate L-shapes and holes remain individual panels until filled; R8 does not invent missing tiles or implement arbitrary-shaped canvases. Expanding one edge places one tile, not an automatically filled row. Visual joining does not merge the machine/transfer networks behind screens.

## Keeping the active canvas layout

R8 stores a layout revision alongside the existing part record. The latest edited shared layout is mirrored to every joined tile. Growth to the left/up, removal of the old top-left controller and save/reload no longer depend on one tile being the sole saved copy. Rebuild chooses a donor deterministically: newer revision, then configured content, previous controller, top-left and UUID tie-break. An intentionally cleared newer layout wins over an older nonempty layout. Explicit front flips snapshot the active settings before rebuilding and retain those settings when the flipped canvas joins another group.

**Behavior change from R7:** splitting a joined canvas now leaves surviving pieces with its shared settings. Joining deliberately replaces hidden per-tile local layouts with the active shared layout; R7's separate local sublayouts are not retained underneath. Back up worlds/layouts before merging configured boards. Revisions describe saved editing order, not real clock time. Old saves without this field start at revision zero. Flips and extension preflight ownership/protection before settings can be mirrored into a joined neighbor.

## Holographic displays

Normal and Advanced Holographic Displays now separate the projector mounting face, projection origin and text viewing frame. Floor/ceiling projectors render upright text rather than using the floor/ceiling as the text plane. The projection is placed away from its base. Viewed from behind, the same plane is rendered with a readable right-handed text frame instead of mirrored glyphs; only the camera-facing copy is submitted. This is two-sided planar text, not a free camera billboard.

New floor/ceiling projectors face toward the placing player. Settings -> **View: north/east/south/west** rotates their viewing axis and base yaw. The base's outline/collision uses the same yaw, and a rotation that would overlap another part is rejected. Wall-mounted projectors derive their viewing axis from the mounting wall; the View control is disabled there. Existing projectors acquire a deterministic horizontal view without moving their mounting slot or ports. Their old flat Front flag is no longer used by the projection. Flat screens keep R6's inward/outward controls.

This is a projection-facing correction, not connected hologram canvases, volumetric geometry, full advanced holographic/GSI tooling or a claim of exact historical animation parity. Actual Minecraft visuals remain unverified.

## Foundations PL4 Field Guide

The existing `foundations_pl4:plguide` item is now named **Foundations PL4 Field Guide**, has a new pixel-art book skin, and opens the new technical manual. Existing books and recipe references keep their registry ID. The unchanged shapeless recipe uses one Minecraft book and one item in `c:gems/sapphire`.

The re-authored presentation follows the Foundations field-manual design: two parchment pages inside a dark leather/metal-trimmed cover, visible center binding, left-edge icon tabs, a bookmark, integrated lower-left search and independent list/detail scrollbars. Cyan circuit-like accents, dark chapter header plates and a Node/Cable/Reader/Display specimen strip distinguish PL4. It is a standalone screen, not a dependency on Soil, Framework or Patchouli, and does not alter those mods.

There are **22 chapters in four sections**: Start, Networks, Displays and Reference. The content covers the current compact reader/panel setup, side expansion and rectangular limits, holograms, hammer GUI/clearance, inventory/fluid/info/energy readers, unit-separated FE/J/EU telemetry, visual-only remote output, wireless/transfer limitations, redstone, configuration, troubleshooting and JSON-recipe KubeJS compatibility. The old PL2 language text remains in assets for provenance but is no longer the guide's visible content. Documentation is written for the implemented 1.21.1 port; it explicitly identifies unfinished capabilities and does not represent unrun native acceptance as passed.

Search examines titles, summaries and body text across all sections. Clear it to return to the active section. Use +/* to bookmark the current chapter and Saved/All to filter bookmarks. Previous/next moves between chapters. Wheel-scroll each pane independently; thin thumbs support dragging and track clicks. Page Up/Down scrolls the reading pane when search is not focused. Escape always closes the guide, including from the search box. At narrow GUI widths a Contents/Read toggle replaces the two-page arrangement rather than shrinking text to a fractional scale. Text uses integer native GUI coordinates; world blur runs before the book and widgets. Item specimen stacks are cached for the lifetime of the screen, not recreated every frame.

Only last chapter and bookmarks are saved locally to `config/foundations/pl4_guide.json`, with bounded loading and atomic replacement. Guide content is the bounded resource `assets/foundations_pl4/guide/en_us.json`; resource packs can override it or provide a language counterpart. Reopen after a resource reload. Malformed/oversized content opens a recovery chapter and logs the error, without touching world data. No monitoring threads or server guide subscriptions are added.

## Preserved behavior and compatibility

R4 screen-layer corrections, R5 hammer structure/menu/recipes, R6 read-only native-energy providers and KubeJS example, and R7 separate slots/typed visual wiring are retained. No original PNG or recipe is changed: 113 original PNGs and all 35 crafting/forging recipes remain byte-identical; three new PNGs are added for the book. Two item/language JSONs select the new skin and name. No EU/J transfer/conversion, universal mod provider, dedicated PL4 KubeJS API, full GSI editor, or external multipart dependency is added.

Host schema remains 2 with additive `hologramView` and `layoutRevision` fields. Part identities, registry IDs and the 13-slot scheme stay unchanged. **Payload protocol is now 3: client and server must both run R8.** Use a copied world for first acceptance. Downgrading source cannot reverse mirrored layout edits; restore a matching prior world backup to roll back safely. Do not downgrade an R7/R8 multipart world to R6's older slots.

## Apply and build

Extract the complete R7-to-R8 updater ZIP. Run `UPDATE-FoundationsPL4-R7-to-R8.bat` from its extracted folder (Downloads/Desktop is fine). Select the R7 source directory containing `build.gradle` and `gradlew.bat`, not the Minecraft mods directory or outer source ZIP root. It validates all affected hashes, backs up changed files under `.foundations_update_backups`, applies the source delta and offers source-only, build, or build plus all **73 server GameTests**. Failed compilation leaves updated source and backup, not a claimed usable runtime.

Java 21 JDK and first-build Internet access are required. The equivalent command is:

```bat
gradlew.bat --no-daemon --console=plain clean build runGameTestServer
```

Only after a successful native build should the runtime be at:

```text
build/libs/FoundationsPL4-1.21.1-0.0.1a.R8.jar
```

Install that runtime on the intended client and server in place of the older PL4 runtime. Do not install ZIPs, source JARs or the Gradle wrapper as a mod. The updater does not install mods, edit worlds, replace live configurations or enable scripts. See VALIDATION.md and R8_ACCEPTANCE.md.
