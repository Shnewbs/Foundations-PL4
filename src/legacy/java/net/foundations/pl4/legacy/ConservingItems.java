package net.foundations.pl4.legacy;
/** Synchronous sided transport. Escrow persists extraction before insertion; adapters must obey simulation. */
public final class ConservingItems {
    public interface Stacks<S>{boolean empty(S value);int count(S value);boolean same(S a,S b);S copy(S value,int count);}
    public interface Inventory<S>{int slots();S extract(int slot,int count,boolean simulate);S insert(int slot,S value,boolean simulate);}
    public interface Escrow<S>{S get();void set(S value);}
    /** Shared across all candidate inventories in one operation; bounds provider calls, not only moved items. */
    public static final class Work {
        private int left;
        public Work(int limit){if(limit<1||limit>65536)throw new IllegalArgumentException("Invalid work limit");left=limit;}
        public boolean take(){if(left==0)return false;left--;return true;}
        public boolean available(){return left>0;}
    }
    private static <S> int remainder(Stacks<S> s,S offered,S remaining){
        if(s.empty(remaining))return 0;
        if(!s.same(offered,remaining)||s.count(remaining)<0||s.count(remaining)>s.count(offered))throw new IllegalStateException("Inventory returned a malformed remainder");
        return s.count(remaining);
    }
    private static void validate(int budget,int slots){if(budget<0||budget>64||slots<1||slots>4096)throw new IllegalArgumentException("Invalid transfer budget");}
    public static <S> int flush(Stacks<S> s,Escrow<S> e,Inventory<S> sink,int budget,int slots){return flush(s,e,sink,budget,slots,new Work(1024));}
    public static <S> int move(Stacks<S> s,Escrow<S> e,Inventory<S> source,Inventory<S> sink,int budget,int slots){return move(s,e,source,sink,budget,slots,new Work(1024));}
    public static <S> int flush(Stacks<S> s,Escrow<S> e,Inventory<S> sink,int budget,int slotLimit,Work work){
        validate(budget,slotLimit);S pending=e.get();if(s.empty(pending)||budget==0||sink==null||!work.take())return 0;
        int moved=0,slots=Math.max(0,Math.min(slotLimit,sink.slots()));
        for(int slot=0;slot<slots&&!s.empty(pending)&&moved<budget;slot++){
            if(!work.take())break;int count=Math.min(s.count(pending),budget-moved);S offered=s.copy(pending,count);
            int accepted=count-remainder(s,offered,sink.insert(slot,s.copy(offered,count),true));if(accepted<=0)continue;
            if(!work.take())break;offered=s.copy(pending,accepted);
            int actual=accepted-remainder(s,offered,sink.insert(slot,s.copy(offered,accepted),false));if(actual==0)continue;
            int left=s.count(pending)-actual;pending=left==0?null:s.copy(pending,left);e.set(pending);moved+=actual;
        }
        return moved;
    }
    public static <S> int move(Stacks<S> s,Escrow<S> e,Inventory<S> source,Inventory<S> sink,int budget,int slotLimit,Work work){
        validate(budget,slotLimit);if(budget==0||source==null||sink==null||source==sink)return 0;
        if(!s.empty(e.get()))return flush(s,e,sink,budget,slotLimit,work);
        if(!work.take())return 0;int slots=Math.max(0,Math.min(slotLimit,source.slots()));
        if(!work.take())return 0;int destinations=Math.max(0,Math.min(slotLimit,sink.slots()));
        for(int slot=0;slot<slots&&work.available();slot++){
            if(!work.take())break;S trial=source.extract(slot,budget,true);if(s.empty(trial))continue;
            if(s.count(trial)<1||s.count(trial)>budget)throw new IllegalStateException("Invalid simulated extraction");
            S remaining=s.copy(trial,s.count(trial));
            for(int dest=0;dest<destinations&&!s.empty(remaining);dest++){
                if(!work.take())return 0;
                int left=remainder(s,remaining,sink.insert(dest,s.copy(remaining,s.count(remaining)),true));remaining=left==0?null:s.copy(remaining,left);
            }
            int amount=s.count(trial)-(s.empty(remaining)?0:s.count(remaining));if(amount==0)continue;
            if(!work.take())return 0;S extracted=source.extract(slot,amount,false);if(s.empty(extracted))continue;
            e.set(s.copy(extracted,s.count(extracted)));
            // Unexpected actual variants remain in escrow, not forwarded as the simulated item.
            if(!s.same(trial,extracted)||s.count(extracted)>amount)return 0;
            return flush(s,e,sink,budget,slotLimit,work);
        }
        return 0;
    }
    private ConservingItems(){}
}
