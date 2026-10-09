package net.foundations.pl4.compat;
import net.minecraft.entity.player.ServerPlayerEntity;
public final class PacketDistributor {
 public static void sendToPlayer(ServerPlayerEntity player,CustomPacketPayload packet){RegisterPayloadHandlersEvent.channel.send(net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(()->player),packet);}
 public static void sendToServer(CustomPacketPayload packet){RegisterPayloadHandlersEvent.channel.sendToServer(packet);}
 private PacketDistributor(){}
}
