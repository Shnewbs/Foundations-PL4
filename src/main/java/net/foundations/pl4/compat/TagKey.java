package net.foundations.pl4.compat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;
/** Look up current tag contents on each membership test, so reloads cannot retain old values. */
public record TagKey<T>(RegistryKey<? extends Registry<T>> registry,ResourceLocation location){
 public static <T> TagKey<T> create(RegistryKey<? extends Registry<T>> registry,ResourceLocation location){return new TagKey<>(registry,location);}
 public boolean contains(T value){
  if(registry.equals(Registries.ITEM))return net.minecraft.tags.ItemTags.getAllTags().getTagOrEmpty(location).contains((net.minecraft.item.Item)value);
  if(registry.equals(Registries.FLUID))return net.minecraft.tags.FluidTags.getAllTags().getTagOrEmpty(location).contains((net.minecraft.fluid.Fluid)value);
  return false;
 }
}
