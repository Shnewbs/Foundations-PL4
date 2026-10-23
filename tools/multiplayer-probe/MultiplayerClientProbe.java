package net.foundations.pl4.client;

import java.nio.file.*;
import java.util.*;
import net.foundations.pl4.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Real client packet/screenshot probe, injected only into disposable development runs. */
@EventBusSubscriber(modid=FoundationsPL4.ID,value=Dist.CLIENT)
public final class MultiplayerClientProbe {
    private static boolean installed,ready,sent,captured;private static UUID token;private static int frames,sentFrame;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;frames++;
        if(!installed){
            var original=PLPackets.clientOpen;PLPackets.clientOpen=packet->{original.accept(packet);if(token==null&&packet.tag().getBoolean("wirelessStorage")&&!packet.tag().getBoolean("closed")&&!packet.tag().getBoolean("reply"))token=UUID.fromString(packet.tag().getString("token"));};installed=true;
        }
        if(!ready&&frames>40){mc.player.connection.sendCommand("pl4probe ready");ready=true;}
        if(token!=null&&!sent){
            var request=new PLPackets.StorageRequest(token,"withdraw",0,17);PacketDistributor.sendToServer(request);PacketDistributor.sendToServer(request);sent=true;sentFrame=frames;
        }
        if(sent&&!captured&&frames-sentFrame>40){
            int count=0;for(int i=0;i<36;i++)if(mc.player.getInventory().getItem(i).is(Items.DIAMOND))count+=mc.player.getInventory().getItem(i).getCount();
            if(count!=17){if(frames-sentFrame>200)throw new IllegalStateException("Expected 17 client-side items after replay-protected request; got "+count);return;}
            try{
                String name=mc.player.getGameProfile().getName();Path out=Path.of(System.getenv("PL4_PROBE_OUTPUT"));Files.createDirectories(out);
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(name+"-storage.png"));}
                Map<String,Object> proof=new LinkedHashMap<>();proof.put("source_commit",System.getenv("PL4_PROBE_SOURCE"));proof.put("name",name);proof.put("uuid",mc.player.getUUID().toString());proof.put("client_player_class",mc.player.getClass().getName());proof.put("items_received",count);proof.put("identical_packets_sent",2);
                Files.writeString(out.resolve(name+"-client-proof.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(proof));mc.player.connection.sendCommand("pl4probe sent");captured=true;
            }catch(java.io.IOException failure){throw new IllegalStateException(failure);}
        }
        if(captured&&frames-sentFrame>100)mc.stop();
        if(frames>1200&&!captured)throw new IllegalStateException("Real multiplayer client probe timed out");
    }
}
