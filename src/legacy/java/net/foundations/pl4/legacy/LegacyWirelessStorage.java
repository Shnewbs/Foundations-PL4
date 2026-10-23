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
    public static boolean ready(LegacyTile tile){return tile!=null&&!LegacyItemSafety.blocked(tile)&&LegacyTile.STACKS.empty(tile.pending);}
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
        private Entry(LegacyTile node,int slot,ItemStack stack){this.node=node;this.slot=slot;this.snapshot=stack.copy();}
    }
    private static List<LegacyTile> endpoints(LegacyTile root){
        List<LegacyTile> result=new ArrayList<LegacyTile>();BoundedNetwork.Plan plan=root.network();if(plan.overflow)return result;
        Set<BoundedNetwork.Point> seen=new HashSet<BoundedNetwork.Point>();
        for(BoundedNetwork.Point point:plan.nodes){LegacyTile node=root.fluidNode(point);if(node==null||!root.owner.equals(node.owner)||!ready(node)||node.fluidPowered())continue;
            int role=node.role();if(role!=1&&role!=2&&role!=LegacyReadings.ITEMS)continue;
            if(seen.add(node.fluidTarget()))result.add(node);
        }return result;
    }
    private static final class StorageInventory implements ConservingItems.Inventory<ItemStack>{
        final net.minecraftforge.items.IItemHandler nativeInventory;StorageInventory(net.minecraftforge.items.IItemHandler inventory){nativeInventory=inventory;}
        public int slots(){return nativeInventory.getSlots();}
        public ItemStack extract(int slot,int amount,boolean simulate){return nativeInventory.extractItem(slot,amount,simulate);}
        public ItemStack insert(int slot,ItemStack value,boolean simulate){return nativeInventory.insertItem(slot,value,simulate);}
        ItemStack peek(int slot){return nativeInventory.getStackInSlot(slot);}
    }
    private static StorageInventory inventory(LegacyTile node){net.minecraftforge.items.IItemHandler handler=node.inventory();return handler==null?null:new StorageInventory(handler);}
    public static List<Entry> entries(EntityPlayer player,ItemStack tool){
        List<Entry> result=new ArrayList<Entry>();LegacyTile root=resolve(player,tool);if(root==null)return result;int left=WORK_LIMIT;
        try{for(LegacyTile node:endpoints(root)){StorageInventory inventory=inventory(node);if(inventory==null)continue;
            int slots=Math.max(0,Math.min(LegacyPL4.slotLimit,inventory.slots()));for(int i=0;i<slots;i++){if(--left<0)return result;ItemStack stack=inventory.peek(i);if(!LegacyTile.STACKS.empty(stack))result.add(new Entry(node,i,stack));}
        }}catch(RuntimeException provider){result.clear();}return result;
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
        LegacyTile root=resolve(player,tool);if(root==null||!LegacyPL4.enabled||selected==null||amount<1||amount>64)return 0;
        if(!endpoints(root).contains(selected.node))return 0;final StorageInventory source=inventory(selected.node);if(source==null||selected.slot<0||selected.slot>=Math.min(source.slots(),LegacyPL4.slotLimit))return 0;
        final Entry entry=selected;ItemStack current=source.peek(entry.slot);if(!LegacyTile.STACKS.same(current,entry.snapshot))return 0;
        ConservingItems.Inventory<ItemStack> one=new ConservingItems.Inventory<ItemStack>(){public int slots(){return 1;}public ItemStack extract(int slot,int n,boolean simulate){return source.extract(entry.slot,n,simulate);}public ItemStack insert(int slot,ItemStack v,boolean simulate){return v;}};
        try{return ConservingItems.move(LegacyTile.STACKS,LegacyItemSafety.escrow(entry.node),one,playerInventory(player,-1),Math.min(amount,entry.snapshot.getMaxStackSize()),Math.max(36,LegacyPL4.slotLimit),new ConservingItems.Work(WORK_LIMIT));}
        finally{retain(entry.node);player.inventory.markDirty();}
    }
    public static int deposit(EntityPlayer player,ItemStack tool,int slot,int amount){
        LegacyTile root=resolve(player,tool);if(root==null||!LegacyPL4.enabled||slot<0||slot>=36||amount<1||amount>64)return 0;
        ItemStack offered=player.inventory.getStackInSlot(slot);if(LegacyTile.STACKS.empty(offered)||tool(offered))return 0;
        ConservingItems.Work work=new ConservingItems.Work(WORK_LIMIT);int moved=0;
        for(LegacyTile node:endpoints(root)){ConservingItems.Inventory<ItemStack> destination=inventory(node);if(destination==null)continue;
            try{moved+=ConservingItems.move(LegacyTile.STACKS,LegacyItemSafety.escrow(node),playerInventory(player,slot),destination,amount-moved,LegacyPL4.slotLimit,work);}
            finally{retain(node);player.inventory.markDirty();}
            if(!ready(node)||moved>=amount||!work.available())break;
        }return moved;
    }
    private static void retain(LegacyTile node){if(!LegacyTile.STACKS.empty(node.pending)&&!LegacyItemSafety.blocked(node))LegacyItemSafety.escrow(node).block("Wireless storage retained items; inspect escrow before reuse");}
    private LegacyWirelessStorage(){}
}
