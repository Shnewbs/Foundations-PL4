import net.foundations.pl4.core.DisplayElements;
import java.util.*;
public final class R12RegressionTests {
  static int checks; static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);} 
  record S(String key,String name,double value,double capacity,String unit) implements DisplayElements.Sample{
    public boolean hasItem(){return false;} public boolean hasBlock(){return false;} public boolean hasFluid(){return false;}
  }
  static boolean overlap(DisplayElements.Rect a,DisplayElements.Rect b){return a.x()<b.right()&&b.x()<a.right()&&a.y()<b.bottom()&&b.y()<a.bottom();}
  static void barPlanes(){
    var spec=new DisplayElements.Spec(UUID.randomUUID(),DisplayElements.Type.BAR,"","","energy","",new DisplayElements.Rect(4,4,120,20),0x55CCFF,true,false,8,0,0,false,true);
    var scene=DisplayElements.plan(spec,List.of(new S("energy","Stored",337_000_000,687_000_000,"EU")));
    var boxes=scene.draws().stream().filter(DisplayElements.Box.class::isInstance).map(DisplayElements.Box.class::cast).toList();
    check(boxes.stream().anyMatch(b->b.filled()&&b.layer()==1),"bar base on plane 1");
    check(boxes.stream().anyMatch(b->b.filled()&&b.layer()==2),"bar fill on plane 2");
    check(boxes.stream().anyMatch(b->!b.filled()&&b.layer()==3),"bar frame on plane 3");
    for(int i=0;i<boxes.size();i++)for(int j=i+1;j<boxes.size();j++)if(boxes.get(i).filled()&&boxes.get(j).filled()&&overlap(boxes.get(i).rect(),boxes.get(j).rect()))check(boxes.get(i).layer()!=boxes.get(j).layer(),"overlapping filled bar quads cannot share a plane");
    check(scene.draws().stream().filter(DisplayElements.Text.class::isInstance).map(DisplayElements.Text.class::cast).anyMatch(DisplayElements.Text::overlay),"bar amount is overlay text");
  }
  static void depthBudget(){
    double previous=DisplayElements.worldDepth(0);for(int layer=1;layer<=5;layer++){double d=DisplayElements.worldDepth(layer);check(d>previous,"strict depth plane order "+layer);previous=d;}
    check(DisplayElements.worldDepth(5)<.004,"top editor plane remains visually shallow");
    for(double scale:new double[]{.0018,.0035,.01,.06,1})for(int layer=0;layer<=5;layer++)check(Math.abs(DisplayElements.logicalDepth(layer,scale)*scale-DisplayElements.worldDepth(layer))<1e-12,"scale-normalized plane "+layer);
  }
  public static void main(String[] args){barPlanes();depthBudget();System.out.println("PASS R12 display-plane separation: "+checks+" assertions. NOT native framebuffer/graphics acceptance.");}
}
