package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.*;

/** Paired reader/display geometry and actual external endpoint leads; no world lookup in rendering. */
public final class MultipartShapes {
    private static final VoxelShape[] READER_WITH_DISPLAY=new VoxelShape[6];
    private static final EnumMap<Kind,VoxelShape[]> LEADS=new EnumMap<>(Kind.class);
    static {
        for(Direction face:Direction.values())READER_WITH_DISPLAY[face.ordinal()]=Shapes.or(
            PartShapes.rotate(new double[]{5.5,1,5.5,10.5,6,10.5},face),
            PartShapes.rotate(new double[]{5,1,5,11,5,11},face)).optimize();
        for(Kind kind:Kind.values())if(!kind.cable()){
            VoxelShape[] values=new VoxelShape[6];
            for(Direction face:Direction.values())values[face.ordinal()]=PartShapes.rotate(
                new double[]{7,leadStart(kind),7,9,16,9},face);
            LEADS.put(kind,values);
        }
    }
    public static double leadStart(Kind kind){
        if(kind.reader())return 6;
        return switch(kind){
            case NODE->1.5;case TRANSFER_NODE,REDSTONE_NODE->2;case CLOCK->3;case SIGNALLER->6;
            case ARRAY,ENTITY_NODE,ADVANCED_HOLOGRAM,DATA_EMITTER,DATA_RECEIVER,REDSTONE_EMITTER,REDSTONE_RECEIVER->4;
            default->1;
        };
    }
    public static String leadKey(Kind kind){return leadStart(kind)==1.5?"15":Integer.toString((int)leadStart(kind));}
    public static boolean paired(Collection<Part> parts,Part reader){
        return reader.kind.reader()&&parts.stream().anyMatch(p->p.kind.panelDisplay()&&p.face==reader.face);
    }
    public static VoxelShape part(Collection<Part> parts,Part part){
        var shape=paired(parts,part)?READER_WITH_DISPLAY[part.face.ordinal()]:PartShapes.part(part);
        if(part.hologram()){
            int turns=net.foundations.pl4.core.HologramProjection.baseYaw(part.face.ordinal(),part.hologramView)/90;
            for(int i=0;i<turns;i++){
                var rotated=Shapes.empty();
                for(var box:shape.toAabbs())rotated=Shapes.or(rotated,Shapes.box(box.minZ,box.minY,1-box.maxX,box.maxZ,box.maxY,1-box.minX));
                shape=rotated.optimize();
            }
        }
        return shape;
    }
    public static VoxelShape lead(Part part){return part.kind.cable()?Shapes.empty():LEADS.get(part.kind)[part.face.ordinal()];}
    private MultipartShapes(){}
}
