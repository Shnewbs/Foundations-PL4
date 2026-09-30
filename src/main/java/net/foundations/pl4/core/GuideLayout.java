package net.foundations.pl4.core;

/** Integer GUI-space layout. Text is never drawn through a fractional scale transform. */
public final class GuideLayout {
    public record Rect(int x,int y,int width,int height) {
        public int right(){return x+width;} public int bottom(){return y+height;}
        public boolean contains(double px,double py){return px>=x&&px<right()&&py>=y&&py<bottom();}
    }
    public record Layout(Rect book,Rect list,Rect detail,boolean compact) {}
    public static Layout fit(int screenWidth,int screenHeight) {
        int w=Math.max(180,Math.min(620,screenWidth-40)),h=Math.max(140,Math.min(360,screenHeight-20));
        int x=(screenWidth-w)/2+10,y=(screenHeight-h)/2;
        boolean compact=screenWidth<500;
        Rect book=new Rect(x,y,w,h);
        Rect list=compact?new Rect(x+20,y+75,w-44,Math.max(12,h-154)):
            new Rect(x+22,y+84,w/2-46,Math.max(12,h-142));
        Rect detail=compact?new Rect(x+22,y+78,w-48,Math.max(12,h-124)):
            new Rect(x+w/2+22,y+70,w/2-48,Math.max(12,h-112));
        return new Layout(book,list,detail,compact);
    }
    public static int clampScroll(int offset,int content,int viewport){return Math.max(0,Math.min(offset,Math.max(0,content-viewport)));}
    public static int thumbSize(int track,int content,int viewport){return content<=viewport?track:Math.min(track,Math.max(12,(int)((long)track*viewport/Math.max(1,content))));}
    public static int thumbPosition(int offset,int track,int content,int viewport){int range=track-thumbSize(track,content,viewport);return content<=viewport?0:(int)((long)range*clampScroll(offset,content,viewport)/Math.max(1,content-viewport));}
    public static int scrollFromThumb(double pixel,int track,int content,int viewport){int range=track-thumbSize(track,content,viewport);return range<=0?0:clampScroll((int)Math.round(pixel*(content-viewport)/range),content,viewport);}
    private GuideLayout(){}
}
