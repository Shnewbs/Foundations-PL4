package net.foundations.pl4.compat;
import net.minecraft.util.registry.Registry;
/** Native global registries for this target, not post-1.16 registry keys. */
public final class Registries {
 public static final Registry<net.minecraft.block.Block> BLOCK=Registry.BLOCK;
 public static final Registry<net.minecraft.tileentity.TileEntityType<?>> BLOCK_ENTITY_TYPE=Registry.BLOCK_ENTITY_TYPE;
 public static final Registry<net.minecraft.world.dimension.DimensionType> DIMENSION=Registry.DIMENSION_TYPE;
 public static final Registry<net.minecraft.fluid.Fluid> FLUID=Registry.FLUID;
 public static final Registry<net.minecraft.item.Item> ITEM=Registry.ITEM;
 public static final Registry<net.minecraft.inventory.container.ContainerType<?>> MENU=Registry.MENU;
 public static final Registry<net.minecraft.item.crafting.IRecipeSerializer<?>> RECIPE_SERIALIZER=Registry.RECIPE_SERIALIZER;
 public static final Registry<net.minecraft.item.crafting.IRecipeType<?>> RECIPE_TYPE=Registry.RECIPE_TYPE;
 private Registries(){}
}
