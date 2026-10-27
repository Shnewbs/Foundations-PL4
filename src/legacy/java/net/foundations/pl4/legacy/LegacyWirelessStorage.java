package net.foundations.pl4.legacy;
import java.util.ArrayList;
import net.minecraft.inventory.IInventory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
/** Loaded-only handheld storage. Snapshots are copies; only validated server actions mutate inventories. */
public final class LegacyWirelessStorage {
    public static final int RANGE=64,ROWS=45,WORK_LIMIT=4096;
    public static boolean ready(LegacyTile tile){if(tile==null)return false;LegacyItemSafety.importSaved(tile);return !LegacyItemSafety.blocked(tile)&&LegacyTile.STACKS.empty(tile.pending);}
    public static boolean tool(ItemStack stack){return !LegacyTile.STACKS.empty(stack)&&stack.itemID==LegacyPL4.WIRELESS_STORAGE.itemID;}
    public static boolean bind(EntityPlayer player,ItemStack tool,LegacyTile root){
        if(!tool(tool)||root==null||!root.fluidServer()||!root.canEdit(player)||root.owner.isEmpty()||root.role()==5||root.role()==8)return false;
        NBTTagCompound tag=new NBTTagCompound();tag.setInteger("X",root.xCoord);tag.setInteger("Y",root.yCoord);tag.setInteger("Z",root.zCoord);
        tag.setInteger("Dimension",player.dimension);tag.setString("Anchor",root.wirelessIdentity);tag.setString("Owner",root.owner);
        tool.setTagCompound(tag);player.inventory.onInventoryChanged();return true;
    }
    public static LegacyTile resolve(EntityPlayer player,ItemStack tool){
        if(player==null||player.worldObj.isRemote||!tool(tool)||!tool.hasTagCompound())return null;
        NBTTagCompound tag=tool.getTagCompound();int x=tag.getInteger("X"),y=tag.getInteger("Y"),z=tag.getInteger("Z");
        if(y<0||y>255||tag.getInteger("Dimension")!=player.dimension||player.getDistanceSq(x+.5,y+.5,z+.5)>RANGE*RANGE||!player.worldObj.blockExists(x,y,z))return null;
        net.minecraft.tileentity.TileEntity raw=player.worldObj.getBlockTileEntity(x,y,z);
        if(!(raw instanceof LegacyTile))return null;LegacyTile root=(LegacyTile)raw;
        if(!root.fluidServer()||!root.canEdit(player)||root.owner.isEmpty()||!root.owner.equals(tag.getString("Owner"))||!root.wirelessIdentity.equals(tag.getString("Anchor"))||root.role()==5||root.role()==8)return null;
        return root;
    }
    /** Identity of a native inventory instance and its actual slot, not a visible item or coordinate. */
    private static final class PhysicalSlot {
        final IInventory backing;final int slot;
        PhysicalSlot(IInventory backing,int slot){this.backing=backing;this.slot=slot;}
        @Override public int hashCode(){return 31*System.identityHashCode(backing)+slot;}
        @Override public boolean equals(Object other){return other instanceof PhysicalSlot&&backing==((PhysicalSlot)other).backing&&slot==((PhysicalSlot)other).slot;}
    }
    private static PhysicalSlot physical(LegacyTile.NativeInventory inventory,int slot){
        return new PhysicalSlot(inventory.inventory,inventory.indices[slot]);
    }
    public static final class Entry {
        public final LegacyTile node;public final int slot;public final ItemStack snapshot;
        private final PhysicalSlot physical;
        private Entry(LegacyTile node,int slot,ItemStack stack,PhysicalSlot physical){
            this.node=node;this.slot=slot;this.snapshot=stack.copy();this.physical=physical;
        }
    }
    private static List<LegacyTile> endpoints(LegacyTile root){
        List<LegacyTile> result=new ArrayList<LegacyTile>();
        // Readmission of retained root evidence precedes all network/provider discovery.
        // A controller with unresolved escrow cannot enumerate healthy downstream aliases.
        if(!ready(root))return result;
        BoundedNetwork.Plan plan=root.network();if(plan.overflow)return result;
        List<LegacyTile> candidates=new ArrayList<LegacyTile>();Set<BoundedNetwork.Point> blocked=new HashSet<BoundedNetwork.Point>();
        // Collect uncertainty before selecting ready routes: another face must not bypass the same target's escrow fence.
        for(BoundedNetwork.Point point:plan.nodes){LegacyTile node=root.fluidNode(point);if(node==null||!root.owner.equals(node.owner))continue;
            int role=node.role();if(role!=1&&role!=2&&role!=LegacyReadings.ITEMS)continue;
            candidates.add(node);if(!ready(node))blocked.add(node.fluidTarget());
        }
        Set<BoundedNetwork.Point> seen=new HashSet<BoundedNetwork.Point>();
        for(LegacyTile node:candidates){if(!ready(node)||node.fluidPowered()||blocked.contains(node.fluidTarget())||LegacyWirelessSafety.data(root.worldObj).blocked(root.owner,node.target()))continue;
            if(seen.add(node.fluidTarget()))result.add(node);
        }return result;
    }
    private static final class StorageInventory implements ConservingItems.Inventory<ItemStack>{
        final LegacyTile.NativeInventory nativeInventory;StorageInventory(LegacyTile.NativeInventory inventory){nativeInventory=inventory;}
        public int slots(){return nativeInventory.slots();}
        public ItemStack extract(int slot,int amount,boolean simulate){return nativeInventory.extract(slot,amount,simulate);}
        public ItemStack insert(int slot,ItemStack value,boolean simulate){return nativeInventory.insert(slot,value,simulate);}
        ItemStack peek(int slot){return nativeInventory.inventory.getStackInSlot(nativeInventory.indices[slot]);}
    }
    private static StorageInventory inventory(LegacyTile node){LegacyTile.NativeInventory handler=node.inventory();return handler==null?null:new StorageInventory(handler);}
    public static List<Entry> entries(EntityPlayer player,ItemStack tool){
        List<Entry> result=new ArrayList<Entry>();LegacyTile root=resolve(player,tool);if(root==null)return result;int left=WORK_LIMIT;
        Set<PhysicalSlot> seen=new HashSet<PhysicalSlot>();
        try{for(LegacyTile node:endpoints(root)){StorageInventory inventory=inventory(node);if(inventory==null)continue;
            int slots=Math.max(0,Math.min(LegacyPL4.slotLimit,inventory.slots()));for(int i=0;i<slots;i++){
                if(--left<0)return result;
                ItemStack stack=inventory.peek(i);
                if(!LegacyTile.STACKS.empty(stack)){
                    PhysicalSlot id=physical(inventory.nativeInventory,i);
                    if(seen.add(id))result.add(new Entry(node,i,stack,id));
                }
            }
        }}catch(RuntimeException provider){result.clear();}return result;
    }
    public static final int MAX_SEARCH=32;
    /** Printable ASCII keeps native short-window controls bounded on both legacy protocols. */
    public static boolean validSearch(String query){
        if(query==null||query.length()>MAX_SEARCH)return false;
        for(int i=0;i<query.length();i++)if(query.charAt(i)<32||query.charAt(i)>126)return false;
        return true;
    }
    /** Sort copies only: each row keeps its physical endpoint/slot and full NBT variant. */
    public static List<Entry> entries(EntityPlayer player,ItemStack tool,final int order){
        return entries(player,tool,order,"");
    }
    /** Filtering never groups variants or changes physical endpoint identities. */
    public static List<Entry> entries(EntityPlayer player,ItemStack tool,final int order,String query){
        if(!validSearch(query))return new ArrayList<Entry>();
        List<Entry> result=entries(player,tool);
        try{String needle=query.toLowerCase(java.util.Locale.ROOT);
            if(!needle.isEmpty())for(java.util.Iterator<Entry> it=result.iterator();it.hasNext();)if(!it.next().snapshot.getDisplayName().toLowerCase(java.util.Locale.ROOT).contains(needle))it.remove();
            if(order==1||order==2)java.util.Collections.sort(result,new java.util.Comparator<Entry>(){
            public int compare(Entry a,Entry b){
                if(order==2){int count=Integer.compare(LegacyTile.STACKS.count(b.snapshot),LegacyTile.STACKS.count(a.snapshot));if(count!=0)return count;}
                return a.snapshot.getDisplayName().compareToIgnoreCase(b.snapshot.getDisplayName());
            }
        });}catch(RuntimeException provider){result.clear();}
        return result;
    }
    public static final long MAX_TOTAL=(long)WORK_LIMIT*Integer.MAX_VALUE;
    /** Display rows retain an immutable ordered roster of physical snapshot sources. */
    public static final class Row {
        public final ItemStack snapshot;public final long total;public final boolean grouped;
        private final List<Entry> sources;
        private Row(List<Entry> sources,boolean grouped){
            this.sources=java.util.Collections.unmodifiableList(new ArrayList<Entry>(sources));this.grouped=grouped;
            snapshot=LegacyTile.STACKS.copy(sources.get(0).snapshot,1);long sum=0;
            for(Entry source:sources)sum+=LegacyTile.STACKS.count(source.snapshot);total=sum;
        }
        public int physicalSlots(){return sources.size();}
        /** Group counts never travel in the signed-byte native ItemStack count. */
        public ItemStack preview(){return LegacyTile.STACKS.copy(snapshot,grouped?1:(int)Math.min(total,Math.max(1,Math.min(64,snapshot.getMaxStackSize()))));}
    }
    private static final class VariantKey {
        final ItemStack stack;
        VariantKey(ItemStack stack){this.stack=LegacyTile.STACKS.copy(stack,1);}
        @Override public int hashCode(){return 31*(31*System.identityHashCode(stack.getItem())+stack.getItemDamage())+(stack.hasTagCompound()?stack.getTagCompound().hashCode():0);}
        @Override public boolean equals(Object other){return other instanceof VariantKey&&LegacyTile.STACKS.same(stack,((VariantKey)other).stack);}
    }
    public static List<Row> rows(EntityPlayer player,ItemStack tool,final int order,String query,boolean grouped){
        List<Row> result=new ArrayList<Row>();
        try{
            java.util.Map<VariantKey,List<Entry>> groups=new java.util.LinkedHashMap<VariantKey,List<Entry>>();
            for(Entry entry:entries(player,tool,0,query)){
                if(!grouped){result.add(new Row(java.util.Collections.singletonList(entry),false));continue;}
                VariantKey key=new VariantKey(entry.snapshot);List<Entry> members=groups.get(key);
                if(members==null){members=new ArrayList<Entry>();groups.put(key,members);}members.add(entry);
            }
            if(grouped)for(List<Entry> members:groups.values())result.add(new Row(members,true));
            if(order==1||order==2)java.util.Collections.sort(result,new java.util.Comparator<Row>(){
                public int compare(Row a,Row b){if(order==2){int count=Long.compare(b.total,a.total);if(count!=0)return count;}return a.snapshot.getDisplayName().compareToIgnoreCase(b.snapshot.getDisplayName());}
            });
        }catch(RuntimeException provider){result.clear();}
        return result;
    }
    public static boolean sameRow(Row a,Row b){
        if(a.grouped!=b.grouped||a.total!=b.total||!LegacyTile.STACKS.same(a.snapshot,b.snapshot)||a.sources.size()!=b.sources.size())return false;
        for(int i=0;i<a.sources.size();i++){Entry x=a.sources.get(i),y=b.sources.get(i);if(x.node!=y.node||x.slot!=y.slot||!ItemStack.areItemStacksEqual(x.snapshot,y.snapshot))return false;}
        return true;
    }
    public static String compactTotal(long value){
        if(value<1000)return Long.toString(Math.max(0,value));
        long unit=1000;int index=0;String[] names={"k","M","B","T"};while(index<3&&value/unit>=1000){unit*=1000;index++;}
        long whole=value/unit,tenth=(value%unit)/(unit/10);return Long.toString(whole)+(whole<100&&tenth>0?"."+tenth:"")+names[index];
    }
    public static int withdrawRow(EntityPlayer player,ItemStack tool,Row selected,int amount){return withdrawRow(player,tool,selected,amount,new ConservingItems.Work(WORK_LIMIT));}
    /** Explicit work object permits callers to share a finite transfer budget. Snapshot scans remain separately bounded by WORK_LIMIT. */
    public static int withdrawRow(EntityPlayer player,ItemStack tool,Row selected,int amount,ConservingItems.Work work){
        LegacyTile root=resolve(player,tool);if(root==null||!LegacyPL4.enabled||selected==null||amount<1||amount>64||work==null)return 0;
        if(!selected.grouped){Entry entry=selected.sources.get(0);return endpoints(root).contains(entry.node)?withdrawEntry(player,entry,amount,inventory(entry.node),work,false):0;}
        // Reject changed counts, variants or group membership before the first extraction.
        boolean current=false;for(Row row:rows(player,tool,0,"",true))if(LegacyTile.STACKS.same(row.snapshot,selected.snapshot)){current=sameRow(row,selected);break;}
        if(!current)return 0;
        int moved=0;
        java.util.Map<LegacyTile,StorageInventory> adapters=new java.util.HashMap<LegacyTile,StorageInventory>();
        for(Entry entry:selected.sources){
            if(moved>=amount||!work.available()||resolve(player,tool)!=root)break;
            if(!endpoints(root).contains(entry.node))break;
            StorageInventory source=adapters.get(entry.node);if(source==null){if(!work.take())break;source=inventory(entry.node);if(source==null)break;adapters.put(entry.node,source);}
            int part=withdrawEntry(player,entry,amount-moved,source,work,true);moved+=part;
            if(part==0||!ready(entry.node))break;
        }return moved;
    }
    private static ConservingItems.Inventory<ItemStack> playerInventory(final EntityPlayer player,final int only){return new ConservingItems.Inventory<ItemStack>(){
        public int slots(){return only<0?36:1;}
        private int index(int slot){return only<0?slot:only;}
        public ItemStack extract(int slot,int amount,boolean simulate){int i=index(slot);ItemStack value=player.inventory.getStackInSlot(i);if(LegacyTile.STACKS.empty(value)||tool(value))return null;
            int n=Math.min(amount,LegacyTile.STACKS.count(value));if(simulate)return LegacyTile.STACKS.copy(value,n);
            ItemStack taken=player.inventory.decrStackSize(i,n);player.inventory.onInventoryChanged();return taken;}
        public ItemStack insert(int slot,ItemStack value,boolean simulate){int i=index(slot);ItemStack current=player.inventory.getStackInSlot(i);if(!LegacyTile.STACKS.empty(current)&&!LegacyTile.STACKS.same(current,value))return value;
            int n=Math.min(LegacyTile.STACKS.count(value),Math.max(0,Math.min(value.getMaxStackSize(),player.inventory.getInventoryStackLimit())-LegacyTile.STACKS.count(current)));
            if(n>0&&!simulate){player.inventory.setInventorySlotContents(i,LegacyTile.STACKS.copy(value,n+LegacyTile.STACKS.count(current)));player.inventory.onInventoryChanged();}
            return LegacyTile.STACKS.copy(value,LegacyTile.STACKS.count(value)-n);}
    };}
    public static int withdraw(EntityPlayer player,ItemStack tool,Entry selected,int amount){
        LegacyTile root=resolve(player,tool);if(root==null||!LegacyPL4.enabled||selected==null||amount<1||amount>64||!endpoints(root).contains(selected.node))return 0;
        return withdrawEntry(player,selected,amount,inventory(selected.node),new ConservingItems.Work(WORK_LIMIT),false);
    }
    private static int withdrawEntry(final EntityPlayer player,final Entry entry,int amount,final StorageInventory source,ConservingItems.Work work,boolean exact){
        final ConservingItems.Escrow<ItemStack> escrow=LegacyWirelessSafety.escrow(entry.node);
        if(source==null||!ready(entry.node)||entry.slot<0||!work.take()||entry.slot>=Math.min(source.slots(),LegacyPL4.slotLimit)||!work.take())return 0;
        // A same-item chest replacement or route flip is not the physical slot the player selected.
        if(!entry.physical.equals(physical(source.nativeInventory,entry.slot)))return 0;
        ItemStack current=source.peek(entry.slot);if(exact?!ItemStack.areItemStacksEqual(current,entry.snapshot):!LegacyTile.STACKS.same(current,entry.snapshot))return 0;
        ConservingItems.Inventory<ItemStack> one=new ConservingItems.Inventory<ItemStack>(){public int slots(){return 1;}public ItemStack extract(int slot,int n,boolean simulate){return source.extract(entry.slot,n,simulate);}public ItemStack insert(int slot,ItemStack v,boolean simulate){return v;}};
        try{return ConservingItems.move(LegacyTile.STACKS,escrow,one,playerInventory(player,-1),Math.min(amount,entry.snapshot.getMaxStackSize()),Math.max(36,LegacyPL4.slotLimit),work);}
        finally{retain(entry.node);player.inventory.onInventoryChanged();}
    }
    public static int deposit(EntityPlayer player,ItemStack tool,int slot,int amount){
        LegacyTile root=resolve(player,tool);if(root==null||!LegacyPL4.enabled||slot<0||slot>=36||amount<1||amount>64)return 0;
        ItemStack offered=player.inventory.getStackInSlot(slot);if(LegacyTile.STACKS.empty(offered)||tool(offered))return 0;
        ConservingItems.Work work=new ConservingItems.Work(WORK_LIMIT);int moved=0;
        for(LegacyTile node:endpoints(root)){ConservingItems.Escrow<ItemStack> escrow=LegacyWirelessSafety.escrow(node);ConservingItems.Inventory<ItemStack> destination=inventory(node);if(destination==null)continue;
            try{moved+=ConservingItems.move(LegacyTile.STACKS,escrow,playerInventory(player,slot),destination,amount-moved,LegacyPL4.slotLimit,work);}
            finally{retain(node);player.inventory.onInventoryChanged();}
            if(!ready(node)||moved>=amount||!work.available())break;
        }return moved;
    }
    private static void retain(LegacyTile node){if(!LegacyTile.STACKS.empty(node.pending)&&!LegacyItemSafety.blocked(node))LegacyItemSafety.escrow(node).block("Wireless storage retained items; inspect escrow before reuse");}
    private LegacyWirelessStorage(){}
}
