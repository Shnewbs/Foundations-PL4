package net.foundations.pl4;

import java.util.EnumMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.*;
import net.foundations.pl4.core.ConnectionRules;

/** Static model-matched part shapes, generated from the retained PL2 JSON elements. */
public final class PartShapes {
    private static final EnumMap<Kind,VoxelShape[]> SHAPES = new EnumMap<>(Kind.class);
    public static final VoxelShape CENTRE = Block.box(6,6,6,10,10,10);
    private static final VoxelShape[][] ARMS = new VoxelShape[4][6];
    static {
        add(Kind.DATA_CABLE, new double[][]{{6,6,6,10,10,10}});
        add(Kind.REDSTONE_CABLE, new double[][]{{6,6,6,10,10,10}});
        add(Kind.NODE, new double[][]{{6,0,6,10,1.5,10},{2.5,0.5,8.5,13.5,1,9.5},{2.5,0.5,6.5,13.5,1,7.5},{6.5,0.5,2.5,7.5,1,13.5},{8.5,0.5,2.5,9.5,1,13.5},{1.5,0,6,2.5,1.5,10},{13.5,0,6,14.5,1.5,10},{6,0,13.5,10,1.5,14.5},{6,0,1.5,10,1.5,2.5}});
        add(Kind.INFO_READER, new double[][]{{5.5,0,5.5,10.5,6,10.5},{5,1,5,11,5,11}});
        add(Kind.INVENTORY_READER, new double[][]{{5.5,0,5.5,10.5,6,10.5},{5,1,5,11,5,11}});
        add(Kind.FLUID_READER, new double[][]{{5.5,0,5.5,10.5,6,10.5},{5,1,5,11,5,11}});
        add(Kind.ENERGY_READER, new double[][]{{5.5,0,5.5,10.5,6,10.5},{5,1,5,11,5,11}});
        add(Kind.NETWORK_READER, new double[][]{{5.5,0,5.5,10.5,6,10.5},{5,1,5,11,5,11}});
        add(Kind.ARRAY, new double[][]{{5,3,5,11,4,11},{3,1,3,13,3,13}});
        add(Kind.ENTITY_NODE, new double[][]{{5.5,2,5.5,10.5,4,10.5}});
        add(Kind.TRANSFER_NODE, new double[][]{{4,0,4,12,1,12},{5,1,5,11,2,11}});
        add(Kind.REDSTONE_NODE, new double[][]{{4,0,4,12,1,12},{5,1,5,11,2,11}});
        add(Kind.CLOCK, new double[][]{{7,0,7,9,1,9},{6,1,6,10,3,10}});
        add(Kind.SIGNALLER, new double[][]{{6.5,4.5,6.5,9.5,5.5,9.5},{7,1,7,9,6,9}});
        add(Kind.DISPLAY, new double[][]{{0,0,4,16,1,12}});
        add(Kind.MINI_DISPLAY, new double[][]{{4,0,4,12,1,12}});
        add(Kind.LARGE_DISPLAY, new double[][]{{0,0,0,16,1,16}});
        add(Kind.HOLOGRAM, new double[][]{{5.5,0,5.5,10.5,1,10},{0,0,10,16,1,11}});
        add(Kind.ADVANCED_HOLOGRAM, new double[][]{{5,3,5,11,4,11},{5,1,5,5.001,3,11},{11,1,5,11.001,3,11},{5,1,5,11,3,5.001},{5,1,11,11,3,11.001}});
        add(Kind.DATA_EMITTER, new double[][]{{6,0.5,6,10,1,10},{5.5,1,5.5,10.5,2.5,10.5},{6,2.5,6,10,3,10},{7.5,3,6.5,8.5,4,7},{7.5,3,9,8.5,4,9.5},{6.5,3,7.5,7,4,8.5},{9,3,7.5,9.5,4,8.5}});
        add(Kind.DATA_RECEIVER, new double[][]{{6,0.5,6,10,1,10},{5.5,1,5.5,10.5,2.5,10.5},{6,2.5,6,10,3,10},{7.5,3,6.5,8.5,4,7},{7.5,3,9,8.5,4,9.5},{6.5,3,7.5,7,4,8.5},{9,3,7.5,9.5,4,8.5}});
        add(Kind.REDSTONE_EMITTER, new double[][]{{6,0.5,6,10,1,10},{5.5,1,5.5,10.5,2.5,10.5},{6,2.5,6,10,3,10},{7.5,3,6.5,8.5,4,7},{7.5,3,9,8.5,4,9.5},{6.5,3,7.5,7,4,8.5},{9,3,7.5,9.5,4,8.5}});
        add(Kind.REDSTONE_RECEIVER, new double[][]{{6,0.5,6,10,1,10},{5.5,1,5.5,10.5,2.5,10.5},{6,2.5,6,10,3,10},{7.5,3,6.5,8.5,4,7},{7.5,3,9,8.5,4,9.5},{6.5,3,7.5,7,4,8.5},{9,3,7.5,9.5,4,8.5}});
        for (int type=1; type<=3; type++) for (Direction face:Direction.values())
                ARMS[type][face.ordinal()]=rotate(new double[]{7,ConnectionRules.armStart(type),7,9,6,9},face);
        }
        private PartShapes() {}
        private static void add(Kind kind,double[][] boxes) {
            VoxelShape[] directions=new VoxelShape[6];
            for(Direction face:Direction.values()) {
                VoxelShape shape=Shapes.empty();
                for(double[] box:boxes) shape=Shapes.or(shape,rotate(box,face));
                directions[face.ordinal()]=shape.optimize();
            }
            SHAPES.put(kind,directions);
        }
        public static VoxelShape part(Part part) { return SHAPES.get(part.kind)[part.face.ordinal()]; }
        public static VoxelShape arm(int type,Direction face) { return type<1||type>3?Shapes.empty():ARMS[type][face.ordinal()]; }
        static VoxelShape rotate(double[] b,Direction face) {
            double[] lo={16,16,16},hi={0,0,0};
            for(int a=0;a<8;a++) {
                double x=b[(a&1)==0?0:3],y=b[(a&2)==0?1:4],z=b[(a&4)==0?2:5];
                double[] v=switch(face) {
                    case DOWN->new double[]{x,y,z}; case UP->new double[]{x,16-y,16-z};
                    case NORTH->new double[]{16-x,z,y}; case SOUTH->new double[]{x,z,16-y};
                    case WEST->new double[]{y,z,x}; case EAST->new double[]{16-y,z,16-x};
                };
                for(int i=0;i<3;i++){lo[i]=Math.min(lo[i],v[i]);hi[i]=Math.max(hi[i],v[i]);}
            }
            return Block.box(lo[0],lo[1],lo[2],hi[0],hi[1],hi[2]);
        }
    }
