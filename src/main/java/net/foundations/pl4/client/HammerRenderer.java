package net.foundations.pl4.client;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.foundations.pl4.*;

public final class HammerRenderer extends TileEntityRenderer<HammerEntity> {
    private static final ResourceLocation TEXTURE=FoundationsPL4.id("textures/block/model/forging_hammer_stone.png");
    private final HammerModel model;
    private final ItemRenderer items;
    public HammerRenderer(TileEntityRendererDispatcher context){super(context);model=new HammerModel();items=net.minecraft.client.Minecraft.getInstance().getItemRenderer();}
    @Override public void render(HammerEntity hammer,float partial,MatrixStack pose,IRenderTypeBuffer buffer,int light,int overlay){
        pose.pushPose();
        pose.translate(.5,1.5,.5);
        pose.mulPose(Vector3f.YP.rotationDegrees(-hammer.getBlockState().getValue(HammerBlock.FACING).toYRot()));
        pose.mulPose(Vector3f.XP.rotationDegrees(180));
        model.render(pose,buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,overlay,
            hammer.animationFraction(partial),hammer.structureReady());
        pose.popPose();
        ItemStack stack=hammer.inventory.getStackInSlot(0);
        if(stack.isEmpty())stack=hammer.inventory.getStackInSlot(1);
        if(!stack.isEmpty()){
            pose.pushPose();pose.translate(.5,.89,.5);pose.scale(.5F,.5F,.5F);
            items.renderStatic(stack,TransformType.GROUND,light,overlay,pose,buffer);
            pose.popPose();
        }
    }
    public AxisAlignedBB getRenderBoundingBox(HammerEntity hammer){return new AxisAlignedBB(hammer.getBlockPos()).expandTowards(0,2,0);}
    public int getViewDistance(){return 64;}
}
