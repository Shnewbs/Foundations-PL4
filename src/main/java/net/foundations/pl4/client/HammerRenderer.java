package net.foundations.pl4.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.foundations.pl4.*;

public final class HammerRenderer implements BlockEntityRenderer<HammerEntity> {
    private static final ResourceLocation TEXTURE=FoundationsPL4.id("textures/block/model/forging_hammer_stone.png");
    private final HammerModel model;
    private final ItemRenderer items;
    public HammerRenderer(BlockEntityRendererProvider.Context context){model=new HammerModel(context.bakeLayer(HammerModel.LAYER));items=context.getItemRenderer();}
    @Override public void render(HammerEntity hammer,float partial,PoseStack pose,MultiBufferSource buffer,int light,int overlay){
        pose.pushPose();
        pose.translate(.5,1.5,.5);
        pose.mulPose(Axis.YP.rotationDegrees(-hammer.getBlockState().getValue(HammerBlock.FACING).toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(180));
        model.render(pose,buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,overlay,
            hammer.animationFraction(partial),hammer.structureReady());
        pose.popPose();
        ItemStack stack=hammer.inventory.getStackInSlot(0);
        if(stack.isEmpty())stack=hammer.inventory.getStackInSlot(1);
        if(!stack.isEmpty()){
            pose.pushPose();pose.translate(.5,.89,.5);pose.scale(.5F,.5F,.5F);
            items.renderStatic(stack,ItemDisplayContext.GROUND,light,overlay,pose,buffer,hammer.getLevel(),hammer.getBlockPos().hashCode());
            pose.popPose();
        }
    }
    public AABB getRenderBoundingBox(HammerEntity hammer){return new AABB(hammer.getBlockPos()).expandTowards(0,2,0);}
    @Override public int getViewDistance(){return 64;}
}
