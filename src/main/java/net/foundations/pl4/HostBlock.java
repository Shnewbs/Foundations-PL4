package net.foundations.pl4;


import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumActionResult;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.ContainerBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.shapes.VoxelShape;

public final class HostBlock extends ContainerBlock {
    public HostBlock(Properties p) { super(p); }
    @Override public BlockRenderType getRenderShape(IBlockState s){return BlockRenderType.INVISIBLE;}
    @Override public TileEntity newBlockEntity(IBlockReader world){return new HostEntity();}
    @Override public VoxelShape getShape(IBlockState s,IBlockReader l,BlockPos p,ISelectionContext c){
        return l.getBlockEntity(p) instanceof HostEntity h ? h.outline() : PartShapes.CENTRE;
    }
    @Override public VoxelShape getCollisionShape(IBlockState s,IBlockReader l,BlockPos p,ISelectionContext c) { return getShape(s,l,p,c); }
    @Override public VoxelShape getOcclusionShape(IBlockState s,IBlockReader l,BlockPos p) { return VoxelShapes.empty(); }
    public static VoxelShape shape(Part p) { return PartShapes.part(p); }
    public static boolean canAdd(HostEntity host,Part candidate) {
        if(host.parts.containsKey(candidate.slot()))return false;
        var prospective=new java.util.ArrayList<>(host.parts.values());prospective.add(candidate);
        VoxelShape shape=MultipartShapes.part(prospective,candidate);
        for(Part other:host.parts.values())if(VoxelShapes.joinIsNotEmpty(shape,MultipartShapes.part(prospective,other),IBooleanFunction.AND))return false;
        return true;
    }
    @Override public boolean use(IBlockState s,World l,BlockPos p,EntityPlayer player,EnumHand hand,RayTraceResult hit){
        ItemStack held=player.getItemInHand(hand);
        if(held.getItem() instanceof PartItem||held.getItem() instanceof ToolItem)return false;
        return useWithoutItem(s,l,p,player,hit)!=EnumActionResult.PASS;
    }
    public EnumActionResult useWithoutItem(IBlockState s,World l,BlockPos p,EntityPlayer player,RayTraceResult hit){
        if(l.getBlockEntity(p) instanceof HostEntity h){ Part part=h.interactionTarget(hit,player.isSneaking()); if(part!=null){if(player instanceof EntityPlayerMP sp&&!DisplayActions.activate(sp,h,part))PLPackets.open(sp,h,part); return net.foundations.pl4.compat.PortInteractions.sidedSuccess(l.isClientSide);}}
        return EnumActionResult.PASS;
    }
    @Override public void onRemove(IBlockState s,World l,BlockPos p,IBlockState next,boolean moving){
        if(s.getBlock()!=next.getBlock() && l.getBlockEntity(p) instanceof HostEntity h && !l.isClientSide){
            for(Part part:h.parts.values())popResource(l,p,PartItem.stack(part,null));
            NetworkEngine.invalidate(l); l.updateNeighborsAt(p,this);
        }
        super.onRemove(s,l,p,next,moving);
    }
    @Override public boolean isSignalSource(IBlockState s){return true;}
    @Override public int getSignal(IBlockState s,IBlockReader l,BlockPos p,EnumFacing side){return l.getBlockEntity(p) instanceof HostEntity h?h.output(side):0;}
    @Override public int getDirectSignal(IBlockState s,IBlockReader l,BlockPos p,EnumFacing side){return getSignal(s,l,p,side);}
    @Override public ItemStack getPickBlock(IBlockState s,RayTraceResult target,IBlockReader l,BlockPos p,EntityPlayer player){
        if(target instanceof RayTraceResult hit && l.getBlockEntity(p) instanceof HostEntity h){Part part=h.hit(hit);if(part!=null)return new ItemStack(FoundationsPL4.PART_ITEMS.get(part.kind).get());}return ItemStack.EMPTY;
    }
}
