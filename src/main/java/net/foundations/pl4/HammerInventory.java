package net.foundations.pl4;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.*;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Native transactional inventory with the existing two-slot persistence layout. */
public class HammerInventory extends ItemStacksResourceHandler {
    public HammerInventory(){super(2);}
    public int getSlots(){return size();}
    public ItemStack getStackInSlot(int slot){return getResource(slot).toStack(getAmountAsInt(slot));}
    public void setStackInSlot(int slot,ItemStack stack){set(slot,ItemResource.of(stack),stack.getCount());}
    public int getSlotLimit(int slot){return 64;}
    public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
        if(stack.isEmpty())return ItemStack.EMPTY;
        try(var tx=Transaction.openRoot()){
            int inserted=insert(slot,ItemResource.of(stack),stack.getCount(),tx);
            if(!simulate)tx.commit();
            return stack.copyWithCount(stack.getCount()-inserted);
        }
    }
    public ItemStack extractItem(int slot,int amount,boolean simulate){
        var resource=getResource(slot);if(resource.isEmpty()||amount<=0)return ItemStack.EMPTY;
        try(var tx=Transaction.openRoot()){
            int extracted=extract(slot,resource,amount,tx);if(!simulate)tx.commit();
            return resource.toStack(extracted);
        }
    }
    public CompoundTag serializeNBT(HolderLookup.Provider registry){
        var tag=new CompoundTag();var items=new ListTag();
        for(int slot=0;slot<size();slot++){
            var stack=getStackInSlot(slot);if(stack.isEmpty())continue;
            var entry=(CompoundTag)NbtStacks.save(stack,registry);entry.putInt("Slot",slot);items.add(entry);
        }
        tag.put("Items",items);tag.putInt("Size",size());return tag;
    }
    public void deserializeNBT(HolderLookup.Provider registry,CompoundTag tag){
        for(int slot=0;slot<size();slot++)setStackInSlot(slot,ItemStack.EMPTY);
        var items=tag.getList("Items").orElseGet(ListTag::new);
        for(int i=0;i<items.size();i++){
            var entry=items.getCompound(i).orElseGet(CompoundTag::new);int slot=entry.getInt("Slot").orElse(-1);
            if(slot>=0&&slot<size())setStackInSlot(slot,NbtStacks.item(registry,entry));
        }
    }
}
