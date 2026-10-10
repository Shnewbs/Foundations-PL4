package net.foundations.pl4.core;

/** The same six-face rules feed the server graph, cable rendering and hit boxes.
 * EnumFacing indices are Minecraft's DOWN, UP, NORTH, SOUTH, WEST, EAST (opposite = face ^ 1).
 * These primitives serve MultipartTopology; endpoints are not unrestricted cable relays.
 */
public final class ConnectionRules {
    public static final int NONE = 0, CABLE = 1, INTERNAL = 2, HALF = 3;
    private ConnectionRules() {}
    public static boolean blocked(int mask, int face) {
        checkFace(face);
        return (mask & (1 << face)) != 0;
    }
    public static boolean internal(boolean cableRedstone, boolean deviceRedstone, int blockedMask, int face) {
        return cableRedstone == deviceRedstone && !blocked(blockedMask, face);
    }
    public static boolean external(boolean redstoneA, boolean redstoneB, int blockedA, int blockedB,
                                   int occupiedA, int occupiedB, int direction) {
        checkFace(direction);
        return redstoneA == redstoneB
            && !blocked(blockedA | occupiedA, direction)
            && !blocked(blockedB | occupiedB, direction ^ 1);
    }
    public static int toggle(int mask, int face) { checkFace(face); return (mask ^ (1 << face)) & 63; }
    public static double armStart(int renderType) {
        return switch (renderType) { case CABLE -> 0; case INTERNAL -> 1; case HALF -> 3;
            default -> throw new IllegalArgumentException("No geometry for disconnected cable"); };
    }
    private static void checkFace(int face) {
        if (face < 0 || face >= 6) throw new IllegalArgumentException("Invalid face " + face);
    }
}
