package net.foundations.pl4.api;

import java.util.*;
import net.foundations.pl4.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Server-thread, read-only extension point for Info Reader telemetry. See docs/INFO_PROVIDER_API.md. */
public final class InfoProviders {
    public record Context(ServerLevel level,Part.Link target,Entity entity) {}
    @FunctionalInterface public interface Sink { void add(String key,String name,double value,double capacity,String unit); }
    @FunctionalInterface public interface Provider { void sample(Context context,Sink sink); }
    public interface Registration extends AutoCloseable { @Override void close(); }
    private static final Map<String,Provider> PROVIDERS=new TreeMap<>();
    private static final Set<String> WARNED=new HashSet<>();
    private InfoProviders() {}
    /** Register during common setup. Duplicate IDs fail rather than replace an installed provider. */
    public static synchronized Registration register(String id,Provider provider){
        Objects.requireNonNull(provider);
        if(id==null||id.length()>64||!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||id.equals("foundations_pl4:vanilla"))throw new IllegalArgumentException("Invalid or reserved provider ID");
        if(PROVIDERS.containsKey(id)||PROVIDERS.size()>=64)throw new IllegalArgumentException("Duplicate provider ID or registry full");
        PROVIDERS.put(id,provider);
        return ()->{synchronized(InfoProviders.class){if(PROVIDERS.remove(id,provider))WARNED.remove(id);}};
    }
    private static synchronized Map<String,Provider> snapshot(){return new TreeMap<>(PROVIDERS);}
    /** Never loads chunks; only connected targets supplied by the reader should be passed here. */
    public static List<Part.Row> sample(ServerLevel level,Part.Link target){
        Entity entity=target.entity()==null?null:level.getEntity(target.entity());
        if(target.entity()!=null?entity==null:!level.hasChunkAt(target.pos()))return List.of();
        Context context=new Context(level,target,entity);List<Part.Row> result=new ArrayList<>();
        collect("foundations_pl4:vanilla",BuiltinInfoProvider::sample,context,result,true);
        for(var entry:snapshot().entrySet()){
            if(result.size()>=256)break;
            collect(entry.getKey(),entry.getValue(),context,result,false);
        }
        return List.copyOf(result);
    }
    private static void collect(String id,Provider provider,Context context,List<Part.Row> result,boolean builtin){
        Map<String,Part.Row> pending=new LinkedHashMap<>();int budget=Math.min(builtin?64:32,256-result.size());
        boolean[] active={true};
        try{
            provider.sample(context,(key,name,value,capacity,unit)->{
                if(!active[0]||pending.size()>=budget||key==null||key.isBlank()||key.length()>48||!key.matches("[a-zA-Z0-9_.-]+")||name==null||unit==null||!Double.isFinite(value)||!Double.isFinite(capacity)||capacity<0)return;
                String fullKey=builtin?key:id+"/"+key;
                pending.putIfAbsent(fullKey,new Part.Row(fullKey,clean(name,96),value,capacity,clean(unit,24)));
            });
            result.addAll(pending.values());
        }catch(RuntimeException|LinkageError ex){
            synchronized(InfoProviders.class){if(WARNED.add(id))org.slf4j.LoggerFactory.getLogger("FoundationsPL4").warn("Info provider {} failed; its partial sample was discarded",id,ex);}
        }finally{active[0]=false;}
    }
    private static String clean(String value,int limit){String text=value.replaceAll("[\\p{Cntrl}§]","");return text.substring(0,Math.min(limit,text.length()));}
}
