package net.foundations.pl4.compat;
import net.minecraft.util.ResourceLocation;
public interface CustomPacketPayload {
 record Type<T extends CustomPacketPayload>(ResourceLocation id){}
 Type<? extends CustomPacketPayload> type();
}
