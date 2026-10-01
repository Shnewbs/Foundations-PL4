package net.foundations.pl4.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.foundations.pl4.*;

public final class HammerRenderer implements BlockEntityRenderer<HammerEntity,HammerRenderer.State> {
    private static final Identifier TEXTURE=FoundationsPL4.id("textures/block/model/forging_hammer_stone.png");
    private final HammerModel model;
    public static final class State extends BlockEntityRenderState {
        float yaw;double progress;boolean assembled;
        final ItemStackRenderState item=new ItemStackRenderState();
    }
    public HammerRenderer(BlockEntityRendererProvider.Context context){model=new HammerModel(context.bakeLayer(HammerModel.LAYER));}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(HammerEntity hammer,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderer.super.extractRenderState(hammer,state,partial,camera,overlay);
        state.yaw=-hammer.getBlockState().getValue(HammerBlock.FACING).toYRot();
        state.progress=hammer.animationFraction(partial);state.assembled=hammer.structureReady();
        ItemStack stack=hammer.inventory.getStackInSlot(0);if(stack.isEmpty())stack=hammer.inventory.getStackInSlot(1);
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.item,stack,ItemDisplayContext.GROUND,hammer.getLevel(),null,hammer.getBlockPos().hashCode());
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        pose.pushPose();pose.translate(.5,1.5,.5);
        pose.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(state.yaw)));
        pose.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(180)));
        double progress=state.progress;boolean assembled=state.assembled;int light=state.lightCoords;
        collector.submitCustomGeometry(pose,RenderTypes.entityCutout(TEXTURE),(transform,vertices)->{
            var drawPose=new PoseStack();drawPose.mulPose(transform.pose());
            model.render(drawPose,vertices,light,OverlayTexture.NO_OVERLAY,progress,assembled);
        });
        pose.popPose();
        if(!state.item.isEmpty()){
            pose.pushPose();pose.translate(.5,.89,.5);pose.scale(.5F,.5F,.5F);
            state.item.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        }
    }
    public AABB getRenderBoundingBox(HammerEntity hammer){return new AABB(hammer.getBlockPos()).expandTowards(0,2,0);}
    @Override public int getViewDistance(){return 64;}
}
