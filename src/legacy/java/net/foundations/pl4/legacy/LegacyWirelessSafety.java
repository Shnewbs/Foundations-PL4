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
/** Per-dimension target fences for wireless handoffs; independent of tile/network lifetime. */
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
            public void block(String reason){delegate.block(reason);journal.begin(token,record);}
            public void clearBlock(){delegate.clearBlock();if(!delegate.blocked()&&LegacyTile.STACKS.empty(delegate.get()))journal.finish(token);}
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
