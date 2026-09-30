package net.foundations.pl4;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** Model carrier. Player placement is handled by PartItem into a multipart host. */
public final class PartModelBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FRONT_OUTWARD=BooleanProperty.create("front_outward");
    public static final BooleanProperty HAS_DISPLAY=BooleanProperty.create("has_display");
    public final Kind kind;
    public PartModelBlock(Kind kind, Properties properties) { super(properties); this.kind=kind; registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN).setValue(FRONT_OUTWARD,false).setValue(HAS_DISPLAY,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING,FRONT_OUTWARD,HAS_DISPLAY); }
}
