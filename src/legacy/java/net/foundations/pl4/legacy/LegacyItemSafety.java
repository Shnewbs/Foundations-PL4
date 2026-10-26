package net.foundations.pl4.legacy;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.nbt.NBTTagCompound;
/** Persist uncertainty before every foreign mutation; uncertain items are never replayed or dropped as spendable stacks. */
public final class LegacyItemSafety {
    public static boolean blocked(LegacyTile tile){return tile.itemLegacyEvidence!=null||tile.itemOpaque!=null||tile.itemRawJournalContext!=null||tile.itemMissingJournalContext||!tile.itemFault.isEmpty();}
    public static ConservingItems.Escrow<ItemStack> escrow(final LegacyTile tile){return new ConservingItems.Escrow<ItemStack>(){
        public ItemStack get(){return tile.pending;}
        public void set(ItemStack value){tile.pending=value==null?null:value;tile.fluidDirty();}
        public boolean blocked(){return LegacyItemSafety.blocked(tile);}
        public void block(String reason){tile.itemFault=reason;tile.fluidDirty();}
        public void clearBlock(){if(tile.itemOpaque==null&&tile.itemRawJournalContext==null&&!tile.itemMissingJournalContext&&tile.itemLegacyEvidence==null)tile.itemFault="";tile.fluidDirty();}
    };}
    /** Refuse route edits while held items or unresolved foreign mutations fence this endpoint. */
    public static boolean selectFace(LegacyTile tile,int face){
        if(face<0||face>5||blocked(tile)||!LegacyTile.STACKS.empty(tile.pending))return false;
        tile.side=face;tile.fluidDirty();return true;
    }
    public static void importSaved(LegacyTile tile){
        if(!tile.fluidServer()||(!tile.itemImportPending&&tile.itemLegacyEvidence==null))return;
        LegacyWirelessSafety.data(tile.worldObj).importLegacy(tile);tile.itemImportPending=false;
    }
    private static void writeContext(LegacyTile tile,NBTTagCompound tag){
        if(tile.itemRawJournalContext!=null)tag.setTag("ItemJournalContext",tile.itemRawJournalContext.copy());
        else if(tile.itemJournalContext!=null)tag.setTag("ItemJournalContext",tile.itemJournalContext.copy());
        else tag.removeTag("ItemJournalContext");
    }
    private static int count(ItemStack stack){return LegacyTile.STACKS.empty(stack)?0:stack.stackSize;}
    public static void write(LegacyTile tile,NBTTagCompound tag){
        tag.setString("ItemTransferFault",tile.itemFault);
        if(tile.itemLegacyEvidence!=null)tag.setTag("ItemLegacyEvidence",tile.itemLegacyEvidence.copy());else tag.removeTag("ItemLegacyEvidence");
        writeContext(tile,tag);
        if(tile.itemOpaque!=null)tag.setTag("Escrow",tile.itemOpaque.copy());
        tag.setInteger("ItemEscrowCount",tile.itemOpaque!=null?tile.itemOpaqueCount:count(tile.pending));
    }
    private static ItemStack parse(NBTTagCompound raw){
        try{ItemStack result=ItemStack.loadItemStackFromNBT(raw);return result!=null&&result.getItem()!=null?result:null;}
        catch(RuntimeException invalid){return null;}
    }
    public static ItemStack loadSaved(NBTTagCompound tag){return tag.hasKey("Escrow")?parse(tag.getCompoundTag("Escrow")):null;}
    public static void read(LegacyTile tile,NBTTagCompound tag){
        tile.itemRawJournalContext=null;tile.itemMissingJournalContext=!tag.hasKey("ItemJournalContext")&&tag.getTag("ItemLegacyEvidence") instanceof NBTTagCompound;
        tile.itemJournalContext=tag.getTag("ItemJournalContext") instanceof NBTTagCompound?(NBTTagCompound)tag.getCompoundTag("ItemJournalContext").copy():null;
        tile.itemLegacyEvidence=tag.getTag("ItemLegacyEvidence") instanceof NBTTagCompound?(NBTTagCompound)tag.getCompoundTag("ItemLegacyEvidence").copy():null;
        boolean malformedContext=tag.hasKey("ItemJournalContext")&&!(tag.getTag("ItemJournalContext") instanceof NBTTagCompound);
        boolean malformedEvidence=tag.hasKey("ItemLegacyEvidence")&&!(tag.getTag("ItemLegacyEvidence") instanceof NBTTagCompound);
        if(malformedContext||malformedEvidence){
            boolean retainedEvidence=tile.itemLegacyEvidence!=null;
            if(!retainedEvidence){tile.itemLegacyEvidence=new NBTTagCompound();tile.itemLegacyEvidence.setString("OriginalFault",tag.getString("ItemTransferFault"));}
            if(malformedContext){if(tag.hasKey("ItemLegacyEvidence"))tile.itemRawJournalContext=tag.getTag("ItemJournalContext").copy();else tile.itemLegacyEvidence.setTag("MalformedContext",tag.getTag("ItemJournalContext").copy());tile.itemJournalContext=null;}
            if(malformedEvidence)tile.itemLegacyEvidence.setTag("MalformedEvidence",tag.getTag("ItemLegacyEvidence").copy());
        }
        tile.itemOpaque=null;tile.itemFault=malformedContext||malformedEvidence||tile.itemMissingJournalContext?"Unresolved saved item context; manual inspection required":tag.getString("ItemTransferFault");
        if(tile.itemLegacyEvidence!=null&&tile.itemFault.isEmpty())tile.itemFault="Unresolved saved item evidence; manual inspection required";
        if(tile.itemFault.length()>512)tile.itemFault=tile.itemFault.substring(0,512);
        int count=tag.getInteger("ItemEscrowCount");tile.itemOpaqueCount=Math.max(0,count);
        if(tag.hasKey("Escrow")&&LegacyTile.STACKS.empty(tile.pending)&&count>0&&!tile.itemFault.isEmpty()){
            NBTTagCompound probe=(NBTTagCompound)tag.getCompoundTag("Escrow").copy();probe.setByte("Count",(byte)1);tile.pending=parse(probe);
        }
        if(tag.hasKey("Escrow")&&LegacyTile.STACKS.empty(tile.pending)){
            tile.itemOpaque=(NBTTagCompound)tag.getCompoundTag("Escrow").copy();tile.itemFault="Unresolved saved item escrow; manual inspection required";
        }else if(!tile.itemFault.isEmpty()&&!LegacyTile.STACKS.empty(tile.pending)&&count>0){tile.pending.stackSize=count;}
        tile.itemImportPending=blocked(tile);
    }
    public static void drop(LegacyTile tile){
        if(!tile.fluidServer()||!blocked(tile))return;
        tile.itemImportPending=true;importSaved(tile);
        ItemStack receipt=new ItemStack(LegacyPL4.ITEM_CELL);NBTTagCompound tag=new NBTTagCompound();
        tag.setString("ItemTransferFault",tile.itemFault);
        if(tile.itemLegacyEvidence!=null)tag.setTag("ItemLegacyEvidence",tile.itemLegacyEvidence.copy());else tag.removeTag("ItemLegacyEvidence");
        writeContext(tile,tag);
        if(tile.itemOpaque!=null)tag.setTag("Escrow",tile.itemOpaque.copy());
        else if(!LegacyTile.STACKS.empty(tile.pending))tag.setTag("Escrow",tile.pending.writeToNBT(new NBTTagCompound()));
        tag.setInteger("ItemEscrowCount",tile.itemOpaque!=null?tile.itemOpaqueCount:count(tile.pending));receipt.setTagCompound(tag);
        tile.itemJournalContext=null;tile.itemRawJournalContext=null;tile.itemMissingJournalContext=false;tile.itemLegacyEvidence=null;tile.itemImportPending=false;tile.pending=null;tile.itemFault="";tile.itemOpaque=null;tile.itemOpaqueCount=0;tile.fluidDirty();tile.worldObj.spawnEntityInWorld(new EntityItem(tile.worldObj,tile.xCoord+.5,tile.yCoord+.5,tile.zCoord+.5,receipt));
    }
    private LegacyItemSafety(){}
}

