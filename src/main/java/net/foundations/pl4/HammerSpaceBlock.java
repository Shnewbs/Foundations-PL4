package net.foundations.pl4;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResultType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.IntegerProperty;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.shapes.VoxelShape;

/** Upper two occupied cells. No inventory or loot of their own. */
public final class HammerSpaceBlock extends Block {
    public static final IntegerProperty OFFSET=IntegerProperty.create("offset",1,2);
    private static final VoxelShape POSTS=VoxelShapes.or(box(1,0,1,4,16,4),box(12,0,1,15,16,4),box(1,0,12,4,16,15),box(12,0,12,15,16,15));
    private static final VoxelShape TOP=VoxelShapes.or(POSTS,box(0,12,0,16,16,16));
    public HammerSpaceBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(OFFSET,1));}
    @Override public void createBlockStateDefinition(StateContainer.Builder<Block,BlockState> b){b.add(OFFSET);}
    @Override public BlockRenderType getRenderShape(BlockState s){return BlockRenderType.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState s,IBlockReader l,BlockPos p,ISelectionContext c){return s.getValue(OFFSET)==2?TOP:POSTS;}
    @Override public ActionResultType use(BlockState s,World l,BlockPos pos,PlayerEntity p,Hand hand,BlockRayTraceResult hit){return useWithoutItem(s,l,pos,p,hit);}
    public ActionResultType useWithoutItem(BlockState s,World l,BlockPos pos,PlayerEntity p,BlockRayTraceResult hit){
        return HammerStructure.open(l,pos.below(s.getValue(OFFSET)),p)?net.foundations.pl4.compat.PortInteractions.sidedSuccess(l.isClientSide):ActionResultType.PASS;
    }
    @Override public boolean removedByPlayer(BlockState s,World l,BlockPos p,PlayerEntity player,boolean willHarvest,FluidState fluid){
        BlockPos base=p.below(s.getValue(OFFSET));
        if(!l.isClientSide&&(l.getBlockState(base).getBlock()==FoundationsPL4.HAMMER.get())) {
            if(player!=null&&!l.mayInteract(player,base))return false;
            l.destroyBlock(base,player==null||!player.abilities.instabuild,player);
        }
        return l.isClientSide?l.setBlock(p,fluid.createLegacyBlock(),11):l.removeBlock(p,false)||l.getBlockState(p).isAir();
    }
    @Override public void onRemove(BlockState s,World l,BlockPos p,BlockState next,boolean moving){
        super.onRemove(s,l,p,next,moving);
        if(!(s.getBlock()==next.getBlock())&&!l.isClientSide) {
            BlockPos base=p.below(s.getValue(OFFSET));
            if((l.getBlockState(base).getBlock()==FoundationsPL4.HAMMER.get()))l.destroyBlock(base,true);
        }
    }
    @Override public ItemStack getPickBlock(BlockState s,RayTraceResult target,IBlockReader l,BlockPos p,PlayerEntity player){return new ItemStack(FoundationsPL4.HAMMER.get());}
    @Override public net.minecraft.block.material.PushReaction getPistonPushReaction(BlockState state){return net.minecraft.block.material.PushReaction.BLOCK;}
}
