package net.foundations.pl4;

import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.server.MinecraftServer;

/** Stable endpoint selection, independent of slot/tank index and list ordering. */
public final class ReaderChannels {
    private ReaderChannels() {}
    public static String id(Part.Link link) {
        String key=link.dimension()+"|"+link.pos().asLong()+"|"+link.side().ordinal()+"|"+link.entity()+"|"+link.part();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }
    public static String sanitize(String value) {
        try{return UUID.fromString(value).toString();}catch(IllegalArgumentException ex){return "";}
    }
    public static List<Part.Link> select(Part reader,List<Part.Link> targets) {
        if(!reader.targetChannel.isEmpty())return targets.stream().filter(t->id(t).equals(reader.targetChannel)).limit(1).toList();
        if(reader.mode.equals("CHANNEL")&&reader.index>0)return targets.size()>=reader.index?List.of(targets.get(reader.index-1)):List.of();
        return targets;
    }
    public static boolean available(MinecraftServer server,Part.Link target) {
        var level=NetworkEngine.level(server,target);
        return level!=null&&(target.entity()!=null?level.getEntity(target.entity())!=null:level.hasChunkAt(target.pos()));
    }
    public static void refresh(MinecraftServer server,Part reader,List<Part.Link> targets) {
        reader.targetChoices.clear();
        Set<String> seen=new HashSet<>();
        for(var t:targets){
            String id=id(t);if(!seen.add(id))continue;
            String label=t.dimension()+" "+t.pos().getX()+", "+t.pos().getY()+", "+t.pos().getZ()+" "+t.side().getName();
            if(t.entity()!=null)label="Entity "+t.entity()+" · "+t.dimension();
            if(!available(server,t))label+=" [unloaded]";
            reader.targetChoices.add(new Part.ReaderChoice(id,label,t.entity()==null?"block":"entity"));
            if(reader.targetChoices.size()==64)break;
        }
    }
    public static String label(Part reader) {
        if(reader.targetChannel.isEmpty())return reader.mode.equals("CHANNEL")&&reader.index>0?"Legacy channel "+reader.index:"All targets";
        return reader.targetChoices.stream().filter(c->c.id().equals(reader.targetChannel)).map(Part.ReaderChoice::name).findFirst().orElse("Selected target disconnected");
    }
}
