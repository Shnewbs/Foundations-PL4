import net.foundations.pl4.legacy.*;
import java.util.*;
/** Actual portable production algorithms; no Minecraft fixtures substituted. */
public final class CoreChecks {
    static int checks;
    static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
    static final class Stack {final int kind,n;Stack(int k,int n){kind=k;this.n=n;}}
    static final ConservingItems.Stacks<Stack> STACKS=new ConservingItems.Stacks<Stack>(){
        public boolean empty(Stack s){return s==null||s.n==0;}public int count(Stack s){return s==null?0:s.n;}
        public boolean same(Stack a,Stack b){return a!=null&&b!=null&&a.kind==b.kind;}public Stack copy(Stack s,int n){return n==0?null:new Stack(s.kind,n);}
    };
    static class Store implements ConservingItems.Inventory<Stack>{
        Stack stack;int capacity,calls;boolean refuseActual,wrongSimulation,throwActual;
        Store(int count,int capacity){this.capacity=capacity;stack=count==0?null:new Stack(1,count);}
        public int slots(){calls++;return 1;}
        public Stack extract(int slot,int count,boolean simulate){calls++;if(stack==null)return null;int n=Math.min(count,stack.n);Stack result=new Stack(stack.kind,n);if(!simulate)stack=stack.n==n?null:new Stack(stack.kind,stack.n-n);return result;}
        public Stack insert(int slot,Stack offered,boolean simulate){calls++;if(!simulate&&throwActual)throw new IllegalStateException("provider fault");if(wrongSimulation)return new Stack(2,1);if(!simulate&&refuseActual)return offered;if(stack!=null&&stack.kind!=offered.kind)return offered;int accepted=Math.min(offered.n,Math.max(0,capacity-(stack==null?0:stack.n)));if(!simulate&&accepted>0)stack=new Stack(offered.kind,(stack==null?0:stack.n)+accepted);return offered.n==accepted?null:new Stack(offered.kind,offered.n-accepted);}
        int count(){return stack==null?0:stack.n;}
    }
    static final class Buffer implements ConservingItems.Escrow<Stack>{Stack held;public Stack get(){return held;}public void set(Stack s){held=s;}int count(){return held==null?0:held.n;}}
    public static void main(String[] args){
        FluidChecks.main(args);
        for(int source=0;source<=64;source++)for(int occupied=0;occupied<=64;occupied++)for(int rate:new int[]{1,8,64}){
            Store a=new Store(source,64),b=new Store(occupied,64);Buffer e=new Buffer();int moved=ConservingItems.move(STACKS,e,a,b,rate,1);
            check(moved==Math.min(source,Math.min(rate,64-occupied)),"bounded move");check(a.count()+b.count()+e.count()==source+occupied,"conservation");check(e.count()==0,"normal escrow drains");
        }
        Store a=new Store(20,64),b=new Store(0,64);b.refuseActual=true;Buffer e=new Buffer();
        check(ConservingItems.move(STACKS,e,a,b,8,1)==0&&a.count()==12&&e.count()==8,"actual refusal retained");b.refuseActual=false;
        check(ConservingItems.flush(STACKS,e,b,8,1)==8&&e.count()==0,"buffer drains");
        a=new Store(20,64);b=new Store(0,64);e=new Buffer();b.throwActual=true;boolean thrown=false;try{ConservingItems.move(STACKS,e,a,b,8,1);}catch(IllegalStateException ex){thrown=true;}
        check(thrown&&e.count()==8&&a.count()==12,"exception retains items");
        a=new Store(20,64);b=new Store(0,64);e=new Buffer();b.wrongSimulation=true;thrown=false;try{ConservingItems.move(STACKS,e,a,b,8,1);}catch(IllegalStateException ex){thrown=true;}
        check(thrown&&a.count()==20&&e.count()==0,"malformed simulation never extracts");
        a=new Store(20,64);e=new Buffer();check(ConservingItems.move(STACKS,e,a,a,8,1)==0&&a.count()==20,"self transfer");
        for(int limit=1;limit<=20;limit++){a=new Store(20,64);b=new Store(0,64);e=new Buffer();ConservingItems.move(STACKS,e,a,b,8,1,new ConservingItems.Work(limit));check(a.calls+b.calls<=limit,"work cap");check(a.count()+b.count()+e.count()==20,"budget conservation");}
        final Set<BoundedNetwork.Point> nodes=new HashSet<BoundedNetwork.Point>();BoundedNetwork.Graph graph=new BoundedNetwork.Graph(){public boolean connected(BoundedNetwork.Point p){return nodes.contains(p);}};
        for(int i=0;i<256;i++)nodes.add(new BoundedNetwork.Point(i,0,0));
        for(int limit=1;limit<=256;limit++){BoundedNetwork.Plan plan=BoundedNetwork.scan(graph,new BoundedNetwork.Point(0,0,0),limit);check(plan.overflow==(limit<256),"graph boundary");check(plan.overflow?plan.nodes.isEmpty():plan.nodes.size()==256,"overflow fails closed");}
        nodes.remove(new BoundedNetwork.Point(100,0,0));check(BoundedNetwork.scan(graph,new BoundedNetwork.Point(0,0,0),256).nodes.size()==100,"disconnected graph");check(BoundedNetwork.scan(graph,new BoundedNetwork.Point(999,0,0),256).nodes.isEmpty(),"missing anchor");
        for(int mask=0;mask<64;mask++){nodes.clear();BoundedNetwork.Point root=new BoundedNetwork.Point(0,0,0);nodes.add(root);for(int side=0;side<6;side++)if((mask&(1<<side))!=0)nodes.add(root.offset(side));check(BoundedNetwork.scan(graph,root,256).nodes.size()==1+Integer.bitCount(mask),"six sides");}
        System.out.println("PASS legacy production rules: "+checks+" assertions (not native Minecraft acceptance)");
    }
}
