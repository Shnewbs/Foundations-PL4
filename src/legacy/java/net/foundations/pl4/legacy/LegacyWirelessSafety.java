package net.foundations.pl4.legacy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.Locale;
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
        final String owner;final BoundedNetwork.Point target;
        Record(String owner,BoundedNetwork.Point target){this.owner=owner.toLowerCase(Locale.ROOT);this.target=target;}
        String key(){return owner+":"+target.x+":"+target.y+":"+target.z;}
    }
    public LegacyWirelessSafety(String name){super(name);}
    public static LegacyWirelessSafety data(World world){
        if(world==null||world.isRemote)throw new IllegalStateException("Wireless journal requires server world");
        MapStorage storage=world.perWorldStorage;
        LegacyWirelessSafety data=(LegacyWirelessSafety)storage.loadData(LegacyWirelessSafety.class,DATA_NAME);
        if(data==null){data=new LegacyWirelessSafety(DATA_NAME);storage.setData(DATA_NAME,data);}
        return data;
    }
    public boolean blocked(String owner,BoundedNetwork.Point target){return opaque!=null||targets.containsKey(new Record(owner,target).key());}
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
        final LegacyWirelessSafety journal=data(node.worldObj);
        final Record record=new Record(node.owner,node.target());
        final String token=UUID.randomUUID().toString();
        final ConservingItems.Escrow<ItemStack> delegate=LegacyItemSafety.escrow(node);
        return new ConservingItems.Escrow<ItemStack>(){
            public ItemStack get(){return delegate.get();}
            public void set(ItemStack value){delegate.set(value);}
            public boolean blocked(){return delegate.blocked();}
            public void block(String reason){delegate.block(reason);journal.begin(token,record);}
            public void clearBlock(){delegate.clearBlock();if(!delegate.blocked()&&LegacyTile.STACKS.empty(delegate.get()))journal.finish(token);}
        };
    }
    /** Automatic transfers reserve only the foreign endpoint about to mutate.
     * Validated known escrow may retry later, so healthy phase settlement releases its own token. */
    public static ConservingItems.PhasedEscrow<ItemStack> automaticEscrow(final LegacyTile node,BoundedNetwork.Point source,BoundedNetwork.Point destination){
        final LegacyWirelessSafety journal=data(node.worldObj);
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
                journal.begin(token,active);
            }
            public void clearBlock(){delegate.clearBlock();if(!delegate.blocked())journal.finish(token);}
        };
    }
    @Override public void readFromNBT(NBTTagCompound tag){
        records.clear();targets.clear();opaque=null;
        try{
            if(tag.getInteger("Schema")==-1&&tag.getTag("Opaque") instanceof NBTTagCompound){opaque=(NBTTagCompound)tag.getCompoundTag("Opaque").copy();return;}
            if(tag.getInteger("Schema")!=1||!(tag.getTag("Entries") instanceof NBTTagList))throw new IllegalArgumentException();
            NBTTagList list=tag.getTagList("Entries");
            if(list.tagCount()>LIMIT)throw new IllegalArgumentException();
            for(int i=0;i<list.tagCount();i++){
                if(!(list.tagAt(i) instanceof NBTTagCompound))throw new IllegalArgumentException();
                NBTTagCompound row=(NBTTagCompound)list.tagAt(i);String token=row.getString("Token"),owner=row.getString("Owner");
                if(!UUID.fromString(token).toString().equals(token)||owner.isEmpty()||owner.length()>128||records.containsKey(token)||!(row.getTag("X") instanceof net.minecraft.nbt.NBTTagInt)||!(row.getTag("Y") instanceof net.minecraft.nbt.NBTTagInt)||!(row.getTag("Z") instanceof net.minecraft.nbt.NBTTagInt))throw new IllegalArgumentException();
                int y=row.getInteger("Y");if(y<0||y>255)throw new IllegalArgumentException();
                add(token,new Record(owner,new BoundedNetwork.Point(row.getInteger("X"),y,row.getInteger("Z"))));
            }
        }catch(RuntimeException invalid){records.clear();targets.clear();opaque=(NBTTagCompound)tag.copy();}
    }
    @Override public void writeToNBT(NBTTagCompound tag){
        if(opaque!=null){tag.setInteger("Schema",-1);tag.setTag("Opaque",opaque.copy());return;}
        tag.setInteger("Schema",1);NBTTagList list=new NBTTagList();
        for(Map.Entry<String,Record> entry:records.entrySet()){
            Record r=entry.getValue();NBTTagCompound row=new NBTTagCompound();row.setString("Token",entry.getKey());row.setString("Owner",r.owner);
            row.setInteger("X",r.target.x);row.setInteger("Y",r.target.y);row.setInteger("Z",r.target.z);list.appendTag(row);
        }
        tag.setTag("Entries",list);
    }
}
