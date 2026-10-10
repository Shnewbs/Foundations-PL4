package net.foundations.pl4.compat;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.foundations.pl4.FoundationsPL4;
import java.util.function.*;
/** Forge SimpleChannel implementation preserving direction checks and server-thread mutation. */
public final class RegisterPayloadHandlersEvent {
 public static SimpleChannel channel;
 public Registrar registrar(String protocol){
  if(channel!=null)throw new IllegalStateException("PL4 packets already registered");
  String version="forge-1.13.2-"+protocol;
  channel=NetworkRegistry.newSimpleChannel(FoundationsPL4.id("main"),()->version,version::equals,version::equals);
  return new Registrar();
 }
 public record Context(NetworkEvent.Context nativeContext){
  public net.minecraft.entity.player.PlayerEntity player(){return nativeContext.getSender();}
  public void enqueueWork(Runnable action){nativeContext.enqueueWork(action);}
 }
 public static final class Registrar {
  private int next;
  public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type,StreamCodec<PacketBuffer,T> codec,BiConsumer<T,Context> handler){add(type,codec,handler,NetworkDirection.PLAY_TO_SERVER);}
  public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type,StreamCodec<PacketBuffer,T> codec,BiConsumer<T,Context> handler){add(type,codec,handler,NetworkDirection.PLAY_TO_CLIENT);}
  @SuppressWarnings("unchecked") private <T extends CustomPacketPayload> void add(CustomPacketPayload.Type<T> type,StreamCodec<PacketBuffer,T> codec,BiConsumer<T,Context> handler,NetworkDirection direction){
   Class<T> packetClass=null;
   for(Class<?> c:net.foundations.pl4.PLPackets.class.getDeclaredClasses())try{if(c.getField("TYPE").get(null)==type){packetClass=(Class<T>)c;break;}}catch(ReflectiveOperationException ignored){}
   if(packetClass==null)throw new IllegalArgumentException("Unknown PL4 packet "+type.id());
   channel.registerMessage(next++,packetClass,(packet,buffer)->codec.encode(buffer,packet),codec::decode,(packet,supplier)->{NetworkEvent.Context context=supplier.get();if(context.getDirection()!=direction){context.setPacketHandled(true);return;}handler.accept(packet,new Context(context));context.setPacketHandled(true);});
  }
 }
}
