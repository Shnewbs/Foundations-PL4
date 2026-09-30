package net.foundations.pl4;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Registry-aware codec persistence, preserving the existing escrow tag layout. */
public final class NbtStacks {
    public static Tag save(ItemStack stack,HolderLookup.Provider registry){return ItemStack.CODEC.encodeStart(registry.createSerializationContext(NbtOps.INSTANCE),stack).getOrThrow();}
    public static Tag save(FluidStack stack,HolderLookup.Provider registry){return FluidStack.CODEC.encodeStart(registry.createSerializationContext(NbtOps.INSTANCE),stack).getOrThrow();}
    public static ItemStack item(HolderLookup.Provider registry,CompoundTag tag){return ItemStack.CODEC.parse(registry.createSerializationContext(NbtOps.INSTANCE),tag).result().orElse(ItemStack.EMPTY);}
    public static FluidStack fluid(HolderLookup.Provider registry,CompoundTag tag){return FluidStack.CODEC.parse(registry.createSerializationContext(NbtOps.INSTANCE),tag).result().orElse(FluidStack.EMPTY);}
    private NbtStacks(){}
}
