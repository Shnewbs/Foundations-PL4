import java.util.*;
import net.foundations.pl4.core.*;

/** R3 editor metadata, transactional boundaries, and channel isolation. */
public final class R3RegressionTests {
    static int assertions;
    static void check(boolean condition,String reason){assertions++;if(!condition)throw new AssertionError(reason);}
    public static void main(String[] args){
        var a=DisplayElements.create(DisplayElements.Type.TEXT,0);
        var b=DisplayElements.create(DisplayElements.Type.BAR,0);
        var other=DisplayElements.create(DisplayElements.Type.TEXT,1);
        var state=new LayoutTransactions.State(List.of(a,b,other),DisplayElements.Mode.CUSTOM,0,0);
        var ids=List.of(a.id(),b.id());
        var group=LayoutTransactions.applyOrganization(state,0,"group",ids);
        check(group.accepted(),"group accepted");var g=group.state();
        check(!g.elements().get(0).options().group().isEmpty()&&g.elements().get(0).options().group().equals(g.elements().get(1).options().group()),"same group");
        check(g.elements().get(2).equals(other),"other pages untouched");
        check(!LayoutTransactions.applyOrganization(g,0,"hide",ids).accepted(),"stale rejected");
        check(!LayoutTransactions.applyOrganization(g,1,"hide",List.of(other.id())).accepted(),"other page rejected");
        check(!LayoutTransactions.applyOrganization(g,1,"hide",List.of(a.id(),a.id())).accepted(),"duplicates rejected");
        var hidden=LayoutTransactions.applyOrganization(g,1,"hide",ids).state();
        check(DisplayElements.plan(hidden.elements().getFirst(),List.of()).draws().isEmpty(),"hidden does not render");
        check(EditorSelection.inBox(hidden.elements(),0,new DisplayElements.Rect(0,0,248,120)).isEmpty(),"hidden not marquee picked");
        var locked=LayoutTransactions.applyOrganization(g,1,"lock",ids).state();
        check(!LayoutTransactions.applyMove(locked,2,ids,4,4,248,120).accepted(),"locked cannot move");
        check(!LayoutTransactions.applyArrange(locked,2,"align_left",ids,248,120).accepted(),"locked cannot arrange");
        check(!LayoutTransactions.applyLayers(locked,2,"layer_front",List.of(a.id())).accepted(),"locked cannot reorder");
        check(!LayoutTransactions.apply(locked,2,"update",a.id(),a.bounds(new DisplayElements.Rect(0,0,20,20)),"").accepted(),"locked cannot update");
        check(!LayoutTransactions.apply(locked,2,"delete",a.id(),null,"").accepted(),"locked cannot delete");
        check(!LayoutTransactions.apply(locked,2,"page_clear",null,null,"").accepted(),"locked cannot page-clear");
        check(!LayoutTransactions.apply(locked,2,"clear",null,null,"").accepted(),"locked cannot clear all");
        var unlocked=LayoutTransactions.applyOrganization(locked,2,"unlock",ids);
        check(unlocked.accepted()&&!unlocked.state().elements().getFirst().options().locked(),"unlock available");
        check(LayoutTransactions.applyReplace(locked,2,state.elements()).accepted(),"authorized undo restores pre-lock snapshot");
        var styled=a.options(new DisplayElements.Options("group",true,false,0x112233,0x445566,7));
        check(styled.bounds(new DisplayElements.Rect(10,10,40,20)).options().equals(styled.options()),"move preserves metadata");
        check(styled.identity(UUID.randomUUID()).onPage(3).textStyle(DisplayElements.TextAlign.CENTER,true,2).options().equals(styled.options()),"copies/style changes preserve metadata");
        check(DisplayElements.plan(styled,List.of()).draws().stream().filter(d->d instanceof DisplayElements.Box).count()==2,"background and border render");
        check(new DisplayElements.Options(null,false,false,-100,-2,99).equals(new DisplayElements.Options("",false,false,-1,-1,7)),"metadata bounds");
        for(String out:List.of("","ore","smelter","ORE"))for(String in:List.of("","ore","smelter","ORE"))check(TransferRules.channelsMatch(out,in)==out.equals(in),"exact channel isolation");
        for(int tick=0;tick<400;tick++)check(ClockRules.high(tick,40,10,0,0,false)==(tick%40<10),"legacy clock pulse");
        check(!ClockRules.high(0,40,10,20,0,true),"paused clock low");
        check(ClockRules.high(30,40,10,5,10,false),"phase wraps");
        check(!ClockRules.high(35,40,10,5,10,false),"custom pulse end");
        check(ClockRules.period(1,10)==20&&ClockRules.period(99999,10)==24000,"period bounds");
        check(ClockRules.high(Long.MIN_VALUE,40,10,24000,23999,false),"overflow-safe phase math and pulse cap");
        System.out.println("PASS R3: "+assertions+" assertions");
    }
}
