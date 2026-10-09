package net.foundations.pl4.core;

/** Dependency-free geometry/style rules for the in-world PL2-style display editor.
 * Rendering and hit-testing consume the same rectangles so visual controls cannot drift away from clicks. */
public final class EditorChrome {
    public static final int TOOLBAR_X=2,TOOLBAR_Y=2,TOOLBAR_W=14,TOOLBAR_H=12,TOOLBAR_STEP=13;
    public static final int HANDLE_SIZE=10;
    public enum Corner { NONE,NW,NE,SW,SE }

    public static DisplayElements.Rect toolRect(int index){return new DisplayElements.Rect(TOOLBAR_X,TOOLBAR_Y+index*TOOLBAR_STEP,TOOLBAR_W,TOOLBAR_H);}
    public static int toolAt(double x,double y,int toolCount){
        if(x<TOOLBAR_X||x>=TOOLBAR_X+TOOLBAR_W||y<TOOLBAR_Y)return -1;
        int index=(int)((y-TOOLBAR_Y)/TOOLBAR_STEP);if(index<0||index>=toolCount)return -1;
        return toolRect(index).contains(x,y)?index:-1;
    }
    public static int toolColor(int index,boolean active,boolean hover){
        int color=switch(index){
            case 0->0xFF59D989; // add
            case 1->0xFF63C7FF; // edit
            case 2->0xFFFF6B6B; // delete
            case 3->0xFFC38BFF; // duplicate
            case 4,5->0xFFFFC45C; // ordering
            case 6->active?0xFF75E56B:0xFF8FA5A8; // snap
            case 7->0xFF67DCE6; // settings
            default->0xFFD4D4D4;
        };
        if(!hover)return color;
        int r=Math.min(255,((color>>16)&255)+42),g=Math.min(255,((color>>8)&255)+42),b=Math.min(255,(color&255)+42);
        return 0xFF000000|(r<<16)|(g<<8)|b;
    }

    public static DisplayElements.Rect handleRect(DisplayElements.Rect r,Corner corner){
        int size=Math.min(HANDLE_SIZE,Math.max(6,Math.min(r.width(),r.height())));int x=switch(corner){case NW,SW->r.x();case NE,SE->r.right()-size;default->r.x();};
        int y=switch(corner){case NW,NE->r.y();case SW,SE->r.bottom()-size;default->r.y();};
        return new DisplayElements.Rect(x,y,size,size);
    }
    public static Corner cornerAt(DisplayElements.Rect r,double x,double y){
        for(Corner c:new Corner[]{Corner.NW,Corner.NE,Corner.SW,Corner.SE})if(handleRect(r,c).contains(x,y))return c;return Corner.NONE;
    }
    private static int snap(int value,int step){return (int)Math.round(value/(double)step)*step;}
    public static DisplayElements.Spec resize(DisplayElements.Spec s,Corner corner,double dx,double dy,boolean grid,int spaceW,int spaceH){
        if(corner==Corner.NONE)return s;
        spaceW=net.foundations.pl4.compat.PortMath.clamp(spaceW,8,DisplayElements.MAX_CANVAS);spaceH=net.foundations.pl4.compat.PortMath.clamp(spaceH,9,DisplayElements.MAX_CANVAS);int step=grid?4:1;
        var b=s.bounds();int left=b.x(),top=b.y(),right=b.right(),bottom=b.bottom();
        if(corner==Corner.NW||corner==Corner.SW){left=snap((int)Math.round(left+dx),step);left=net.foundations.pl4.compat.PortMath.clamp(left,0,right-8);}
        else{right=snap((int)Math.round(right+dx),step);right=net.foundations.pl4.compat.PortMath.clamp(right,left+8,spaceW);}
        if(corner==Corner.NW||corner==Corner.NE){top=snap((int)Math.round(top+dy),step);top=net.foundations.pl4.compat.PortMath.clamp(top,0,bottom-9);}
        else{bottom=snap((int)Math.round(bottom+dy),step);bottom=net.foundations.pl4.compat.PortMath.clamp(bottom,top+9,spaceH);}
        return s.bounds(new DisplayElements.Rect(left,top,right-left,bottom-top));
    }
    private EditorChrome(){}
}
