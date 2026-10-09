package net.foundations.pl4.compat;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
/** Native pre-component NBT persistence. Never writes to a live item during preview sampling. */
public final class PortData {
 @SuppressWarnings("unchecked") public static <T> T get(ItemStack stack,DataComponents.Key<T> key){
  if(key==DataComponents.CUSTOM_DATA)return stack.hasTag()?(T)new CustomData(stack.getTag()):null;
  if(key==DataComponents.CUSTOM_NAME)return stack.hasCustomHoverName()?(T)stack.getHoverName():null;
  if(key==DataComponents.BLOCK_ENTITY_DATA)return stack.hasTag()&&stack.getTag().contains("BlockEntityTag",Tag.TAG_COMPOUND)?(T)stack.getTag().getCompound("BlockEntityTag").copy():null;
  if(key==DataComponents.CONTAINER){
   if(!stack.hasTag()||!stack.getTag().getCompound("BlockEntityTag").contains("Items",Tag.TAG_LIST))return null;
   var items=new java.util.ArrayList<ItemStack>();for(Tag entry:stack.getTag().getCompound("BlockEntityTag").getList("Items",Tag.TAG_COMPOUND))items.add(ItemStack.of((CompoundTag)entry));return (T)new ItemContainerContents(items);
  }
  throw new IllegalArgumentException("Unsupported bridge key "+key.name());
 }
 public static <T> boolean has(ItemStack stack,DataComponents.Key<T> key){return get(stack,key)!=null;}
 public static <T> void set(ItemStack stack,DataComponents.Key<T> key,T value){
  if(key==DataComponents.CUSTOM_NAME){stack.setHoverName((Component)value);return;}
  if(key==DataComponents.CUSTOM_DATA){stack.setTag(((CustomData)value).copyTag());return;}
  if(key==DataComponents.BLOCK_ENTITY_DATA){stack.getOrCreateTag().put("BlockEntityTag",((CompoundTag)value).copy());return;}
  if(key==DataComponents.CONTAINER){ListTag items=new ListTag();int slot=0;for(ItemStack item:((ItemContainerContents)value).items()){CompoundTag t=item.save(new CompoundTag());t.putByte("Slot",(byte)slot++);items.add(t);}stack.getOrCreateTagElement("BlockEntityTag").put("Items",items);return;}
  throw new IllegalArgumentException("Unsupported bridge key "+key.name());
 }
 public static <T> void remove(ItemStack stack,DataComponents.Key<T> key){
  if(key==DataComponents.CUSTOM_NAME){stack.resetHoverName();return;}
  if(key==DataComponents.CUSTOM_DATA){stack.setTag(null);return;}
  if(!stack.hasTag())return;
  if(key==DataComponents.BLOCK_ENTITY_DATA)stack.getTag().remove("BlockEntityTag");
  else if(key==DataComponents.CONTAINER){CompoundTag block=stack.getTagElement("BlockEntityTag");if(block!=null){block.remove("Items");if(block.isEmpty())stack.getTag().remove("BlockEntityTag");}}
  else throw new IllegalArgumentException("Unsupported bridge key "+key.name());
  if(stack.getTag().isEmpty())stack.setTag(null);
 }
 public static CompoundTag components(ItemStack s){return s.hasTag()?s.getTag().copy():new CompoundTag();}
 public static CompoundTag components(FluidStack s){return s.hasTag()?s.getTag().copy():new CompoundTag();}
 public static CompoundTag save(ItemStack s,Object registry){return s.save(new CompoundTag());}
 public static CompoundTag save(FluidStack s,Object registry){return s.writeToNBT(new CompoundTag());}
 public static ItemStack parseItem(Object registry,CompoundTag tag){return ItemStack.of(tag);}
 public static FluidStack parseFluid(Object registry,CompoundTag tag){return FluidStack.loadFluidStackFromNBT(tag);}
 public static FluidStack copyWithAmount(FluidStack s,int amount){FluidStack out=s.copy();out.setAmount(amount);return out;}
 public static boolean sameFluid(FluidStack a,FluidStack b){return a.isFluidEqual(b);}
 public static ItemStack copyWithCount(ItemStack stack,int count){ItemStack copy=stack.copy();copy.setCount(count);return copy;}
 private PortData(){}
}
