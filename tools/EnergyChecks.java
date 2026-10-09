import net.foundations.pl4.legacy.ConservingEnergy;
import net.foundations.pl4.legacy.ConservingItems;

/** Tests production integer transfer rules without a Minecraft runtime. */
public final class EnergyChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static final class Buffer implements ConservingEnergy.Escrow {
        int n;boolean fault;
        public int get(){return n;}public void set(int v){n=v;}
        public boolean blocked(){return fault;}public void block(String r){fault=true;}public void clearBlock(){fault=false;}
    }
    static final class Store implements ConservingEnergy.Port {
        int n,capacity,calls,exec,mode;
        Store(int n,int capacity){this.n=n;this.capacity=capacity;}
        public Object identity(){return this;}
        public int extract(int max,boolean sim){calls++;if(mode==4&&sim)return -1;int take=Math.min(max,n);if(!sim){exec++;if(mode==5)throw new IllegalStateException("extract failure");if(mode==6){n-=Math.min(n,take+1);return take+1;}n-=take;}return take;}
        public int receive(int max,boolean sim){calls++;if(mode==3&&sim)return max+1;int take=Math.min(max,capacity-n);if(!sim){exec++;if(mode==1)return 0;if(mode==2){n+=take;throw new IllegalStateException("insert failed after mutation");}if(mode==7)return -1;n+=take;}return take;}
    }
    public static void main(String[] args) {
        checks=0;
        for(int source=0;source<=80;source++)for(int occupied=0;occupied<=80;occupied++)for(int rate:new int[]{1,17,80,100000}) {
            Store a=new Store(source,80),b=new Store(occupied,80);Buffer e=new Buffer();
            int moved=ConservingEnergy.move(e,a,b,rate,new ConservingItems.Work(64));
            check(moved==Math.min(source,Math.min(rate,80-occupied)),"bounded transfer");
            check(a.n+b.n+e.n==source+occupied,"conservation");
            check(!e.fault&&e.n==0,"healthy transfer clears escrow");
        }
        for(int limit=1;limit<=64;limit++) {
            Store a=new Store(100000,100000),b=new Store(0,100000);Buffer e=new Buffer();
            ConservingEnergy.move(e,a,b,400,new ConservingItems.Work(limit));
            check(a.calls+b.calls<=limit,"shared work limit");check(a.n+b.n+e.n==100000,"budget interruption conserves");check(!e.fault,"budget alone never quarantines");
        }
        for(int mode:new int[]{1,2,3,4,5,6,7}) {
            Store a=new Store(100,100),b=new Store(0,100);Buffer e=new Buffer();
            if(mode>=4&&mode<=6)a.mode=mode;else b.mode=mode;
            try { ConservingEnergy.move(e,a,b,40,new ConservingItems.Work(64)); check(mode==1,"only ordinary refusal returns normally"); }
            catch(IllegalStateException expected){check(mode!=1,"invalid provider rejected");}
            if(mode==1){check(e.n==40&&a.n==60&&!e.fault,"refusal retains healthy escrow");b.mode=0;check(ConservingEnergy.move(e,null,b,40,new ConservingItems.Work(64))==40&&a.n==60,"flush without reextracting");}
            if(mode==2||mode==5||mode==6||mode==7){check(e.fault,"uncertain operation quarantined");int calls=a.calls+b.calls;check(ConservingEnergy.move(e,a,b,40,new ConservingItems.Work(64))==0&&a.calls+b.calls==calls,"no replay");}
            if(mode==3||mode==4)check(a.n==100&&e.n==0,"invalid simulation never extracts");
        }
        Store a=new Store(100,100);Buffer e=new Buffer();check(ConservingEnergy.move(e,a,a,40,new ConservingItems.Work(64))==0&&a.calls==0,"self transfer");
        e.n=100;Store b=new Store(0,30);check(ConservingEnergy.move(e,null,b,40,new ConservingItems.Work(64))==30&&e.n==70,"partial pending flush");
        for(int bad:new int[]{-1,100001,Integer.MAX_VALUE}){try{ConservingEnergy.move(e,null,b,bad,new ConservingItems.Work(64));throw new AssertionError("bad budget");}catch(IllegalArgumentException expected){check(true,"budget rejected");}}
        System.out.println("PASS legacy energy production rules: "+checks+" assertions (not native Minecraft acceptance)");
    }
}
