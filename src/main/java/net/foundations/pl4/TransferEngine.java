package net.foundations.pl4;

import java.util.*;
import net.foundations.pl4.core.TransferRules;
import net.minecraft.util.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.ItemStack;
import net.foundations.pl4.compat.Capabilities;
import net.minecraftforge.items.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.energy.IEnergyStorage;

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
        List<NetworkEngine.Ref> endpoints=plan.endpoints(),transfer=plan.nextDrivers();
        if(transfer.isEmpty())return;
        boolean nativeEnergy=NativeEnergyTransfers.needed(plan);

        Budgets budgets=new Budgets();
        Set<PairKey> itemPairs=new HashSet<>(),fluidPairs=new HashSet<>(),energyPairs=new HashSet<>();
        Set<TargetKey> itemReceived=new HashSet<>(),fluidReceived=new HashSet<>(),energyReceived=new HashSet<>();

        // Preserve R15 source-side escrow from ADD/REMOVE nodes even though R16 no longer lets
        // bidirectional nodes autonomously drive the passive endpoint pool.
        for(NetworkEngine.Ref ref:transfer)if(ref.part().transferMode==TransferRules.ADD_REMOVE&&hasPending(ref.part())){
            List<NetworkEngine.Ref> sinks=sinks(endpoints,ref,plan.routingCursor());
            if(ref.part().items&&!ref.part().pendingItem.isEmpty())flushItem(server,ref,sinks,budgets,itemPairs,itemReceived);
            if(ref.part().fluids&&!ref.part().pendingFluid.isEmpty())flushFluid(server,ref,sinks,budgets,fluidPairs,fluidReceived);
            if(!nativeEnergy&&ref.part().energy&&ref.part().pendingEnergy>0)flushEnergy(server,ref,sinks,budgets,energyPairs,energyReceived);
        }

        // REMOVE drives exports. This phase keeps the old REMOVE -> ADD behavior and also
        // allows REMOVE -> ordinary Node endpoints.
        for(NetworkEngine.Ref source:transfer)if(TransferRules.drivesRemove(source.part().transferMode)){
            List<NetworkEngine.Ref> sinks=sinks(endpoints,source,plan.routingCursor());
            if(source.part().items)pushItems(server,source,sinks,budgets,itemPairs,itemReceived);
            if(source.part().fluids)pushFluids(server,source,sinks,budgets,fluidPairs,fluidReceived);
            if(!nativeEnergy&&source.part().energy)pushEnergy(server,source,sinks,budgets,energyPairs,energyReceived);
        }

        // ADD drives imports. Explicit REMOVE peers already handled above are skipped by the
        // pair ledger; passive Nodes now work as PL2-style network sources.
        for(NetworkEngine.Ref sink:transfer)if(TransferRules.drivesAdd(sink.part().transferMode)){
            List<NetworkEngine.Ref> sources=sources(endpoints,sink,plan.routingCursor());
            if(sink.part().items)pullItems(server,sink,sources,budgets,itemPairs,itemReceived);
            if(sink.part().fluids)pullFluids(server,sink,sources,budgets,fluidPairs,fluidReceived);
            if(!nativeEnergy&&sink.part().energy)pullEnergy(server,sink,sources,budgets,energyPairs,energyReceived);
        }

        if(nativeEnergy){var energyPlan=new Plan(endpoints,transfer);energyPlan.cycle=plan.cycle;NativeEnergyTransfers.run(server,energyPlan);}
        for(NetworkEngine.Ref ref:transfer){updateStatus(ref,endpoints);if(nativeEnergy&&!ref.part().energyTransferStatus.isEmpty())ref.part().status+="; "+ref.part().energyTransferStatus;}
    }

    private static boolean hasPending(Part p){return !p.pendingItem.isEmpty()||!p.pendingFluid.isEmpty()||p.energyCredits()>0;}
    private static Comparator<NetworkEngine.Ref> driverOrder(){
        return Comparator.<NetworkEngine.Ref>comparingInt(r->r.part().priority).reversed()
            .thenComparing(r->r.level().dimension.getType().getRegistryName().toString())
            .thenComparingLong(r->r.host().getBlockPos().asLong()).thenComparingInt(r->r.part().slot());
    }
    private static Comparator<NetworkEngine.Ref> endpointOrder(){
        return Comparator.<NetworkEngine.Ref>comparingInt(r->TransferRules.endpointClass(r.part().kind==Kind.TRANSFER_NODE)).reversed()
            .thenComparing(Comparator.<NetworkEngine.Ref>comparingInt(r->r.part().kind==Kind.TRANSFER_NODE?r.part().priority:0).reversed())
            .thenComparing(r->r.level().dimension.getType().getRegistryName().toString())
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
        return rotateTies(endpoints.stream().filter(s->s!=source&&!sameTarget(source,s)&&TransferRules.channelsMatch(source.part().outputChannel,s.part().inputChannel)&&TransferRules.canRoute(true,source.part().transferMode,s.part().kind==Kind.TRANSFER_NODE,s.part().transferMode)).toList(),cursor);
    }
    static List<NetworkEngine.Ref> sources(List<NetworkEngine.Ref> endpoints,NetworkEngine.Ref sink,long cursor){
        return rotateTies(endpoints.stream().filter(s->s!=sink&&!sameTarget(s,sink)&&TransferRules.channelsMatch(s.part().outputChannel,sink.part().inputChannel)&&TransferRules.canRoute(s.part().kind==Kind.TRANSFER_NODE,s.part().transferMode,true,sink.part().transferMode)).toList(),cursor);
    }
    private static PairKey pair(NetworkEngine.Ref source,NetworkEngine.Ref sink){return new PairKey(source.part().identity,sink.part().identity);}
    private static TargetKey target(NetworkEngine.Ref ref){Part.Link l=ref.adjacent();return new TargetKey(l.dimension(),l.pos().asLong(),l.side().ordinal());}
    private static boolean sameTarget(NetworkEngine.Ref a,NetworkEngine.Ref b){return target(a).equals(target(b));}
    private static boolean itemAllowed(ItemStack stack,NetworkEngine.Ref ref,boolean input){return TransferFilters.items(stack,ref.part(),input);}
    private static boolean fluidAllowed(FluidStack stack,NetworkEngine.Ref ref,boolean input){return TransferFilters.fluids(stack,ref.part(),input);}
    private static boolean energyAllowed(NetworkEngine.Ref ref){return ref.part().kind!=Kind.TRANSFER_NODE||ref.part().energy;}

    // ------------------------------------------------------------ items
    private static void pushItems(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();boolean hadPending=!p.pendingItem.isEmpty();
        if(hadPending){flushItem(server,source,sinks,budgets,pairs,received);return;}
        if(received.contains(target(source)))return;
        IItemHandler from=itemHandler(server,source);if(from==null||from.getSlots()<=0)return;
        int sourceBudget=budgets.remaining(budgets.items,source,PLConfig.ITEM_RATE.get());if(sourceBudget<=0)return;
        for(int n=0;n<Math.min(from.getSlots(),65536);n++){
            int slot=Math.floorMod((int)(p.ticks+n),from.getSlots());
            ItemStack trial=from.extractItem(slot,sourceBudget,true);if(trial.isEmpty()||!itemAllowed(trial,source,false))continue;
            int space=itemSpace(server,trial,sinks,source,budgets,pairs);if(space<=0)continue;
            ItemStack extracted=from.extractItem(slot,Math.min(space,trial.getCount()),false);if(extracted.isEmpty())continue;
            p.pendingItem=extracted;source.host().setChanged();budgets.use(budgets.items,source,extracted.getCount());p.ticks=slot+1L;
            flushItem(server,source,sinks,budgets,pairs,received);break;
        }
    }
    private static int itemSpace(MinecraftServer server,ItemStack stack,List<NetworkEngine.Ref> sinks,NetworkEngine.Ref source,Budgets budgets,Set<PairKey> pairs){
        int remaining=stack.getCount();
        for(NetworkEngine.Ref sink:sinks){
            if(remaining<=0)break;if(pairs.contains(pair(source,sink))||!itemAllowed(stack,sink,true))continue;
            int budget=budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get());if(budget<=0)continue;
            IItemHandler to=itemHandler(server,sink);if(to==null)continue;
            int offered=Math.min(remaining,budget);ItemStack send=net.foundations.pl4.compat.PortData.copyWithCount(stack,offered);ItemStack rest=ItemHandlerHelper.insertItemStacked(to,send,true);remaining-=offered-rest.getCount();
        }
        return stack.getCount()-remaining;
    }
    private static void flushItem(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();
        for(NetworkEngine.Ref sink:sinks){
            if(p.pendingItem.isEmpty())break;if(pairs.contains(pair(source,sink))||!itemAllowed(p.pendingItem,sink,true))continue;
            int budget=budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get());if(budget<=0)continue;
            IItemHandler to=itemHandler(server,sink);if(to==null)continue;
            int offered=Math.min(p.pendingItem.getCount(),budget);ItemStack send=net.foundations.pl4.compat.PortData.copyWithCount(p.pendingItem,offered);ItemStack rest=ItemHandlerHelper.insertItemStacked(to,send,false);int accepted=offered-rest.getCount();
            if(accepted>0){p.pendingItem.shrink(accepted);budgets.use(budgets.items,sink,accepted);budgets.delivered(budgets.items,accepted);pairs.add(pair(source,sink));received.add(target(sink));source.host().setChanged();sink.host().setChanged();}
        }
    }
    private static void pullItems(MinecraftServer server,NetworkEngine.Ref sink,List<NetworkEngine.Ref> sources,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=sink.part();IItemHandler to=itemHandler(server,sink);if(to==null)return;
        boolean hadPending=!p.pendingItem.isEmpty();
        if(hadPending){
            int budget=budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get());int offered=Math.min(p.pendingItem.getCount(),budget);
            if(offered>0){ItemStack rest=ItemHandlerHelper.insertItemStacked(to,net.foundations.pl4.compat.PortData.copyWithCount(p.pendingItem,offered),false);int accepted=offered-rest.getCount();if(accepted>0){p.pendingItem.shrink(accepted);budgets.use(budgets.items,sink,accepted);budgets.delivered(budgets.items,accepted);received.add(target(sink));sink.host().setChanged();}}
            return;
        }
        for(NetworkEngine.Ref source:sources){
            if(budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get())<=0)break;if(received.contains(target(source))||pairs.contains(pair(source,sink)))continue;
            int sourceBudget=budgets.remaining(budgets.items,source,PLConfig.ITEM_RATE.get());int sinkBudget=budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get());int limit=Math.min(sourceBudget,sinkBudget);if(limit<=0)continue;
            IItemHandler from=itemHandler(server,source);if(from==null||from.getSlots()<=0)continue;
            for(int n=0;n<Math.min(from.getSlots(),65536)&&limit>0;n++){
                int slot=Math.floorMod((int)(p.ticks+n),from.getSlots());ItemStack trial=from.extractItem(slot,limit,true);if(trial.isEmpty()||!itemAllowed(trial,source,false)||!itemAllowed(trial,sink,true))continue;
                ItemStack rest=ItemHandlerHelper.insertItemStacked(to,trial.copy(),true);int accepted=trial.getCount()-rest.getCount();if(accepted<=0)continue;
                ItemStack extracted=from.extractItem(slot,accepted,false);if(extracted.isEmpty())continue;
                p.pendingItem=extracted.copy();sink.host().setChanged();source.host().setChanged();
                ItemStack offered=net.foundations.pl4.compat.PortData.copyWithCount(extracted,Math.min(extracted.getCount(),budgets.remaining(budgets.items,sink,PLConfig.ITEM_RATE.get())));ItemStack restActual=ItemHandlerHelper.insertItemStacked(to,offered,false);int inserted=offered.getCount()-restActual.getCount();p.pendingItem.shrink(inserted);
                budgets.use(budgets.items,source,extracted.getCount());budgets.use(budgets.items,sink,extracted.getCount());pairs.add(pair(source,sink));if(inserted>0){budgets.delivered(budgets.items,inserted);received.add(target(sink));}p.ticks=slot+1L;
                sink.host().setChanged();source.host().setChanged();break;
            }
            if(!p.pendingItem.isEmpty())break;
        }
    }

    // ------------------------------------------------------------ fluids
    private static void pushFluids(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();boolean hadPending=!p.pendingFluid.isEmpty();if(hadPending){flushFluid(server,source,sinks,budgets,pairs,received);return;}if(received.contains(target(source)))return;
        IFluidHandler from=fluidHandler(server,source);if(from==null)return;int sourceBudget=budgets.remaining(budgets.fluids,source,PLConfig.FLUID_RATE.get());if(sourceBudget<=0)return;
        for(int tank=0;tank<Math.min(from.getTanks(),65536);tank++){
            FluidStack shown=from.getFluidInTank(tank);if(shown.isEmpty()||!fluidAllowed(shown,source,false))continue;FluidStack request=shown.copy();request.setAmount(Math.min(request.getAmount(),sourceBudget));
            FluidStack trial=from.drain(request,IFluidHandler.FluidAction.SIMULATE);if(trial.isEmpty())continue;int space=fluidSpace(server,trial,sinks,source,budgets,pairs);if(space<=0)continue;request=trial.copy();request.setAmount(Math.min(space,trial.getAmount()));
            FluidStack extracted=from.drain(request,IFluidHandler.FluidAction.EXECUTE);if(extracted.isEmpty())continue;p.pendingFluid=extracted;budgets.use(budgets.fluids,source,extracted.getAmount());source.host().setChanged();p.ticks++;
            flushFluid(server,source,sinks,budgets,pairs,received);break;
        }
    }
    private static int fluidSpace(MinecraftServer server,FluidStack stack,List<NetworkEngine.Ref> sinks,NetworkEngine.Ref source,Budgets budgets,Set<PairKey> pairs){
        int remaining=stack.getAmount();for(NetworkEngine.Ref sink:sinks){if(remaining<=0)break;if(pairs.contains(pair(source,sink))||!fluidAllowed(stack,sink,true))continue;int budget=budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get());if(budget<=0)continue;IFluidHandler to=fluidHandler(server,sink);if(to==null)continue;FluidStack send=stack.copy();send.setAmount(Math.min(remaining,budget));remaining-=net.foundations.pl4.compat.PortMath.clamp(to.fill(send,IFluidHandler.FluidAction.SIMULATE),0,send.getAmount());}return stack.getAmount()-remaining;
    }
    private static void flushFluid(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();for(NetworkEngine.Ref sink:sinks){if(p.pendingFluid.isEmpty())break;if(pairs.contains(pair(source,sink))||!fluidAllowed(p.pendingFluid,sink,true))continue;int budget=budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get());if(budget<=0)continue;IFluidHandler to=fluidHandler(server,sink);if(to==null)continue;FluidStack send=p.pendingFluid.copy();send.setAmount(Math.min(send.getAmount(),budget));int accepted=net.foundations.pl4.compat.PortMath.clamp(to.fill(send,IFluidHandler.FluidAction.EXECUTE),0,send.getAmount());if(accepted>0){p.pendingFluid.shrink(accepted);budgets.use(budgets.fluids,sink,accepted);budgets.delivered(budgets.fluids,accepted);pairs.add(pair(source,sink));received.add(target(sink));source.host().setChanged();sink.host().setChanged();}}
    }
    private static void pullFluids(MinecraftServer server,NetworkEngine.Ref sink,List<NetworkEngine.Ref> sources,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=sink.part();IFluidHandler to=fluidHandler(server,sink);if(to==null)return;boolean hadPending=!p.pendingFluid.isEmpty();if(hadPending){int budget=budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get());FluidStack send=p.pendingFluid.copy();send.setAmount(Math.min(send.getAmount(),budget));int accepted=net.foundations.pl4.compat.PortMath.clamp(to.fill(send,IFluidHandler.FluidAction.EXECUTE),0,send.getAmount());if(accepted>0){p.pendingFluid.shrink(accepted);budgets.use(budgets.fluids,sink,accepted);budgets.delivered(budgets.fluids,accepted);received.add(target(sink));sink.host().setChanged();}return;}
        for(NetworkEngine.Ref source:sources){if(budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get())<=0)break;if(received.contains(target(source))||pairs.contains(pair(source,sink)))continue;int limit=Math.min(budgets.remaining(budgets.fluids,source,PLConfig.FLUID_RATE.get()),budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get()));if(limit<=0)continue;IFluidHandler from=fluidHandler(server,source);if(from==null)continue;
            for(int tank=0;tank<Math.min(from.getTanks(),65536);tank++){FluidStack shown=from.getFluidInTank(tank);if(shown.isEmpty()||!fluidAllowed(shown,source,false)||!fluidAllowed(shown,sink,true))continue;FluidStack request=shown.copy();request.setAmount(Math.min(request.getAmount(),limit));FluidStack trial=from.drain(request,IFluidHandler.FluidAction.SIMULATE);if(trial.isEmpty())continue;int accepted=net.foundations.pl4.compat.PortMath.clamp(to.fill(trial.copy(),IFluidHandler.FluidAction.SIMULATE),0,trial.getAmount());if(accepted<=0)continue;request=trial.copy();request.setAmount(accepted);FluidStack extracted=from.drain(request,IFluidHandler.FluidAction.EXECUTE);if(extracted.isEmpty())continue;p.pendingFluid=extracted.copy();sink.host().setChanged();source.host().setChanged();FluidStack offered=extracted.copy();offered.setAmount(Math.min(offered.getAmount(),budgets.remaining(budgets.fluids,sink,PLConfig.FLUID_RATE.get())));int inserted=net.foundations.pl4.compat.PortMath.clamp(to.fill(offered,IFluidHandler.FluidAction.EXECUTE),0,offered.getAmount());budgets.use(budgets.fluids,source,extracted.getAmount());budgets.use(budgets.fluids,sink,extracted.getAmount());pairs.add(pair(source,sink));if(inserted>0){budgets.delivered(budgets.fluids,inserted);received.add(target(sink));}p.pendingFluid.shrink(inserted);sink.host().setChanged();source.host().setChanged();p.ticks++;break;}
            if(!p.pendingFluid.isEmpty())break;
        }
    }

    // ------------------------------------------------------------ FE
    private static void pushEnergy(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();boolean hadPending=p.pendingEnergy>0;if(hadPending){flushEnergy(server,source,sinks,budgets,pairs,received);return;}if(received.contains(target(source)))return;IEnergyStorage from=energyHandler(server,source);if(from==null)return;int sourceBudget=budgets.remaining(budgets.energy,source,PLConfig.ENERGY_RATE.get());int trial=from.extractEnergy(sourceBudget,true);if(trial<=0)return;int space=energySpace(server,trial,sinks,source,budgets,pairs);if(space<=0)return;int extracted=Math.max(0,from.extractEnergy(Math.min(trial,space),false));if(extracted<=0)return;p.pendingEnergy=extracted;budgets.use(budgets.energy,source,extracted);source.host().setChanged();p.ticks++;flushEnergy(server,source,sinks,budgets,pairs,received);
    }
    private static int energySpace(MinecraftServer server,int amount,List<NetworkEngine.Ref> sinks,NetworkEngine.Ref source,Budgets budgets,Set<PairKey> pairs){
        int remaining=amount;for(NetworkEngine.Ref sink:sinks){if(remaining<=0)break;if(pairs.contains(pair(source,sink))||!energyAllowed(sink))continue;int budget=budgets.remaining(budgets.energy,sink,PLConfig.ENERGY_RATE.get());if(budget<=0)continue;IEnergyStorage to=energyHandler(server,sink);if(to==null)continue;int offered=Math.min(remaining,budget);remaining-=net.foundations.pl4.compat.PortMath.clamp(to.receiveEnergy(offered,true),0,offered);}return amount-remaining;
    }
    private static void flushEnergy(MinecraftServer server,NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=source.part();for(NetworkEngine.Ref sink:sinks){if(p.pendingEnergy<=0)break;if(pairs.contains(pair(source,sink))||!energyAllowed(sink))continue;int budget=budgets.remaining(budgets.energy,sink,PLConfig.ENERGY_RATE.get());if(budget<=0)continue;IEnergyStorage to=energyHandler(server,sink);if(to==null)continue;int offered=Math.min(p.pendingEnergy,budget);int accepted=net.foundations.pl4.compat.PortMath.clamp(to.receiveEnergy(offered,false),0,offered);if(accepted>0){p.pendingEnergy-=accepted;budgets.use(budgets.energy,sink,accepted);budgets.delivered(budgets.energy,accepted);pairs.add(pair(source,sink));received.add(target(sink));source.host().setChanged();sink.host().setChanged();}}
    }
    private static void pullEnergy(MinecraftServer server,NetworkEngine.Ref sink,List<NetworkEngine.Ref> sources,Budgets budgets,Set<PairKey> pairs,Set<TargetKey> received){
        Part p=sink.part();IEnergyStorage to=energyHandler(server,sink);if(to==null)return;boolean hadPending=p.pendingEnergy>0;if(hadPending){int budget=budgets.remaining(budgets.energy,sink,PLConfig.ENERGY_RATE.get());int offered=Math.min(p.pendingEnergy,budget);int accepted=net.foundations.pl4.compat.PortMath.clamp(to.receiveEnergy(offered,false),0,offered);if(accepted>0){p.pendingEnergy-=accepted;budgets.use(budgets.energy,sink,accepted);budgets.delivered(budgets.energy,accepted);received.add(target(sink));sink.host().setChanged();}return;}
        for(NetworkEngine.Ref source:sources){int sinkBudget=budgets.remaining(budgets.energy,sink,PLConfig.ENERGY_RATE.get());if(sinkBudget<=0)break;if(received.contains(target(source))||pairs.contains(pair(source,sink))||!energyAllowed(source))continue;int limit=Math.min(sinkBudget,budgets.remaining(budgets.energy,source,PLConfig.ENERGY_RATE.get()));if(limit<=0)continue;IEnergyStorage from=energyHandler(server,source);if(from==null)continue;int trial=from.extractEnergy(limit,true);if(trial<=0)continue;int accepted=net.foundations.pl4.compat.PortMath.clamp(to.receiveEnergy(trial,true),0,trial);if(accepted<=0)continue;int extracted=Math.max(0,from.extractEnergy(accepted,false));if(extracted<=0)continue;p.pendingEnergy=extracted;sink.host().setChanged();source.host().setChanged();int offered=Math.min(extracted,budgets.remaining(budgets.energy,sink,PLConfig.ENERGY_RATE.get()));int inserted=net.foundations.pl4.compat.PortMath.clamp(to.receiveEnergy(offered,false),0,offered);budgets.use(budgets.energy,source,extracted);budgets.use(budgets.energy,sink,extracted);pairs.add(pair(source,sink));if(inserted>0){budgets.delivered(budgets.energy,inserted);received.add(target(sink));}p.pendingEnergy=extracted-inserted;sink.host().setChanged();source.host().setChanged();p.ticks++;
        }
    }

    private static void updateStatus(NetworkEngine.Ref ref,List<NetworkEngine.Ref> endpoints){
        Part p=ref.part();String mode=switch(p.transferMode){case 1->"ADD";case 2->"REMOVE";case 3->"ADD / REMOVE";default->"PASSIVE";};
        if(p.transferMode==TransferRules.PASSIVE){p.status="Transfer PASSIVE";return;}
        if(p.transferMode==TransferRules.ADD_REMOVE){long peers=endpoints.stream().filter(other->other!=ref&&other.part().kind==Kind.TRANSFER_NODE&&!sameTarget(ref,other)&&(TransferRules.canRoute(true,p.transferMode,true,other.part().transferMode)||TransferRules.canRoute(true,other.part().transferMode,true,p.transferMode))).count();p.status=peers>0?"Transfer ADD / REMOVE: "+peers+" explicit peer(s)":"Transfer ADD / REMOVE: needs ADD or REMOVE peer";return;}
        long count=p.transferMode==TransferRules.ADD?endpoints.stream().filter(source->source!=ref&&!sameTarget(source,ref)&&TransferRules.canRoute(source.part().kind==Kind.TRANSFER_NODE,source.part().transferMode,true,p.transferMode)).count():endpoints.stream().filter(sink->sink!=ref&&!sameTarget(ref,sink)&&TransferRules.canRoute(true,p.transferMode,sink.part().kind==Kind.TRANSFER_NODE,sink.part().transferMode)).count();
        p.status="Transfer "+mode+": "+count+" network endpoint(s)";
    }

    private static IItemHandler itemHandler(MinecraftServer s,NetworkEngine.Ref r){Part.Link l=r.adjacent();return NetworkEngine.loaded(s,l)?net.foundations.pl4.compat.PortCapabilities.get(NetworkEngine.level(s,l),Capabilities.ItemHandler.BLOCK,l.pos(),l.side()):null;}
    private static IFluidHandler fluidHandler(MinecraftServer s,NetworkEngine.Ref r){Part.Link l=r.adjacent();return NetworkEngine.loaded(s,l)?net.foundations.pl4.compat.PortCapabilities.get(NetworkEngine.level(s,l),Capabilities.FluidHandler.BLOCK,l.pos(),l.side()):null;}
    private static IEnergyStorage energyHandler(MinecraftServer s,NetworkEngine.Ref r){Part.Link l=r.adjacent();return NetworkEngine.loaded(s,l)?net.foundations.pl4.compat.PortCapabilities.get(NetworkEngine.level(s,l),Capabilities.EnergyStorage.BLOCK,l.pos(),l.side()):null;}
    private TransferEngine(){}
}
