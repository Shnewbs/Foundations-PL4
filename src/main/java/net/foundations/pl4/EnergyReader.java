package net.foundations.pl4;

import java.util.*;
import net.foundations.pl4.core.EnergyValues;
import net.foundations.pl4.core.ReflectiveEnergyAccess;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.*;
import org.slf4j.LoggerFactory;

/** Server-thread, sided, read-only energy telemetry. The native provider wins over its FE wrapper.
 * Missing/blocked APIs are not reported as an empty battery. All handler references are local to a sample.
 */
public final class EnergyReader {
    private record Target(String dimension,BlockPos pos){}
    private record Probe(String id,String unit,BlockCapability<Object,Direction> capability,ReflectiveEnergyAccess access){}
    private record Sample(Target target,String name,EnergyValues.Reading reading){}
    private static List<Probe> probes;
    private static final Set<String> warned=new HashSet<>(); // At most one entry per built-in provider, not per block.
    private static void warn(String provider,Exception failure){if(warned.add(provider))LoggerFactory.getLogger("FoundationsPL4").warn("Energy reader provider {} unavailable/failed; check API versions and node side. Subsequent identical provider warnings suppressed.",provider,failure);}
    public static void clear(){probes=null;warned.clear();OptionalPowerTelemetry.clear();}
    @SuppressWarnings("unchecked")
    private static List<Probe> probes(){
        if(probes!=null)return probes;
        Map<String,BlockCapability<?,?>> known=new HashMap<>();for(var cap:BlockCapability.getAll())known.put(cap.name().toString(),cap);
        List<Probe> found=new ArrayList<>();
        // Prefer GT's non-transfer telemetry interface, including BigInteger storage, before its energy container.
        for(String id:List.of("gtceu:energy_info_provider","gtceu:energy_container","mekanism:strict_energy_handler")){
            var cap=known.get(id);if(cap==null||cap.contextClass()!=Direction.class)continue;
            try{
                var access=switch(id){case "gtceu:energy_info_provider"->ReflectiveEnergyAccess.gregtechInfo(cap.typeClass());case "gtceu:energy_container"->ReflectiveEnergyAccess.gregtech(cap.typeClass());default->ReflectiveEnergyAccess.mekanism(cap.typeClass());};
                found.add(new Probe(id,id.startsWith("mekanism:")?"J":"EU",(BlockCapability<Object,Direction>)cap,access));
            }catch(ReflectiveOperationException|RuntimeException failure){warn(id,failure);}
        }
        probes=List.copyOf(found);return probes;
    }
    private static EnergyValues.Reading read(ServerLevel level,BlockPos pos,List<Direction> sides,Part reader,int[] failures){
        for(Probe probe:probes()){
            if(!EnergyValues.accepts(reader.energySystem,probe.unit))continue;
            if(probe.unit.equals("J")&&!PLConfig.MEKANISM_READS.get()||probe.unit.equals("EU")&&!PLConfig.GREGTECH_READS.get())continue;
            for(Direction side:sides)try{
                Object handler=level.getCapability(probe.capability,pos,side);
                var result=probe.access.read(handler,PLConfig.MAX_ENERGY_CONTAINERS.get());if(result!=null)return result;
            }catch(ReflectiveOperationException|RuntimeException failure){failures[0]++;warn(probe.id,failure);}
        }
        if(EnergyValues.accepts(reader.energySystem,"FE"))for(Direction side:sides)try{
            var handler=level.getCapability(Capabilities.EnergyStorage.BLOCK,pos,side);
            if(handler!=null)return new EnergyValues.Reading("neoforge","FE",handler.getEnergyStored(),handler.getMaxEnergyStored());
        }catch(RuntimeException failure){failures[0]++;warn("neoforge:energy",failure);}
        return null;
    }
    public static List<Part.Row> sample(MinecraftServer server,NetworkEngine.Ref ref,List<Part.Link> links){
        if(ref.part().energySystem.equals("CREATE")||ref.part().energySystem.equals("AE2"))return OptionalPowerTelemetry.sample(server,ref,links);
        Part reader=ref.part();Map<Target,List<Direction>> targets=new LinkedHashMap<>();
        for(Part.Link link:links)if(link.entity()==null){var key=new Target(link.dimension(),link.pos());var sides=targets.computeIfAbsent(key,k->new ArrayList<>());if(!sides.contains(link.side()))sides.add(link.side());}
        List<Sample> samples=new ArrayList<>();int unloaded=0,unsupported=0;int[] failures={0};
        for(var entry:targets.entrySet()){
            var key=entry.getKey();var level=NetworkEngine.level(server,new Part.Link(key.dimension,key.pos,Direction.DOWN,null,null));
            if(level==null||!level.hasChunkAt(key.pos)){unloaded++;continue;}
            var value=read(level,key.pos,entry.getValue(),reader,failures);
            if(value==null){unsupported++;continue;}
            samples.add(new Sample(key,level.getBlockState(key.pos).getBlock().getName().getString(),value));
        }
        List<Part.Row> result=new ArrayList<>();
        try{
            for(var total:EnergyValues.totals(samples.stream().map(Sample::reading).toList()).values())
                result.add(new Part.Row(EnergyValues.totalKey(total.unit()),"Storage ("+total.unit()+")",total.stored(),total.capacity(),total.unit()));
        }catch(IllegalArgumentException failure){reader.status="Energy total exceeds telemetry range";return List.of();}
        if(!reader.mode.equals("STORAGE"))for(var sample:samples){
            var value=sample.reading;String location=sample.target.dimension+":"+sample.target.pos.toShortString();
            String key=value.unit().equals("FE")?location:"energy:"+value.unit().toLowerCase(Locale.ROOT)+":"+location;
            result.add(new Part.Row(key,sample.name+" ["+value.unit()+"]",value.stored(),value.capacity(),value.unit()));
        }
        reader.status=targets.isEmpty()?"No node connections":samples.isEmpty()?"No supported energy exposed on connected node sides":"Energy: "+samples.size()+" / "+targets.size()+" targets ("+EnergyValues.system(reader.energySystem)+")";
        if(unsupported>0)reader.status+="; "+unsupported+" unsupported/blocked";
        if(unloaded>0)reader.status+="; "+unloaded+" unloaded";
        if(failures[0]>0)reader.status+="; provider error (server log)";
        return List.copyOf(result.subList(0,Math.min(result.size(),PLConfig.MAX_ROWS.get())));
    }
    private EnergyReader(){}
}
