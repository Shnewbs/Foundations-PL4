package net.foundations.pl4.config;

/** Resolves the server-synced config type across the FML config rename. */
public final class ConfigTypeCompat {
    private ConfigTypeCompat() {}

    public static <T extends Enum<T>> T serverType(Class<T> type) {
        for (T candidate : type.getEnumConstants()) {
            if (candidate.name().equals("SYNCED")) return candidate;
        }
        for (T candidate : type.getEnumConstants()) {
            if (candidate.name().equals("SERVER")) return candidate;
        }
        throw new IllegalStateException("Loader has no server-synced config type: " + type.getName());
    }
}
