package net.foundations.pl4;

import java.util.*;
import net.foundations.pl4.core.EnergyConversion;
import net.foundations.pl4.core.TransferRules;
import net.minecraft.server.MinecraftServer;

/** Explicit native/conversion routes. Integer escrow is persisted on the driving node. */
final class NativeEnergyTransfers {
    private record Pair(UUID source,UUID sink){}
    private record Target(String dimension,long pos,int side){}
    private record PortKey(Target target,String unit){}
    private final MinecraftServer server;
    private final TransferEngine.Plan plan;
    private final java.util.function.BiFunction<NetworkEngine.Ref,String,EnergyConversion.Port> resolver;
    private final EnergyConversion.Rates rates=EnergyPorts.rates();
    private final Map<UUID,Integer> used=new HashMap<>();
    private final TransferRules.CycleLimit shared=new TransferRules.CycleLimit(PLConfig.NETWORK_ENERGY_RATE.get());
    private final Set<Pair> pairs=new HashSet<>();
    private final Set<Target> received=new HashSet<>();
    private final Map<PortKey,EnergyConversion.Port> ports=new HashMap<>();
    private NativeEnergyTransfers(MinecraftServer server,TransferEngine.Plan plan,java.util.function.BiFunction<NetworkEngine.Ref,String,EnergyConversion.Port> resolver){this.server=server;this.plan=plan;this.resolver=resolver;}
    static boolean needed(TransferEngine.Plan plan){return plan.endpoints().stream().anyMatch(r->!r.part().energyInput.equals("FE")||!r.part().energyOutput.equals("FE")||r.part().pendingEnergyCredits>0);}
    static void run(MinecraftServer server,TransferEngine.Plan plan){new NativeEnergyTransfers(server,plan,(ref,unit)->EnergyPorts.resolve(server,ref,unit)).run();}
    static void run(MinecraftServer server,TransferEngine.Plan plan,java.util.function.BiFunction<NetworkEngine.Ref,String,EnergyConversion.Port> resolver){new NativeEnergyTransfers(server,plan,resolver).run();}
    private static boolean driver(NetworkEngine.Ref r){return r.part().kind==Kind.TRANSFER_NODE;}
    private static String input(NetworkEngine.Ref r){return driver(r)?r.part().energyInput:"FE";}
    private static String output(NetworkEngine.Ref r){return driver(r)?r.part().energyOutput:"FE";}
    private static Target target(NetworkEngine.Ref r){var l=r.adjacent();return new Target(l.dimension(),l.pos().asLong(),l.side().ordinal());}
    private static Pair pair(NetworkEngine.Ref a,NetworkEngine.Ref b){return new Pair(a.part().identity,b.part().identity);}
    private boolean enabled(NetworkEngine.Ref r){
        Part p=r.part();return (!driver(r)||p.energy)&&EnergyPorts.enabled(input(r))&&EnergyPorts.enabled(output(r))&&(input(r).equals(output(r))||p.energyConvert&&PLConfig.ENERGY_CONVERSION.get());
    }
    private int remaining(NetworkEngine.Ref r){return Math.min(shared.remaining(),driver(r)?Math.max(0,PLConfig.ENERGY_RATE.get()-used.getOrDefault(r.part().identity,0)):Integer.MAX_VALUE);}
    private long extractionLimit(NetworkEngine.Ref r){return (long)(driver(r)?Math.max(0,PLConfig.ENERGY_RATE.get()-used.getOrDefault(r.part().identity,0)):Integer.MAX_VALUE)*EnergyConversion.FE;}
    private long limit(NetworkEngine.Ref r){return (long)remaining(r)*EnergyConversion.FE;}
    private void use(NetworkEngine.Ref r,long credits){if(driver(r))used.merge(r.part().identity,EnergyConversion.charge(credits),Integer::sum);}
    private EnergyConversion.Port port(NetworkEngine.Ref r,String unit){
        PortKey key=new PortKey(target(r),unit);if(!ports.containsKey(key))ports.put(key,resolver.apply(r,unit));return ports.get(key);
    }
    private int efficiency(NetworkEngine.Ref r){return rates.efficiency(input(r),output(r));}
    private boolean route(NetworkEngine.Ref source,NetworkEngine.Ref sink,String wireUnit){return enabled(source)&&enabled(sink)&&wireUnit.equals(input(sink))&&!pairs.contains(pair(source,sink));}
    private void run(){
        for(var ref:plan.drivers())ref.part().energyTransferStatus="";
        for(var ref:plan.drivers()){
            Part p=ref.part();
            if(!p.energy)continue;
            if(!enabled(ref)){p.energyTransferStatus="Energy route disabled by server";continue;}
            try{
                if(p.transferMode==TransferRules.ADD_REMOVE&&p.energyCredits()>0)flush(ref,TransferEngine.sinks(plan.endpoints(),ref,p.ticks));
                else if(TransferRules.drivesRemove(p.transferMode))push(ref,TransferEngine.sinks(plan.endpoints(),ref,p.ticks));
            }catch(RuntimeException failure){p.energyTransferStatus="Energy provider error; inspect server log";EnergyPorts.failure(input(ref),failure);}
        }
        for(var ref:plan.drivers())if(ref.part().energy&&TransferRules.drivesAdd(ref.part().transferMode)&&enabled(ref))try{
            pull(ref,TransferEngine.sources(plan.endpoints(),ref,ref.part().ticks));
        }catch(RuntimeException failure){ref.part().energyTransferStatus="Energy provider error; inspect server log";EnergyPorts.failure(output(ref),failure);}
        for(var ref:plan.drivers()){
            Part p=ref.part();if(!p.energy||!p.energyTransferStatus.isEmpty())continue;
            String attached=p.transferMode==TransferRules.ADD?output(ref):input(ref);
            try{
                if(port(ref,attached)==null)p.energyTransferStatus="No supported "+EnergyPorts.label(attached)+" capability on attached side";
                else p.energyTransferStatus="Energy "+EnergyPorts.label(input(ref))+" -> "+EnergyPorts.label(output(ref))+(p.energyCredits()>0?"; escrow awaiting compatible destination":"");
            }catch(RuntimeException failure){p.energyTransferStatus="Energy provider error; inspect server log";EnergyPorts.failure(attached,failure);}
        }
    }
    private boolean profile(NetworkEngine.Ref holder){
        Part p=holder.part();if(p.energyCredits()==0)return true;
        if(p.pendingEnergyJRate==0&&p.pendingEnergyEURate==0&&p.pendingEnergyUnit.equals("FE")){
            p.pendingEnergyJRate=rates.fePer1000J();p.pendingEnergyEURate=rates.fePerEU();p.pendingEnergyEDRate=rates.fePerElectrodynamicsJ();p.energyCredits(p.energyCredits());holder.host().setChanged();
        }
        if(p.pendingEnergyJRate!=rates.fePer1000J()||p.pendingEnergyEURate!=rates.fePerEU()||p.pendingEnergyEDRate!=rates.fePerElectrodynamicsJ()){
            p.energyTransferStatus="Escrow paused: restore saved energy conversion ratios";return false;
        }
        return true;
    }
    private void save(NetworkEngine.Ref holder,long credits,String wireUnit){
        Part p=holder.part();p.energyCredits(credits);p.pendingEnergyUnit=wireUnit;p.pendingEnergyJRate=rates.fePer1000J();p.pendingEnergyEURate=rates.fePerEU();p.pendingEnergyEDRate=rates.fePerElectrodynamicsJ();holder.host().setChanged();
    }
    private long extract(NetworkEngine.Ref source,long room,boolean simulate){
        var from=port(source,input(source));if(from==null)return 0;
        long units=EnergyConversion.sourceUnits(room,rates.cost(input(source)),efficiency(source));
        units=Math.min(units,EnergyConversion.extractable(extractionLimit(source),rates.cost(input(source))));if(units<=0)return 0;
        long actual=from.extract(units,simulate);if(actual<0||actual>units)throw new IllegalStateException("Native energy extraction exceeded request");
        if(!simulate)use(source,actual*rates.cost(input(source)));
        return EnergyConversion.credits(actual,rates.cost(input(source)),efficiency(source));
    }
    private long deliver(NetworkEngine.Ref sink,long available,boolean simulate){
        var to=port(sink,output(sink));if(to==null)return 0;
        return EnergyConversion.deliver(to,Math.min(available,limit(sink)),rates.cost(output(sink)),efficiency(sink),simulate);
    }
    private void record(NetworkEngine.Ref source,NetworkEngine.Ref sink,long credits){
        if(credits<=0)return;use(sink,credits);shared.delivered(EnergyConversion.charge(credits));pairs.add(pair(source,sink));received.add(target(sink));sink.host().setChanged();
    }
    private void push(NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks){
        Part p=source.part();
        if(p.energyCredits()>0){flush(source,sinks);if(!profile(source)||!p.pendingEnergyUnit.equals(output(source)))return;}
        if(received.contains(target(source))||shared.remaining()==0)return;
        long trial=extract(source,extractionLimit(source),true);if(trial==0)return;
        long existing=p.energyCredits(),total=Math.min(Math.min(existing,(long)Integer.MAX_VALUE*EnergyConversion.FE)+trial,(long)shared.remaining()*EnergyConversion.FE),remaining=total;
        for(var sink:sinks)if(remaining>0&&route(source,sink,output(source)))remaining-=deliver(sink,remaining,true);
        long room=total-remaining-existing;if(room<=0)return;
        long extracted=extract(source,room,false);if(extracted<=0)return;
        save(source,Math.addExact(existing,extracted),output(source));p.ticks++;flush(source,sinks);
    }
    private void flush(NetworkEngine.Ref source,List<NetworkEngine.Ref> sinks){
        if(!profile(source))return;Part p=source.part();
        for(var sink:sinks){
            if(p.energyCredits()==0)break;if(!route(source,sink,p.pendingEnergyUnit))continue;
            long accepted=deliver(sink,p.energyCredits(),false);if(accepted==0)continue;
            save(source,p.energyCredits()-accepted,p.pendingEnergyUnit);record(source,sink,accepted);
        }
    }
    private void pull(NetworkEngine.Ref sink,List<NetworkEngine.Ref> sources){
        Part p=sink.part();
        if(p.energyCredits()>0){
            if(!profile(sink)||!p.pendingEnergyUnit.equals(input(sink)))return;
            long accepted=deliver(sink,p.energyCredits(),false);
            if(accepted>0){save(sink,p.energyCredits()-accepted,p.pendingEnergyUnit);use(sink,accepted);shared.delivered(EnergyConversion.charge(accepted));received.add(target(sink));}
        }
        for(var source:sources){
            if(limit(sink)==0)break;if(source.part().energyCredits()>0||received.contains(target(source))||!route(source,sink,output(source)))continue;
            if(p.energyCredits()>0&&!p.pendingEnergyUnit.equals(output(source)))continue;
            long trial=extract(source,extractionLimit(source),true);if(trial==0)continue;
            long existing=p.energyCredits();long combined=Math.min(existing,(long)Integer.MAX_VALUE*EnergyConversion.FE)+trial;
            long room=deliver(sink,combined,true)-existing;if(room<=0)continue;
            long extracted=extract(source,room,false);if(extracted==0)continue;
            // Native quantum excess and fractional leftovers can be topped up after retries,
            // but only when simulation proves that combining them can deliver more energy.
            save(sink,Math.addExact(existing,extracted),output(source));p.ticks++;
            long accepted=deliver(sink,p.energyCredits(),false);
            if(accepted>0){save(sink,p.energyCredits()-accepted,p.pendingEnergyUnit);record(source,sink,accepted);}
            else pairs.add(pair(source,sink));
            source.host().setChanged();if(p.energyCredits()>0)break;
        }
    }
}
