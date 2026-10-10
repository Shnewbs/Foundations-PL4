package net.foundations.pl4.compat;
import org.joml.Quaternionf;
/** Forge44 JOML replacements for the former Minecraft axis quaternions. */
public final class AxisRotation {
    public static Quaternionf xp(float deg){return new Quaternionf().rotationX((float)Math.toRadians(deg));}
    public static Quaternionf yp(float deg){return new Quaternionf().rotationY((float)Math.toRadians(deg));}
    public static Quaternionf zp(float deg){return new Quaternionf().rotationZ((float)Math.toRadians(deg));}
    private AxisRotation(){}
}
