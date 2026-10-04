package net.foundations.pl4.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Local adapters preserve PL4's escrow engine while using real 26.x transactions.
 * Simulations abort their transaction; successful executions explicitly commit. */
public final class TransferAdapters {
    public interface IItemHandler {
        default Object identity(){return this;}
        int getSlots();ItemStack getStackInSlot(int index);int getSlotLimit(int index);
        ItemStack insertItem(int index,ItemStack stack,boolean simulate);ItemStack extractItem(int index,int amount,boolean simulate);
    }
    public interface IFluidHandler {
        default Object identity(){return this;}
        enum FluidAction {SIMULATE,EXECUTE}
        int getTanks();FluidStack getFluidInTank(int index);int getTankCapacity(int index);
        int fill(FluidStack stack,FluidAction action);FluidStack drain(FluidStack stack,FluidAction action);
    }
    public interface IEnergyStorage {
        int extractEnergy(int amount,boolean simulate);int receiveEnergy(int amount,boolean simulate);
    }
    public static IItemHandler items(Level level,BlockPos pos,Direction side){
        var handler=level.getCapability(Capabilities.Item.BLOCK,pos,side);return handler==null?null:items(handler);
    }
    public static IItemHandler items(ResourceHandler<ItemResource> handler){return new IItemHandler(){
        public Object identity(){return handler;}
        public int getSlots(){return handler.size();}
        public ItemStack getStackInSlot(int i){return handler.getResource(i).toStack(handler.getAmountAsInt(i));}
        public int getSlotLimit(int i){return handler.getCapacityAsInt(i,ItemResource.EMPTY);}
        public ItemStack insertItem(int i,ItemStack stack,boolean sim){if(stack.isEmpty())return ItemStack.EMPTY;try(var tx=Transaction.openRoot()){int n=handler.insert(i,ItemResource.of(stack),stack.getCount(),tx);if(!sim)tx.commit();return stack.copyWithCount(stack.getCount()-n);}}
        public ItemStack extractItem(int i,int amount,boolean sim){var resource=handler.getResource(i);if(resource.isEmpty()||amount<=0)return ItemStack.EMPTY;try(var tx=Transaction.openRoot()){int n=handler.extract(i,resource,amount,tx);if(!sim)tx.commit();return resource.toStack(n);}}
    };}
    public static final class ItemHandlerHelper {
        public static ItemStack insertItemStacked(IItemHandler handler,ItemStack stack,boolean sim){
            ItemStack rest=stack.copy();for(int pass=0;pass<2;pass++)for(int i=0;i<handler.getSlots()&&!rest.isEmpty();i++){
                ItemStack existing=handler.getStackInSlot(i);if(pass==0?(existing.isEmpty()||!ItemStack.isSameItemSameComponents(existing,rest)):!existing.isEmpty())continue;
                rest=handler.insertItem(i,rest,sim);
            }return rest;
        }
    }
    public static IFluidHandler fluids(Level level,BlockPos pos,Direction side){
        var handler=level.getCapability(Capabilities.Fluid.BLOCK,pos,side);if(handler==null)return null;
        return new IFluidHandler(){
            public Object identity(){return handler;}
            public int getTanks(){return handler.size();}
            public FluidStack getFluidInTank(int i){return handler.getResource(i).toStack(handler.getAmountAsInt(i));}
            public int getTankCapacity(int i){return handler.getCapacityAsInt(i,handler.getResource(i));}
            public int fill(FluidStack stack,FluidAction action){if(stack.isEmpty())return 0;try(var tx=Transaction.openRoot()){int n=handler.insert(FluidResource.of(stack),stack.getAmount(),tx);if(action==FluidAction.EXECUTE)tx.commit();return n;}}
            public FluidStack drain(FluidStack stack,FluidAction action){if(stack.isEmpty())return FluidStack.EMPTY;try(var tx=Transaction.openRoot()){int n=handler.extract(FluidResource.of(stack),stack.getAmount(),tx);if(action==FluidAction.EXECUTE)tx.commit();return stack.copyWithAmount(n);}}
        };
    }
    public static IEnergyStorage energy(Level level,BlockPos pos,Direction side){var handler=level.getCapability(Capabilities.Energy.BLOCK,pos,side);return handler==null?null:energy(handler);}
    public static IEnergyStorage energy(EnergyHandler handler){return new IEnergyStorage(){
        public int extractEnergy(int amount,boolean sim){try(var tx=Transaction.openRoot()){int n=handler.extract(amount,tx);if(!sim)tx.commit();return n;}}
        public int receiveEnergy(int amount,boolean sim){try(var tx=Transaction.openRoot()){int n=handler.insert(amount,tx);if(!sim)tx.commit();return n;}}
    };}
    private TransferAdapters(){}
}
