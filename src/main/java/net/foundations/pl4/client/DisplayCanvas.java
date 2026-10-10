package net.foundations.pl4.client;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.Vector3f;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.text.ITextComponent;

import net.minecraft.util.ResourceLocation;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.BlockItem;
import net.foundations.pl4.*;
import net.foundations.pl4.core.DisplayElements;


/** Depth-tested, very shallow world-space canvas. Does not disable depth testing globally.
 * The model-space 0.01 steps are normalized by canvas scale, so a 16x16 board is not 16x deeper.
 */
final class DisplayCanvas {
    private static final ResourceLocation WHITE=FoundationsPL4.id("textures/gui/display_white.png");
    /** Small but real local depth for isometric block previews. Keeps rotated cube faces distinct
     * without visibly floating the preview away from the monitor plane. */
    private static final double BLOCK_MODEL_DEPTH_BLOCKS=.0006;
    private static final double FLAT_ITEM_DEPTH_BLOCKS=.000001/16.0;
    private int order;
    private final MatrixStack pose;private final IRenderTypeBuffer buffers;private final double scale;private final Minecraft mc;
    DisplayCanvas(MatrixStack pose,IRenderTypeBuffer buffers,double scale){this.pose=pose;this.buffers=buffers;this.scale=scale;mc=Minecraft.getInstance();}
    void order(int order){this.order=net.foundations.pl4.compat.PortMath.clamp(order,0,31);}
    double depth(int layer){return (DisplayElements.worldDepth(layer)+(layer>0&&layer<5?order*.004/(16.0*32):0))/scale;}
    void rect(double x,double y,double w,double h,int color,int layer){if(w<=0||h<=0)return;quad(WHITE,x,y,w,h,0,0,1,1,color,depth(layer));}
    void outline(DisplayElements.Rect r,int color,int layer){rect(r.x(),r.y(),r.width(),.6,color,layer);rect(r.x(),r.bottom()-.6,r.width(),.6,color,layer);rect(r.x(),r.y(),.6,r.height(),color,layer);rect(r.right()-.6,r.y(),.6,r.height(),color,layer);}
    void text(String value,int x,int y,int width,int color,boolean right,int layer){
        text(value,x,y,width,12,color,right?DisplayElements.TextAlign.RIGHT:DisplayElements.TextAlign.LEFT,false,1F,layer);
    }
    void text(String value,int x,int y,int width,int height,int color,DisplayElements.TextAlign alignment,boolean wrap,float textScale,int layer){
        if(width<=0||height<=0)return;FontRenderer font=mc.font;pose.pushPose();
        try{
            pose.translate(x,y,depth(layer));pose.scale(textScale,textScale,1);
            int scaledWidth=Math.max(1,Math.round(width/textScale)),scaledHeight=Math.max(1,Math.round(height/textScale));
            int lineY=0;int maxY=scaledHeight;
            List<String> lines=wrap?font.split(value,scaledWidth):
                List.of(font.substrByWidth(value,scaledWidth));
            for(var line:lines){
                if(lineY+font.lineHeight>maxY)break;
                int lineWidth=font.width(line);int tx=switch(alignment){case LEFT->0;case CENTER->(scaledWidth-lineWidth)/2;case RIGHT->scaledWidth-lineWidth;};
                font.drawInBatch(line,tx,lineY,0xFF000000|color,false,pose.last().pose(),buffers,false,0,15728880);
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
                pose.scale(.62F,.62F,.62F);pose.mulPose(Vector3f.XP.rotationDegrees(30));pose.mulPose(Vector3f.YP.rotationDegrees(45));pose.translate(-.5,-.5,-.5);
                mc.getBlockRenderer().renderSingleBlock(bi.getBlock().defaultBlockState(),pose,buffers,15728880,OverlayTexture.NO_OVERLAY);
            }else mc.getItemRenderer().renderStatic(sample.item(),TransformType.GUI,15728880,OverlayTexture.NO_OVERLAY,pose,buffers);
        }catch(RuntimeException error){warn(sample.itemId(),error);}finally{pose.popPose();}
    }
    void fluid(Part.Row sample,DisplayElements.Rect r,double fraction){
        if(sample.fluid().isEmpty()||fraction<=0)return;
        try{
            var ext=sample.fluid().getFluid().getAttributes();var id=ext.getStillTexture(sample.fluid());if(id==null)return;
            var sprite=mc.getTextureAtlas(PlayerContainer.BLOCK_ATLAS).apply(id);int color=ext.getColor(sample.fluid());
            double fill=r.height()*net.foundations.pl4.compat.PortMath.clamp(fraction,0,1),top=r.bottom()-fill;
            // Tile/crop instead of stretching a fluid sprite over the entire tank.
            for(double x=r.x();x<r.right();x+=16)for(double y=top;y<r.bottom();y+=16){double w=Math.min(16,r.right()-x),h=Math.min(16,r.bottom()-y);
                float u1=sprite.getU0()+(sprite.getU1()-sprite.getU0())*(float)(w/16),v1=sprite.getV0()+(sprite.getV1()-sprite.getV0())*(float)(h/16);
                quad(PlayerContainer.BLOCK_ATLAS,x,y,w,h,sprite.getU0(),sprite.getV0(),u1,v1,color,depth(2));
            }
        }catch(RuntimeException error){warn(sample.fluidId(),error);}
    }
    private static final java.util.Set<String> WARNED=new java.util.LinkedHashSet<>();
    private static void warn(String id,RuntimeException error){if(WARNED.size()<64&&WARNED.add(id))org.apache.logging.log4j.LogManager.getLogger("FoundationsPL4").warn("Cannot render display picture {}",id,error);}
    private void quad(ResourceLocation texture,double x,double y,double w,double h,float u0,float v0,float u1,float v1,int color,double z){
        var v=buffers.getBuffer(RenderType.entityTranslucent(texture));var p=pose.last();
        v.vertex(p.pose(),(float)x,(float)y,(float)z).color((color>>>16)&255,(color>>>8)&255,color&255,(color>>>24)&255).uv(u0,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.normal(),0,0,1).endVertex();
        v.vertex(p.pose(),(float)x,(float)(y+h),(float)z).color((color>>>16)&255,(color>>>8)&255,color&255,(color>>>24)&255).uv(u0,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.normal(),0,0,1).endVertex();
        v.vertex(p.pose(),(float)(x+w),(float)(y+h),(float)z).color((color>>>16)&255,(color>>>8)&255,color&255,(color>>>24)&255).uv(u1,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.normal(),0,0,1).endVertex();
        v.vertex(p.pose(),(float)(x+w),(float)y,(float)z).color((color>>>16)&255,(color>>>8)&255,color&255,(color>>>24)&255).uv(u1,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.normal(),0,0,1).endVertex();
    }
}
