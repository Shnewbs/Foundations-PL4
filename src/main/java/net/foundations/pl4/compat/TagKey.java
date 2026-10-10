package net.foundations.pl4.compat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Registry;

/** 1.18.1 uses tag collections, before the 1.18.2 TagKey API was introduced.
 * Resolve tag membership dynamically so data pack reloads are not cached.
 */
public record TagKey<T>(ResourceKey<? extends Registry<T>> registry,ResourceLocation location){
    public static <T> TagKey<T> create(ResourceKey<? extends Registry<T>> registry,ResourceLocation location){
        return new TagKey<>(registry,location);
    }
    public boolean contains(T value){
        if(registry.equals(Registries.ITEM))
            return net.minecraft.tags.ItemTags.getAllTags().getTagOrEmpty(location)
                .contains((net.minecraft.world.item.Item)value);
        if(registry.equals(Registries.FLUID))
            return net.minecraft.tags.FluidTags.getAllTags().getTagOrEmpty(location)
                .contains((net.minecraft.world.level.material.Fluid)value);
        return false;
    }
}
