package net.foundations.pl4.core;

import java.util.OptionalInt;

/** Side/rim placement is resolved in the existing panel's plane, never in the hit polygon's plane. */
public final class DisplayPlacement {
    public static final double EDGE_DISTANCE = 0.20;
    public static OptionalInt extension(int mount, boolean outward, int hitFace, double x, double y, double z) {
        if (mount < 0 || mount > 5 || hitFace < 0 || hitFace > 5
            || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || x<-.07 || y<-.07 || z<-.07 || x>1.07 || y>1.07 || z>1.07) return OptionalInt.empty();
        if ((mount >> 1) != (hitFace >> 1)) return OptionalInt.of(hitFace);
        var frame = DisplayFacing.frame(mount, outward);
        double rx = x - .5, ry = y - .5, rz = z - .5;
        double right = rx*frame.right().x()+ry*frame.right().y()+rz*frame.right().z();
        double up = rx*frame.up().x()+ry*frame.up().y()+rz*frame.up().z();
        if (Math.max(Math.abs(right), Math.abs(up)) < .5-EDGE_DISTANCE) return OptionalInt.empty();
        var axis = Math.abs(right) >= Math.abs(up) ? frame.right() : frame.up();
        double coordinate = Math.abs(right) >= Math.abs(up) ? right : up;
        return OptionalInt.of(DisplayFacing.direction(coordinate < 0 ? axis.negate() : axis));
    }
    public static boolean withinLimits(java.util.Collection<DisplayLayout.Cell> cells){
        if(cells.isEmpty()||cells.size()>256)return false;
        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minY=Integer.MAX_VALUE,maxY=Integer.MIN_VALUE;
        for(var c:cells){minX=Math.min(minX,c.x());maxX=Math.max(maxX,c.x());minY=Math.min(minY,c.y());maxY=Math.max(maxY,c.y());}
        return (long)maxX-minX<DisplayLayout.MAX_SIDE&&(long)maxY-minY<DisplayLayout.MAX_SIDE;
    }
    private DisplayPlacement() {}
}
