"""Reproducible first-pass Forge 1.20.1 port from the pinned 0.2a source."""
from pathlib import Path
import os,re,shutil,subprocess,json
W=Path(os.environ['PL4_PORT_WORK']).resolve();B=W/'base';R=W/'1.20.1'
if not (B/'src/main/java/net/foundations/pl4/FoundationsPL4.java').is_file():raise SystemExit('Missing pinned PL4 source')
if R.exists():shutil.rmtree(R)
shutil.copytree(B,R)
J=R/'src/main/java/net/foundations/pl4'
C=J/'compat';C.mkdir()
def write(name,text):
 p=J/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
def patch(name,old,new,required=True):
 p=J/name;s=p.read_text()
 if required and old not in s:raise RuntimeError((name,old))
 p.write_text(s.replace(old,new))
for p in (R/'src').rglob('*.java'):
 s=p.read_text()
 s=s.replace('net.neoforged.neoforge','net.minecraftforge').replace('net.neoforged.bus.api','net.minecraftforge.eventbus.api').replace('net.neoforged.fml','net.minecraftforge.fml').replace('net.neoforged.api','net.minecraftforge.api')
 s=s.replace('net.minecraftforge.common.NeoForge','net.minecraftforge.common.MinecraftForge').replace('NeoForge.EVENT_BUS','MinecraftForge.EVENT_BUS')
 s=s.replace('ModConfigSpec','ForgeConfigSpec')
 s=s.replace('net.minecraftforge.fml.common.EventBusSubscriber','net.minecraftforge.fml.common.Mod.EventBusSubscriber')
 s=s.replace('@EventBusSubscriber(modid=FoundationsPL4.ID,value=Dist.CLIENT)','@EventBusSubscriber(modid=FoundationsPL4.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)')
 s=s.replace('Math.clamp(', 'net.foundations.pl4.compat.PortMath.clamp(').replace('.getFirst()', '.get(0)')
 s=s.replace('ResourceLocation.fromNamespaceAndPath(', 'new ResourceLocation(').replace('ResourceLocation.parse(', 'new ResourceLocation(')
 s=s.replace('net.minecraft.resources.new ResourceLocation(', 'new net.minecraft.resources.ResourceLocation(')
 s=s.replace('ItemStack.isSameItemSameComponents','ItemStack.isSameItemSameTags')
 s=s.replace('FluidStack.isSameFluidSameComponents','net.foundations.pl4.compat.PortData.sameFluid')
 s=s.replace('HolderLookup.Provider','net.minecraft.core.RegistryAccess')
 s=s.replace('net.minecraft.core.component.DataComponents','net.foundations.pl4.compat.DataComponents')
 s=s.replace('net.minecraft.world.item.component.CustomData','net.foundations.pl4.compat.CustomData')
 s=s.replace('net.minecraft.world.item.component.ItemContainerContents','net.foundations.pl4.compat.ItemContainerContents')
 s=s.replace('net.minecraft.network.RegistryFriendlyByteBuf','net.minecraft.network.FriendlyByteBuf').replace('RegistryFriendlyByteBuf','FriendlyByteBuf')
 s=s.replace('net.minecraft.network.codec.StreamCodec','net.foundations.pl4.compat.StreamCodec')
 s=s.replace('net.minecraft.network.protocol.common.custom.CustomPacketPayload','net.foundations.pl4.compat.CustomPacketPayload')
 s=s.replace('net.minecraftforge.network.PacketDistributor','net.foundations.pl4.compat.PacketDistributor')
 s=s.replace('net.minecraftforge.network.event.RegisterPayloadHandlersEvent','net.foundations.pl4.compat.RegisterPayloadHandlersEvent')
 s=s.replace('net.minecraftforge.capabilities.BlockCapability','net.foundations.pl4.compat.BlockCapability')
 s=s.replace('net.minecraftforge.capabilities.Capabilities','net.foundations.pl4.compat.Capabilities')
 s=s.replace('net.minecraftforge.capabilities.RegisterCapabilitiesEvent','net.foundations.pl4.compat.RegisterCapabilitiesEvent')
 s=s.replace('import net.minecraftforge.capabilities.*;','import net.foundations.pl4.compat.Capabilities;\nimport net.foundations.pl4.compat.BlockCapability;\nimport net.foundations.pl4.compat.RegisterCapabilitiesEvent;')
 s=s.replace('import net.minecraftforge.event.tick.ServerTickEvent;','import net.minecraftforge.event.TickEvent.ServerTickEvent;')
 s=s.replace('ServerTickEvent.Post','ServerTickEvent')
 s=re.sub(r'DeferredHolder<(?:MenuType|BlockEntityType|RecipeType|RecipeSerializer)<\?>,([^;=]+?)>',r'RegistryObject<\1>',s)
 s=s.replace('import net.minecraftforge.registries.DeferredHolder;','import net.minecraftforge.registries.RegistryObject;')
 s=s.replace('DeferredBlock<','RegistryObject<').replace('DeferredItem<','RegistryObject<')
 s=s.replace('DeferredRegister.Blocks','DeferredRegister<Block>').replace('DeferredRegister.Items','DeferredRegister<Item>')
 s=s.replace('DeferredRegister.createBlocks(ID)','DeferredRegister.create(Registries.BLOCK,ID)').replace('DeferredRegister.createItems(ID)','DeferredRegister.create(Registries.ITEM,ID)')
 s=s.replace('ITEMS.registerSimpleItem(id)','ITEMS.register(id, () -> new Item(new Item.Properties()))')
 s=s.replace('ITEMS.registerSimpleBlockItem(ORE); ITEMS.registerSimpleBlockItem(HAMMER);','ITEMS.register("sapphireore", () -> new BlockItem(ORE.get(),new Item.Properties())); ITEMS.register("hammer", () -> new BlockItem(HAMMER.get(),new Item.Properties()));')
 p.write_text(s)
write('compat/PortMath.java','''package net.foundations.pl4.compat;
/** Java 17 implementation of the bounded arithmetic used by the common PL4 rules. */
public final class PortMath {
 public static int clamp(long value,int min,int max){if(min>max)throw new IllegalArgumentException("min > max");return (int)Math.min(max,Math.max(min,value));}
 public static long clamp(long value,long min,long max){if(min>max)throw new IllegalArgumentException("min > max");return Math.min(max,Math.max(min,value));}
 public static double clamp(double value,double min,double max){if(Double.isNaN(min)||Double.isNaN(max)||Double.compare(min,max)>0)throw new IllegalArgumentException("Invalid bounds");return Math.min(max,Math.max(min,value));}
 public static float clamp(float value,float min,float max){if(Float.isNaN(min)||Float.isNaN(max)||Float.compare(min,max)>0)throw new IllegalArgumentException("Invalid bounds");return Math.min(max,Math.max(min,value));}
 private PortMath(){}
}
''')
write('compat/PortLists.java','''package net.foundations.pl4.compat;
public final class PortLists {
 public static <T> T last(java.util.List<T> list){if(list.isEmpty())throw new java.util.NoSuchElementException();return list.get(list.size()-1);}
 private PortLists(){}
}
''')
write('compat/DataComponents.java','''package net.foundations.pl4.compat;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
/** PL4-owned bridge tokens; this does not add Minecraft data components to older games. */
public final class DataComponents {
 public record Key<T>(String name){}
 public static final Key<CustomData> CUSTOM_DATA=new Key<>("custom_data");
 public static final Key<Component> CUSTOM_NAME=new Key<>("custom_name");
 public static final Key<ItemContainerContents> CONTAINER=new Key<>("container");
 public static final Key<CompoundTag> BLOCK_ENTITY_DATA=new Key<>("block_entity_data");
 private DataComponents(){}
}
''')
write('compat/CustomData.java','''package net.foundations.pl4.compat;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
public record CustomData(CompoundTag tag){
 public CustomData {tag=tag.copy();}
 public CompoundTag copyTag(){return tag.copy();}
 public boolean contains(String key){return tag.contains(key);}
 public static void update(DataComponents.Key<CustomData> key,ItemStack stack,Consumer<CompoundTag> change){
  if(key!=DataComponents.CUSTOM_DATA)throw new IllegalArgumentException("Not custom data");
  CompoundTag tag=stack.hasTag()?stack.getTag().copy():new CompoundTag();change.accept(tag);stack.setTag(tag.isEmpty()?null:tag);
 }
}
''')
write('compat/ItemContainerContents.java','''package net.foundations.pl4.compat;
import java.util.List;
import net.minecraft.world.item.ItemStack;
public record ItemContainerContents(List<ItemStack> items){
 public ItemContainerContents {items=items.stream().map(ItemStack::copy).toList();}
 public static ItemContainerContents fromItems(List<ItemStack> items){return new ItemContainerContents(items);}
}
''')
write('compat/PortData.java','''package net.foundations.pl4.compat;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
/** Native pre-component NBT persistence. Never writes to a live item during preview sampling. */
public final class PortData {
 @SuppressWarnings("unchecked") public static <T> T get(ItemStack stack,DataComponents.Key<T> key){
  if(key==DataComponents.CUSTOM_DATA)return stack.hasTag()?(T)new CustomData(stack.getTag()):null;
  if(key==DataComponents.CUSTOM_NAME)return stack.hasCustomHoverName()?(T)stack.getHoverName():null;
  if(key==DataComponents.BLOCK_ENTITY_DATA)return stack.hasTag()&&stack.getTag().contains("BlockEntityTag",Tag.TAG_COMPOUND)?(T)stack.getTag().getCompound("BlockEntityTag").copy():null;
  if(key==DataComponents.CONTAINER){
   if(!stack.hasTag()||!stack.getTag().getCompound("BlockEntityTag").contains("Items",Tag.TAG_LIST))return null;
   var items=new java.util.ArrayList<ItemStack>();for(Tag entry:stack.getTag().getCompound("BlockEntityTag").getList("Items",Tag.TAG_COMPOUND))items.add(ItemStack.of((CompoundTag)entry));return (T)new ItemContainerContents(items);
  }
  throw new IllegalArgumentException("Unsupported bridge key "+key.name());
 }
 public static <T> boolean has(ItemStack stack,DataComponents.Key<T> key){return get(stack,key)!=null;}
 public static <T> void set(ItemStack stack,DataComponents.Key<T> key,T value){
  if(key==DataComponents.CUSTOM_NAME){stack.setHoverName((Component)value);return;}
  if(key==DataComponents.CUSTOM_DATA){stack.setTag(((CustomData)value).copyTag());return;}
  if(key==DataComponents.BLOCK_ENTITY_DATA){stack.getOrCreateTag().put("BlockEntityTag",((CompoundTag)value).copy());return;}
  if(key==DataComponents.CONTAINER){ListTag items=new ListTag();int slot=0;for(ItemStack item:((ItemContainerContents)value).items()){CompoundTag t=item.save(new CompoundTag());t.putByte("Slot",(byte)slot++);items.add(t);}stack.getOrCreateTagElement("BlockEntityTag").put("Items",items);return;}
  throw new IllegalArgumentException("Unsupported bridge key "+key.name());
 }
 public static <T> void remove(ItemStack stack,DataComponents.Key<T> key){
  if(key==DataComponents.CUSTOM_NAME){stack.resetHoverName();return;}
  if(key==DataComponents.CUSTOM_DATA){stack.setTag(null);return;}
  if(!stack.hasTag())return;
  if(key==DataComponents.BLOCK_ENTITY_DATA)stack.getTag().remove("BlockEntityTag");
  else if(key==DataComponents.CONTAINER){CompoundTag block=stack.getTagElement("BlockEntityTag");if(block!=null){block.remove("Items");if(block.isEmpty())stack.getTag().remove("BlockEntityTag");}}
  else throw new IllegalArgumentException("Unsupported bridge key "+key.name());
  if(stack.getTag().isEmpty())stack.setTag(null);
 }
 public static CompoundTag components(ItemStack s){return s.hasTag()?s.getTag().copy():new CompoundTag();}
 public static CompoundTag components(FluidStack s){return s.hasTag()?s.getTag().copy():new CompoundTag();}
 public static CompoundTag save(ItemStack s,Object registry){return s.save(new CompoundTag());}
 public static CompoundTag save(FluidStack s,Object registry){return s.writeToNBT(new CompoundTag());}
 public static ItemStack parseItem(Object registry,CompoundTag tag){return ItemStack.of(tag);}
 public static FluidStack parseFluid(Object registry,CompoundTag tag){return FluidStack.loadFluidStackFromNBT(tag);}
 public static FluidStack copyWithAmount(FluidStack s,int amount){FluidStack out=s.copy();out.setAmount(amount);return out;}
 public static boolean sameFluid(FluidStack a,FluidStack b){return a.isFluidEqual(b);}
 private PortData(){}
}
''')
write('compat/StreamCodec.java','''package net.foundations.pl4.compat;
import java.util.function.*;
public record StreamCodec<B,T>(BiConsumer<B,T> writer,Function<B,T> reader){
 public void encode(B buffer,T value){writer.accept(buffer,value);}
 public T decode(B buffer){return reader.apply(buffer);}
 public static <B,T> StreamCodec<B,T> of(BiConsumer<B,T> writer,Function<B,T> reader){return new StreamCodec<>(writer,reader);}
}
''')
write('compat/CustomPacketPayload.java','''package net.foundations.pl4.compat;
import net.minecraft.resources.ResourceLocation;
public interface CustomPacketPayload {
 record Type<T extends CustomPacketPayload>(ResourceLocation id){}
 Type<? extends CustomPacketPayload> type();
}
''')
write('compat/RegisterPayloadHandlersEvent.java','''package net.foundations.pl4.compat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.foundations.pl4.FoundationsPL4;
import java.util.function.*;
/** Forge SimpleChannel implementation preserving direction checks and server-thread mutation. */
public final class RegisterPayloadHandlersEvent {
 public static SimpleChannel channel;
 public Registrar registrar(String protocol){
  if(channel!=null)throw new IllegalStateException("PL4 packets already registered");
  String version="forge-1.20.1-"+protocol;
  channel=NetworkRegistry.newSimpleChannel(FoundationsPL4.id("main"),()->version,version::equals,version::equals);
  return new Registrar();
 }
 public record Context(NetworkEvent.Context nativeContext){
  public ServerPlayer player(){return nativeContext.getSender();}
  public void enqueueWork(Runnable action){nativeContext.enqueueWork(action);}
 }
 public static final class Registrar {
  private int next;
  public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type,StreamCodec<FriendlyByteBuf,T> codec,BiConsumer<T,Context> handler){add(type,codec,handler,NetworkDirection.PLAY_TO_SERVER);}
  public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type,StreamCodec<FriendlyByteBuf,T> codec,BiConsumer<T,Context> handler){add(type,codec,handler,NetworkDirection.PLAY_TO_CLIENT);}
  @SuppressWarnings("unchecked") private <T extends CustomPacketPayload> void add(CustomPacketPayload.Type<T> type,StreamCodec<FriendlyByteBuf,T> codec,BiConsumer<T,Context> handler,NetworkDirection direction){
   Class<T> packetClass=null;
   for(Class<?> c:net.foundations.pl4.PLPackets.class.getDeclaredClasses())try{if(c.getField("TYPE").get(null)==type){packetClass=(Class<T>)c;break;}}catch(ReflectiveOperationException ignored){}
   if(packetClass==null)throw new IllegalArgumentException("Unknown PL4 packet "+type.id());
   channel.registerMessage(next++,packetClass,(packet,buffer)->codec.encode(buffer,packet),codec::decode,(packet,supplier)->{NetworkEvent.Context context=supplier.get();handler.accept(packet,new Context(context));context.setPacketHandled(true);},java.util.Optional.of(direction));
  }
 }
}
''')
write('compat/PacketDistributor.java','''package net.foundations.pl4.compat;
import net.minecraft.server.level.ServerPlayer;
public final class PacketDistributor {
 public static void sendToPlayer(ServerPlayer player,CustomPacketPayload packet){RegisterPayloadHandlersEvent.channel.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),packet);}
 public static void sendToServer(CustomPacketPayload packet){RegisterPayloadHandlersEvent.channel.sendToServer(packet);}
 private PacketDistributor(){}
}
''')
write('compat/BlockCapability.java','''package net.foundations.pl4.compat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import java.util.*;
public record BlockCapability<T,C>(ResourceLocation name,Class<T> typeClass,Class<C> contextClass,Capability<T> nativeCapability){
 private static final List<BlockCapability<?,?>> FOREIGN=new ArrayList<>();
 public static synchronized <T> void registerForeign(String id,Class<T> type,Capability<T> capability){
  ResourceLocation name=new ResourceLocation(id);if(FOREIGN.stream().anyMatch(c->c.name().equals(name)))throw new IllegalArgumentException("Duplicate PL4 provider "+id);
  FOREIGN.add(new BlockCapability<>(name,type,Direction.class,capability));
 }
 public static synchronized List<BlockCapability<?,?>> getAll(){return List.copyOf(FOREIGN);}
}
''')
write('compat/Capabilities.java','''package net.foundations.pl4.compat;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
public final class Capabilities {
 public static final class ItemHandler {public static final BlockCapability<net.minecraftforge.items.IItemHandler,Direction> BLOCK=new BlockCapability<>(new ResourceLocation("forge","item_handler"),net.minecraftforge.items.IItemHandler.class,Direction.class,ForgeCapabilities.ITEM_HANDLER);}
 public static final class FluidHandler {public static final BlockCapability<net.minecraftforge.fluids.capability.IFluidHandler,Direction> BLOCK=new BlockCapability<>(new ResourceLocation("forge","fluid_handler"),net.minecraftforge.fluids.capability.IFluidHandler.class,Direction.class,ForgeCapabilities.FLUID_HANDLER);}
 public static final class EnergyStorage {public static final BlockCapability<net.minecraftforge.energy.IEnergyStorage,Direction> BLOCK=new BlockCapability<>(new ResourceLocation("forge","energy"),net.minecraftforge.energy.IEnergyStorage.class,Direction.class,ForgeCapabilities.ENERGY);}
 private Capabilities(){}
}
''')
write('compat/RegisterCapabilitiesEvent.java','''package net.foundations.pl4.compat;
import java.util.function.BiFunction;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.*;
public final class RegisterCapabilitiesEvent {
 public <T,B extends BlockEntity> void registerBlockEntity(BlockCapability<T,Direction> cap,BlockEntityType<B> type,BiFunction<B,Direction,T> provider){PortCapabilities.register(cap,type,provider);}
}
''')
write('compat/PortCapabilities.java','''package net.foundations.pl4.compat;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.foundations.pl4.FoundationsPL4;
import java.util.*;
import java.util.function.BiFunction;
import java.lang.ref.WeakReference;
/** Loaded-chunk-only queries and invalidatable Forge capability providers. */
public final class PortCapabilities {
 private static final Map<BlockEntityType<?>,Map<Capability<?>,BiFunction<BlockEntity,Direction,?>>> PROVIDERS=new IdentityHashMap<>();
 private static final Map<BlockEntity,WeakReference<Binding>> BINDINGS=new WeakHashMap<>();
 @SuppressWarnings("unchecked") public static <T,B extends BlockEntity> void register(BlockCapability<T,Direction> cap,BlockEntityType<B> type,BiFunction<B,Direction,T> provider){PROVIDERS.computeIfAbsent(type,k->new IdentityHashMap<>()).put(cap.nativeCapability(),(be,side)->provider.apply((B)be,side));}
 public static <T> T get(Level level,BlockCapability<T,Direction> cap,BlockPos pos,Direction side){
  if(level==null||!level.hasChunkAt(pos))return null;
  BlockEntity be=level.getBlockEntity(pos);return be==null||be.isRemoved()?null:be.getCapability(cap.nativeCapability(),side).orElse(null);
 }
 public static void attach(AttachCapabilitiesEvent<BlockEntity> event){
  BlockEntity be=event.getObject();
  if(!(be instanceof net.foundations.pl4.HostEntity)&&!(be instanceof net.foundations.pl4.HammerEntity))return;
  Binding binding=new Binding(be);synchronized(BINDINGS){BINDINGS.put(be,new WeakReference<>(binding));}
  event.addCapability(FoundationsPL4.id("native_adapters"),binding);event.addListener(binding::invalidate);
 }
 public static void invalidate(BlockEntity be){Binding b; synchronized(BINDINGS){var ref=BINDINGS.get(be);b=ref==null?null:ref.get();}if(b!=null)b.invalidate();}
 private record Key(Capability<?> capability,Direction side){}
 private static final class Binding implements ICapabilityProvider {
  private final BlockEntity be;private final Map<Key,LazyOptional<?>> cached=new HashMap<>();
  Binding(BlockEntity be){this.be=be;}
  public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){
   if(be.isRemoved())return LazyOptional.empty();var byType=PROVIDERS.get(be.getType());if(byType==null)return LazyOptional.empty();var provider=byType.get(cap);if(provider==null)return LazyOptional.empty();
   Key key=new Key(cap,side);LazyOptional<?> existing=cached.get(key);if(existing!=null)return existing.cast();
   Object value=provider.apply(be,side);if(value==null)return LazyOptional.empty();var result=LazyOptional.of(()->value);cached.put(key,result);return result.cast();
  }
  void invalidate(){var previous=new ArrayList<>(cached.values());cached.clear();previous.forEach(LazyOptional::invalidate);}
 }
 private PortCapabilities(){}
}
''')
patch('FoundationsPL4.java','public FoundationsPL4(IEventBus bus, ModContainer container) {','public FoundationsPL4() {\n        IEventBus bus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();\n        var container=net.minecraftforge.fml.ModLoadingContext.get();')
patch('FoundationsPL4.java','        bus.addListener(PLPackets::register);\n        bus.addListener(HammerEntity::capabilities);\n        bus.addListener(NativeEnergyInput::register);','''        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e)->{
            PLPackets.register(new net.foundations.pl4.compat.RegisterPayloadHandlersEvent());
            var caps=new net.foundations.pl4.compat.RegisterCapabilitiesEvent();
            HammerEntity.capabilities(caps);NativeEnergyInput.register(caps);
        });
        MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.world.level.block.entity.BlockEntity.class,net.foundations.pl4.compat.PortCapabilities::attach);''')
patch('HostEntity.java','level.invalidateCapabilities(worldPosition);','net.foundations.pl4.compat.PortCapabilities.invalidate(this);')
patch('NetworkEngine.java','public static void tick(ServerTickEvent e){','public static void tick(ServerTickEvent e){\n        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;')
patch('client/PLClient.java','import net.minecraftforge.client.event.RegisterMenuScreensEvent;','')
patch('client/PLClient.java','e.enqueueWork(()->{','e.enqueueWork(()->{net.minecraft.client.gui.screens.MenuScreens.register(FoundationsPL4.HAMMER_MENU.get(),HammerScreen::new);')
p=J/'client/PLClient.java';s=p.read_text();s=re.sub(r'    @SubscribeEvent public static void screens\(RegisterMenuScreensEvent e\).*?\n','',s);p.write_text(s)
subprocess.run(['java','-cp',str(W),'RewriteCalls',str(R/'src/main/java')],check=True)
for p in (R/'src').rglob('*.java'):
 p.write_text(p.read_text().replace('fluid.getHoverName()','fluid.getDisplayName()'))
print('First source migration complete; native build is a separate gate:',R)
