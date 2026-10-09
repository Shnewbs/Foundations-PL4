package net.foundations.pl4.compat;

import net.minecraft.entity.player.ServerPlayerEntity;

/** Target-native payload dispatch. Players used by server automation may have no client. */
public final class PacketDistributor {
    public static void sendToPlayer(ServerPlayerEntity player, CustomPacketPayload packet) {
        // A missing connection has no outbound recipient. This does not grant permission or
        // suppress processing/validation failures: authorization remains in the packet handlers.
        if (player.connection == null) return;
        RegisterPayloadHandlersEvent.channel.send(
            net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> player), packet);
    }
    public static void sendToServer(CustomPacketPayload packet) {
        RegisterPayloadHandlersEvent.channel.sendToServer(packet);
    }
    private PacketDistributor() {}
}
