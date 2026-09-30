package net.foundations.pl4;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Upper two occupied cells. No inventory or loot of their own. */
public final class HammerSpaceBlock extends Block {
    public static final IntegerProperty OFFSET=IntegerProperty.create("offset",1,2);
    private static final VoxelShape POSTS=Shapes.or(box(1,0,1,4,16,4),box(12,0,1,15,16,4),box(1,0,12,4,16,15),box(12,0,12,15,16,15));
    private static final VoxelShape TOP=Shapes.or(POSTS,box(0,12,0,16,16,16));
    public HammerSpaceBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(OFFSET,1));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(OFFSET);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(OFFSET)==2?TOP:POSTS;}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        return HammerStructure.open(l,pos.below(s.getValue(OFFSET)),p)?ItemInteractionResult.sidedSuccess(l.isClientSide):ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
        return HammerStructure.open(l,pos.below(s.getValue(OFFSET)),p)?InteractionResult.sidedSuccess(l.isClientSide):InteractionResult.PASS;
    }
    @Override public boolean onDestroyedByPlayer(BlockState s,Level l,BlockPos p,Player player,boolean willHarvest,FluidState fluid){
        BlockPos base=p.below(s.getValue(OFFSET));
        if(!l.isClientSide&&l.getBlockState(base).is(FoundationsPL4.HAMMER.get())) {
            if(player!=null&&!l.mayInteract(player,base))return false;
            l.destroyBlock(base,player==null||!player.getAbilities().instabuild,player);
        }
        return l.isClientSide?l.setBlock(p,fluid.createLegacyBlock(),11):l.removeBlock(p,false)||l.getBlockState(p).isAir();
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        super.onRemove(s,l,p,next,moving);
        if(!s.is(next.getBlock())&&!l.isClientSide) {
            BlockPos base=p.below(s.getValue(OFFSET));
            if(l.getBlockState(base).is(FoundationsPL4.HAMMER.get()))l.destroyBlock(base,true);
        }
    }
    @Override public ItemStack getCloneItemStack(BlockState s,HitResult target,LevelReader l,BlockPos p,Player player){return new ItemStack(FoundationsPL4.HAMMER.get());}
}
