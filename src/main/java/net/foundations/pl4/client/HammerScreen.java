package net.foundations.pl4.client;

import net.foundations.pl4.compat.GuiGraphics;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.PlayerInventory;
import net.foundations.pl4.*;
import net.foundations.pl4.core.HammerMotion;

/** Original 176x143 container texture and slot positions; status text occupies the unused separator. */
public final class HammerScreen extends net.foundations.pl4.compat.PortContainerScreen<HammerMenu> {
    private static final ResourceLocation TEXTURE=FoundationsPL4.id("textures/gui/hammer.png");
    public HammerScreen(HammerMenu menu,PlayerInventory inventory,ITextComponent title){
        super(menu,inventory,title);imageWidth=176;imageHeight=143;
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        int pixels=HammerMotion.progressPixels(menu.progress(),menu.duration());
        if(pixels>0)g.blit(TEXTURE,leftPos+76,topPos+24,176,0,pixels,16);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        g.drawString(font,title,(imageWidth-font.width(title.getColoredString()))/2,6,0x404040,false);
        String key=switch(menu.status()){case 1->"working";case 2->"cooldown";case 3->"output_full";case 4->"headroom";case 5->"no_recipe";default->"idle";};
        ITextComponent status=new net.minecraft.util.text.TranslationTextComponent("gui.foundations_pl4.hammer."+key);
        g.drawString(font,status,(imageWidth-font.width(status.getColoredString()))/2,45,menu.status()==4?0x993333:0x404040,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        // Native ContainerScreen performs the background pass BEFORE renderBg/slots.
        renderBackground(g);super.render(g,mouseX,mouseY,partial);
        renderTooltip(g,mouseX,mouseY);
        if(mouseX>=leftPos+76&&mouseX<leftPos+100&&mouseY>=topPos+24&&mouseY<topPos+40)
            g.renderTooltip(font,new net.minecraft.util.text.TextComponentString(menu.cooldown()>0?"Cooldown: "+menu.cooldown()+" ticks":menu.progress()+" / "+menu.duration()+" ticks"),mouseX,mouseY);
    }
}
