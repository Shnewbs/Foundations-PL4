package net.foundations.pl4.compat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.foundations.pl4.FoundationsPL4;
import java.util.function.BiConsumer;

/** Exact Forge native SimpleChannel. The handshake is numeric and version exact.
 * Direction is asserted before calling a client-provided handler; all server
 * world mutations are queued to the server thread inside PLPackets.
 */
public final class RegisterPayloadHandlersEvent {
 public static SimpleChannel channel;
 public Registrar registrar(String protocol){
  if(channel!=null)throw new IllegalStateException("PL4 packets already registered");
  if(protocol==null||!protocol.matches("[0-9]{1,6}"))throw new IllegalArgumentException("Invalid PL4 protocol");
  channel=ChannelBuilder.named(FoundationsPL4.id("main"))
      .networkProtocolVersion(Integer.parseInt(protocol)).simpleChannel();
  return new Registrar();
 }
 public record Context(CustomPayloadEvent.Context nativeContext) {
  public net.minecraft.world.entity.player.Player player(){return nativeContext.getSender();}
  public void enqueueWork(Runnable action){nativeContext.enqueueWork(action);}
 }
 public static final class Registrar {
  private int next;
  public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type,StreamCodec<FriendlyByteBuf,T> codec,BiConsumer<T,Context> handler){
   add(type,codec,handler,NetworkDirection.PLAY_TO_SERVER);
  }
  public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type,StreamCodec<FriendlyByteBuf,T> codec,BiConsumer<T,Context> handler){
   add(type,codec,handler,NetworkDirection.PLAY_TO_CLIENT);
  }
  @SuppressWarnings("unchecked")
  private <T extends CustomPacketPayload> void add(CustomPacketPayload.Type<T> type,StreamCodec<FriendlyByteBuf,T> codec,BiConsumer<T,Context> handler,NetworkDirection direction){
   Class<T> packetClass=null;
   for(Class<?> c:net.foundations.pl4.PLPackets.class.getDeclaredClasses()){
    try{if(c.getField("TYPE").get(null)==type){packetClass=(Class<T>)c;break;}}
    catch(ReflectiveOperationException ignored){}
   }
   if(packetClass==null)throw new IllegalArgumentException("Unknown PL4 packet "+type.id());
   channel.messageBuilder(packetClass,next++,direction)
      .encoder((packet,buffer)->codec.encode(buffer,packet))
      .decoder(codec::decode)
      .consumerNetworkThread((packet,nativeContext)->{
       if(nativeContext.getDirection()!=direction){
        nativeContext.setPacketHandled(true);
        return;
       }
       handler.accept(packet,new Context(nativeContext));
       nativeContext.setPacketHandled(true);
      }).add();
  }
 }
}
