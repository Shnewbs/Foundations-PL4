package net.foundations.pl4;

import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.*;
import net.foundations.pl4.core.*;

public final class HammerEntity extends BlockEntity implements MenuProvider {
    public int progress,cooldown;
    private int processingTicks=100,cooldownTotal=200,status;
    private String activeRecipe="";
    private boolean structureReady,working,visualDirty=true;
    private long animationTime;
    private final RecipeManager.CachedCheck<SingleRecipeInput,ForgingRecipe> recipeCache=RecipeManager.createCheck(CoreRecipes.HAMMER.get());
    public final ItemStackHandler inventory=new ItemStackHandler(2){
        @Override protected void onContentsChanged(int slot){setChanged();visualDirty=true;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot==0&&validIngredient(stack);}
    };
    private final IItemHandler automation=new IItemHandler(){
        public int getSlots(){return 2;}
        public ItemStack getStackInSlot(int slot){return inventory.getStackInSlot(slot);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return slot==0?inventory.insertItem(slot,stack,simulate):stack;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return slot==1?inventory.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return inventory.getSlotLimit(slot);}
        public boolean isItemValid(int slot,ItemStack stack){return inventory.isItemValid(slot,stack);}
    };
    private final ContainerData menuData=new ContainerData(){
        @Override public int get(int i){return switch(i){case 0->progress;case 1->processingTicks;case 2->cooldown;case 3->cooldownTotal;case 4->status;default->0;};}
        @Override public void set(int i,int value){} // Authoritative server data is read-only through a menu.
        @Override public int getCount(){return 5;}
    };
    public HammerEntity(BlockPos p,BlockState s){super(FoundationsPL4.HAMMER_ENTITY.get(),p,s);}
    public static void capabilities(RegisterCapabilitiesEvent e){e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,FoundationsPL4.HAMMER_ENTITY.get(),(be,side)->be.automation);}
    public void inventoryChanged(){setChanged();visualDirty=true;}
    public boolean validIngredient(ItemStack stack){
        return level!=null&&!stack.isEmpty()&&level.getRecipeManager().getAllRecipesFor(CoreRecipes.HAMMER.get()).stream().anyMatch(r->r.value().ingredient().test(stack));
    }
    public Optional<RecipeHolder<ForgingRecipe>> recipe(){return level==null?Optional.empty():recipeCache.getRecipeFor(new SingleRecipeInput(inventory.getStackInSlot(0)),level);}
    public boolean structureReady(){return structureReady;}
    public double animationFraction(float partial){
        double elapsed=level==null?0:Math.max(0,level.getGameTime()-animationTime)+partial;
        return HammerMotion.fraction(progress,processingTicks,cooldown,cooldownTotal,elapsed,working);
    }
    @Override public Component getDisplayName(){return Component.translatable("block.foundations_pl4.hammer");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new HammerMenu(id,inv,this,menuData);}
    public static void tick(Level l,BlockPos p,BlockState state,HammerEntity h){
        if(l.isClientSide)return;
        int oldStatus=h.status;boolean oldWorking=h.working,oldStructure=h.structureReady;
        h.structureReady=HammerStructure.complete(l,p);
        if(!h.structureReady&&(h.visualDirty||l.getGameTime()%20==0))h.structureReady=HammerStructure.ensure(l,p);
        h.working=false;
        if(!h.structureReady)h.status=4; // Keep legacy progress/inventory intact while headroom is blocked.
        else if(h.cooldown>0){h.cooldown--;h.status=2;h.setChanged();}
        else {
            var holder=h.recipe();
            if(holder.isEmpty()) {
                if(h.progress!=0||!h.activeRecipe.isEmpty()){h.progress=0;h.activeRecipe="";h.setChanged();}
                h.status=h.inventory.getStackInSlot(0).isEmpty()?0:5;
            } else {
                var recipe=holder.get().value();String id=holder.get().id().toString();
                if(!id.equals(h.activeRecipe)){h.progress=0;h.activeRecipe=id;h.setChanged();}
                h.processingTicks=recipe.processingTicks();h.cooldownTotal=recipe.cooldownTicks();
                ItemStack output=recipe.result(),existing=h.inventory.getStackInSlot(1);
                int limit=Math.min(h.inventory.getSlotLimit(1),existing.isEmpty()?output.getMaxStackSize():existing.getMaxStackSize());
                if(!HammerMotion.outputFits(existing.getCount(),output.getCount(),limit,ItemStack.isSameItemSameComponents(existing,output))) {
                    if(h.progress!=0){h.progress=0;h.setChanged();}h.status=3;
                } else {
                    h.working=true;h.status=1;
                    if(++h.progress>h.processingTicks) {
                        h.inventory.extractItem(0,recipe.inputCount(),false);
                        if(existing.isEmpty())h.inventory.setStackInSlot(1,output);
                        else {ItemStack combined=existing.copy();combined.grow(output.getCount());h.inventory.setStackInSlot(1,combined);}
                        h.progress=0;h.cooldown=h.cooldownTotal;h.working=false;h.status=h.cooldown>0?2:0;h.visualDirty=true;
                    }
                    h.setChanged();
                }
            }
        }
        if(h.visualDirty||oldStatus!=h.status||oldWorking!=h.working||oldStructure!=h.structureReady
            ||((h.working||h.cooldown>0)&&l.getGameTime()%10==0)) {
            h.animationTime=l.getGameTime();h.visualDirty=false;
            l.sendBlockUpdated(p,state,state,2);
        }
    }
    private void write(CompoundTag tag,HolderLookup.Provider r){
        tag.put("inventory",inventory.serializeNBT(r));tag.putInt("progress",progress);tag.putInt("cooldown",cooldown);tag.putString("activeRecipe",activeRecipe);
        tag.putInt("processingTicks",processingTicks);tag.putInt("cooldownTotal",cooldownTotal);tag.putInt("status",status);
        tag.putBoolean("structureReady",structureReady);tag.putBoolean("working",working);tag.putLong("animationTime",animationTime);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);write(tag,r);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){
        super.loadAdditional(tag,r);inventory.deserializeNBT(r,tag.getCompound("inventory"));
        progress=Math.clamp(tag.getInt("progress"),0,72000);cooldown=Math.clamp(tag.getInt("cooldown"),0,72000);activeRecipe=tag.getString("activeRecipe");
        processingTicks=tag.contains("processingTicks")?Math.clamp(tag.getInt("processingTicks"),1,72000):100;
        cooldownTotal=tag.contains("cooldownTotal")?Math.clamp(tag.getInt("cooldownTotal"),0,72000):Math.max(200,cooldown);
        status=Math.clamp(tag.getInt("status"),0,5);structureReady=tag.getBoolean("structureReady");working=tag.getBoolean("working");animationTime=tag.getLong("animationTime");visualDirty=true;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){CompoundTag tag=new CompoundTag();write(tag,r);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
