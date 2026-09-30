package net.foundations.pl4.core;

/** Pure geometry for joined large-display logical canvases. Large boards use the whole physical rectangle
 * at a stable square-pixel density; old 248x120 layouts are migrated proportionally once. */
public final class DynamicCanvasLayout {
    public static final int LEGACY_WIDTH=248, LEGACY_HEIGHT=120, PIXELS_PER_BLOCK=180;
    public record Space(int width,int height,double scale) {}
    public static Space large(int columns,int rows){
        columns=Math.clamp(columns,1,16);rows=Math.clamp(rows,1,16);
        double usableW=columns-.12, usableH=rows-.12;
        int w=Math.max(96,(int)Math.round(usableW*PIXELS_PER_BLOCK));
        int h=Math.max(96,(int)Math.round(usableH*PIXELS_PER_BLOCK));
        double scale=Math.min(usableW/w,usableH/h);
        return new Space(w,h,scale);
    }
    public static DisplayElements.Rect migrate(DisplayElements.Rect r,int fromW,int fromH,int toW,int toH){
        fromW=Math.max(8,fromW);fromH=Math.max(9,fromH);toW=Math.max(8,toW);toH=Math.max(9,toH);
        int x=(int)Math.round(r.x()*(double)toW/fromW), y=(int)Math.round(r.y()*(double)toH/fromH);
        int w=Math.max(8,(int)Math.round(r.width()*(double)toW/fromW)), h=Math.max(9,(int)Math.round(r.height()*(double)toH/fromH));
        x=Math.clamp(x,0,Math.max(0,toW-8));y=Math.clamp(y,0,Math.max(0,toH-9));
        w=Math.clamp(w,8,Math.max(8,toW-x));h=Math.clamp(h,9,Math.max(9,toH-y));
        return new DisplayElements.Rect(x,y,w,h);
    }
    public static DisplayElements.Spec migrate(DisplayElements.Spec s,int fromW,int fromH,int toW,int toH){return s.bounds(migrate(s.bounds(),fromW,fromH,toW,toH));}
    private DynamicCanvasLayout(){}
}
