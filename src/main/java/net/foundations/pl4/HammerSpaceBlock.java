package net.foundations.pl4;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumActionResult;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.IntegerProperty;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.math.RayTraceResult;
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
    @Override public void createBlockStateDefinition(StateContainer.Builder<Block,IBlockState> b){b.add(OFFSET);}
    @Override public BlockRenderType getRenderShape(IBlockState s){return BlockRenderType.INVISIBLE;}
    @Override public VoxelShape getShape(IBlockState s,IBlockReader l,BlockPos p,ISelectionContext c){return s.getValue(OFFSET)==2?TOP:POSTS;}
    @Override public boolean use(IBlockState s,World l,BlockPos pos,EntityPlayer p,EnumHand hand,RayTraceResult hit){return useWithoutItem(s,l,pos,p,hit)!=EnumActionResult.PASS;}
    public EnumActionResult useWithoutItem(IBlockState s,World l,BlockPos pos,EntityPlayer p,RayTraceResult hit){
        return HammerStructure.open(l,pos.below(s.getValue(OFFSET)),p)?net.foundations.pl4.compat.PortInteractions.sidedSuccess(l.isClientSide):EnumActionResult.PASS;
    }
    @Override public boolean removedByPlayer(IBlockState s,World l,BlockPos p,EntityPlayer player,boolean willHarvest,IFluidState fluid){
        BlockPos base=p.below(s.getValue(OFFSET));
        if(!l.isClientSide&&(l.getBlockState(base).getBlock()==FoundationsPL4.HAMMER.get())) {
            if(player!=null&&!l.mayInteract(player,base))return false;
            l.destroyBlock(base,player==null||!player.abilities.instabuild);
        }
        return l.isClientSide?l.setBlock(p,fluid.createLegacyBlock(),11):l.removeBlock(p,false)||l.getBlockState(p).isAir();
    }
    @Override public void onRemove(IBlockState s,World l,BlockPos p,IBlockState next,boolean moving){
        super.onRemove(s,l,p,next,moving);
        if(!(s.getBlock()==next.getBlock())&&!l.isClientSide) {
            BlockPos base=p.below(s.getValue(OFFSET));
            if((l.getBlockState(base).getBlock()==FoundationsPL4.HAMMER.get()))l.destroyBlock(base,true);
        }
    }
    @Override public ItemStack getPickBlock(IBlockState s,RayTraceResult target,IBlockReader l,BlockPos p,EntityPlayer player){return new ItemStack(FoundationsPL4.HAMMER.get());}
    @Override public net.minecraft.block.material.PushReaction getPistonPushReaction(IBlockState state){return net.minecraft.block.material.PushReaction.BLOCK;}
}
