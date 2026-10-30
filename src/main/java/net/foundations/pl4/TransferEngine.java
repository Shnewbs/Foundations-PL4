package net.foundations.pl4;

import java.util.*;
import net.foundations.pl4.core.TransferRules;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Server-authoritative resource movement over one data-network component.
 *
 * <p>Normal Nodes expose passive capability endpoints, matching PL2's useful network-pool
 * behavior. A unidirectional Transfer Node drives movement: REMOVE exports its attached
 * target into explicit ADD peers first and then passive Nodes; ADD imports from explicit
 * REMOVE peers first and then passive Nodes. ADD/REMOVE is peer-only until the historical
 * directional channel/filter model is restored, avoiding blind passive-network ping-pong.</p>
 *
 * <p>Every transfer is simulated before extraction. If a provider still accepts less than
 * simulation promised, the driving Transfer Node owns persistent escrow and retries it on
 * a later cycle. No chunks are force-loaded.</p>
 */
public final class TransferEngine {
    private record PairKey(UUID source,UUID sink){}
    private record TargetKey(String dimension,long pos,int side){}

    private static final class Budgets {
        final Map<UUID,Integer> items=new HashMap<>(),fluids=new HashMap<>(),energy=new HashMap<>();
        final TransferRules.CycleLimit itemCap=new TransferRules.CycleLimit(PLConfig.NETWORK_ITEM_RATE.get()),fluidCap=new TransferRules.CycleLimit(PLConfig.NETWORK_FLUID_RATE.get()),energyCap=new TransferRules.CycleLimit(PLConfig.NETWORK_ENERGY_RATE.get());
        TransferRules.CycleLimit cap(Map<UUID,Integer> used){return used==items?itemCap:used==fluids?fluidCap:energyCap;}
        int remaining(Map<UUID,Integer> used,NetworkEngine.Ref ref,int limit){
            int node=ref.part().kind==Kind.TRANSFER_NODE?Math.max(0,limit-used.getOrDefault(ref.part().identity,0)):Integer.MAX_VALUE;
            return Math.min(node,cap(used).remaining());
        }
        void delivered(Map<UUID,Integer> used,int amount){cap(used).delivered(amount);}
        void use(Map<UUID,Integer> used,NetworkEngine.Ref ref,int amount){
            if(amount>0&&ref.part().kind==Kind.TRANSFER_NODE)used.merge(ref.part().identity,amount,Integer::sum);
        }
    }

    /** Topology-owned membership/order. Modes, permissions, filters and capabilities stay live. */
    public static final class Plan {
        private final List<NetworkEngine.Ref> endpoints,drivers;
        private long cycle;
        long routingCursor(){return Math.max(0,cycle-1);}
        public Plan(List<NetworkEngine.Ref> endpoints,List<NetworkEngine.Ref> drivers){this.endpoints=List.copyOf(endpoints);this.drivers=List.copyOf(drivers);}
        public List<NetworkEngine.Ref> endpoints(){return endpoints;}
        public List<NetworkEngine.Ref> drivers(){return drivers;}
        private List<NetworkEngine.Ref> nextDrivers(){return rotateTies(drivers,cycle++).stream().sorted(Comparator.comparing(r->!hasPending(r.part()))).toList();}
    }
    public static Plan prepare(List<NetworkEngine.Ref> network){
        List<NetworkEngine.Ref> endpoints=network.stream().filter(r->r.part().kind==Kind.NODE||r.part().kind==Kind.TRANSFER_NODE).toList();
        List<NetworkEngine.Ref> transfer=endpoints.stream().filter(r->r.part().kind==Kind.TRANSFER_NODE).sorted(driverOrder()).toList();
        return new Plan(endpoints,transfer);
    }
    public static void run(MinecraftServer server,List<NetworkEngine.Ref> network){run(server,prepare(network));}
    public static void run(MinecraftServer server,Plan plan){
        List<NetworkEngine.Ref> endpoints=available(plan.endpoints()),transfer=available(plan.nextDrivers());
        if(transfer.isEmpty())return;
        boolean nativeEnergy=NativeEnergyTransfers.needed(plan);
        Map<NetworkEngine.Ref,TargetKey> targets=new IdentityHashMap<>();
        for(NetworkEngine.Ref endpoint:endpoints)targets.put(endpoint,target(endpoint));

        Budgets budgets=new Budgets();
        Set<PairKey> itemPairs=new HashSet<>(),fluidPairs=new HashSet<>(),energyPairs=new HashSet<>();
        Set<TargetKey> itemReceived=new HashSet<>(),fluidReceived=new HashSet<>(),energyReceived=new HashSet<>();

        // Preserve R15 source-side escrow from ADD/REMOVE nodes even though R16 no longer lets
        // bidirectional nodes autonomously drive the passive endpoint pool.
        for(NetworkEngine.Ref ref:transfer)if(ref.part().transferMode==TransferRules.ADD_REMOVE&&hasPending(ref.part())){
            List<NetworkEngine.Ref> sinks=sinks(endpoints,ref,plan.routingCursor(),targets);
            if(ref.part().items&&!ref.part().pendingItem.isEmpty())flushItem(server,ref,sinks,budgets,itemPairs,itemReceived);
            if(ref.part().fluids&&!ref.part().pendingFluid.isEmpty())flushFluid(server,ref,sinks,budgets,fluidPairs,fluidReceived);
            if(!nativeEnergy&&ref.part().energy&&ref.part().pendingEnergy>0)flushEnergy(server,ref,sinks,budgets,energyPairs,energyReceived);
        }

        // REMOVE drives exports. This phase keeps the old REMOVE -> ADD behavior and also
        // allows REMOVE -> ordinary Node endpoints.
        for(NetworkEngine.Ref source:transfer)if(TransferRules.drivesRemove(source.part().transferMode)){
            List<NetworkEngine.Ref> sinks=sinks(endpoints,source,plan.routingCursor(),targets);
            if(source.part().items)pushItems(server,source,sinks,budgets,itemPairs,itemReceived);
            if(source.part().fluids)pushFluids(server,source,sinks,budgets,fluidPairs,fluidReceived);
            if(!nativeEnergy&&source.part().energy)pushEnergy(server,source,sinks,budgets,energyPairs,energyReceived);
        }

        // ADD drives imports. Explicit REMOVE peers already handled above are skipped by the
        // pair ledger; passive Nodes now work as PL2-style network sources.
        for(NetworkEngine.Ref sink:transfer)if(TransferRules.drivesAdd(sink.part().transferMode)){
            List<NetworkEngine.Ref> sources=sources(endpoints,sink,plan.routingCursor(),targets);
            if(sink.part().items)pullItems(server,sink,sources,budgets,itemPairs,itemReceived);
            if(sink.part().fluids)pullFluids(server,sink,sources,budgets,fluidPairs,fluidReceived);
            if(!nativeEnergy&&sink.part().energy)pullEnergy(server,sink,sources,budgets,energyPairs,energyReceived);
        }

        if(nativeEnergy){var energyPlan=new Plan(endpoints,transfer);energyPlan.cycle=plan.cycle;NativeEnergyTransfers.run(server,energyPlan);}
        for(NetworkEngine.Ref ref:transfer){updateStatus(ref,endpoints,targets);if(nativeEnergy&&!ref.part().energyTransferStatus.isEmpty())ref.part().status+="; "+ref.part().energyTransferStatus;}
    }

    /** Healthy cycles reuse their immutable lists. A rare fault filters once, before any routing. */
    private static List<NetworkEngine.Ref> available(List<NetworkEngine.Ref> refs){
        for(NetworkEngine.Ref ref:refs)if(ResourceTransferSafety.blocked(ref.part())){
            List<NetworkEngine.Ref> result=new ArrayList<>();
            for(NetworkEngine.Ref candidate:refs){
                if(ResourceTransferSafety.blocked(candidate.part()))candidate.part().status="Resource transfer quarantined; manual inspection required";
                else result.add(candidate);
            }
            return result;
        }
        return refs;
    }
    private static boolean hasPending(Part p){return !p.pendingItem.isEmpty()||!p.pendingFluid.isEmpty()||p.energyCredits()>0;}
    private static Comparator<NetworkEngine.Ref> driverOrder(){
        return Comparator.<NetworkEngine.Ref>comparingInt(r->r.part().priority).reversed()
            .thenComparing(r->r.level().dimension().location().toString())
            .thenComparingLong(r->r.host().getBlockPos().asLong()).thenComparingInt(r->r.part().slot());
    }
    private static Comparator<NetworkEngine.Ref> endpointOrder(){
        return Comparator.<NetworkEngine.Ref>comparingInt(r->TransferRules.endpointClass(r.part().kind==Kind.TRANSFER_NODE)).reversed()
            .thenComparing(Comparator.<NetworkEngine.Ref>comparingInt(r->r.part().kind==Kind.TRANSFER_NODE?r.part().priority:0).reversed())
            .thenComparing(r->r.level().dimension().location().toString())
            .thenComparingLong(r->r.host().getBlockPos().asLong()).thenComparingInt(r->r.part().slot());
    }
    private static List<NetworkEngine.Ref> rotateTies(List<NetworkEngine.Ref> refs,long cursor){
        if(refs.size()<2)return refs;
        ArrayList<NetworkEngine.Ref> sorted=new ArrayList<>(refs);sorted.sort(endpointOrder());
        ArrayList<NetworkEngine.Ref> out=new ArrayList<>(sorted.size());
        for(int from=0;from<sorted.size();){
            NetworkEngine.Ref first=sorted.get(from);boolean type=first.part().kind==Kind.TRANSFER_NODE;int priority=type?first.part().priority:0;int to=from+1;
            while(to<sorted.size()){
                var r=sorted.get(to);boolean rt=r.part().kind==Kind.TRANSFER_NODE;int rp=rt?r.part().priority:0;
                if(rt!=type||rp!=priority)break;to++;
            }
            int size=to-from,shift=Math.floorMod((int)(cursor%Math.max(1,size)),size);
            for(int i=0;i<size;i++)out.add(sorted.get(from+(i+shift)%size));
            from=to;
        }
        return out;
    }
    static List<NetworkEngine.Ref> sinks(List<NetworkEngine.Ref> endpoints,NetworkEngine.Ref source,long cursor){
        return sinks(endpoints,source,cursor,new IdentityHashMap<>());
    }
    static List<NetworkEngine.Ref> sources(List<NetworkEngine.Ref> endpoints,NetworkEngine.Ref sink,long cursor){
        return sources(endpoints,sink,cursor,new IdentityHashMap<>());
    }
    private static List<NetworkEngine.Ref> sinks(List<NetworkEngine.Ref> endpoints,NetworkEngine.Ref source,long cursor,Map<NetworkEngine.Ref,TargetKey> targets){
        return rotateTies(endpoints.stream().filter(s->routable(source,s,targets)).toList(),cursor);
    }
    private static List<NetworkEngine.Ref> sources(List<NetworkEngine.Ref> endpoints,NetworkEngine.Ref sink,long cursor,Map<NetworkEngine.Ref,TargetKey> targets){
        return rotateTies(endpoints.stream().filter(s->routable(s,sink,targets)).toList(),cursor);
    }
    private static boolean routable(NetworkEngine.Ref source,NetworkEngine.Ref sink,Map<NetworkEngine.Ref,TargetKey> targets){
        // A cached transfer plan is not authority: live owner and host membership can
        // change between topology rebuilds or while a provider callback executes.
        return source!=sink&&source.part().owner!=null
            &&source.part().owner.equals(sink.part().owner)
            &&source.host().parts.get(source.part().slot())==source.part()
            &&sink.host().parts.get(sink.part().slot())==sink.part()
            &&!sameTarget(source,sink,targets)&&TransferRules.canRouteOnChannels(
            source.part().kind==Kind.TRANSFER_NODE,source.part().transferMode,source.part().outputChannel,
            sink.part().kind==Kind.TRANSFER_NODE,sink.part().transferMode,sink.part().inputChannel);
    }
    private static PairKey pair(NetworkEngine.Ref source,NetworkEngine.Ref sink){return new PairKey(source.part().identity,sink.part().identity);}
    private static TargetKey target(NetworkEngine.Ref ref){Part.Link l=ref.adjacent();return new TargetKey(l.dimension(),l.pos().asLong(),l.side().ordinal());}
    private static boolean sameTarget(NetworkEngine.Ref a,NetworkEngine.Ref b,Map<NetworkEngine.Ref,TargetKey> targets){return targets.computeIfAbsent(a,TransferEngine::target).equals(targets.computeIfAbsent(b,TransferEngine::target));}
    private static boolean liveItemEndpoint(MinecraftServer server,NetworkEngine.Ref ref){
        if(ref.part().owner==null||ref.host().isRemoved()||!(ref.host().getLevel() instanceof ServerLevel level)
            ||level.getServer()!=server||!level.hasChunkAt(ref.host().getBlockPos())
            ||level.getBlockEntity(ref.host().getBlockPos())!=ref.host()
            ||ref.host().parts.get(ref.part().slot())!=ref.part())return false;
        return NetworkEngine.loaded(server,ref.adjacent());
    }
    /** No cached topology decision grants authority after a provider callback. */
    static boolean liveItemRoute(MinecraftServer server,NetworkEngine.Ref source,NetworkEngine.Ref sink){
        return liveItemEndpoint(server,source)&&liveItemEndpoint(server,sink)
            &&routable(source,sink,new IdentityHashMap<>());
    }
    /** A provider handle is valid only while its exact loaded backing block remains attached. */
    static record ItemTarget(IItemHandler handler,ServerLevel level,BlockPos pos,BlockState state,BlockEntity entity){
        boolean live(MinecraftServer server,NetworkEngine.Ref ref){
            if(!liveItemEndpoint(server,ref))return false;
            Part.Link link=ref.adjacent();
            return NetworkEngine.level(server,link)==level&&link.pos().equals(pos)
                &&level.hasChunkAt(pos)&&level.getBlockState(pos)==state&&level.getBlockEntity(pos)==entity;
        }
    }
    /** Capture before capability lookup and detect target replacement during provider discovery. */
    static ItemTarget itemTarget(MinecraftServer server,NetworkEngine.Ref ref){
        if(!liveItemEndpoint(server,ref))return null;
        Part.Link link=ref.adjacent();ServerLevel level=NetworkEngine.level(server,link);
        if(level==null||!level.hasChunkAt(link.pos()))return null;
        BlockState state=level.getBlockState(link.pos());BlockEntity entity=level.getBlockEntity(link.pos());
        IItemHandler handler=itemHandler(server,ref);
        if(handler==null)return null;
        ItemTarget result=new ItemTarget(handler,level,link.pos(),state,entity);
        return result.live(server,ref)?result:null;
    }
    /** Exact live backing identity, not just a coordinate or a cached capability handle. */
    static record ResourceBacking(ServerLevel level,BlockPos pos,BlockState state,BlockEntity entity){
        boolean live(MinecraftServer server,NetworkEngine.Ref ref){
            if(!liveItemEndpoint(server,ref))return false;
            Part.Link link=ref.adjacent();
            return NetworkEngine.level(server,link)==level&&link.pos().equals(pos)
                &&level.hasChunkAt(pos)&&level.getBlockState(pos)==state&&level.getBlockEntity(pos)==entity;
        }
    }
    static ResourceBacking resourceBacking(MinecraftServer server,NetworkEngine.Ref ref){
        if(!liveItemEndpoint(server,ref))return null;
        Part.Link link=ref.adjacent();ServerLevel level=NetworkEngine.level(server,link);
        if(level==null||!level.hasChunkAt(link.pos()))return null;
        return new ResourceBacking(level,link.pos(),level.getBlockState(link.pos()),level.getBlockEntity(link.pos()));
    }
    private record FluidTarget(IFluidHandler handler,ResourceBacking backing){
        boolean live(MinecraftServer server,NetworkEngine.Ref ref){return backing.live(server,ref);}
    }
    private record EnergyTarget(IEnergyStorage handler,ResourceBacking backing){
        boolean live(MinecraftServer server,NetworkEngine.Ref ref){return backing.live(server,ref);}
    }
    private static FluidTarget fluidTarget(MinecraftServer server,NetworkEngine.Ref ref){
        ResourceBacking backing=resourceBacking(server,ref);
        if(backing==null)return null;
        IFluidHandler handler=fluidHandler(server,ref);
        FluidTarget result=handler==null?null:new FluidTarget(handler,backing);
        return result!=null&&result.live(server,ref)?result:null;
    }
    private static EnergyTarget energyTarget(MinecraftServer server,NetworkEngine.Ref ref){
        ResourceBacking backing=resourceBacking(server,ref);
        if(backing==null)return null;
        IEnergyStorage handler=energyHandler(server,ref);
        EnergyTarget result=handler==null?null:new EnergyTarget(handler,backing);
        return result!=null&&result.live(server,ref)?result:null;
    }
    private static boolean itemAllowed(ItemStack stack,NetworkEngine.Ref ref,boolean input){return TransferFilters.items(stack,ref.part(),input);}
    private static boolean fluidAllowed(FluidStack stack,NetworkEngine.Ref ref,boolean input){return TransferFilters.fluids(stack,ref.part(),input);}
    private static boolean energyAllowed(NetworkEngine.Ref ref){return ref.part().kind!=Kind.TRANSFER_NODE||ref.part().energy;}

    // ------------------------------------------------------------ items
    private static void pushItems(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();if(ItemTransferSafety.blocked(p))return;boolean hadPending=!p.pendingItem.isEmpty();
        if(hadPending){flushItem(server,source,sinks,budgets,pairs,received);return;}
        if(received.contains(target(source)))return;
        ItemTarget from=itemTarget(server,source);if(from==null)return;
        int slots=from.handler().getSlots();if(slots<=0||!from.live(server,source))return;
        int sourceBudget=budgets.remaining(budgets.items,source,PLConfig.ITEM_RATE.get());if(sourceBudget<=0)return;
        for(int n=0;n<Math.min(slots,65536);n++){
            int slot=Math.floorMod((int)(p.ticks+n),slots);
            ItemStack trial=from.handler().extractItem(slot,sourceBudget,true);
            if(!from.live(server,source))return;
            if(!ItemTransferSafety.trial(trial,sourceBudget)||!itemAllowed(trial,source,false))continue;
            int space=itemSpace(server,trial,sinks,source,budgets,pairs);
            if(!from.live(server,source))return;
            if(space<=0)continue;
            int request=Math.min(space,trial.getCount());
            ItemStack extracted=ItemTransferSafety.extract(source,trial,request,
                ()->from.live(server,source),()->from.handler().extractItem(slot,request,false));
            if(extracted.isEmpty())continue;
            p.pendingItem=extracted;source.host().setChanged();budgets.use(budgets.items,source,extracted.getCount());p.ticks=slot+1L;
            flushItem(server,source,sinks,budgets,pairs,received);break;
        }
    }
    private static int itemSpace(MinecraftServer server,ItemStack stack,List<NetworkEngine.Ref> sinks,NetworkEngine.Ref source,Budgets budgets,Set<PairKey> pairs){
        int remaining=stack.getCount();
        for(NetworkEngine.Ref sink:sinks){
            if(remaining<=0)break;if(pairs.contains(pair(source,sink))||ItemTransferSafety.blocked(sink.part())||!itemAllowed(stack,sink,true)||!liveItemRoute(server,source,sink))continue;
            int budget=budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get());if(budget<=0)continue;
            ItemTarget to=itemTarget(server,sink);if(to==null||!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            int offered=Math.min(remaining,budget);ItemStack send=stack.copyWithCount(offered);ItemStack rest=ItemHandlerHelper.insertItemStacked(to.handler(),send,true);
            if(!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            remaining-=offered-ItemTransferSafety.remainder(send,rest);
        }
        return stack.getCount()-remaining;
    }
    private static void flushItem(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();if(ItemTransferSafety.blocked(p))return;
        for(NetworkEngine.Ref sink:sinks){
            if(p.pendingItem.isEmpty())break;if(pairs.contains(pair(source,sink))||ItemTransferSafety.blocked(sink.part())||!itemAllowed(p.pendingItem,sink,true)||!liveItemRoute(server,source,sink))continue;
            int budget=budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get());if(budget<=0)continue;
            ItemTarget to=itemTarget(server,sink);if(to==null||!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            int offered=Math.min(p.pendingItem.getCount(),budget);ItemStack send=p.pendingItem.copyWithCount(offered);
            ItemStack rest=ItemTransferSafety.insert(source,send,()->to.live(server,sink)&&liveItemRoute(server,source,sink),
                ()->ItemHandlerHelper.insertItemStacked(to.handler(),send,false));int accepted=offered-rest.getCount();
            if(accepted>0){p.pendingItem.shrink(accepted);budgets.use(budgets.items,sink,accepted);budgets.delivered(budgets.items,accepted);pairs.add(pair(source,sink));received.add(target(sink));source.host().setChanged();sink.host().setChanged();}
            ItemTransferSafety.completed(source);
        }
    }
    private static void pullItems(MinecraftServer server,NetworkEngine.Ref sink,List<NetworkEngine.Ref> sources,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=sink.part();if(ItemTransferSafety.blocked(p)||!liveItemEndpoint(server,sink))return;
        ItemTarget to=itemTarget(server,sink);if(to==null)return;
        boolean hadPending=!p.pendingItem.isEmpty();
        if(hadPending){
            int budget=budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get());int offered=Math.min(p.pendingItem.getCount(),budget);
            if(offered>0){ItemStack send=p.pendingItem.copyWithCount(offered);ItemStack rest=ItemTransferSafety.insert(sink,send,()->to.live(server,sink),
                    ()->ItemHandlerHelper.insertItemStacked(to.handler(),send,false));int accepted=offered-rest.getCount();if(accepted>0){p.pendingItem.shrink(accepted);budgets.use(budgets.items,sink,accepted);budgets.delivered(budgets.items,accepted);received.add(target(sink));sink.host().setChanged();}ItemTransferSafety.completed(sink);}
            return;
        }
        for(NetworkEngine.Ref source:sources){
            if(budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get())<=0)break;
            if(ItemTransferSafety.blocked(source.part())||received.contains(target(source))||pairs.contains(pair(source,sink))||!liveItemRoute(server,source,sink))continue;
            int sourceBudget=budgets.remaining(budgets.items,source,PLConfig.ITEM_RATE.get());int sinkBudget=budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get());int limit=Math.min(sourceBudget,sinkBudget);if(limit<=0)continue;
            ItemTarget from=itemTarget(server,source);if(from==null||!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            int slots=from.handler().getSlots();if(slots<=0||!from.live(server,source)||!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            for(int n=0;n<Math.min(slots,65536)&&limit>0;n++){
                int slot=Math.floorMod((int)(p.ticks+n),slots);ItemStack trial=from.handler().extractItem(slot,limit,true);
                if(!from.live(server,source)||!to.live(server,sink)||!liveItemRoute(server,source,sink))break;
                if(!ItemTransferSafety.trial(trial,limit)||!itemAllowed(trial,source,false)||!itemAllowed(trial,sink,true))continue;
                ItemStack rest=ItemHandlerHelper.insertItemStacked(to.handler(),trial.copy(),true);
                if(!from.live(server,source)||!to.live(server,sink)||!liveItemRoute(server,source,sink))break;
                int accepted=trial.getCount()-ItemTransferSafety.remainder(trial,rest);if(accepted<=0)continue;
                ItemStack extracted=ItemTransferSafety.extract(sink,trial,accepted,
                    ()->from.live(server,source)&&to.live(server,sink)&&liveItemRoute(server,source,sink),()->from.handler().extractItem(slot,accepted,false));
                if(extracted.isEmpty())continue;
                p.pendingItem=extracted.copy();sink.host().setChanged();source.host().setChanged();
                ItemStack offered=extracted.copyWithCount(Math.min(extracted.getCount(),budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get())));ItemStack restActual=ItemTransferSafety.insert(sink,offered,()->from.live(server,source)&&to.live(server,sink)&&liveItemRoute(server,source,sink),
                    ()->ItemHandlerHelper.insertItemStacked(to.handler(),offered,false));int inserted=offered.getCount()-restActual.getCount();p.pendingItem.shrink(inserted);ItemTransferSafety.completed(sink);
                budgets.use(budgets.items,source,extracted.getCount());budgets.use(budgets.items,sink,extracted.getCount());pairs.add(pair(source,sink));if(inserted>0){budgets.delivered(budgets.items,inserted);received.add(target(sink));}p.ticks=slot+1L;
                sink.host().setChanged();source.host().setChanged();break;
            }
            if(!p.pendingItem.isEmpty())break;
        }
    }

    // ------------------------------------------------------------ fluids
    private static void pushFluids(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();
        if(ResourceTransferSafety.blocked(p)||!liveItemEndpoint(server,source))return;
        if(!p.pendingFluid.isEmpty()){flushFluid(server,source,sinks,budgets,pairs,received);return;}
        if(received.contains(target(source)))return;
        FluidTarget from=fluidTarget(server,source);if(from==null)return;
        int sourceBudget=budgets.remaining(budgets.fluids,source,PLConfig.FLUID_RATE.get());
        if(sourceBudget<=0)return;
        int tanks=from.handler().getTanks();if(!from.live(server,source))return;
        for(int tank=0;tank<Math.min(tanks,65536);tank++){
            FluidStack shown=from.handler().getFluidInTank(tank);
            if(!from.live(server,source))return;
            if(shown.isEmpty()||!fluidAllowed(shown,source,false))continue;
            FluidStack request=shown.copy();request.setAmount(Math.min(request.getAmount(),sourceBudget));
            FluidStack trial=from.handler().drain(request,IFluidHandler.FluidAction.SIMULATE);
            if(!from.live(server,source))return;
            if(!ResourceTransferSafety.fluidTrial(request,trial,request.getAmount()))continue;
            int space=fluidSpace(server,trial,sinks,source,budgets,pairs);
            if(!from.live(server,source))return;
            if(space<=0)continue;
            FluidStack mutationRequest=trial.copy();mutationRequest.setAmount(Math.min(space,trial.getAmount()));
            FluidStack extracted=ResourceTransferSafety.extractFluid(source,mutationRequest,
                ()->from.live(server,source),()->from.handler().drain(mutationRequest,IFluidHandler.FluidAction.EXECUTE));
            if(extracted.isEmpty())continue;
            p.pendingFluid=extracted;budgets.use(budgets.fluids,source,extracted.getAmount());
            source.host().setChanged();p.ticks++;
            flushFluid(server,source,sinks,budgets,pairs,received);break;
        }
    }
    private static int fluidSpace(MinecraftServer server,FluidStack stack,List<NetworkEngine.Ref> sinks,NetworkEngine.Ref source,Budgets budgets,Set<PairKey> pairs){
        int remaining=stack.getAmount();
        for(NetworkEngine.Ref sink:sinks){
            if(remaining<=0)break;
            if(pairs.contains(pair(source,sink))||!fluidAllowed(stack,sink,true)||!liveItemRoute(server,source,sink))continue;
            int budget=budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get());if(budget<=0)continue;
            FluidTarget to=fluidTarget(server,sink);
            if(to==null||!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            FluidStack send=stack.copy();send.setAmount(Math.min(remaining,budget));
            int simulated=(int)ResourceTransferSafety.amount(to.handler().fill(send,IFluidHandler.FluidAction.SIMULATE),send.getAmount());
            if(!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            remaining-=simulated;
        }
        return stack.getAmount()-remaining;
    }
    private static void flushFluid(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();if(ResourceTransferSafety.blocked(p))return;
        for(NetworkEngine.Ref sink:sinks){
            if(p.pendingFluid.isEmpty())break;
            if(pairs.contains(pair(source,sink))||!fluidAllowed(p.pendingFluid,sink,true)||!liveItemRoute(server,source,sink))continue;
            int budget=budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get());if(budget<=0)continue;
            FluidTarget to=fluidTarget(server,sink);if(to==null||!liveItemRoute(server,source,sink))continue;
            FluidStack send=p.pendingFluid.copy();send.setAmount(Math.min(send.getAmount(),budget));
            int accepted=ResourceTransferSafety.insertFluid(source,send,
                ()->to.live(server,sink)&&liveItemRoute(server,source,sink),
                ()->to.handler().fill(send,IFluidHandler.FluidAction.EXECUTE));
            if(accepted>0){
                p.pendingFluid.shrink(accepted);budgets.use(budgets.fluids,sink,accepted);
                budgets.delivered(budgets.fluids,accepted);pairs.add(pair(source,sink));
                received.add(target(sink));source.host().setChanged();sink.host().setChanged();
            }
            ResourceTransferSafety.completed(source);
        }
    }
    private static void pullFluids(MinecraftServer server,NetworkEngine.Ref sink,List<NetworkEngine.Ref> sources,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=sink.part();if(ResourceTransferSafety.blocked(p)||!liveItemEndpoint(server,sink))return;
        FluidTarget to=fluidTarget(server,sink);if(to==null)return;
        if(!p.pendingFluid.isEmpty()){
            int budget=budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get());if(budget<=0)return;
            FluidStack send=p.pendingFluid.copy();send.setAmount(Math.min(send.getAmount(),budget));
            int accepted=ResourceTransferSafety.insertFluid(sink,send,()->to.live(server,sink),
                ()->to.handler().fill(send,IFluidHandler.FluidAction.EXECUTE));
            if(accepted>0){
                p.pendingFluid.shrink(accepted);budgets.use(budgets.fluids,sink,accepted);
                budgets.delivered(budgets.fluids,accepted);received.add(target(sink));sink.host().setChanged();
            }
            ResourceTransferSafety.completed(sink);return;
        }
        for(NetworkEngine.Ref source:sources){
            if(budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get())<=0)break;
            if(ResourceTransferSafety.blocked(source.part())||received.contains(target(source))
                ||pairs.contains(pair(source,sink))||!liveItemRoute(server,source,sink))continue;
            int limit=Math.min(budgets.remaining(budgets.fluids,source,PLConfig.FLUID_RATE.get()),
                budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get()));
            if(limit<=0)continue;
            FluidTarget from=fluidTarget(server,source);
            if(from==null||!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            int tanks=from.handler().getTanks();
            if(!from.live(server,source)||!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            for(int tank=0;tank<Math.min(tanks,65536);tank++){
                FluidStack shown=from.handler().getFluidInTank(tank);
                if(!from.live(server,source)||!to.live(server,sink)||!liveItemRoute(server,source,sink))break;
                if(shown.isEmpty()||!fluidAllowed(shown,source,false)||!fluidAllowed(shown,sink,true))continue;
                FluidStack request=shown.copy();request.setAmount(Math.min(request.getAmount(),limit));
                FluidStack trial=from.handler().drain(request,IFluidHandler.FluidAction.SIMULATE);
                if(!from.live(server,source)||!to.live(server,sink)||!liveItemRoute(server,source,sink))break;
                if(!ResourceTransferSafety.fluidTrial(request,trial,request.getAmount()))continue;
                int accepted=(int)ResourceTransferSafety.amount(
                    to.handler().fill(trial.copy(),IFluidHandler.FluidAction.SIMULATE),trial.getAmount());
                if(!from.live(server,source)||!to.live(server,sink)||!liveItemRoute(server,source,sink))break;
                if(accepted<=0)continue;
                FluidStack mutationRequest=trial.copy();mutationRequest.setAmount(accepted);
                FluidStack extracted=ResourceTransferSafety.extractFluid(sink,mutationRequest,
                    ()->from.live(server,source)&&to.live(server,sink)&&liveItemRoute(server,source,sink),
                    ()->from.handler().drain(mutationRequest,IFluidHandler.FluidAction.EXECUTE));
                if(extracted.isEmpty())continue;
                p.pendingFluid=extracted.copy();sink.host().setChanged();source.host().setChanged();
                FluidStack offered=extracted.copy();
                offered.setAmount(Math.min(offered.getAmount(),budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get())));
                int inserted=ResourceTransferSafety.insertFluid(sink,offered,
                    ()->from.live(server,source)&&to.live(server,sink)&&liveItemRoute(server,source,sink),
                    ()->to.handler().fill(offered,IFluidHandler.FluidAction.EXECUTE));
                budgets.use(budgets.fluids,source,extracted.getAmount());
                budgets.use(budgets.fluids,sink,extracted.getAmount());pairs.add(pair(source,sink));
                if(inserted>0){budgets.delivered(budgets.fluids,inserted);received.add(target(sink));}
                p.pendingFluid.shrink(inserted);ResourceTransferSafety.completed(sink);
                sink.host().setChanged();source.host().setChanged();p.ticks++;break;
            }
            if(!p.pendingFluid.isEmpty())break;
        }
    }

    // ------------------------------------------------------------ FE
    private static void pushEnergy(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();if(ResourceTransferSafety.blocked(p)||!liveItemEndpoint(server,source))return;
        if(p.pendingEnergy>0){flushEnergy(server,source,sinks,budgets,pairs,received);return;}
        if(received.contains(target(source)))return;
        EnergyTarget from=energyTarget(server,source);if(from==null)return;
        int sourceBudget=budgets.remaining(budgets.energy,source,PLConfig.ENERGY_RATE.get());
        int trial=(int)ResourceTransferSafety.amount(from.handler().extractEnergy(sourceBudget,true),sourceBudget);
        if(!from.live(server,source))return;
        if(trial<=0)return;
        int space=energySpace(server,trial,sinks,source,budgets,pairs);
        if(!from.live(server,source)||space<=0)return;
        int request=Math.min(trial,space);
        int extracted=ResourceTransferSafety.extractEnergy(source,request,()->from.live(server,source),
            ()->from.handler().extractEnergy(request,false));
        if(extracted<=0)return;
        p.pendingEnergy=extracted;budgets.use(budgets.energy,source,extracted);
        source.host().setChanged();p.ticks++;flushEnergy(server,source,sinks,budgets,pairs,received);
    }
    private static int energySpace(MinecraftServer server,int amount,List<NetworkEngine.Ref> sinks,NetworkEngine.Ref source,Budgets budgets,Set<PairKey> pairs){
        int remaining=amount;
        for(NetworkEngine.Ref sink:sinks){
            if(remaining<=0)break;
            if(pairs.contains(pair(source,sink))||!energyAllowed(sink)||!liveItemRoute(server,source,sink))continue;
            int budget=budgets.remaining(budgets.energy,sink,PLConfig.ENERGY_RATE.get());if(budget<=0)continue;
            EnergyTarget to=energyTarget(server,sink);if(to==null||!liveItemRoute(server,source,sink))continue;
            int offered=Math.min(remaining,budget);
            int accepted=(int)ResourceTransferSafety.amount(to.handler().receiveEnergy(offered,true),offered);
            if(!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            remaining-=accepted;
        }
        return amount-remaining;
    }
    private static void flushEnergy(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();if(ResourceTransferSafety.blocked(p))return;
        for(NetworkEngine.Ref sink:sinks){
            if(p.pendingEnergy<=0)break;
            if(pairs.contains(pair(source,sink))||!energyAllowed(sink)||!liveItemRoute(server,source,sink))continue;
            int budget=budgets.remaining(budgets.energy,sink,PLConfig.ENERGY_RATE.get());if(budget<=0)continue;
            EnergyTarget to=energyTarget(server,sink);if(to==null)continue;
            int offered=Math.min(p.pendingEnergy,budget);
            int accepted=ResourceTransferSafety.insertEnergy(source,offered,
                ()->to.live(server,sink)&&liveItemRoute(server,source,sink),
                ()->to.handler().receiveEnergy(offered,false));
            if(accepted>0){
                p.pendingEnergy-=accepted;budgets.use(budgets.energy,sink,accepted);
                budgets.delivered(budgets.energy,accepted);pairs.add(pair(source,sink));
                received.add(target(sink));source.host().setChanged();sink.host().setChanged();
            }
            ResourceTransferSafety.completed(source);
        }
    }
    private static void pullEnergy(MinecraftServer server,NetworkEngine.Ref sink,List<NetworkEngine.Ref> sources,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=sink.part();if(ResourceTransferSafety.blocked(p)||!liveItemEndpoint(server,sink))return;
        EnergyTarget to=energyTarget(server,sink);if(to==null)return;
        if(p.pendingEnergy>0){
            int budget=budgets.remaining(budgets.energy,sink,PLConfig.ENERGY_RATE.get());if(budget<=0)return;
            int offered=Math.min(p.pendingEnergy,budget);
            int accepted=ResourceTransferSafety.insertEnergy(sink,offered,()->to.live(server,sink),
                ()->to.handler().receiveEnergy(offered,false));
            if(accepted>0){
                p.pendingEnergy-=accepted;budgets.use(budgets.energy,sink,accepted);
                budgets.delivered(budgets.energy,accepted);received.add(target(sink));sink.host().setChanged();
            }
            ResourceTransferSafety.completed(sink);return;
        }
        for(NetworkEngine.Ref source:sources){
            int sinkBudget=budgets.remaining(budgets.energy,sink,PLConfig.ENERGY_RATE.get());if(sinkBudget<=0)break;
            if(ResourceTransferSafety.blocked(source.part())||received.contains(target(source))
                ||pairs.contains(pair(source,sink))||!energyAllowed(source)||!liveItemRoute(server,source,sink))continue;
            int limit=Math.min(sinkBudget,budgets.remaining(budgets.energy,source,PLConfig.ENERGY_RATE.get()));
            if(limit<=0)continue;
            EnergyTarget from=energyTarget(server,source);
            if(from==null||!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            int trial=(int)ResourceTransferSafety.amount(from.handler().extractEnergy(limit,true),limit);
            if(!from.live(server,source)||!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            if(trial<=0)continue;
            int accepted=(int)ResourceTransferSafety.amount(to.handler().receiveEnergy(trial,true),trial);
            if(!from.live(server,source)||!to.live(server,sink)||!liveItemRoute(server,source,sink))continue;
            if(accepted<=0)continue;
            int extracted=ResourceTransferSafety.extractEnergy(sink,accepted,
                ()->from.live(server,source)&&to.live(server,sink)&&liveItemRoute(server,source,sink),
                ()->from.handler().extractEnergy(accepted,false));
            if(extracted<=0)continue;
            p.pendingEnergy=extracted;sink.host().setChanged();source.host().setChanged();
            int offered=Math.min(extracted,budgets.remaining(budgets.energy,sink,PLConfig.ENERGY_RATE.get()));
            int inserted=ResourceTransferSafety.insertEnergy(sink,offered,
                ()->from.live(server,source)&&to.live(server,sink)&&liveItemRoute(server,source,sink),
                ()->to.handler().receiveEnergy(offered,false));
            budgets.use(budgets.energy,source,extracted);
            budgets.use(budgets.energy,sink,extracted);pairs.add(pair(source,sink));
            if(inserted>0){budgets.delivered(budgets.energy,inserted);received.add(target(sink));}
            p.pendingEnergy=extracted-inserted;ResourceTransferSafety.completed(sink);
            sink.host().setChanged();source.host().setChanged();p.ticks++;
        }
    }

    private static void updateStatus(NetworkEngine.Ref ref,List<NetworkEngine.Ref> endpoints,Map<NetworkEngine.Ref,TargetKey> targets){
        Part p=ref.part();if(ResourceTransferSafety.blocked(p)){p.status="Resource transfer quarantined; manual inspection required";return;}if(ItemTransferSafety.blocked(p)){p.status="Item transfer quarantined; manual inspection required";return;}String mode=switch(p.transferMode){case 1->"ADD";case 2->"REMOVE";case 3->"ADD / REMOVE";default->"PASSIVE";};
        if(p.transferMode==TransferRules.PASSIVE){p.status="Transfer PASSIVE";return;}
        if(p.transferMode==TransferRules.ADD_REMOVE){long peers=endpoints.stream().filter(other->other.part().kind==Kind.TRANSFER_NODE&&(routable(ref,other,targets)||routable(other,ref,targets))).count();p.status=peers>0?"Transfer ADD / REMOVE: "+peers+" explicit peer(s)":"Transfer ADD / REMOVE: needs matching ADD or REMOVE peer";return;}
        long count=p.transferMode==TransferRules.ADD?endpoints.stream().filter(source->routable(source,ref,targets)).count():endpoints.stream().filter(sink->routable(ref,sink,targets)).count();
        p.status="Transfer "+mode+": "+count+" network endpoint(s)";
    }

    private static IItemHandler itemHandler(MinecraftServer s,NetworkEngine.Ref r){Part.Link l=r.adjacent();return NetworkEngine.loaded(s,l)?NetworkEngine.level(s,l).getCapability(Capabilities.ItemHandler.BLOCK,l.pos(),l.side()):null;}
    private static IFluidHandler fluidHandler(MinecraftServer s,NetworkEngine.Ref r){Part.Link l=r.adjacent();return NetworkEngine.loaded(s,l)?NetworkEngine.level(s,l).getCapability(Capabilities.FluidHandler.BLOCK,l.pos(),l.side()):null;}
    private static IEnergyStorage energyHandler(MinecraftServer s,NetworkEngine.Ref r){Part.Link l=r.adjacent();return NetworkEngine.loaded(s,l)?NetworkEngine.level(s,l).getCapability(Capabilities.EnergyStorage.BLOCK,l.pos(),l.side()):null;}
    private TransferEngine(){}
}
