package net.foundations.pl4;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class HammerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape BASE=Shapes.or(box(0,8,0,16,12,16),box(4,12,4,12,14,12),
        box(0,0,0,16,2,16),box(1,0,1,4,16,4),box(12,0,1,15,16,4),box(1,0,12,4,16,15),box(12,0,12,15,16,15));
    public HammerBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(HammerBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        if(!HammerStructure.canPlace(c.getLevel(),c.getClickedPos()))return null;
        Player player=c.getPlayer();
        if(player!=null)for(int i=1;i<=2;i++)if(!c.getLevel().mayInteract(player,c.getClickedPos().above(i))
            ||!player.mayUseItemAt(c.getClickedPos().above(i),Direction.UP,c.getItemInHand()))return null;
        return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());
    }
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return BASE;}
    @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){
        super.onPlace(s,l,p,old,moving);if(!old.is(this)&&!l.isClientSide())HammerStructure.ensure(l,p);
    }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new HammerEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){
        return l.isClientSide()?null:createTickerHelper(type,FoundationsPL4.HAMMER_ENTITY.get(),HammerEntity::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        return HammerStructure.open(l,pos,p)?ItemInteractionResult.sidedSuccess(l.isClientSide()):ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
        return HammerStructure.open(l,pos,p)?InteractionResult.sidedSuccess(l.isClientSide()):InteractionResult.PASS;
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos pos,BlockState next,boolean moving){
        if(!s.is(next.getBlock())&&!l.isClientSide()&&l.getBlockEntity(pos) instanceof HammerEntity h) {
            // Clear before dropping so callbacks cannot observe the same inventory twice.
            for(int i=0;i<h.inventory.getSlots();i++) {
                ItemStack stack=h.inventory.getStackInSlot(i).copy();h.inventory.setStackInSlot(i,ItemStack.EMPTY);
                if(!stack.isEmpty())popResource(l,pos,stack);
            }
        }
        super.onRemove(s,l,pos,next,moving);
        if(!s.is(next.getBlock()))HammerStructure.removeOwnedSpaces(l,pos);
    }
}
