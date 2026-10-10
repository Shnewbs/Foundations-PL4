package net.foundations.pl4.compat;
import net.minecraft.entity.player.EntityPlayerMP;
public final class PacketDistributor {
 public static void sendToPlayer(EntityPlayerMP player,CustomPacketPayload packet){
  if(player.connection==null)return;
  RegisterPayloadHandlersEvent.channel.send(net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(()->player),packet);
 }
 public static void sendToServer(CustomPacketPayload packet){RegisterPayloadHandlersEvent.channel.sendToServer(packet);}
 private PacketDistributor(){}
}
