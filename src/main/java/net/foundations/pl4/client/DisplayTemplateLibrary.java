package net.foundations.pl4.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.foundations.pl4.LayoutTemplate;

/** Client-local named templates; names cannot select arbitrary filesystem paths. */
final class DisplayTemplateLibrary {
    private static Path directory()throws IOException{
        Path dir=net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("foundations_pl4").resolve("display_templates");
        if(Files.isSymbolicLink(dir)||Files.isSymbolicLink(dir.getParent()))throw new IOException("Template folder cannot be a symbolic link");
        Files.createDirectories(dir);return dir;
    }
    private static Path file(String name)throws IOException{
        if(!name.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,47}"))throw new IOException("Use 1-48 letters, numbers, - or _");
        Path file=directory().resolve(name+".json");if(Files.isSymbolicLink(file))throw new IOException("Template cannot be a symbolic link");return file;
    }
    static List<String> names()throws IOException{
        try(var paths=Files.list(directory())){return paths.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)).map(p->p.getFileName().toString()).filter(s->s.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,47}\\.json")).map(s->s.substring(0,s.length()-5)).sorted().limit(128).toList();}
    }
    static LayoutTemplate load(String name)throws IOException{
        Path p=file(name);try(var input=Files.newInputStream(p,LinkOption.NOFOLLOW_LINKS)){byte[] data=input.readNBytes(262145);if(data.length>262144)throw new IOException("Template file too large");return LayoutTemplate.decode(new String(data,StandardCharsets.UTF_8));}
    }
    static void save(String name,LayoutTemplate template)throws IOException{
        Path p=file(name);byte[] data=template.encode().getBytes(StandardCharsets.UTF_8);if(data.length>262144)throw new IOException("Template file too large");
        Path temp=Files.createTempFile(p.getParent(),"layout-",".tmp");try{Files.write(temp,data);try{Files.move(temp,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,p,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
    }
    static void delete(String name)throws IOException{Files.deleteIfExists(file(name));}
    private DisplayTemplateLibrary(){}
}
