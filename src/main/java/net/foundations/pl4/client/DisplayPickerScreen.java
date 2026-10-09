package net.foundations.pl4.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import net.foundations.pl4.Part;

/** Bounded picker of server-authorized reader choices / exact component variant row keys. */
final class DisplayPickerScreen extends net.foundations.pl4.compat.PortScreen {
    final DisplayPropertiesScreen parent;private final boolean readers;private EditBox search;private int left,top,w,h,scroll;private String query="";
    private record Choice(String key,String label,Part.Row row){}
    private List<Choice> choices=List.of();
    DisplayPickerScreen(DisplayPropertiesScreen parent,boolean readers){super(Component.literal(readers?"Choose reader":"Choose data row"));this.parent=parent;this.readers=readers;}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){w=Math.min(470,width-16);h=Math.min(330,height-16);left=(width-w)/2;top=(height-h)/2;
        search=new EditBox(font,left+10,top+30,w-20,20,Component.literal("Search"));search.setMaxLength(96);search.setValue(query);search.setResponder(v->{query=v;scroll=0;});addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("Automatic / default"),b->choose("")).bounds(left+10,top+h-26,140,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(left+w-66,top+h-26,56,20).build());refresh();}
    private void refresh(){List<Choice> all=new ArrayList<>();if(readers)for(var c:parent.parent.part.readerChoices)all.add(new Choice(c.id(),c.name()+" / "+c.kind(),null));else for(var r:parent.parent.source(parent.reader()))all.add(new Choice(r.key(),r.name()+" : "+Part.Row.number(r.value())+" "+r.unit(),r));
        String filter=query.toLowerCase(Locale.ROOT);choices=all.stream().filter(c->(c.label+" "+c.key).toLowerCase(Locale.ROOT).contains(filter)).toList();scroll=net.foundations.pl4.compat.PortMath.clamp(scroll,0,Math.max(0,choices.size()-Math.max(1,(h-94)/22)));}
    private void choose(String value){if(readers)parent.reader(value);else parent.key(value);}
    @Override public void tick(){parent.parent.tick();refresh();}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        super.renderBackground(g,mx,my,partial);g.fill(left,top,left+w,top+h,0xFA171717);g.fill(left,top,left+w,top+24,0xFF3C3C3C);g.drawString(font,title,left+10,top+8,0xFFFFFFFF,false);
        int count=Math.max(1,(h-94)/22);for(int i=0;i<count&&i+scroll<choices.size();i++){var c=choices.get(i+scroll);int y=top+58+i*22;
            g.fill(left+8,y,left+w-8,y+21,mx>=left+8&&mx<left+w-8&&my>=y&&my<y+21?0xFF404F3F:0xFF282828);
            int x=left+12;if(c.row!=null&&c.row.hasItem()){g.renderItem(c.row.item(),x,y+2);x+=22;}
            g.drawString(font,font.plainSubstrByWidth(c.label,w-(x-left)-16),x,y+6,0xFFEEEEEE,false);
        }
        if(choices.isEmpty())g.drawString(font,readers?"No connected readers":"No matching rows; check reader data mode",left+12,top+64,0xFFE1B1A5,false);
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(button==1){onClose();return true;}if(button==0&&x>=left+8&&x<left+w-8&&y>=top+58&&y<top+h-36){int i=scroll+(int)(y-top-58)/22;if(i-scroll<Math.max(1,(h-94)/22)&&i<choices.size()){choose(choices.get(i).key);return true;}}return super.mouseClicked(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){scroll=Math.max(0,scroll-(int)Math.signum(dy)*3);refresh();return true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
