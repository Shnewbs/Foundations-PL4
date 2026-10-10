package net.foundations.pl4.compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import java.util.ArrayDeque;
import java.util.Deque;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;

/** Native Forge 28 fixed-function GUI bridge; shares the current GL modelview.
 * Every GL clip is nested, size-limited and restored to the prior clip region.
 */
public final class GuiGraphics {
    private final Minecraft mc=Minecraft.getInstance();
    private final Deque<int[]> clips=new ArrayDeque<>();
    public GuiGraphics(){}
    public void fill(int x,int y,int right,int bottom,int color){AbstractGui.fill(x,y,right,bottom,color);}
    public void renderOutline(int x,int y,int w,int h,int color){
        fill(x,y,x+w,y+1,color);fill(x,y+h-1,x+w,y+h,color);fill(x,y,x+1,y+h,color);fill(x+w-1,y,x+w,y+h,color);
    }
    public int drawString(FontRenderer font,String text,int x,int y,int color,boolean shadow){
        return shadow?font.drawShadow(text,x,y,color):font.draw(text,x,y,color);
    }
    public int drawString(FontRenderer font,ITextComponent text,int x,int y,int color,boolean shadow){
        return drawString(font,text.getColoredString(),x,y,color,shadow);
    }
    public void blit(ResourceLocation texture,int x,int y,int u,int v,int w,int h){
        mc.getTextureManager().bind(texture);AbstractGui.blit(x,y,u,v,w,h,256,256);
    }
    public void renderTooltip(FontRenderer font,ITextComponent text,int x,int y){
        if(mc.screen!=null)mc.screen.renderTooltip(text.getColoredString(),x,y);
    }
    public void renderItem(ItemStack stack,int x,int y){mc.getItemRenderer().renderAndDecorateItem(stack,x,y);}
    public void enableScissor(int x,int y,int right,int bottom){
        if(!clips.isEmpty()){
            int[] p=clips.peek();x=Math.max(x,p[0]);y=Math.max(y,p[1]);right=Math.min(right,p[2]);bottom=Math.min(bottom,p[3]);
        }
        clips.push(new int[]{x,y,Math.max(x,right),Math.max(y,bottom)});
        applyClip();
    }
    public void disableScissor(){if(!clips.isEmpty())clips.pop();applyClip();}
    private void applyClip(){
        if(clips.isEmpty()){GL11.glDisable(GL11.GL_SCISSOR_TEST);return;}
        int[] rect=clips.peek();
        var vp=BufferUtils.createIntBuffer(16);GL11.glGetInteger(GL11.GL_VIEWPORT,vp);
        int vx=vp.get(0),vy=vp.get(1),vw=vp.get(2),vh=vp.get(3);
        int sw=mc.screen==null?Math.max(1,vw):Math.max(1,mc.screen.width);
        int sh=mc.screen==null?Math.max(1,vh):Math.max(1,mc.screen.height);
        double scaleX=(double)vw/sw,scaleY=(double)vh/sh;
        int x=(int)Math.floor(vx+rect[0]*scaleX),y=(int)Math.floor(vy+vh-rect[3]*scaleY);
        int w=Math.max(0,(int)Math.ceil((rect[2]-rect[0])*scaleX));
        int h=Math.max(0,(int)Math.ceil((rect[3]-rect[1])*scaleY));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x,y,w,h);
    }
}
