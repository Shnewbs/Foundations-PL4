import net.foundations.pl4.legacy.*;
/** Executes the real shared fluid algorithm without Minecraft dependencies. */
public final class FluidChecks {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static final class Stack { final int variant,amount;Stack(int v,int a){variant=v;amount=a;} }
    static final ConservingItems.Stacks<Stack> S=new ConservingItems.Stacks<Stack>(){
        public boolean empty(Stack s){return s==null||s.amount==0;}
        public int count(Stack s){return s==null?0:s.amount;}
        public boolean same(Stack a,Stack b){return a!=null&&b!=null&&a.variant==b.variant;}
        public Stack copy(Stack s,int n){return n==0?null:new Stack(s.variant,n);}
    };
    static final class Buffer implements ConservingFluids.Escrow<Stack>{
        Stack value;String fault="";public Stack get(){return value;}public void set(Stack s){value=s;}
        public boolean blocked(){return !fault.isEmpty();}public void block(String reason){fault=reason;}public void clearBlock(){fault="";}
    }
    static final class Tank implements ConservingFluids.Port<Stack>{
        int amount,capacity,calls,variant=1;boolean refuse,failFill,failDrain,invalidFill,wrongDrain;
        public Object identity(){return this;}
        Tank(int amount,int capacity){this.amount=amount;this.capacity=capacity;}
        public Stack drain(int n,boolean simulate){calls++;int k=Math.min(n,amount);if(!simulate){if(failDrain)throw new IllegalStateException("drain fault");amount-=k;}return k==0?null:new Stack(variant,k);}
        public Stack drain(Stack wanted,boolean simulate){if(wanted.variant!=variant)return null;Stack s=drain(wanted.amount,simulate);return wrongDrain&&!simulate&&s!=null?new Stack(2,s.amount):s;}
        public int fill(Stack offered,boolean simulate){calls++;if(!simulate&&failFill)throw new IllegalStateException("fill fault");if(!simulate&&invalidFill)return offered.amount+1;if((!simulate&&refuse)||(amount>0&&variant!=offered.variant))return 0;int n=Math.min(offered.amount,capacity-amount);if(!simulate){amount+=n;variant=offered.variant;}return n;}
    }
    static int move(Tank a,Tank b,Buffer e,int budget,int calls){return ConservingFluids.move(S,e,a,b,budget,new ConservingItems.Work(calls));}
    public static void main(String[] args){
        for(int source=0;source<=16000;source+=250)for(int occupied=0;occupied<=16000;occupied+=250)for(int rate:new int[]{1,250,1000,16000}){
            Tank a=new Tank(source,16000),b=new Tank(occupied,16000);Buffer e=new Buffer();int n=move(a,b,e,rate,64);
            check(n==Math.min(source,Math.min(rate,16000-occupied)),"rate and capacity");
            check(a.amount+b.amount+S.count(e.value)==source+occupied,"conservation");check(!e.blocked(),"healthy providers not quarantined");
        }
        for(int cap=1;cap<=8;cap++){Tank a=new Tank(1000,16000),b=new Tank(0,16000);Buffer e=new Buffer();move(a,b,e,250,cap);check(a.calls+b.calls<=cap,"provider call budget");check(a.amount+b.amount+S.count(e.value)==1000,"budget-boundary escrow");}
        Tank a=new Tank(1000,16000),b=new Tank(0,16000);Buffer e=new Buffer();b.refuse=true;
        check(move(a,b,e,250,64)==0&&a.amount==750&&S.count(e.value)==250&&!e.blocked(),"actual refusal preserved");b.refuse=false;
        check(ConservingFluids.flush(S,e,b,250,new ConservingItems.Work(64))==250&&e.value==null,"escrow drain");
        for(int fault=0;fault<3;fault++){
            a=new Tank(1000,16000);b=new Tank(0,16000);e=new Buffer();b.failFill=fault==0;b.invalidFill=fault==1;a.failDrain=fault==2;
            try{move(a,b,e,250,64);}catch(IllegalStateException expected){}
            check(e.blocked(),"ambiguous execute quarantined");int calls=a.calls+b.calls;move(a,b,e,250,64);check(a.calls+b.calls==calls,"no uncertain retry");
        }
        a=new Tank(1000,16000);b=new Tank(0,16000);e=new Buffer();a.wrongDrain=true;
        check(move(a,b,e,250,64)==0&&e.blocked()&&e.value.variant==2&&e.value.amount==250,"unexpected variant held and blocked");
        a=new Tank(1000,16000);b=new Tank(100,16000);b.variant=2;e=new Buffer();check(move(a,b,e,250,64)==0&&a.amount==1000,"variants isolated");
        e=new Buffer();check(move(a,a,e,250,64)==0&&a.amount==1000,"self transfer rejected");
        System.out.println("PASS fluid production rules: "+checks+" assertions; no installed-mod or native acceptance implied");
    }
}
