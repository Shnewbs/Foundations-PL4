package net.foundations.pl4.client;

import java.util.*;
import net.foundations.pl4.compat.GuiGraphics;
import net.foundations.pl4.compat.EditBox;
import net.foundations.pl4.compat.Button;
import net.foundations.pl4.compat.Tooltip;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.item.ItemStack;
import net.foundations.pl4.FoundationsPL4;
import net.foundations.pl4.core.GuideBook;
import net.foundations.pl4.core.GuideLayout;
import net.foundations.pl4.core.GuideNavigation;

/** Foundations technical binder: Calculator-style two-pane reference layout in PL4 graphite/cyan. All text remains native GUI scale. */
public final class GuideScreen extends net.foundations.pl4.compat.PortScreen {
    private static final ResourceLocation COVER=FoundationsPL4.id("field_guide/cover"),PAGE=FoundationsPL4.id("field_guide/page");
    private static final String[] CATEGORIES={"start","network","display","reference"};
    private static final String[] TAB_NAMES={"Welcome and tutorials","Networks","Displays","Reference"};
    private static final String[] TAB_SHORT={"START","NET","DISP","REF"};
    private static final String[] TAB_ICONS={"plguide","datacable","largedisplayscreen","operator"};
    private static final int INK=0xFF263239,MUTED=0xFF485E64,ACCENT=0xFF185769;
    private record Line(IReorderingProcessor text,int y,boolean heading) {}
    private GuideBook book;private GuideBook.Chapter chapter;private GuideResources.Preferences preferences;
    private GuideLayout.Layout layout;private EditBox search;
    private String category="start",query="";private boolean savedOnly,contents;
    private int listScroll,bodyScroll,bodyHeight,drag;private double dragOffset;
    private List<GuideBook.Chapter> filtered=List.of();private List<Line> lines=List.of();
    private final List<GuideButton> chapterButtons=new ArrayList<>();
    private final Map<String,ItemStack> icons=new HashMap<>();
    private int bookmarkWidth,footerJumpX;
    private static final String[] SPECIMEN={"node","datacable","inventoryreader","displayscreen"};
    public GuideScreen(){super(new net.minecraft.util.text.StringTextComponent("Foundations PL4 Field Guide"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        if(book==null){
            book=GuideResources.load(minecraft.getResourceManager(),minecraft.getLanguageManager().getSelected().getCode());preferences=GuideResources.preferences();
            chapter=book.chapters().stream().filter(c->c.id().equals(preferences.chapter)).findFirst().orElse(book.chapters().get(0));category=chapter.category();
            preferences.saved.removeIf(id->book.chapters().stream().noneMatch(c->c.id().equals(id)));
        }
        layout=GuideLayout.fit(width,height);drag=0;footerJumpX=width;chapterButtons.clear();
        var b=layout.book();
        for(int i=0;i<4;i++){
            final int index=i;int tabW=42,tabX=Math.min(width-tabW,b.right()-1);
            GuideButton tab=button(tabX,b.y()+46+i*30,tabW,26,TAB_SHORT[i],TAB_NAMES[i],false,ItemStack.EMPTY,v->{
                category=CATEGORIES[index];query="";savedOnly=false;listScroll=0;
                chapter=GuideNavigation.section(book,category,chapter);category=chapter.category();preferences.chapter=chapter.id();bodyScroll=0;contents=layout.compact();rebuildWidgets();
            });tab.selected=category.equals(CATEGORIES[i]);
        }
        int footer=b.bottom()-29;
        if(layout.compact())button(b.x()+18,b.y()+42,font.width("Contents")+12,20,contents?"Read":"Contents","Switch chapter list and reading page",false,ItemStack.EMPTY,v->{contents=!contents;rebuildWidgets();});
        int right=layout.compact()?b.x()+22:b.x()+b.width()/2+22;
        button(right,footer,24,19,"<","Previous chapter",false,ItemStack.EMPTY,v->move(-1));
        button(right+28,footer,24,19,">","Next chapter",false,ItemStack.EMPTY,v->move(1));
        button(b.right()-font.width("Done")-31,footer,font.width("Done")+14,19,"Done","Close Field Guide",false,ItemStack.EMPTY,v->onClose());
        // Clear filters when jumping home; the tutorial is always reachable from Welcome.
        if(!layout.compact()){
            boolean welcome=chapter.id().equals("start");String caption=welcome?"Tutorial >":"Welcome";
            int width=font.width(caption)+14,doneWidth=font.width("Done")+14;
            footerJumpX=b.right()-17-doneWidth-6-width;
            button(footerJumpX,footer,width,19,caption,welcome?"Build your first inventory monitor":"Return to Welcome",false,ItemStack.EMPTY,v->{
                query="";savedOnly=false;listScroll=0;
                select(GuideNavigation.byId(book,welcome?"tutorial_inventory":"start"));
            });
        }
        GuideButton star=button(b.right()-38,b.y()+12,21,21,preferences.saved.contains(chapter.id())?"*":"+","Bookmark this chapter",false,ItemStack.EMPTY,v->{
            if(!preferences.saved.remove(chapter.id()))preferences.saved.add(chapter.id());rebuildWidgets();
        });star.selected=preferences.saved.contains(chapter.id());
        if(listVisible()){
            var list=layout.list();int sy=layout.compact()?b.bottom()-48:b.y()+48;
            bookmarkWidth=font.width("Saved")+16;
            int sx=layout.compact()?list.x()+4:list.x();
            search=new GuideSearchBox(font,sx,sy,Math.max(30,list.width()-bookmarkWidth-7),17);
            search.setBordered(true);search.setMaxLength(96);search.setTextColor(0xFFEAF3F2);search.setTextColorUneditable(0xFF91A9AD);search.setValue(query);
            search.setTooltip(Tooltip.create(new net.minecraft.util.text.StringTextComponent("Search all chapters; clear to return to this section")));
            search.setResponder(value->{query=value;listScroll=0;refreshList();});addRenderableWidget(search);
            GuideButton saved=button(list.right()-bookmarkWidth,sy,bookmarkWidth,19,"Saved","Show only bookmarked chapters",false,ItemStack.EMPTY,v->{savedOnly=!savedOnly;listScroll=0;refreshList();((GuideButton)v).selected=savedOnly;});saved.selected=savedOnly;
            refreshList();
        }else{search=null;filtered=book.search(category,query,preferences.saved,savedOnly);}
        wrapBody();
    }
    private GuideButton button(int x,int y,int w,int h,String text,String tip,boolean left,ItemStack icon,Button.OnPress press){return addRenderableWidget(new GuideButton(x,y,w,h,text,tip,left,icon,press));}
    private ItemStack item(String id){return icons.computeIfAbsent(id,value->{var key=ResourceLocation.tryParse(value);return key==null?ItemStack.EMPTY:new ItemStack(Registry.ITEM.get(key));});}
    private boolean listVisible(){return !layout.compact()||contents;}
    private boolean detailVisible(){return !layout.compact()||!contents;}
    private void select(GuideBook.Chapter next){chapter=next;category=next.category();preferences.chapter=next.id();bodyScroll=0;contents=false;rebuildWidgets();}
    private void move(int direction){int index=book.chapters().indexOf(chapter);var next=book.chapters().get(Math.floorMod(index+direction,book.chapters().size()));category=next.category();listScroll=0;select(next);}
    private void refreshList(){
        for(var button:chapterButtons)removeWidget(button);chapterButtons.clear();
        filtered=book.search(category,query,preferences.saved,savedOnly);var rect=layout.list();
        listScroll=GuideLayout.clampScroll(listScroll,filtered.size()*25,rect.height());
        int first=listScroll/25;
        for(int i=first;i<filtered.size();i++){
            int y=rect.y()+i*25-listScroll;if(y+22>rect.bottom())break;if(y<rect.y())continue;
            var c=filtered.get(i);var row=button(rect.x(),y,rect.width()-9,22,(preferences.saved.contains(c.id())?"* ":"")+c.title(),"",true,ItemStack.EMPTY,v->select(c));
            row.selected=c==chapter;row.chapterNumber=i+1;chapterButtons.add(row);
        }
    }
    private void wrapBody(){
        List<Line> result=new ArrayList<>();int y=0,w=layout.detail().width()-13;
        for(var section:chapter.sections()){
            for(var line:font.split(new net.minecraft.util.text.StringTextComponent(section.heading()),Math.max(16,w-12))){result.add(new Line(line,y,true));y+=18;}
            y+=5;
            for(String paragraph:section.body().split("\\n",-1)){
                for(var line:font.split(new net.minecraft.util.text.StringTextComponent(paragraph),Math.max(16,w-4))){result.add(new Line(line,y,false));y+=12;}
            }
            y+=13;
        }
        lines=List.copyOf(result);bodyHeight=y;bodyScroll=GuideLayout.clampScroll(bodyScroll,bodyHeight,layout.detail().height());
    }
    /** Native blur first; then book, art, text; inherited Screen.render draws the widgets last. */
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){
        super.renderBackground(g,x,y,partial);
        var b=layout.book();drawBook(g,b);
        int leftWidth=layout.compact()?b.width()-62:b.width()/2-44;
        String name=font.plainSubstrByWidth("FOUNDATIONS PL4 FIELD GUIDE",leftWidth);
        g.drawString(font,name,b.x()+22,b.y()+20,INK,false);
        if(!layout.compact()){
            g.drawString(font,font.plainSubstrByWidth("TECHNICAL MANUAL  /  1.21.1",leftWidth),b.x()+22,b.y()+34,ACCENT,false);
            if(listVisible()){int ry=b.y()+69;g.drawString(font,"DATA",layout.list().x(),ry,ACCENT,false);g.drawString(font,">  READER  >  VIEW",layout.list().x()+35,ry,MUTED,false);}
        }
        if(listVisible()){
            var list=layout.list();if(filtered.isEmpty())g.drawString(font,"No matching chapters",list.x()+2,list.y()+6,MUTED,false);
            scrollbar(g,list,listScroll,filtered.size()*25);
            var description=chapter;
            for(int i=listScroll/25;i<filtered.size();i++){int rowY=list.y()+i*25-listScroll;if(rowY+22>list.bottom())break;if(rowY>=list.y()&&x>=list.x()&&x<list.right()-9&&y>=rowY&&y<rowY+22){description=filtered.get(i);break;}}
            int lineY=list.bottom()+5,count=0;for(var line:font.split(new net.minecraft.util.text.StringTextComponent(description.summary()),list.width()-8)){if(count++==2)break;g.drawString(font,line,list.x()+2,lineY,MUTED,false);lineY+=10;}

        }
        if(detailVisible()){
            var r=layout.detail();int headerY=layout.compact()?b.y()+64:b.y()+26;
            String heading=font.plainSubstrByWidth(chapter.title(),r.width()-14);
            g.drawString(font,heading,r.x(),headerY,INK,false);
            if(!layout.compact()){
                g.drawString(font,font.plainSubstrByWidth(GuideNavigation.heading(chapter.category()),r.width()-14),r.x(),b.y()+42,ACCENT,false);
                g.fill(r.x(),b.y()+59,r.right()-9,b.y()+60,0xFF648B91);
            }
            g.enableScissor(r.x(),r.y(),r.right()-7,r.bottom());
            try{for(var line:lines){int ly=r.y()+line.y()-bodyScroll;if(ly<r.y()||ly+(line.heading()?16:10)>r.bottom())continue;
                if(line.heading()){g.fill(r.x(),ly,r.right()-10,ly+16,0xFF203F4A);g.fill(r.x(),ly,r.x()+2,ly+16,0xFF79D3FF);}
                g.drawString(font,line.text(),r.x()+(line.heading()?6:2),ly+(line.heading()?3:0),line.heading()?0xFFE7F4EE:INK,false);
            }}finally{g.disableScissor();}
            scrollbar(g,r,bodyScroll,bodyHeight);
        }
        if(!layout.compact()&&b.x()+b.width()/2+83+font.width((book.chapters().indexOf(chapter)+1)+" / "+book.chapters().size())+6<footerJumpX)g.drawString(font,(book.chapters().indexOf(chapter)+1)+" / "+book.chapters().size(),b.x()+b.width()/2+83,b.bottom()-23,MUTED,false);
    }
    private void drawBook(GuiGraphics g,GuideLayout.Rect b){
        // Same established PL4 footprint, but a cleaner Calculator-style technical binder: graphite shell, pale pages and cyan rails.
        g.fill(b.x()+5,b.y()+6,b.right()+6,b.bottom()+6,0x66000000);
        g.fill(b.x(),b.y(),b.right(),b.bottom(),0xFF1B252A);
        g.fill(b.x()+2,b.y()+2,b.right()-2,b.bottom()-2,0xFF53656A);
        g.fill(b.x()+5,b.y()+5,b.right()-5,b.bottom()-5,0xFF202C31);
        g.fill(b.x()+8,b.y()+8,b.x()+10,b.bottom()-8,0xFF62C7D6);
        g.fill(b.right()-10,b.y()+8,b.right()-8,b.bottom()-8,0xFF62C7D6);
        if(layout.compact()){
            g.fill(b.x()+14,b.y()+12,b.right()-14,b.bottom()-12,0xFFE8E2C9);
            g.fill(b.x()+15,b.y()+13,b.right()-15,b.y()+14,0xFFBFC5B4);
        }else{
            int spine=b.x()+b.width()/2;
            g.fill(b.x()+14,b.y()+12,spine-8,b.bottom()-12,0xFFE8E2C9);
            g.fill(spine+8,b.y()+12,b.right()-14,b.bottom()-12,0xFFE8E2C9);
            g.fill(spine-7,b.y()+10,spine+7,b.bottom()-10,0xFF2A3438);
            g.fill(spine-4,b.y()+12,spine-2,b.bottom()-12,0xFF778589);
            g.fill(spine+2,b.y()+12,spine+4,b.bottom()-12,0xFF11191C);
            for(int y=b.y()+24;y<b.bottom()-18;y+=19)g.fill(spine-5,y,spine+5,y+1,0xFF69777A);
        }
        g.fill(b.x()+14,b.y()+12,b.right()-14,b.y()+13,0xFFB8C2B2);
        g.fill(b.x()+14,b.bottom()-13,b.right()-14,b.bottom()-12,0xFF9FA99D);
    }
    private void drawSpecimen(GuiGraphics g,GuideLayout.Rect b){
        int x=b.x()+24,y=b.y()+56,w=b.width()/2-50;
        g.fill(x,y,x+w,y+43,0xFF243B46);g.fill(x+1,y+1,x+w-1,y+42,0xFF172A33);
        g.fill(x+10,y+21,x+w-10,y+22,0xFF619CAA);
        for(int i=0;i<4;i++){int px=x+8+i*(w-34)/3;g.fill(px-2,y+6,px+18,y+31,0xFF243B46);g.renderItem(item("foundations_pl4:"+SPECIMEN[i]),px,y+10);}
        g.drawString(font,"DATA  >  READER  >  VIEW",x+6,y+33,0xFFBAE1E5,false);
    }
    private void scrollbar(GuiGraphics g,GuideLayout.Rect r,int scroll,int content){
        if(content<=r.height())return;int thumb=GuideLayout.thumbSize(r.height(),content,r.height()),top=r.y()+GuideLayout.thumbPosition(scroll,r.height(),content,r.height());
        g.fill(r.right()-6,r.y(),r.right()-2,r.bottom(),0xFFB2B7A5);g.fill(r.right()-6,top,r.right()-2,top+thumb,0xFF285F70);
    }
    private boolean beginDrag(GuideLayout.Rect r,int content,int scroll,double x,double y,int which){
        if(content<=r.height()||x<r.right()-8||x>=r.right()||y<r.y()||y>=r.bottom())return false;
        int thumb=GuideLayout.thumbSize(r.height(),content,r.height()),top=r.y()+GuideLayout.thumbPosition(scroll,r.height(),content,r.height());
        if(y>=top&&y<top+thumb){drag=which;dragOffset=y-top;}
        else{int value=GuideLayout.clampScroll(scroll+(y<top?-r.height():r.height()),content,r.height());if(which==1){listScroll=value;refreshList();}else bodyScroll=value;}
        return true;
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){
        if(listVisible()&&layout.list().contains(x,y)){listScroll=GuideLayout.clampScroll(listScroll-(int)Math.signum(dy)*50,filtered.size()*25,layout.list().height());refreshList();return true;}
        if(detailVisible()&&layout.detail().contains(x,y)){bodyScroll=GuideLayout.clampScroll(bodyScroll-(int)Math.signum(dy)*36,bodyHeight,layout.detail().height());return true;}
        return super.mouseScrolled(x,y,dx,dy);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&((listVisible()&&beginDrag(layout.list(),filtered.size()*25,listScroll,x,y,1))||(detailVisible()&&beginDrag(layout.detail(),bodyHeight,bodyScroll,x,y,2))))return true;
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button==0&&drag!=0){var rect=drag==1?layout.list():layout.detail();int value=GuideLayout.scrollFromThumb(y-rect.y()-dragOffset,rect.height(),drag==1?filtered.size()*25:bodyHeight,rect.height());
            if(drag==1){listScroll=value;refreshList();}else bodyScroll=value;return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){boolean handled=drag!=0;drag=0;return super.mouseReleased(x,y,button)||handled;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==256){onClose();return true;}
        if(search==null||!search.isFocused()){
            if(key==268){query="";savedOnly=false;listScroll=0;select(GuideNavigation.byId(book,"start"));return true;}
            if(key==266){bodyScroll=GuideLayout.clampScroll(bodyScroll-layout.detail().height(),bodyHeight,layout.detail().height());return true;}
            if(key==267){bodyScroll=GuideLayout.clampScroll(bodyScroll+layout.detail().height(),bodyHeight,layout.detail().height());return true;}
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void removed(){if(preferences!=null){preferences.chapter=chapter.id();GuideResources.save(preferences);}drag=0;chapterButtons.clear();icons.clear();super.removed();}
}
