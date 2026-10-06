package net.foundations.pl4.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.foundations.pl4.*;

@EventBusSubscriber(modid=FoundationsPL4.ID,value=Dist.CLIENT)
public final class PLClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CommunityWelcome::login);PLPackets.clientOpen=packet->{
        Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;
        if(packet.tag().getBoolean("guide").orElse(false)){mc.gui.setScreen(new GuideScreen());return;}
        if(packet.tag().getBoolean("wirelessStorage").orElse(false)){if(mc.gui.screen() instanceof WirelessStorageScreen storage)storage.update(packet.tag());else if(!packet.tag().getBoolean("reply").orElse(false))mc.gui.setScreen(new WirelessStorageScreen(packet.tag()));return;}
        Part part=Part.load(packet.tag(),mc.level.registryAccess());if(part==null)return;
        var editor=DisplayEditorScreen.active();if(editor!=null&&editor.identity().equals(part.identity)){editor.receive(packet,part);return;}
        if(mc.gui.screen() instanceof PartScreen screen&&screen.identity().equals(part.identity)){screen.update(part);screen.error(packet.tag().getString("layoutError").orElse(""));return;}
        if(packet.tag().getBoolean("reply").orElse(false))return; // Late acknowledgement must not reopen an editor after Escape.
        mc.gui.setScreen(part.kind.display()?new DisplayEditorScreen(packet.pos(),part,packet.tag().getBoolean("editable").orElse(false)):new PartScreen(packet.pos(),part,packet.tag().getBoolean("editable").orElse(false)));
    };});}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(FoundationsPL4.HOST_ENTITY.get(),HostRenderer::new);e.registerBlockEntityRenderer(FoundationsPL4.HAMMER_ENTITY.get(),HammerRenderer::new);}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(HammerModel.LAYER,HammerModel::layer);}
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(FoundationsPL4.HAMMER_MENU.get(),HammerScreen::new);}
}

