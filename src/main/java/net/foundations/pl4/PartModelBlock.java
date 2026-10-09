package net.foundations.pl4;

import net.minecraft.util.Direction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;

/** Model carrier. PlayerEntity placement is handled by PartItem into a multipart host. */
public final class PartModelBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FRONT_OUTWARD=BooleanProperty.create("front_outward");
    public static final BooleanProperty HAS_DISPLAY=BooleanProperty.create("has_display");
    public final Kind kind;
    public PartModelBlock(Kind kind, Properties properties) { super(properties); this.kind=kind; registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN).setValue(FRONT_OUTWARD,false).setValue(HAS_DISPLAY,false)); }
    @Override protected void createBlockStateDefinition(StateContainer.Builder<Block,BlockState> b) { b.add(FACING,FRONT_OUTWARD,HAS_DISPLAY); }
}
