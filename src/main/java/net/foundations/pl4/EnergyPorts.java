package net.foundations.pl4;

import java.util.*;
import net.foundations.pl4.core.EnergyConversion;
import net.foundations.pl4.core.ReflectiveEnergyTransfer;
import net.foundations.pl4.core.ReflectiveElectrodynamicTransfer;
import net.minecraft.util.Direction;
import net.minecraft.server.MinecraftServer;
import net.foundations.pl4.compat.Capabilities;
import net.foundations.pl4.compat.BlockCapability;
import net.foundations.pl4.compat.RegisterCapabilitiesEvent;
import org.apache.logging.log4j.LogManager;

/** Sided optional adapters; no external mod classes are linked or world handlers retained. */
public final class EnergyPorts {
    private record Adapter(BlockCapability<Object,Direction> capability,ReflectiveEnergyTransfer access,ReflectiveElectrodynamicTransfer electro){}
    private static Map<String,Adapter> adapters;
    private static final Set<String> warned=new HashSet<>();
    private static void warn(String unit,Exception failure){if(warned.add(unit))LogManager.getLogger("FoundationsPL4").warn("Native {} transfer adapter unavailable; unsupported API versions fail closed.",unit,failure);}
    static void failure(String unit,RuntimeException failure){warn(unit,failure);}
    public static void clear(){adapters=null;warned.clear();}
    @SuppressWarnings("unchecked")
    private static Map<String,Adapter> adapters(){
        if(adapters!=null)return adapters;
        Map<String,Adapter> found=new HashMap<>();
        for(var cap:BlockCapability.getAll()){
            String id=cap.name().toString();String unit=id.equals("mekanism:strict_energy_handler")?"J":id.equals("gtceu:energy_container")?"EU":id.equals("voltaic:electrodynamicblock")?"ED_J":null;
            if(unit==null||cap.contextClass()!=Direction.class)continue;
            try{if(unit.equals("ED_J")){found.put(unit,new Adapter((BlockCapability<Object,Direction>)cap,null,ReflectiveElectrodynamicTransfer.resolve(cap.typeClass())));continue;}var access=unit.equals("J")?ReflectiveEnergyTransfer.mekanism(cap.typeClass()):ReflectiveEnergyTransfer.gregtech(cap.typeClass(),Direction.class);found.put(unit,new Adapter((BlockCapability<Object,Direction>)cap,access,null));}
            catch(ReflectiveOperationException|RuntimeException failure){warn(unit,failure);}
        }
        adapters=Map.copyOf(found);return adapters;
    }
    public static boolean supported(String unit){return unit.equals("FE")||adapters().containsKey(unit);}
    public static String label(String unit){return unit.equals("ED_J")?"ED Joules":unit.equals("J")?"Mek J":unit;}
    public static String next(String unit){List<String> values=List.of("FE","J","EU","ED_J").stream().filter(EnergyPorts::supported).toList();return values.get((Math.max(0,values.indexOf(unit))+1)%values.size());}
    public static boolean enabled(String unit){return unit.equals("FE")||unit.equals("J")&&PLConfig.MEKANISM_TRANSFERS.get()||unit.equals("EU")&&PLConfig.GREGTECH_TRANSFERS.get()||unit.equals("ED_J")&&PLConfig.ELECTRODYNAMICS_TRANSFERS.get();}
    public static EnergyConversion.Rates rates(){return new EnergyConversion.Rates(PLConfig.FE_PER_1000_J.get(),PLConfig.FE_PER_EU.get(),PLConfig.CONVERSION_EFFICIENCY.get(),PLConfig.FE_PER_ED_J.get());}
    public static EnergyConversion.Port resolve(MinecraftServer server,NetworkEngine.Ref ref,String unit){
        if(!enabled(unit))return null;Part.Link link=ref.adjacent();if(!NetworkEngine.loaded(server,link))return null;
        var level=NetworkEngine.level(server,link);
        if(unit.equals("FE")){
            var storage=net.foundations.pl4.compat.PortCapabilities.get(level,Capabilities.EnergyStorage.BLOCK,link.pos(),link.side());if(storage==null)return null;
            return new EnergyConversion.Port(){
                public long extract(long n,boolean simulate){return storage.extractEnergy((int)Math.min(Integer.MAX_VALUE,n),simulate);}
                public long insert(long n,boolean simulate){return storage.receiveEnergy((int)Math.min(Integer.MAX_VALUE,n),simulate);}
            };
        }
        Adapter adapter=adapters().get(unit);if(adapter==null)return null;
        Object handler=net.foundations.pl4.compat.PortCapabilities.get(level,adapter.capability,link.pos(),link.side());
        return adapter.electro!=null?adapter.electro.bind(handler):adapter.access.bind(handler,link.side(),ref.part().energyVoltage,PLConfig.MAX_ENERGY_CONTAINERS.get());
    }
    private EnergyPorts(){}
}
