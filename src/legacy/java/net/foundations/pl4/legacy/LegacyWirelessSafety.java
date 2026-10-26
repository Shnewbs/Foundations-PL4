package net.foundations.pl4.legacy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
/** Per-dimension target fences for item handoffs; independent of tile/network lifetime. */
public final class LegacyWirelessSafety extends WorldSavedData {
    public static final String DATA_NAME="foundations_pl4_wireless_uncertainty";
    public static final int LIMIT=4096;
    private final Map<String,Record> records=new HashMap<String,Record>();
    private final Map<String,Integer> targets=new HashMap<String,Integer>();
    private final Map<String,Integer> legacyOwners=new HashMap<String,Integer>();
    private NBTTagCompound opaque;
    private static final class Record {
        // For legacy imports this position is the observed host, never an inferred mutation target.
        final String owner;final BlockPos target;final NBTTagCompound evidence;
        Record(String owner,BlockPos target){this(owner,target,null);}
        Record(String owner,BlockPos target,NBTTagCompound evidence){this.owner=owner;this.target=target;this.evidence=evidence;}
        boolean legacy(){return evidence!=null;}
        String key(){return owner+":"+target.getX()+":"+target.getY()+":"+target.getZ();}
    }
    public LegacyWirelessSafety(String name){super(name);}
    public static LegacyWirelessSafety data(World world){
        if(world==null||world.isRemote)throw new IllegalStateException("Wireless journal requires server world");
        MapStorage storage=world.getPerWorldStorage();
        LegacyWirelessSafety data=(LegacyWirelessSafety)storage.getOrLoadData(LegacyWirelessSafety.class,DATA_NAME);
        if(data==null){data=new LegacyWirelessSafety(DATA_NAME);storage.setData(DATA_NAME,data);}
        return data;
    }
    public boolean blocked(String owner,BlockPos target){Record query=new Record(owner,target);return opaque!=null||legacyOwners.containsKey("")||legacyOwners.containsKey(query.owner)||targets.containsKey(query.key());}
    /** Bounded sorted read-only snapshots. Does not load inventories or settle any operation. */
    public String[] describe(String owner,BlockPos target,int page){
        if(page<1||page>1024)throw new IllegalArgumentException("Journal page out of range");
        if(opaque!=null)return new String[]{"PL4 item journal: opaque saved data; all targets fenced; preserve save for inspection."};
        java.util.List<String> tokens=new java.util.ArrayList<String>();
        String key=new Record(owner,target).key();
        for(Map.Entry<String,Record> entry:records.entrySet())if(entry.getValue().legacy()?(entry.getValue().owner.isEmpty()||entry.getValue().owner.equals(new Record(owner,target).owner)):entry.getValue().key().equals(key))tokens.add(entry.getKey());
        java.util.Collections.sort(tokens);int pages=Math.max(1,(tokens.size()+3)/4);
        if(page>pages)throw new IllegalArgumentException("Journal page exceeds "+pages);
        java.util.List<String> lines=new java.util.ArrayList<String>();
        lines.add("PL4 item journal: "+tokens.size()+" unresolved affecting queried target "+target.getX()+","+target.getY()+","+target.getZ()+"; page "+page+"/"+pages+"; capacity "+records.size()+"/"+LIMIT+". Read only.");
        for(int i=(page-1)*4;i<Math.min(page*4,tokens.size());i++){String token=tokens.get(i);lines.add("Token "+token+(records.get(token).legacy()?"; legacy import: original target/phase unknown; "+(records.get(token).owner.isEmpty()?"dimension-wide":"owner-wide")+" fence; no replay.":"; inspection required; no automatic replay."));}
        return lines.toArray(new String[lines.size()]);
    }
    private static void link(LegacyTile node,String token,Record record,String phase){
        NBTTagCompound context=new NBTTagCompound();context.setString("Token",token);context.setString("Owner",record.owner);
        context.setInteger("Dimension",node.getWorld().provider.getDimension());context.setInteger("X",record.target.getX());context.setInteger("Y",record.target.getY());context.setInteger("Z",record.target.getZ());context.setString("Phase",phase);
        node.itemJournalContext=context;node.fluidDirty();
    }
    private static void unlink(LegacyTile node,String token){
        if(node.itemJournalContext!=null&&token.equals(text(node.itemJournalContext,"Token"))){node.itemJournalContext=null;node.fluidDirty();}
    }
    private static String text(NBTTagCompound tag,String key){return tag.getTag(key) instanceof net.minecraft.nbt.NBTTagString?tag.getString(key):"";}
    private static String receiptPhase(NBTTagCompound context){
        String phase=text(context,"Phase");
        return "wireless".equals(phase)||"extract".equals(phase)||"insert".equals(phase)?phase:"unknown";
    }
    public String receiptStatus(NBTTagCompound context){
        if(context==null)return "PL4 item receipt: unlinked legacy fault; original target/outcome unknown. Preserve receipt.";
        String token=text(context,"Token");Record record=records.get(token);
        if(opaque!=null)return "PL4 item receipt: opaque journal; preserve receipt and saved data.";
        if(record==null)return "PL4 item receipt: token not present in this dimension; this is not recovery clearance.";
        boolean matches=matches(record,context);
        if(matches&&record.legacy())return "PL4 item receipt: unresolved legacy token "+token+"; original target/phase unknown; observed host only; "+(record.owner.isEmpty()?"dimension-wide":"owner-wide")+" fence; preserve receipt; no replay.";
        return matches?"PL4 item receipt: unresolved token "+token+"; recorded phase "+receiptPhase(context)+" at "+record.target.getX()+","+record.target.getY()+","+record.target.getZ()+"; preserve receipt; no replay.":"PL4 item receipt: context mismatch; preserve receipt and inspect saved data.";
    }
    public static String receiptDiagnostic(World world,net.minecraft.entity.player.EntityPlayer player,ItemStack receipt){
        if(receipt==null||receipt.isEmpty()||receipt.getItem()!=LegacyPL4.ITEM_CELL)throw new IllegalArgumentException("Hold a sealed PL4 item receipt");
        NBTTagCompound tag=receipt.getTagCompound(),context=tag!=null&&tag.getTag("ItemJournalContext") instanceof NBTTagCompound?tag.getCompoundTag("ItemJournalContext"):null;
        if(context!=null&&!tag.hasKey("ItemJournalContext"))context=null;
        boolean authenticatedContext=context!=null&&context.getTag("Owner") instanceof net.minecraft.nbt.NBTTagString&&context.getTag("Dimension") instanceof net.minecraft.nbt.NBTTagInt;
        // Unlinked historical receipts have no trustworthy owner; operator inspection only.
        if(player==null||(!(authenticatedContext&&player.getUniqueID().toString().equals(text(context,"Owner")))&&!player.canUseCommand(2,"pl4legacy")))throw new IllegalArgumentException("Receipt inspection requires its recorded owner or an operator");
        if(context!=null&&!authenticatedContext)return "PL4 item receipt: malformed owner/dimension context; preserve receipt. No recovery clearance.";
        if(context!=null&&context.getInteger("Dimension")!=world.provider.getDimension())return "PL4 item receipt: different dimension; inspect in recorded dimension. No clearance.";
        return data(world).receiptStatus(context);
    }

    private static boolean matches(Record record,NBTTagCompound context){
        String prefix=record.legacy()?"Host":"";
        return context.getTag("Owner") instanceof net.minecraft.nbt.NBTTagString&&context.getTag("Dimension") instanceof net.minecraft.nbt.NBTTagInt&&(!context.hasKey("Scope")||context.getTag("Scope") instanceof net.minecraft.nbt.NBTTagString)&&record.owner.equals(text(context,"Owner"))&&(record.legacy()?"legacy-unknown".equals(text(context,"Scope")):text(context,"Scope").isEmpty())
            &&context.getTag(prefix+"X") instanceof net.minecraft.nbt.NBTTagInt&&context.getTag(prefix+"Y") instanceof net.minecraft.nbt.NBTTagInt&&context.getTag(prefix+"Z") instanceof net.minecraft.nbt.NBTTagInt
            &&record.target.getX()==context.getInteger(prefix+"X")&&record.target.getY()==context.getInteger(prefix+"Y")&&record.target.getZ()==context.getInteger(prefix+"Z");
    }
    /** An old fault has no reliable route history. Fence its observed owner in this dimension, not a guessed endpoint. */
    public void importLegacy(LegacyTile node){
        if(!LegacyItemSafety.blocked(node))return;
        NBTTagCompound previous=node.itemJournalContext;
        Record known=previous==null?null:records.get(text(previous,"Token"));
        if(known!=null&&matches(known,previous)&&known.owner.equals(node.owner)&&previous.getTag("Dimension") instanceof net.minecraft.nbt.NBTTagInt&&previous.getInteger("Dimension")==node.getWorld().provider.getDimension())return;
        // Never replace another rejected context once original legacy evidence exists.
        // Preserve the first rejection and freeze this dimension for reviewed reconciliation.
        if(node.itemLegacyEvidence!=null&&previous!=null){
            if(opaque==null){
                NBTTagCompound retained=new NBTTagCompound();retained.setTag("PreviousJournal",writeToNBT(new NBTTagCompound()));
                retained.setTag("RejectedContext",previous.copy());retained.setTag("ExistingLegacyEvidence",node.itemLegacyEvidence.copy());
                opaque=retained;markDirty();
            }
            node.fluidDirty();return;
        }
        if(node.itemLegacyEvidence==null){
            node.itemLegacyEvidence=new NBTTagCompound();node.itemLegacyEvidence.setString("Fault",node.itemFault);node.itemLegacyEvidence.setString("ObservedOwner",node.owner);node.itemLegacyEvidence.setInteger("ObservedDimension",node.getWorld().provider.getDimension());node.itemLegacyEvidence.setInteger("HostX",node.getPos().getX());node.itemLegacyEvidence.setInteger("HostY",node.getPos().getY());node.itemLegacyEvidence.setInteger("HostZ",node.getPos().getZ());
            if(previous!=null)node.itemLegacyEvidence.setTag("PreviousContext",previous.copy());
        }
        if(opaque!=null){node.fluidDirty();return;}
        BlockPos observed=node.getPos();
        if(records.size()>=LIMIT||node.owner.length()>128||observed.getY()<0||observed.getY()>255){
            NBTTagCompound saved=new NBTTagCompound();saved=writeToNBT(saved);
            NBTTagCompound retained=new NBTTagCompound();retained.setTag("PreviousJournal",saved);retained.setTag("UnlocatedLegacyFault",node.itemLegacyEvidence.copy());opaque=retained;markDirty();node.fluidDirty();return;
        }
        String token=UUID.randomUUID().toString();Record record=new Record(node.owner,observed,(NBTTagCompound)node.itemLegacyEvidence.copy());begin(token,record);
        NBTTagCompound context=new NBTTagCompound();context.setString("Token",token);context.setString("Owner",record.owner);context.setInteger("Dimension",node.getWorld().provider.getDimension());context.setString("Scope","legacy-unknown");
        context.setInteger("HostX",observed.getX());context.setInteger("HostY",observed.getY());context.setInteger("HostZ",observed.getZ());
        node.itemJournalContext=context;node.fluidDirty();
    }
    private void add(String token,Record record){
        records.put(token,record);Map<String,Integer> index=record.legacy()?legacyOwners:targets;String key=record.legacy()?record.owner:record.key();Integer n=index.get(key);index.put(key,n==null?1:n+1);
    }
    private void begin(String token,Record record){
        if(opaque!=null)throw new IllegalStateException("Unresolved wireless journal data; no provider call permitted");
        if(records.containsKey(token))return;
        if(records.size()>=LIMIT)throw new IllegalStateException("Wireless uncertainty journal full; no provider call permitted");
        add(token,record);markDirty();
    }
    private void finish(String token){
        Record record=records.remove(token);if(record==null)return;
        Map<String,Integer> index=record.legacy()?legacyOwners:targets;String key=record.legacy()?record.owner:record.key();int n=index.get(key);if(n==1)index.remove(key);else index.put(key,n-1);markDirty();
    }
    /** Capture owner/target before invoking provider callbacks; one token per bounded handoff. */
    public static ConservingItems.Escrow<ItemStack> escrow(final LegacyTile node){
        final LegacyWirelessSafety journal=data(node.getWorld());
        final Record record=new Record(node.owner,node.target());
        final String token=UUID.randomUUID().toString();
        final ConservingItems.Escrow<ItemStack> delegate=LegacyItemSafety.escrow(node);
        return new ConservingItems.Escrow<ItemStack>(){
            public ItemStack get(){return delegate.get();}
            public void set(ItemStack value){delegate.set(value);}
            public boolean blocked(){return delegate.blocked();}
            public void block(String reason){delegate.block(reason);journal.begin(token,record);link(node,token,record,"wireless");}
            public void clearBlock(){delegate.clearBlock();if(!delegate.blocked()&&LegacyTile.STACKS.empty(delegate.get())){journal.finish(token);unlink(node,token);}}
        };
    }
    /** Automatic transfers reserve only the foreign endpoint about to mutate.
     * Validated known escrow may retry later, so healthy phase settlement releases its own token. */
    public static ConservingItems.PhasedEscrow<ItemStack> automaticEscrow(final LegacyTile node,BlockPos source,BlockPos destination){
        final LegacyWirelessSafety journal=data(node.getWorld());
        final Record sourceRecord=new Record(node.owner,source),destinationRecord=new Record(node.owner,destination);
        // Mutation tokens are allocated lazily; idle and simulated-refusal routes need none.
        final ConservingItems.Escrow<ItemStack> delegate=LegacyItemSafety.escrow(node);
        return new ConservingItems.PhasedEscrow<ItemStack>(){
            private Record active;private String token,sourceToken,destinationToken;
            public void extracting(){active=sourceRecord;if(sourceToken==null)sourceToken=UUID.randomUUID().toString();token=sourceToken;}
            public void inserting(){active=destinationRecord;if(destinationToken==null)destinationToken=UUID.randomUUID().toString();token=destinationToken;}
            public ItemStack get(){return delegate.get();}
            public void set(ItemStack value){delegate.set(value);}
            public boolean blocked(){return delegate.blocked();}
            public void block(String reason){
                delegate.block(reason);
                if(active==null)throw new IllegalStateException("Automatic mutation phase missing; no provider call permitted");
                journal.begin(token,active);link(node,token,active,active==sourceRecord?"extract":"insert");
            }
            public void clearBlock(){delegate.clearBlock();if(!delegate.blocked()){journal.finish(token);unlink(node,token);}}
        };
    }
    @Override public void readFromNBT(NBTTagCompound tag){
        records.clear();targets.clear();legacyOwners.clear();opaque=null;
        try{

            int schema=tag.getInteger("Schema");if((schema!=1&&schema!=2)||!(tag.getTag("Entries") instanceof NBTTagList))throw new IllegalArgumentException();
            NBTTagList list=(NBTTagList)tag.getTag("Entries");
            if(list.tagCount()>LIMIT)throw new IllegalArgumentException();
            for(int i=0;i<list.tagCount();i++){
                if(!(list.get(i) instanceof NBTTagCompound))throw new IllegalArgumentException();
                NBTTagCompound row=list.getCompoundTagAt(i);String token=row.getString("Token"),owner=row.getString("Owner"),scope=row.getString("Scope");
                boolean legacy="legacy-unknown".equals(scope);String prefix=legacy?"Host":"";
                if(!UUID.fromString(token).toString().equals(token)||owner.length()>128||(!legacy&&owner.isEmpty())||records.containsKey(token)||(!scope.isEmpty()&&!legacy)||(legacy&&schema!=2)
                    ||!(row.getTag(prefix+"X") instanceof net.minecraft.nbt.NBTTagInt)||!(row.getTag(prefix+"Y") instanceof net.minecraft.nbt.NBTTagInt)||!(row.getTag(prefix+"Z") instanceof net.minecraft.nbt.NBTTagInt)
                    ||(legacy&&!(row.getTag("Evidence") instanceof NBTTagCompound)))throw new IllegalArgumentException();
                int y=row.getInteger(prefix+"Y");if(y<0||y>255)throw new IllegalArgumentException();
                add(token,new Record(owner,new BlockPos(row.getInteger(prefix+"X"),y,row.getInteger(prefix+"Z")),legacy?(NBTTagCompound)row.getCompoundTag("Evidence").copy():null));
            }
        }catch(RuntimeException invalid){records.clear();targets.clear();legacyOwners.clear();opaque=(NBTTagCompound)tag.copy();}
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag){
        if(opaque!=null)return (NBTTagCompound)opaque.copy();
        tag.setInteger("Schema",2);NBTTagList list=new NBTTagList();
        for(Map.Entry<String,Record> entry:records.entrySet()){
            Record r=entry.getValue();NBTTagCompound row=new NBTTagCompound();row.setString("Token",entry.getKey());row.setString("Owner",r.owner);String prefix=r.legacy()?"Host":"";
            if(r.legacy()){row.setString("Scope","legacy-unknown");row.setTag("Evidence",r.evidence.copy());}
            row.setInteger(prefix+"X",r.target.getX());row.setInteger(prefix+"Y",r.target.getY());row.setInteger(prefix+"Z",r.target.getZ());list.appendTag(row);
        }
        tag.setTag("Entries",list);return tag;
    }
}

