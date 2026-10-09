package net.foundations.pl4.compat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Registry;
/** Version-native tag lookup, resolved on membership checks so reloads do not retain stale tags. */
public record TagKey<T>(ResourceKey<? extends Registry<T>> registry,ResourceLocation location){
 public static <T> TagKey<T> create(ResourceKey<? extends Registry<T>> registry,ResourceLocation location){return new TagKey<>(registry,location);}
 public boolean contains(T value){
  if(registry.equals(Registries.ITEM))return net.minecraft.tags.ItemTags.getAllTags().getTagOrEmpty(location).contains((net.minecraft.world.item.Item)value);
  if(registry.equals(Registries.FLUID))return net.minecraft.tags.FluidTags.getAllTags().getTagOrEmpty(location).contains((net.minecraft.world.level.material.Fluid)value);
  return false;
 }
}
