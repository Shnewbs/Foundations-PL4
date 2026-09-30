package net.foundations.pl4.core;

import java.util.Optional;
/** Perspective-correct cursor unprojection. Uses the actual rendered model-view-projection,
 * not an assumed FOV, global block axis or screen orientation. No state retained across sessions. */
public final class DisplayPicking {
    public record Point(double x,double y){}
    public static Optional<double[]> inverse(double[] columnMajor){
        if(columnMajor.length!=16)return Optional.empty();double[][] a=new double[4][8];
        for(int r=0;r<4;r++){for(int c=0;c<4;c++){double v=columnMajor[c*4+r];if(!Double.isFinite(v))return Optional.empty();a[r][c]=v;}a[r][r+4]=1;}
        for(int c=0;c<4;c++){
            int pivot=c;for(int r=c+1;r<4;r++)if(Math.abs(a[r][c])>Math.abs(a[pivot][c]))pivot=r;
            if(Math.abs(a[pivot][c])<1e-14)return Optional.empty();double[] swap=a[c];a[c]=a[pivot];a[pivot]=swap;
            double v=a[c][c];for(int k=0;k<8;k++)a[c][k]/=v;
            for(int r=0;r<4;r++)if(r!=c){v=a[r][c];for(int k=0;k<8;k++)a[r][k]-=v*a[c][k];}
        }
        double[] out=new double[16];for(int r=0;r<4;r++)for(int c=0;c<4;c++)out[c*4+r]=a[r][c+4];return Optional.of(out);
    }
    private static double[] point(double[] m,double x,double y,double z){
        double w=m[3]*x+m[7]*y+m[11]*z+m[15];if(Math.abs(w)<1e-12)return null;
        double[] p={(m[0]*x+m[4]*y+m[8]*z+m[12])/w,(m[1]*x+m[5]*y+m[9]*z+m[13])/w,(m[2]*x+m[6]*y+m[10]*z+m[14])/w};
        for(double value:p)if(!Double.isFinite(value))return null;return p;
    }
    public static Optional<Point> hit(double[] inverse,double mouseX,double mouseY,int width,int height){
        if(inverse==null||width<=0||height<=0)return Optional.empty();double x=2*mouseX/width-1,y=1-2*mouseY/height;
        double[] a=point(inverse,x,y,-1),b=point(inverse,x,y,1);if(a==null||b==null||Math.abs(b[2]-a[2])<1e-10)return Optional.empty();
        double t=-a[2]/(b[2]-a[2]);if(t<0||t>1)return Optional.empty();return Optional.of(new Point(a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t));
    }
    private DisplayPicking(){}
}
