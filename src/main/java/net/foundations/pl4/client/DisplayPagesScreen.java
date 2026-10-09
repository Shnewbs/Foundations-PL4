package net.foundations.pl4.client;

import net.foundations.pl4.compat.GuiGraphics;
import net.foundations.pl4.compat.Button;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.foundations.pl4.core.DisplayElements;

/** Eight stable page slots. Clearing page contents never renumbers other pages. */
final class DisplayPagesScreen extends net.foundations.pl4.compat.PortScreen {
    final DisplayEditorScreen parent;
    private int left,top,w,h;
    private long revision;
    private boolean pending;
    DisplayPagesScreen(DisplayEditorScreen parent){super(new net.minecraft.util.text.StringTextComponent("Display pages"));this.parent=parent;}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        w=Math.min(400,width-16);h=Math.min(340,height-16);left=(width-w)/2;top=(height-h)/2;revision=parent.part.layoutRevision;pending=parent.layoutPending();
        int cellW=(w-28)/2,step=Math.max(24,(h-138)/4),source=parent.pageElementCount(parent.part.displayPage);
        for(int page=0;page<DisplayElements.MAX_PAGES;page++){
            final int target=page;int count=parent.pageElementCount(page),x=left+10+(page%2)*(cellW+8),y=top+53+(page/2)*step;
            String label=(page==parent.part.displayPage?"> ":"")+font.plainSubstrByWidth(parent.part.pageName(page),Math.max(20,cellW-78))+": "+count;
            var select=addRenderableWidget(Button.builder(new net.minecraft.util.text.StringTextComponent(label),b->{parent.pageAction("page",Integer.toString(target));rebuildWidgets();}).bounds(x,y,cellW-48,20).build());select.active=parent.editable&&!pending&&page!=parent.part.displayPage;
            var copy=addRenderableWidget(Button.builder(new net.minecraft.util.text.StringTextComponent("Copy"),b->{parent.pageAction("page_copy",Integer.toString(target));rebuildWidgets();}).bounds(x+cellW-46,y,46,20).build());copy.active=parent.editable&&!pending&&page!=parent.part.displayPage&&count==0&&source>0&&parent.part.elements.size()+source<=DisplayElements.MAX_ELEMENTS;
        }
        var name=addRenderableWidget(new net.foundations.pl4.compat.EditBox(font,left+10,top+h-79,w-98,20,new net.minecraft.util.text.StringTextComponent("Page name")));name.setMaxLength(32);name.setValue(parent.part.pageNames.get(parent.part.displayPage));name.setEditable(parent.editable&&!pending);
        var rename=addRenderableWidget(Button.builder(new net.minecraft.util.text.StringTextComponent("Rename"),b->parent.commit("page_name",null,name.getValue(),parent.part.layoutRevision)).bounds(left+w-80,top+h-79,70,20).build());rename.active=parent.editable&&!pending;
        var clear=addRenderableWidget(Button.builder(new net.minecraft.util.text.StringTextComponent("Clear current page"),b->{parent.pageAction("page_clear","");rebuildWidgets();}).bounds(left+10,top+h-49,(w-28)/2,20).build());clear.active=parent.editable&&!pending&&source>0;
        addRenderableWidget(Button.builder(new net.minecraft.util.text.StringTextComponent("Back to editor"),b->onClose()).bounds(left+18+(w-28)/2,top+h-49,(w-28)/2,20).build());
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        super.renderBackground(g,mx,my,partial);g.fill(left,top,left+w,top+h,0xF0182228);
        g.drawString(font,title,left+10,top+9,0xFFE3F2F4,false);
        g.drawString(font,font.plainSubstrByWidth("Copy current page into an empty slot. Ctrl+Z in editor undoes it.",w-20),left+10,top+25,0xFF92CAD2,false);
        g.drawString(font,font.plainSubstrByWidth(parent.message.isBlank()?"32 elements total · Eight fixed page slots":parent.message,w-20),left+10,top+40,0xFFB8C4CC,false);
        g.drawString(font,font.plainSubstrByWidth("Clear removes this page's contents; other pages stay intact.",w-20),left+10,top+h-21,0xFFB8C4CC,false);
    }
    @Override public void tick(){parent.tick();if(revision!=parent.part.layoutRevision||pending!=parent.layoutPending())rebuildWidgets();}
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==256){onClose();return true;}return super.keyPressed(key,scan,mods);}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==1){onClose();return true;}return super.mouseClicked(x,y,button);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
