package net.foundations.pl4;

import java.util.*;
import net.foundations.pl4.core.MechanicalEnergyAccess;
import net.minecraft.util.Direction;
import net.minecraft.server.MinecraftServer;

/** Loaded-target-only telemetry; multiple AE2 nodes on the same grid are counted once. */
final class OptionalPowerTelemetry {
    private static MechanicalEnergyAccess.Create create;
    private static MechanicalEnergyAccess.AE2 ae2;
    private static boolean resolved;private static final Set<String> warned=new HashSet<>();
    static void clear(){resolved=false;create=null;ae2=null;warned.clear();}
    private static void warn(String provider,Throwable e){if(warned.add(provider))org.apache.logging.log4j.LogManager.getLogger("FoundationsPL4").warn("{} telemetry unavailable; check installed API version",provider,e);}
    private static void resolve(){
        if(resolved)return;resolved=true;
        try{create=new MechanicalEnergyAccess.Create(Class.forName("com.simibubi.create.content.kinetics.base.KineticBlockEntity",false,OptionalPowerTelemetry.class.getClassLoader()));}catch(ClassNotFoundException absent){}catch(ReflectiveOperationException|RuntimeException|LinkageError e){warn("Create",e);}
        try{ae2=new MechanicalEnergyAccess.AE2(Class.forName("appeng.api.networking.IInWorldGridNodeHost",false,OptionalPowerTelemetry.class.getClassLoader()),Direction.class);}catch(ClassNotFoundException absent){}catch(ReflectiveOperationException|RuntimeException|LinkageError e){warn("AE2",e);}
    }
    static List<Part.Row> sample(MinecraftServer server,NetworkEngine.Ref ref,List<Part.Link> links){
        resolve();boolean kinetic=ref.part().energySystem.equals("CREATE");String provider=kinetic?"Create":"AE2";
        if(kinetic?!PLConfig.CREATE_READS.get():!PLConfig.AE2_READS.get()){ref.part().status=provider+" telemetry disabled by server";return List.of();}
        if(kinetic?create==null:ae2==null){ref.part().status=provider+" adapter unavailable: install a supported version";return List.of();}
        List<Part.Row> rows=new ArrayList<>();Set<String> targets=new HashSet<>();Set<Object> grids=Collections.newSetFromMap(new IdentityHashMap<>());
        int sampled=0,unloaded=0,unsupported=0,failed=0;
        for(Part.Link link:links){
            if(link.entity()!=null)continue;
            if(!NetworkEngine.loaded(server,link)){unloaded++;continue;}
            String target=link.dimension()+":"+link.pos().toString();if(kinetic&&!targets.add(target))continue;
            Object entity=NetworkEngine.level(server,link).getBlockEntity(link.pos());
            try{
                if(kinetic){var v=create.read(entity);if(v==null){unsupported++;continue;}
                    if(rows.size()+5>PLConfig.MAX_ROWS.get())break;
                    String key="create:"+target;
                    rows.add(new Part.Row(key+":rpm","Speed",v.rpm(),Math.max(1,Math.abs(v.theoretical())),"RPM"));
                    rows.add(new Part.Row(key+":theoretical_rpm","Theoretical speed",v.theoretical(),Math.max(1,Math.abs(v.theoretical())),"RPM"));
                    rows.add(new Part.Row(key+":stress","Network stress",v.stress(),v.capacity(),"SU"));
                    rows.add(new Part.Row(key+":capacity","Network capacity",v.capacity(),v.capacity(),"SU"));
                    rows.add(new Part.Row(key+":overstressed","Overstressed",v.overstressed()?1:0,1,""));sampled++;
                }else{var v=ae2.read(entity,link.side());if(v==null){unsupported++;continue;}if(!grids.add(v.identity()))continue;
                    if(rows.size()+5>PLConfig.MAX_ROWS.get())break;
                    String key="ae2:"+target;
                    rows.add(new Part.Row(key+":storage","Grid energy",v.stored(),v.capacity(),"AE"));
                    rows.add(new Part.Row(key+":capacity","Grid capacity",v.capacity(),v.capacity(),"AE"));
                    rows.add(new Part.Row(key+":usage","Average usage",v.usage(),0,"AE/t"));
                    rows.add(new Part.Row(key+":injection","Average injection",v.injection(),0,"AE/t"));
                    rows.add(new Part.Row(key+":powered","Grid powered",v.powered()?1:0,1,""));sampled++;
                }
            }catch(ReflectiveOperationException|RuntimeException|LinkageError e){failed++;warn(provider,e);}
        }
        ref.part().status=provider+": "+sampled+(kinetic?" targets":" grids")+"; "+unsupported+" unsupported, "+unloaded+" unloaded, "+failed+" errors";
        return List.copyOf(rows);
    }
    private OptionalPowerTelemetry(){}
}
