package net.foundations.pl4;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** Three-block ownership. No block above a hammer is ever replaced unless it is air or its own placeholder. */
public final class HammerStructure {
    private HammerStructure() {}
    public static boolean ownSpace(BlockState state,int offset) {
        return state.is(FoundationsPL4.HAMMER_SPACE.get())&&state.getValue(HammerSpaceBlock.OFFSET)==offset;
    }
    public static boolean canPlace(Level level,BlockPos base) {
        for(int i=1;i<=2;i++) {
            BlockPos p=base.above(i);
            if(level.isOutsideBuildHeight(p)||!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p)||!level.isEmptyBlock(p))return false;
        }
        return true;
    }
    public static boolean complete(LevelReader level,BlockPos base) {
        return level.hasChunkAt(base)&&ownSpace(level.getBlockState(base.above()),1)&&ownSpace(level.getBlockState(base.above(2)),2);
    }
    public static boolean ensure(Level level,BlockPos base) {
        if(level.isClientSide||!level.getBlockState(base).is(FoundationsPL4.HAMMER.get()))return false;
        // Preflight both positions before touching either. Also supports safe R3/R4 single-block upgrades.
        for(int i=1;i<=2;i++) {
            BlockPos p=base.above(i);
            if(level.isOutsideBuildHeight(p)||!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p))return false;
            BlockState s=level.getBlockState(p);
            if(!ownSpace(s,i)&&!s.isAir())return false;
        }
        for(int i=1;i<=2;i++) {
            BlockPos p=base.above(i);
            if(!ownSpace(level.getBlockState(p),i)) {
                if(!level.setBlock(p,FoundationsPL4.HAMMER_SPACE.get().defaultBlockState().setValue(HammerSpaceBlock.OFFSET,i),3))return false;
            }
        }
        return complete(level,base);
    }
    public static void removeOwnedSpaces(Level level,BlockPos base) {
        if(level.isClientSide)return;
        for(int i=1;i<=2;i++)if(ownSpace(level.getBlockState(base.above(i)),i))level.removeBlock(base.above(i),false);
    }
    public static boolean open(Level level,BlockPos base,Player player) {
        if(!level.hasChunkAt(base)||!level.mayInteract(player,base)||player.distanceToSqr(base.getCenter())>64)return false;
        if(!(level.getBlockEntity(base) instanceof HammerEntity hammer))return false;
        if(player instanceof ServerPlayer sp)sp.openMenu(hammer);
        return true;
    }
}
