package net.foundations.pl4;

import net.foundations.pl4.core.*;
import net.minecraft.entity.player.EntityPlayerMP;

/** Server-resolved page links. Sneak-use always opens the editor instead. */
final class DisplayActions {
    static boolean activate(EntityPlayerMP player,HostEntity anchor,Part clicked){
        if(!player.getLevel().mayInteract(player,anchor.getBlockPos())||player.isSneaking()||player.isSpectator()||!clicked.kind.display()||!anchor.canEdit(player)||player.distanceToSqr(net.foundations.pl4.compat.PortVectors.atCenterOf(anchor.getBlockPos()))>64)return false;
        var controller=DisplayNetworks.controller(anchor,clicked);var host=controller.host();var p=controller.part();
        if(p.displayMode!=DisplayElements.Mode.CUSTOM||p.layoutRevision==Long.MAX_VALUE||!DisplayNetworks.canEditCanvas(player,anchor,clicked))return false;
        var eye=player.getEyePosition(1.0F).subtract(host.getBlockPos().getX(),host.getBlockPos().getY(),host.getBlockPos().getZ());
        var look=player.getLookAngle();DisplayFacing.Frame frame;double ox,oy,oz;
        if(p.hologram()){
            var projection=HologramProjection.forCamera(p.face.ordinal(),p.hologramView,p.kind==Kind.ADVANCED_HOLOGRAM,new HologramProjection.Point(eye.x,eye.y,eye.z));
            frame=projection.frame();ox=projection.centre().x();oy=projection.centre().y();oz=projection.centre().z();
        }else{
            frame=DisplayFacing.frame(p.face.ordinal(),p.displayOutward);double offset=DisplayFacing.planeOffset(p.displayOutward);
            ox=.5+p.face.getStepX()*offset;oy=.5+p.face.getStepY()*offset;oz=.5+p.face.getStepZ()*offset;
        }
        double scale=p.kind==Kind.MINI_DISPLAY?.0018F:.0035F;double halfW=124,halfH=60;
        if(p.kind==Kind.LARGE_DISPLAY){
            var space=DynamicCanvasLayout.large(p.canvasWidth,p.canvasHeight);scale=(float)space.scale();halfW=space.width()/2.0;halfH=space.height()/2.0;
            double right=(p.canvasWidth-1)/2.0,down=(p.canvasHeight-1)/2.0;
            ox+=frame.right().x()*right-frame.up().x()*down;oy+=frame.right().y()*right-frame.up().y()*down;oz+=frame.right().z()*right-frame.up().z()*down;
        }
        double denominator=dot(look.x,look.y,look.z,frame.normal());if(denominator>=-1e-8)return false;
        double distance=dot(ox-eye.x,oy-eye.y,oz-eye.z,frame.normal())/denominator;if(distance<0||distance>8)return false;
        double x=eye.x+look.x*distance-ox,y=eye.y+look.y*distance-oy,z=eye.z+look.z*distance-oz;
        double u=halfW+dot(x,y,z,frame.right())/scale,v=halfH-dot(x,y,z,frame.up())/scale;
        if(u<0||v<0||u>=p.layoutWidth||v>=p.layoutHeight)return false;
        for(int i=p.elements.size()-1;i>=0;i--){var e=p.elements.get(i).spec();
            if(e.page()!=p.displayPage||e.options().hidden()||!e.bounds().contains(u,v))continue;
            // Frontmost visible element owns the hit, even if it has no action.
            int page=e.options().actionPage();if(page<0)return false;
            var s=p.displaySettings();DisplayNetworks.applyLayout(host,p,new Part.DisplaySettings(s.label(),s.selected(),s.metric(),s.color(),s.elements(),s.displayMode(),page,s.layoutWidth(),s.layoutHeight(),s.pageNames()),DisplayNetworks.nextLayoutRevision(p));
            return true;
        }
        return false;
    }
    private static double dot(double x,double y,double z,DisplayFacing.Vector v){return x*v.x()+y*v.y()+z*v.z();}
    private DisplayActions(){}
}
