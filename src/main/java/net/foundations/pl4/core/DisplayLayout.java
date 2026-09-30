package net.foundations.pl4.core;

import java.util.*;

/** Bounded rectangular canvas math, independent of game/client state. Coordinates are screen right/down. */
public final class DisplayLayout {
    public static final int MAX_SIDE=16;
    public record Cell(int x,int y) {}
    public record Rectangle(int x,int y,int width,int height) {
        public int mask(Cell cell){
            int col=cell.x-x,row=cell.y-y;
            if(col<0||row<0||col>=width||row>=height)throw new IllegalArgumentException("Outside canvas");
            return (col>0?1:0)|(col+1<width?2:0)|(row>0?4:0)|(row+1<height?8:0);
        }
    }
    public static Optional<Rectangle> rectangle(Collection<Cell> cells){
        if(cells.isEmpty()||cells.size()>MAX_SIDE*MAX_SIDE)return Optional.empty();
        Set<Cell> unique=new HashSet<>(cells);if(unique.size()!=cells.size())return Optional.empty();
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE;
        for(Cell c:cells){minX=Math.min(minX,c.x);minY=Math.min(minY,c.y);maxX=Math.max(maxX,c.x);maxY=Math.max(maxY,c.y);}
        long w=(long)maxX-minX+1,h=(long)maxY-minY+1;
        if(w>MAX_SIDE||h>MAX_SIDE||w*h!=cells.size())return Optional.empty();
        return Optional.of(new Rectangle(minX,minY,(int)w,(int)h));
    }
    public static String texture(int mask){return switch(mask&15){
        case 0->"none";case 1->"one_w";case 2->"one_e";case 3->"opposite_2";
        case 4->"one_n";case 5->"two_w";case 6->"two_n";case 7->"three_s";
        case 8->"one_s";case 9->"two_s";case 10->"two_e";case 11->"three_n";
        case 12->"opposite_1";case 13->"three_e";case 14->"three_w";default->"all";};}
    private DisplayLayout(){}
}
