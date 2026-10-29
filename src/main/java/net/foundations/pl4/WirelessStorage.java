package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/** Bounded server snapshots. Sessions retain identities and item variants, never capabilities. */
public final class WirelessStorage {
    public static final int PAGE_SIZE=6,MAX_SLOTS=4096,MAX_ENDPOINTS=256;
    private record Target(Part node,IItemHandler inventory,Part.Link link,NetworkEngine.Ref ref,TransferEngine.ItemTarget backing){}
    /** Recheck current server authority after any foreign inventory callback.
     * A retained capability or a prior UI session is never an access grant. */
    static boolean authorizedTarget(ServerPlayer player,Part.Link binding,NetworkEngine.Ref ref){
        if(ref==null||anchor(player,binding)==null)return false;
        HostEntity host=ref.host();
        if(!(host.getLevel() instanceof net.minecraft.server.level.ServerLevel world)
            ||world.getServer()!=player.serverLevel().getServer()
            ||host.isRemoved()||!world.hasChunkAt(host.getBlockPos())
            ||world.getBlockEntity(host.getBlockPos())!=host
            ||!host.parts.containsValue(ref.part())||!host.canEdit(player)
            ||!world.mayInteract(player,host.getBlockPos())||!ref.part().items
            ||(ref.part().kind!=Kind.NODE&&ref.part().kind!=Kind.TRANSFER_NODE)
            ||(!PLConfig.CROSS_DIMENSION.get()&&world!=player.serverLevel()))return false;
        Part.Link link=ref.adjacent();
        return world.hasChunkAt(link.pos())&&world.mayInteract(player,link.pos());
    }
    /** Cached session capabilities cannot survive target block-entity replacement at the same position. */
    static boolean liveBackingTarget(ServerPlayer player,Part.Link binding,NetworkEngine.Ref ref,TransferEngine.ItemTarget backing){
        return backing!=null&&authorizedTarget(player,binding,ref)
            &&backing.live(player.serverLevel().getServer(),ref);
    }
    private static boolean live(ServerPlayer player,Part.Link binding,Target target){
        return target.link().equals(target.ref().adjacent())
            &&liveBackingTarget(player,binding,target.ref(),target.backing());
    }
    /** After a provider mutated items, replacement of its backing block is also revocation. */
    static void requirePostExtractionBackingAuthority(ServerPlayer player,Part.Link binding,
                                                       NetworkEngine.Ref ref,TransferEngine.ItemTarget backing){
        if(liveBackingTarget(player,binding,ref,backing))return;
        sealPostExtractionRevocation(ref);
    }
    private static void sealPostExtractionRevocation(NetworkEngine.Ref ref){
        ref.part().itemTransferFault="Wireless extraction authority changed after provider mutation; manual inspection required";
        ref.host().setChanged();
        throw new IllegalStateException("Wireless withdrawal authority revoked during provider mutation");
    }
    /** Preserve the actual extracted stack and seal it when a provider revokes
     * access after mutation. This must execute AFTER extract() journals the result,
     * never from inside the supplier before its result can be recorded. */
    static void requirePostExtractionAuthority(ServerPlayer player,Part.Link binding,NetworkEngine.Ref ref,Part.Link target){
        if(authorizedTarget(player,binding,ref)&&target.equals(ref.adjacent()))return;
        sealPostExtractionRevocation(ref);
    }
    private record Slot(UUID node,int slot){}
    static final class Entry {
        final ItemStack item;long count;final List<Slot> sources=new ArrayList<>();
        Entry(ItemStack item){this.item=item.copyWithCount(1);}
    }
    private record View(List<Entry> entries,int endpoints,int slots,boolean limited){}
    private record Session(UUID token,InteractionHand hand,ItemStack tool,Part.Link binding,int page,List<Entry> shown,String query,String sort,int opened){}
    private record Rate(int tick,int count){}
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Rate> RATES=new WeakHashMap<>();
    private static HostEntity anchor(ServerPlayer player,Part.Link binding){
        if(player.isSpectator()||binding==null||binding.part()==null||binding.entity()!=null||!PLConfig.WIRELESS.get()||!PLConfig.TRANSFERS.get())return null;
        if(!PLConfig.CROSS_DIMENSION.get()&&!player.level().dimension().location().toString().equals(binding.dimension()))return null;
        var server=player.serverLevel().getServer();if(!NetworkEngine.loaded(server,binding))return null;
        var world=NetworkEngine.level(server,binding);
        if(!(world.getBlockEntity(binding.pos()) instanceof HostEntity host)||!host.canEdit(player)||!world.mayInteract(player,binding.pos()))return null;
        return host.parts.values().stream().anyMatch(p->p.identity.equals(binding.part())&&(p.kind==Kind.NODE||p.kind==Kind.TRANSFER_NODE))?host:null;
    }
    private static List<Target> resolve(ServerPlayer player,Part.Link binding){
        HostEntity host=anchor(player,binding);if(host==null)return null;
        Part node=host.parts.values().stream().filter(p->p.identity.equals(binding.part())).findFirst().orElseThrow();
        List<Target> result=new ArrayList<>();SampleSources seen=new SampleSources();int slots=0;
        for(var ref:NetworkEngine.storageNodes(player.serverLevel().getServer(),node)){
            if(result.size()>=MAX_ENDPOINTS||slots>=MAX_SLOTS)break;
            var world=ref.level();var link=ref.adjacent();
            if(!WirelessItemTransfers.available(ref.part())||ref.host().isRemoved()||!world.hasChunkAt(ref.host().getBlockPos())||!ref.host().canEdit(player)||!world.mayInteract(player,ref.host().getBlockPos())||!ref.part().items)continue;
            if(!PLConfig.CROSS_DIMENSION.get()&&world!=player.serverLevel())continue;
            if(!world.hasChunkAt(link.pos())||!world.mayInteract(player,link.pos()))continue;
            TransferEngine.ItemTarget backing=TransferEngine.itemTarget(player.serverLevel().getServer(),ref);
            if(backing==null)continue;
            IItemHandler inventory=backing.handler();
            int count=inventory.getSlots();
            Target candidate=new Target(ref.part(),inventory,link,ref,backing);
            if(!live(player,binding,candidate))return null;
            if(!seen.inventory(world,link,inventory,count))continue;
            result.add(candidate);slots+=Math.min(MAX_SLOTS-slots,Math.max(0,count));
        }
        return result;
    }
    private static View view(ServerPlayer player,Part.Link binding,List<Target> targets,String query,String sort){
        Map<net.minecraft.world.item.Item,List<Entry>> byItem=new LinkedHashMap<>();int scanned=0;boolean limited=targets.size()>=MAX_ENDPOINTS;
        outer:for(Target target:targets){
            if(!live(player,binding,target))return null;
            int slotCount=target.inventory().getSlots();
            if(!live(player,binding,target))return null;
            for(int slot=0;slot<slotCount;slot++){
            if(scanned>=MAX_SLOTS){limited=true;break outer;}scanned++;
            ItemStack stack=target.inventory().getStackInSlot(slot);
            if(!live(player,binding,target))return null;
            if(stack.isEmpty()||!TransferFilters.items(stack,target.node(),false))continue;
            String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if(!query.isEmpty()&&!id.contains(query)&&!stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))continue;
            List<Entry> variants=byItem.computeIfAbsent(stack.getItem(),unused->new ArrayList<>());Entry row=null;
            for(Entry existing:variants)if(ItemStack.isSameItemSameComponents(existing.item,stack)){row=existing;break;}
            if(row==null){row=new Entry(stack);variants.add(row);}row.count+=stack.getCount();row.sources.add(new Slot(target.node().identity,slot));
            }
        }
        List<Entry> rows=new ArrayList<>();byItem.values().forEach(rows::addAll);
        Comparator<Entry> names=Comparator.comparing((Entry e)->e.item.getHoverName().getString(),String.CASE_INSENSITIVE_ORDER).thenComparing(e->BuiltInRegistries.ITEM.getKey(e.item.getItem()).toString());
        rows.sort(sort.equals("COUNT")?Comparator.<Entry>comparingLong(e->e.count).reversed().thenComparing(names):names);
        return new View(rows,targets.size(),scanned,limited);
    }
    public static UUID open(ServerPlayer player,InteractionHand hand){
        ItemStack tool=player.getItemInHand(hand);Part.Link binding=ToolItem.link(tool);
        if(!tool.is(FoundationsPL4.item("wirelessstorage"))){SESSIONS.remove(player);return null;}
        List<Target> targets=resolve(player,binding);
        if(targets==null){SESSIONS.remove(player);player.displayClientMessage(Component.literal("Bind Wireless Storage to your loaded network Node. Wireless and transfers must be enabled."),true);return null;}
        return send(player,hand,tool,binding,targets,0,"","NAME",false,"");
    }
    static void request(ServerPlayer player,PLPackets.StorageRequest packet){
        Session session=SESSIONS.get(player);
        if(session==null||!session.token().equals(packet.token())||player.tickCount-session.opened()>1200)return;
        Rate rate=RATES.get(player);if(rate!=null&&rate.tick()==player.tickCount&&rate.count()>=4)return;
        RATES.put(player,new Rate(player.tickCount,rate!=null&&rate.tick()==player.tickCount?rate.count()+1:1));
        SESSIONS.remove(player);
        ItemStack held=player.getItemInHand(session.hand());
        if(held!=session.tool()||!held.is(FoundationsPL4.item("wirelessstorage"))||!Objects.equals(ToolItem.link(held),session.binding()))return;
        List<Target> targets=resolve(player,session.binding());if(targets==null){sendClosed(player,"Target unavailable or permission changed.");return;}
        try{
        int page=session.page();String status="",query=session.query(),sort=session.sort();
        if(packet.action().equals("page"))page=Math.clamp(packet.index(),0,MAX_SLOTS/PAGE_SIZE);
        else if(packet.action().equals("search")){
            query=packet.query().replaceAll("[\\p{Cntrl}§]","").trim().toLowerCase(Locale.ROOT);if(query.length()>64||!Set.of("NAME","COUNT").contains(packet.sort())){sendClosed(player,"Invalid search.");return;}sort=packet.sort();page=0;
        }else if(packet.action().equals("withdraw")){
            int local=packet.index()-page*PAGE_SIZE;
            if(local<0||local>=session.shown().size()||packet.amount()<1||packet.amount()>64){sendClosed(player,"Invalid storage request.");return;}
            Entry expected=session.shown().get(local);int remaining=Math.min(packet.amount(),room(player,expected.item)),moved=0;
            Map<UUID,Target> current=new HashMap<>();targets.forEach(t->current.put(t.node().identity,t));
            for(Slot source:expected.sources){
                if(remaining<=0)break;Target target=current.get(source.node());
                if(target==null||!live(player,session.binding(),target))continue;
                int slotCount=target.inventory().getSlots();
                if(!live(player,session.binding(),target)||source.slot()<0||source.slot()>=slotCount)continue;
                ItemStack stack=target.inventory().getStackInSlot(source.slot());
                if(!live(player,session.binding(),target))continue;
                if(!ItemStack.isSameItemSameComponents(expected.item,stack)||!TransferFilters.items(stack,target.node(),false))continue;
                ItemStack trial=target.inventory().extractItem(source.slot(),remaining,true);
                if(!live(player,session.binding(),target)||!WirelessItemTransfers.available(target.node()))continue;
                if(!ItemTransferSafety.trial(trial,remaining)||!ItemStack.isSameItemSameComponents(expected.item,trial))continue;
                int requested=Math.min(remaining,trial.getCount());
                ItemStack extracted=ItemTransferSafety.extract(target.ref(),expected.item,requested,()->{
                    if(!live(player,session.binding(),target))throw new IllegalStateException("Wireless withdrawal authority revoked before provider mutation");
                    return target.inventory().extractItem(source.slot(),requested,false);
                });
                requirePostExtractionBackingAuthority(player,session.binding(),target.ref(),target.backing());
                requirePostExtractionAuthority(player,session.binding(),target.ref(),target.link());
                if(extracted.isEmpty())continue;int count=WirelessItemTransfers.deliver(target.ref(),stackToPlayer->{player.getInventory().add(stackToPlayer);return stackToPlayer;});moved+=count;remaining-=extracted.getCount();
            }
            player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();status=moved==0?"Nothing moved: contents, access, filters or available space changed.":"Withdrew "+moved+" item(s).";
        }else if(packet.action().equals("deposit")){
            if(session.hand()!=InteractionHand.MAIN_HAND)status="Hold the tool in your main hand; deposit from your offhand.";
            else{
                ItemStack offhand=player.getOffhandItem();int moved=0,scanned=0;
                outer:for(Target target:targets){
                    if(offhand.isEmpty())break;if(!live(player,session.binding(),target)||!TransferFilters.items(offhand,target.node(),true))continue;
                    int slotCount=target.inventory().getSlots();
                    if(!live(player,session.binding(),target))continue;
                    for(int slot=0;slot<slotCount;slot++){
                        if(offhand.isEmpty()||scanned++>=MAX_SLOTS)break outer;
                        ItemStack offered=offhand.copy();ItemStack simulated=target.inventory().insertItem(slot,offered,true);
                        if(!live(player,session.binding(),target))continue outer;
                        int capacity=offered.getCount()-ItemTransferSafety.remainder(offered,simulated);if(capacity<=0)continue;
                        ItemStack sent=offhand.copyWithCount(Math.min(capacity,offhand.getCount()));final int destinationSlot=slot;
                        int accepted=WirelessItemTransfers.deposit(target.ref(),offhand,sent,value->{
                            if(!live(player,session.binding(),target))throw new IllegalStateException("Wireless deposit authority revoked before provider mutation");
                            ItemStack remainder=target.inventory().insertItem(destinationSlot,value,false);
                            if(!live(player,session.binding(),target))throw new IllegalStateException("Wireless deposit authority revoked during provider mutation");
                            return remainder;
                        });moved+=accepted;
                    }
                }
                player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();status="Deposited "+moved+" item(s).";
            }
        }else if(!packet.action().equals("refresh")){sendClosed(player,"Unknown storage action.");return;}
        send(player,session.hand(),held,session.binding(),targets,page,query,sort,true,status);
        }catch(RuntimeException failure){
            org.slf4j.LoggerFactory.getLogger("FoundationsPL4").error("Wireless Storage provider failed; inspect node escrow before recovery",failure);
            sendClosed(player,"Storage provider failed; inspect node escrow before recovery.");
        }finally{player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();}
    }
    private static int room(ServerPlayer player,ItemStack stack){int room=0;for(int i=0;i<36;i++){ItemStack existing=player.getInventory().getItem(i);if(existing.isEmpty())room+=stack.getMaxStackSize();else if(ItemStack.isSameItemSameComponents(existing,stack))room+=Math.max(0,Math.min(existing.getMaxStackSize(),stack.getMaxStackSize())-existing.getCount());if(room>=64)return 64;}return room;}
    private static UUID send(ServerPlayer player,InteractionHand hand,ItemStack tool,Part.Link binding,List<Target> targets,int page,String query,String sort,boolean reply,String status){
        View view=view(player,binding,targets,query,sort);
        if(view==null){SESSIONS.remove(player);sendClosed(player,"Storage access changed during provider enumeration.");return null;}
        int count=view.entries().size();page=Math.clamp(page,0,Math.max(0,(count-1)/PAGE_SIZE));
        UUID token=UUID.randomUUID();List<Entry> shown=List.copyOf(view.entries().subList(page*PAGE_SIZE,Math.min(count,(page+1)*PAGE_SIZE)));ListTag rows=new ListTag();
        for(int i=0;i<shown.size();i++){Entry e=shown.get(i);CompoundTag row=new CompoundTag();row.putInt("slot",page*PAGE_SIZE+i);row.putString("name",e.item.getHoverName().getString());row.putLong("count",e.count);rows.add(row);}
        SESSIONS.put(player,new Session(token,hand,tool,binding,page,shown,query,sort,player.tickCount));
        CompoundTag tag=new CompoundTag();tag.putBoolean("wirelessStorage",true);tag.putBoolean("reply",reply);tag.putString("token",token.toString());tag.putInt("page",page);tag.putInt("slots",count);tag.putInt("endpoints",view.endpoints());tag.putInt("scanned",view.slots());tag.putBoolean("limited",view.limited());tag.putString("query",query);tag.putString("sort",sort);tag.put("storageRows",rows);tag.putString("status",status);
        PacketDistributor.sendToPlayer(player,new PLPackets.Open(BlockPos.ZERO,-1,tag));return token;
    }
    static List<Long> visibleCounts(ServerPlayer player){Session s=SESSIONS.get(player);return s==null?List.of():s.shown().stream().map(e->e.count).toList();}
    private static void sendClosed(ServerPlayer player,String status){CompoundTag tag=new CompoundTag();tag.putBoolean("wirelessStorage",true);tag.putBoolean("reply",true);tag.putBoolean("closed",true);tag.putString("status",status);PacketDistributor.sendToPlayer(player,new PLPackets.Open(BlockPos.ZERO,-1,tag));}
    private WirelessStorage(){}
}
