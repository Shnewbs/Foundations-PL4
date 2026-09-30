import net.foundations.pl4.core.TransferRules;

public final class R16RegressionTests {
    private static long count;
    private static void check(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(!TransferRules.canRoute(false,0,false,0),"Normal Nodes must not move resources without a Transfer Node");
        check(TransferRules.canRoute(false,0,true,TransferRules.ADD),"Passive Node must feed ADD");
        check(!TransferRules.canRoute(false,0,true,TransferRules.REMOVE),"Passive Node must not feed REMOVE");
        check(!TransferRules.canRoute(false,0,true,TransferRules.ADD_REMOVE),"ADD/REMOVE must not blindly pull passive endpoints");
        check(TransferRules.canRoute(true,TransferRules.REMOVE,false,0),"REMOVE must feed passive Node");
        check(!TransferRules.canRoute(true,TransferRules.ADD,false,0),"ADD must not push passive Node");
        check(!TransferRules.canRoute(true,TransferRules.ADD_REMOVE,false,0),"ADD/REMOVE must not blindly push passive endpoints");
        check(TransferRules.canRoute(true,TransferRules.REMOVE,true,TransferRules.ADD),"REMOVE -> ADD");
        check(TransferRules.canRoute(true,TransferRules.REMOVE,true,TransferRules.ADD_REMOVE),"REMOVE -> ADD/REMOVE peer");
        check(TransferRules.canRoute(true,TransferRules.ADD_REMOVE,true,TransferRules.ADD),"ADD/REMOVE peer -> ADD");
        check(!TransferRules.canRoute(true,TransferRules.ADD_REMOVE,true,TransferRules.ADD_REMOVE),"Two bidirectional peers must not ping-pong");
        check(!TransferRules.canRoute(true,TransferRules.ADD,true,TransferRules.REMOVE),"Reverse direction is invalid");
        for(int source=0;source<4;source++)for(int sink=0;sink<4;sink++){
            boolean expected=TransferRules.canSource(source)&&TransferRules.canSink(sink)&&!(source==3&&sink==3);
            check(TransferRules.canRoute(true,source,true,sink)==expected,"Transfer peer matrix "+source+" -> "+sink);
        }
        for(int mode=0;mode<4;mode++){
            check(TransferRules.drivesAdd(mode)==(mode==1),"Only ADD drives imports");
            check(TransferRules.drivesRemove(mode)==(mode==2),"Only REMOVE drives exports");
            check(TransferRules.peerOnly(mode)==(mode==3),"Only ADD/REMOVE is peer-only");
        }
        check(TransferRules.endpointClass(true)>TransferRules.endpointClass(false),"Explicit transfer endpoint wins passive tie");
        for(int limit:new int[]{1,64,1000,10000000,Integer.MAX_VALUE}){
            var cap=new TransferRules.CycleLimit(limit);int first=limit/2;cap.delivered(first);
            check(cap.remaining()==limit-first,"shared cap counts actual delivery once");cap.delivered(cap.remaining());check(cap.remaining()==0,"all drivers share the exhausted budget");
            try{cap.delivered(1);throw new AssertionError("over-cap delivery accepted");}catch(IllegalArgumentException expected){check(cap.remaining()==0,"rejected delivery cannot mutate the budget");}
            check(new TransferRules.CycleLimit(limit).remaining()==limit,"next network/cycle gets an independent budget");
        }
        var unlimited=new TransferRules.CycleLimit(0);for(int i=0;i<1000;i++)unlimited.delivered(Integer.MAX_VALUE);check(unlimited.remaining()==Integer.MAX_VALUE,"uncapped delivery counters cannot overflow");
        try{new TransferRules.CycleLimit(-1);throw new AssertionError("negative cap accepted");}catch(IllegalArgumentException expected){check(true,"negative config rejected");}
        var items=new TransferRules.CycleLimit(5);var fluid=new TransferRules.CycleLimit(1000);var fe=new TransferRules.CycleLimit(10000);items.delivered(5);
        check(items.remaining()==0&&fluid.remaining()==1000&&fe.remaining()==10000,"resource caps are independent");
        System.out.println("PASS R16 transfer routing rules: "+count+" assertions.");
    }
}
