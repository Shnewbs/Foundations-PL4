package net.foundations.pl4.core;

import java.util.*;
import net.foundations.pl4.Kind;

/** Pure six-axis planner used by the live server. It never samples data, touches worlds or loads chunks.
 * Ordinary slots 0..5, cable centre 6, display slots 7..12. A reader's face is its visual front;
 * its NETWORK input is the centre behind it, or the exposed cable in the next cell behind it.
 * VISUAL exports are deliberately NOT network edges. A face endpoint is not a cable junction.
 */
public final class MultipartTopology {
    public static final int CENTRE=6, DISPLAY_BASE=7, SLOT_COUNT=13;
    private static final int[][] V={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
    public record Cell(String dimension,int x,int y,int z) {
        public Cell { Objects.requireNonNull(dimension); }
        public Cell offset(int face){checkFace(face);return new Cell(dimension,x+V[face][0],y+V[face][1],z+V[face][2]);}
    }
    public record Node(int id,Cell cell,Kind kind,int face,int blocked) {
        public Node { if(id<0)throw new IllegalArgumentException("Negative part id");Objects.requireNonNull(cell);Objects.requireNonNull(kind);checkFace(face);blocked&=63; }
        public int slot(){return MultipartTopology.slot(kind,face);}
    }
    public record Edge(int a,int b) {}
    public record Export(int reader,int cable) {}
    public record Plan(List<Edge> network,List<Export> visual,Map<Integer,Integer> displayFeeds,
                       Map<Integer,int[]> cableArms,Set<Integer> externalLeads) {}
    public static int slot(Kind kind,int face){checkFace(face);return kind.cable()?CENTRE:kind.display()?DISPLAY_BASE+face:face;}
    private static final class Host {
        final Node[] slots=new Node[SLOT_COUNT];
        Node cable(){return slots[CENTRE];}
        Node face(int face){return slots[face];}
        Node display(int face){return slots[DISPLAY_BASE+face];}
        boolean clear(int face){return face(face)==null&&display(face)==null;}
    }
    public static Plan plan(List<Node> nodes){
        Map<Cell,Host> hosts=new HashMap<>();Set<Integer> ids=new HashSet<>();
        Map<Integer,int[]> arms=new HashMap<>();List<Edge> edges=new ArrayList<>();List<Export> exports=new ArrayList<>();
        Map<Integer,Integer> feeds=new HashMap<>();Set<Integer> leads=new HashSet<>();
        for(Node n:nodes){
            if(!ids.add(n.id))throw new IllegalArgumentException("Duplicate id "+n.id);
            Host h=hosts.computeIfAbsent(n.cell,k->new Host());
            if(h.slots[n.slot()]!=null)throw new IllegalArgumentException("Occupied slot "+n.slot()+" at "+n.cell);
            h.slots[n.slot()]=n;if(n.kind.cable())arms.put(n.id,new int[6]);
        }
        // True cable runs, both ports checked. A face attachment terminates that side of the run.
        for(Node a:nodes)if(a.kind.cable())for(int d=0;d<6;d++){
            Host h=hosts.get(a.cell),other=hosts.get(a.cell.offset(d));
            if(other==null||!h.clear(d)||!other.clear(d^1))continue;
            Node b=other.cable();
            if(b!=null&&compatible(a,b)&&enabled(a,d)&&enabled(b,d^1)){
                if(a.id<b.id)edges.add(new Edge(a.id,b.id));arms.get(a.id)[d]=ConnectionRules.CABLE;
            }
        }
        // Endpoints choose ONE network input. A local centre, even a disabled or wrong-family
        // one, precludes an invisible through-connection to an outside network.
        for(Node device:nodes)if(!device.kind.cable()&&!device.kind.display()){
            Host h=hosts.get(device.cell);Node cable=h.cable();
            if(cable!=null){
                if(compatible(cable,device)&&enabled(cable,device.face)){
                    edges.add(new Edge(cable.id,device.id));arms.get(cable.id)[device.face]=internalArm(device.kind);
                }
            }else{
                cable=backCable(hosts,h,device);
                if(cable!=null){edges.add(new Edge(cable.id,device.id));arms.get(cable.id)[device.face]=ConnectionRules.CABLE;leads.add(device.id);}
            }
        }
        // Reader -> locally mounted display. Separate slots and geometry, no electrical union.
        for(Node reader:nodes)if(reader.kind.reader()){
            Host h=hosts.get(reader.cell);Node display=h.display(reader.face);
            if(display!=null&&display.kind.panelDisplay()){feeds.put(display.id,reader.id);continue;}
            if(display!=null)continue; // Any display occupying the front blocks an external lead.
            Host other=hosts.get(reader.cell.offset(reader.face));if(other==null)continue;
            // A panel across the block boundary can sit directly against the reader's visual front.
            Node externalDisplay=other.display(reader.face^1);
            if(externalDisplay!=null&&other.face(reader.face^1)==null&&externalDisplay.kind.panelDisplay()){
                feeds.putIfAbsent(externalDisplay.id,reader.id);continue;
            }
            Node cable=other.cable();
            if(cable!=null&&other.clear(reader.face^1)&&compatible(reader,cable)&&enabled(cable,reader.face^1)){
                exports.add(new Export(reader.id,cable.id));arms.get(cable.id)[reader.face^1]=ConnectionRules.CABLE;
            }
        }
        // Unpaired displays read from their own compatible cable bus or an exposed back cable.
        for(Node display:nodes)if(display.kind.display()&&!feeds.containsKey(display.id)){
            Host h=hosts.get(display.cell);
            if(h.face(display.face)!=null)continue; // Cannot bypass a reader/Node/device on this face.
            Node cable=h.cable();
            if(cable!=null){
                if(compatible(cable,display)&&enabled(cable,display.face)){
                    feeds.put(display.id,cable.id);arms.get(cable.id)[display.face]=internalArm(display.kind);
                }
            }else{
                cable=backCable(hosts,h,display);
                if(cable!=null){feeds.put(display.id,cable.id);arms.get(cable.id)[display.face]=ConnectionRules.CABLE;leads.add(display.id);}
            }
        }
        return new Plan(List.copyOf(edges),List.copyOf(exports),Map.copyOf(feeds),Map.copyOf(arms),Set.copyOf(leads));
    }
    private static Node backCable(Map<Cell,Host> hosts,Host h,Node device){
        if(!h.clear(device.face^1))return null;
        Host other=hosts.get(device.cell.offset(device.face^1));if(other==null||!other.clear(device.face))return null;
        Node cable=other.cable();return cable!=null&&compatible(cable,device)&&enabled(cable,device.face)?cable:null;
    }
    private static boolean compatible(Node a,Node b){return a.kind.redstone()==b.kind.redstone();}
    private static boolean enabled(Node cable,int face){return !ConnectionRules.blocked(cable.blocked,face);}
    private static int internalArm(Kind k){return k==Kind.NODE||k==Kind.TRANSFER_NODE||k==Kind.REDSTONE_NODE||k.display()?ConnectionRules.INTERNAL:ConnectionRules.HALF;}
    private static void checkFace(int face){if(face<0||face>=6)throw new IllegalArgumentException("Invalid face "+face);}
    private MultipartTopology(){}
}
