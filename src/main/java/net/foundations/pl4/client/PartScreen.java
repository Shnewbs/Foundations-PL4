package net.foundations.pl4.client;

import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.foundations.pl4.compat.PacketDistributor;
import net.foundations.pl4.*;

public final class PartScreen extends net.foundations.pl4.compat.PortScreen {
    private final BlockPos pos;private Part part;private final boolean editable;
    private final java.util.UUID clickedIdentity;private final int clickedSlot;
    private int left,top,w,h,scroll,tab,contentScroll;
    private Component hoveredRowTooltip;
    private boolean draggingSettings;private double settingsDragOffset;
    private Button energyInputButton,energyOutputButton,energyModeButton;
    private Button channelButton,channelPageButton;
    private Button viewToggle;private String displayError="";
    private final Map<String,EditBox> fields=new LinkedHashMap<>();
    private final Map<AbstractWidget,Integer> contentWidgets=new LinkedHashMap<>();
    public PartScreen(BlockPos pos,Part part,boolean editable){super(Component.translatable("block."+FoundationsPL4.ID+"."+part.kind.id));this.pos=pos;this.part=part;this.editable=editable;this.clickedIdentity=part.identity;this.clickedSlot=part.slot();}
    public UUID identity(){return clickedIdentity;}
    public void update(Part p){boolean linksChanged=!p.links.equals(part.links)||!p.statements.equals(part.statements)||(ComponentLinks.supported(p)&&(!p.targetChoices.equals(part.targetChoices)||p.targetPage!=part.targetPage));if(!p.targetChannel.equals(part.targetChannel)&&fields.containsKey("channel_name"))fields.get("channel_name").setValue(p.channelNames.getOrDefault(p.targetChannel,""));if(!p.kind.display()||p.layoutRevision>=part.layoutRevision)part=p;if(viewToggle!=null)viewToggle.setMessage(Component.literal("View: "+part.displayMode));
        if(channelPageButton!=null)channelPageButton.setMessage(Component.literal("Next: "+(part.targetPage+1)+" / "+Math.max(1,(part.targetCount+63)/64)));
        if(channelButton!=null){channelButton.setMessage(Component.literal(font.plainSubstrByWidth("Target: "+ReaderChannels.label(part),w-52)));channelButton.setTooltip(Tooltip.create(Component.literal(ReaderChannels.label(part)+" · click for next target; 64 targets per page")));}
        if(energyInputButton!=null){energyInputButton.setMessage(Component.literal("Input: "+EnergyPorts.label(part.energyInput)));energyInputButton.active=editable&&part.energyRouteEditable();}
        if(energyOutputButton!=null){energyOutputButton.setMessage(Component.literal("Output: "+EnergyPorts.label(part.energyOutput)));energyOutputButton.active=editable&&part.energyRouteEditable()&&part.energyConvert;}
        if(energyModeButton!=null){energyModeButton.setMessage(Component.literal("Conversion: "+(part.energyConvert?"On":"Off")));energyModeButton.active=editable&&part.energyRouteEditable();}
        if(linksChanged)rebuildWidgets();
        if(fields.containsKey("energy_voltage"))fields.get("energy_voltage").setEditable(editable&&part.energyRouteEditable());
    }
    public void error(String message){displayError=message;}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        w=Math.min(540,width-12);h=Math.min(360,height-12);left=(width-w)/2;top=(height-h)/2;fields.clear();contentWidgets.clear();viewToggle=null;channelButton=null;channelPageButton=null;energyInputButton=energyOutputButton=energyModeButton=null;
        button("Data",left+10,top+25,60,b->{tab=0;scroll=0;rebuildWidgets();});
        button("Settings",left+74,top+25,80,b->{tab=1;rebuildWidgets();});
        if(part.kind.display())button("Edit screen",left+158,top+25,82,b->{Part anchor=Part.load(part.save(minecraft.level.registryAccess(),true),minecraft.level.registryAccess());anchor.identity=clickedIdentity;minecraft.setScreen(new DisplayEditorScreen(pos,anchor,editable));});
        button("Done",left+w-66,top+h-27,56,b->onClose());
        if(tab==1){
            int x=left+112,y=top+58,fw=Math.max(90,w-132);
            if(part.hologram()){
                int current=net.foundations.pl4.core.HologramProjection.view(part.face.ordinal(),part.hologramView);
                String viewLabel="View: "+net.minecraft.core.Direction.from3DDataValue(current).getName();
                Button viewButton=button(viewLabel,left+12,y,Math.min(w-24,font.width("View: north")+24),b->{
                    int value=net.foundations.pl4.core.HologramProjection.nextView(part.face.ordinal(),part.hologramView);
                    send("hologram_view",Integer.toString(value));b.setMessage(Component.literal("View: "+net.minecraft.core.Direction.from3DDataValue(value).getName()));
                });
                viewButton.active=editable&&part.face.getAxis()==net.minecraft.core.Direction.Axis.Y;
                viewButton.setTooltip(Tooltip.create(Component.literal(viewButton.active?"Rotate the projection around its base":"Wall projection follows the mounting face")));y+=28;
            }else if(part.kind.display()){
                String label="Front: "+(part.displayOutward?"outward":"inward");
                button(label,left+12,y,Math.min(w-24,font.width("Front: outward")+24),b->{boolean value=!part.displayOutward;send("display_outward",Boolean.toString(value));b.setMessage(Component.literal("Front: "+(value?"outward":"inward")));});y+=28;
            }
            if(part.kind==Kind.ENERGY_READER){
                String label="Energy: "+part.energySystem;
                button(label,left+12,y,Math.min(w-24,font.width("Energy: CREATE")+24),b->{String value=next(new String[]{"AUTO","FE","EU","J","CREATE","AE2"},part.energySystem);send("energy_system",value);b.setMessage(Component.literal("Energy: "+value));});y+=28;
            }
            if(part.kind.reader()){
                channelButton=button(font.plainSubstrByWidth("Target: "+ReaderChannels.label(part),w-52),left+12,y,w-24,b->{
                    List<String> ids=new ArrayList<>();ids.add("");part.targetChoices.forEach(c->ids.add(c.id()));
                    int next=(ids.indexOf(part.targetChannel)+1)%ids.size();send("target_channel",ids.get(next));
                });channelButton.setTooltip(Tooltip.create(Component.literal("Cycle stable block/entity targets on this page. All targets clears selection. Slot/tank and list mode stay separate.")));y+=28;
                button("All targets",left+12,y,104,b->send("target_channel",""));y+=28;
                button("Previous",left+12,y,80,b->send("target_page",Integer.toString(Math.max(0,part.targetPage-1))));
                channelPageButton=button("Next: "+(part.targetPage+1)+" / "+Math.max(1,(part.targetCount+63)/64),left+98,y,132,b->send("target_page",Integer.toString(part.targetPage+1)));y+=28;
                field("target_query","Target search",part.targetQuery,x,y,fw);fields.get("target_query").setMaxLength(48);y+=28;
                button("Search targets",left+12,y,132,b->send("target_query",fields.get("target_query").getValue()));y+=28;
                field("channel_name","Channel name",part.channelNames.getOrDefault(part.targetChannel,""),x,y,fw);y+=28;
                fields.get("channel_name").setMaxLength(48);fields.get("channel_name").setTooltip(Tooltip.create(Component.literal("Name the selected endpoint for this reader. Apply fields saves it. Blank removes a name; up to 64 names.")));
            }
            field("label","Name",part.label,x,y,fw);y+=28;
            field("filter","Filter IDs / tags",part.filter,x,y,fw);y+=28;
            fields.get("filter").setTooltip(Tooltip.create(Component.literal("Comma-separated IDs or #tags, e.g. minecraft:stone,#c:ingots. Applies to item/fluid lists; STORAGE shows the full target.")));
            if(part.kind==Kind.INVENTORY_READER||part.kind==Kind.TRANSFER_NODE){
                button("Held item filter",left+12,y,132,b->{if(minecraft.player!=null&&!minecraft.player.getMainHandItem().isEmpty())fields.get("filter").setValue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(minecraft.player.getMainHandItem().getItem()).toString());});
                button("Clear filter",left+150,y,104,b->fields.get("filter").setValue(""));y+=28;
            }
            if(part.kind==Kind.NODE||part.kind==Kind.TRANSFER_NODE){
                field("input_channel","Input channel",part.inputChannel,x,y,fw);y+=28;
                field("output_channel","Output channel",part.outputChannel,x,y,fw);y+=28;
                for(String key:new String[]{"input_channel","output_channel"}){fields.get(key).setMaxLength(48);fields.get(key).setEditable(editable&&part.routeEditable());fields.get(key).setTooltip(Tooltip.create(Component.literal("Exact, case-sensitive route name. Source output must equal destination input. Blank matches blank. Drain escrow before editing.")));}
            }
            if(part.kind==Kind.NODE||part.kind==Kind.TRANSFER_NODE){
                for(boolean input:new boolean[]{true,false}){
                    String key=input?"input_filter":"output_filter";
                    field(key,input?"Receive filter":"Send filter",input?part.inputFilter:part.outputFilter,x,y,fw);fields.get(key).setEditable(editable&&part.routeEditable());y+=28;
                    String mode=input?part.inputFilterMode:part.outputFilterMode;
                    Button filterMode=button((input?"Receive: ":"Send: ")+mode,left+12,y,160,b->{String next=next(new String[]{"INHERIT","ALLOW","DENY"},input?part.inputFilterMode:part.outputFilterMode);send(key+"_mode",next);b.setMessage(Component.literal((input?"Receive: ":"Send: ")+next));});
                    filterMode.active=editable&&part.routeEditable();filterMode.setTooltip(Tooltip.create(Component.literal("INHERIT uses the existing Transfer Node filter; normal Nodes accept all. ALLOW/DENY uses this direction's IDs or #tags. Blank accepts all. Drain escrow before editing.")));y+=28;
                }
            }
            if(part.kind==Kind.CLOCK){
                field("clock_pulse","Pulse ticks (0 = auto)",Integer.toString(part.clockPulse),x,y,fw);y+=28;
                field("clock_phase","Phase ticks",Integer.toString(part.clockPhase),x,y,fw);y+=28;
                button(part.clockPaused?"Resume clock":"Pause clock",left+12,y,132,b->{send("clock_paused",Boolean.toString(!part.clockPaused));b.setMessage(Component.literal(part.clockPaused?"Pause clock":"Resume clock"));});
                button("Reset phase",left+150,y,132,b->send("clock_reset",""));y+=28;
                fields.get("clock_pulse").setTooltip(Tooltip.create(Component.literal("0 = one server sample. 1-24000 ticks, capped by interval. Timing resolution is the server sampling interval.")));
            }
            field("selected","Reader name",part.selected,x,y,fw);y+=28;
            field("metric","Data key",part.metric,x,y,fw);y+=28;
            field("index","Slot / tank / rank",Integer.toString(part.index),x,y,70);
            field("priority","Priority",Integer.toString(part.priority),x+fw-70,y,70);y+=28;
            field("threshold",part.kind==Kind.CLOCK?"Interval (ticks)":"Threshold",Double.toString(part.threshold),x,y,90);
            button(part.comparison,x+98,y,45,b->{String[] ops={">=",">","<","<=","=","!="};send("comparison",next(ops,part.comparison));b.setMessage(Component.literal(next(ops,part.comparison)));});y+=28;
            if(part.kind==Kind.SIGNALLER){
                field("signal_strength","Output strength",Integer.toString(part.signalStrength),x,y,90);y+=28;
                button(part.statementsAll?"Match ALL":"Match ANY",left+12,y,132,b->{send("statements_all",Boolean.toString(!part.statementsAll));b.setMessage(Component.literal(part.statementsAll?"Match ANY":"Match ALL"));});y+=28;
                button("Add statement from fields",left+12,y,188,b->{
                    try{var json=new com.google.gson.JsonObject();json.addProperty("reader",fields.get("selected").getValue());json.addProperty("key",fields.get("metric").getValue());json.addProperty("operator",part.comparison);json.addProperty("threshold",Double.parseDouble(fields.get("threshold").getValue()));send("statement_add",json.toString());}catch(IllegalArgumentException invalid){displayError="Enter a finite threshold.";}
                }).active=editable&&part.statements.size()<net.foundations.pl4.core.SignalRules.MAX_STATEMENTS;y+=28;
                for(var statement:part.statements){String label=(statement.reader().isEmpty()?"First reader":statement.reader())+" / "+(statement.key().isEmpty()?"first value":statement.key())+" "+statement.operator()+" "+statement.threshold();
                    Button remove=button(font.plainSubstrByWidth("Remove: "+label,w-48),left+12,y,w-24,b->send("statement_remove",statement.id().toString()));remove.setTooltip(Tooltip.create(Component.literal(label+" · remove this statement. With no statements, the original single condition is used.")));y+=24;
                }
            }
            if(!part.kind.display())button("Data: "+part.mode,left+12,y,132,b->{String v=next(new String[]{"LIST","STACK","SLOT","POS","STORAGE","CHANNEL"},part.mode);send("mode",v);b.setMessage(Component.literal("Data: "+v));});
            else viewToggle=button("View: "+part.displayMode,left+12,y,132,b->{var mode=part.displayMode==net.foundations.pl4.core.DisplayElements.Mode.AUTO_LIST?"CUSTOM":"AUTO_LIST";PacketDistributor.sendToServer(new PLPackets.LayoutEdit(pos,clickedSlot,clickedIdentity,part.layoutRevision,"mode",new UUID(0,0),mode));b.setMessage(Component.literal("View: "+mode));});
            button("Sort: "+(part.descending?"High first":"Low first"),left+150,y,132,b->{send("descending",Boolean.toString(!part.descending));b.setMessage(Component.literal("Sort: "+(!part.descending?"High first":"Low first")));});
            y+=25;button(part.whitelist?"Allow filter":"Exclude filter",left+12,y,132,b->{send("whitelist",Boolean.toString(!part.whitelist));b.setMessage(Component.literal(!part.whitelist?"Allow filter":"Exclude filter"));});
            y+=25;
            if(part.kind==Kind.TRANSFER_NODE){
                String[] names={"PASSIVE","ADD / IMPORT","REMOVE / EXPORT","ADD / REMOVE (PEER)"};
                Button modeButton=button(names[part.transferMode],left+12,y,156,b->{int next=(part.transferMode+1)%4;send("transfer",Integer.toString(next));b.setMessage(Component.literal(names[next]));b.setTooltip(transferTooltip(next));});
                modeButton.setTooltip(transferTooltip(part.transferMode));
                y+=25;toggle("items","Items",part.items,left+12,y);toggle("fluids","Fluids",part.fluids,left+90,y);toggle("energy","Energy",part.energy,left+168,y);
                y+=25;energyModeButton=button("Conversion: "+(part.energyConvert?"On":"Off"),left+12,y,156,b->{send("energy_convert",Boolean.toString(!part.energyConvert));});
                energyModeButton.active=editable&&part.energyRouteEditable();energyModeButton.setTooltip(Tooltip.create(Component.literal("Off: native same-unit transfer. On: choose input and output; server policy applies. Drain escrow before editing.")));
                y+=25;
                energyInputButton=button("Input: "+EnergyPorts.label(part.energyInput),left+12,y,104,b->{String value=EnergyPorts.next(part.energyInput);send("energy_input",value);b.setMessage(Component.literal("Input: "+EnergyPorts.label(value)));});
                energyOutputButton=button("Output: "+EnergyPorts.label(part.energyOutput),left+124,y,104,b->{String value=EnergyPorts.next(part.energyOutput);send("energy_output",value);b.setMessage(Component.literal("Output: "+EnergyPorts.label(value)));});
                energyInputButton.active=editable&&part.energyRouteEditable();energyOutputButton.active=editable&&part.energyRouteEditable()&&part.energyConvert;
                energyInputButton.setTooltip(Tooltip.create(Component.literal("REMOVE: attached machine input; ADD: network input. Equal types mean native transfer.")));
                energyOutputButton.setTooltip(Tooltip.create(Component.literal("REMOVE: network output; ADD: attached machine output. Different types request server-controlled conversion.")));
                y+=28;field("energy_voltage","EU voltage",Integer.toString(part.energyVoltage),x,y,90);
                fields.get("energy_voltage").setEditable(editable&&part.energyRouteEditable());
                fields.get("energy_voltage").setTooltip(Tooltip.create(Component.literal("EU insertion voltage; maximum accepted voltage from a pushing EU source. Set to match the source tier.")));
            }else if(part.kind==Kind.ARRAY||part.kind==Kind.ENTITY_NODE||part.kind.receiver()){
                button("Add held Transceiver link",left+12,y,188,b->send("link_held",""));y+=28;
                if(part.kind.receiver()||part.kind==Kind.ENTITY_NODE){
                    field("link_query","Find emitter / entity",part.targetQuery,x,y,fw);y+=28;
                    button("Search",left+12,y,80,b->send("link_query",fields.get("link_query").getValue()));
                    button("Previous",left+98,y,80,b->send("link_page",Integer.toString(Math.max(0,part.targetPage-1))));
                    button("Next",left+184,y,60,b->send("link_page",Integer.toString(part.targetPage+1)));y+=28;
                    for(var choice:part.targetChoices){button(font.plainSubstrByWidth("Add: "+choice.name(),w-48),left+12,y,w-24,b->send("link_add",choice.id()));y+=24;}
                }
                button("Clear links ("+part.links.size()+")",left+12,y,140,b->send("clear_links",""));y+=28;
                for(var link:part.links){String id=ReaderChannels.id(link);String detail=link.dimension()+" "+link.pos().getX()+", "+link.pos().getY()+", "+link.pos().getZ()+" "+link.side().getName()+(link.entity()==null?"":" · entity "+link.entity());
                    var remove=button(font.plainSubstrByWidth("Remove: "+detail,w-48),left+12,y,w-24,b->send("remove_link",id));remove.setTooltip(Tooltip.create(Component.literal(detail+" · remove only this saved link")));y+=24;
                    button("Make first target",left+12,y,132,b->send("link_first",id));y+=24;
                }
            }
            button("Apply fields",left+12,top+h-27,104,b->{Map<String,String> values=new LinkedHashMap<>();fields.forEach((key,box)->values.put(key,box.getValue()));values.forEach(this::send);});
        }
        layoutContent();
    }
    private int contentHeight(){return contentWidgets.values().stream().mapToInt(Integer::intValue).max().orElse(top+52)+20-(top+52);}
    private int settingsViewport(){return Math.max(1,h-90);}
    private void layoutContent(){int bottom=contentWidgets.values().stream().mapToInt(Integer::intValue).max().orElse(top+52)+20;contentScroll=net.foundations.pl4.compat.PortMath.clamp(contentScroll,0,Math.max(0,bottom-(top+h-38)));contentWidgets.forEach((widget,y)->{widget.setY(y-contentScroll);widget.visible=widget.getY()>=top+52&&widget.getY()+20<=top+h-35;});}
    private static String next(String[] values,String current){for(int i=0;i<values.length;i++)if(values[i].equals(current))return values[(i+1)%values.length];return values[0];}
    private void toggle(String key,String label,boolean initial,int x,int y){button(label+": "+(initial?"On":"Off"),x,y,72,new Button.OnPress(){boolean value=initial;public void onPress(Button b){value=!value;send(key,Boolean.toString(value));b.setMessage(Component.literal(label+": "+(value?"On":"Off")));}});}
    private static Tooltip transferTooltip(int mode){return Tooltip.create(Component.literal(switch(mode){
        case 1->"ADD imports from normal Nodes or explicit REMOVE peers into this attached target";
        case 2->"REMOVE exports this attached target to explicit ADD peers first, then normal Nodes";
        case 3->"ADD / REMOVE is explicit-peer only until directional PL2 channel filters are restored";
        default->"PASSIVE observes the network but does not move resources";
    }));}
    private void field(String key,String label,String value,int x,int y,int width){EditBox box=new EditBox(font,x,y,width,20,Component.literal(label));box.setMaxLength(256);box.setValue(value);box.setEditable(editable);box.setTooltip(Tooltip.create(Component.literal(label)));fields.put(key,box);addRenderableWidget(box);contentWidgets.put(box,y);}
    private Button button(String title,int x,int y,int width,Button.OnPress action){Button b=Button.builder(Component.literal(title),action).bounds(x,y,width,20).build();if(tab==1&&!Set.of("Data","Settings","Layout","Edit screen","Done").contains(title))b.active=editable;addRenderableWidget(b);if(y>=top+52&&y!=top+h-27)contentWidgets.put(b,y);return b;}
    private void send(String field,String value){if(editable)PacketDistributor.sendToServer(new PLPackets.Edit(pos,clickedSlot,clickedIdentity,field,value));}
    @Override public void tick(){
        if(minecraft.level==null||!(minecraft.level.getBlockEntity(pos) instanceof HostEntity host))return;
        Part live=host.parts.get(clickedSlot);if(live==null||!live.identity.equals(clickedIdentity))return;
        if(live.kind==Kind.LARGE_DISPLAY){
            BlockPos root=pos.relative(DisplayNetworks.right(live),-live.canvasColumn).relative(DisplayNetworks.up(live),live.canvasRow);
            if(minecraft.level.hasChunkAt(root)&&minecraft.level.getBlockEntity(root) instanceof HostEntity controller){
                Part shared=controller.parts.get(clickedSlot);
                if(shared!=null&&shared.kind==Kind.LARGE_DISPLAY&&shared.displayOutward==live.displayOutward&&java.util.Objects.equals(shared.owner,live.owner))update(shared);
            }
        }else update(live);
    }
    /**
     * Screen.render calls this before rendering its widgets in Minecraft 1.21.1.
     * Finish the native world blur/dim pass before drawing any PL4 pixels.
     */
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        super.renderBackground(g,mx,my,partial);
        hoveredRowTooltip=null;
        g.fill(left,top,left+w,top+h,0xF21B2533);g.fill(left,top,left+w,top+22,0xFF293D56);
        g.drawString(font,title,left+10,top+7,0xFFE4F3FF,false);
        if(!displayError.isBlank())g.drawString(font,font.plainSubstrByWidth(displayError,w-180),left+126,top+h-21,0xFFFFA5A5,false);
        if(tab==0){
            g.drawString(font,font.plainSubstrByWidth(part.status,w-20),left+10,top+52,0xFF8CAEC5,false);
            int count=Math.max(1,(h-112)/19);scroll=net.foundations.pl4.compat.PortMath.clamp(scroll,0,Math.max(0,part.rows.size()-count));
            for(int i=0;i<count&&i+scroll<part.rows.size();i++){
                Part.Row row=part.rows.get(i+scroll);int y=top+73+i*19;
                if(i%2==0)g.fill(left+8,y-2,left+w-12,y+16,0x442C465E);
                g.drawString(font,font.plainSubstrByWidth(row.text(),w-34),left+13,y+2,0xFFDBEFFF,false);
                if(mx>=left+8&&mx<left+w-12&&my>=y&&my<y+18)hoveredRowTooltip=Component.literal(row.key()+" · click to copy");
            }
            if(part.rows.size()>count){int track=h-112;int thumb=Math.max(12,track*count/part.rows.size());int sy=top+73+(track-thumb)*scroll/Math.max(1,part.rows.size()-count);g.fill(left+w-8,top+73,left+w-5,top+73+track,0xFF30445C);g.fill(left+w-8,sy,left+w-5,sy+thumb,0xFF79D3FF);}
            g.drawString(font,part.rows.size()+" data rows"+(editable?"":" · read only"),left+12,top+h-23,0xFF8CAEC5,false);
        }else{
            for(var e:fields.entrySet()){EditBox box=e.getValue();if(!box.visible)continue;String label=switch(e.getKey()){case "signal_strength"->"Output (0-15)";case "input_filter"->"Receive filter";case "output_filter"->"Send filter";case "clock_pulse"->"Pulse ticks";case "clock_phase"->"Phase ticks";case "input_channel"->"Input channel";case "output_channel"->"Output channel";case "target_query"->"Target search";case "channel_name"->"Channel name";case "energy_voltage"->"EU voltage";case "label"->"Name";case "filter"->"Filter IDs / tags";case "selected"->"Reader name";case "metric","key"->"Data key";case "index"->"Slot / tank / rank";case "threshold"->part.kind==Kind.CLOCK?"Interval":"Threshold";case "color"->"Colour (hex)";default->e.getKey();};if(!e.getKey().equals("priority"))g.drawString(font,label,left+12,box.getY()+6,0xFFAFC4D9,false);}
            int viewport=settingsViewport(),content=contentHeight();
            if(content>viewport){int thumb=net.foundations.pl4.core.GuideLayout.thumbSize(viewport,content,viewport);int sy=top+52+net.foundations.pl4.core.GuideLayout.thumbPosition(contentScroll,viewport,content,viewport);g.fill(left+w-8,top+52,left+w-3,top+52+viewport,0xFF30445C);g.fill(left+w-8,sy,left+w-3,sy+thumb,0xFF79D3FF);}
        }
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        // Native Screen.render invokes our background/content hook once, then widgets.
        super.render(g,mx,my,partial);
        // Row tooltips belong above the panel, labels and widgets, never in the blur pass.
        if(hoveredRowTooltip!=null)g.renderTooltip(font,hoveredRowTooltip,mx,my);
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){if(tab==0){scroll=Math.max(0,scroll-(int)Math.signum(dy)*3);return true;}contentScroll-=((int)Math.signum(dy))*25;layoutContent();return true;}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&tab==1&&contentHeight()>settingsViewport()&&x>=left+w-10&&x<left+w&&y>=top+52&&y<top+52+settingsViewport()){
        int viewport=settingsViewport(),content=contentHeight();int thumb=net.foundations.pl4.core.GuideLayout.thumbSize(viewport,content,viewport);int sy=top+52+net.foundations.pl4.core.GuideLayout.thumbPosition(contentScroll,viewport,content,viewport);
        settingsDragOffset=y>=sy&&y<sy+thumb?y-sy:thumb/2.0;draggingSettings=true;dragSettings(y);return true;
    }if(button==1){if(tab!=0){tab=0;scroll=0;contentScroll=0;rebuildWidgets();}else onClose();return true;}if(tab==0&&button==0&&x>=left+8&&x<left+w-12&&y>=top+73&&y<top+h-39){int index=scroll+(int)(y-(top+73))/19;if(index<part.rows.size()){minecraft.keyboardHandler.setClipboard(part.rows.get(index).key());return true;}}return super.mouseClicked(x,y,button);}
    private void dragSettings(double y){contentScroll=net.foundations.pl4.core.GuideLayout.scrollFromThumb(y-(top+52)-settingsDragOffset,settingsViewport(),contentHeight(),settingsViewport());layoutContent();}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(button==0&&draggingSettings){dragSettings(y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&draggingSettings){draggingSettings=false;return true;}return super.mouseReleased(x,y,button);}
}
