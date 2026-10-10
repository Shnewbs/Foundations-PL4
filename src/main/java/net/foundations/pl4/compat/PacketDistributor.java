package net.foundations.pl4.compat;
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
