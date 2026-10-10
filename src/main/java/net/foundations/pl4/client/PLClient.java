package net.foundations.pl4.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


import net.foundations.pl4.*;

@EventBusSubscriber(modid=FoundationsPL4.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class PLClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){net.minecraftforge.fml.DeferredWorkQueue.runLater(()->{net.minecraftforge.fml.client.registry.ClientRegistry.bindTileEntityRenderer(FoundationsPL4.HOST_ENTITY.get(),HostRenderer::new);net.minecraftforge.fml.client.registry.ClientRegistry.bindTileEntityRenderer(FoundationsPL4.HAMMER_ENTITY.get(),HammerRenderer::new);HostEntity.clientRenderBounds=HostRenderer::getRenderBoundingBox;net.minecraft.client.gui.ScreenManager.register(FoundationsPL4.HAMMER_MENU.get(),HammerScreen::new);net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(CommunityWelcome::login);PLPackets.clientOpen=packet->{
        Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;
        if(packet.tag().getBoolean("guide")){mc.setScreen(new GuideScreen());return;}
        if(packet.tag().getBoolean("wirelessStorage")){if(mc.screen instanceof WirelessStorageScreen storage)storage.update(packet.tag());else if(!packet.tag().getBoolean("reply"))mc.setScreen(new WirelessStorageScreen(packet.tag()));return;}
        Part part=Part.load(packet.tag(),null);if(part==null)return;
        var editor=DisplayEditorScreen.active();if(editor!=null&&editor.identity().equals(part.identity)){editor.receive(packet,part);return;}
        if(mc.screen instanceof PartScreen screen&&screen.identity().equals(part.identity)){screen.update(part);screen.error(packet.tag().getString("layoutError"));return;}
        if(packet.tag().getBoolean("reply"))return; // Late acknowledgement must not reopen an editor after Escape.
        mc.setScreen(part.kind.display()?new DisplayEditorScreen(packet.pos(),part,packet.tag().getBoolean("editable")):new PartScreen(packet.pos(),part,packet.tag().getBoolean("editable")));
    };});}
}

