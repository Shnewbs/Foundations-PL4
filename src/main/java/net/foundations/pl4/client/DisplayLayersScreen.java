package net.foundations.pl4.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.foundations.pl4.core.DisplayElements;

/** Page-local element list, frontmost first. Selection is shared with the world editor. */
final class DisplayLayersScreen extends Screen {
    final DisplayEditorScreen parent;
    private int left,top,w,h,offset,rows;
    private long revision;
    private boolean pending;
    DisplayLayersScreen(DisplayEditorScreen parent){super(Component.literal("Display layers"));this.parent=parent;}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        w=Math.min(380,width-16);h=Math.min(426,height-16);left=(width-w)/2;top=(height-h)/2;
        rows=Math.max(1,(h-222)/22);var layers=parent.pageLayers();offset=Math.clamp(offset,0,Math.max(0,layers.size()-rows));
        revision=parent.part.layoutRevision;pending=parent.layoutPending();
        for(int i=0;i<rows&&offset+i<layers.size();i++){
            var element=layers.get(offset+i);String detail=element.text().isBlank()?(element.asset().isBlank()?element.key():element.asset()):element.text();
            String label=(parent.layerSelected(element.id())?"[x] ":"[ ] ")+"#"+(offset+i+1)+" "+element.type()+" "+(element.options().locked()?"[locked] ":"")+(element.options().hidden()?"[hidden] ":"")+(element.options().group().isEmpty()?"":"[group] ")+detail;
            var button=addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(label,w-32)),b->{parent.selectLayer(element.id(),net.minecraft.client.Minecraft.getInstance().hasShiftDown());rebuildWidgets();}).bounds(left+10,top+55+i*22,w-20,20).build());button.active=!pending;
        }
        var previous=addRenderableWidget(Button.builder(Component.literal("Previous"),b->{offset=Math.max(0,offset-rows);rebuildWidgets();}).bounds(left+10,top+h-169,(w-28)/2,20).build());previous.active=offset>0;
        var next=addRenderableWidget(Button.builder(Component.literal("Next"),b->{offset+=rows;rebuildWidgets();}).bounds(left+18+(w-28)/2,top+h-169,(w-28)/2,20).build());next.active=offset+rows<layers.size();
        String[] labels={"Bring forward","Send backward","Bring to front","Send to back"};String[] actions={"layer_forward","layer_backward","layer_front","layer_back"};
        for(int i=0;i<actions.length;i++){
            final String action=actions[i];var button=addRenderableWidget(Button.builder(Component.literal(labels[i]),b->{parent.layer(action);rebuildWidgets();}).bounds(left+10+(i%2)*((w-28)/2+8),top+h-145+(i/2)*22,(w-28)/2,20).build());button.active=parent.editable&&!pending&&parent.selectionCount()>0;
        }
        String[] organization={"group","ungroup","lock","unlock","hide","show"};
        for(int i=0;i<organization.length;i++){
            String action=organization[i];var button=addRenderableWidget(Button.builder(Component.literal(Character.toUpperCase(action.charAt(0))+action.substring(1)),b->{parent.organize(action);rebuildWidgets();}).bounds(left+10+(i%2)*((w-28)/2+8),top+h-101+(i/2)*22,(w-28)/2,20).build());button.active=parent.editable&&!pending&&parent.selectionCount()>0;
        }
        addRenderableWidget(Button.builder(Component.literal("Back to editor"),b->onClose()).bounds(left+10,top+h-31,w-20,20).build());
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
        super.extractBackground(g,mx,my,partial);g.fill(left,top,left+w,top+h,0xF0182228);
        g.text(font,title,left+10,top+9,0xFFE3F2F4,false);
        g.text(font,font.plainSubstrByWidth("Page "+(parent.part.displayPage+1)+" · Frontmost first · "+parent.selectionCount()+" selected",w-20),left+10,top+25,0xFF92CAD2,false);
        String hint=parent.message.isBlank()?"Shift: toggle selection · Ctrl+A: select page":parent.message;
        g.text(font,font.plainSubstrByWidth(hint,w-20),left+10,top+40,0xFFB8C4CC,false);
    }
    @Override public void tick(){parent.tick();if(revision!=parent.part.layoutRevision||pending!=parent.layoutPending())rebuildWidgets();}
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){int key=event.key(),scan=event.keycode(),mods=event.modifiers();
        if(key==com.mojang.blaze3d.platform.InputConstants.KEY_A&&net.minecraft.client.Minecraft.getInstance().hasControlDown()){parent.selectAllLayers();rebuildWidgets();return true;}
        if(key==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE||key==com.mojang.blaze3d.platform.InputConstants.KEY_L){onClose();return true;}return super.keyPressed(event);
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick){double x=event.x(),y=event.y();int button=event.button();if(button==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){onClose();return true;}return super.mouseClicked(event,doubleClick);}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
