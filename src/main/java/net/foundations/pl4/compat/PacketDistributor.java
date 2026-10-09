package net.foundations.pl4.compat;
import net.minecraft.server.level.ServerPlayer;
public final class PacketDistributor {
 public static void sendToPlayer(ServerPlayer player,CustomPacketPayload packet){RegisterPayloadHandlersEvent.channel.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),packet);}
 public static void sendToServer(CustomPacketPayload packet){RegisterPayloadHandlersEvent.channel.sendToServer(packet);}
 private PacketDistributor(){}
}
