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
    private NBTTagCompound opaque;
    private static final class Record {
        final String owner;final BlockPos target;
        Record(String owner,BlockPos target){this.owner=owner;this.target=target;}
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
    public boolean blocked(String owner,BlockPos target){return opaque!=null||targets.containsKey(new Record(owner,target).key());}
    /** Bounded sorted read-only snapshots. Does not load inventories or settle any operation. */
    public String[] describe(String owner,BlockPos target,int page){
        if(page<1||page>1024)throw new IllegalArgumentException("Journal page out of range");
        if(opaque!=null)return new String[]{"PL4 item journal: opaque saved data; all targets fenced; preserve save for inspection."};
        java.util.List<String> tokens=new java.util.ArrayList<String>();
        String key=new Record(owner,target).key();
        for(Map.Entry<String,Record> entry:records.entrySet())if(entry.getValue().key().equals(key))tokens.add(entry.getKey());
        java.util.Collections.sort(tokens);int pages=Math.max(1,(tokens.size()+3)/4);
        if(page>pages)throw new IllegalArgumentException("Journal page exceeds "+pages);
        java.util.List<String> lines=new java.util.ArrayList<String>();
        lines.add("PL4 item journal: "+tokens.size()+" unresolved at "+target.getX()+","+target.getY()+","+target.getZ()+"; page "+page+"/"+pages+"; capacity "+records.size()+"/"+LIMIT+". Read only.");
        for(int i=(page-1)*4;i<Math.min(page*4,tokens.size());i++)lines.add("Token "+tokens.get(i)+"; inspection required; no automatic replay.");
        return lines.toArray(new String[lines.size()]);
    }
    private static void link(LegacyTile node,String token,Record record,String phase){
        NBTTagCompound context=new NBTTagCompound();context.setString("Token",token);context.setString("Owner",record.owner);
        context.setInteger("Dimension",node.getWorld().provider.getDimension());context.setInteger("X",record.target.getX());context.setInteger("Y",record.target.getY());context.setInteger("Z",record.target.getZ());context.setString("Phase",phase);
        node.itemJournalContext=context;node.fluidDirty();
    }
    private static void unlink(LegacyTile node,String token){
        if(node.itemJournalContext!=null&&token.equals(node.itemJournalContext.getString("Token"))){node.itemJournalContext=null;node.fluidDirty();}
    }
    public String receiptStatus(NBTTagCompound context){
        if(context==null)return "PL4 item receipt: unlinked legacy fault; original target/outcome unknown. Preserve receipt.";
        String token=context.getString("Token");Record record=records.get(token);
        if(opaque!=null)return "PL4 item receipt: opaque journal; preserve receipt and saved data.";
        if(record==null)return "PL4 item receipt: token not present in this dimension; this is not recovery clearance.";
        boolean matches=record.owner.equals(context.getString("Owner"))&&record.target.getX()==context.getInteger("X")&&record.target.getY()==context.getInteger("Y")&&record.target.getZ()==context.getInteger("Z");
        return matches?"PL4 item receipt: unresolved token "+token+" at "+record.target.getX()+","+record.target.getY()+","+record.target.getZ()+"; preserve receipt; no replay.":"PL4 item receipt: context mismatch; preserve receipt and inspect saved data.";
    }
    public static String receiptDiagnostic(World world,net.minecraft.entity.player.EntityPlayer player,ItemStack receipt){
        if(receipt==null||receipt.isEmpty()||receipt.getItem()!=LegacyPL4.ITEM_CELL)throw new IllegalArgumentException("Hold a sealed PL4 item receipt");
        NBTTagCompound tag=receipt.getTagCompound(),context=tag==null?null:tag.getCompoundTag("ItemJournalContext");
        if(context!=null&&!tag.hasKey("ItemJournalContext"))context=null;
        // Unlinked historical receipts have no trustworthy owner; operator inspection only.
        if(player==null||(!(context!=null&&player.getUniqueID().toString().equals(context.getString("Owner")))&&!player.canUseCommand(2,"pl4legacy")))throw new IllegalArgumentException("Receipt inspection requires its recorded owner or an operator");
        if(context!=null&&context.getInteger("Dimension")!=world.provider.getDimension())return "PL4 item receipt: different dimension; inspect in recorded dimension. No clearance.";
        return data(world).receiptStatus(context);
    }

    private void add(String token,Record record){
        records.put(token,record);String key=record.key();Integer n=targets.get(key);targets.put(key,n==null?1:n+1);
    }
    private void begin(String token,Record record){
        if(opaque!=null)throw new IllegalStateException("Unresolved wireless journal data; no provider call permitted");
        if(records.containsKey(token))return;
        if(records.size()>=LIMIT)throw new IllegalStateException("Wireless uncertainty journal full; no provider call permitted");
        add(token,record);markDirty();
    }
    private void finish(String token){
        Record record=records.remove(token);if(record==null)return;
        String key=record.key();int n=targets.get(key);if(n==1)targets.remove(key);else targets.put(key,n-1);markDirty();
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
        records.clear();targets.clear();opaque=null;
        try{
            if(tag.getInteger("Schema")!=1||!tag.hasKey("Entries",9))throw new IllegalArgumentException();
            NBTTagList list=(NBTTagList)tag.getTag("Entries");
            if(list.tagCount()>LIMIT||(list.tagCount()>0&&list.getTagType()!=10))throw new IllegalArgumentException();
            for(int i=0;i<list.tagCount();i++){
                NBTTagCompound row=list.getCompoundTagAt(i);String token=row.getString("Token"),owner=row.getString("Owner");
                if(!UUID.fromString(token).toString().equals(token)||owner.isEmpty()||owner.length()>128||records.containsKey(token)||!row.hasKey("X",3)||!row.hasKey("Y",3)||!row.hasKey("Z",3))throw new IllegalArgumentException();
                int y=row.getInteger("Y");if(y<0||y>255)throw new IllegalArgumentException();
                add(token,new Record(owner,new BlockPos(row.getInteger("X"),y,row.getInteger("Z"))));
            }
        }catch(RuntimeException invalid){records.clear();targets.clear();opaque=(NBTTagCompound)tag.copy();}
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag){
        if(opaque!=null)return (NBTTagCompound)opaque.copy();
        tag.setInteger("Schema",1);NBTTagList list=new NBTTagList();
        for(Map.Entry<String,Record> entry:records.entrySet()){
            Record r=entry.getValue();NBTTagCompound row=new NBTTagCompound();row.setString("Token",entry.getKey());row.setString("Owner",r.owner);
            row.setInteger("X",r.target.getX());row.setInteger("Y",r.target.getY());row.setInteger("Z",r.target.getZ());list.appendTag(row);
        }
        tag.setTag("Entries",list);return tag;
    }
}
