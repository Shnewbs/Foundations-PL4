package net.foundations.pl4.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.foundations.pl4.*;
import net.foundations.pl4.core.DisplayElements;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Depth-tested, very shallow world-space canvas. Does not disable depth testing globally.
 * The model-space 0.01 steps are normalized by canvas scale, so a 16x16 board is not 16x deeper.
 */
final class DisplayCanvas {
    private static final Identifier WHITE=FoundationsPL4.id("textures/gui/display_white.png");
    /** Small but real local depth for isometric block previews. Keeps rotated cube faces distinct
     * without visibly floating the preview away from the monitor plane. */
    private static final double BLOCK_MODEL_DEPTH_BLOCKS=.0006;
    private static final double FLAT_ITEM_DEPTH_BLOCKS=.000001/16.0;
    private int order;
    private final PoseStack pose;private final RenderCommands buffers;private final double scale;private final Minecraft mc;
    DisplayCanvas(PoseStack pose,RenderCommands buffers,double scale){this.pose=pose;this.buffers=buffers;this.scale=scale;mc=Minecraft.getInstance();}
    void order(int order){this.order=Math.clamp(order,0,31);}
    double depth(int layer){return (DisplayElements.worldDepth(layer)+(layer>0&&layer<5?order*.004/(16.0*32):0))/scale;}
    void rect(double x,double y,double w,double h,int color,int layer){if(w<=0||h<=0)return;quad(WHITE,x,y,w,h,0,0,1,1,color,depth(layer));}
    void outline(DisplayElements.Rect r,int color,int layer){rect(r.x(),r.y(),r.width(),.6,color,layer);rect(r.x(),r.bottom()-.6,r.width(),.6,color,layer);rect(r.x(),r.y(),.6,r.height(),color,layer);rect(r.right()-.6,r.y(),.6,r.height(),color,layer);}
    void text(String value,int x,int y,int width,int color,boolean right,int layer){
        text(value,x,y,width,12,color,right?DisplayElements.TextAlign.RIGHT:DisplayElements.TextAlign.LEFT,false,1F,layer);
    }
    void text(String value,int x,int y,int width,int height,int color,DisplayElements.TextAlign alignment,boolean wrap,float textScale,int layer){
        if(width<=0||height<=0)return;Font font=mc.font;pose.pushPose();
        try{
            pose.translate(x,y,depth(layer));pose.scale(textScale,textScale,1);
            int scaledWidth=Math.max(1,Math.round(width/textScale)),scaledHeight=Math.max(1,Math.round(height/textScale));
            int lineY=0;int maxY=scaledHeight;
            List<FormattedCharSequence> lines=wrap?font.split(Component.literal(value),scaledWidth):
                List.of(FormattedCharSequence.forward(font.plainSubstrByWidth(value,scaledWidth),net.minecraft.network.chat.Style.EMPTY));
            for(var line:lines){
                if(lineY+font.lineHeight>maxY)break;
                int lineWidth=font.width(line);int tx=switch(alignment){case LEFT->0;case CENTER->(scaledWidth-lineWidth)/2;case RIGHT->scaledWidth-lineWidth;};
                int drawY=lineY;buffers.add(pose,(p,c)->c.submitText(p,tx,drawY,line,false,Font.DisplayMode.NORMAL,net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,0xFF000000|color,0,0));
                lineY+=font.lineHeight;
            }
        }finally{pose.popPose();}
    }
    void item(Part.Row sample,DisplayElements.Rect r,boolean block){
        if(sample.item().isEmpty())return;pose.pushPose();
        try{
            pose.translate(r.x()+r.width()/2.0,r.y()+r.height()/2.0,depth(2));
            float size=Math.min(r.width(),r.height());
            // A block preview still needs real local Z separation or its own faces become coplanar
            // after the isometric rotation. Keep that depth bounded between display planes 1 and 3:
            // 0.0006 block of Z scale gives the rotated unit cube ~0.00103 block total depth,
            // while a flat item icon keeps the old nearly-flat treatment.
            boolean blockPreview=block&&sample.item().getItem() instanceof BlockItem;
            double modelDepthBlocks=blockPreview ? BLOCK_MODEL_DEPTH_BLOCKS : FLAT_ITEM_DEPTH_BLOCKS;
            pose.scale(size,-size,(float)(modelDepthBlocks/scale));
            if(blockPreview&&sample.item().getItem() instanceof BlockItem bi){
                pose.scale(.62F,.62F,.62F);pose.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(30)));pose.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(45)));pose.translate(-.5,-.5,-.5);
                buffers.block(bi.getBlock().defaultBlockState(),pose,net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            }else buffers.item(sample.item(),ItemDisplayContext.GUI,pose,net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        }catch(RuntimeException error){warn(sample.itemId(),error);}finally{pose.popPose();}
    }
    void fluid(Part.Row sample,DisplayElements.Rect r,double fraction){
        if(sample.fluid().isEmpty()||fraction<=0)return;
        try{
            var model=mc.getModelManager().getFluidStateModelSet().get(sample.fluid().getFluid().defaultFluidState());
            var material=model.stillMaterial();if(material==null)return;var sprite=material.sprite();int color=model.fluidTintSource().colorAsStack(sample.fluid());
            double fill=r.height()*Math.clamp(fraction,0,1),top=r.bottom()-fill;
            // Tile/crop instead of stretching a fluid sprite over the entire tank.
            for(double x=r.x();x<r.right();x+=16)for(double y=top;y<r.bottom();y+=16){double w=Math.min(16,r.right()-x),h=Math.min(16,r.bottom()-y);
                float u1=sprite.getU0()+(sprite.getU1()-sprite.getU0())*(float)(w/16),v1=sprite.getV0()+(sprite.getV1()-sprite.getV0())*(float)(h/16);
                quad(sprite.atlasLocation(),x,y,w,h,sprite.getU0(),sprite.getV0(),u1,v1,color,depth(2));
            }
        }catch(RuntimeException error){warn(sample.fluidId(),error);}
    }
    private static final java.util.Set<String> WARNED=new java.util.LinkedHashSet<>();
    private static void warn(String id,RuntimeException error){if(WARNED.size()<64&&WARNED.add(id))org.slf4j.LoggerFactory.getLogger("FoundationsPL4").warn("Cannot render display picture {}",id,error);}
    private void quad(Identifier texture,double x,double y,double w,double h,float u0,float v0,float u1,float v1,int color,double z){
        buffers.add(pose,(drawPose,collector)->collector.submitCustomGeometry(drawPose,net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture),(p,v)->{
        v.addVertex(p,(float)x,(float)y,(float)z).setColor(color).setUv(u0,v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT).setNormal(p,0,0,1);
        v.addVertex(p,(float)x,(float)(y+h),(float)z).setColor(color).setUv(u0,v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT).setNormal(p,0,0,1);
        v.addVertex(p,(float)(x+w),(float)(y+h),(float)z).setColor(color).setUv(u1,v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT).setNormal(p,0,0,1);
        v.addVertex(p,(float)(x+w),(float)y,(float)z).setColor(color).setUv(u1,v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT).setNormal(p,0,0,1);
        }));
    }
}
