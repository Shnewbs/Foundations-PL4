import net.foundations.pl4.core.*;

/** Final 0.0.1a hologram-clearance checks. No Minecraft classpath required. */
public final class R15RegressionTests {
    private static int count;
    private static void ok(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        for(int mount=0;mount<6;mount++)for(boolean advanced:new boolean[]{false,true}){
            var n=DisplayFacing.facing(mount).normal();
            var c=HologramProjection.centre(mount,advanced);
            var surface=new HologramProjection.Point(.5+n.x()*HologramProjection.MOUNT_SURFACE_OFFSET,.5+n.y()*HologramProjection.MOUNT_SURFACE_OFFSET,.5+n.z()*HologramProjection.MOUNT_SURFACE_OFFSET);
            double away=-((c.x()-surface.x())*n.x()+(c.y()-surface.y())*n.y()+(c.z()-surface.z())*n.z());
            double expected=advanced?HologramProjection.ADVANCED_PROJECTION_CLEARANCE:HologramProjection.NORMAL_PROJECTION_CLEARANCE;
            ok(Math.abs(away-expected)<1e-12,"Projection clearance matches contract");
            ok(away>=.90,"Every hologram clears the multipart host by at least 0.90 blocks");
            if(advanced)ok(away>HologramProjection.NORMAL_PROJECTION_CLEARANCE,"Advanced projection is farther than normal");
            for(int requested=2;requested<6;requested++){
                var configured=DisplayFacing.facing(HologramProjection.view(mount,requested));
                for(int side:new int[]{-1,1}){
                    var eye=new HologramProjection.Point(c.x()+configured.normal().x()*side*2,c.y()+configured.normal().y()*side*2,c.z()+configured.normal().z()*side*2);
                    var projection=HologramProjection.forCamera(mount,requested,advanced,eye);
                    ok(projection.centre().equals(c),"Camera side cannot move projection origin");
                    ok(projection.frame().right().cross(projection.frame().up()).equals(projection.frame().normal()),"Projection frame remains right handed");
                }
            }
        }
        ok(HologramProjection.ADVANCED_PROJECTION_CLEARANCE-HologramProjection.NORMAL_PROJECTION_CLEARANCE>=.25,"Advanced has meaningful extra clearance");
        System.out.println("PASS R15 hologram projection rules: "+count+" assertions.");
    }
}
