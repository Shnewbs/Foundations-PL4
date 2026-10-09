package net.foundations.pl4;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;

public final class CommunityLinks {
    public static final String DISCORD="https://discord.gg/tCTS9xduad",SUPPORT="https://ko-fi.com/shnewbs";
    public static Component message(){return new net.minecraft.network.chat.TextComponent("[PL4] ").withStyle(ChatFormatting.GRAY)
        .append(link("Discord",DISCORD,ChatFormatting.AQUA)).append(new net.minecraft.network.chat.TextComponent(" · ").withStyle(ChatFormatting.GRAY))
        .append(link("Support on Ko-fi",SUPPORT,ChatFormatting.GOLD));}
    private static Component link(String label,String url,ChatFormatting color){return new net.minecraft.network.chat.TextComponent(label).withStyle(style->style.withColor(color).withUnderlined(true)
        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,url)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new net.minecraft.network.chat.TextComponent(url))));}
    private CommunityLinks(){}
}
