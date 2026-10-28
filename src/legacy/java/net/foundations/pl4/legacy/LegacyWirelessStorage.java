package net.foundations.pl4.legacy;
import java.util.ArrayList;
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
    public static boolean tool(ItemStack stack){return !LegacyTile.STACKS.empty(stack)&&stack.getItem()==LegacyPL4.WIRELESS_STORAGE;}
    public static boolean bind(EntityPlayer player,ItemStack tool,LegacyTile root){
        if(!tool(tool)||root==null||!root.fluidServer()||!root.canEdit(player)||root.owner.isEmpty()||root.role()==5||root.role()==8)return false;
        NBTTagCompound tag=new NBTTagCompound();tag.setInteger("X",root.getPos().getX());tag.setInteger("Y",root.getPos().getY());tag.setInteger("Z",root.getPos().getZ());
        tag.setInteger("Dimension",player.dimension);tag.setString("Anchor",root.wirelessIdentity);tag.setString("Owner",root.owner);
        tool.setTagCompound(tag);player.inventory.markDirty();return true;
    }
    public static LegacyTile resolve(EntityPlayer player,ItemStack tool){
        if(player==null||player.world.isRemote||!tool(tool)||!tool.hasTagCompound())return null;
        NBTTagCompound tag=tool.getTagCompound();int x=tag.getInteger("X"),y=tag.getInteger("Y"),z=tag.getInteger("Z");
        if(y<0||y>255||tag.getInteger("Dimension")!=player.dimension||player.getDistanceSq(x+.5,y+.5,z+.5)>RANGE*RANGE||!player.world.isBlockLoaded(new net.minecraft.util.math.BlockPos(x,y,z),false))return null;
        net.minecraft.tileentity.TileEntity raw=player.world.getTileEntity(new net.minecraft.util.math.BlockPos(x,y,z));
        if(!(raw instanceof LegacyTile))return null;LegacyTile root=(LegacyTile)raw;
        if(!root.fluidServer()||!root.canEdit(player)||root.owner.isEmpty()||!root.owner.equals(tag.getString("Owner"))||!root.wirelessIdentity.equals(tag.getString("Anchor"))||root.role()==5||root.role()==8)return null;
        return root;
    }
    public static final class Entry {
        public final LegacyTile node;public final int slot;public final ItemStack snapshot;
        private final PhysicalSlot physical;
        private Entry(LegacyTile node,int slot,ItemStack stack,PhysicalSlot physical){this.node=node;this.slot=slot;this.snapshot=stack.copy();this.physical=physical;}
    }
    /** Native APIs only: never infer physical identity from visible stacks or provider equality. */
    private static Object[] backing(net.minecraftforge.items.IItemHandler handler){
        if(handler.getClass()==net.minecraftforge.items.VanillaDoubleChestItemHandler.class){
            net.minecraftforge.items.VanillaDoubleChestItemHandler h=(net.minecraftforge.items.VanillaDoubleChestItemHandler)handler;
            Object a=h.getChest(true),b=h.getChest(false);if(a==null||b==null)throw new IllegalStateException("Incomplete native double chest");return new Object[]{handler,a,b};
        }
        if(handler.getClass()==LegacySidedInventory.class)return new Object[]{handler,((LegacySidedInventory)handler).inventoryIdentity()};
        if(handler.getClass()==net.minecraftforge.items.wrapper.InvWrapper.class)return new Object[]{handler,((net.minecraftforge.items.wrapper.InvWrapper)handler).getInv()};
        return new Object[]{handler};
    }
    private static final class PhysicalSlot {
        final Object inventory;final int slot;
        PhysicalSlot(Object inventory,int slot){this.inventory=inventory;this.slot=slot;}
        @Override public int hashCode(){return 31*System.identityHashCode(inventory)+slot;}
        @Override public boolean equals(Object other){return other instanceof PhysicalSlot&&inventory==((PhysicalSlot)other).inventory&&slot==((PhysicalSlot)other).slot;}
    }
    private static PhysicalSlot physical(net.minecraftforge.items.IItemHandler handler,int slot,LegacyTile node){
        if(handler.getClass()==net.minecraftforge.items.VanillaDoubleChestItemHandler.class){
            if(slot<0||slot>=54)throw new IllegalArgumentException("Invalid native double chest slot");
            Object chest=((net.minecraftforge.items.VanillaDoubleChestItemHandler)handler).getChest(slot<27);
            if(chest==null)throw new IllegalStateException("Missing native chest half");return new PhysicalSlot(chest,slot%27);
        }
        if(handler.getClass()==LegacySidedInventory.class){LegacySidedInventory h=(LegacySidedInventory)handler;return new PhysicalSlot(h.inventoryIdentity(),h.physicalSlot(slot));}
        if(handler.getClass()==net.minecraftforge.items.wrapper.InvWrapper.class)return new PhysicalSlot(((net.minecraftforge.items.wrapper.InvWrapper)handler).getInv(),slot);
        // Opaque adapters retain their existing logical route identity; no backing-store alias claim.
        return new PhysicalSlot(node,slot);
    }
    /** A foreign-owned loaded endpoint can still face the same physical chest.
     * Owner-filtered graph discovery must not hide its unacknowledged escrow.
     * Inspect only adjacent already-loaded hosts; never scan or force-load worlds.
     * Same-owner hosts retain existing target/handler journal ownership rules,
     * including the separate healthy-source case after a destination-only fault. */
    private static boolean loadedForeignAliasUncertain(LegacyTile root,BoundedNetwork.Point target){
        for(int face=0;face<6;face++){
            BoundedNetwork.Point next=target.offset(face);
            if(next.y<0||next.y>255)continue;
            LegacyTile alias=root.fluidNode(next);
            if(alias==null||root.owner.equals(alias.owner))continue;
            int role=alias.role();
            if((role==1||role==2||role==LegacyReadings.ITEMS)
                    &&target.equals(alias.fluidTarget())&&!ready(alias))return true;
        }
        return false;
    }
    private static List<LegacyTile> endpoints(LegacyTile root){
        List<LegacyTile> result=new ArrayList<LegacyTile>();
        // Readmission of retained root evidence precedes all network/provider discovery.
        // A controller with unresolved escrow cannot enumerate healthy downstream aliases.
        if(!ready(root))return result;
        BoundedNetwork.Plan plan=root.network();if(plan.overflow)return result;
        // Observe all loaded authorized item hosts before any native inventory/provider callback.
        // A later endpoint's rejected context must already fence earlier handler discovery.
        for(BoundedNetwork.Point point:plan.nodes){LegacyTile node=root.fluidNode(point);if(node==null||!root.owner.equals(node.owner))continue;
            int role=node.role();if(role==1||role==2||role==LegacyReadings.ITEMS)LegacyItemSafety.importSaved(node);
        }
        List<LegacyTile> candidates=new ArrayList<LegacyTile>();
        java.util.Map<LegacyTile,net.minecraftforge.items.IItemHandler> handlers=new java.util.IdentityHashMap<LegacyTile,net.minecraftforge.items.IItemHandler>();
        Set<Object> blockedHandlers=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object,Boolean>());
        Set<BoundedNetwork.Point> blockedTargets=new HashSet<BoundedNetwork.Point>();
        java.util.Map<BoundedNetwork.Point,Boolean> allOwnedSided=new java.util.HashMap<BoundedNetwork.Point,Boolean>();
        // Inspect every authorized item endpoint before deduplication: a healthy alias must not bypass escrow quarantine.
        for(BoundedNetwork.Point point:plan.nodes){LegacyTile node=root.fluidNode(point);if(node==null||!root.owner.equals(node.owner))continue;
            int role=node.role();if(role!=1&&role!=2&&role!=LegacyReadings.ITEMS)continue;
            // Provider discovery may synchronously replace the target tile, host, or owner.
            // A fresh handler acquired after such a callback has NOT passed this
            // network's quarantine/alias inspection. Abort the whole roster.
            net.minecraft.world.World observedWorld=node.getWorld();
            net.minecraft.util.math.BlockPos observedHost=node.getPos(),observedTarget=node.target();
            String observedOwner=node.owner;
            if(observedWorld==null||!observedWorld.isBlockLoaded(observedHost,false)
                    ||!observedWorld.isBlockLoaded(observedTarget,false)){
                blockedTargets.add(node.fluidTarget());continue;
            }
            net.minecraft.tileentity.TileEntity observedTile=observedWorld.getTileEntity(observedTarget);
            net.minecraftforge.items.IItemHandler handler=node.inventory();
            if(node.getWorld()!=observedWorld||!observedOwner.equals(node.owner)
                    ||!observedHost.equals(node.getPos())||!observedTarget.equals(node.target())
                    ||!observedWorld.isBlockLoaded(observedHost,false)
                    ||!observedWorld.isBlockLoaded(observedTarget,false)
                    ||observedWorld.getTileEntity(observedHost)!=node
                    ||observedWorld.getTileEntity(observedTarget)!=observedTile)return result;
            handlers.put(node,handler);candidates.add(node);
            Boolean previous=allOwnedSided.get(node.fluidTarget());allOwnedSided.put(node.fluidTarget(),(previous==null||previous.booleanValue())&&handler!=null&&handler.getClass()==LegacySidedInventory.class);
            if(!ready(node)){blockedTargets.add(node.fluidTarget());if(handler!=null)for(Object id:backing(handler))blockedHandlers.add(id);}
        }
        Set<BoundedNetwork.Point> seen=new HashSet<BoundedNetwork.Point>();
        Set<Object> seenHandlers=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object,Boolean>());
        for(LegacyTile node:candidates){net.minecraftforge.items.IItemHandler handler=handlers.get(node);
            if(!ready(node)||node.fluidPowered()||blockedTargets.contains(node.fluidTarget())||loadedForeignAliasUncertain(root,node.fluidTarget())||LegacyWirelessSafety.data(root.getWorld()).blocked(root.owner,node.target()))continue;
            boolean blocked=false;if(handler!=null)for(Object id:backing(handler))if(blockedHandlers.contains(id)){blocked=true;break;}if(blocked)continue;
            if((!Boolean.TRUE.equals(allOwnedSided.get(node.fluidTarget()))&&seen.contains(node.fluidTarget()))||(handler!=null&&seenHandlers.contains(handler)))continue;
            seen.add(node.fluidTarget());if(handler!=null)seenHandlers.add(handler);result.add(node);
        }return result;
    }
    /** The handler snapshot is valid only while its installed host and target tiles remain
     * the same. Capability/slot callbacks can synchronously replace a target tile, so
     * validate both before and after EVERY provider call. Never force-load a chunk. */
    private static final class StorageInventory implements ConservingItems.Inventory<ItemStack>{
        final LegacyTile node,root;final EntityPlayer player;final ItemStack tool;final net.minecraftforge.items.IItemHandler nativeInventory;
        final net.minecraft.world.World world;final net.minecraft.util.math.BlockPos host,target;
        final net.minecraft.tileentity.TileEntity installed;final String owner;
        StorageInventory(EntityPlayer player,ItemStack tool,LegacyTile root,LegacyTile node,net.minecraftforge.items.IItemHandler inventory,
                         net.minecraft.world.World world,net.minecraft.util.math.BlockPos host,
                         net.minecraft.util.math.BlockPos target,net.minecraft.tileentity.TileEntity installed,String owner){
            this.node=node;this.root=root;this.player=player;this.tool=tool;nativeInventory=inventory;this.world=world;this.host=host;
            this.target=target;this.installed=installed;this.owner=owner;
        }
        private boolean current(){
            return resolve(player,tool)==root&&node.fluidServer()&&node.getWorld()==world&&owner.equals(node.owner)
                    &&host.equals(node.getPos())&&target.equals(node.target())
                    &&world.isBlockLoaded(host,false)&&world.isBlockLoaded(target,false)
                    &&world.getTileEntity(host)==node&&world.getTileEntity(target)==installed;
        }
        public int slots(){if(!current())return 0;int n=nativeInventory.getSlots();return current()?n:0;}
        public ItemStack extract(int slot,int amount,boolean simulate){
            if(!current())return ItemStack.EMPTY;
            ItemStack taken=nativeInventory.extractItem(slot,amount,simulate);
            if(!current()){
                if(!simulate)throw new IllegalStateException("Native storage tile changed during extraction");
                return ItemStack.EMPTY;
            }
            return taken;
        }
        public ItemStack insert(int slot,ItemStack value,boolean simulate){
            if(!current())return value;
            ItemStack remaining=nativeInventory.insertItem(slot,value,simulate);
            if(!current()){
                if(!simulate)throw new IllegalStateException("Native storage tile changed during insertion");
                return value;
            }
            return remaining;
        }
        ItemStack peek(int slot){
            if(!current())return ItemStack.EMPTY;
            ItemStack value=nativeInventory.getStackInSlot(slot);
            return current()?value:ItemStack.EMPTY;
        }
    }
    private static StorageInventory inventory(EntityPlayer player,ItemStack tool,LegacyTile root,LegacyTile node){
        if(node==null||!node.fluidServer())return null;
        net.minecraft.world.World world=node.getWorld();String owner=node.owner;
        net.minecraft.util.math.BlockPos host=node.getPos(),target=node.target();
        if(!world.isBlockLoaded(host,false)||!world.isBlockLoaded(target,false)
                ||world.getTileEntity(host)!=node)return null;
        net.minecraft.tileentity.TileEntity installed=world.getTileEntity(target);
        if(installed==null||installed instanceof LegacyTile)return null;
        net.minecraftforge.items.IItemHandler handler=node.inventory();
        // A capability callback can also unload the host or retarget its tile.
        // Reject before getTileEntity(host) could inspect a no-longer-loaded chunk.
        if(handler==null||!node.fluidServer()||node.getWorld()!=world
                ||!owner.equals(node.owner)||!host.equals(node.getPos())||!target.equals(node.target())
                ||!world.isBlockLoaded(host,false)||!world.isBlockLoaded(target,false)
                ||world.getTileEntity(host)!=node||world.getTileEntity(target)!=installed)return null;
        return new StorageInventory(player,tool,root,node,handler,world,host,target,installed,owner);
    }
    public static List<Entry> entries(EntityPlayer player,ItemStack tool){
        List<Entry> result=new ArrayList<Entry>();LegacyTile root=resolve(player,tool);if(root==null)return result;int left=WORK_LIMIT;Set<PhysicalSlot> seen=new HashSet<PhysicalSlot>();
        try{for(LegacyTile node:endpoints(root)){StorageInventory inventory=inventory(player,tool,root,node);if(inventory==null)continue;
            int slots=Math.max(0,Math.min(LegacyPL4.slotLimit,inventory.slots()));for(int i=0;i<slots;i++){if(--left<0)return result;ItemStack stack=inventory.peek(i);if(!LegacyTile.STACKS.empty(stack)){PhysicalSlot id=physical(inventory.nativeInventory,i,node);if(seen.add(id))result.add(new Entry(node,i,stack,id));}}
        }if(resolve(player,tool)!=root)result.clear();}catch(RuntimeException provider){result.clear();}return result;
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
        for(int i=0;i<a.sources.size();i++){Entry x=a.sources.get(i),y=b.sources.get(i);if(x.node!=y.node||x.slot!=y.slot||!x.physical.equals(y.physical)||!ItemStack.areItemStacksEqual(x.snapshot,y.snapshot))return false;}
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
        if(!selected.grouped){Entry entry=selected.sources.get(0);return endpoints(root).contains(entry.node)?withdrawEntry(player,entry,amount,inventory(player,tool,root,entry.node),work,false):0;}
        // Reject changed counts, variants or group membership before the first extraction.
        boolean current=false;for(Row row:rows(player,tool,0,"",true))if(LegacyTile.STACKS.same(row.snapshot,selected.snapshot)){current=sameRow(row,selected);break;}
        if(!current)return 0;
        int moved=0;
        java.util.Map<LegacyTile,StorageInventory> adapters=new java.util.HashMap<LegacyTile,StorageInventory>();
        for(Entry entry:selected.sources){
            if(moved>=amount||!work.available()||resolve(player,tool)!=root)break;
            if(!endpoints(root).contains(entry.node))break;
            StorageInventory source=adapters.get(entry.node);if(source==null){if(!work.take())break;source=inventory(player,tool,root,entry.node);if(source==null)break;adapters.put(entry.node,source);}
            int part=withdrawEntry(player,entry,amount-moved,source,work,true);moved+=part;
            if(part==0||!ready(entry.node))break;
        }return moved;
    }
    private static ConservingItems.Inventory<ItemStack> playerInventory(final EntityPlayer player,final int only){return new ConservingItems.Inventory<ItemStack>(){
        public int slots(){return only<0?36:1;}
        private int index(int slot){return only<0?slot:only;}
        public ItemStack extract(int slot,int amount,boolean simulate){int i=index(slot);ItemStack value=player.inventory.getStackInSlot(i);if(LegacyTile.STACKS.empty(value)||tool(value))return ItemStack.EMPTY;
            int n=Math.min(amount,LegacyTile.STACKS.count(value));if(simulate)return LegacyTile.STACKS.copy(value,n);
            ItemStack taken=player.inventory.decrStackSize(i,n);player.inventory.markDirty();return taken;}
        public ItemStack insert(int slot,ItemStack value,boolean simulate){int i=index(slot);ItemStack current=player.inventory.getStackInSlot(i);if(!LegacyTile.STACKS.empty(current)&&!LegacyTile.STACKS.same(current,value))return value;
            int n=Math.min(LegacyTile.STACKS.count(value),Math.max(0,Math.min(value.getMaxStackSize(),player.inventory.getInventoryStackLimit())-LegacyTile.STACKS.count(current)));
            if(n>0&&!simulate){player.inventory.setInventorySlotContents(i,LegacyTile.STACKS.copy(value,n+LegacyTile.STACKS.count(current)));player.inventory.markDirty();}
            return LegacyTile.STACKS.copy(value,LegacyTile.STACKS.count(value)-n);}
    };}
    public static int withdraw(EntityPlayer player,ItemStack tool,Entry selected,int amount){
        LegacyTile root=resolve(player,tool);if(root==null||!LegacyPL4.enabled||selected==null||amount<1||amount>64||!endpoints(root).contains(selected.node))return 0;
        return withdrawEntry(player,selected,amount,inventory(player,tool,root,selected.node),new ConservingItems.Work(WORK_LIMIT),false);
    }
    private static int withdrawEntry(final EntityPlayer player,final Entry entry,int amount,final StorageInventory source,ConservingItems.Work work,boolean exact){
        final ConservingItems.Escrow<ItemStack> escrow=LegacyWirelessSafety.escrow(entry.node);
        if(source==null||!ready(entry.node)||entry.slot<0||!work.take()||entry.slot>=Math.min(source.slots(),LegacyPL4.slotLimit)||!work.take())return 0;
        if(!entry.physical.equals(physical(source.nativeInventory,entry.slot,entry.node)))return 0;
        ItemStack current=source.peek(entry.slot);if(exact?!ItemStack.areItemStacksEqual(current,entry.snapshot):!LegacyTile.STACKS.same(current,entry.snapshot))return 0;
        ConservingItems.Inventory<ItemStack> one=new ConservingItems.Inventory<ItemStack>(){public int slots(){return 1;}public ItemStack extract(int slot,int n,boolean simulate){return source.extract(entry.slot,n,simulate);}public ItemStack insert(int slot,ItemStack v,boolean simulate){return v;}};
        try{return ConservingItems.move(LegacyTile.STACKS,escrow,one,playerInventory(player,-1),Math.min(amount,entry.snapshot.getMaxStackSize()),Math.max(36,LegacyPL4.slotLimit),work);}
        finally{retain(entry.node);player.inventory.markDirty();}
    }
    public static int deposit(EntityPlayer player,ItemStack tool,int slot,int amount){
        LegacyTile root=resolve(player,tool);if(root==null||!LegacyPL4.enabled||slot<0||slot>=36||amount<1||amount>64)return 0;
        ItemStack offered=player.inventory.getStackInSlot(slot);if(LegacyTile.STACKS.empty(offered)||tool(offered))return 0;
        ConservingItems.Work work=new ConservingItems.Work(WORK_LIMIT);int moved=0;
        for(LegacyTile node:endpoints(root)){ConservingItems.Escrow<ItemStack> escrow=LegacyWirelessSafety.escrow(node);ConservingItems.Inventory<ItemStack> destination=inventory(player,tool,root,node);if(destination==null)continue;
            try{moved+=ConservingItems.move(LegacyTile.STACKS,escrow,playerInventory(player,slot),destination,amount-moved,LegacyPL4.slotLimit,work);}
            finally{retain(node);player.inventory.markDirty();}
            if(!ready(node)||moved>=amount||!work.available())break;
        }return moved;
    }
    private static void retain(LegacyTile node){if(!LegacyTile.STACKS.empty(node.pending)&&!LegacyItemSafety.blocked(node))LegacyItemSafety.escrow(node).block("Wireless storage retained items; inspect escrow before reuse");}
    private LegacyWirelessStorage(){}
}
