package net.foundations.pl4.compat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.foundations.pl4.FoundationsPL4;
import java.util.*;
import java.util.function.BiFunction;
import java.lang.ref.WeakReference;
/** Loaded-chunk-only queries and invalidatable Forge capability providers. */
public final class PortCapabilities {
 private static final Map<TileEntityType<?>,Map<Capability<?>,BiFunction<TileEntity,EnumFacing,?>>> PROVIDERS=new IdentityHashMap<>();
 private static final Map<TileEntity,WeakReference<Binding>> BINDINGS=new WeakHashMap<>();
 @SuppressWarnings("unchecked") public static <T,B extends TileEntity> void register(BlockCapability<T,EnumFacing> cap,TileEntityType<B> type,BiFunction<B,EnumFacing,T> provider){PROVIDERS.computeIfAbsent(type,k->new IdentityHashMap<>()).put(cap.nativeCapability(),(be,side)->provider.apply((B)be,side));}
 public static <T> T get(World level,BlockCapability<T,EnumFacing> cap,BlockPos pos,EnumFacing side){
  if(level==null||!level.hasChunkAt(pos))return null;
  TileEntity be=level.getBlockEntity(pos);return be==null||be.isRemoved()?null:be.getCapability(cap.nativeCapability(),side).orElse(null);
 }
 public static void attach(AttachCapabilitiesEvent<TileEntity> event){
  TileEntity be=event.getObject();
  if(!(be instanceof net.foundations.pl4.HostEntity)&&!(be instanceof net.foundations.pl4.HammerEntity))return;
  Binding binding=new Binding(be);synchronized(BINDINGS){BINDINGS.put(be,new WeakReference<>(binding));}
  event.addCapability(FoundationsPL4.id("native_adapters"),binding);event.addListener(binding::invalidate);
 }
 public static void invalidate(TileEntity be){Binding b; synchronized(BINDINGS){var ref=BINDINGS.get(be);b=ref==null?null:ref.get();}if(b!=null)b.invalidate();}
 private record Key(Capability<?> capability,EnumFacing side){}
 private static final class Binding implements ICapabilityProvider {
  private final TileEntity be;private final Map<Key,LazyOptional<?>> cached=new HashMap<>();
  Binding(TileEntity be){this.be=be;}
  public <T> LazyOptional<T> getCapability(Capability<T> cap,EnumFacing side){
   if(be.isRemoved())return LazyOptional.empty();var byType=PROVIDERS.get(be.getType());if(byType==null)return LazyOptional.empty();var provider=byType.get(cap);if(provider==null)return LazyOptional.empty();
   Key key=new Key(cap,side);LazyOptional<?> existing=cached.get(key);if(existing!=null)return existing.cast();
   Object value=provider.apply(be,side);if(value==null)return LazyOptional.empty();var result=LazyOptional.of(()->value);cached.put(key,result);return result.cast();
  }
  void invalidate(){var previous=new ArrayList<>(cached.values());cached.clear();previous.forEach(LazyOptional::invalidate);}
 }
 private PortCapabilities(){}
}
