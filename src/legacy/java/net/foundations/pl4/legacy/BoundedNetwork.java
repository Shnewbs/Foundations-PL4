package net.foundations.pl4.legacy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/** Java-7-compatible physical graph. The world adapter rejects unloaded or foreign-owned hosts. */
public final class BoundedNetwork {
    public static final int[][] DIRECTIONS={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
    public static final class Point {
        public final int x,y,z;
        public Point(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
        public Point offset(int side){if(side<0||side>=6)throw new IllegalArgumentException("Invalid side");int[] d=DIRECTIONS[side];return new Point(x+d[0],y+d[1],z+d[2]);}
        @Override public boolean equals(Object value){if(!(value instanceof Point))return false;Point p=(Point)value;return x==p.x&&y==p.y&&z==p.z;}
        @Override public int hashCode(){return (x*31+y)*31+z;}
        @Override public String toString(){return x+","+y+","+z;}
    }
    public interface Graph {boolean connected(Point point);}
    public static final class Plan {
        public final List<Point> nodes;public final boolean overflow;
        private Plan(List<Point> nodes,boolean overflow){this.nodes=Collections.unmodifiableList(new ArrayList<Point>(nodes));this.overflow=overflow;}
    }
    public static Plan scan(Graph graph,Point start,int limit){
        if(graph==null||start==null||limit<1||limit>4096)throw new IllegalArgumentException("Invalid graph budget");
        List<Point> nodes=new ArrayList<Point>();if(!graph.connected(start))return new Plan(nodes,false);
        Set<Point> seen=new HashSet<Point>();ArrayDeque<Point> queue=new ArrayDeque<Point>();seen.add(start);queue.add(start);
        while(!queue.isEmpty()){
            Point point=queue.removeFirst();nodes.add(point);
            for(int side=0;side<6;side++){
                Point next=point.offset(side);if(seen.contains(next)||!graph.connected(next))continue;
                if(seen.size()>=limit)return new Plan(Collections.<Point>emptyList(),true);
                seen.add(next);queue.addLast(next);
            }
        }
        return new Plan(nodes,false);
    }
    private BoundedNetwork(){}
}
