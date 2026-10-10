package net.foundations.pl4.core;

/** One front-facing coordinate convention shared by screen layout and the renderer.
 * EnumFacing indices are Minecraft's DOWN, UP, NORTH, SOUTH, WEST, EAST.
 * The slot remains fixed; changing the front never moves the attachment or cable port.
 */
public final class DisplayFacing {
    public record Vector(int x,int y,int z) {
        public Vector negate(){return new Vector(-x,-y,-z);}
        public int dot(Vector other){return x*other.x+y*other.y+z*other.z;}
        public Vector cross(Vector other){return new Vector(y*other.z-z*other.y,z*other.x-x*other.z,x*other.y-y*other.x);}
    }
    public record Frame(Vector right,Vector up,Vector normal,int rotationX,int rotationY) {}
    private static final Vector[] DIRECTIONS={new Vector(0,-1,0),new Vector(0,1,0),new Vector(0,0,-1),new Vector(0,0,1),new Vector(-1,0,0),new Vector(1,0,0)};
    public static int front(int mount,boolean outward){if(mount<0||mount>=6)throw new IllegalArgumentException("Bad display face");return outward?mount:mount^1;}
    public static Frame frame(int mount,boolean outward){return facing(front(mount,outward));}
    public static Frame facing(int front){
        return switch(front){
            case 0 -> new Frame(DIRECTIONS[5],DIRECTIONS[3],DIRECTIONS[0],90,0);
            case 1 -> new Frame(DIRECTIONS[5],DIRECTIONS[2],DIRECTIONS[1],-90,0);
            case 2 -> new Frame(DIRECTIONS[4],DIRECTIONS[1],DIRECTIONS[2],0,180);
            case 3 -> new Frame(DIRECTIONS[5],DIRECTIONS[1],DIRECTIONS[3],0,0);
            case 4 -> new Frame(DIRECTIONS[3],DIRECTIONS[1],DIRECTIONS[4],0,-90);
            case 5 -> new Frame(DIRECTIONS[2],DIRECTIONS[1],DIRECTIONS[5],0,90);
            default -> throw new IllegalArgumentException("Bad display front");
        };
    }
    public static int direction(Vector v){for(int i=0;i<6;i++)if(DIRECTIONS[i].equals(v))return i;throw new IllegalArgumentException("Not an axis");}
    /** Thin-screen outward skin is at -0.001/16; inner skin is at 1/16.
     * Offset by 0.0005 blocks to keep glyphs in front, never inside the screen mesh. */
    public static double planeOffset(boolean outward){return outward?0.5005625:0.4370;}
    private DisplayFacing(){}
}
