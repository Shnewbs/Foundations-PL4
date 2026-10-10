package net.foundations.pl4;

import net.minecraft.util.EnumFacing;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.DirectionProperty;

/** Non-item model carrier for a real PL2 connector mesh. Never placed in a world. */
public final class CableModelBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public CableModelBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(FACING, EnumFacing.DOWN)); }
    @Override protected void createBlockStateDefinition(StateContainer.Builder<Block, IBlockState> b) { b.add(FACING); }
}
