package net.foundations.pl4;

import net.minecraft.util.Direction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;

/** A model-only carrier. Actual display parts continue to live in the persistent host. */
public final class LargeDisplayModelBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty FRONT_OUTWARD=BooleanProperty.create("front_outward");
    public static final IntegerProperty CONNECTIONS=IntegerProperty.create("connections",0,15);
    public LargeDisplayModelBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN).setValue(CONNECTIONS,0).setValue(FRONT_OUTWARD,false));}
    @Override protected void createBlockStateDefinition(StateContainer.Builder<Block,BlockState> b){b.add(FACING,CONNECTIONS,FRONT_OUTWARD);}
}
