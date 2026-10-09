package net.foundations.pl4.legacy;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
/** Native 1.12.2 adapter: loaded targets and owner partitions only. */
public final class LegacyTile extends TileEntity implements ITickable {
    public final LegacyFluids.State fluid=new LegacyFluids.State(this);
    public String owner="";public int side=2;public ItemStack pending=ItemStack.EMPTY;public String status="Idle";
    private long lastRun=Long.MIN_VALUE,lastError=Long.MIN_VALUE;
    public int role(){return world!=null&&world.getBlockState(pos).getBlock() instanceof LegacyBlock?((LegacyBlock)world.getBlockState(pos).getBlock()).role:0;}
    public boolean canEdit(EntityPlayer player){return player!=null&&(owner.equals(player.getUniqueID().toString())||player.canUseCommand(2,"pl4legacy"));}
    public BlockPos target(){return pos.offset(EnumFacing.getFront(side));}
    public IItemHandler inventory(){
        if(world==null||side<0||side>5||!world.isBlockLoaded(target(),false))return null;
        TileEntity target=world.getTileEntity(target());if(target==null||target instanceof LegacyTile)return null;EnumFacing face=EnumFacing.getFront(side).getOpposite();
        if(target.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,face))return target.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,face);
        if(target instanceof ISidedInventory)return new SidedInvWrapper((ISidedInventory)target,face);
        if(target instanceof IInventory)return new InvWrapper((IInventory)target);return null;
    }
    public BoundedNetwork.Plan network(){
        return BoundedNetwork.scan(new BoundedNetwork.Graph(){public boolean connected(BoundedNetwork.Point p){
            if(world==null||p.y<0||p.y>255||owner.isEmpty())return false;BlockPos at=new BlockPos(p.x,p.y,p.z);if(!world.isBlockLoaded(at,false))return false;
            TileEntity tile=world.getTileEntity(at);return tile instanceof LegacyTile&&((LegacyTile)tile).role()!=5&&owner.equals(((LegacyTile)tile).owner);
        }},new BoundedNetwork.Point(pos.getX(),pos.getY(),pos.getZ()),LegacyPL4.nodeLimit);
    }
    @Override public void update(){
        if(world==null||world.isRemote||(role()!=1&&role()!=3)||!LegacyPL4.enabled||owner.isEmpty())return;long tick=world.getTotalWorldTime();
        if(tick==lastRun||Math.floorMod(tick+pos.toLong(),LegacyPL4.interval)!=0)return;lastRun=tick;
        if(world.isBlockPowered(pos)){status="Paused by redstone";return;}
        try{transferOnce();}catch(RuntimeException error){status="Provider error; transfer stopped";if(lastError==Long.MIN_VALUE||tick-lastError>=1200){lastError=tick;if(LegacyPL4.logger!=null)LegacyPL4.logger.warn("PL4 legacy provider failed at "+pos+"; retained escrow",error);}}
    }
    public int transferOnce(){
        if(role()==3)return LegacyFluids.transfer(this);
        if(world==null||world.isRemote||(role()!=1&&role()!=3)||!LegacyPL4.enabled||owner.isEmpty()||world.isBlockPowered(pos))return 0;
        BoundedNetwork.Plan plan=network();if(plan.overflow){status="Network exceeds configured host limit";return 0;}
        IItemHandler source=inventory();if(source==null&&pending.isEmpty()){status="No loaded source inventory";return 0;}
        ConservingItems.Work work=new ConservingItems.Work(1024);int moved=0;Set<BlockPos> seenTargets=new HashSet<BlockPos>();seenTargets.add(target());
        for(BoundedNetwork.Point p:plan.nodes){
            TileEntity raw=world.getTileEntity(new BlockPos(p.x,p.y,p.z));if(!(raw instanceof LegacyTile))continue;LegacyTile sink=(LegacyTile)raw;
            if(sink.role()!=2||!owner.equals(sink.owner)||!seenTargets.add(sink.target())||world.isBlockPowered(sink.pos))continue;
            IItemHandler destination=sink.inventory();if(destination==null||destination==source)continue;
            ConservingItems.Escrow<ItemStack> escrow=new ConservingItems.Escrow<ItemStack>(){public ItemStack get(){return pending;}public void set(ItemStack value){pending=value==null?ItemStack.EMPTY:value;markDirty();}};
            int budget=LegacyPL4.itemRate-moved;
            if(!pending.isEmpty())moved+=ConservingItems.flush(STACKS,escrow,adapt(destination),budget,LegacyPL4.slotLimit,work);
            else if(source!=null)moved+=ConservingItems.move(STACKS,escrow,adapt(source),adapt(destination),budget,LegacyPL4.slotLimit,work);
            if(moved>=LegacyPL4.itemRate||!work.available())break;
        }
        status=moved>0?"Moved "+moved+" items":pending.isEmpty()?"Waiting for items or receiving space":"Waiting with "+pending.getCount()+" items in escrow";return moved;
    }
    public String describe(){
        if(role()>=3)return LegacyFluids.describe(this);
        BoundedNetwork.Plan plan=network();IItemHandler inventory=inventory();long count=0;int slots=0;
        if(inventory!=null){slots=Math.max(0,Math.min(LegacyPL4.slotLimit,inventory.getSlots()));for(int i=0;i<slots;i++){ItemStack stack=inventory.getStackInSlot(i);if(!stack.isEmpty())count+=Math.max(0,stack.getCount());}}
        return "PL4 "+(role()==0?"Cable":role()==1?"Export":"Import")+" | Side "+EnumFacing.getFront(side).getName()+" | Hosts "+(plan.overflow?"LIMIT":plan.nodes.size())+" | Slots "+slots+" | Items "+count+" | Escrow "+pending.getCount()+" | "+status;
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag){super.writeToNBT(tag);tag.setInteger("PL4LegacySchema",2);fluid.write(tag);tag.setString("Owner",owner);tag.setInteger("Side",side);if(!pending.isEmpty())tag.setTag("Escrow",pending.writeToNBT(new NBTTagCompound()));else tag.removeTag("Escrow");return tag;}
    @Override public void readFromNBT(NBTTagCompound tag){super.readFromNBT(tag);fluid.read(tag);owner=tag.getString("Owner");side=Math.max(0,Math.min(5,tag.getInteger("Side")));pending=tag.hasKey("Escrow",10)?new ItemStack(tag.getCompoundTag("Escrow")):ItemStack.EMPTY;}
    public static final ConservingItems.Stacks<ItemStack> STACKS=new ConservingItems.Stacks<ItemStack>(){
        public boolean empty(ItemStack value){return value==null||value.isEmpty();}public int count(ItemStack value){return empty(value)?0:value.getCount();}
        public boolean same(ItemStack a,ItemStack b){return !empty(a)&&!empty(b)&&a.isItemEqual(b)&&ItemStack.areItemStackTagsEqual(a,b);}
        public ItemStack copy(ItemStack value,int count){if(count==0)return ItemStack.EMPTY;ItemStack copy=value.copy();copy.setCount(count);return copy;}
    };
    public static ConservingItems.Inventory<ItemStack> adapt(final IItemHandler handler){return new ConservingItems.Inventory<ItemStack>(){
        public int slots(){return handler.getSlots();}public ItemStack extract(int slot,int amount,boolean simulate){return handler.extractItem(slot,amount,simulate);}
        public ItemStack insert(int slot,ItemStack value,boolean simulate){return handler.insertItem(slot,value,simulate);}
    };}

    public void fluidDirty(){markDirty();}
    public boolean fluidServer(){return world!=null&&!world.isRemote&&!isInvalid();}
    public boolean fluidPowered(){return world.isBlockPowered(pos);}
    public BoundedNetwork.Point fluidTarget(){BlockPos p=target();return new BoundedNetwork.Point(p.getX(),p.getY(),p.getZ());}
    public LegacyTile fluidNode(BoundedNetwork.Point p){BlockPos at=new BlockPos(p.x,p.y,p.z);if(!world.isBlockLoaded(at,false))return null;TileEntity t=world.getTileEntity(at);return t instanceof LegacyTile?(LegacyTile)t:null;}
    public ConservingFluids.Port<net.minecraftforge.fluids.FluidStack> fluids(){return LegacyFluidPlatform.resolve(this);}
    private net.minecraftforge.fluids.capability.IFluidHandler nativeFluids;
    @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> cap,EnumFacing face){return role()==5&&cap==net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY||super.hasCapability(cap,face);}
    @Override public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,EnumFacing face){if(role()==5&&cap==net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){if(nativeFluids==null)nativeFluids=LegacyFluidPlatform.nativeTank(this);return net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(nativeFluids);}return super.getCapability(cap,face);}
}
