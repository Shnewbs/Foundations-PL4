package net.foundations.pl4;

import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.world.level.storage.*;
import com.mojang.serialization.MapCodec;
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
import net.neoforged.neoforge.transfer.*;
import net.neoforged.neoforge.transfer.item.*;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.foundations.pl4.core.*;

public final class HammerEntity extends BlockEntity implements MenuProvider {
    public int progress,cooldown;
    private int processingTicks=100,cooldownTotal=200,status;
    private String activeRecipe="";
    private boolean structureReady,working,visualDirty=true;
    private long animationTime;
    private final RecipeManager.CachedCheck<SingleRecipeInput,ForgingRecipe> recipeCache=RecipeManager.createCheck(CoreRecipes.HAMMER.get());
    public final HammerInventory inventory=new HammerInventory(){
        @Override protected void onContentsChanged(int slot,ItemStack previous){setChanged();visualDirty=true;}
        @Override public boolean isValid(int slot,ItemResource resource){return slot==0&&validIngredient(resource.toStack(1));}
    };
    private final ResourceHandler<ItemResource> automation=new DelegatingResourceHandler<ItemResource>(inventory){
        @Override public int insert(int slot,ItemResource resource,int amount,TransactionContext tx){return slot==0?inventory.insert(slot,resource,amount,tx):0;}
        @Override public int extract(int slot,ItemResource resource,int amount,TransactionContext tx){return slot==1?inventory.extract(slot,resource,amount,tx):0;}
        @Override public int insert(ItemResource resource,int amount,TransactionContext tx){return insert(0,resource,amount,tx);}
        @Override public int extract(ItemResource resource,int amount,TransactionContext tx){return extract(1,resource,amount,tx);}
    };
    private final ContainerData menuData=new ContainerData(){
        @Override public int get(int i){return switch(i){case 0->progress;case 1->processingTicks;case 2->cooldown;case 3->cooldownTotal;case 4->status;default->0;};}
        @Override public void set(int i,int value){} // Authoritative server data is read-only through a menu.
        @Override public int getCount(){return 5;}
    };
    public HammerEntity(BlockPos p,BlockState s){super(FoundationsPL4.HAMMER_ENTITY.get(),p,s);}
    public static void capabilities(RegisterCapabilitiesEvent e){e.registerBlockEntity(Capabilities.Item.BLOCK,FoundationsPL4.HAMMER_ENTITY.get(),(be,side)->be.automation);}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState next){
        if(level!=null&&!level.isClientSide()){
            for(int slot=0;slot<inventory.size();slot++){
                ItemStack stack=inventory.getStackInSlot(slot);inventory.setStackInSlot(slot,ItemStack.EMPTY);
                if(!stack.isEmpty())net.minecraft.world.level.block.Block.popResource(level,pos,stack);
            }
            HammerStructure.removeOwnedSpaces(level,pos);
        }
        super.preRemoveSideEffects(pos,next);
    }
    public void inventoryChanged(){setChanged();visualDirty=true;}
    public boolean validIngredient(ItemStack stack){
        return level instanceof net.minecraft.server.level.ServerLevel server&&!stack.isEmpty()&&server.getServer().getRecipeManager().getRecipes().stream().anyMatch(r->r.value() instanceof ForgingRecipe forging&&forging.ingredient().test(stack));
    }
    public Optional<RecipeHolder<ForgingRecipe>> recipe(){return level instanceof net.minecraft.server.level.ServerLevel server?recipeCache.getRecipeFor(new SingleRecipeInput(inventory.getStackInSlot(0)),server):Optional.empty();}
    public boolean structureReady(){return structureReady;}
    public double animationFraction(float partial){
        double elapsed=level==null?0:Math.max(0,level.getGameTime()-animationTime)+partial;
        return HammerMotion.fraction(progress,processingTicks,cooldown,cooldownTotal,elapsed,working);
    }
    @Override public Component getDisplayName(){return Component.translatable("block.foundations_pl4.hammer");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new HammerMenu(id,inv,this,menuData);}
    public static void tick(Level l,BlockPos p,BlockState state,HammerEntity h){
        if(l.isClientSide())return;
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
    protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registry){write(tag,registry);}
    @Override protected void saveAdditional(ValueOutput output){super.saveAdditional(output);var tag=new CompoundTag();write(tag,persistenceLookup());output.store(tag);}
    private HolderLookup.Provider persistenceLookup(){return level!=null?level.registryAccess():HolderLookup.Provider.create(java.util.stream.Stream.of(net.minecraft.core.registries.BuiltInRegistries.ITEM,net.minecraft.core.registries.BuiltInRegistries.FLUID));}
    @Override protected void loadAdditional(ValueInput input){super.loadAdditional(input);loadAdditional(input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new),input.lookup());}
    protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){
        inventory.deserializeNBT(r,tag.getCompound("inventory").orElseGet(CompoundTag::new));
        progress=Math.clamp(tag.getInt("progress").orElse(0),0,72000);cooldown=Math.clamp(tag.getInt("cooldown").orElse(0),0,72000);activeRecipe=tag.getString("activeRecipe").orElse("");
        processingTicks=tag.contains("processingTicks")?Math.clamp(tag.getInt("processingTicks").orElse(0),1,72000):100;
        cooldownTotal=tag.contains("cooldownTotal")?Math.clamp(tag.getInt("cooldownTotal").orElse(0),0,72000):Math.max(200,cooldown);
        status=Math.clamp(tag.getInt("status").orElse(0),0,5);structureReady=tag.getBoolean("structureReady").orElse(false);working=tag.getBoolean("working").orElse(false);animationTime=tag.getLong("animationTime").orElse(0L);visualDirty=true;
    }
    public void loadWithComponents(CompoundTag tag,HolderLookup.Provider registry){loadWithComponents(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,registry,tag));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){CompoundTag tag=new CompoundTag();write(tag,r);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
