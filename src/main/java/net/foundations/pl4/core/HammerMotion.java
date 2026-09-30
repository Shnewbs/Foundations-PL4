package net.foundations.pl4.core;

/** Pure animation/timing rules, also executed by the offline regression tests. */
public final class HammerMotion {
    private HammerMotion() {}
    public static double fraction(int progress, int duration, int cooldown, int cooldownTotal,
                                  double elapsedTicks, boolean working) {
        double elapsed = Double.isFinite(elapsedTicks) ? Math.clamp(elapsedTicks, 0, 20) : 0;
        if (cooldown > 0) return Math.clamp((cooldown - elapsed) / Math.max(1.0, cooldownTotal), 0, 1);
        return working ? Math.clamp((progress + elapsed) / Math.max(1.0, duration), 0, 1) : 0;
    }
    public static int progressPixels(int progress, int duration) {
        return (int) Math.clamp((long)Math.max(0, progress) * 23 / Math.max(1, duration), 0, 23);
    }
    public static boolean outputFits(int existingCount, int outputCount, int limit, boolean sameComponents) {
        return existingCount >= 0 && outputCount > 0 && limit > 0
            && (existingCount == 0 || sameComponents) && (long)existingCount + outputCount <= limit;
    }
}
