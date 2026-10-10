package net.foundations.pl4.client;

import net.minecraft.client.Minecraft;
import net.foundations.pl4.compat.GuiGraphics;
import net.foundations.pl4.compat.Button;
import net.foundations.pl4.compat.Tooltip;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.item.ItemStack;

/** Low-chrome book controls. Native Button retains keyboard, narration and tooltip behavior. */
final class GuideButton extends Button {
    private static final int[][] NODES={{1,5},{6,1},{11,5},{6,11}};
    int glyph=-1,chapterNumber;boolean selected;private final boolean leftAlign;private final ItemStack icon;
    GuideButton(int x,int y,int width,int height,String label,String tooltip,boolean leftAlign,ItemStack icon,OnPress press){
        super(x,y,width,height,new net.minecraft.util.text.TextComponentString(label),press,DEFAULT_NARRATION);
        this.leftAlign=leftAlign;this.icon=icon;
        if(tooltip!=null&&!tooltip.isBlank())setTooltip(Tooltip.create(new net.minecraft.util.text.TextComponentString(tooltip)));
    }
    private static void drawGlyph(GuiGraphics g,int x,int y,int type,int c){
        if(type==0){g.fill(x+2,y+1,x+13,y+15,c);g.fill(x+4,y+2,x+12,y+14,0xFF27343B);g.fill(x+6,y+5,x+11,y+6,c);g.fill(x+6,y+8,x+11,y+9,c);}
        else if(type==1){g.fill(x+2,y+6,x+14,y+8,c);g.fill(x+7,y+2,x+9,y+14,c);for(int[] p:NODES){g.fill(x+p[0],y+p[1],x+p[0]+4,y+p[1]+4,c);g.fill(x+p[0]+1,y+p[1]+1,x+p[0]+3,y+p[1]+3,0xFF27343B);}}
        else if(type==2){g.fill(x,y+2,x+16,y+13,c);g.fill(x+2,y+4,x+14,y+11,0xFF27343B);g.fill(x+5,y+14,x+11,y+15,c);g.fill(x+3,y+6,x+10,y+7,c);}
        else{g.fill(x+3,y+2,x+13,y+4,c);g.fill(x+3,y+2,x+5,y+14,c);g.fill(x+3,y+12,x+13,y+14,c);g.fill(x+7,y+6,x+12,y+7,c);g.fill(x+7,y+9,x+12,y+10,c);}
    }
    @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial){
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean hot=isHoveredOrFocused();
        if(leftAlign&&glyph<0&&icon.isEmpty()){
            // Chapter index on parchment, rather than a wall of identical dark machine buttons.
            int ink=selected?0xFF123F4C:0xFF334448;
            g.fill(x,y,x+w,y+h,selected?0xB0AAC8C6:hot?0x709EB8B6:0x20B4B9A8);
            g.fill(x,y+h-1,x+w,y+h,selected?0xFF39778A:0x55798582);
            if(selected)g.fill(x,y,x+2,y+h,0xFF23667A);
            g.fill(x+5,y+5,x+23,y+h-5,selected?0xFF2C5662:0xFF5B6D6E);
            var font=Minecraft.getInstance().font;String number=chapterNumber<10?"0"+chapterNumber:Integer.toString(chapterNumber);
            g.drawString(font,number,x+14-font.width(number)/2,y+(h-8)/2,0xFFE9F0E8,false);
            String text=font.substrByWidth(getMessage(),Math.max(1,w-34));
            g.drawString(font,text,x+29,y+(h-8)/2,ink,false);return;
        }
        int border=selected?0xFF69C9D9:hot?0xFF92C3C9:0xFF607679;
        int fill=selected?0xFF224C56:hot?0xFF304851:0xFF27343B;
        g.fill(x,y,x+w,y+h,border);g.fill(x+1,y+1,x+w-1,y+h-1,fill);
        if(selected)g.fill(x+1,y+1,x+3,y+h-1,0xFF79D3FF);
        if(glyph>=0){drawGlyph(g,x+(w-16)/2,y+(h-16)/2,glyph,selected?0xFF9CE8ED:0xFFE1EBE9);return;}
        if(!icon.isEmpty()){g.renderItem(icon,x+(w-16)/2,y+(h-16)/2);return;}
        var font=Minecraft.getInstance().font;String value=font.substrByWidth(getMessage(),Math.max(1,w-10));
        g.drawString(font,value,leftAlign?x+6:x+(w-font.width(value))/2,y+(h-8)/2,active?0xFFF0F2EB:0xFF899292,false);
    }
}
