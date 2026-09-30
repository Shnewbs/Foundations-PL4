package net.foundations.pl4;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class HostBlock extends BaseEntityBlock {
    public static final MapCodec<HostBlock> CODEC=simpleCodec(HostBlock::new);
    public HostBlock(Properties p) { super(p); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new HostEntity(p,s);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return l.getBlockEntity(p) instanceof HostEntity h ? h.outline() : PartShapes.CENTRE;
    }
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return getShape(s,l,p,c); }
    @Override protected VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p) { return Shapes.empty(); }
    public static VoxelShape shape(Part p) { return PartShapes.part(p); }
    public static boolean canAdd(HostEntity host,Part candidate) {
        if(host.parts.containsKey(candidate.slot()))return false;
        var prospective=new java.util.ArrayList<>(host.parts.values());prospective.add(candidate);
        VoxelShape shape=MultipartShapes.part(prospective,candidate);
        for(Part other:host.parts.values())if(Shapes.joinIsNotEmpty(shape,MultipartShapes.part(prospective,other),BooleanOp.AND))return false;
        return true;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        // Otherwise the default useWithoutItem consumes the click by opening a GUI before PartItem/Operator can run.
        return stack.getItem() instanceof PartItem||stack.getItem() instanceof ToolItem
            ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(l.getBlockEntity(p) instanceof HostEntity h){ Part part=h.interactionTarget(hit,player.isShiftKeyDown()); if(part!=null){if(player instanceof ServerPlayer sp)PLPackets.open(sp,h,part); return InteractionResult.sidedSuccess(l.isClientSide);}}
        return InteractionResult.PASS;
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(s.getBlock()!=next.getBlock() && l.getBlockEntity(p) instanceof HostEntity h && !l.isClientSide){
            for(Part part:h.parts.values())popResource(l,p,PartItem.stack(part,l.registryAccess()));
            NetworkEngine.invalidate(l); l.updateNeighborsAt(p,this);
        }
        super.onRemove(s,l,p,next,moving);
    }
    @Override protected boolean isSignalSource(BlockState s){return true;}
    @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction side){return l.getBlockEntity(p) instanceof HostEntity h?h.output(side):0;}
    @Override protected int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction side){return getSignal(s,l,p,side);}
    @Override public ItemStack getCloneItemStack(BlockState s,HitResult target,LevelReader l,BlockPos p,Player player){
        if(target instanceof BlockHitResult hit && l.getBlockEntity(p) instanceof HostEntity h){Part part=h.hit(hit);if(part!=null)return new ItemStack(FoundationsPL4.PART_ITEMS.get(part.kind).get());}return ItemStack.EMPTY;
    }
}
