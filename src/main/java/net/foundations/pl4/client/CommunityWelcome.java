package net.foundations.pl4.client;

import java.util.ArrayList;
import net.foundations.pl4.*;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** One local chat message per account, across reconnects, deaths, worlds and servers. */
final class CommunityWelcome {
    static void login(ClientPlayerNetworkEvent.LoggingIn event){
        String account=event.getPlayer().getUUID().toString();
        var seen=new ArrayList<String>(PLClientConfig.COMMUNITY_LINKS_SEEN.get());if(seen.contains(account))return;
        Minecraft.getInstance().gui.hud.getChat().addMessage(CommunityLinks.message());
        seen.add(account);PLClientConfig.COMMUNITY_LINKS_SEEN.set(seen);PLClientConfig.SPEC.save();
    }
    private CommunityWelcome(){}
}
