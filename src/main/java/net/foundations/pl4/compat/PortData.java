package net.foundations.pl4.compat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.FluidStack;
/** Native pre-component NBT persistence. Never writes to a live item during preview sampling. */
public final class PortData {
 @SuppressWarnings("unchecked") public static <T> T get(ItemStack stack,DataComponents.Key<T> key){
  if(key==DataComponents.CUSTOM_DATA)return stack.hasTag()?(T)new CustomData(stack.getTag()):null;
  if(key==DataComponents.CUSTOM_NAME)return stack.hasCustomHoverName()?(T)stack.getHoverName():null;
  if(key==DataComponents.BLOCK_ENTITY_DATA)return stack.hasTag()&&stack.getTag().contains("BlockEntityTag",net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND)?(T)stack.getTag().getCompound("BlockEntityTag").copy():null;
  if(key==DataComponents.CONTAINER){
   if(!stack.hasTag()||!stack.getTag().getCompound("BlockEntityTag").contains("Items",net.minecraftforge.common.util.Constants.NBT.TAG_LIST))return null;
   var items=new java.util.ArrayList<ItemStack>();for(NBTBase entry:stack.getTag().getCompound("BlockEntityTag").getList("Items",net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND))items.add(ItemStack.of((NBTTagCompound)entry));return (T)new ItemContainerContents(items);
  }
  throw new IllegalArgumentException("Unsupported bridge key "+key.name());
 }
 public static <T> boolean has(ItemStack stack,DataComponents.Key<T> key){return get(stack,key)!=null;}
 public static <T> void set(ItemStack stack,DataComponents.Key<T> key,T value){
  if(key==DataComponents.CUSTOM_NAME){stack.setHoverName((ITextComponent)value);return;}
  if(key==DataComponents.CUSTOM_DATA){stack.setTag(((CustomData)value).copyTag());return;}
  if(key==DataComponents.BLOCK_ENTITY_DATA){stack.getOrCreateTag().put("BlockEntityTag",((NBTTagCompound)value).copy());return;}
  if(key==DataComponents.CONTAINER){NBTTagList items=new NBTTagList();int slot=0;for(ItemStack item:((ItemContainerContents)value).items()){NBTTagCompound t=item.save(new NBTTagCompound());t.putByte("Slot",(byte)slot++);items.add(t);}stack.getOrCreateTagElement("BlockEntityTag").put("Items",items);return;}
  throw new IllegalArgumentException("Unsupported bridge key "+key.name());
 }
 public static <T> void remove(ItemStack stack,DataComponents.Key<T> key){
  if(key==DataComponents.CUSTOM_NAME){stack.resetHoverName();return;}
  if(key==DataComponents.CUSTOM_DATA){stack.setTag(null);return;}
  if(!stack.hasTag())return;
  if(key==DataComponents.BLOCK_ENTITY_DATA)stack.getTag().remove("BlockEntityTag");
  else if(key==DataComponents.CONTAINER){NBTTagCompound block=stack.getTagElement("BlockEntityTag");if(block!=null){block.remove("Items");if(block.isEmpty())stack.getTag().remove("BlockEntityTag");}}
  else throw new IllegalArgumentException("Unsupported bridge key "+key.name());
  if(stack.getTag().isEmpty())stack.setTag(null);
 }
 public static NBTTagCompound components(ItemStack s){return s.hasTag()?s.getTag().copy():new NBTTagCompound();}
 public static NBTTagCompound components(FluidStack s){return s.hasTag()?s.getTag().copy():new NBTTagCompound();}
 public static NBTTagCompound save(ItemStack s,Object registry){return s.save(new NBTTagCompound());}
 public static NBTTagCompound save(FluidStack s,Object registry){return s.writeToNBT(new NBTTagCompound());}
 public static ItemStack parseItem(Object registry,NBTTagCompound tag){return ItemStack.of(tag);}
 public static FluidStack parseFluid(Object registry,NBTTagCompound tag){return FluidStack.loadFluidStackFromNBT(tag);}
 public static FluidStack copyWithAmount(FluidStack s,int amount){FluidStack out=s.copy();out.setAmount(amount);return out;}
 public static boolean sameFluid(FluidStack a,FluidStack b){return a.isFluidEqual(b);}
 public static ItemStack copyWithCount(ItemStack stack,int count){ItemStack copy=stack.copy();copy.setCount(count);return copy;}
 public static boolean sameItem(ItemStack a,ItemStack b){return a.getItem()==b.getItem()&&ItemStack.tagMatches(a,b);}
 private PortData(){}
}
