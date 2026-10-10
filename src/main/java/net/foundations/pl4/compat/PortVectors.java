package net.foundations.pl4.compat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
public final class PortVectors {
 public static Vec3d atCenterOf(BlockPos pos){return new Vec3d(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);}
 public static Vec3d atLowerCornerOf(BlockPos pos){return new Vec3d(pos.getX(),pos.getY(),pos.getZ());}
 private PortVectors(){}
}
