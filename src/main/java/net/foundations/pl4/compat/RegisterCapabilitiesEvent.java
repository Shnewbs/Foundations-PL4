package net.foundations.pl4.compat;
import java.util.function.BiFunction;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.*;
public final class RegisterCapabilitiesEvent {
 public <T,B extends BlockEntity> void registerBlockEntity(BlockCapability<T,Direction> cap,BlockEntityType<B> type,BiFunction<B,Direction,T> provider){PortCapabilities.register(cap,type,provider);}
}
