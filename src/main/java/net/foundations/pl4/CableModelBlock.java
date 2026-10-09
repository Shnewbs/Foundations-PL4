package net.foundations.pl4;

import net.minecraft.util.Direction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.DirectionProperty;

/** Non-item model carrier for a real PL2 connector mesh. Never placed in a world. */
public final class CableModelBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public CableModelBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN)); }
    @Override protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> b) { b.add(FACING); }
}
