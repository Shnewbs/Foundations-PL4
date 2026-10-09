package net.foundations.pl4.compat;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
public final class Capabilities {
 public static final class ItemHandler {public static final BlockCapability<net.minecraftforge.items.IItemHandler,Direction> BLOCK=new BlockCapability<>(new ResourceLocation("forge","item_handler"),net.minecraftforge.items.IItemHandler.class,Direction.class,ForgeCapabilities.ITEM_HANDLER);}
 public static final class FluidHandler {public static final BlockCapability<net.minecraftforge.fluids.capability.IFluidHandler,Direction> BLOCK=new BlockCapability<>(new ResourceLocation("forge","fluid_handler"),net.minecraftforge.fluids.capability.IFluidHandler.class,Direction.class,ForgeCapabilities.FLUID_HANDLER);}
 public static final class EnergyStorage {public static final BlockCapability<net.minecraftforge.energy.IEnergyStorage,Direction> BLOCK=new BlockCapability<>(new ResourceLocation("forge","energy"),net.minecraftforge.energy.IEnergyStorage.class,Direction.class,ForgeCapabilities.ENERGY);}
 private Capabilities(){}
}
