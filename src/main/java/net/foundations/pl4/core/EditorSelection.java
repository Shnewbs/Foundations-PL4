package net.foundations.pl4.core;

import java.util.*;

/** Page-local marquee selection and rigid translation shared by preview and server edits. */
public final class EditorSelection {
    public static DisplayElements.Rect box(double ax,double ay,double bx,double by,int width,int height){
        int left=(int)Math.floor(net.foundations.pl4.compat.PortMath.clamp(Math.min(ax,bx),0,width)),top=(int)Math.floor(net.foundations.pl4.compat.PortMath.clamp(Math.min(ay,by),0,height));
        int right=(int)Math.ceil(net.foundations.pl4.compat.PortMath.clamp(Math.max(ax,bx),0,width)),bottom=(int)Math.ceil(net.foundations.pl4.compat.PortMath.clamp(Math.max(ay,by),0,height));
        return new DisplayElements.Rect(left,top,right-left,bottom-top);
    }
    public static List<UUID> inBox(List<DisplayElements.Spec> elements,int page,DisplayElements.Rect box){
        if(box.width()<=0||box.height()<=0)return List.of();
        return elements.stream().filter(e->e.page()==page&&!e.options().hidden()&&!e.options().locked()&&e.bounds().x()<box.right()&&e.bounds().right()>box.x()&&e.bounds().y()<box.bottom()&&e.bounds().bottom()>box.y()).map(DisplayElements.Spec::id).toList();
    }
    /** Clamp one common displacement; never squash sizes or change spacing within the selection. */
    public static List<DisplayElements.Spec> move(List<DisplayElements.Spec> elements,int dx,int dy,int width,int height){
        if(elements.isEmpty())return List.of();
        if(width<8||height<9||width>DisplayElements.MAX_CANVAS||height>DisplayElements.MAX_CANVAS)throw new IllegalArgumentException("Invalid canvas dimensions.");
        int left=elements.stream().mapToInt(e->e.bounds().x()).min().orElseThrow(),top=elements.stream().mapToInt(e->e.bounds().y()).min().orElseThrow();
        int right=elements.stream().mapToInt(e->e.bounds().right()).max().orElseThrow(),bottom=elements.stream().mapToInt(e->e.bounds().bottom()).max().orElseThrow();
        if(right>width||bottom>height)throw new IllegalArgumentException("Selection does not fit this canvas.");
        int x=net.foundations.pl4.compat.PortMath.clamp(dx,-left,width-right),y=net.foundations.pl4.compat.PortMath.clamp(dy,-top,height-bottom);
        return elements.stream().map(e->{var b=e.bounds();return e.bounds(new DisplayElements.Rect(b.x()+x,b.y()+y,b.width(),b.height()));}).toList();
    }
    private EditorSelection(){}
}
