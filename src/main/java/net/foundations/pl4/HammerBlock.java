package net.foundations.pl4;


import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResultType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.ContainerBlock;
import net.minecraft.block.Block;
import net.minecraft.util.Mirror;
import net.minecraft.block.BlockRenderType;
import net.minecraft.util.Rotation;
import net.minecraft.tileentity.TileEntity;

import net.minecraft.tileentity.TileEntityType;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.DirectionProperty;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.shapes.VoxelShape;

public final class HammerBlock extends ContainerBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape BASE=VoxelShapes.or(box(0,8,0,16,12,16),box(4,12,4,12,14,12),
        box(0,0,0,16,2,16),box(1,0,1,4,16,4),box(12,0,1,15,16,4),box(1,0,12,4,16,15),box(12,0,12,15,16,15));
    public HammerBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override public void createBlockStateDefinition(StateContainer.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockItemUseContext c){
        if(!HammerStructure.canPlace(c.getLevel(),c.getClickedPos()))return null;
        PlayerEntity player=c.getPlayer();
        if(player!=null)for(int i=1;i<=2;i++)if(!c.getLevel().mayInteract(player,c.getClickedPos().above(i))
            ||!player.mayUseItemAt(c.getClickedPos().above(i),Direction.UP,c.getItemInHand()))return null;
        return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override public BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override public BlockRenderType getRenderShape(BlockState s){return BlockRenderType.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState s,IBlockReader l,BlockPos p,ISelectionContext c){return BASE;}
    @Override public void onPlace(BlockState s,World l,BlockPos p,BlockState old,boolean moving){
        super.onPlace(s,l,p,old,moving);if(!(old.getBlock()==this)&&!l.isClientSide)HammerStructure.ensure(l,p);
    }
    @Override public TileEntity newBlockEntity(IBlockReader world){return new HammerEntity();}

    @Override public boolean use(BlockState s,World l,BlockPos pos,PlayerEntity p,Hand hand,BlockRayTraceResult hit){return useWithoutItem(s,l,pos,p,hit)!=ActionResultType.PASS;}
    public ActionResultType useWithoutItem(BlockState s,World l,BlockPos pos,PlayerEntity p,BlockRayTraceResult hit){
        return HammerStructure.open(l,pos,p)?net.foundations.pl4.compat.PortInteractions.sidedSuccess(l.isClientSide):ActionResultType.PASS;
    }
    @Override public void onRemove(BlockState s,World l,BlockPos pos,BlockState next,boolean moving){
        if(!(s.getBlock()==next.getBlock())&&!l.isClientSide&&l.getBlockEntity(pos) instanceof HammerEntity h) {
            // Clear before dropping so callbacks cannot observe the same inventory twice.
            for(int i=0;i<h.inventory.getSlots();i++) {
                ItemStack stack=h.inventory.getStackInSlot(i).copy();h.inventory.setStackInSlot(i,ItemStack.EMPTY);
                if(!stack.isEmpty())popResource(l,pos,stack);
            }
        }
        super.onRemove(s,l,pos,next,moving);
        if(!(s.getBlock()==next.getBlock()))HammerStructure.removeOwnedSpaces(l,pos);
    }
    @Override public net.minecraft.block.material.PushReaction getPistonPushReaction(BlockState state){return net.minecraft.block.material.PushReaction.BLOCK;}
}
