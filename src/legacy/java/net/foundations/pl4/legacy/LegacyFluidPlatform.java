package net.foundations.pl4.legacy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.capability.FluidTankProperties;

/** Exact 1.12.2 sided fluid capability. Never substitutes an unsided capability for a denied face. */
public final class LegacyFluidPlatform {
    public static ConservingFluids.Port<FluidStack> resolve(final LegacyTile node) {
        if (!node.fluidServer() || node.side < 0 || node.side > 5 || !node.getWorld().isBlockLoaded(node.target(),false)) return null;
        TileEntity target = node.getWorld().getTileEntity(node.target());
        if (target == null || target.isInvalid()) return null;
        if (target instanceof LegacyTile) {
            LegacyTile tank = (LegacyTile)target;
            return tank.role() == 5 && node.owner.equals(tank.owner) ? tank.fluid : null;
        }
        EnumFacing face = EnumFacing.getFront(node.side).getOpposite();
        if (!target.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,face)) return null;
        final IFluidHandler handler = target.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,face);
        if (handler == null) return null;
        return new ConservingFluids.Port<FluidStack>() {
            public Object identity(){return handler;}
            public FluidStack drain(int n,boolean simulate){return handler.drain(n,!simulate);}
            public FluidStack drain(FluidStack f,boolean simulate){return handler.drain(f,!simulate);}
            public int fill(FluidStack f,boolean simulate){return handler.fill(f,!simulate);}
        };
    }
    public static IFluidHandler nativeTank(final LegacyTile node) {
        return new IFluidHandler() {
            public IFluidTankProperties[] getTankProperties(){return new IFluidTankProperties[]{new FluidTankProperties(node.fluid.contents(),LegacyFluids.CAPACITY)};}
            public int fill(FluidStack f,boolean execute){return node.fluid.fill(f,!execute);}
            public FluidStack drain(FluidStack f,boolean execute){return node.fluid.drain(f,!execute);}
            public FluidStack drain(int n,boolean execute){return node.fluid.drain(n,!execute);}
        };
    }
    public static boolean interact(LegacyTile node,EntityPlayer player,ItemStack held) {
        if (!node.fluidServer() || node.role()!=5 || !node.canEdit(player) || held==null || held.isEmpty()) return false;
        if (held.getItem()==LegacyPL4.FLUID_CELL) {
            if (!held.hasTagCompound()) return true;
            int n=node.fluid.restore(held.getTagCompound());
            if(n>0 && !held.getTagCompound().hasKey("Contents")) held.shrink(1);
            player.inventory.markDirty();return true;
        }
        FluidStack incoming=held.getItem()==Items.WATER_BUCKET?new FluidStack(FluidRegistry.WATER,1000):held.getItem()==Items.LAVA_BUCKET?new FluidStack(FluidRegistry.LAVA,1000):null;
        if(incoming!=null){
            if(node.fluid.fill(incoming,true)==1000){node.fluid.fill(incoming,false);held.shrink(1);give(node,player,new ItemStack(Items.BUCKET));}
            return true;
        }
        if(held.getItem()==Items.BUCKET){
            FluidStack contents=node.fluid.contents();
            if(contents==null || contents.amount<1000 || contents.tag!=null || node.fluid.blocked())return true;
            ItemStack full=contents.getFluid()==FluidRegistry.WATER?new ItemStack(Items.WATER_BUCKET):contents.getFluid()==FluidRegistry.LAVA?new ItemStack(Items.LAVA_BUCKET):ItemStack.EMPTY;
            if(!full.isEmpty()){node.fluid.drain(1000,false);held.shrink(1);give(node,player,full);}return true;
        }
        return false;
    }
    private static void give(LegacyTile node,EntityPlayer player,ItemStack stack){
        if(!player.inventory.addItemStackToInventory(stack))node.getWorld().spawnEntity(new EntityItem(node.getWorld(),player.posX,player.posY,player.posZ,stack));
        player.inventory.markDirty();
    }
    public static void drop(LegacyTile node){
        if(!node.fluidServer())return;
        for(NBTTagCompound data:node.fluid.recovery()){
            ItemStack cell=new ItemStack(LegacyPL4.FLUID_CELL);cell.setTagCompound(data);
            BlockPos p=node.getPos();node.getWorld().spawnEntity(new EntityItem(node.getWorld(),p.getX()+.5,p.getY()+.5,p.getZ()+.5,cell));
        }
        node.fluid.clearRecovered();
    }
    private LegacyFluidPlatform(){}
}
