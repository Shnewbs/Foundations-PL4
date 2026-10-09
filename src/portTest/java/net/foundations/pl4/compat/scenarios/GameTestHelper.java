package net.foundations.pl4.compat.scenarios;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
/** Fixture semantics on a real isolated server; not Mojang's modern GameTest API. */
public final class GameTestHelper {
 final ServerLevel level;final BlockPos origin;final java.util.TreeMap<Long,java.util.List<Runnable>> scheduled=new java.util.TreeMap<>();
 boolean passed;long tick;
 public GameTestHelper(ServerLevel level,BlockPos origin){this.level=level;this.origin=origin;}
 public ServerLevel getLevel(){return level;}
 public BlockPos absolutePos(BlockPos relative){return origin.offset(relative);}
 public void setBlock(BlockPos pos,Block block){setBlock(pos,block.defaultBlockState());}
 public void setBlock(BlockPos pos,BlockState state){BlockPos target=absolutePos(pos);if(!level.setBlock(target,state,3)&&level.getBlockState(target)!=state)throw new AssertionError("Unable to set fixture block "+pos);}
 public BlockEntity getBlockEntity(BlockPos pos){return level.getBlockEntity(absolutePos(pos));}
 public void runAtTickTime(long time,Runnable action){if(time<tick)throw new IllegalArgumentException("Callback scheduled in the past");scheduled.computeIfAbsent(time,k->new java.util.ArrayList<>()).add(action);}
 public void succeed(){passed=true;}
 void advance(){while(!scheduled.isEmpty()&&scheduled.firstKey()<=tick){var work=scheduled.pollFirstEntry().getValue();for(var action:work)action.run();}tick++;}
}
