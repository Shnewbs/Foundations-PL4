package net.foundations.pl4.compat;
import java.util.function.BiFunction;
import net.minecraft.util.EnumFacing;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
public final class RegisterCapabilitiesEvent {
 public <T,B extends TileEntity> void registerBlockEntity(BlockCapability<T,EnumFacing> cap,TileEntityType<B> type,BiFunction<B,EnumFacing,T> provider){PortCapabilities.register(cap,type,provider);}
}
