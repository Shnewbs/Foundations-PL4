package net.foundations.pl4.client;

import java.util.*;
import net.foundations.pl4.compat.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.foundations.pl4.compat.Button;
import net.foundations.pl4.compat.EditBox;
import net.foundations.pl4.compat.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.foundations.pl4.core.DisplayElements;
import net.foundations.pl4.core.EditorPalette;

/** PL2-like element configuration, separate from reader data modes. Changes are committed atomically. */
final class DisplayPropertiesScreen extends net.foundations.pl4.compat.PortScreen {
    final DisplayEditorScreen parent;private DisplayElements.Spec spec;private final boolean add;private final long revision;
    private final Map<String,EditBox> fields=new LinkedHashMap<>();private final Map<AbstractWidget,Integer> positions=new LinkedHashMap<>();
    private int left,top,w,h,scroll;private String error="";
    DisplayPropertiesScreen(DisplayEditorScreen parent,DisplayElements.Spec spec,boolean add){super(new net.minecraft.network.chat.TextComponent(add?"Add display element":"Edit display element"));this.parent=parent;this.spec=spec;this.add=add;revision=parent.part.layoutRevision;}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        fields.clear();positions.clear();w=Math.min(466,width-16);h=Math.min(350,height-16);left=(width-w)/2;top=(height-h)/2;int x=left+116,y=top+33,fw=w-130;boolean narrow=w<390;
        control("Type: "+label(spec.type()),left+10,y,w-20,b->{if(!collect())return;DisplayElements.Type[] all=DisplayElements.Type.values();var oldType=spec.type();var type=all[(oldType.ordinal()+1)%all.length];int columns=(type==DisplayElements.Type.INVENTORY||type==DisplayElements.Type.FLUID_GRID)&&(oldType!=DisplayElements.Type.INVENTORY&&oldType!=DisplayElements.Type.FLUID_GRID)?DisplayElements.defaultColumns(type):spec.columns();spec=new DisplayElements.Spec(spec.id(),type,spec.text(),spec.reader(),spec.key(),spec.asset(),spec.bounds(),spec.color(),spec.count(),spec.names(),columns,spec.offset(),spec.page(),spec.vertical(),spec.compact(),spec.textAlign(),spec.wrap(),spec.textScale(),spec.options());rebuildWidgets();});y+=25;
        String source=spec.reader().isBlank()?"Display reader (default)":parent.part.readerChoices.stream().filter(c->c.id().equals(spec.reader())).map(c->c.name()).findFirst().orElse(spec.reader());
        control("Reader: "+source,left+10,y,w-20,b->{if(collect())minecraft.setScreen(new DisplayPickerScreen(this,true));});y+=29;
        field("key","Data key",spec.key(),x,y,fw-48);control("Pick",left+w-52,y,42,b->{if(collect()){parent.inspect(spec.reader());minecraft.setScreen(new DisplayPickerScreen(this,false));}});y+=29;
        field("asset","Static item/fluid ID",spec.asset(),x,y,fw);y+=29;
        field("text","Caption",spec.text(),x,y,fw);y+=29;
        field("x","X",Integer.toString(spec.bounds().x()),x,y,58);if(narrow)y+=29;field("y","Y",Integer.toString(spec.bounds().y()),narrow?x:x+114,y,58);y+=29;
        field("w","Width",Integer.toString(spec.bounds().width()),x,y,58);if(narrow)y+=29;field("h","Height",Integer.toString(spec.bounds().height()),narrow?x:x+114,y,58);y+=29;
        field("columns","Columns",Integer.toString(spec.columns()),x,y,58);if(narrow)y+=29;field("offset","Grid offset",Integer.toString(spec.offset()),narrow?x:x+114,y,58);y+=29;
        field("color","Colour (hex)",EditorPalette.hex(spec.color()),x,y,100);control("Palette…",x+108,y,78,b->{if(collect())minecraft.setScreen(new DisplayColorPickerScreen(this,spec.color()));});y+=29;
        control("Quantity: "+(spec.count()?"On":"Off"),left+10,y,118,b->toggle(0));if(narrow)y+=25;control("Names: "+(spec.names()?"On":"Off"),narrow?left+10:left+136,y,108,b->toggle(1));y+=25;
        control("Bar: "+(spec.vertical()?"Vertical":"Horizontal"),left+10,y,138,b->toggle(2));if(narrow)y+=25;control("Numbers: "+(spec.compact()?"Compact":"Exact"),narrow?left+10:left+154,y,138,b->toggle(3));y+=25;
        control("Text align: "+spec.textAlign().name(),left+10,y,138,b->{if(collect()){var all=DisplayElements.TextAlign.values();spec=spec.textStyle(all[(spec.textAlign().ordinal()+1)%all.length],spec.wrap(),spec.textScale());rebuildWidgets();}});
        if(narrow)y+=25;control("Wrap: "+(spec.wrap()?"On":"Off"),narrow?left+10:left+154,y,108,b->{if(collect()){spec=spec.textStyle(spec.textAlign(),!spec.wrap(),spec.textScale());rebuildWidgets();}});y+=25;
        control("Text scale: "+String.format(Locale.ROOT,"%.2fx",spec.textScale()),left+10,y,138,b->{if(collect()){float next=spec.textScale()+.25F;if(next>DisplayElements.MAX_TEXT_SCALE+.001F)next=DisplayElements.MIN_TEXT_SCALE;spec=spec.textStyle(spec.textAlign(),spec.wrap(),next);rebuildWidgets();}});y+=25;
        field("background","Background hex / blank",spec.options().background()<0?"":EditorPalette.hex(spec.options().background()),x,y,fw);y+=29;
        field("border","Border hex / blank",spec.options().border()<0?"":EditorPalette.hex(spec.options().border()),x,y,fw);y+=29;
        control("Click: "+(spec.options().actionPage()<0?"None":"Page "+(spec.options().actionPage()+1)),left+10,y,180,b->{if(collect()){var o=spec.options();spec=spec.options(new DisplayElements.Options(o.group(),o.locked(),o.hidden(),o.background(),o.border(),o.actionPage()==7?-1:o.actionPage()+1));rebuildWidgets();}});y+=25;
        control("Page: "+(spec.page()+1),left+10,y,100,b->{if(collect()){spec=spec.onPage((spec.page()+1)%8);rebuildWidgets();}});
        addRenderableWidget(Button.builder(new net.minecraft.network.chat.TextComponent("Back"),b->onClose()).bounds(left+10,top+h-26,64,20).build());
        addRenderableWidget(Button.builder(new net.minecraft.network.chat.TextComponent(add?"Add element":"Save element"),b->{if(collect()){parent.selected=spec.id();minecraft.setScreen(parent);parent.commit(add?"add":"update",spec,"",revision);}}).bounds(left+w-112,top+h-26,102,20).build());layout();
    }
    private static String label(DisplayElements.Type type){return switch(type){case TEXT->"Text / value";case ITEM->"Item icon";case BLOCK->"Block model";case INVENTORY->"Inventory grid";case FLUID->"Fluid tank";case FLUID_GRID->"Fluid grid";case BAR->"Progress / energy bar";};}
    private void field(String key,String label,String value,int x,int y,int w){EditBox b=new EditBox(font,x,y,Math.max(20,w),20,new net.minecraft.network.chat.TextComponent(label));b.setMaxLength(key.equals("key")||key.equals("asset")?192:128);b.setValue(value);b.setTooltip(Tooltip.create(new net.minecraft.network.chat.TextComponent(label)));fields.put(key,b);positions.put(b,y);addRenderableWidget(b);}
    private Button control(String label,int x,int y,int w,Button.OnPress action){Button b=Button.builder(new net.minecraft.network.chat.TextComponent(label),action).bounds(x,y,Math.min(w,left+this.w-10-x),20).build();positions.put(b,y);return addRenderableWidget(b);}
    private void toggle(int choice){if(!collect())return;spec=new DisplayElements.Spec(spec.id(),spec.type(),spec.text(),spec.reader(),spec.key(),spec.asset(),spec.bounds(),spec.color(),choice==0?!spec.count():spec.count(),choice==1?!spec.names():spec.names(),spec.columns(),spec.offset(),spec.page(),choice==2?!spec.vertical():spec.vertical(),choice==3?!spec.compact():spec.compact(),spec.textAlign(),spec.wrap(),spec.textScale(),spec.options());rebuildWidgets();}
    private int optionalColor(String key){String value=fields.get(key).getValue().trim();return value.isEmpty()?-1:Integer.parseUnsignedInt(value.replace("#",""),16)&0xFFFFFF;}
    private int number(String key){return Integer.parseInt(fields.get(key).getValue());}
    private boolean collect(){
        try{String asset=fields.get("asset").getValue();if(!asset.isBlank()&&ResourceLocation.tryParse(asset)==null)throw new IllegalArgumentException("Use a namespaced item/fluid ID, e.g. minecraft:stone.");
            spec=new DisplayElements.Spec(spec.id(),spec.type(),fields.get("text").getValue(),spec.reader(),fields.get("key").getValue(),asset,
                new DisplayElements.Rect(number("x"),number("y"),number("w"),number("h")),Integer.parseUnsignedInt(fields.get("color").getValue().replace("#",""),16),spec.count(),spec.names(),number("columns"),number("offset"),spec.page(),spec.vertical(),spec.compact(),spec.textAlign(),spec.wrap(),spec.textScale(),new DisplayElements.Options(spec.options().group(),spec.options().locked(),spec.options().hidden(),optionalColor("background"),optionalColor("border"),spec.options().actionPage()));error="";return true;
        }catch(IllegalArgumentException ex){error="Check numeric fields, hex colour, and resource ID.";return false;}
    }
    void color(int rgb){spec=new DisplayElements.Spec(spec.id(),spec.type(),spec.text(),spec.reader(),spec.key(),spec.asset(),spec.bounds(),rgb&0xFFFFFF,spec.count(),spec.names(),spec.columns(),spec.offset(),spec.page(),spec.vertical(),spec.compact(),spec.textAlign(),spec.wrap(),spec.textScale(),spec.options());}
    void reader(String id){spec=new DisplayElements.Spec(spec.id(),spec.type(),spec.text(),id,"",spec.asset(),spec.bounds(),spec.color(),spec.count(),spec.names(),spec.columns(),spec.offset(),spec.page(),spec.vertical(),spec.compact(),spec.textAlign(),spec.wrap(),spec.textScale(),spec.options());parent.inspect(id);minecraft.setScreen(this);}
    void key(String key){spec=new DisplayElements.Spec(spec.id(),spec.type(),spec.text(),spec.reader(),key,"",spec.bounds(),spec.color(),spec.count(),spec.names(),spec.columns(),spec.offset(),spec.page(),spec.vertical(),spec.compact(),spec.textAlign(),spec.wrap(),spec.textScale(),spec.options());minecraft.setScreen(this);}
    String reader(){return spec.reader();}
    private void layout(){int end=positions.values().stream().mapToInt(Integer::intValue).max().orElse(0)+20;scroll=net.foundations.pl4.compat.PortMath.clamp(scroll,0,Math.max(0,end-(top+h-38)));positions.forEach((b,y)->{b.y=y-scroll;b.visible=b.y>=top+31&&b.y+20<=top+h-35;});}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        super.renderBackground(g,mx,my,partial);g.fill(left,top,left+w,top+h,0xFA181818);g.fill(left,top,left+w,top+24,0xFF3C3C3C);g.drawString(font,title,left+10,top+8,0xFFFFFFFF,false);
        for(var entry:fields.entrySet()){var b=entry.getValue();if(!b.visible)continue;String label=switch(entry.getKey()){case "key"->"Data key";case "asset"->"Static resource";case "text"->"Caption";case "w"->"Width";case "h"->"Height";case "color"->"Colour";default->entry.getKey();};int x=b.getX()==left+230?b.getX()-48:left+10;g.drawString(font,label,x,b.y+6,0xFFDADADA,false);}
        if(!error.isBlank())g.drawString(font,font.plainSubstrByWidth(error,w-20),left+10,top+h-37,0xFFFF9999,false);
        var colorBox=fields.get("color");if(colorBox!=null&&colorBox.visible){int c=EditorPalette.parse(colorBox.getValue(),spec.color());int px=Math.min(left+w-30,colorBox.getX()+194);g.fill(px,colorBox.getY()+2,px+18,colorBox.getY()+18,0xFF000000|c);g.renderOutline(px,colorBox.getY()+2,18,16,0xFFBFD7DA);}
        var columnsBox=fields.get("columns");if(columnsBox!=null&&columnsBox.visible&&(spec.type()==DisplayElements.Type.INVENTORY||spec.type()==DisplayElements.Type.FLUID_GRID)){int cols=3;try{cols=net.foundations.pl4.compat.PortMath.clamp(Integer.parseInt(columnsBox.getValue()),1,16);}catch(NumberFormatException ignored){}int px=Math.min(left+w-102,columnsBox.getX()+72),py=columnsBox.getY()+2,pw=84,ph=16;g.fill(px,py,px+pw,py+ph,0xFF20282C);int cell=Math.max(2,(pw-4)/cols);for(int i=0;i<cols;i++){int sx=px+2+i*cell;g.fill(sx,py+2,Math.min(px+pw-2,sx+cell-2),py+ph-2,0xFF3B4B50);}g.drawString(font,cols+" cols",px+4,py+4,0xFFB8DCE0,false);}
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(button==1){onClose();return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){scroll-=(int)Math.signum(dy)*28;layout();return true;}
    @Override public void tick(){parent.tick();}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
