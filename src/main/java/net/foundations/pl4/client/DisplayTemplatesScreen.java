package net.foundations.pl4.client;

import java.util.*;
import net.foundations.pl4.compat.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.foundations.pl4.compat.Button;
import net.foundations.pl4.compat.EditBox;
import net.foundations.pl4.compat.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.foundations.pl4.LayoutTemplate;
import net.foundations.pl4.core.DisplayElements;

/** Preview first, then apply a revision-fenced, undoable whole-layout edit. */
final class DisplayTemplatesScreen extends net.foundations.pl4.compat.PortScreen {
    private final DisplayEditorScreen parent;private int left,top,w,h,page;private EditBox name;
    private long previewRevision;private List<DisplayElements.Spec> previewElements=List.of();private int previewWidth,previewHeight;private boolean validPreview;private LayoutTemplate preview;private boolean fit=true,clearReaders;private String status="";private List<String> names=List.of();private String savedName="layout";private int selected,preset;
    DisplayTemplatesScreen(DisplayEditorScreen parent){super(Component.literal("Layout library"));this.parent=parent;}
    @Override public boolean isPauseScreen(){return false;}
    private interface Action{void run()throws Exception;}
    private void action(Action action){try{action.run();}catch(Exception e){status=e.getMessage()==null?"Invalid layout/template":e.getMessage();}}
    private void refresh()throws Exception{names=DisplayTemplateLibrary.names();selected=Math.min(selected,Math.max(0,names.size()-1));}
    @Override protected void init(){
        w=Math.min(480,width-16);h=Math.min(348,height-16);left=(width-w)/2;top=(height-h)/2;int cw=(w-28)/3;
        name=addRenderableWidget(new EditBox(font,left+10,top+31,w-20,20,Component.literal("Template name")));name.setMaxLength(48);name.setValue(savedName);name.setResponder(v->savedName=v);
        action(()->refresh());
        button("Save current",0,55,cw,()->{DisplayTemplateLibrary.save(name.getValue(),parent.exportLayout());refresh();status="Saved "+name.getValue();});
        button("Browse names",1,55,cw,()->{if(names.isEmpty())throw new IllegalArgumentException("No local templates");name.setValue(names.get(selected));selected=(selected+1)%names.size();status="Selected "+name.getValue();});
        button("Load preview",2,55,cw,()->{preview=DisplayTemplateLibrary.load(name.getValue());previewRevision=parent.part.layoutRevision;page=0;status="Preview loaded; Apply replaces all pages";});
        button("Copy JSON",0,79,cw,()->{minecraft.keyboardHandler.setClipboard(parent.exportLayout().encode());status="Layout JSON copied";});
        button("Paste preview",1,79,cw,()->{preview=LayoutTemplate.decode(minecraft.keyboardHandler.getClipboard());previewRevision=parent.part.layoutRevision;page=0;status="Clipboard preview loaded";});
        button("Delete named",2,79,cw,()->{DisplayTemplateLibrary.delete(name.getValue());refresh();status="Deleted local template "+name.getValue();});
        button("Fit: "+(fit?"On":"Off"),0,103,cw,()->{fit=!fit;rebuildWidgets();});
        button(clearReaders?"Readers: Auto":"Readers: Keep",1,103,cw,()->{clearReaders=!clearReaders;rebuildWidgets();});
        button("Preview page "+(page+1),2,103,cw,()->{page=(page+1)%DisplayElements.MAX_PAGES;rebuildWidgets();});
        button("Starter: "+switch(preset){case 0->"Items";case 1->"Fluids";default->"Energy";},0,127,cw,()->{preview=LayoutTemplate.preset(preset,parent.spaceW(),parent.spaceH());previewRevision=parent.part.layoutRevision;page=0;preset=(preset+1)%3;status="Starter loaded; configure its reader after applying";});
        button("Save preview",1,127,cw,()->{if(preview==null)throw new IllegalArgumentException("Load a preview first");DisplayTemplateLibrary.save(name.getValue(),preview);refresh();status="Preview saved";});
        button("Discard preview",2,127,cw,()->{preview=null;status="Preview discarded";});
        validPreview=false;
        if(preview!=null)try{previewElements=preview.prepare(parent.spaceW(),parent.spaceH(),fit,clearReaders);previewWidth=parent.spaceW();previewHeight=parent.spaceH();validPreview=true;}catch(IllegalArgumentException e){previewElements=preview.elements();previewWidth=preview.width();previewHeight=preview.height();status=e.getMessage();}
        var apply=addRenderableWidget(Button.builder(Component.literal("Apply all pages"),b->action(()->{if(preview==null)throw new IllegalArgumentException("Load a preview first");parent.importLayout(preview,fit,clearReaders,previewRevision);if(parent.layoutPending())onClose();else status=parent.message;})).bounds(left+10,top+h-28,(w-28)/2,20).build());apply.active=parent.editable&&!parent.layoutPending()&&validPreview;
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(left+18+(w-28)/2,top+h-28,(w-28)/2,20).build());
    }
    private void button(String text,int column,int y,int cw,Action action){addRenderableWidget(Button.builder(Component.literal(text),b->{action(action);rebuildWidgets();}).bounds(left+10+column*(cw+4),top+y,cw,20).build());}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        super.renderBackground(g,mx,my,partial);g.fill(left,top,left+w,top+h,0xFA182229);g.drawString(font,title,left+10,top+10,0xFFE3F2F4,false);
        int px=left+10,py=top+152,pw=w-20,ph=Math.max(10,h-208);g.fill(px,py,px+pw,py+ph,0xFF0B1014);
        if(preview!=null){double scale=Math.min((double)pw/previewWidth,(double)ph/previewHeight);for(var e:previewElements)if(e.page()==page){var r=e.bounds();int x=px+(int)(r.x()*scale),y=py+(int)(r.y()*scale),ew=Math.max(1,(int)(r.width()*scale)),eh=Math.max(1,(int)(r.height()*scale));g.fill(x,y,x+ew,y+eh,0xA0000000|e.color());}
            g.drawString(font,previewWidth+" x "+previewHeight+" / "+preview.elements().size()+" elements",px,py+ph+3,0xFFB8CDD0,false);
        }
        g.drawString(font,font.plainSubstrByWidth(status.isBlank()?"Apply replaces all pages. Ctrl+Z restores the previous layout.":status,w-20),left+10,top+h-43,0xFFB8CDD0,false);
    }
    @Override public void tick(){parent.tick();}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
