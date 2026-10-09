package net.foundations.pl4.compat;
import net.minecraft.util.math.vector.Matrix4f;
/** Active-editor picking captures the real fixed-function matrices, not guessed camera transforms. */
public final class PortMatrices {
 public static Matrix4f projection(){return matrix(org.lwjgl.opengl.GL11.GL_PROJECTION_MATRIX);}
 public static Matrix4f modelView(){return matrix(org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX);}
 private static Matrix4f matrix(int key){var buffer=org.lwjgl.BufferUtils.createFloatBuffer(16);org.lwjgl.opengl.GL11.glGetFloatv(key,buffer);float[] values=new float[16];buffer.get(values);Matrix4f matrix=new Matrix4f(values);matrix.transpose();return matrix;}
 private PortMatrices(){}
}
