package net.foundations.pl4.compat;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Exact fixed-function display picking: clip = projection * current modelview.
 * Both OpenGL matrices use column-major storage. Matrix capture occurs while the
 * display's scale/translation/rotation are active, so no estimated poses are used.
 */
public final class PortMatrices {
    public static double[] clipMatrix(){
        return multiply(get(GL11.GL_PROJECTION_MATRIX),get(GL11.GL_MODELVIEW_MATRIX));
    }
    private static double[] get(int type){
        var buf=BufferUtils.createFloatBuffer(16);
        GL11.glGetFloatv(type,buf);
        double[] out=new double[16];
        for(int i=0;i<16;i++)out[i]=buf.get(i);
        return out;
    }
    public static double[] multiply(double[] a,double[] b){
        if(a.length!=16||b.length!=16)throw new IllegalArgumentException("Expected two 4x4 matrices");
        double[] out=new double[16];
        for(int c=0;c<4;c++)for(int r=0;r<4;r++)
            for(int k=0;k<4;k++)out[c*4+r]+=a[k*4+r]*b[c*4+k];
        return out;
    }
    private PortMatrices(){}
}
