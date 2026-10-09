package net.foundations.pl4.legacy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.common.ForgeDirection;

/** Native pre-capability Forge fluid interface; every external call uses the selected side. */
public final class LegacyFluidPlatform {
    public static ConservingFluids.Port<FluidStack> resolve(final LegacyTile node){
        BoundedNetwork.Point p=node.fluidTarget();
        if(!node.fluidServer()||!node.worldObj.blockExists(p.x,p.y,p.z))return null;
        TileEntity target=node.worldObj.getBlockTileEntity(p.x,p.y,p.z);
        if(target==null||target.isInvalid())return null;
        if(target instanceof LegacyTile){LegacyTile tank=(LegacyTile)target;return tank.role()==5&&node.owner.equals(tank.owner)?tank.fluid:null;}
        if(!(target instanceof IFluidHandler))return null;
        final IFluidHandler handler=(IFluidHandler)target;
        final ForgeDirection face=ForgeDirection.getOrientation(node.side^1);
        return new ConservingFluids.Port<FluidStack>(){
            public Object identity(){return handler;}
            public FluidStack drain(int n,boolean simulate){return handler.drain(face,n,!simulate);}
            public FluidStack drain(FluidStack f,boolean simulate){return handler.drain(face,f,!simulate);}
            public int fill(FluidStack f,boolean simulate){return handler.fill(face,f,!simulate);}
        };
    }
    public static boolean interact(LegacyTile node,EntityPlayer player,ItemStack held){
        if(!node.fluidServer()||node.role()!=5||!node.canEdit(player)||held==null||held.stackSize<=0)return false;
        if(held.itemID==LegacyPL4.FLUID_CELL.itemID){
            if(!held.hasTagCompound())return true;int n=node.fluid.restore(held.getTagCompound());
            if(n>0&&!held.getTagCompound().hasKey("Contents"))consume(player,held);return true;
        }
        FluidStack incoming=held.itemID==Item.bucketWater.itemID?new FluidStack(FluidRegistry.WATER,1000):held.itemID==Item.bucketLava.itemID?new FluidStack(FluidRegistry.LAVA,1000):null;
        if(incoming!=null){if(node.fluid.fill(incoming,true)==1000){node.fluid.fill(incoming,false);consume(player,held);give(node,player,new ItemStack(Item.bucketEmpty));}return true;}
        if(held.itemID==Item.bucketEmpty.itemID){
            FluidStack contents=node.fluid.contents();if(contents==null||contents.amount<1000||contents.tag!=null||node.fluid.blocked())return true;
            ItemStack full=contents.getFluid()==FluidRegistry.WATER?new ItemStack(Item.bucketWater):contents.getFluid()==FluidRegistry.LAVA?new ItemStack(Item.bucketLava):null;
            if(full!=null){node.fluid.drain(1000,false);consume(player,held);give(node,player,full);}return true;
        }
        return false;
    }
    private static void consume(EntityPlayer player,ItemStack held){held.stackSize--;if(held.stackSize<=0&&player.getCurrentEquippedItem()==held)player.inventory.setInventorySlotContents(player.inventory.currentItem,null);player.inventory.onInventoryChanged();}
    private static void give(LegacyTile node,EntityPlayer player,ItemStack stack){if(!player.inventory.addItemStackToInventory(stack))node.worldObj.spawnEntityInWorld(new EntityItem(node.worldObj,player.posX,player.posY,player.posZ,stack));player.inventory.onInventoryChanged();}
    public static void drop(LegacyTile node){
        if(!node.fluidServer())return;
        for(NBTTagCompound data:node.fluid.recovery()){ItemStack cell=new ItemStack(LegacyPL4.FLUID_CELL);cell.setTagCompound(data);node.worldObj.spawnEntityInWorld(new EntityItem(node.worldObj,node.xCoord+.5,node.yCoord+.5,node.zCoord+.5,cell));}
        node.fluid.clearRecovered();
    }
    private LegacyFluidPlatform(){}
}
