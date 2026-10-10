package net.foundations.pl4.compat;
import java.util.function.Consumer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
public record CustomData(NBTTagCompound tag){
 public CustomData {tag=tag.copy();}
 public NBTTagCompound copyTag(){return tag.copy();}
 public boolean contains(String key){return tag.contains(key);}
 public static void update(DataComponents.Key<CustomData> key,ItemStack stack,Consumer<NBTTagCompound> change){
  if(key!=DataComponents.CUSTOM_DATA)throw new IllegalArgumentException("Not custom data");
  NBTTagCompound tag=stack.hasTag()?stack.getTag().copy():new NBTTagCompound();change.accept(tag);stack.setTag(tag.isEmpty()?null:tag);
 }
}
