package net.foundations.pl4;


import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumActionResult;
import net.minecraft.entity.player.EntityPlayer;
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
import net.minecraft.block.state.IBlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.DirectionProperty;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.shapes.VoxelShape;

public final class HammerBlock extends ContainerBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape BASE=VoxelShapes.or(box(0,8,0,16,12,16),box(4,12,4,12,14,12),
        box(0,0,0,16,2,16),box(1,0,1,4,16,4),box(12,0,1,15,16,4),box(1,0,12,4,16,15),box(12,0,12,15,16,15));
    public HammerBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,EnumFacing.NORTH));}
    @Override public void createBlockStateDefinition(StateContainer.Builder<Block,IBlockState> b){b.add(FACING);}
    @Override public IBlockState getStateForPlacement(BlockItemUseContext c){
        if(!HammerStructure.canPlace(c.getLevel(),c.getClickedPos()))return null;
        EntityPlayer player=c.getPlayer();
        if(player!=null)for(int i=1;i<=2;i++)if(!c.getLevel().mayInteract(player,c.getClickedPos().above(i))
            ||!player.mayUseItemAt(c.getClickedPos().above(i),EnumFacing.UP,c.getItemInHand()))return null;
        return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());
    }
    @Override public IBlockState rotate(IBlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override public IBlockState mirror(IBlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override public BlockRenderType getRenderShape(IBlockState s){return BlockRenderType.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(IBlockState s,IBlockReader l,BlockPos p,ISelectionContext c){return BASE;}
    @Override public void onPlace(IBlockState s,World l,BlockPos p,IBlockState old,boolean moving){
        super.onPlace(s,l,p,old,moving);if(!(old.getBlock()==this)&&!l.isClientSide)HammerStructure.ensure(l,p);
    }
    @Override public TileEntity newBlockEntity(IBlockReader world){return new HammerEntity();}

    @Override public boolean use(IBlockState s,World l,BlockPos pos,EntityPlayer p,EnumHand hand,RayTraceResult hit){return useWithoutItem(s,l,pos,p,hit)!=EnumActionResult.PASS;}
    public EnumActionResult useWithoutItem(IBlockState s,World l,BlockPos pos,EntityPlayer p,RayTraceResult hit){
        return HammerStructure.open(l,pos,p)?net.foundations.pl4.compat.PortInteractions.sidedSuccess(l.isClientSide):EnumActionResult.PASS;
    }
    @Override public void onRemove(IBlockState s,World l,BlockPos pos,IBlockState next,boolean moving){
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
    @Override public net.minecraft.block.material.PushReaction getPistonPushReaction(IBlockState state){return net.minecraft.block.material.PushReaction.BLOCK;}
}
