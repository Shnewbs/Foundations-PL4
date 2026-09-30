import net.foundations.pl4.core.*;
import java.util.*;
public final class R11RegressionTests {
  static int checks; static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);} 
  static void spaces(){
    for(int w=1;w<=16;w++)for(int h=1;h<=16;h++){
      var s=DynamicCanvasLayout.large(w,h);check(s.width()>=96&&s.height()>=96,"bounded");
      double pw=s.width()*s.scale(),ph=s.height()*s.scale();
      check(pw<=w-.12+1e-6&&ph<=h-.12+1e-6,"inside frame");
      double expected=(w-.12)/(h-.12),actual=(double)s.width()/s.height();check(Math.abs(actual-expected)/expected<.004,"whole-board aspect");
    }
    var a=DynamicCanvasLayout.large(2,2);var b=DynamicCanvasLayout.large(6,2);
    check(b.width()>a.width()*2.9,"wide joined board gains horizontal workspace");
    check(Math.abs(b.height()-a.height())<=1,"same row count keeps height");
  }
  static void migration(){
    var old=new DisplayElements.Rect(124,60,62,30);
    var migrated=DynamicCanvasLayout.migrate(old,248,120,DynamicCanvasLayout.large(6,2).width(),DynamicCanvasLayout.large(6,2).height());
    double cx=(migrated.x()+migrated.width()/2.0)/DynamicCanvasLayout.large(6,2).width();
    double cy=(migrated.y()+migrated.height()/2.0)/DynamicCanvasLayout.large(6,2).height();
    check(Math.abs(cx-(old.x()+old.width()/2.0)/248)<.01,"relative x preserved");
    check(Math.abs(cy-(old.y()+old.height()/2.0)/120)<.01,"relative y preserved");
    for(var t:DisplayElements.Type.values()){
      var s=DisplayElements.create(t,0,900,300);var m=DisplayElements.move(s,800,250,false,false,900,300);
      check(m.bounds().right()<=900&&m.bounds().bottom()<=300,"dynamic move clamp "+t);
      var r=DisplayElements.move(m,500,500,true,false,900,300);check(r.bounds().right()<=900&&r.bounds().bottom()<=300,"dynamic resize clamp "+t);
    }
  }
  static void styledText(){
    var base=DisplayElements.create(DisplayElements.Type.TEXT,0,248,120);
    var styled=base.textStyle(DisplayElements.TextAlign.CENTER,true,2F);
    check(styled.bounds(base.bounds()).textAlign()==DisplayElements.TextAlign.CENTER&&styled.onPage(1).wrap()&&styled.identity(styled.id()).textScale()==2F,"element copies preserve text style and scale");
    var draw=(DisplayElements.Text)DisplayElements.plan(styled,List.of()).draws().getFirst();
    check(draw.alignment()==DisplayElements.TextAlign.CENTER&&draw.wrap()&&draw.scale()==2F&&draw.height()==styled.bounds().height(),"text scene carries alignment, wrapping, scale and clipping height");
    check(DisplayElements.TextAlign.parse("right")==DisplayElements.TextAlign.RIGHT&&DisplayElements.TextAlign.parse("unknown")==DisplayElements.TextAlign.LEFT,"alignment parsing is case-insensitive and safely defaults");
    check(base.textScale()==1F,"default text scale is 1x");
    check(base.textStyle(DisplayElements.TextAlign.LEFT,false,0F).textScale()==1F&&base.textStyle(DisplayElements.TextAlign.LEFT,false,99F).textScale()==DisplayElements.MAX_TEXT_SCALE,"text scale clamps to a safe finite range");
  }
  public static void main(String[] args){spaces();migration();styledText();System.out.println("PASS R11/R1 display rules: "+checks+" assertions. NOT native rendering/API acceptance.");}
}
