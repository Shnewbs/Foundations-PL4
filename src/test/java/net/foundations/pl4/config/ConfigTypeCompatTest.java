package net.foundations.pl4.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTypeCompatTest {
    enum Legacy { COMMON, CLIENT, SERVER, STARTUP }
    enum Modern { LOCAL, CLIENT, SYNCED, STARTUP }
    enum Transition { SERVER, SYNCED, CLIENT }
    enum Unsupported { LOCAL, CLIENT }

    @Test void retainsLegacyServerSync() {
        assertEquals(Legacy.SERVER, ConfigTypeCompat.serverType(Legacy.class));
    }
    @Test void selectsModernSyncedConfig() {
        assertEquals(Modern.SYNCED, ConfigTypeCompat.serverType(Modern.class));
        assertEquals(Transition.SYNCED, ConfigTypeCompat.serverType(Transition.class));
    }
    @Test void neverFallsBackToUnsyncedConfig() {
        assertThrows(IllegalStateException.class, () -> ConfigTypeCompat.serverType(Unsupported.class));
    }
}
