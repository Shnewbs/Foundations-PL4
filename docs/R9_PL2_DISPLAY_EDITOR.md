# Foundations PL4 0.0.1a.R9 — PL2-style display elements and editor

**SOURCE CANDIDATE. No R9 runtime mod JAR is included.** Target: Minecraft 1.21.1, NeoForge 21.1.250, Java 21. Baseline: the attached R8 Displays-FieldGuide source. Native compilation was blocked before it began by the Gradle distribution download (`UnknownHostException: services.gradle.org`).

## Scope and visual reference

Released Practical Logistics 2 is the visual/interaction reference, not the unfinished PL3 project or the generated CurseForge promotional art. R9 replaces the text-only presentation bottleneck with real typed render commands and a bounded left-edge, world-space editor. It is a substantial implementation pass, not a claim of complete GSI or pixel-perfect PL2 parity. Original block models, screen fronts, cable geometry, mounting rules and PNG textures remain intact.

## The inventory-list bug

R8's generic Mode setting changed data selection but the renderer only drew automatic text rows or text/text-based bars. R9 separates the reader's **Data** mode from the display's **View** mode and each element's **Type**. Saving an element selects `CUSTOM`. `AUTO_LIST` remains an explicit legacy view, not an implicit error fallback. Changing View does not delete the custom layout; deleting its last element leaves a blank custom screen.

Seven element types now dispatch to distinct render paths:

| Type | Actual implementation |
|---|---|
| Text / value | Caption and optional selected numeric value/unit. |
| Item icon | Native item GUI model with bounded component-aware preview; optional quantity and name. |
| Block model | Isometric default block-state model, only for an actual BlockItem. It is not a live block-entity/machine renderer. |
| Inventory grid | Real item pictures with columns, start offset, optional labels and quantities. Numeric/fluid rows are not treated as items. |
| Fluid tank | Fluid's native still sprite and tint, tiled/cropped, with stored/capacity fill when supplied. |
| Fluid grid | Per-fluid graphical samples, optional amounts/names; not per-tank geometry or extraction controls. |
| Progress / energy bar | Filled horizontal/vertical geometry using a real value/capacity pair; unknown/zero capacity is not invented as a full bar. |

Unavailable or incompatible samples show a diagnostic in that element instead of an inventory list. A block type with an ingot source says to select a block item. An optional static resource ID is a sample of one, not an inventory count. Multiple component variants can be bound by their exact suffixed row keys; a bare registry ID selects the first matching variant.

## Using the editor

1. Right-click a monitor to enter its unblurred world-space editor. Empty-hand Shift-right-click on a paired panel still opens its underlying reader.
2. Click **+** on the monitor's left edge. Cycle Type, choose an authorized Reader (or the display default), and **Pick** its data row. Add element commits the typed choice, not just a text setting.
3. Click/select an element, drag it to move, or drag the lower-right handle to resize. **# / G** toggles 4-pixel snapping. **E** opens properties, **X / Delete** removes, **C / Ctrl+D** duplicates, and **^ / v** reorder within the icon/text layer categories.
4. Use the bottom **< / >** controls for eight stored pages. The properties page can assign an element to a different page. **?** opens Data / Settings, including View AUTO_LIST/CUSTOM, selected reader and front controls.
5. Escape closes the editor. Late save/preview replies are ignored once its matching screen is closed; they do not deliberately reopen it.

The property and data-picking windows are native screens. The toolbar, selection outline and drag/resize handles are drawn on the actual monitor plane. The editor captures the renderer's actual model-view-projection matrix for pointer unprojection, including perspective, mounting, screen size and hologram frames. No fixed-FOV or guessed-world-axis hit mapping is used. This matrix/renderer integration is source-implemented, not graphically verified here.

Limits: 248x120 logical canvas, 32 elements across eight pages, 128 rendered item/fluid pictures per canvas, 64 visible reader choices and eight explicit source bindings. Explicit sources have at most 64 rows each and 256 rows in total. A picker includes only readers that the existing visual topology already exposes; entering a UUID does not grant network access. Long content is clipped/truncated to the configured element; mod-supplied special renderers still need visual acceptance for overflow behavior.

## Quantity and editor layering

The ordering is **display surface → item/block/fluid picture → quantity text → editor controls**. Quantity text is not rendered with the native GUI inventory-decoration routine, whose offsets are inappropriate for a world monitor. Icons use a flattened model-depth transform, and counter/editor planes remain distinct. There is no global disable-depth-test workaround.

The separation uses 0.01 **model units**, divided by 16 before canvas scaling: picture +0.000625 block, counter +0.00125 block, editing overlay +0.001875 block relative to the canvas plane. A small bounded intra-category ordering bias is below the next category; growing a 16x16 screen does not multiply layer depth. The underlying R8 surface offset is separate. This is intentionally not a 0.03-block protrusion. Edge-on readability, custom item models, glints, shaders, batching and all viewing orientations remain native visual acceptance items.

## Data snapshots, performance and edit safety

Inventory visuals aggregate visual component variants rather than registry IDs alone. Pictures carry one item; total quantities remain separate numbers. Nested `CONTAINER` and `BLOCK_ENTITY_DATA` components are removed from picture metadata. A preview is capped at 4096 bytes; component-bearing previews share a 32768-byte per-reader budget, with small base-ID fallback records when the budget is exhausted. Fallback variants can combine. Large integer totals retain the existing double precision boundary above 2^53. Distinct Nodes exposing the same combined inventory are not globally deduplicated.

Only aggregated/selected rows are serialized. The renderer caches typed plans with weak keys; it does not query remote inventories or capabilities during rendering. Item/fluid picture count and layout size are bounded. Editor drags preview locally and send one final transaction on release, not one packet per mouse movement. These are code properties, not a measured frame-time, memory or large-server performance guarantee.

The server validates the clicked tile's identity, owner/protection, loaded state and distance, then checks the whole joined canvas. Layout changes carry an expected revision; stale changes, malformed properties, duplicate IDs and unavailable source selections are rejected rather than overwriting newer settings. Layout rate is capped at eight requests per player/tick; inspect requests use a four-request bound. Disconnectable player keys are weak references. Same-world screen edits remain serialized on the server thread.

Joined-screen layout copying includes typed elements, mode and page. Expansion, controller removal and R8 mirroring remain the same mechanisms. A stale local snapshot cannot replace a newer reply. R8 legacy text/bar elements migrate to equivalent R9 types; an absent legacy custom layout stays AUTO_LIST. Unknown future types cannot be restored faithfully by an older build.

## Field Guide cleanup

The guide remains a Foundations two-page field manual, not a PL2 copy. Its main title is now FOUNDATIONS PL4, with FIELD GUIDE / TECHNICAL MANUAL underneath. Four consistent schematic side-tab glyphs replace tiny/dark block thumbnails. Chapter hover summaries occupy a reserved two-line area below the list instead of a large tooltip over the reading pane. The search hint is drawn once without a heavy shadow. Bookmarks is an explicit highlighted filter. Heading padding and full-line clipping stop partially drawn section/title text at the bottom edge. Native integer-coordinate text and background-before-content order are retained.

All 22 chapters are current for this R9 source, including typed presentation, reader data vs display view, static/live sources, counters, pages, conflict handling and limitations. The `foundations_pl4:plguide` item ID, recipe, personal bookmark file and resource override path are unchanged. `docs/FIELD_GUIDE_R9.md` is exported from the same JSON.

## Preserved work / not included

R4 GUI layering, R5 hammer, R6 front and optional read-only FE/J/EU telemetry, R7 compact connections and visual-network separation, and R8 expansion/hologram/book work are retained. All 116 pre-R9 PNGs (113 original plus three guide assets) and all 35 recipes are byte-identical. One 1x1 white rendering primitive sprite is added; no existing texture is replaced.

Not included: complete original GSI actions, nested element containers, hyperlinks, inventory/fluid extraction from screen controls, layout export/sharing, arbitrary large-screen shapes, live remote block-entity renderers, full historical integrations, or a dedicated KubeJS display API. KubeJS remains the existing recipe/data compatibility example; it was not runtime-tested here. No energy conversion or EU/J transfer is added.

## Update, build and rollback

Extract the entire R8-to-R9 updater and run `UPDATE-FoundationsPL4-R8-to-R9.bat`. It may stay in Downloads/Desktop. Select the **R8 source directory containing build.gradle and gradlew.bat**, not your mods folder. Changed-source and payload hashes are checked before writes; backups go to `.foundations_update_backups`; build logs go to `.foundations_build_logs`. Local edits to affected files cause rejection so they can be merged deliberately. Unrelated custom files are not overwritten.

Choose **3 — Build and all 93 server tests**, or run `gradlew.bat --no-daemon --console=plain clean build runGameTestServer` with Java 21 JDK. After a successful local build the intended runtime is `build/libs/FoundationsPL4-1.21.1-0.0.1a.R9.jar`. The package contains only the Gradle wrapper JAR, not a precompiled mod. Install matching R9 builds on client and server: payload protocol is now 4.

**Test a copied/backed-up world.** Host schema2/13 slots remain, with additive typed element/mode/page fields. R8 and earlier builds cannot preserve the new layouts; source rollback does not restore world data or previous mirrored layouts. Preserve the matching world backup before editing.
