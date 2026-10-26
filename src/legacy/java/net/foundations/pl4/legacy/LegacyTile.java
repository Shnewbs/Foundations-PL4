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
/** Native 1.12.2 adapter: loaded targets and owner partitions only. */
public final class LegacyTile extends TileEntity implements ITickable {
    public final LegacyFluids.State fluid=new LegacyFluids.State(this);
    public final LegacyEnergy.State energy=new LegacyEnergy.State(this);
    public String owner="";public int side=2;public ItemStack pending=ItemStack.EMPTY;public String status="Idle";
    public String itemFault="";
    public String wirelessIdentity=java.util.UUID.randomUUID().toString();
    public NBTTagCompound itemOpaque;
    public int itemOpaqueCount;
    public boolean publishReadings=false;
    public int readerIndex=0,displayFacing=2;
    public String[] displayLines=LegacyReadings.privateLines();
    private long lastDisplay=Long.MIN_VALUE;
    private long lastRun=Long.MIN_VALUE,lastError=Long.MIN_VALUE;
    public int role(){return world!=null&&world.getBlockState(pos).getBlock() instanceof LegacyBlock?((LegacyBlock)world.getBlockState(pos).getBlock()).role:0;}
    public boolean canEdit(EntityPlayer player){return player!=null&&(owner.equals(player.getUniqueID().toString())||player.canUseCommand(2,"pl4legacy"));}
    public BlockPos target(){return pos.offset(EnumFacing.getFront(side));}
    public IItemHandler inventory(){
        if(world==null||side<0||side>5||!world.isBlockLoaded(target(),false))return null;
        TileEntity target=world.getTileEntity(target());if(target==null||target instanceof LegacyTile)return null;EnumFacing face=EnumFacing.getFront(side).getOpposite();
        if(target.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,face))return target.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,face);
        if(target instanceof ISidedInventory)return new LegacySidedInventory((ISidedInventory)target,face);
        if(target instanceof IInventory)return new InvWrapper((IInventory)target);return null;
    }
    public BoundedNetwork.Plan network(){
        return BoundedNetwork.scan(new BoundedNetwork.Graph(){public boolean connected(BoundedNetwork.Point p){
            if(world==null||p.y<0||p.y>255||owner.isEmpty())return false;BlockPos at=new BlockPos(p.x,p.y,p.z);if(!world.isBlockLoaded(at,false))return false;
            TileEntity tile=world.getTileEntity(at);return tile instanceof LegacyTile&&((LegacyTile)tile).role()!=5&&((LegacyTile)tile).role()!=8&&owner.equals(((LegacyTile)tile).owner);
        }},new BoundedNetwork.Point(pos.getX(),pos.getY(),pos.getZ()),LegacyPL4.nodeLimit);
    }
    public void refreshDisplay(){
        if(!fluidServer()||role()!=LegacyReadings.DISPLAY)return;
        String[] next=publishReadings?LegacyReadings.sample(this):LegacyReadings.privateLines();
        if(!java.util.Arrays.equals(next,displayLines)){displayLines=next;world.notifyBlockUpdate(pos,world.getBlockState(pos),world.getBlockState(pos),2);}
    }
    public void displayChanged(){
        if(!fluidServer())return;
        markDirty();refreshDisplay();world.notifyBlockUpdate(pos,world.getBlockState(pos),world.getBlockState(pos),2);
    }
    @Override public void update(){
        if(fluidServer()&&role()==LegacyReadings.DISPLAY){
            long now=world.getTotalWorldTime();long phase=now+pos.toLong();
            if(now!=lastDisplay&&((phase%20)+20)%20==0){lastDisplay=now;refreshDisplay();}return;
        }
        if(world==null||world.isRemote||(role()!=1&&role()!=3&&role()!=6)||!LegacyPL4.enabled||owner.isEmpty())return;long tick=world.getTotalWorldTime();
        if(tick==lastRun||Math.floorMod(tick+pos.toLong(),LegacyPL4.interval)!=0)return;lastRun=tick;
        if(world.isBlockPowered(pos)){status="Paused by redstone";return;}
        try{transferOnce();}catch(RuntimeException error){status="Provider error; transfer stopped";if(lastError==Long.MIN_VALUE||tick-lastError>=1200){lastError=tick;if(LegacyPL4.logger!=null)LegacyPL4.logger.warn("PL4 legacy provider failed at "+pos+"; retained escrow",error);}}
    }
    public int transferOnce(){
        if(role()==6)return LegacyEnergy.transfer(this);
        if(role()==3)return LegacyFluids.transfer(this);
        if(fluidServer()&&role()==1&&LegacyWirelessSafety.data(world).blocked(owner,target())){status="Item target fenced by unresolved wireless handoff";return 0;}
        if(LegacyItemSafety.blocked(this)){status="Item transfer quarantined; manual inspection required";return 0;}
        if(world==null||world.isRemote||(role()!=1&&role()!=3&&role()!=6)||!LegacyPL4.enabled||owner.isEmpty()||world.isBlockPowered(pos))return 0;
        BoundedNetwork.Plan plan=network();if(plan.overflow){status="Network exceeds configured host limit";return 0;}
        IItemHandler source=inventory();if(source==null&&pending.isEmpty()){status="No loaded source inventory";return 0;}
        ConservingItems.Work work=new ConservingItems.Work(1024);int moved=0;Set<BlockPos> seenTargets=new HashSet<BlockPos>();seenTargets.add(target());
        for(BoundedNetwork.Point p:plan.nodes){
            TileEntity raw=world.getTileEntity(new BlockPos(p.x,p.y,p.z));if(!(raw instanceof LegacyTile))continue;LegacyTile sink=(LegacyTile)raw;
            if(sink.role()!=2||!owner.equals(sink.owner)||LegacyWirelessSafety.data(world).blocked(owner,sink.target())||!seenTargets.add(sink.target())||world.isBlockPowered(sink.pos))continue;
            IItemHandler destination=sink.inventory();if(destination==null||destination==source)continue;
            ConservingItems.Escrow<ItemStack> escrow=LegacyItemSafety.escrow(this);
            int budget=LegacyPL4.itemRate-moved;
            if(!pending.isEmpty())moved+=ConservingItems.flush(STACKS,escrow,adapt(destination),budget,LegacyPL4.slotLimit,work);
            else if(source!=null)moved+=ConservingItems.move(STACKS,escrow,adapt(source),adapt(destination),budget,LegacyPL4.slotLimit,work);
            if(moved>=LegacyPL4.itemRate||!work.available())break;
        }
        status=moved>0?"Moved "+moved+" items":pending.isEmpty()?"Waiting for items or receiving space":"Waiting with "+pending.getCount()+" items in escrow";return moved;
    }
    public String describe(){
        if(LegacyItemSafety.blocked(this))return "PL4 Item escrow QUARANTINED: "+itemFault+" | Manual inspection required; no automatic replay.";
        if(role()>=LegacyReadings.ITEMS)return java.util.Arrays.toString(LegacyReadings.sample(this));
        if(role()>=6)return LegacyEnergy.describe(this);
        if(role()>=3)return LegacyFluids.describe(this);
        BoundedNetwork.Plan plan=network();IItemHandler inventory=inventory();long count=0;int slots=0;
        if(inventory!=null){slots=Math.max(0,Math.min(LegacyPL4.slotLimit,inventory.getSlots()));for(int i=0;i<slots;i++){ItemStack stack=inventory.getStackInSlot(i);if(!stack.isEmpty())count+=Math.max(0,stack.getCount());}}
        return "PL4 "+(role()==0?"Cable":role()==1?"Export":"Import")+" | Side "+EnumFacing.getFront(side).getName()+" | Hosts "+(plan.overflow?"LIMIT":plan.nodes.size())+" | Slots "+slots+" | Items "+count+" | Escrow "+pending.getCount()+" | "+status;
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag){super.writeToNBT(tag);tag.setInteger("PL4LegacySchema",3);tag.setString("WirelessIdentity",wirelessIdentity);LegacyReadings.writeSettings(this,tag);energy.write(tag);fluid.write(tag);tag.setString("Owner",owner);tag.setInteger("Side",side);if(!pending.isEmpty())tag.setTag("Escrow",pending.writeToNBT(new NBTTagCompound()));else tag.removeTag("Escrow");LegacyItemSafety.write(this,tag);return tag;}
    @Override public void readFromNBT(NBTTagCompound tag){super.readFromNBT(tag);if(tag.hasKey("WirelessIdentity")&&!tag.getString("WirelessIdentity").isEmpty())wirelessIdentity=tag.getString("WirelessIdentity");LegacyReadings.readSettings(this,tag);energy.read(tag);fluid.read(tag);owner=tag.getString("Owner");side=Math.max(0,Math.min(5,tag.getInteger("Side")));pending=LegacyItemSafety.loadSaved(tag);LegacyItemSafety.read(this,tag);}
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
    private net.minecraftforge.energy.IEnergyStorage nativeEnergy;
    @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> cap,EnumFacing face){return role()==8&&cap==net.minecraftforge.energy.CapabilityEnergy.ENERGY||role()==5&&cap==net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY||super.hasCapability(cap,face);}
    @Override public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,EnumFacing face){if(role()==8&&cap==net.minecraftforge.energy.CapabilityEnergy.ENERGY){if(nativeEnergy==null)nativeEnergy=LegacyEnergyPlatform.nativeBuffer(this);return net.minecraftforge.energy.CapabilityEnergy.ENERGY.cast(nativeEnergy);}if(role()==5&&cap==net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){if(nativeFluids==null)nativeFluids=LegacyFluidPlatform.nativeTank(this);return net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(nativeFluids);}return super.getCapability(cap,face);}

    @Override public NBTTagCompound getUpdateTag(){NBTTagCompound tag=LegacyReadings.clientTag(this);tag.setString("id","foundations_pl4:legacy_host");tag.setInteger("x",pos.getX());tag.setInteger("y",pos.getY());tag.setInteger("z",pos.getZ());return tag;}
    @Override public void handleUpdateTag(NBTTagCompound tag){LegacyReadings.readClientTag(this,tag);}
    @Override public net.minecraft.network.play.server.SPacketUpdateTileEntity getUpdatePacket(){return new net.minecraft.network.play.server.SPacketUpdateTileEntity(pos,0,getUpdateTag());}
    @Override public void onDataPacket(net.minecraft.network.NetworkManager net,net.minecraft.network.play.server.SPacketUpdateTileEntity packet){handleUpdateTag(packet.getNbtCompound());}
}
