package net.foundations.pl4.core;

/**
 * Direction rules for the bounded PL4 transfer pool.
 *
 * <p>Normal Nodes are passive network endpoints. A unidirectional Transfer Node is the
 * driver: REMOVE exports its attached capability into explicit ADD peers first and then
 * passive Nodes; ADD imports from explicit REMOVE peers first and then passive Nodes.
 * ADD/REMOVE is deliberately peer-only until PL2 directional channel/filter semantics are
 * restored, which prevents a bidirectional node from blindly pumping a passive network
 * back into itself.</p>
 */
public final class TransferRules {
    public static final int PASSIVE=0, ADD=1, REMOVE=2, ADD_REMOVE=3;

    public static boolean drivesAdd(int mode){return mode==ADD;}
    public static boolean drivesRemove(int mode){return mode==REMOVE;}
    public static boolean peerOnly(int mode){return mode==ADD_REMOVE;}
    public static boolean canSource(int mode){return mode==REMOVE||mode==ADD_REMOVE;}
    public static boolean canSink(int mode){return mode==ADD||mode==ADD_REMOVE;}

    /**
     * Whether an endpoint pair may carry resources. Normal endpoints are represented by
     * transfer=false and ignore their mode. At least one endpoint must be a Transfer Node.
     */
    public static boolean canRoute(boolean sourceTransfer,int sourceMode,boolean sinkTransfer,int sinkMode){
        if(!sourceTransfer&&!sinkTransfer)return false;
        if(sourceTransfer&&sinkTransfer){
            if(!canSource(sourceMode)||!canSink(sinkMode))return false;
            return !(sourceMode==ADD_REMOVE&&sinkMode==ADD_REMOVE);
        }
        if(sourceTransfer)return sourceMode==REMOVE; // REMOVE -> passive Node
        return sinkMode==ADD;                        // passive Node -> ADD
    }

    /** Explicit Transfer Nodes win equal-priority ties over passive Nodes. */
    public static int endpointClass(boolean transfer){return transfer?1:0;}

    private TransferRules(){}
}
