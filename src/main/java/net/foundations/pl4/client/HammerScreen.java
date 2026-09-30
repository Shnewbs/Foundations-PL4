package net.foundations.pl4.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.foundations.pl4.*;
import net.foundations.pl4.core.HammerMotion;

/** Original 176x143 container texture and slot positions; status text occupies the unused separator. */
public final class HammerScreen extends AbstractContainerScreen<HammerMenu> {
    private static final Identifier TEXTURE=FoundationsPL4.id("textures/gui/hammer.png");
    public HammerScreen(HammerMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);imageWidth=176;imageHeight=143;titleLabelY=6;
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        int pixels=HammerMotion.progressPixels(menu.progress(),menu.duration());
        if(pixels>0)g.blit(TEXTURE,leftPos+76,topPos+24,176,0,pixels,16);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        g.drawString(font,title,(imageWidth-font.width(title))/2,6,0x404040,false);
        String key=switch(menu.status()){case 1->"working";case 2->"cooldown";case 3->"output_full";case 4->"headroom";case 5->"no_recipe";default->"idle";};
        Component status=Component.translatable("gui.foundations_pl4.hammer."+key);
        g.drawString(font,status,(imageWidth-font.width(status))/2,45,menu.status()==4?0x993333:0x404040,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        // Native AbstractContainerScreen performs the background pass BEFORE renderBg/slots.
        super.render(g,mouseX,mouseY,partial);
        renderTooltip(g,mouseX,mouseY);
        if(mouseX>=leftPos+76&&mouseX<leftPos+100&&mouseY>=topPos+24&&mouseY<topPos+40)
            g.renderTooltip(font,Component.literal(menu.cooldown()>0?"Cooldown: "+menu.cooldown()+" ticks":menu.progress()+" / "+menu.duration()+" ticks"),mouseX,mouseY);
    }
}
