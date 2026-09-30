package net.foundations.pl4.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.foundations.pl4.core.EditorPalette;

/** Simple visual palette that fills the normal hex field; no hidden colour format is introduced. */
final class DisplayColorPickerScreen extends Screen {
    private final DisplayPropertiesScreen parent;private int selected;private int left,top,w,h;
    DisplayColorPickerScreen(DisplayPropertiesScreen parent,int selected){super(Component.literal("Choose display colour"));this.parent=parent;this.selected=selected&0xFFFFFF;}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        w=Math.min(392,width-16);h=Math.min(268,height-16);left=(width-w)/2;top=(height-h)/2;
        addRenderableWidget(Button.builder(Component.literal("PL4 default"),b->selected=EditorPalette.DEFAULT).bounds(left+10,top+h-26,86,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(left+w-138,top+h-26,56,20).build());
        addRenderableWidget(Button.builder(Component.literal("Use colour"),b->{parent.color(selected);minecraft.setScreen(parent);}).bounds(left+w-78,top+h-26,68,20).build());
    }
    private int columns(){return 6;}private int cell(){return 34;}private int gap(){return 5;}
    private int gridX(){return left+14;}private int gridY(){return top+56;}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        super.renderBackground(g,mx,my,partial);g.fill(left,top,left+w,top+h,0xFA171C20);g.fill(left,top,left+w,top+26,0xFF2A3B42);g.fill(left,top,left+3,top+h,0xFF62C7D6);
        g.drawString(font,title,left+10,top+9,0xFFFFFFFF,false);g.drawString(font,"Click a swatch. The chosen value is written back to the hex field.",left+10,top+34,0xFFB8CDD0,false);
        int i=0;for(var sw:EditorPalette.PRESETS){int col=i%columns(),row=i/columns(),x=gridX()+col*(cell()+gap()),y=gridY()+row*(cell()+gap());boolean hover=mx>=x&&mx<x+cell()&&my>=y&&my<y+cell();int rgb=sw.rgb();
            g.fill(x,y,x+cell(),y+cell(),0xFF000000|rgb);g.renderOutline(x-1,y-1,cell()+2,cell()+2,(rgb==selected?0xFFFFFFFF:(hover?0xFF62C7D6:0xFF48575D)));i++;}
        int px=left+w-128,py=top+56;g.fill(px,py,px+108,py+76,0xFF20272B);g.fill(px+10,py+10,px+98,py+46,0xFF000000|selected);g.renderOutline(px+9,py+9,90,38,0xFFBFD7DA);g.drawString(font,"#"+EditorPalette.hex(selected),px+18,py+56,0xFFE8F3F4,false);
        g.drawString(font,"Right-click = Back",left+10,top+h-40,0xFF8FA9AD,false);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==1){onClose();return true;}if(button==0){int i=0;for(var sw:EditorPalette.PRESETS){int col=i%columns(),row=i/columns(),sx=gridX()+col*(cell()+gap()),sy=gridY()+row*(cell()+gap());if(x>=sx&&x<sx+cell()&&y>=sy&&y<sy+cell()){selected=sw.rgb();return true;}i++;}}return super.mouseClicked(x,y,button);
    }
    @Override public void tick(){parent.parent.tick();}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
