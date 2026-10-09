package net.foundations.pl4.legacy;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.Block;
/** Native pre-capability adapter. Name ownership is explicit for 1.6.4, not a claimed modern account-UUID contract. */
public final class LegacyTile extends TileEntity {
    public String owner="";public int side=2;public ItemStack pending;public String status="Idle";
    private long lastRun=Long.MIN_VALUE,lastError=Long.MIN_VALUE;
    public static String identity(EntityPlayer player){return player.username.toLowerCase(Locale.ROOT);}
    public int role(){Block block=worldObj==null?null:Block.blocksList[worldObj.getBlockId(xCoord,yCoord,zCoord)];return block instanceof LegacyBlock?((LegacyBlock)block).role:0;}
    public boolean canEdit(EntityPlayer player){return player!=null&&(owner.equals(identity(player))||player.canCommandSenderUseCommand(2,"pl4legacy"));}
    public BoundedNetwork.Point point(){return new BoundedNetwork.Point(xCoord,yCoord,zCoord);}
    public BoundedNetwork.Point target(){return point().offset(side);}
    public NativeInventory inventory(){BoundedNetwork.Point p=target();if(worldObj==null||!worldObj.blockExists(p.x,p.y,p.z))return null;TileEntity tile=worldObj.getBlockTileEntity(p.x,p.y,p.z);return tile instanceof IInventory&&!(tile instanceof LegacyTile)?new NativeInventory((IInventory)tile,side^1):null;}
    public BoundedNetwork.Plan network(){return BoundedNetwork.scan(new BoundedNetwork.Graph(){public boolean connected(BoundedNetwork.Point p){if(worldObj==null||owner.isEmpty()||!worldObj.blockExists(p.x,p.y,p.z))return false;TileEntity tile=worldObj.getBlockTileEntity(p.x,p.y,p.z);return tile instanceof LegacyTile&&owner.equals(((LegacyTile)tile).owner);}},point(),LegacyPL4.nodeLimit);}
    @Override public void updateEntity(){
        if(worldObj==null||worldObj.isRemote||role()!=1||!LegacyPL4.enabled||owner.isEmpty())return;long tick=worldObj.getTotalWorldTime();long phase=tick+xCoord*31L+yCoord*17L+zCoord;
        if(tick==lastRun||((phase%LegacyPL4.interval)+LegacyPL4.interval)%LegacyPL4.interval!=0)return;lastRun=tick;
        try{transferOnce();}catch(RuntimeException error){status="Provider error; retained escrow";if(lastError==Long.MIN_VALUE||tick-lastError>=1200){lastError=tick;if(LegacyPL4.logger!=null)LegacyPL4.logger.log(java.util.logging.Level.WARNING,"PL4 legacy transfer stopped at "+point(),error);}}
    }
    public int transferOnce(){
        if(worldObj==null||worldObj.isRemote||role()!=1||!LegacyPL4.enabled||owner.isEmpty()||worldObj.isBlockIndirectlyGettingPowered(xCoord,yCoord,zCoord))return 0;
        BoundedNetwork.Plan plan=network();if(plan.overflow){status="Network host limit exceeded";return 0;}NativeInventory source=inventory();if(source==null&&pending==null)return 0;
        ConservingItems.Work work=new ConservingItems.Work(1024);Set<BoundedNetwork.Point> seen=new HashSet<BoundedNetwork.Point>();seen.add(target());int moved=0;
        for(BoundedNetwork.Point p:plan.nodes){
            TileEntity raw=worldObj.getBlockTileEntity(p.x,p.y,p.z);if(!(raw instanceof LegacyTile))continue;LegacyTile sink=(LegacyTile)raw;
            if(sink.role()!=2||!owner.equals(sink.owner)||worldObj.isBlockIndirectlyGettingPowered(p.x,p.y,p.z)||!seen.add(sink.target()))continue;
            NativeInventory dest=sink.inventory();if(dest==null||(source!=null&&source.inventory==dest.inventory))continue;
            ConservingItems.Escrow<ItemStack> e=new ConservingItems.Escrow<ItemStack>(){public ItemStack get(){return pending;}public void set(ItemStack s){pending=s;onInventoryChanged();}};
            int budget=LegacyPL4.itemRate-moved;if(pending!=null)moved+=ConservingItems.flush(STACKS,e,dest,budget,LegacyPL4.slotLimit,work);else if(source!=null)moved+=ConservingItems.move(STACKS,e,source,dest,budget,LegacyPL4.slotLimit,work);
            if(moved>=LegacyPL4.itemRate||!work.available())break;
        }
        status=moved>0?"Moved "+moved+" items":pending==null?"Waiting for items or space":"Waiting with "+pending.stackSize+" items in escrow";return moved;
    }
    public String describe(){BoundedNetwork.Plan plan=network();NativeInventory inventory=inventory();int slots=inventory==null?0:inventory.slots();long count=0;for(int i=0;i<slots;i++){ItemStack s=inventory.inventory.getStackInSlot(inventory.indices[i]);if(s!=null)count+=Math.max(0,s.stackSize);}return "PL4 "+(role()==0?"Cable":role()==1?"Export":"Import")+" | Side "+side+" | Hosts "+(plan.overflow?"LIMIT":plan.nodes.size())+" | Items "+count+" | Escrow "+(pending==null?0:pending.stackSize)+" | "+status;}
    @Override public void writeToNBT(NBTTagCompound tag){super.writeToNBT(tag);tag.setInteger("PL4LegacySchema",1);tag.setString("OwnerName",owner);tag.setInteger("Side",side);if(pending!=null)tag.setTag("Escrow",pending.writeToNBT(new NBTTagCompound()));else tag.removeTag("Escrow");}
    @Override public void readFromNBT(NBTTagCompound tag){super.readFromNBT(tag);owner=tag.getString("OwnerName").toLowerCase(Locale.ROOT);side=Math.max(0,Math.min(5,tag.getInteger("Side")));pending=tag.hasKey("Escrow")?ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Escrow")):null;}
    public static final ConservingItems.Stacks<ItemStack> STACKS=new ConservingItems.Stacks<ItemStack>(){
        public boolean empty(ItemStack s){return s==null||s.stackSize<=0;}public int count(ItemStack s){return empty(s)?0:s.stackSize;}
        public boolean same(ItemStack a,ItemStack b){return !empty(a)&&!empty(b)&&a.isItemEqual(b)&&ItemStack.areItemStackTagsEqual(a,b);}
        public ItemStack copy(ItemStack s,int n){if(n==0)return null;ItemStack copy=s.copy();copy.stackSize=n;return copy;}
    };
    public static final class NativeInventory implements ConservingItems.Inventory<ItemStack>{
        public final IInventory inventory;public final int[] indices;private final int face;
        public NativeInventory(IInventory inventory,int face){
            this.inventory=inventory;this.face=face;int size=Math.max(0,inventory.getSizeInventory());int[] raw;
            if(inventory instanceof ISidedInventory)raw=((ISidedInventory)inventory).getAccessibleSlotsFromSide(face);else{raw=new int[Math.min(size,LegacyPL4.slotLimit)];for(int i=0;i<raw.length;i++)raw[i]=i;}
            java.util.List<Integer> valid=new java.util.ArrayList<Integer>();Set<Integer> seen=new HashSet<Integer>();if(raw!=null)for(int i=0;i<Math.min(raw.length,LegacyPL4.slotLimit);i++)if(raw[i]>=0&&raw[i]<size&&seen.add(raw[i]))valid.add(raw[i]);
            indices=new int[valid.size()];for(int i=0;i<indices.length;i++)indices[i]=valid.get(i);
        }
        public int slots(){return indices.length;}
        public ItemStack extract(int slot,int amount,boolean simulate){int index=indices[slot];ItemStack s=inventory.getStackInSlot(index);if(STACKS.empty(s)||amount<=0||(inventory instanceof ISidedInventory&&!((ISidedInventory)inventory).canExtractItem(index,s,face)))return null;int n=Math.min(amount,s.stackSize);if(simulate)return STACKS.copy(s,n);ItemStack result=inventory.decrStackSize(index,n);inventory.onInventoryChanged();return result;}
        public ItemStack insert(int slot,ItemStack offered,boolean simulate){
            int index=indices[slot];if(STACKS.empty(offered))return null;
            if(!inventory.isItemValidForSlot(index,offered)||(inventory instanceof ISidedInventory&&!((ISidedInventory)inventory).canInsertItem(index,offered,face)))return offered;
            ItemStack current=inventory.getStackInSlot(index);if(!STACKS.empty(current)&&!STACKS.same(current,offered))return offered;
            int capacity=Math.max(0,Math.min(inventory.getInventoryStackLimit(),offered.getMaxStackSize())-STACKS.count(current));int n=Math.min(offered.stackSize,capacity);
            if(n>0&&!simulate){inventory.setInventorySlotContents(index,STACKS.copy(offered,STACKS.count(current)+n));inventory.onInventoryChanged();}
            return n==offered.stackSize?null:STACKS.copy(offered,offered.stackSize-n);
        }
    }
}
