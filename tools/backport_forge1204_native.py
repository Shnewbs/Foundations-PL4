"""Port Forge48/49 GameTest and networking APIs without dropping features.

Use the native ChannelBuilder/SimpleChannel message builder, check packet
direction before dispatch, preserve all 191 original GameTests and let the
server own every client mutation via enqueueWork.
"""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
J=R/'src/main/java/net/foundations/pl4'
status_path=R/'BUILD_STATUS.json'
status=json.loads(status_path.read_text())
if status.get('minecraft')!='1.20.4' or status.get('loader_version')!='49.2.8':
    raise SystemExit('Refusing wrong native Forge compatibility target')

reg=J/'compat/RegisterPayloadHandlersEvent.java'
expected='net.minecraftforge.network.simple.SimpleChannel'
if status.get('native_compat_backport')=='forge-modern-channel-gametest-v1' and expected not in reg.read_text():
    print('Forge native channel and GameTests already adapted; verifying exact source state')
    assert 'net.minecraftforge.network.ChannelBuilder' in reg.read_text()
    assert '@net.minecraftforge.gametest.GameTestDontPrefix' in (J/'PLGameTests.java').read_text()
    raise SystemExit(0)

classes=list(J.glob('*GameTests.java'))
total=sum(p.read_text().count('@GameTest(') for p in classes)
if total!=191:raise SystemExit('Expected exactly 191 original GameTests, got '+str(total))
for p in classes:
    source=p.read_text(encoding='utf-8')
    source=source.replace('import net.minecraftforge.gametest.PrefixGameTestTemplate;\n','')
    if '@PrefixGameTestTemplate(false)' in source:
        source=source.replace('@PrefixGameTestTemplate(false)',
            '@net.minecraftforge.gametest.GameTestHolder(namespace=FoundationsPL4.ID)\n'
            '@net.minecraftforge.gametest.GameTestDontPrefix')
    source=source.replace(',templateNamespace=FoundationsPL4.ID','')
    if 'templateNamespace=' in source or '@PrefixGameTestTemplate' in source:
        raise SystemExit('Unadapted GameTest fixture '+p.name)
    p.write_text(source,encoding='utf-8')

reg.write_text('''package net.foundations.pl4.compat;
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
''',encoding='utf-8')
distributor=J/'compat/PacketDistributor.java'
distributor.write_text('''package net.foundations.pl4.compat;
import net.minecraft.server.level.ServerPlayer;
/** Targeted messages use Forge's native PacketTarget and reversed send order. */
public final class PacketDistributor {
 public static void sendToPlayer(ServerPlayer player,CustomPacketPayload packet){
  RegisterPayloadHandlersEvent.channel.send(packet,
      net.minecraftforge.network.PacketDistributor.PLAYER.with(player));
 }
 public static void sendToServer(CustomPacketPayload packet){
  RegisterPayloadHandlersEvent.channel.send(packet,
      net.minecraftforge.network.PacketDistributor.SERVER.noArg());
 }
 private PacketDistributor(){}
}
''',encoding='utf-8')

checks=R/'tools/run_port_checks.py'
s=checks.read_text(encoding='utf-8')
for old,new in [
 ("'NetworkDirection.PLAY_TO_SERVER' in packet and 'NetworkDirection.PLAY_TO_CLIENT' in packet",
  "'NetworkDirection.PLAY_TO_SERVER' in packet and 'NetworkDirection.PLAY_TO_CLIENT' in packet"),
 ("'version::equals,version::equals' in packet and 'setPacketHandled(true)' in packet",
  "'ChannelBuilder.named(' in packet and 'getDirection()!=direction' in packet and 'setPacketHandled(true)' in packet")
]:
 if old not in s:raise SystemExit('Unexpected native packet safety assertions')
 s=s.replace(old,new)
checks.write_text(s,encoding='utf-8')
status['native_compat_backport']='forge-modern-channel-gametest-v1'
status['native_compilation']='PENDING'
status['installed_runtime_acceptance']='PENDING'
status['further_api_testing_required']=True
status_path.write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')
print('Adapted exact 1.20.4 / Forge 49.2.8 network + 191 GameTests; no feature or scenario removed')
