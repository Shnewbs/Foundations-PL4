package net.foundations.pl4.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explicit alignment controls for the current-page selection. */
final class DisplayArrangementScreen extends Screen {
    final DisplayEditorScreen parent;
    private int left,top,w,h;
    private static final String[] LABELS={"Align left","Align right","Align top","Align bottom","Centre horizontally","Centre vertically","Space horizontally","Space vertically"};
    private static final String[] ACTIONS={"align_left","align_right","align_top","align_bottom","align_hcenter","align_vcenter","distribute_x","distribute_y"};
    DisplayArrangementScreen(DisplayEditorScreen parent){super(Component.literal("Arrange elements"));this.parent=parent;}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        w=Math.min(360,width-16);h=Math.min(216,height-16);left=(width-w)/2;top=(height-h)/2;
        int count=parent.selectionCount(),bw=(w-28)/2,step=Math.min(25,Math.max(16,(h-98)/4));
        for(int i=0;i<ACTIONS.length;i++){
            final String action=ACTIONS[i];
            var button=addRenderableWidget(Button.builder(Component.literal(LABELS[i]),b->{parent.arrange(action);minecraft.gui.setScreen(parent);}).bounds(left+10+(i%2)*(bw+8),top+57+(i/2)*step,bw,Math.min(20,step-2)).build());
            button.active=count>=(i>=6?3:1);
        }
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(left+10,top+h-28,w-20,20).build());
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
        super.extractBackground(g,mx,my,partial);g.fill(left,top,left+w,top+h,0xF0182228);
        g.text(font,title,left+10,top+10,0xFFE3F2F4,false);
        String selection=parent.selectionCount()+" selected · Shift-click to add elements";
        g.text(font,font.plainSubstrByWidth(selection,w-20),left+10,top+27,0xFF92CAD2,false);
        g.text(font,font.plainSubstrByWidth("One: canvas edges. Multiple: selection bounds.",w-20),left+10,top+41,0xFFB8C4CC,false);
    }
    @Override public void tick(){parent.tick();}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick){double x=event.x(),y=event.y();int button=event.button();if(button==1){onClose();return true;}return super.mouseClicked(event,doubleClick);}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
