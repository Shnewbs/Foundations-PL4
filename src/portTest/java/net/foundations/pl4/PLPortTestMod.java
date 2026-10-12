package net.foundations.pl4;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Registers the native scenarios only when the separate test mod is installed. */
@Mod(PLPortTestMod.ID)
public final class PLPortTestMod {
    public static final String ID = "foundations_pl4_native_tests";

    public PLPortTestMod(IEventBus bus) {
        bus.addListener(PLGameTests::register);
    }
}
