package net.foundations.pl4.compat;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Isolated server GameTest identity; no external fake-player library or optional runtime API. */
public final class TestPlayers {
    private TestPlayers() {}
    public static ServerPlayer get(ServerLevel level, GameProfile identity) {
        // Do not add fixture players to the real player list or impersonate a connected user.
        return new ServerPlayer(level.getServer(),level,identity,ClientInformation.createDefault());
    }
}
