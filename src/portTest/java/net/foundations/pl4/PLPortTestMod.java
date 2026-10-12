package net.foundations.pl4;

import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Registers the development-only scenarios within the PL4 module. */
public final class PLPortTestMod {
    public static void register(RegisterGameTestsEvent event) { PLGameTests.register(event); }
}
