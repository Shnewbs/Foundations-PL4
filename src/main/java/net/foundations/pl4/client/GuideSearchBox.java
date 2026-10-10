package net.foundations.pl4.client;

import net.minecraft.client.gui.FontRenderer;
import net.foundations.pl4.compat.GuiGraphics;
import net.foundations.pl4.compat.EditBox;
import net.minecraft.util.text.ITextComponent;
/** Draw the empty hint once and without Minecraft's dark text shadow on parchment. */
final class GuideSearchBox extends EditBox {
    private final FontRenderer font;
    GuideSearchBox(FontRenderer font,int x,int y,int width,int height){super(font,x,y,width,height,new net.minecraft.util.text.TextComponentString("Search chapters"));this.font=font;setBordered(false);}
    @Override public void renderWidget(GuiGraphics g,int x,int y,float partial){
        super.renderWidget(g,x,y,partial);
        if(getValue().isEmpty()&&!isFocused())g.drawString(font,"Search chapters...",getX()+4,getY()+5,0xFF8FAAAD,false);
    }
}
