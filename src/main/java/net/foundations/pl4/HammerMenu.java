package net.foundations.pl4;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/** Original two-slot / 176x143 layout, with server-authoritative vanilla container transactions. */
public final class HammerMenu extends AbstractContainerMenu {
    private static final int PLAYER_START=2,MAIN_END=29,END=38;
    private final HammerEntity hammer;
    private final ContainerData data;
    // Client has a fresh dummy inventory. No local world inventory is ever modified by slot prediction.
    public HammerMenu(int id,Inventory playerInventory){this(id,playerInventory,null,new SimpleContainerData(5));}
    public HammerMenu(int id,Inventory playerInventory,HammerEntity hammer,ContainerData data){
        super(FoundationsPL4.HAMMER_MENU.get(),id);this.hammer=hammer;this.data=data;
        checkContainerDataCount(data,5);
        HammerInventory inventory=hammer==null?new HammerInventory():hammer.inventory;
        addSlot(new ResourceHandlerSlot(inventory,inventory::set,0,53,24));
        addSlot(new ResourceHandlerSlot(inventory,inventory::set,1,107,24){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(playerInventory,col+row*9+9,8+col*18,62+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(playerInventory,col,8+col*18,120));
        addDataSlots(data);
    }
    public int progress(){return data.get(0);} public int duration(){return Math.max(1,data.get(1));}
    public int cooldown(){return data.get(2);} public int status(){return data.get(4);}
    @Override public boolean stillValid(Player player){
        return hammer==null||(!hammer.isRemoved()&&hammer.getLevel()==player.level()
            &&player.level().getBlockEntity(hammer.getBlockPos())==hammer
            &&player.level().getBlockState(hammer.getBlockPos()).is(FoundationsPL4.HAMMER.get())
            &&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(hammer.getBlockPos()))<=64);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size()||!stillValid(player))return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        ItemStack source=slot.getItem(),original=source.copy();
        if(index<PLAYER_START) { if(!moveItemStackTo(source,PLAYER_START,END,true))return ItemStack.EMPTY; }
        else if(slots.get(0).mayPlace(source)) { if(!moveItemStackTo(source,0,1,false))return ItemStack.EMPTY; }
        else if(index<MAIN_END) { if(!moveItemStackTo(source,MAIN_END,END,false))return ItemStack.EMPTY; }
        else if(!moveItemStackTo(source,PLAYER_START,MAIN_END,false))return ItemStack.EMPTY;
        slot.set(source.isEmpty()?ItemStack.EMPTY:source);
        if(source.getCount()==original.getCount())return ItemStack.EMPTY;
        slot.onTake(player,source);return original;
    }
}
