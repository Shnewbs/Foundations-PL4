import java.util.*;
import net.foundations.pl4.core.*;

public final class R14RegressionTests {
  static long checks;
  static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
  record Sample(String key,String name,double value,double capacity,String unit,boolean item,boolean block,boolean fluid) implements DisplayElements.Sample {
    public boolean hasItem(){return item;}public boolean hasBlock(){return block;}public boolean hasFluid(){return fluid;}
    public String itemId(){return item?key:"";}public String fluidId(){return fluid?key:"";}
  }
  static DisplayElements.Spec grid(int columns){return new DisplayElements.Spec(UUID.randomUUID(),DisplayElements.Type.INVENTORY,"","","","",new DisplayElements.Rect(0,0,90,90),0x79D3FF,true,false,columns,0,0,false,true);}
  static void columns(){
    check(DisplayElements.defaultColumns(DisplayElements.Type.INVENTORY)==3,"inventory grids default to three columns");
    check(DisplayElements.defaultColumns(DisplayElements.Type.FLUID_GRID)==3,"fluid grids default to three columns");
    check(DisplayElements.defaultColumns(DisplayElements.Type.ITEM)==1,"single pictures do not pretend to be an eight-column grid");
    var rows=List.of(new Sample("a","A",1,0,"",true,false,false),new Sample("b","B",1,0,"",true,false,false),new Sample("c","C",1,0,"",true,false,false),new Sample("d","D",1,0,"",true,false,false));
    var three=DisplayElements.plan(grid(3),rows).draws().stream().filter(d->d instanceof DisplayElements.Icon).map(d->((DisplayElements.Icon)d).rect()).toList();
    check(three.size()==4,"four inventory pictures rendered");
    check(three.get(0).y()==three.get(1).y()&&three.get(1).y()==three.get(2).y(),"three columns share first row");
    check(three.get(3).y()>three.get(0).y(),"fourth picture wraps to second row");
    var one=DisplayElements.plan(grid(1),rows).draws().stream().filter(d->d instanceof DisplayElements.Icon).map(d->((DisplayElements.Icon)d).rect()).toList();
    check(one.get(1).y()>one.get(0).y(),"one-column setting changes real layout");
  }
  static void palette(){
    check(EditorPalette.PRESETS.size()>=20,"visual palette has useful coverage");
    check(EditorPalette.hex(EditorPalette.DEFAULT).equals("79D3FF"),"PL4 default hex");
    check(EditorPalette.parse("#ff6b6b",0)==0xFF6B6B,"hash hex parses");
    check(EditorPalette.parse("bad hex",0x123456)==0x123456,"invalid input keeps fallback");
    check(EditorPalette.PRESETS.stream().map(EditorPalette.Swatch::rgb).distinct().count()==EditorPalette.PRESETS.size(),"preset colors are unique");
  }
  public static void main(String[] args){columns();palette();System.out.println("PASS R14 columns/palette rules: "+checks+" assertions. UI/native acceptance still separate.");}
}
