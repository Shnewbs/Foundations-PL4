package net.foundations.pl4.client;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import net.foundations.pl4.*;
import net.foundations.pl4.core.*;

/** PL2-style editing directly on the world display. Only final gestures are transmitted.
 * Identity is anchored to the clicked tile; joined-canvas roots may be elsewhere. */
public final class DisplayEditorScreen extends Screen {
    final BlockPos pos;final UUID clickedIdentity;final int clickedSlot;final boolean editable;
    Part part;UUID selected;private final LinkedHashSet<UUID> selectedIds=new LinkedHashSet<>();private DisplayElements.Spec clipboard;private DisplayElements.Spec draft,start;private DisplayPicking.Point dragStart;private long dragRevision;
    private double[] inverse;private long captureTime;private boolean snap=true,pending;private int waitTicks;
    private EditorChrome.Corner resizeCorner=EditorChrome.Corner.NONE,hoveredCorner=EditorChrome.Corner.NONE;private int hoveredTool=-1;
    String message="";
    final Map<String,List<Part.Row>> inspected=new HashMap<>();
    private static final int MAX_HISTORY=20;
    private final Deque<List<DisplayElements.Spec>> undoStack=new ArrayDeque<>(),redoStack=new ArrayDeque<>();
    private static final String[] TOOLS={"+","E","X","C","^","v","#","?"};
    private static final String[] HELP={"Add element","Edit selected element","Delete selected element","Duplicate selected element","Bring forward","Send backward","Toggle 4-pixel snap","Data / settings"};
    int spaceW(){return Math.max(8,part.layoutWidth);}int spaceH(){return Math.max(9,part.layoutHeight);}
    public DisplayEditorScreen(BlockPos pos,Part part,boolean editable){super(Component.literal("PL4 Display Editor"));this.pos=pos;this.part=part;this.editable=editable;clickedIdentity=part.identity;clickedSlot=part.slot();}
    public UUID identity(){return clickedIdentity;}
    static DisplayEditorScreen active(){Screen s=Minecraft.getInstance().screen;if(s instanceof DisplayEditorScreen e)return e;if(s instanceof DisplayPropertiesScreen p)return p.parent;if(s instanceof DisplayPickerScreen p)return p.parent.parent;return null;}
    public void receive(PLPackets.Open packet,Part p){
        if(p.layoutRevision>=part.layoutRevision)part=p;
        if(!packet.tag().contains("previewReader")){pending=false;waitTicks=0;message=packet.tag().getString("layoutError");}
        if(packet.tag().contains("previewReader")){
            String id=packet.tag().getString("previewReader");List<Part.Row> rows=new ArrayList<>();var tags=packet.tag().getList("previewRows",net.minecraft.nbt.Tag.TAG_COMPOUND);
            for(int i=0;i<Math.min(64,tags.size());i++)rows.add(Part.Row.load(tags.getCompound(i),minecraft.level.registryAccess()));if(inspected.size()>=8)inspected.clear();inspected.put(id,List.copyOf(rows));
        }
        selectedIds.removeIf(id->part.elements.stream().noneMatch(e->e.id().equals(id)));selected=selectedIds.stream().findFirst().orElse(null);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        var arrange=addRenderableWidget(Button.builder(Component.literal("Arrange [A]"),b->arrangementScreen()).bounds(8,46,104,20).build());
        arrange.active=editable;
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){} // World, not a blurred menu.
    public boolean matches(HostEntity host,Part p){
        if(minecraft==null||minecraft.level==null||!(minecraft.level.getBlockEntity(pos) instanceof HostEntity anchor))return false;
        Part hit=anchor.parts.get(clickedSlot);if(hit==null||!hit.identity.equals(clickedIdentity))return false;
        BlockPos root=hit.kind==Kind.LARGE_DISPLAY?pos.relative(DisplayNetworks.right(hit),-hit.canvasColumn).relative(DisplayNetworks.up(hit),hit.canvasRow):pos;
        return root.equals(host.getBlockPos())&&p.slot()==clickedSlot&&p.kind==hit.kind;
    }
    public List<Part.Element> preview(Part live){
        if(draft==null)return live.elements;
        List<Part.Element> result=new ArrayList<>();for(var e:live.elements)result.add(e.id().equals(draft.id())?new Part.Element(draft):e);return result;
    }
    public void capture(Matrix4f localPose){
        var matrix=new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(localPose);
        float[] values=new float[16];matrix.get(values);double[] m=new double[16];for(int i=0;i<16;i++)m[i]=values[i];
        inverse=DisplayPicking.inverse(m).orElse(null);captureTime=System.nanoTime();
    }
    private Optional<DisplayPicking.Point> point(double x,double y){if(System.nanoTime()-captureTime>300_000_000L)return Optional.empty();return DisplayPicking.hit(inverse,x,y,width,height);}
    void drawOnMonitor(DisplayCanvas canvas,Part live){
        for(int i=0;i<TOOLS.length;i++){
            var r=EditorChrome.toolRect(i);int accent=editable?EditorChrome.toolColor(i,i==6&&snap,i==hoveredTool):0xFF606060;
            canvas.rect(r.x(),r.y(),r.width(),r.height(),0xE6080B0D,5);
            canvas.outline(r,accent,6);
            if(i==hoveredTool)canvas.rect(r.x()+1,r.y()+1,1.2,r.height()-2,accent,8);
            canvas.text(TOOLS[i],r.x()+4,r.y()+2,10,editable?accent:0x808080,false,7);
        }
        int pageY=Math.max(EditorChrome.TOOLBAR_Y,spaceH()-14);var page=new DisplayElements.Rect(EditorChrome.TOOLBAR_X,pageY,EditorChrome.TOOLBAR_W,EditorChrome.TOOLBAR_H);
        canvas.rect(page.x(),page.y(),page.width(),page.height(),0xE6080B0D,5);canvas.outline(page,0xFF67DCE6,6);canvas.text(Integer.toString(live.displayPage+1),page.x()+4,page.y()+2,10,0xFF67DCE6,false,7);
        canvas.text("<",20,pageY+2,8,0xFFFFC45C,false,7);canvas.text(">",Math.max(28,spaceW()-10),pageY+2,8,0xFFFFC45C,false,7);
        var elem=draft!=null?draft:selectedElement();
        for(var id:selectedIds){var marked=live.elements.stream().filter(e->e.id().equals(id)).map(Part.Element::spec).findFirst().orElse(null);if(marked!=null&&marked.page()==live.displayPage)canvas.outline(marked.bounds(),id.equals(selected)?0xFF73D86B:0xFF4D8D94,6);}
        if(elem!=null&&elem.page()==live.displayPage){
            boolean pulse=minecraft!=null&&minecraft.level!=null&&((minecraft.level.getGameTime()/6)&1)==0;
            for(var corner:new EditorChrome.Corner[]{EditorChrome.Corner.NW,EditorChrome.Corner.NE,EditorChrome.Corner.SW,EditorChrome.Corner.SE}){
                var h=EditorChrome.handleRect(elem.bounds(),corner);int c=corner==hoveredCorner?0xFFFFFF66:(pulse?0xFF7DF4FF:0xFF4DB8C8);
                // Full 10x10 hit target, but render it as a PL4 cyan corner bracket instead of a bulky square.
                if(corner==EditorChrome.Corner.NW||corner==EditorChrome.Corner.NE)canvas.rect(h.x(),h.y(),h.width(),2,c,8);
                else canvas.rect(h.x(),h.bottom()-2,h.width(),2,c,8);
                if(corner==EditorChrome.Corner.NW||corner==EditorChrome.Corner.SW)canvas.rect(h.x(),h.y(),2,h.height(),c,8);
                else canvas.rect(h.right()-2,h.y(),2,h.height(),c,8);
                if(corner==hoveredCorner)canvas.rect(h.x()+2,h.y()+2,Math.max(1,h.width()-4),Math.max(1,h.height()-4),0x401DE7FF,7);
            }
        }
    }
    private void drawHudHelp(GuiGraphics g){
        if(!editable)return;
        int x=8,y=8,w=Math.min(width-16,420),h=34;
        g.fill(x,y,x+w,y+h,0xD0141B20);g.fill(x,y,x+3,y+h,0xFF62C7D6);
        String line=hoveredTool>=0?HELP[hoveredTool]:(hoveredCorner!=EditorChrome.Corner.NONE?"Resize from this corner":"LMB select/drag/resize  •  RMB back  •  side tools stay active");
        g.drawString(font,font.plainSubstrByWidth(line,w-12),x+8,y+5,0xFFE3F2F4,false);
        int kx=x+8,ky=y+18;kx=hudKey(g,kx,ky,"E",0xFF63C7FF);kx=hudKey(g,kx+4,ky,"DEL",0xFFFF6B6B);kx=hudKey(g,kx+4,ky,"G",0xFF75E56B);kx=hudKey(g,kx+4,ky,"ESC",0xFFFFC45C);hudKey(g,kx+4,ky,"CTRL+Z",0xFFAA88FF);
    }
    private int hudKey(GuiGraphics g,int x,int y,String key,int color){int w=Math.max(14,font.width(key)+8);g.fill(x,y,x+w,y+12,0xE6263238);g.fill(x,y,x+2,y+12,color);g.drawString(font,key,x+5,y+2,color,false);return x+w;}
    DisplayElements.Spec selectedElement(){return selected==null?null:part.elements.stream().filter(e->e.id().equals(selected)).map(Part.Element::spec).findFirst().orElse(null);}
    void inspect(String reader){if(!reader.isEmpty()&&!inspected.containsKey(reader))PacketDistributor.sendToServer(new PLPackets.Edit(pos,clickedSlot,clickedIdentity,"preview_reader",reader));}
    List<Part.Row> source(String reader){return reader.isBlank()?part.rows:inspected.getOrDefault(reader,part.sourceRows.getOrDefault(reader,List.of()));}
    void commit(String action,DisplayElements.Spec spec,String value,long revision){
        if(!editable||pending)return;
        if(action.equals("add")||action.equals("update")||action.equals("delete")||action.equals("forward")||action.equals("backward")||action.equals("clear"))pushUndo();
        UUID id=spec==null?(selected==null?new UUID(0,0):selected):spec.id();
        if(spec!=null)value=ElementJson.encode(spec);else if(action.equals("delete")&&selectedIds.size()>1)value=String.join(",",selectedIds.stream().map(UUID::toString).toList());pending=true;waitTicks=0;message="Saving...";
        PacketDistributor.sendToServer(new PLPackets.LayoutEdit(pos,clickedSlot,clickedIdentity,revision,action,id,value));
    }
    private List<DisplayElements.Spec> snapshot(){return part.elements.stream().map(Part.Element::spec).toList();}
    int selectionCount(){return (int)part.elements.stream().filter(e->selectedIds.contains(e.id())&&e.spec().page()==part.displayPage).count();}
    private void arrangementScreen(){if(editable&&!pending)minecraft.setScreen(new DisplayArrangementScreen(this));}
    void arrange(String action){
        if(!editable||pending)return;
        var ids=part.elements.stream().filter(e->selectedIds.contains(e.id())&&e.spec().page()==part.displayPage).map(Part.Element::id).toList();
        var before=new LayoutTransactions.State(snapshot(),part.displayMode,part.displayPage,part.layoutRevision);
        var preview=LayoutTransactions.applyArrange(before,part.layoutRevision,action,ids,spaceW(),spaceH());
        if(!preview.accepted()){message=preview.message();return;}
        pushUndo();pending=true;waitTicks=0;message="Saving...";
        PacketDistributor.sendToServer(new PLPackets.LayoutEdit(pos,clickedSlot,clickedIdentity,part.layoutRevision,action,new UUID(0,0),String.join(",",ids.stream().map(UUID::toString).toList())));
    }
    private void pushUndo(){undoStack.addLast(snapshot());if(undoStack.size()>MAX_HISTORY)undoStack.removeFirst();redoStack.clear();}
    private void undo(){
        if(!editable||pending){message="Not ready.";return;}
        if(undoStack.isEmpty()){message="Nothing to undo.";return;}
        redoStack.addLast(snapshot());if(redoStack.size()>MAX_HISTORY)redoStack.removeFirst();
        var previous=undoStack.removeLast();selectedIds.clear();selected=null;commitReplace(previous);
    }
    private void redo(){
        if(!editable||pending){message="Not ready.";return;}
        if(redoStack.isEmpty()){message="Nothing to redo.";return;}
        undoStack.addLast(snapshot());if(undoStack.size()>MAX_HISTORY)undoStack.removeFirst();
        var next=redoStack.removeLast();selectedIds.clear();selected=null;commitReplace(next);
    }
    /** Restores a whole-layout snapshot for undo/redo; bypasses commit()'s own history tracking. */
    private void commitReplace(List<DisplayElements.Spec> elements){
        pending=true;waitTicks=0;message="Saving...";
        PacketDistributor.sendToServer(new PLPackets.LayoutEdit(pos,clickedSlot,clickedIdentity,part.layoutRevision,"replace",new UUID(0,0),ElementJson.encodeList(elements)));
    }
    void properties(boolean add){if(!editable||pending)return;var spec=add?DisplayElements.create(DisplayElements.Type.ITEM,part.displayPage,spaceW(),spaceH()):selectedElement();if(spec!=null)minecraft.setScreen(new DisplayPropertiesScreen(this,spec,add));else message="Select an element first.";}
    private void tool(int id){if(pending)return;switch(id){
        case 0->properties(true);case 1->properties(false);case 2->commit("delete",null,"",part.layoutRevision);
        case 3->duplicateSelected();
        case 4->commit("forward",null,"",part.layoutRevision);case 5->commit("backward",null,"",part.layoutRevision);
        case 6->snap=!snap;case 7->minecraft.setScreen(new PartScreen(pos,anchoredPart(),editable));default->{}
    }}
    private void duplicateSelected(){if(selectedElement()==null){message="Select an element first.";return;}copySelected();pasteSelected();}
    private void copySelected(){var e=selectedElement();if(e!=null){clipboard=e;message="Element copied.";}}
    private void pasteSelected(){
        if(clipboard==null){message="Copy an element first.";return;}
        var copy=DisplayElements.move(clipboard,4,4,false,snap,spaceW(),spaceH()).identity(UUID.randomUUID());
        selectedIds.clear();selectedIds.add(copy.id());selected=copy.id();commit("add",copy,"",part.layoutRevision);
    }
    Part anchoredPart(){var p=Part.load(part.save(minecraft.level.registryAccess(),true),minecraft.level.registryAccess());p.identity=clickedIdentity;return p;}
    @Override public void tick(){
        if(minecraft.level==null||minecraft.player==null||minecraft.player.distanceToSqr(pos.getCenter())>64||!(minecraft.level.getBlockEntity(pos) instanceof HostEntity host)){onClose();return;}
        var anchor=host.parts.get(clickedSlot);if(anchor==null||!anchor.identity.equals(clickedIdentity)){onClose();return;}
        BlockPos root=anchor.kind==Kind.LARGE_DISPLAY?pos.relative(DisplayNetworks.right(anchor),-anchor.canvasColumn).relative(DisplayNetworks.up(anchor),anchor.canvasRow):pos;
        if(minecraft.level.getBlockEntity(root) instanceof HostEntity h){Part live=h.parts.get(clickedSlot);if(live!=null&&live.layoutRevision>=part.layoutRevision)part=live;}
        if(pending&&++waitTicks==60){pending=false;message="No acknowledgement; refresh requested. Review before retrying.";PacketDistributor.sendToServer(new PLPackets.Edit(pos,clickedSlot,clickedIdentity,"refresh",""));}
    }
    private static int toolAt(DisplayPicking.Point p){return EditorChrome.toolAt(p.x(),p.y(),TOOLS.length);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        super.render(g,mx,my,partial);hoveredTool=-1;hoveredCorner=EditorChrome.Corner.NONE;var hit=point(mx,my);
        if(hit.isPresent()){var p=hit.get();hoveredTool=toolAt(p);if(hoveredTool<0){var e=selectedElement();if(e!=null&&e.page()==part.displayPage)hoveredCorner=EditorChrome.cornerAt(e.bounds(),p.x(),p.y());}}
        drawHudHelp(g);
        if(!message.isBlank()){int w=Math.min(width-20,font.width(message)+12);g.fill(6,height-20,6+w,height-4,0xCC080808);g.drawString(font,font.plainSubstrByWidth(message,w-8),10,height-16,0xFFECECEC,false);}
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==1){onClose();return true;}
        if(super.mouseClicked(x,y,button))return true;
        if(button!=0||pending)return super.mouseClicked(x,y,button);var hit=point(x,y);if(hit.isEmpty()){message="Aim at the visible screen; move closer if necessary.";return false;}
        var p=hit.get();int toolbarTool=toolAt(p);if(toolbarTool>=0){tool(toolbarTool);return true;}
        if(p.y()>=spaceH()-16&&p.y()<spaceH()&&((p.x()>=18&&p.x()<30)||p.x()>=spaceW()-14)){commit("page",null,Integer.toString(Math.floorMod(part.displayPage+(p.x()<30?-1:1),8)),part.layoutRevision);return true;}
        UUID hitId=null;for(int i=part.elements.size()-1;i>=0;i--){var e=part.elements.get(i).spec();if(e.page()==part.displayPage&&e.bounds().contains(p.x(),p.y())){hitId=e.id();break;}}
        if(hitId!=null){if(hasShiftDown())selectedIds.add(hitId);else{selectedIds.clear();selectedIds.add(hitId);}selected=hitId;}else if(!hasShiftDown()){selectedIds.clear();selected=null;}
        var e=selectedElement();if(e!=null&&editable){start=e;draft=e;dragStart=p;dragRevision=part.layoutRevision;resizeCorner=EditorChrome.cornerAt(e.bounds(),p.x(),p.y());}
        return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button==0&&start!=null&&dragStart!=null){point(x,y).ifPresent(p->{double sx=p.x()-dragStart.x(),sy=p.y()-dragStart.y();draft=resizeCorner==EditorChrome.Corner.NONE?DisplayElements.move(start,sx,sy,false,snap,spaceW(),spaceH()):EditorChrome.resize(start,resizeCorner,sx,sy,snap,spaceW(),spaceH());});return true;}return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==0&&start!=null){var result=draft;boolean changed=result!=null&&!result.equals(start);start=null;draft=null;dragStart=null;resizeCorner=EditorChrome.Corner.NONE;if(changed)commit("update",result,"",dragRevision);return true;}return super.mouseReleased(x,y,button);
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==256){onClose();return true;}if(key==69){properties(false);return true;}if(key==261){commit("delete",null,"",part.layoutRevision);return true;}
        if(key==67&&hasControlDown()){copySelected();return true;}if(key==86&&hasControlDown()){pasteSelected();return true;}
        if(key==68&&hasControlDown()){duplicateSelected();return true;}if(key==71){snap=!snap;return true;}
        if(key==65&&!hasControlDown()){arrangementScreen();return true;}
        if(key==90&&hasControlDown()){if(hasShiftDown())redo();else undo();return true;}if(key==89&&hasControlDown()){redo();return true;}
        return super.keyPressed(key,scan,mods);
    }
    @Override public void removed(){inverse=null;draft=null;start=null;dragStart=null;resizeCorner=EditorChrome.Corner.NONE;hoveredCorner=EditorChrome.Corner.NONE;hoveredTool=-1;super.removed();}
}
