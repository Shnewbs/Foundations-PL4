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
    public static String clean(String text){String value=text.replaceAll("[\\p{Cntrl}§]","").trim();return value.substring(0,Math.min(48,value.length()));}
    private static String description(Part reader,Part.Link t){
        String alias=reader.channelNames.getOrDefault(id(t),"");
        String location=t.entity()!=null?"Entity "+t.entity()+" · "+t.dimension():t.dimension()+" "+t.pos().getX()+", "+t.pos().getY()+", "+t.pos().getZ()+" "+t.side().getName();
        return alias.isBlank()?location:alias+" · "+location;
    }
    public static boolean rename(Part reader,String name){
        if(reader.targetChannel.isEmpty())return false;
        String label=clean(name);
        if(label.isEmpty()){reader.channelNames.remove(reader.targetChannel);return true;}
        if(reader.channelNames.size()>=64&&!reader.channelNames.containsKey(reader.targetChannel))return false;
        reader.channelNames.put(reader.targetChannel,label);return true;
    }
    public static void refresh(MinecraftServer server,Part reader,List<Part.Link> targets) {
        reader.targetChoices.clear();reader.targetLabel="Selected target disconnected";
        List<Part.Link> matches=new ArrayList<>();Set<String> seen=new HashSet<>();
        String query=reader.targetQuery.toLowerCase(Locale.ROOT);
        for(var t:targets){
            String id=id(t);if(!seen.add(id))continue;
            String label=description(reader,t);
            if(id.equals(reader.targetChannel))reader.targetLabel=label+(available(server,t)?"":" [unloaded]");
            if(query.isEmpty()||label.toLowerCase(Locale.ROOT).contains(query)||id.contains(query))matches.add(t);
        }
        reader.targetCount=matches.size();reader.targetPage=net.foundations.pl4.compat.PortMath.clamp(reader.targetPage,0,Math.max(0,(matches.size()-1)/64));
        int start=reader.targetPage*64;
        for(int i=start;i<Math.min(start+64,matches.size());i++){
            var t=matches.get(i);String label=description(reader,t);
            if(!available(server,t))label+=" [unloaded]";
            reader.targetChoices.add(new Part.ReaderChoice(id(t),label,t.entity()==null?"block":"entity"));
        }
    }
    public static String label(Part reader) {
        if(reader.targetChannel.isEmpty())return reader.mode.equals("CHANNEL")&&reader.index>0?"Legacy channel "+reader.index:"All targets";
        return reader.targetLabel.isEmpty()?"Pinned endpoint "+reader.targetChannel:reader.targetLabel;
    }
}
