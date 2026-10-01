import java.util.*;
import net.foundations.pl4.Kind;
import net.foundations.pl4.core.*;
import net.foundations.pl4.core.MultipartTopology.*;

/** Exercises the SAME topology planner used in NetworkEngine, without simulated Minecraft classes. */
public final class R7RegressionTests {
    private static long assertions;
    private static void check(boolean b,String message){assertions++;if(!b)throw new AssertionError(message);}
    private static Node n(int id,Cell cell,Kind kind,int face,int mask){return new Node(id,cell,kind,face,mask);}
    private static DisjointSets groups(List<Node> nodes,Plan plan){var d=new DisjointSets(nodes.size());for(var e:plan.network())d.union(e.a(),e.b());return d;}
    private static Set<Integer> visible(List<Node> nodes,Plan plan,int display){
        Integer feed=plan.displayFeeds().get(display);if(feed==null)return Set.of();
        if(nodes.get(feed).kind().reader())return Set.of(feed);
        var groups=groups(nodes,plan);Set<Integer> result=new HashSet<>();
        for(Node a:nodes)if(a.kind().reader()&&groups.find(a.id())==groups.find(feed))result.add(a.id());
        for(var e:plan.visual())if(groups.find(e.cable())==groups.find(feed))result.add(e.reader());return result;
    }
    private static boolean network(List<Node> nodes,Plan plan,int a,int b){var d=groups(nodes,plan);return d.find(a)==d.find(b);}
    private static void expectFailure(Runnable fn){try{fn.run();throw new AssertionError("Expected invalid topology rejection");}catch(IllegalArgumentException expected){assertions++;}}
    public static void main(String[] args){
        Cell origin=new Cell("overworld",0,0,0);
        for(int face=0;face<6;face++){
            check(MultipartTopology.slot(Kind.INVENTORY_READER,face)==face,"ordinary slot");
            check(MultipartTopology.slot(Kind.DISPLAY,face)==7+face,"display slot");
            check(MultipartTopology.slot(Kind.DATA_CABLE,face)==6,"centre slot");
            for(Kind reader:Kind.values())if(reader.reader())for(Kind screen:List.of(Kind.DISPLAY,Kind.MINI_DISPLAY,Kind.LARGE_DISPLAY)){
                List<Node> local=List.of(n(0,origin,Kind.DATA_CABLE,0,0),n(1,origin,reader,face,0),n(2,origin,screen,face,0));
                Plan plan=MultipartTopology.plan(local);
                check(network(local,plan,0,1),"reader uses internal network port");
                check(!network(local,plan,0,2),"display never electrical cable junction");
                check(visible(local,plan,2).equals(Set.of(1)),"paired screen sees its reader");
                check(plan.cableArms().get(0)[face]==ConnectionRules.HALF,"arm terminates at reader, not through screen");
                check(plan.externalLeads().isEmpty(),"paired centre geometry does not get external leads");
                // A neighboring cable cannot span the empty centre of a mounted endpoint.
                List<Node> external=List.of(n(0,origin.offset(face^1),Kind.DATA_CABLE,0,0),n(1,origin,reader,face,0),n(2,origin,screen,face,0));
                plan=MultipartTopology.plan(external);
                check(!network(external,plan,0,1),"endpoint cell requires its own cable");
                check(visible(external,plan,2).equals(Set.of(1)),"external rear input drives attached screen");
                check(plan.externalLeads().isEmpty(),"no free endpoint cable lead");
                check(plan.cableArms().get(0)[face]==0,"neighbor cable cannot cross empty endpoint cell");
            }
            // Each enabled/disabled bit and both families: this applies to the actual full planner.
            for(int mask=0;mask<64;mask++)for(boolean redstone:List.of(false,true)){
                Kind cable=redstone?Kind.REDSTONE_CABLE:Kind.DATA_CABLE;
                List<Node> pair=List.of(n(0,origin,cable,0,mask),n(1,origin.offset(face),cable,0,0));
                Plan plan=MultipartTopology.plan(pair);boolean expected=(mask&(1<<face))==0;
                check(network(pair,plan,0,1)==expected,"port mask gates wire edge");
                check((plan.cableArms().get(0)[face]!=0)==expected,"arm agrees with network edge");
                List<Node> input=List.of(n(0,origin.offset(face^1),cable,0,mask),n(1,origin,Kind.ENERGY_READER,face,0));
                plan=MultipartTopology.plan(input);expected=false;
                check(network(input,plan,0,1)==expected,"native endpoint family and port checks");
                check(plan.externalLeads().contains(1)==expected,"lead agrees with actual endpoint connectivity");
            }
            // NETWORK and VISUAL side isolation, including a long remote output bus.
            List<Node> split=new ArrayList<>();
            split.add(n(0,origin,Kind.DATA_CABLE,0,0));
            split.add(n(1,origin,Kind.INVENTORY_READER,face,0));
            split.add(n(2,origin.offset(face),Kind.DATA_CABLE,0,0));
            split.add(n(3,origin.offset(face).offset(face),Kind.DATA_CABLE,0,0));
            int side=(face+2)%6;if(side==(face^1))side=(side+2)%6;
            split.add(n(4,split.get(3).cell(),Kind.DISPLAY,side,0));
            split.add(n(5,split.get(3).cell(),Kind.NODE,side^1,0));
            Plan plan=MultipartTopology.plan(split);
            check(network(split,plan,0,1),"reader input at back");
            check(!network(split,plan,1,2),"VISUAL is NOT NETWORK");
            check(!network(split,plan,0,5),"output-side Node cannot enter input network");
            check(network(split,plan,2,5),"output network still functional itself");
            check(visible(split,plan,4).equals(Set.of(1)),"remote output display sees exported reader");
            check(plan.visual().size()==1,"one explicit export");
            check(plan.cableArms().get(2)[face^1]==ConnectionRules.CABLE,"visual edge still has a connector");
            // Wrong family and blocked output port must not create a hidden export.
            for(Kind family:List.of(Kind.DATA_CABLE,Kind.REDSTONE_CABLE))for(int mask=0;mask<64;mask++){
                List<Node> test=new ArrayList<>(split);test.set(2,n(2,split.get(2).cell(),family,0,mask));
                Plan p=MultipartTopology.plan(test);boolean expected=family==Kind.DATA_CABLE&&(mask&(1<<(face^1)))==0;
                check(!network(test,p,0,2),"no data bridge for any output port mask");
                check((!p.visual().isEmpty())==expected,"visual mask/domain gating");
            }
            // No sideways reader connection and no relay through raw adjacent face devices.
            for(int sideFace=0;sideFace<6;sideFace++)if(sideFace!=face&&sideFace!=(face^1)){
                List<Node> pair=List.of(n(0,origin,Kind.INVENTORY_READER,face,0),n(1,origin.offset(sideFace),Kind.DATA_CABLE,0,0));
                plan=MultipartTopology.plan(pair);check(plan.network().isEmpty()&&plan.visual().isEmpty(),"side is not a reader port");
            }
            List<Node> disabledLocal=List.of(n(0,origin,Kind.DATA_CABLE,0,1<<face),n(1,origin,Kind.INVENTORY_READER,face,0),n(2,origin.offset(face^1),Kind.DATA_CABLE,0,0));
            plan=MultipartTopology.plan(disabledLocal);
            check(!network(disabledLocal,plan,1,2),"disabled local input cannot bypass via external cable");
            check(!plan.externalLeads().contains(1),"no invisible bypass stem");
            // Bare Node rear port, direct boundary screen, standalone display rear port.
            for(Kind device:List.of(Kind.NODE,Kind.TRANSFER_NODE,Kind.ARRAY,Kind.DATA_RECEIVER)){
                List<Node> pair=List.of(n(0,origin,device,face,0),n(1,origin.offset(face^1),Kind.DATA_CABLE,0,0));
                plan=MultipartTopology.plan(pair);check(!network(pair,plan,0,1)&&plan.externalLeads().isEmpty(),"bare device cannot extend a cable");
            }
            List<Node> boundary=List.of(n(0,origin,Kind.INVENTORY_READER,face,0),n(1,origin.offset(face),Kind.DISPLAY,face^1,0));
            plan=MultipartTopology.plan(boundary);check(visible(boundary,plan,1).equals(Set.of(0)),"direct boundary display visual feed");
            check(plan.network().isEmpty(),"boundary screen not bridge");
            List<Node> displayBack=List.of(n(0,origin,Kind.LARGE_DISPLAY,face,0),n(1,origin.offset(face^1),Kind.DATA_CABLE,0,0));
            plan=MultipartTopology.plan(displayBack);check(!plan.displayFeeds().containsKey(0)&&plan.externalLeads().isEmpty(),"bare screen cannot extend a cable");
            // Two endpoints separated by three host cells require three placed cables.
            List<Node> span=new ArrayList<>(List.of(n(0,origin,Kind.DATA_CABLE,0,0),
                n(1,origin.offset(face^1),Kind.NODE,face^1,0),n(2,origin.offset(face),Kind.TRANSFER_NODE,face,0)));
            plan=MultipartTopology.plan(span);
            check(!network(span,plan,1,2)&&plan.externalLeads().isEmpty(),"one cable cannot span three cells between endpoints");
            span.add(n(3,origin.offset(face^1),Kind.DATA_CABLE,0,0));
            plan=MultipartTopology.plan(span);check(!network(span,plan,1,2),"one missing cable still breaks the run");
            span.add(n(4,origin.offset(face),Kind.DATA_CABLE,0,0));
            plan=MultipartTopology.plan(span);check(network(span,plan,1,2),"placing every cable completes the run");
            // Mounted display occupies the visual output; it must not transmit through its screen face.
            List<Node> blocked=List.of(n(0,origin,Kind.INVENTORY_READER,face,0),n(1,origin,Kind.DISPLAY,face,0),n(2,origin.offset(face),Kind.DATA_CABLE,0,0));
            plan=MultipartTopology.plan(blocked);check(plan.visual().isEmpty()&&plan.cableArms().get(2)[face^1]==0,"display blocks cable through front");
        }
        // One host retains all thirteen logical slots. Geometry acceptance is tested separately in native fixtures.
        List<Node> all=new ArrayList<>();all.add(n(0,origin,Kind.DATA_CABLE,0,0));
        for(int f=0;f<6;f++){all.add(n(all.size(),origin,Kind.INVENTORY_READER,f,0));all.add(n(all.size(),origin,Kind.DISPLAY,f,0));}
        check(MultipartTopology.plan(all).displayFeeds().size()==6,"six independent paired displays and centre");
        expectFailure(()->MultipartTopology.plan(List.of(n(0,origin,Kind.NODE,0,0),n(1,origin,Kind.INVENTORY_READER,0,0))));
        expectFailure(()->MultipartTopology.plan(List.of(n(0,origin,Kind.NODE,0,0),n(0,origin,Kind.DISPLAY,0,0))));
        expectFailure(()->MultipartTopology.slot(Kind.DISPLAY,6));
        List<Node> dimensions=List.of(n(0,origin,Kind.DATA_CABLE,0,0),n(1,new Cell("nether",0,1,0),Kind.DATA_CABLE,0,0));
        check(MultipartTopology.plan(dimensions).network().isEmpty(),"no coordinate-only cross-dimension edge");
        // Large deterministic graph: a snake is not required; ensure cached planner's partition is complete.
        List<Node> chain=new ArrayList<>();for(int x=0;x<4096;x++)chain.add(n(x,new Cell("overworld",x,0,0),Kind.DATA_CABLE,0,0));
        var p=MultipartTopology.plan(chain);check(p.network().size()==4095,"bounded neighbor discovery, no all-pairs network edges");
        var groups=groups(chain,p);for(int i=0;i<4096;i++)check(groups.find(i)==groups.find(0),"long chain connectivity");
        // Input list ordering cannot select a different physical reader or network.
        Collections.reverse(chain);p=MultipartTopology.plan(chain);check(p.network().size()==4095,"input order independent");
        System.out.println("PASS R7 production multipart planner: "+assertions+" assertions. Six orientations, all 64 masks, separate slots, direct and remote visual paths, isolation, domains, endpoint leads and 4096-cable chain.");
    }
}
