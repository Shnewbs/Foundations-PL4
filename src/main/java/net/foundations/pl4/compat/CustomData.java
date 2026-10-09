package net.foundations.pl4.compat;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
public record CustomData(CompoundTag tag){
 public CustomData {tag=tag.copy();}
 public CompoundTag copyTag(){return tag.copy();}
 public boolean contains(String key){return tag.contains(key);}
 public static void update(DataComponents.Key<CustomData> key,ItemStack stack,Consumer<CompoundTag> change){
  if(key!=DataComponents.CUSTOM_DATA)throw new IllegalArgumentException("Not custom data");
  CompoundTag tag=stack.hasTag()?stack.getTag().copy():new CompoundTag();change.accept(tag);stack.setTag(tag.isEmpty()?null:tag);
 }
}
