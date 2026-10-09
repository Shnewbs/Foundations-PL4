import java.util.*;
import net.foundations.pl4.core.*;

/** Executable tests call production math/model code; no Minecraft or fake API classpath. */
public final class R8RegressionTests {
    private static int count;
    private static void ok(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    private static void bad(Runnable action,String message){boolean failed=false;try{action.run();}catch(IllegalArgumentException|IllegalStateException e){failed=true;}ok(failed,message);}
    public static void main(String[] args){placement();continuity();holograms();guide();System.out.println("PASS R8 production rules: "+count+" assertions (edge/rim placement, canvas limits/donors, hologram camera frames, guide search/layout/scroll). No native Minecraft execution.");}
    private static void placement(){
        for(int mount=0;mount<6;mount++)for(boolean outward:new boolean[]{false,true}){
            var frame=DisplayFacing.frame(mount,outward);
            for(int hit=0;hit<6;hit++){
                var value=DisplayPlacement.extension(mount,outward,hit,.5,.5,.5);
                if((hit>>1)!=(mount>>1))ok(value.orElseThrow()==hit,"Physical edge keeps original plane");else ok(value.isEmpty(),"Centre does not invent an extension");
            }
            for(var axis:List.of(frame.right(),frame.right().negate(),frame.up(),frame.up().negate()))for(int hit:List.of(mount,mount^1)){
                int expected=DisplayFacing.direction(axis);
                for(double edge:new double[]{.31,.4,.49999}){
                    var value=DisplayPlacement.extension(mount,outward,hit,.5+axis.x()*edge,.5+axis.y()*edge,.5+axis.z()*edge);
                    ok(value.orElseThrow()==expected,"Front/back rim chooses tangent, not surface normal");ok((expected>>1)!=(mount>>1),"Always coplanar");
                }
            }
            ok(DisplayPlacement.extension(mount,outward,mount,.55,.55,.55).isEmpty(),"Interior remains ambiguous");
        }
        ok(DisplayPlacement.extension(0,true,2,Double.NaN,.5,.5).isEmpty(),"Reject NaN");
        ok(DisplayPlacement.extension(9,true,2,.5,.5,.5).isEmpty(),"Reject bad face");
        ok(DisplayPlacement.extension(0,true,2,999,.5,.5).isEmpty(),"Reject invalid location");
        for(int w=1;w<=17;w++)for(int h=1;h<=17;h++){
            List<DisplayLayout.Cell> cells=new ArrayList<>();for(int x=0;x<w;x++)for(int y=0;y<h;y++)cells.add(new DisplayLayout.Cell(x-8,y-8));
            ok(DisplayPlacement.withinLimits(cells)==(w<=16&&h<=16),"Canvas maximum sides");
            if(w>1&&h>1&&w<=16&&h<=16){net.foundations.pl4.compat.PortLists.removeLast(cells);ok(DisplayPlacement.withinLimits(cells),"Temporary L/hole may be completed");ok(DisplayLayout.rectangle(cells).isEmpty(),"Still no false rectangular join");}
        }
        ok(!DisplayPlacement.withinLimits(List.of(new DisplayLayout.Cell(Integer.MIN_VALUE,0),new DisplayLayout.Cell(Integer.MAX_VALUE,0))),"Extent math does not overflow");
    }
    private static void continuity(){
        var old=new CanvasContinuity.Candidate("original",5,true,true,false);
        var blank=new CanvasContinuity.Candidate("new-top-left",0,false,false,true);
        ok(CanvasContinuity.donor(List.of(blank,old)).equals("original"),"Expanding up/left keeps configured donor");
        var newer=new CanvasContinuity.Candidate("newer",6,true,false,false);
        ok(CanvasContinuity.donor(List.of(old,blank,newer)).equals("newer"),"Merging picks newest revision");
        var cleared=new CanvasContinuity.Candidate("cleared",7,false,false,false);
        ok(CanvasContinuity.donor(List.of(newer,cleared)).equals("cleared"),"Explicit newer clear is not undone");
        var survivor=new CanvasContinuity.Candidate("survivor",5,true,false,true);
        ok(CanvasContinuity.donor(List.of(survivor,blank)).equals("survivor"),"Mirrored survivor retains donor revision");
        var a=new CanvasContinuity.Candidate("a",3,true,false,false);var b=new CanvasContinuity.Candidate("b",3,true,false,false);
        ok(CanvasContinuity.donor(List.of(a,b)).equals(CanvasContinuity.donor(List.of(b,a))),"Reload order cannot change a tie");
        ok(CanvasContinuity.next(99)==100,"Revision monotonic");bad(()->CanvasContinuity.next(Long.MAX_VALUE),"Overflow cannot wrap");bad(()->CanvasContinuity.donor(List.of()),"No empty donor");
    }
    private static void holograms(){
        for(int mount=0;mount<6;mount++)for(int requested=2;requested<6;requested++)for(boolean advanced:new boolean[]{false,true}){
            var centre=HologramProjection.centre(mount,requested,advanced);var configured=DisplayFacing.facing(HologramProjection.view(mount,requested));
            for(int side:new int[]{-1,1}){
                var camera=new HologramProjection.Point(centre.x()+configured.normal().x()*side*3,centre.y()+configured.normal().y()*side*3,centre.z()+configured.normal().z()*side*3);
                var projection=HologramProjection.forCamera(mount,requested,advanced,camera);var frame=projection.frame();
                ok(frame.right().cross(frame.up()).equals(frame.normal()),"Readable frame is right handed, never mirrored");
                ok(frame.up().equals(new DisplayFacing.Vector(0,1,0)),"Text remains upright on floor, ceiling and wall");
                double facing=(camera.x()-centre.x())*frame.normal().x()+(camera.y()-centre.y())*frame.normal().y()+(camera.z()-centre.z())*frame.normal().z();
                ok(facing>0,"Only readable camera side submitted");ok(projection.centre().equals(centre),"Back view does not move the projection");
            }
            var geometry=HologramProjection.geometry(mount,requested,advanced);
            double delta=(centre.x()-geometry.emitterAnchor().x())*geometry.normal().x()
                +(centre.y()-geometry.emitterAnchor().y())*geometry.normal().y()
                +(centre.z()-geometry.emitterAnchor().z())*geometry.normal().z();
            ok(delta>0,"Project away from the physical emitter");
            if(mount<2){int v=requested;Set<Integer> seen=new HashSet<>();for(int i=0;i<4;i++){seen.add(v);v=HologramProjection.nextView(mount,v);}ok(v==requested&&seen.size()==4,"Four-way view control");}
        }
    }
    private static void guide(){
        var section=new GuideBook.Section("POWER","Use an Energy Reader for Joules, not an Inventory Reader.");
        var chapter=new GuideBook.Chapter("power","network","Energy Reader","Native units","foundations_pl4:energyreader",List.of(section));
        var other=new GuideBook.Chapter("screens","display","Large Displays","Expand the edge","foundations_pl4:largedisplayscreen",List.of(new GuideBook.Section("EDGE","Preserve the plane")));
        var book=new GuideBook("Field Guide","PL4 / 1.21.1",List.of(chapter,other));
        ok(book.search("start","joules",Set.of(),false).equals(List.of(chapter)),"Search body across categories");
        ok(book.search("network","",Set.of(),false).equals(List.of(chapter)),"Empty search respects section");
        ok(book.search("display","reader",Set.of("screens"),true).isEmpty(),"Bookmark and query both apply");
        ok(book.search("network","",Set.of("power"),true).equals(List.of(chapter)),"Bookmark filter");
        bad(()->new GuideBook("x","x",List.of(chapter,chapter)),"Duplicate IDs rejected");
        bad(()->new GuideBook.Chapter("../x","network","x","x","minecraft:book",List.of(section)),"Invalid IDs rejected");
        bad(()->new GuideBook.Section("x","x".repeat(8193)),"Bound content");
        for(int width:new int[]{320,418,500,559,854,1280,1920})for(int height:new int[]{180,240,299,360,720,1080}){
            var l=GuideLayout.fit(width,height);var b=l.book();ok(b.x()>=23&&b.right()<=width&&b.y()>=0&&b.bottom()<=height,"Book and side tabs fit");
            for(var r:List.of(l.list(),l.detail()))ok(r.x()>=b.x()&&r.right()<=b.right()&&r.y()>=b.y()&&r.bottom()<b.bottom(),"Text stays inside page");
            if(!l.compact())ok(l.list().right()<l.detail().x(),"Binding safe area never overlapped");
            for(int content:new int[]{0,10,100,500,4000}){
                var r=l.detail();int max=Math.max(0,content-r.height());
                ok(GuideLayout.clampScroll(-1,content,r.height())==0,"Scroll lower bound");ok(GuideLayout.clampScroll(Integer.MAX_VALUE,content,r.height())==max,"Scroll upper bound");
                int end=GuideLayout.thumbPosition(max,r.height(),content,r.height());
                ok(end+GuideLayout.thumbSize(r.height(),content,r.height())<=r.height(),"Thumb inside track");
                if(content>r.height()&&r.height()>12)ok(Math.abs(GuideLayout.scrollFromThumb(end,r.height(),content,r.height())-max)<=1,"Drag reaches bottom");
            }
        }
    }
}
