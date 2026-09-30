package net.foundations.pl4.core;

/** Projector geometry and readable text orientation are separate coordinate systems. */
public final class HologramProjection {
    /** Clear air between the emitter hardware and a normal hologram plane. */
    public static final double NORMAL_PROJECTION_CLEARANCE = .90;
    /** Advanced holograms project farther so their larger base/cabling cannot cut through the canvas. */
    public static final double ADVANCED_PROJECTION_CLEARANCE = 1.20;
    public record Point(double x, double y, double z) {}
    public record Projection(Point centre, DisplayFacing.Frame frame) {}
    public record Geometry(Point mountAnchor, Point emitterAnchor, DisplayFacing.Vector normal, double distance) {
        public Point centre() {
            return new Point(emitterAnchor.x()+normal.x()*distance,
                emitterAnchor.y()+normal.y()*distance,emitterAnchor.z()+normal.z()*distance);
        }
    }
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
    public static Geometry geometry(int mount,int requested,boolean advanced) {
        int front=view(mount,requested);
        var normal=DisplayFacing.facing(front).normal();
        int yaw=baseYaw(mount,requested);
        Point mountAnchor=modelPoint(mount,yaw,8,advanced?3:0,8);
        Point emitterAnchor;
        if(advanced) {
            Point[] panels={modelPoint(mount,yaw,5,2,8),modelPoint(mount,yaw,11,2,8),
                modelPoint(mount,yaw,8,2,5),modelPoint(mount,yaw,8,2,11)};
            emitterAnchor=panels[0];
            for(int i=1;i<panels.length;i++)
                if(dot(offset(panels[i]),normal)>dot(offset(emitterAnchor),normal))emitterAnchor=panels[i];
        } else {
            emitterAnchor=modelPoint(mount,yaw,8,0,10.5);
        }
        return new Geometry(mountAnchor,emitterAnchor,normal,clearance(advanced));
    }
    public static Point centre(int mount,int requested,boolean advanced) {
        return geometry(mount,requested,advanced).centre();
    }
    public static Projection forCamera(int mount,int requested,boolean advanced,Point camera) {
        var origin=centre(mount,requested,advanced);
        int front=view(mount,requested);
        var frame=DisplayFacing.facing(front);
        double side=(camera.x-origin.x)*frame.normal().x()+(camera.y-origin.y)*frame.normal().y()+(camera.z-origin.z)*frame.normal().z();
        if(side<0)frame=DisplayFacing.facing(front^1);
        return new Projection(origin,frame);
    }
    private static Point modelPoint(int mount,int yaw,double x,double y,double z) {
        Point mounted=switch(mount) {
            case 0 -> new Point(x/16,y/16,z/16);
            case 1 -> new Point(x/16,1-y/16,1-z/16);
            case 2 -> new Point(1-x/16,z/16,y/16);
            case 3 -> new Point(x/16,z/16,1-y/16);
            case 4 -> new Point(y/16,z/16,x/16);
            case 5 -> new Point(1-y/16,z/16,1-x/16);
            default -> throw new IllegalArgumentException("Invalid mount");
        };
        double dx=mounted.x()-.5,dz=mounted.z()-.5;
        return switch(yaw) {
            case 0 -> mounted;
            case 90 -> new Point(.5+dz,mounted.y(),.5-dx);
            case 180 -> new Point(.5-dx,mounted.y(),.5-dz);
            case 270 -> new Point(.5-dz,mounted.y(),.5+dx);
            default -> throw new IllegalArgumentException("Invalid projector yaw");
        };
    }
    private static DisplayFacing.Vector offset(Point point) {
        return new DisplayFacing.Vector((int)Math.round((point.x()-.5)*16),
            (int)Math.round((point.y()-.5)*16),(int)Math.round((point.z()-.5)*16));
    }
    private static int dot(DisplayFacing.Vector a,DisplayFacing.Vector b) {
        return a.x()*b.x()+a.y()*b.y()+a.z()*b.z();
    }
    private HologramProjection() {}
}
