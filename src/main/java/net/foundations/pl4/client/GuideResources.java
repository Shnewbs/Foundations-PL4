package net.foundations.pl4.client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.foundations.pl4.FoundationsPL4;
import net.foundations.pl4.core.GuideBook;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.LoggerFactory;

/** Loaded only when opening the guide. No world subscriptions, polling threads or static screen references. */
final class GuideResources {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    static GuideBook load(ResourceManager manager,String language){
        try{
            String safe=language!=null&&language.matches("[a-z_]{2,16}")?language:"en_us";
            var resource=manager.getResource(FoundationsPL4.id("guide/"+safe+".json"));
            if(resource.isEmpty())resource=manager.getResource(FoundationsPL4.id("guide/en_us.json"));
            try(var in=resource.orElseThrow(()->new IOException("Missing PL4 guide resource")).open()){
                byte[] bytes=in.readNBytes(1_048_577);if(bytes.length>1_048_576)throw new IOException("Guide exceeds 1 MiB limit");
                return decode(JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject());
            }
        }catch(IOException|RuntimeException error){
            LoggerFactory.getLogger("FoundationsPL4").warn("Cannot load PL4 Field Guide resource",error);
            return new GuideBook("Foundations PL4 Field Guide","Resource recovery",List.of(new GuideBook.Chapter("recovery","start","Guide resource error",
                "Restore the bundled guide or fix your resource pack.","foundations_pl4:plguide",List.of(new GuideBook.Section("RECOVERY",
                "A guide resource could not be loaded. Restore assets/foundations_pl4/guide/en_us.json in your resource pack, reload resources, then reopen this guide. The client log contains the validation error. Your world data is unchanged.")))));
        }
    }
    static GuideBook decode(JsonObject root){
        if(root.get("schema").getAsInt()!=1)throw new IllegalArgumentException("Unsupported guide schema");
        var source=root.getAsJsonArray("chapters");if(source.size()>128)throw new IllegalArgumentException("Too many chapters");
        List<GuideBook.Chapter> chapters=new ArrayList<>();
        for(var entry:source){var c=entry.getAsJsonObject();var raw=c.getAsJsonArray("sections");if(raw.size()>32)throw new IllegalArgumentException("Too many sections");
            List<GuideBook.Section> sections=new ArrayList<>();for(var e:raw){var s=e.getAsJsonObject();sections.add(new GuideBook.Section(s.get("heading").getAsString(),s.get("body").getAsString()));}
            chapters.add(new GuideBook.Chapter(c.get("id").getAsString(),c.get("category").getAsString(),c.get("title").getAsString(),c.get("summary").getAsString(),c.get("icon").getAsString(),sections));
        }
        return new GuideBook(root.get("title").getAsString(),root.get("edition").getAsString(),chapters);
    }
    static final class Preferences {String chapter="start";final Set<String> saved=new LinkedHashSet<>();}
    private static Path path(){return FMLPaths.CONFIGDIR.get().resolve("foundations").resolve("pl4_guide.json");}
    static Preferences preferences(){
        Preferences p=new Preferences();Path file=path();
        try{if(Files.isRegularFile(file)&&Files.size(file)<=16_384){var json=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(json.has("chapter"))p.chapter=json.get("chapter").getAsString();
            if(json.has("saved"))for(var id:json.getAsJsonArray("saved")){if(p.saved.size()>=128)break;p.saved.add(id.getAsString());}
        }}catch(IOException|RuntimeException e){LoggerFactory.getLogger("FoundationsPL4").warn("Cannot read guide preferences; using defaults",e);}
        return p;
    }
    static void save(Preferences p){
        Path file=path(),temp=null;
        try{
            Files.createDirectories(file.getParent());temp=Files.createTempFile(file.getParent(),"pl4-guide-",".tmp");
            JsonObject json=new JsonObject();json.addProperty("chapter",p.chapter);JsonArray saved=new JsonArray();p.saved.stream().limit(128).forEach(saved::add);json.add("saved",saved);
            Files.writeString(temp,JSON.toJson(json),StandardCharsets.UTF_8);
            try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
        }catch(IOException e){LoggerFactory.getLogger("FoundationsPL4").warn("Cannot save guide preferences",e);}
        finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}
    }
    private GuideResources(){}
}
