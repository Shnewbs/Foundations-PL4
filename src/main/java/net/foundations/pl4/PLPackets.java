package net.foundations.pl4;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class PLPackets {
    public static Consumer<Open> clientOpen=packet->{};
    public record Open(BlockPos pos,int slot,CompoundTag tag) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(FoundationsPL4.id("open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeVarInt(p.slot);b.writeNbt(p.tag);},b->new Open(b.readBlockPos(),b.readVarInt(),Objects.requireNonNull(b.readNbt())));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Edit(BlockPos pos,int slot,UUID identity,String field,String value) implements CustomPacketPayload {
        public static final Type<Edit> TYPE=new Type<>(FoundationsPL4.id("edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Edit> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeVarInt(p.slot);b.writeUUID(p.identity);b.writeUtf(p.field,32);b.writeUtf(p.value,1024);},b->new Edit(b.readBlockPos(),b.readVarInt(),b.readUUID(),b.readUtf(32),b.readUtf(1024)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record LayoutEdit(BlockPos pos,int slot,UUID identity,long revision,String action,UUID element,String value) implements CustomPacketPayload {
        public static final Type<LayoutEdit> TYPE=new Type<>(FoundationsPL4.id("layout_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf,LayoutEdit> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeVarInt(p.slot);b.writeUUID(p.identity);b.writeLong(p.revision);b.writeUtf(p.action,16);b.writeUUID(p.element);b.writeUtf(p.value,65536);},b->new LayoutEdit(b.readBlockPos(),b.readVarInt(),b.readUUID(),b.readLong(),b.readUtf(16),b.readUUID(),b.readUtf(65536)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private record Rate(long tick,int count){}
    private static final Map<ServerPlayer,Rate> EDIT_RATE=new WeakHashMap<>(); // Keys expire on disconnect; main server thread only.
    static void editLayout(ServerPlayer player,LayoutEdit packet){
        long tick=player.level().getGameTime();Rate rate=EDIT_RATE.get(player);if(rate!=null&&rate.tick==tick&&rate.count>=8)return;EDIT_RATE.put(player,new Rate(tick,rate!=null&&rate.tick==tick?rate.count+1:1));
        if(player.isSpectator()||packet.slot<0||packet.slot>=net.foundations.pl4.core.MultipartTopology.SLOT_COUNT||player.distanceToSqr(packet.net.minecraft.world.phys.Vec3.atCenterOf(pos))>64||!player.level().hasChunkAt(packet.pos)||!player.level().mayInteract(player,packet.pos))return;
        if(!(player.level().getBlockEntity(packet.pos) instanceof HostEntity anchor))return;
        Part clicked=anchor.parts.get(packet.slot);if(clicked==null||!clicked.identity.equals(packet.identity)||!clicked.kind.display()||!anchor.canEdit(player))return;
        var target=DisplayNetworks.controller(anchor,clicked);HostEntity host=target.host();Part part=target.part();
        if(!host.canEdit(player)||!host.getLevel().mayInteract(player,host.getBlockPos())||!DisplayNetworks.canEditCanvas(player,anchor,clicked))return;
        try{
            var spec=packet.action.equals("add")||packet.action.equals("update")?ElementJson.decode(packet.value):null;
            if(spec!=null){
                boolean visible=spec.reader().isEmpty()||(part.readerChoices.stream().anyMatch(choice->choice.id().equals(spec.reader()))||part.readerChoices.stream().filter(choice->choice.name().equals(spec.reader())).count()==1);
                if(!spec.id().equals(packet.element)||!visible){openWithError(player,anchor,clicked,"Choose a reader visible to this display.");return;}
            }
            var before=new net.foundations.pl4.core.LayoutTransactions.State(part.elements.stream().map(Part.Element::spec).toList(),part.displayMode,part.displayPage,part.layoutRevision);
            net.foundations.pl4.core.LayoutTransactions.Result result;
            if(packet.action.equals("replace")||packet.action.equals("paste")){
                var restored=ElementJson.decodeList(packet.value);
                for(var s:restored){
                    boolean visible=s.reader().isEmpty()||(part.readerChoices.stream().anyMatch(choice->choice.id().equals(s.reader()))||part.readerChoices.stream().filter(choice->choice.name().equals(s.reader())).count()==1);
                    if(!visible){openWithError(player,anchor,clicked,"Choose a reader visible to this display.");return;}
                }
                result=packet.action.equals("paste")?net.foundations.pl4.core.LayoutTransactions.applyPaste(before,packet.revision,restored,part.layoutWidth,part.layoutHeight):net.foundations.pl4.core.LayoutTransactions.applyReplace(before,packet.revision,restored);
            }else if(packet.action.equals("move_selection")){
                var fields=packet.value.split(";",-1);if(fields.length!=3)throw new IllegalArgumentException("Invalid movement.");
                var ids=Arrays.stream(fields[2].split(",",-1)).map(UUID::fromString).toList();
                result=net.foundations.pl4.core.LayoutTransactions.applyMove(before,packet.revision,ids,Integer.parseInt(fields[0]),Integer.parseInt(fields[1]),part.layoutWidth,part.layoutHeight);
            }else if(net.foundations.pl4.core.LayoutTransactions.layerAction(packet.action)){
                var ids=Arrays.stream(packet.value.split(",",-1)).map(UUID::fromString).toList();
                result=net.foundations.pl4.core.LayoutTransactions.applyLayers(before,packet.revision,packet.action,ids);
            }else if(net.foundations.pl4.core.LayoutTransactions.arrangement(packet.action)){
                var ids=Arrays.stream(packet.value.split(",",-1)).map(UUID::fromString).toList();
                result=net.foundations.pl4.core.LayoutTransactions.applyArrange(before,packet.revision,packet.action,ids,part.layoutWidth,part.layoutHeight);
            }else{
                result=net.foundations.pl4.core.LayoutTransactions.apply(before,packet.revision,packet.action,packet.element,spec,packet.value);
            }
            if(!result.accepted()){openWithError(player,anchor,clicked,result.message());return;}
            long nextRevision=DisplayNetworks.nextLayoutRevision(part); // Preflight before changing even the root.
            var settings=new Part.DisplaySettings(part.label,part.selected,part.metric,part.color,result.state().elements().stream().map(Part.Element::new).toList(),result.state().mode(),result.state().page(),part.layoutWidth,part.layoutHeight);
            DisplayNetworks.applyLayout(host,part,settings,nextRevision);host.changed();reply(player,anchor,clicked);
        }catch(RuntimeException ex){openWithError(player,anchor,clicked,"Display edit failed; refresh the layout before retrying.");}
    }
    private static void openWithError(ServerPlayer player,HostEntity host,Part part,String error){sendOpen(player,host,part,error,true);}
    public static void register(RegisterPayloadHandlersEvent event){
        var r=event.registrar("4");
        r.playToServer(LayoutEdit.TYPE,LayoutEdit.CODEC,(packet,context)->context.enqueueWork(()->{if(context.player() instanceof ServerPlayer player)editLayout(player,packet);}));
        r.playToClient(Open.TYPE,Open.CODEC,(packet,context)->context.enqueueWork(()->clientOpen.accept(packet)));
        r.playToServer(Edit.TYPE,Edit.CODEC,(packet,context)->context.enqueueWork(()->{if(context.player() instanceof ServerPlayer player)edit(player,packet);}));
    }
    public static void open(ServerPlayer player,HostEntity host,Part p){sendOpen(player,host,p,"",false);}
    private static void reply(ServerPlayer player,HostEntity host,Part p){sendOpen(player,host,p,"",true);}
    private static void sendOpen(ServerPlayer player,HostEntity host,Part p,String error,boolean reply){
        var target=DisplayNetworks.controller(host,p);
        CompoundTag t=target.part().save(host.getLevel().registryAccess(),true);
        // The packet remains anchored to the clicked tile. Distance/identity checks never trust a remote root.
        t.store("identity",net.minecraft.core.UUIDUtil.CODEC,p.identity);t.putBoolean("reply",reply);t.putString("layoutError",error);t.putBoolean("editable",host.canEdit(player)&&target.host().canEdit(player)&&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(host.getBlockPos()))<=64);
        PacketDistributor.sendToPlayer(player,new Open(host.getBlockPos(),p.slot(),t));
    }
    private static void edit(ServerPlayer player,Edit packet){
        if(packet.slot<0||packet.slot>=net.foundations.pl4.core.MultipartTopology.SLOT_COUNT||player.distanceToSqr(packet.net.minecraft.world.phys.Vec3.atCenterOf(pos))>64||!player.level().hasChunkAt(packet.pos)||!player.level().mayInteract(player,packet.pos))return;
        if(!(player.level().getBlockEntity(packet.pos) instanceof HostEntity h))return;
        Part p=h.parts.get(packet.slot);if(p==null||!p.identity.equals(packet.identity))return;
        if(packet.field.equals("preview_reader")){
            long tick=player.level().getGameTime();Rate rate=EDIT_RATE.get(player);if(rate!=null&&rate.tick==tick&&rate.count>=4)return;EDIT_RATE.put(player,new Rate(tick,rate!=null&&rate.tick==tick?rate.count+1:1));
            if(!p.kind.display()||packet.value.length()>64)return;var target=DisplayNetworks.controller(h,p);
            CompoundTag tag=target.part().save(h.getLevel().registryAccess(),true);tag.store("identity",net.minecraft.core.UUIDUtil.CODEC,p.identity);tag.putBoolean("editable",h.canEdit(player)&&target.host().canEdit(player));tag.putString("previewReader",packet.value);tag.putBoolean("reply",true);
            var rows=new net.minecraft.nbt.ListTag();DisplayNetworks.preview(h,p,packet.value).forEach(row->rows.add(row.save()));tag.put("previewRows",rows);PacketDistributor.sendToPlayer(player,new Open(h.getBlockPos(),p.slot(),tag));return;
        }
        if(packet.field.equals("refresh")){reply(player,h,p);return;}
        if(!h.canEdit(player))return;
        HostEntity anchorHost=h;Part anchorPart=p;var target=DisplayNetworks.controller(h,p);
        h=target.host();p=target.part();if(!h.canEdit(player)||!h.getLevel().mayInteract(player,h.getBlockPos())||!DisplayNetworks.canEditCanvas(player,anchorHost,anchorPart))return;
        String v=packet.value;
        if(packet.field.equals("display_outward")){
            if(p.hologram())return;
            if(p.kind.display()&&(v.equals("true")||v.equals("false")))DisplayNetworks.flip(player,anchorHost,anchorPart,Boolean.parseBoolean(v));
            reply(player,anchorHost,anchorPart);return;
        }
        try{
            switch(packet.field){
                case "hologram_view" -> {
                    if(!p.hologram()||p.face.getAxis()!=net.minecraft.core.Direction.Axis.Y)return;
                    int n=Integer.parseInt(v);if(n<2||n>5)return;
                    int previous=p.hologramView;p.hologramView=n;
                    var shape=MultipartShapes.part(h.parts.values(),p);
                    for(Part other:h.parts.values())if(other!=p&&net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,MultipartShapes.part(h.parts.values(),other),net.minecraft.world.phys.shapes.BooleanOp.AND)){
                        p.hologramView=previous;reply(player,anchorHost,anchorPart);return;
                    }
                }
                case "energy_convert" -> {
                    if(p.kind!=Kind.TRANSFER_NODE||!p.energyRouteEditable()||!Set.of("true","false").contains(v))return;
                    p.energyConvert=Boolean.parseBoolean(v);if(!p.energyConvert)p.energyOutput=p.energyInput;
                }
                case "energy_input", "energy_output" -> {
                    if(p.kind!=Kind.TRANSFER_NODE||!p.energyRouteEditable()||!Set.of("FE","J","EU","ED_J").contains(v)||!EnergyPorts.supported(v)||!EnergyPorts.enabled(v)){reply(player,anchorHost,anchorPart);return;}
                    if(packet.field.equals("energy_input")){p.energyInput=v;if(!p.energyConvert)p.energyOutput=v;}
                    else {if(!p.energyConvert)return;p.energyOutput=v;}
                }
                case "energy_voltage" -> {if(p.kind!=Kind.TRANSFER_NODE||!p.energyRouteEditable())return;int n=Integer.parseInt(v);if(n<1||n>1048576)return;p.energyVoltage=n;}
                case "energy_system" -> {if(p.kind==Kind.ENERGY_READER&&Set.of("AUTO","FE","EU","J").contains(v))p.energySystem=v;else return;}
                case "label" -> p.label=clean(v,48);
                case "filter" -> p.filter=clean(v,256);
                case "selected" -> p.selected=clean(v,64);
                case "metric" -> p.metric=clean(v,128);
                case "mode" -> {if(p.kind.display())return;if(Set.of("LIST","STACK","SLOT","POS","STORAGE","CHANNEL").contains(v))p.mode=v;}
                case "index" -> p.index=Math.clamp(Integer.parseInt(v),0,65535);
                case "priority" -> p.priority=Math.clamp(Integer.parseInt(v),-1000,1000);
                case "comparison" -> {if(Set.of(">=","<=",">","<","=","!=").contains(v))p.comparison=v;}
                case "threshold" -> {double n=Double.parseDouble(v);if(Double.isFinite(n)&&Math.abs(n)<=1e15)p.threshold=n;}
                case "color" -> p.color=Integer.parseUnsignedInt(v.replace("#",""),16)&0xFFFFFF;
                case "transfer" -> p.transferMode=Math.clamp(Integer.parseInt(v),0,3);
                case "items" -> p.items=Boolean.parseBoolean(v);
                case "fluids" -> p.fluids=Boolean.parseBoolean(v);
                case "energy" -> p.energy=Boolean.parseBoolean(v);
                case "descending" -> p.descending=Boolean.parseBoolean(v);
                case "whitelist" -> p.whitelist=Boolean.parseBoolean(v);
                case "clear_links" -> p.links.clear();
                case "clear_elements" -> {if(!p.kind.display())return;p.elements.clear();p.displayMode=net.foundations.pl4.core.DisplayElements.Mode.CUSTOM;}
                case "element" -> {
                    if(!p.kind.display()||p.elements.size()>=32)break;
                    String[] fields=v.split("\\|",6);
                    if(fields.length==6){p.displayMode=net.foundations.pl4.core.DisplayElements.Mode.CUSTOM;p.elements.add(new Part.Element(clean(fields[5],128),"",clean(fields[4],128),Math.clamp(Integer.parseInt(fields[0]),0,240),Math.clamp(Integer.parseInt(fields[1]),0,120),Integer.parseUnsignedInt(fields[2],16)&0xFFFFFF,Boolean.parseBoolean(fields[3])));}
                }
                default -> {return;}
            }
        }catch(IllegalArgumentException ignored){return;}
        if(p.kind.display())DisplayNetworks.layoutEdited(h,p);
        h.changed();reply(player,anchorHost,anchorPart);
    }
    private static String clean(String s,int length){String value=s.replaceAll("[\\p{Cntrl}§]","");return value.substring(0,Math.min(length,value.length()));}
}
