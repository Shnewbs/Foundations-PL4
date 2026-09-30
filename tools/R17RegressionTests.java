import net.foundations.pl4.core.*;

/** Emitter-anchored hologram geometry checks without a Minecraft classpath. */
public final class R17RegressionTests {
    private static int count;
    private static void ok(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        for(int mount=0;mount<6;mount++)for(int requested=2;requested<6;requested++)for(boolean advanced:new boolean[]{false,true}){
            var geometry=HologramProjection.geometry(mount,requested,advanced);
            var normal=DisplayFacing.facing(mount^1).normal();
            var viewNormal=DisplayFacing.facing(HologramProjection.view(mount,requested)).normal();
            var centre=geometry.centre();
            double along=(centre.x()-geometry.emitterAnchor().x())*normal.x()
                +(centre.y()-geometry.emitterAnchor().y())*normal.y()
                +(centre.z()-geometry.emitterAnchor().z())*normal.z();
            ok(geometry.normal().equals(normal),"Projection follows attachment axis independently of view");
            ok(Math.abs(along-HologramProjection.clearance(advanced))<1e-12,
                "Projection clearance is measured from emitter hardware");
            var anchor=geometry.emitterAnchor();
            if(mount<2){
                ok(Math.abs(centre.x()-anchor.x())<1e-12 && Math.abs(centre.z()-anchor.z())<1e-12,
                    "Floor/ceiling canvas is centered on emitter, without sideways displacement");
                double edge=centre.y()+(mount==0?-.21:.21);
                ok(mount==0?edge>anchor.y():edge<anchor.y(),"Entire canvas clears the emitter");
            }
            ok(anchor.x()>=0&&anchor.x()<=1&&anchor.y()>=0&&anchor.y()<=1&&anchor.z()>=0&&anchor.z()<=1,
                "Emitter anchor remains on projector hardware");
            for(int side:new int[]{-1,1}){
                var eye=new HologramProjection.Point(centre.x()+viewNormal.x()*side*2,
                    centre.y()+viewNormal.y()*side*2,centre.z()+viewNormal.z()*side*2);
                var projection=HologramProjection.forCamera(mount,requested,advanced,eye);
                ok(projection.centre().equals(centre),"Camera side cannot move projection origin");
                ok(projection.frame().right().cross(projection.frame().up()).equals(projection.frame().normal()),
                    "Readable frame remains right handed");
            }
        }
        for(int mount=0;mount<6;mount++){
            var anchor=HologramProjection.geometry(mount,3,false).mountAnchor();
            ok(anchor.x()>=0&&anchor.x()<=1&&anchor.y()>=0&&anchor.y()<=1&&anchor.z()>=0&&anchor.z()<=1,
                "Mount anchor remains within host geometry");
        }
        System.out.println("PASS R17 emitter-anchored hologram rules: "+count+" assertions.");
    }
}
