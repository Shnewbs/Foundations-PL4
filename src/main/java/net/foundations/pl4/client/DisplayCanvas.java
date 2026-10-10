package net.foundations.pl4.client;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.BlockItem;
import net.foundations.pl4.*;
import net.foundations.pl4.compat.LegacyWorldPose;
import net.foundations.pl4.core.DisplayElements;
import org.lwjgl.opengl.GL11;
import java.util.List;

/** World-space monitor canvas for Forge28. Quads use the native BufferBuilder;
 * all text/icons are rendered under the real GL modelview so picking remains exact.
 * The display's depth planes remain positive and depth testing stays enabled.
 */
final class DisplayCanvas {
    private static final ResourceLocation WHITE=FoundationsPL4.id("textures/gui/display_white.png");
    private static final ResourceLocation ATLAS=new ResourceLocation("textures/atlas/blocks.png");
    private static final double BLOCK_MODEL_DEPTH_BLOCKS=.0006;
    private static final double FLAT_ITEM_DEPTH_BLOCKS=.000001/16.0;
    private final LegacyWorldPose pose;
    private final double scale;
    private final Minecraft mc;
    private int order;
    DisplayCanvas(LegacyWorldPose pose,double scale){this.pose=pose;this.scale=scale;mc=Minecraft.getInstance();}
    void order(int layer){order=net.foundations.pl4.compat.PortMath.clamp(layer,0,31);}
    double depth(int layer){return (DisplayElements.worldDepth(layer)+(layer>0&&layer<5?order*.004/(16.0*32):0))/scale;}
    void rect(double x,double y,double w,double h,int color,int layer){
        if(w<=0||h<=0)return;
        quad(WHITE,x,y,w,h,0,0,1,1,color,depth(layer));
    }
    void outline(DisplayElements.Rect r,int color,int layer){
        rect(r.x(),r.y(),r.width(),.6,color,layer);
        rect(r.x(),r.bottom()-.6,r.width(),.6,color,layer);
        rect(r.x(),r.y(),.6,r.height(),color,layer);
        rect(r.right()-.6,r.y(),.6,r.height(),color,layer);
    }
    void text(String value,int x,int y,int width,int color,boolean right,int layer){
        text(value,x,y,width,12,color,right?DisplayElements.TextAlign.RIGHT:DisplayElements.TextAlign.LEFT,false,1F,layer);
    }
    void text(String value,int x,int y,int width,int height,int color,DisplayElements.TextAlign alignment,boolean wrap,float textScale,int layer){
        if(width<=0||height<=0)return;
        FontRenderer font=mc.font;pose.pushPose();
        try{
            pose.translate(x,y,depth(layer));pose.scale(textScale,textScale,1);
            int scaledWidth=Math.max(1,Math.round(width/textScale)),scaledHeight=Math.max(1,Math.round(height/textScale));
            int lineY=0;
            List<String> lines=wrap?font.split(value,scaledWidth):List.of(font.substrByWidth(value,scaledWidth));
            for(String line:lines){
                if(lineY+font.lineHeight>scaledHeight)break;
                int lineWidth=font.width(line),tx=switch(alignment){
                    case LEFT->0;
                    case CENTER->(scaledWidth-lineWidth)/2;
                    case RIGHT->scaledWidth-lineWidth;
                };
                font.draw(line,tx,lineY,0xFF000000|color);
                lineY+=font.lineHeight;
            }
        }finally{pose.popPose();}
    }
    void item(Part.Row sample,DisplayElements.Rect r,boolean block){
        if(sample.item().isEmpty())return;
        pose.pushPose();
        try{
            pose.translate(r.x()+r.width()/2.0,r.y()+r.height()/2.0,depth(2));
            float size=Math.min(r.width(),r.height());
            boolean isBlock=block&&sample.item().getItem() instanceof BlockItem;
            pose.scale(size,-size,(isBlock?BLOCK_MODEL_DEPTH_BLOCKS:FLAT_ITEM_DEPTH_BLOCKS)/scale);
            if(isBlock&&sample.item().getItem() instanceof BlockItem bi){
                pose.scale(.62,.62,.62);pose.rotateX(30);pose.rotateY(45);pose.translate(-.5,-.5,-.5);
                mc.getBlockRenderer().renderSingleBlock(bi.getBlock().defaultBlockState(),1.0F);
            }else mc.getItemRenderer().renderStatic(sample.item(),TransformType.GUI);
        }catch(RuntimeException ex){warn(sample.itemId(),ex);}
        finally{pose.popPose();}
    }
    void fluid(Part.Row sample,DisplayElements.Rect r,double fraction){
        if(sample.fluid().isEmpty()||fraction<=0)return;
        try{
            var a=sample.fluid().getFluid().getAttributes();
            ResourceLocation id=a.getStillTexture();if(id==null)return;
            var sprite=mc.getTextureAtlas().getSprite(id);
            int color=a.getColor(sample.fluid());
            double fill=r.height()*net.foundations.pl4.compat.PortMath.clamp(fraction,0,1),top=r.bottom()-fill;
            for(double x=r.x();x<r.right();x+=16)for(double y=top;y<r.bottom();y+=16){
                double w=Math.min(16,r.right()-x),h=Math.min(16,r.bottom()-y);
                float u1=sprite.getU0()+(sprite.getU1()-sprite.getU0())*(float)(w/16);
                float v1=sprite.getV0()+(sprite.getV1()-sprite.getV0())*(float)(h/16);
                quad(ATLAS,x,y,w,h,sprite.getU0(),sprite.getV0(),u1,v1,color,depth(2));
            }
        }catch(RuntimeException ex){warn(sample.fluidId(),ex);}
    }
    private static final java.util.Set<String> WARNED=new java.util.LinkedHashSet<>();
    private static void warn(String id,RuntimeException ex){
        if(WARNED.size()<64&&WARNED.add(id))
            org.apache.logging.log4j.LogManager.getLogger("FoundationsPL4").warn("Cannot render display picture {}",id,ex);
    }
    private void quad(ResourceLocation texture,double x,double y,double w,double h,float u0,float v0,float u1,float v1,int color,double z){
        mc.getTextureManager().bind(texture);
        GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator tess=Tessellator.getInstance();
        BufferBuilder v=tess.getBuilder();
        int red=(color>>>16)&255,green=(color>>>8)&255,blue=color&255,alpha=(color>>>24)&255;
        v.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
        v.vertex(x,y,z).uv(u0,v0).color(red,green,blue,alpha).endVertex();
        v.vertex(x,y+h,z).uv(u0,v1).color(red,green,blue,alpha).endVertex();
        v.vertex(x+w,y+h,z).uv(u1,v1).color(red,green,blue,alpha).endVertex();
        v.vertex(x+w,y,z).uv(u1,v0).color(red,green,blue,alpha).endVertex();
        tess.end();
    }
}
