package net.foundations.pl4;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;

public final class CommunityLinks {
    public static final String DISCORD="https://discord.gg/tCTS9xduad",SUPPORT="https://ko-fi.com/shnewbs";
    public static Component message(){return Component.literal("[PL4] ").withStyle(ChatFormatting.GRAY)
        .append(link("Discord",DISCORD,ChatFormatting.AQUA)).append(Component.literal(" · ").withStyle(ChatFormatting.GRAY))
        .append(link("Support on Ko-fi",SUPPORT,ChatFormatting.GOLD));}
    private static Component link(String label,String url,ChatFormatting color){return Component.literal(label).withStyle(style->style.withColor(color).withUnderlined(true)
        .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(url))).withHoverEvent(new HoverEvent.ShowText(Component.literal(url))));}
    private CommunityLinks(){}
}
