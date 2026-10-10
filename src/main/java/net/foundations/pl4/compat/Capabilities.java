package net.foundations.pl4.compat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public final class Capabilities {
 public static final class ItemHandler {public static final BlockCapability<net.minecraftforge.items.IItemHandler,EnumFacing> BLOCK=new BlockCapability<>(new ResourceLocation("forge","item_handler"),net.minecraftforge.items.IItemHandler.class,EnumFacing.class,net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);}
 public static final class FluidHandler {public static final BlockCapability<net.minecraftforge.fluids.capability.IFluidHandler,EnumFacing> BLOCK=new BlockCapability<>(new ResourceLocation("forge","fluid_handler"),net.minecraftforge.fluids.capability.IFluidHandler.class,EnumFacing.class,net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);}
 public static final class EnergyStorage {public static final BlockCapability<net.minecraftforge.energy.IEnergyStorage,EnumFacing> BLOCK=new BlockCapability<>(new ResourceLocation("forge","energy"),net.minecraftforge.energy.IEnergyStorage.class,EnumFacing.class,net.minecraftforge.energy.CapabilityEnergy.ENERGY);}
 private Capabilities(){}
}
