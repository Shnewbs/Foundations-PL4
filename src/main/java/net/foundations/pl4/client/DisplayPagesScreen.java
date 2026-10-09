package net.foundations.pl4.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.foundations.pl4.core.DisplayElements;

/** Eight stable page slots. Clearing page contents never renumbers other pages. */
final class DisplayPagesScreen extends Screen {
    final DisplayEditorScreen parent;
    private int left,top,w,h;
    private long revision;
    private boolean pending;
    DisplayPagesScreen(DisplayEditorScreen parent){super(Component.literal("Display pages"));this.parent=parent;}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        w=Math.min(400,width-16);h=Math.min(340,height-16);left=(width-w)/2;top=(height-h)/2;revision=parent.part.layoutRevision;pending=parent.layoutPending();
        int cellW=(w-28)/2,step=Math.max(24,(h-138)/4),source=parent.pageElementCount(parent.part.displayPage);
        for(int page=0;page<DisplayElements.MAX_PAGES;page++){
            final int target=page;int count=parent.pageElementCount(page),x=left+10+(page%2)*(cellW+8),y=top+53+(page/2)*step;
            String label=(page==parent.part.displayPage?"> ":"")+font.plainSubstrByWidth(parent.part.pageName(page),Math.max(20,cellW-78))+": "+count;
            var select=addRenderableWidget(Button.builder(Component.literal(label),b->{parent.pageAction("page",Integer.toString(target));rebuildWidgets();}).bounds(x,y,cellW-48,20).build());select.active=parent.editable&&!pending&&page!=parent.part.displayPage;
            var copy=addRenderableWidget(Button.builder(Component.literal("Copy"),b->{parent.pageAction("page_copy",Integer.toString(target));rebuildWidgets();}).bounds(x+cellW-46,y,46,20).build());copy.active=parent.editable&&!pending&&page!=parent.part.displayPage&&count==0&&source>0&&parent.part.elements.size()+source<=DisplayElements.MAX_ELEMENTS;
        }
        var name=addRenderableWidget(new net.minecraft.client.gui.components.EditBox(font,left+10,top+h-79,w-98,20,Component.literal("Page name")));name.setMaxLength(32);name.setValue(parent.part.pageNames.get(parent.part.displayPage));name.setEditable(parent.editable&&!pending);
        var rename=addRenderableWidget(Button.builder(Component.literal("Rename"),b->parent.commit("page_name",null,name.getValue(),parent.part.layoutRevision)).bounds(left+w-80,top+h-79,70,20).build());rename.active=parent.editable&&!pending;
        var clear=addRenderableWidget(Button.builder(Component.literal("Clear current page"),b->{parent.pageAction("page_clear","");rebuildWidgets();}).bounds(left+10,top+h-49,(w-28)/2,20).build());clear.active=parent.editable&&!pending&&source>0;
        addRenderableWidget(Button.builder(Component.literal("Back to editor"),b->onClose()).bounds(left+18+(w-28)/2,top+h-49,(w-28)/2,20).build());
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
        super.extractBackground(g,mx,my,partial);g.fill(left,top,left+w,top+h,0xF0182228);
        g.text(font,title,left+10,top+9,0xFFE3F2F4,false);
        g.text(font,font.plainSubstrByWidth("Copy current page into an empty slot. Ctrl+Z in editor undoes it.",w-20),left+10,top+25,0xFF92CAD2,false);
        g.text(font,font.plainSubstrByWidth(parent.message.isBlank()?"32 elements total · Eight fixed page slots":parent.message,w-20),left+10,top+40,0xFFB8C4CC,false);
        g.text(font,font.plainSubstrByWidth("Clear removes this page's contents; other pages stay intact.",w-20),left+10,top+h-21,0xFFB8C4CC,false);
    }
    @Override public void tick(){parent.tick();if(revision!=parent.part.layoutRevision||pending!=parent.layoutPending())rebuildWidgets();}
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){int key=event.key(),mods=event.modifiers();if(key==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE){onClose();return true;}return super.keyPressed(event);}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick){double x=event.x(),y=event.y();int button=event.button();if(button==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){onClose();return true;}return super.mouseClicked(event,doubleClick);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
