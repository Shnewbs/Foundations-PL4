package net.foundations.pl4.compat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
/** Current native tag contents are resolved on each check to honor recipe/tag reloads. */
public record TagKey<T>(Registry<T> registry,ResourceLocation location){
 public static <T> TagKey<T> create(Registry<T> registry,ResourceLocation location){return new TagKey<>(registry,location);}
 public boolean contains(T value){
  if(registry.equals(Registries.ITEM))return net.minecraft.tags.ItemTags.getAllTags().getTagOrEmpty(location).contains((net.minecraft.item.Item)value);
  if(registry.equals(Registries.FLUID))return net.minecraft.tags.FluidTags.getCollection().getTagOrEmpty(location).contains((net.minecraft.fluid.Fluid)value);
  return false;
 }
}
