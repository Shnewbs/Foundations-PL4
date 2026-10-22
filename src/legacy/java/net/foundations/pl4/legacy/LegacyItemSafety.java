package net.foundations.pl4.legacy;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.nbt.NBTTagCompound;
/** Persist uncertainty before every foreign mutation; uncertain items are never replayed or dropped as spendable stacks. */
public final class LegacyItemSafety {
    public static boolean blocked(LegacyTile tile){return tile.itemOpaque!=null||!tile.itemFault.isEmpty();}
    public static ConservingItems.Escrow<ItemStack> escrow(final LegacyTile tile){return new ConservingItems.Escrow<ItemStack>(){
        public ItemStack get(){return tile.pending;}
        public void set(ItemStack value){tile.pending=value==null?ItemStack.EMPTY:value;tile.fluidDirty();}
        public boolean blocked(){return LegacyItemSafety.blocked(tile);}
        public void block(String reason){tile.itemFault=reason;tile.fluidDirty();}
        public void clearBlock(){if(tile.itemOpaque==null)tile.itemFault="";tile.fluidDirty();}
    };}
    private static int count(ItemStack stack){return LegacyTile.STACKS.empty(stack)?0:stack.getCount();}
    public static void write(LegacyTile tile,NBTTagCompound tag){
        tag.setString("ItemTransferFault",tile.itemFault);
        if(tile.itemOpaque!=null)tag.setTag("Escrow",tile.itemOpaque.copy());
        tag.setInteger("ItemEscrowCount",tile.itemOpaque!=null?tile.itemOpaqueCount:count(tile.pending));
    }
    public static void read(LegacyTile tile,NBTTagCompound tag){
        tile.itemOpaque=null;tile.itemFault=tag.getString("ItemTransferFault");
        if(tile.itemFault.length()>512)tile.itemFault=tile.itemFault.substring(0,512);
        int count=tag.getInteger("ItemEscrowCount");tile.itemOpaqueCount=Math.max(0,count);
        if(tag.hasKey("Escrow")&&LegacyTile.STACKS.empty(tile.pending)&&count>0&&!tile.itemFault.isEmpty()){
            NBTTagCompound probe=(NBTTagCompound)tag.getCompoundTag("Escrow").copy();probe.setByte("Count",(byte)1);tile.pending=new ItemStack(probe);
        }
        if(tag.hasKey("Escrow")&&LegacyTile.STACKS.empty(tile.pending)){
            tile.itemOpaque=(NBTTagCompound)tag.getCompoundTag("Escrow").copy();tile.itemFault="Unresolved saved item escrow; manual inspection required";
        }else if(!tile.itemFault.isEmpty()&&!LegacyTile.STACKS.empty(tile.pending)&&count>0){tile.pending.setCount(count);}
    }
    public static void drop(LegacyTile tile){
        if(!tile.fluidServer()||!blocked(tile))return;
        ItemStack receipt=new ItemStack(LegacyPL4.ITEM_CELL);NBTTagCompound tag=new NBTTagCompound();
        tag.setString("ItemTransferFault",tile.itemFault);
        if(tile.itemOpaque!=null)tag.setTag("Escrow",tile.itemOpaque.copy());
        else if(!LegacyTile.STACKS.empty(tile.pending))tag.setTag("Escrow",tile.pending.writeToNBT(new NBTTagCompound()));
        tag.setInteger("ItemEscrowCount",tile.itemOpaque!=null?tile.itemOpaqueCount:count(tile.pending));receipt.setTagCompound(tag);
        tile.pending=ItemStack.EMPTY;tile.itemFault="";tile.itemOpaque=null;tile.itemOpaqueCount=0;tile.fluidDirty();tile.getWorld().spawnEntity(new EntityItem(tile.getWorld(),tile.getPos().getX()+.5,tile.getPos().getY()+.5,tile.getPos().getZ()+.5,receipt));
    }
    private LegacyItemSafety(){}
}
