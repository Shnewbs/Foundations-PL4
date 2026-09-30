"""Upgrade the bundled guide content. Existing chapter IDs/bookmarks remain valid.
Idempotent; does not touch the player's config or resource packs.
"""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
def chapter(id,title,summary,icon,sections):
    return dict(id=id,category='start',title=title,summary=summary,icon='foundations_pl4:'+icon,sections=[dict(heading=h,body=b) for h,b in sections])

def upgrade():
    path=R/'src/main/resources/assets/foundations_pl4/guide/en_us.json'
    book=json.loads(path.read_text());book['edition']='Minecraft 1.21.1 / NeoForge / Foundations PL4 0.0.1a.R10'
    byid={c['id']:c for c in book['chapters']}
    byid['start']=chapter('start','Welcome to Foundations PL4','Start with one chest. Turn its contents into a useful live monitor.','plguide',[
        ('WELCOME','Foundations PL4 helps you see what is happening inside your storage and machines. Connect a target, read its data, then choose how a monitor should present it. A simple chest counter is enough to learn the whole loop.'),
        ('CONNECT  /  READ  /  DISPLAY','A Node touches the target. Data Cables carry the connection. A Reader turns the target into inventory, fluid, energy or information samples. A Display presents those samples as text, item or block pictures, grids, fluid tanks or bars.'),
        ('BEGIN HERE','Use the Tutorial > button below in two-page mode, or open First inventory monitor in the chapter list. The walkthrough uses a chest, 17 stone and 3 dirt, with a check after every stage. Nothing in the guide moves items, places blocks or changes your layout for you.'),
        ('BUILD AT YOUR PACE','After the first monitor, try From list to block icon, Your first energy monitor, Grow a large display, Hologram walkthrough and Hammer walkthrough. Network, Display and Reference tabs explain the individual components in more detail.'),
        ('THIS EDITION','This guide covers Foundations PL4 on Minecraft 1.21.1 / NeoForge. Practical Logistics 2 is the visual reference; the unfinished PL3 project is not the feature checklist. Some original GSI interactions and integrations are still pending. See Port status and credits for the boundaries.'),
        ('THE GUIDE ITEM','Craft one Minecraft book with one sapphire accepted by c:gems/sapphire. Right-click the resulting Field Guide in the air. The existing foundations_pl4:plguide ID, recipe and saved bookmarks are retained.')])
    tutorials=[
      chapter('tutorial_inventory','First inventory monitor','A chest, two test stacks and a compact reader/display pair.','inventoryreader',[
        ('1  /  GATHER THE PARTS','Bring one chest, one Node, several Data Cables, one Inventory Reader, one normal Display Screen and an Operator. Large Display also works, but begin with one panel. Put exactly 17 stone and 3 dirt in the otherwise empty chest.'),
        ('2  /  CONNECT THE CHEST','Place the Node on an exposed chest face. Click that Node with a Data Cable to add the cable centre in the same host block. Extend the cable away from the chest. Check: a visible connector joins the Node to the cable; the Node still faces the chest.'),
        ('3  /  ADD THE READER','Mount the Inventory Reader on the cable network with its network side toward the cable and its outward side available for a screen. Right-click the reader before adding the panel. Give it the unique name chest_demo. Check its Data tab: stone should be 17 and dirt should be 3.'),
        ('4  /  MOUNT THE SCREEN','Click the reader outward face with the Display Screen. It occupies the separate display slot, not a new cable branch. Read it from that front face. Empty-hand Shift-right-click opens the reader underneath; normal right-click opens the on-screen editor.'),
        ('5  /  SELECT THE SOURCE','In the editor, open ? for Data / Settings. Set Reader name to chest_demo and View to AUTO_LIST. Leave Reader name blank only when automatic reader selection is unambiguous. Exit the editor with Escape. Check: the monitor shows stone 17 and dirt 3.'),
        ('6  /  PROVE IT IS LIVE','Add 5 more stone to the chest. After the configured sampling interval, the monitor should show 22 stone. Remove 1 dirt: it should show 2 dirt. A static picture of a stone item is not a live counter; use a selected reader row for live totals.'),
        ('NOT WORKING YET?','Return to the reader Data tab first. Empty data points to the Node, machine side or cable ports. Correct reader data but an empty monitor points to reader selection, display view/page or facing. Use the Operator to check disabled ports. See Reader + display and Troubleshooting before rebuilding the entire network.')]),
      chapter('tutorial_graphics','From list to block icon','Replace automatic text with a real stone model and live quantity.','displayscreen',[
        ('1  /  OPEN THE EDITOR','Use the working chest_demo monitor from the first tutorial. Right-click the screen, then click + on its left edge. The editor previews the custom canvas. An empty custom preview is normal even when the saved resting view is AUTO_LIST.'),
        ('2  /  CHOOSE BLOCK MODEL','Cycle Type to Block model. Choose chest_demo under Reader, press Pick and select the stone row. Keep Static resource blank: selecting a live row supplies the real chest quantity. An ingot is not a block item; choose Item icon for ingots or tools.'),
        ('3  /  ADD THE ELEMENT','Leave Quantity On. Set the desired page, then press Add element. A successful server reply switches View to CUSTOM and reveals that page. Expected: a stone block picture with the live count, not the old inventory text list. A rejected edit shows a message and does not overwrite the existing layout.'),
        ('4  /  POSITION AND VERIFY','Select the picture, drag it to move, and drag its lower-right handle to resize. G toggles snapping; E opens properties. Add stone to the chest and confirm the quantity changes. Leave the editor: the picture should remain after Escape.'),
        ('5  /  TRY A GRID','Use +, Type Inventory grid, Reader chest_demo, then leave Data key and Static resource blank. Choose columns and optional names, then Add element. This presents the visible item samples as pictures. Use a new page or remove the first picture to avoid accidental overlap.'),
        ('VIEW IS NOT READER MODE','Reader data modes decide what is measured. Display View decides AUTO_LIST versus CUSTOM. Element Type decides the actual renderer. Switching to AUTO_LIST keeps the custom elements saved; switch back to CUSTOM to see them again. Saving an element to another page now reveals that page automatically.')]),
      chapter('tutorial_energy','Your first energy monitor','Use the correct reader, machine face and unit before drawing a bar.','energyreader',[
        ('1  /  CHOOSE A BATTERY','Use a charged battery with a supported energy interface. Open its own GUI and note stored energy and capacity. Attach one Node to an exposed energy face; a multiblock may require its energy port rather than any casing.'),
        ('2  /  READ POWER','Connect an Energy Reader to the Node data network. Name it power_demo. Set Energy to AUTO, or choose FE, EU or J deliberately. Check the Data tab against the battery GUI. An Inventory Reader named inventoryreader does not measure energy.'),
        ('3  /  VERIFY THE UNIT','FE reads NeoForge energy storage. The optional native telemetry providers cover supported Mekanism Joules and GTCEu EU interfaces. FE, EU and J stay separate; this is not automatic conversion or native EU/J transfer. A blocked or unsupported machine should report a diagnostic, not an invented empty battery.'),
        ('4  /  BUILD A BAR','Attach or select a display, choose power_demo, then add Type Progress / energy bar. Pick a row with both stored value and capacity. Turn Quantity On. Expected: a proportional fill and matching unit. Unknown capacity cannot produce a meaningful percentage.'),
        ('5  /  WATCH IT CHANGE','Charge or discharge the battery and compare the monitor again after a sample. Storage/capacity is not generation or throughput per tick. For distinct units use separate elements. Exact telemetry above the double-integer precision limit may be rounded; see Energy Reader.')]),
      chapter('tutorial_expansion','Grow a large display','Extend an edge without turning the new panel sideways or losing its layout.','largedisplayscreen',[
        ('1  /  START WITH ONE TILE','Place a Large Display on a working reader or supported visual connection. Set its reader and add one visible custom element. Keep all tiles under the same owner, mounting face, front and plane.'),
        ('2  /  CLICK AN EDGE','Hold another Large Display. Click the existing panel thin side edge, or near the outer edge of its front. The new tile extends in that direction in the same plane. A centre click gives a hint instead of guessing. Sneak-placement uses the independent placement path.'),
        ('3  /  COMPLETE THE RECTANGLE','For a 2 by 2 board, fill all four cells. Adding only one tile to the side of a taller board can create an L-shape; the supported join needs a filled rectangle. The limit is 16 by 16 tiles. It does not automatically spend items to fill an entire row.'),
        ('4  /  CHECK THE LAYOUT','Check the original element after extending left or upward, where the top-left controller changes. Joined members mirror the shared layout. Splitting keeps the last shared settings; the old independent per-tile layouts are not hidden underneath.'),
        ('5  /  SAVE AND RELOAD','Test save/quit/reload with a copied world and confirm facing, reader selection and custom page survive. Joining display panels is a visual operation and must not connect otherwise separate machine/transfer networks.')]),
      chapter('tutorial_hologram','Hologram walkthrough','Place a projector, choose its view, then bind a real data element.','holographicdisplay',[
        ('1  /  CONNECT A PROJECTOR','Put a normal or advanced holographic display on a supported visual connection. Use a working reader so you can distinguish projection problems from missing data.'),
        ('2  /  SET THE VIEW','For floor or ceiling placement, Settings > View chooses north, east, south or west. Wall projectors follow the mounting face. Flat-screen Front inward/outward is not a substitute for projector direction.'),
        ('3  /  ADD CONTENT','Right-click to edit, choose the reader, then add an Item icon or Text / value with a live data key. A static caption is also useful for checking orientation before troubleshooting data.'),
        ('4  /  WALK AROUND IT','Check the text from both sides. The projection uses a readable two-sided plane; floor/ceiling text should remain upright. Look along an oblique angle to check counters and editor controls remain close to the projection plane.'),
        ('SCOPE','Holograms do not join into flat-monitor rectangles. This implementation does not claim volumetric geometry or the complete old advanced-hologram GSI system. Report the projector type, mounting face and View value with any remaining orientation issue.')]),
      chapter('tutorial_hammer','Hammer walkthrough','Use the three-block machine and its actual two-slot inventory.','hammer',[
        ('1  /  LEAVE HEADROOM','Place the Forging Hammer with two clear air blocks above its base. It owns those upper structure cells. An older obstructed machine pauses rather than replacing the obstruction.'),
        ('2  /  OPEN THE INVENTORY','Right-click the base or either upper part. The hammer screen has an input slot, an output slot and your inventory. This is not the old hand-insert or hand-extract interaction.'),
        ('3  /  TRY STONE PLATES','With the default recipes, put one item matching c:stones into input. The stone-plate recipe produces four plates. Processing and cooldown are data-driven; a modpack may deliberately replace the recipe or timings.'),
        ('4  /  WATCH AND COLLECT','Watch the arrow while the head processes the item. Collect the output and wait for cooldown before the next cycle. A full or incompatible output must stop production without consuming more input.'),
        ('5  /  AUTOMATE CAREFULLY','Supported automation inserts input and extracts output. Test with hoppers before adding a larger route. Breaking an upper structure part dismantles the machine; back up your world before downgrading to versions without the upper structure IDs.')])
    ]
    # Insert onboarding as a coherent sequence, retaining every existing reference ID.
    newids={c['id'] for c in tutorials}
    rest=[c for c in book['chapters'] if c['id'] not in newids and c['id']!='start']
    book['chapters']=[byid['start'],*tutorials,*rest]
    for c in book['chapters']:
        for s in c['sections']:
            s['body']=s['body'].replace('AUTO_LIST is the explicit up-to-eight-row legacy view, not a fallback.','AUTO_LIST is an explicit top-aligned table with up to 24 rows when space permits, not a fallback. Its footer shows displayed and received row counts.')
            s['body']=s['body'].replace('The automatic world display shows at most eight rows; inspect Data for the full received list.','The automatic world display shows up to 24 rows when the physical panel height permits; inspect Data for the full received list.')
            s['body']=s['body'].replace('only the legacy rows are shown','only the automatic table is shown')
            s['body']=s['body'].replace('R9 groups','The sampler groups').replace('R9 does not convert','PL4 does not convert').replace('R9 adds typed','The port supplies typed')
            if s['heading']=='R9 VALIDATION':
                s['heading']='VALIDATION BOUNDARY'
                s['body']='R10 retains the earlier connections, hammer, energy telemetry, display editor, expansion and hologram work. Source checks and render-plan tests do not prove a Minecraft build or in-game rendering. Read the release validation report for executed checks. Test a copied world; R8 and older versions cannot preserve typed layouts.'
            s['body']=s['body'].replace('Right-click opens the panel settings.','Right-click opens the on-screen editor; ? opens Data / Settings.')
            s['body']=s['body'].replace('Automatic mode shows the label/status and up to eight rows.','Automatic mode uses an inset heading and aligned name/amount columns, with up to 24 rows when the panel height permits.')
    controls=next(c for c in book['chapters'] if c['id']=='guide_controls')
    controls['sections'][0]['body']='The four side tabs group chapters into Welcome and tutorials, Networks, Displays and Reference. A section click clears search/bookmark filters and selects a chapter in that section, so the right page cannot silently remain on an unrelated chapter. Select a chapter on the left; each pane scrolls separately. Welcome returns to the landing page; its Tutorial > button starts the inventory walkthrough. Home also returns to Welcome when search is not focused.'
    status=next(c for c in book['chapters'] if c['id']=='status')
    status['sections'].append(dict(heading='R10 PRESENTATION',body='Held, dropped, framed and inventory appearances for all 23 multipart parts now use separate centred item models and explicit transforms. Placed block geometry and collision are unchanged. Automatic lists are inset and top-aligned; the editor previews CUSTOM without changing the saved resting view. These changes still require native client visual acceptance.')) if not any(s['heading']=='R10 PRESENTATION' for s in status['sections']) else None
    path.write_text(json.dumps(book,indent=2,ensure_ascii=False)+'\n')
    for name in ['FIELD_GUIDE.md','FIELD_GUIDE_R10.md']:
        lines=['# '+book['title'], '',book['edition'],'','This is the same chapter text shipped in the game. Tutorial checkpoints are manual, not automated acceptance results.','']
        for c in book['chapters']:
            lines += ['## '+c['title'],'',c['summary'],'']
            for s in c['sections']:lines += ['### '+s['heading'],'',s['body'],'']
        (R/'docs'/name).write_text('\n'.join(lines)+'\n')
    print('Guide:',len(book['chapters']),'chapters;',sum(len(s['body'].split()) for c in book['chapters'] for s in c['sections']),'body words')
if __name__=='__main__':upgrade()
