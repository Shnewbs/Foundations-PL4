package net.foundations.pl4.legacy;

import java.util.ArrayList;
import java.util.HashSet;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

/** PL4-owned fallback only. Native permissions and transfer implementation remain authoritative. */
public final class LegacySidedInventory implements IItemHandler {
    private final ISidedInventory inventory;
    private final EnumFacing face;
    private final int[] indices,positions;
    private final InvWrapper delegate;
    public LegacySidedInventory(ISidedInventory inventory,EnumFacing face){
        this.inventory=inventory;this.face=face;int[][] map=snapshot(inventory,face);this.indices=map[0];this.positions=map[1];this.delegate=new InvWrapper(inventory);
    }
    private static int[][] snapshot(ISidedInventory inventory,EnumFacing face){
        int size=Math.max(0,inventory.getSizeInventory());int[] raw=inventory.getSlotsForFace(face);
        ArrayList<Integer> valid=new ArrayList<Integer>(),positions=new ArrayList<Integer>();HashSet<Integer> seen=new HashSet<Integer>();
        int limit=raw==null?0:Math.min(raw.length,Math.max(0,Math.min(4096,LegacyPL4.slotLimit)));
        for(int i=0;i<limit;i++)if(raw[i]>=0&&raw[i]<size&&seen.add(raw[i])){valid.add(raw[i]);positions.add(i);}
        int[] result=new int[valid.size()],origins=new int[valid.size()];for(int i=0;i<result.length;i++){result[i]=valid.get(i);origins[i]=positions.get(i);}return new int[][]{result,origins};
    }
    /** Constant work per slot; no full face rescan/allocation inside a stack scan or transfer loop. */
    private boolean current(int slot){
        if(!valid(slot))return false;int[] raw=inventory.getSlotsForFace(face);int physical=indices[slot];
        int limit=raw==null?0:Math.min(raw.length,Math.max(0,Math.min(4096,LegacyPL4.slotLimit)));
        return positions[slot]<limit&&raw[positions[slot]]==physical&&physical>=0&&physical<inventory.getSizeInventory();
    }
    private boolean valid(int slot){return slot>=0&&slot<indices.length;}
    public ISidedInventory inventoryIdentity(){return inventory;}
    public int physicalSlot(int slot){if(!valid(slot))throw new IllegalArgumentException("Invalid sided logical slot");return indices[slot];}
    public int getSlots(){return indices.length;}
    public ItemStack getStackInSlot(int slot){return valid(slot)&&current(slot)?inventory.getStackInSlot(indices[slot]):ItemStack.EMPTY;}
    public ItemStack extractItem(int slot,int amount,boolean simulate){
        if(amount<=0||!valid(slot)||!current(slot))return ItemStack.EMPTY;int physical=indices[slot];
        ItemStack value=inventory.getStackInSlot(physical);
        if(value.isEmpty()||!inventory.canExtractItem(physical,value,face)||!current(slot))return ItemStack.EMPTY;
        return delegate.extractItem(physical,amount,simulate);
    }
    public ItemStack insertItem(int slot,ItemStack value,boolean simulate){
        if(value.isEmpty()||!valid(slot)||!current(slot))return value;int physical=indices[slot];
        if(!inventory.canInsertItem(physical,value,face)||!current(slot))return value;
        return delegate.insertItem(physical,value,simulate);
    }
    public int getSlotLimit(int slot){return valid(slot)&&current(slot)?delegate.getSlotLimit(indices[slot]):0;}
    public boolean isItemValid(int slot,ItemStack value){return valid(slot)&&current(slot)&&inventory.canInsertItem(indices[slot],value,face)&&delegate.isItemValid(indices[slot],value);}
}
