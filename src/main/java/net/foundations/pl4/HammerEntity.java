package net.foundations.pl4;

import java.util.Optional;
import net.minecraft.util.math.BlockPos;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.IIntArray;
import net.minecraft.item.ItemStack;

import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.BlockState;
import net.foundations.pl4.compat.Capabilities;
import net.foundations.pl4.compat.BlockCapability;
import net.foundations.pl4.compat.RegisterCapabilitiesEvent;
import net.minecraftforge.items.*;
import net.foundations.pl4.core.*;

public final class HammerEntity extends TileEntity implements INamedContainerProvider,net.minecraft.tileentity.ITickableTileEntity {
    @Override public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox(){return new net.minecraft.util.math.AxisAlignedBB(getBlockPos()).expandTowards(0,2,0);}

    public int progress,cooldown;
    private int processingTicks=100,cooldownTotal=200,status;
    private String activeRecipe="";
    private boolean structureReady,working,visualDirty=true;
    private long animationTime;
    private final net.foundations.pl4.compat.PortRecipeCache<net.foundations.pl4.compat.SingleRecipeInput,ForgingRecipe> recipeCache=net.foundations.pl4.compat.PortRecipeCache.create(CoreRecipes.HAMMER.get());
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
    private final IIntArray menuData=new IIntArray(){
        @Override public int get(int i){return switch(i){case 0->progress;case 1->processingTicks;case 2->cooldown;case 3->cooldownTotal;case 4->status;default->0;};}
        @Override public void set(int i,int value){} // Authoritative server data is read-only through a menu.
        @Override public int getCount(){return 5;}
    };
    public HammerEntity(){super(FoundationsPL4.HAMMER_ENTITY.get());}
    public HammerEntity(BlockPos p,BlockState s){this();setPosition(p);}
    public static void capabilities(RegisterCapabilitiesEvent e){e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,FoundationsPL4.HAMMER_ENTITY.get(),(be,side)->be.automation);}
    public void inventoryChanged(){setChanged();visualDirty=true;}
    public boolean validIngredient(ItemStack stack){
        return level!=null&&!stack.isEmpty()&&level.getRecipeManager().getRecipes().stream().filter(r->r.getType()==CoreRecipes.HAMMER.get()).map(r->(ForgingRecipe)r).collect(java.util.stream.Collectors.toList()).stream().anyMatch(r->r.value().ingredient().test(stack));
    }
    public Optional<ForgingRecipe> recipe(){return level==null?Optional.empty():recipeCache.getRecipeFor(new net.foundations.pl4.compat.SingleRecipeInput(inventory.getStackInSlot(0)),level);}
    public boolean structureReady(){return structureReady;}
    public double animationFraction(float partial){
        double elapsed=level==null?0:Math.max(0,level.getGameTime()-animationTime)+partial;
        return HammerMotion.fraction(progress,processingTicks,cooldown,cooldownTotal,elapsed,working);
    }
    @Override public ITextComponent getDisplayName(){return new net.minecraft.util.text.TranslationTextComponent("block.foundations_pl4.hammer");}
    @Override public Container createMenu(int id,PlayerInventory inv,PlayerEntity player){return new HammerMenu(id,inv,this,menuData);}
    public static void tick(World l,BlockPos p,BlockState state,HammerEntity h){
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
                if(!HammerMotion.outputFits(existing.getCount(),output.getCount(),limit,net.foundations.pl4.compat.PortData.sameItem(existing,output))) {
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
    private void write(CompoundNBT tag,Object r){
        tag.put("inventory",inventory.serializeNBT());tag.putInt("progress",progress);tag.putInt("cooldown",cooldown);tag.putString("activeRecipe",activeRecipe);
        tag.putInt("processingTicks",processingTicks);tag.putInt("cooldownTotal",cooldownTotal);tag.putInt("status",status);
        tag.putBoolean("structureReady",structureReady);tag.putBoolean("working",working);tag.putLong("animationTime",animationTime);
    }
    protected void saveAdditional(CompoundNBT tag,Object r){super.save(tag);write(tag,r);}
    protected void loadAdditional(CompoundNBT tag,Object r){
        inventory.deserializeNBT(tag.getCompound("inventory"));
        progress=net.foundations.pl4.compat.PortMath.clamp(tag.getInt("progress"),0,72000);cooldown=net.foundations.pl4.compat.PortMath.clamp(tag.getInt("cooldown"),0,72000);activeRecipe=tag.getString("activeRecipe");
        processingTicks=tag.contains("processingTicks")?net.foundations.pl4.compat.PortMath.clamp(tag.getInt("processingTicks"),1,72000):100;
        cooldownTotal=tag.contains("cooldownTotal")?net.foundations.pl4.compat.PortMath.clamp(tag.getInt("cooldownTotal"),0,72000):Math.max(200,cooldown);
        status=net.foundations.pl4.compat.PortMath.clamp(tag.getInt("status"),0,5);structureReady=tag.getBoolean("structureReady");working=tag.getBoolean("working");animationTime=tag.getLong("animationTime");visualDirty=true;
    }
    public CompoundNBT getUpdateTag(Object r){CompoundNBT tag=new CompoundNBT();write(tag,r);return tag;}
    @Override public SUpdateTileEntityPacket getUpdatePacket(){return new SUpdateTileEntityPacket(getBlockPos(),0,getUpdateTag());}

    @Override public CompoundNBT save(CompoundNBT tag){saveAdditional(tag,null);return tag;}
    @Override public void load(CompoundNBT tag){super.load(tag);loadAdditional(tag,null);}
    @Override public CompoundNBT getUpdateTag(){CompoundNBT t=super.getUpdateTag();t.merge(getUpdateTag(null));return t;}
    @Override public void onDataPacket(net.minecraft.network.NetworkManager manager,SUpdateTileEntityPacket packet){load(packet.getTag());}
    @Override public void tick(){if(level!=null)tick(level,worldPosition,getBlockState(),this);}
}
