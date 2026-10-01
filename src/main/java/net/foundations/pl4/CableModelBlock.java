package net.foundations.pl4;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Non-item model carrier for a real PL2 connector mesh. Never placed in a world. */
public final class CableModelBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public CableModelBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
}
