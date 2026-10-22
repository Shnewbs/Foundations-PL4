package net.foundations.pl4.legacy;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/** Native sided telemetry. No extraction, insertion or simulation calls. */
public final class LegacyReaderPlatform {
    public static String[] sample(LegacyTile node,int role){
        if(node.side<0||node.side>5||!node.getWorld().isBlockLoaded(node.target(),false))return unavailable();
        TileEntity target=node.getWorld().getTileEntity(node.target());
        if(target==null||target.isInvalid())return unavailable();
        if(target instanceof LegacyTile){
            LegacyTile own=(LegacyTile)target;
            if(!node.owner.equals(own.owner))return unavailable();
            if(role==LegacyReadings.FLUIDS&&own.role()==5)return own.fluid.blocked()?unavailable():fluid(own.fluid.storedAmount(),LegacyFluids.CAPACITY,1,false);
            if(role==LegacyReadings.ENERGY&&own.role()==8)return own.energy.blocked()?unavailable():energy(own.energy.stored(),LegacyEnergy.CAPACITY);
            return unavailable();
        }
        if(role==LegacyReadings.ITEMS){
            IItemHandler inventory=node.inventory();if(inventory==null)return unavailable();
            int total=Math.max(0,inventory.getSlots()),slots=Math.min(total,LegacyPL4.slotLimit);long count=0;
            for(int i=0;i<slots;i++){ItemStack stack=inventory.getStackInSlot(i);if(stack!=null&&!stack.isEmpty())count+=Math.max(0,stack.getCount());}
            return LegacyReadings.lines("PL4 Items","Items: "+count,"Slots: "+slots,slots<total?"Partial: slot limit":"Read-only; selected face");
        }
        EnumFacing face=EnumFacing.getFront(node.side).getOpposite();
        if(role==LegacyReadings.FLUIDS){
            if(!target.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,face))return unavailable();
            IFluidHandler handler=target.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,face);if(handler==null)return unavailable();
            IFluidTankProperties[] tanks=handler.getTankProperties();if(tanks==null)return unavailable();
            long amount=0,capacity=0;int n=Math.min(tanks.length,LegacyPL4.slotLimit);
            for(int i=0;i<n;i++)if(tanks[i]!=null){FluidStack f=tanks[i].getContents();amount+=f==null?0:Math.max(0,f.amount);capacity+=Math.max(0,tanks[i].getCapacity());}
            return fluid(amount,capacity,n,n<tanks.length);
        }
        if(role==LegacyReadings.ENERGY){
            if(!target.hasCapability(CapabilityEnergy.ENERGY,face))return unavailable();
            IEnergyStorage handler=target.getCapability(CapabilityEnergy.ENERGY,face);return handler==null?unavailable():energy(Math.max(0,handler.getEnergyStored()),Math.max(0,handler.getMaxEnergyStored()));
        }
        return unavailable();
    }
    private static String[] unavailable(){return LegacyReadings.lines("PL4 Reader","No readable target","Check face and ownership","");}
    private static String[] fluid(long amount,long capacity,int tanks,boolean limited){return LegacyReadings.lines("PL4 Fluids","mB: "+amount+" / "+capacity,"Tanks: "+tanks,limited?"Partial: tank limit":"Read-only; selected face");}
    private static String[] energy(long amount,long capacity){return LegacyReadings.lines("PL4 Energy (FE)","Stored: "+amount,"Capacity: "+capacity,"Read-only; selected face");}
    private LegacyReaderPlatform(){}
}
