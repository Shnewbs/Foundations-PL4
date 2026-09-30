import java.util.*;
import net.foundations.pl4.core.*;

public final class R13RegressionTests {
  static long checks;
  static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
  static DisplayElements.Spec spec(int x,int y,int w,int h){return new DisplayElements.Spec(UUID.randomUUID(),DisplayElements.Type.BAR,"","","","",new DisplayElements.Rect(x,y,w,h),0xFFFFFF,true,false,8,0,0,false,false);}
  static void toolbar(){
    for(int i=0;i<8;i++){
      var r=EditorChrome.toolRect(i);check(r.x()==EditorChrome.TOOLBAR_X&&r.y()==EditorChrome.TOOLBAR_Y+i*EditorChrome.TOOLBAR_STEP,"toolbar geometry "+i);
      check(EditorChrome.toolAt(r.x()+r.width()/2.0,r.y()+r.height()/2.0,8)==i,"toolbar center hit "+i);
      if(i<7)check(EditorChrome.toolAt(r.x()+2,r.bottom()+.4,8)==-1,"toolbar gap rejects "+i);
      check(EditorChrome.toolColor(i,i==6,false)!=0,"tool color "+i);
    }
    check(EditorChrome.toolAt(-1,5,8)==-1&&EditorChrome.toolAt(100,5,8)==-1,"toolbar outside rejects");
    check(EditorChrome.toolColor(0,false,false)!=EditorChrome.toolColor(2,false,false),"add and delete are visually distinct");
  }
  static void corners(){
    var s=spec(40,30,80,50);var b=s.bounds();
    for(var c:new EditorChrome.Corner[]{EditorChrome.Corner.NW,EditorChrome.Corner.NE,EditorChrome.Corner.SW,EditorChrome.Corner.SE}){
      var h=EditorChrome.handleRect(b,c);check(h.width()>=6&&h.height()>=6,"large visible handle "+c);check(EditorChrome.cornerAt(b,h.x()+h.width()/2.0,h.y()+h.height()/2.0)==c,"corner hit "+c);
    }
    check(EditorChrome.cornerAt(b,80,55)==EditorChrome.Corner.NONE,"center is move area");
    var nw=EditorChrome.resize(s,EditorChrome.Corner.NW,-8,-4,false,240,140);check(nw.bounds().x()==32&&nw.bounds().y()==26&&nw.bounds().right()==120&&nw.bounds().bottom()==80,"NW resize anchors opposite corner");
    var ne=EditorChrome.resize(s,EditorChrome.Corner.NE,12,-7,false,240,140);check(ne.bounds().x()==40&&ne.bounds().y()==23&&ne.bounds().right()==132&&ne.bounds().bottom()==80,"NE resize anchors SW");
    var sw=EditorChrome.resize(s,EditorChrome.Corner.SW,-9,13,false,240,140);check(sw.bounds().x()==31&&sw.bounds().y()==30&&sw.bounds().right()==120&&sw.bounds().bottom()==93,"SW resize anchors NE");
    var se=EditorChrome.resize(s,EditorChrome.Corner.SE,15,20,false,240,140);check(se.bounds().x()==40&&se.bounds().y()==30&&se.bounds().right()==135&&se.bounds().bottom()==100,"SE resize anchors NW");
    for(var c:new EditorChrome.Corner[]{EditorChrome.Corner.NW,EditorChrome.Corner.NE,EditorChrome.Corner.SW,EditorChrome.Corner.SE}){
      var r=EditorChrome.resize(s,c,-10000,-10000,true,240,140).bounds();check(r.x()>=0&&r.y()>=0&&r.right()<=240&&r.bottom()<=140&&r.width()>=8&&r.height()>=9,"resize clamp "+c);
      r=EditorChrome.resize(s,c,10000,10000,true,240,140).bounds();check(r.x()>=0&&r.y()>=0&&r.right()<=240&&r.bottom()<=140&&r.width()>=8&&r.height()>=9,"resize positive clamp "+c);
    }
  }
  static void depth(){
    double prev=DisplayElements.worldDepth(4);for(int i=5;i<=8;i++){double d=DisplayElements.worldDepth(i);check(d>prev,"editor chrome strict plane "+i);prev=d;}
    check(DisplayElements.worldDepth(8)<.006,"editor chrome remains below .006 block");
  }
  static void itemRules(){
    check(!PartItemDataRules.needsEscrowPayload(false,false,0),"ordinary drop needs no custom payload");
    check(PartItemDataRules.needsEscrowPayload(true,false,0),"item escrow retained");
    check(PartItemDataRules.needsEscrowPayload(false,true,0),"fluid escrow retained");
    check(PartItemDataRules.needsEscrowPayload(false,false,1),"energy escrow retained");
    var keys=Set.of(PartItemDataRules.VOLATILE_KEYS);check(keys.containsAll(Set.of("identity","owner","signal","ticks","layoutRevision")),"volatile item fields declared");
  }
  public static void main(String[] args){toolbar();corners();depth();itemRules();System.out.println("PASS R13 editor chrome/resize/item-drop rules: "+checks+" assertions. NOT native framebuffer/NBT acceptance.");}
}
