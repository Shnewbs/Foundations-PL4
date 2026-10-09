package net.foundations.pl4;

import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.StringTextComponent;

public final class CommunityLinks {
    public static final String DISCORD="https://discord.gg/tCTS9xduad",SUPPORT="https://ko-fi.com/shnewbs";
    public static ITextComponent message(){return new net.minecraft.util.text.StringTextComponent("[PL4] ").withStyle(TextFormatting.GRAY)
        .append(link("Discord",DISCORD,TextFormatting.AQUA)).append(new net.minecraft.util.text.StringTextComponent(" · ").withStyle(TextFormatting.GRAY))
        .append(link("Support on Ko-fi",SUPPORT,TextFormatting.GOLD));}
    private static ITextComponent link(String label,String url,TextFormatting color){return new net.minecraft.util.text.StringTextComponent(label).withStyle(style->style.withColor(color).withUnderlined(true)
        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,url)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new net.minecraft.util.text.StringTextComponent(url))));}
    private CommunityLinks(){}
}
