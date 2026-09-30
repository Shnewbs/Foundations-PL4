import java.util.*;
import net.foundations.pl4.core.*;

/** Executes the same presentation/layout/navigation functions used by client and server.
 * It is deliberately not a native render or item-pose acceptance test. */
public final class R10RegressionTests {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    record Sample(String key,String name,double value,double capacity,String unit,boolean hasItem,boolean hasBlock,boolean hasFluid) implements DisplayElements.Sample {}
    static void areas(){
        for(int w=1;w<=16;w++)for(int h=1;h<=16;h++){
            double legacyScale=Math.min((w-.12)/248.0,(h-.12)/120.0);
            var a=MonitorPresentation.area(true,w,h,legacyScale);var space=DynamicCanvasLayout.large(w,h);
            check(a.x()==0&&a.y()==0&&a.width()==space.width()&&a.height()==space.height(),"R11 dynamic area follows joined board");
            check(a.width()*space.scale()<=w-.12+1e-6&&a.height()*space.scale()<=h-.12+1e-6,"Dynamic canvas stays inside frame");
            var t=MonitorPresentation.table(a,100);
            check(t.titleY()==a.y()+6&&t.titleY()>=a.y(),"Title belongs at physical upper edge");
            check(t.firstRowY()>t.ruleY()+9&&t.ruleY()>t.titleY()+8,"Heading, labels, rows do not overlap");
            check(t.rows()>0&&t.rows()<=24,"Bound automatic rows");
            check(t.rowY(t.rows()-1)+9<t.footerY()-4,"Whole final row above footer rule");
            check(t.footerY()+9<a.bottom()&&t.contentWidth()>20,"Footer inside panel");
        }
        var square=MonitorPresentation.area(true,3,3,(3-.12)/248);check(square.x()==0&&square.y()==0,"Joined surface origin");
        check(MonitorPresentation.area(false,3,3,.0035).equals(new MonitorPresentation.Region(0,0,248,120)),"Ordinary custom canvas units remain");
        var none=MonitorPresentation.table(square,0);check(none.rows()==0,"No invented rows for empty data");
        for(double bad:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY}){boolean failed=false;try{MonitorPresentation.area(true,1,1,bad);}catch(IllegalArgumentException e){failed=true;}check(failed,"Reject invalid scale");}
        check(MonitorPresentation.title("","inventoryreader (3 x 3)").equals("Inventory"),"Readable automatic title");
        check(MonitorPresentation.title("My tank","fluidreader").equals("My tank"),"Preserve user title");
        check(MonitorPresentation.amount(new Sample("stone","Stone",1,0,"items",true,true,false)).equals("1"),"No '1 items' suffix");
        check(MonitorPresentation.amount(new Sample("power","Power",2500,10000,"EU",false,false,false)).endsWith(" EU"),"Keep energy units");
        check(MonitorPresentation.footer(17,128).equals("17 / 128 rows  /  AUTO LIST"),"Do not imply hidden rows absent");
    }
    static void views(){
        for(var mode:DisplayElements.Mode.values()){
            check(!MonitorPresentation.automatic(mode,true),"Editing always previews custom content");
            check(MonitorPresentation.automatic(mode,false)==(mode==DisplayElements.Mode.AUTO_LIST),"Resting view is explicit");
        }
        for(var type:DisplayElements.Type.values())for(int page=0;page<8;page++){
            var spec=DisplayElements.create(type,page);
            var before=new LayoutTransactions.State(List.of(),DisplayElements.Mode.AUTO_LIST,0,41);
            var add=LayoutTransactions.apply(before,41,"add",spec.id(),spec,"");
            check(add.accepted()&&add.state().mode()==DisplayElements.Mode.CUSTOM&&add.state().page()==page,"Add reveals requested page and custom mode");
            var moved=spec.onPage((page+1)%8);var edit=LayoutTransactions.apply(add.state(),42,"update",spec.id(),moved,"");
            check(edit.accepted()&&edit.state().page()==moved.page(),"Editing page reveals edited element");
            var stale=LayoutTransactions.apply(edit.state(),42,"update",spec.id(),spec,"");
            check(!stale.accepted()&&stale.state().equals(edit.state()),"Stale edit cannot change page");
            var auto=LayoutTransactions.apply(edit.state(),43,"mode",spec.id(),null,"AUTO_LIST");
            check(auto.accepted()&&auto.state().elements().equals(edit.state().elements()),"Switching view never discards layout");
        }
        var stone=new Sample("minecraft:stone","Stone",22,0,"items",true,true,false);
        var scene=DisplayElements.plan(DisplayElements.create(DisplayElements.Type.BLOCK,0),List.of(stone));
        check(scene.draws().stream().anyMatch(d->d instanceof DisplayElements.Icon i&&i.block()),"Real block model dispatch retained");
        check(scene.draws().stream().anyMatch(d->d instanceof DisplayElements.Text t&&t.overlay()&&t.value().equals("22")),"Counter kept above icon");
        check(DisplayElements.worldDepth(3)>DisplayElements.worldDepth(2)&&DisplayElements.worldDepth(3)<.002,"Shallow editor above counters");
    }
    static GuideBook.Chapter ch(String id,String cat){return new GuideBook.Chapter(id,cat,id,"summary","foundations_pl4:plguide",List.of(new GuideBook.Section("TITLE","body")));}
    static void navigation(){
        var book=new GuideBook("PL4","R10",List.of(ch("start","start"),ch("tutorial_inventory","start"),ch("cables","network"),ch("panels","display"),ch("kubejs","reference")));
        var current=GuideNavigation.byId(book,"kubejs");
        check(GuideNavigation.section(book,"start",current).id().equals("start"),"Start tab cannot leave KubeJS on right");
        check(GuideNavigation.section(book,"reference",current)==current,"Reselecting category keeps current chapter");
        check(GuideNavigation.byId(book,"tutorial_inventory").category().equals("start"),"Tutorial jump");
        check(GuideNavigation.byId(book,"missing").id().equals("start"),"Missing override chapter falls back safely");
        check(GuideNavigation.heading("start").contains("TUTORIAL"),"Onboarding category");
        for(var c:book.chapters())check(GuideNavigation.section(book,c.category(),current).category().equals(c.category()),"Every tab reads matching category");
        for(int width:new int[]{320,400,500,640,900,1280,1920})for(int height:new int[]{240,300,400,600,1080}){
            var l=GuideLayout.fit(width,height);check(l.book().x()>=0&&l.book().right()<=width,"Book horizontal fit");
            check(l.detail().bottom()<=l.book().bottom()-20&&l.list().bottom()<=l.book().bottom()-20,"Page contents above footer");
        }
    }
    public static void main(String[] args){areas();views();navigation();System.out.println("PASS R10 production presentation/navigation/page tests: "+checks+" assertions. NOT native rendering/API acceptance.");}
}
