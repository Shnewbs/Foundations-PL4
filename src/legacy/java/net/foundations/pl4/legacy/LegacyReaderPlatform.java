package net.foundations.pl4.legacy;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.common.ForgeDirection;

/** Native 1.6.4 sided telemetry; external energy without a reading API is explicit. */
public final class LegacyReaderPlatform {
    public static String[] sample(LegacyTile node,int role){
        if(node.side<0||node.side>5)return unavailable();BoundedNetwork.Point p=node.target();
        if(!node.worldObj.blockExists(p.x,p.y,p.z))return unavailable();
        TileEntity target=node.worldObj.getBlockTileEntity(p.x,p.y,p.z);if(target==null||target.isInvalid())return unavailable();
        if(target instanceof LegacyTile){
            LegacyTile own=(LegacyTile)target;if(!node.owner.equals(own.owner))return unavailable();
            if(role==LegacyReadings.FLUIDS&&own.role()==5)return own.fluid.blocked()?unavailable():fluid(own.fluid.storedAmount(),LegacyFluids.CAPACITY,1,false);
            if(role==LegacyReadings.ENERGY&&own.role()==8)return own.energy.blocked()?unavailable():LegacyReadings.lines("PL4 Energy (internal)","Stored: "+own.energy.stored(),"Capacity: "+LegacyEnergy.CAPACITY,"Read-only; PL4 units");
            return unavailable();
        }
        if(role==LegacyReadings.ITEMS){
            LegacyTile.NativeInventory inventory=node.inventory();if(inventory==null)return unavailable();long count=0;
            for(int i=0;i<inventory.slots();i++){ItemStack stack=inventory.inventory.getStackInSlot(inventory.indices[i]);if(stack!=null)count+=Math.max(0,stack.stackSize);}
            return LegacyReadings.lines("PL4 Items","Items: "+count,"Visible slots: "+inventory.slots(),"Bounded; selected face");
        }
        if(role==LegacyReadings.FLUIDS&&target instanceof IFluidHandler){
            FluidTankInfo[] tanks=((IFluidHandler)target).getTankInfo(ForgeDirection.getOrientation(node.side^1));if(tanks==null)return unavailable();
            long amount=0,capacity=0;int n=Math.min(tanks.length,LegacyPL4.slotLimit);
            for(int i=0;i<n;i++)if(tanks[i]!=null){FluidStack f=tanks[i].fluid;amount+=f==null?0:Math.max(0,f.amount);capacity+=Math.max(0,tanks[i].capacity);}
            return fluid(amount,capacity,n,n<tanks.length);
        }
        if(role==LegacyReadings.ENERGY)return LegacyReadings.lines("PL4 Energy","No telemetry API","Use a PL4 energy buffer","No inferred RF/EU/FE");
        return unavailable();
    }
    private static String[] unavailable(){return LegacyReadings.lines("PL4 Reader","No readable target","Check face and ownership","");}
    private static String[] fluid(long amount,long capacity,int tanks,boolean limited){return LegacyReadings.lines("PL4 Fluids","mB: "+amount+" / "+capacity,"Tanks: "+tanks,limited?"Partial: tank limit":"Read-only; selected face");}
    private LegacyReaderPlatform(){}
}
