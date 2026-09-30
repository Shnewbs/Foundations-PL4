package net.foundations.pl4.core;

/** Projector mount, projection origin and readable text front are separate coordinate systems.
 * Floor/ceiling projectors have upright text. The back view uses a new right-handed frame,
 * not mirrored glyphs. Only one readable side is submitted for each camera. */
public final class HologramProjection {
    /** Mount surface offset from the host centre, matching the projector base geometry. */
    public static final double MOUNT_SURFACE_OFFSET = .4375;
    /** Clear air between the mounting surface and a normal hologram plane. */
    public static final double NORMAL_PROJECTION_CLEARANCE = .90;
    /** Advanced holograms project farther so their larger base/cabling cannot cut through the canvas. */
    public static final double ADVANCED_PROJECTION_CLEARANCE = 1.20;
    public record Point(double x, double y, double z) {}
    public record Projection(Point centre, DisplayFacing.Frame frame) {}
    public static int view(int mount, int requested) {
        if (mount < 0 || mount > 5) throw new IllegalArgumentException("Invalid mount");
        return mount < 2 ? (requested >= 2 && requested <= 5 ? requested : 3) : mount ^ 1;
    }
    public static int nextView(int mount, int current) {
        if (mount >= 2) return view(mount,current);
        return switch(view(mount,current)) { case 2 -> 5; case 5 -> 3; case 3 -> 4; default -> 2; };
    }
    public static int baseYaw(int mount,int requested) {
        if(mount>=2)return 0;
        return switch(view(mount,requested)){case 2->180;case 4->90;case 5->270;default->0;};
    }
    public static double clearance(boolean advanced) {
        return advanced ? ADVANCED_PROJECTION_CLEARANCE : NORMAL_PROJECTION_CLEARANCE;
    }
    public static Point centre(int mount,boolean advanced) {
        var normal=DisplayFacing.facing(mount).normal();
        double distance=MOUNT_SURFACE_OFFSET-clearance(advanced);
        return new Point(.5+normal.x()*distance,.5+normal.y()*distance,.5+normal.z()*distance);
    }
    public static Projection forCamera(int mount,int requested,boolean advanced,Point camera) {
        var origin=centre(mount,advanced);
        int front=view(mount,requested);
        var frame=DisplayFacing.facing(front);
        double side=(camera.x-origin.x)*frame.normal().x()+(camera.y-origin.y)*frame.normal().y()+(camera.z-origin.z)*frame.normal().z();
        if(side<0)frame=DisplayFacing.facing(front^1);
        return new Projection(origin,frame);
    }
    private HologramProjection() {}
}
