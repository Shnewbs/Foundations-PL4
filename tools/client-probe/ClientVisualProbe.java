package net.foundations.pl4.client;

import java.nio.file.*;
import java.util.*;
import net.foundations.pl4.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Injected only in a disposable client checkout. Snapshot presentation; no server or gameplay acceptance. */
@EventBusSubscriber(modid=FoundationsPL4.ID,value=Dist.CLIENT)
public final class ClientVisualProbe {
    private static boolean started,waiting;private static int frame,index;private static long due;
    private static final List<Map<String,Object>> captures=new ArrayList<>();
    private static CompoundTag storage(){
        CompoundTag tag=new CompoundTag();tag.putString("token",UUID.randomUUID().toString());tag.putInt("slots",37);tag.putInt("endpoints",128);tag.putString("sort","NAME");
        ListTag rows=new ListTag();String[] names={"Diamond","Polished Deepslate","A very long item display name to test truncation","Oak Planks","Redstone Dust","Sapphire"};
        for(int i=0;i<6;i++){CompoundTag row=new CompoundTag();row.putInt("slot",i);row.putString("name",names[i]);row.putLong("count",4096L*(i+1));rows.add(row);}tag.put("storageRows",rows);return tag;
    }
    private static Screen screen(int kind){
        if(kind==0)return new GuideScreen();if(kind==1)return new WirelessStorageScreen(storage());
        Part reader=new Part(Kind.INVENTORY_READER,Direction.UP,UUID.randomUUID());reader.label="Warehouse monitor";
        reader.status="Connected: 4096 hosts";reader.rows.add(new Part.Row("diamond","Diamond",12480,64000,""));reader.rows.add(new Part.Row("planks","Oak Planks",2048,64000,""));
        return new PartScreen(BlockPos.ZERO,reader,false);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        Minecraft mc=Minecraft.getInstance();
        try{
            if(!started){if(mc.getOverlay()!=null||!(mc.screen instanceof TitleScreen))return;if(++frame<40)return;started=true;}
            if(waiting){
                if(System.nanoTime()<due)return;
                int kind=index%3,scale=index/3+1;String name=List.of("guide","wireless-storage","reader").get(kind)+"-scale-"+scale;
                Path output=Path.of(System.getenv("PL4_CLIENT_OUTPUT"));Files.createDirectories(output);
                try(var picture=Screenshot.takeScreenshot(mc.getMainRenderTarget())){picture.writeToFile(output.resolve(name+".png"));}
                Map<String,Object> capture=new LinkedHashMap<>();capture.put("image",name+".png");capture.put("requested_gui_scale",scale);capture.put("actual_gui_scale",mc.getWindow().getGuiScale());capture.put("gui_width",mc.getWindow().getGuiScaledWidth());capture.put("gui_height",mc.getWindow().getGuiScaledHeight());capture.put("screen",mc.screen.getClass().getName());captures.add(capture);
                waiting=false;index++;
            }
            if(index==12){
                Path output=Path.of(System.getenv("PL4_CLIENT_OUTPUT"));Map<String,Object> report=new LinkedHashMap<>();report.put("source_commit",System.getenv("PL4_CLIENT_SOURCE"));report.put("captures",captures);report.put("scope","Actual development client, software OpenGL, snapshot data, title-menu background. No installed-client, world display, server interaction or multiplayer acceptance.");
                Files.writeString(output.resolve("client-visual-probe.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));mc.stop();return;
            }
            mc.options.guiScale().set(index/3+1);mc.resizeDisplay();mc.setScreen(screen(index%3));waiting=true;due=System.nanoTime()+2_000_000_000L;
        }catch(Exception failure){throw new IllegalStateException("PL4 client visual probe failed",failure);}
    }
}
