package net.foundations.pl4.compat;
import org.lwjgl.opengl.GL11;

/** Forge28 world-space pose, backed by its native fixed-function OpenGL stack.
 * Push/pop must be paired by callers; all transforms compose with the camera
 * modelview supplied by vanilla's tile renderer.
 */
public final class LegacyWorldPose {
    public void pushPose(){GL11.glPushMatrix();}
    public void popPose(){GL11.glPopMatrix();}
    public void translate(double x,double y,double z){GL11.glTranslated(x,y,z);}
    public void scale(double x,double y,double z){GL11.glScaled(x,y,z);}
    public void rotateX(float degrees){GL11.glRotatef(degrees,1,0,0);}
    public void rotateY(float degrees){GL11.glRotatef(degrees,0,1,0);}
    public void rotateZ(float degrees){GL11.glRotatef(degrees,0,0,1);}
}
