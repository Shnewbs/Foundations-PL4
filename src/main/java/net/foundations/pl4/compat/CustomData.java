package net.foundations.pl4.compat;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.item.ItemStack;
public record CustomData(CompoundNBT tag){
 public CustomData {tag=tag.copy();}
 public CompoundNBT copyTag(){return tag.copy();}
 public boolean contains(String key){return tag.contains(key);}
 public static void update(DataComponents.Key<CustomData> key,ItemStack stack,Consumer<CompoundNBT> change){
  if(key!=DataComponents.CUSTOM_DATA)throw new IllegalArgumentException("Not custom data");
  CompoundNBT tag=stack.hasTag()?stack.getTag().copy():new CompoundNBT();change.accept(tag);stack.setTag(tag.isEmpty()?null:tag);
 }
}
