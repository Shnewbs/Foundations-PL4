package net.foundations.pl4.compat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
/** Native PoseStack-backed GUI bridge; no replacement Minecraft classes are injected. */
public final class GuiGraphics {
 private final PoseStack pose;private final Minecraft mc=Minecraft.getInstance();
 private final java.util.Deque<int[]> clips=new java.util.ArrayDeque<>();
 public GuiGraphics(PoseStack pose){this.pose=pose;}
 public PoseStack pose(){return pose;}
 public void fill(int x,int y,int right,int bottom,int color){GuiComponent.fill(pose,x,y,right,bottom,color);}
 public void renderOutline(int x,int y,int w,int h,int color){fill(x,y,x+w,y+1,color);fill(x,y+h-1,x+w,y+h,color);fill(x,y,x+1,y+h,color);fill(x+w-1,y,x+w,y+h,color);}
 public int drawString(Font font,String text,int x,int y,int color,boolean shadow){return shadow?font.drawShadow(pose,text,x,y,color):font.draw(pose,text,x,y,color);}
 public int drawString(Font font,Component text,int x,int y,int color,boolean shadow){return drawString(font,text.getVisualOrderText(),x,y,color,shadow);}
 public int drawString(Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow){return shadow?font.drawShadow(pose,text,x,y,color):font.draw(pose,text,x,y,color);}
 public void blit(ResourceLocation texture,int x,int y,int u,int v,int w,int h){RenderSystem.setShaderTexture(0,texture);GuiComponent.blit(pose,x,y,u,v,w,h,256,256);}
 public void renderTooltip(Font font,Component text,int x,int y){if(mc.screen!=null)mc.screen.renderTooltip(pose,text,x,y);}
 public void renderItem(ItemStack stack,int x,int y){
  var model=RenderSystem.getModelViewStack();model.pushPose();model.mulPoseMatrix(pose.last().pose());RenderSystem.applyModelViewMatrix();
  try{mc.getItemRenderer().renderAndDecorateFakeItem(pose,stack,x,y);}finally{model.popPose();RenderSystem.applyModelViewMatrix();}
 }
 public void enableScissor(int x,int y,int right,int bottom){if(!clips.isEmpty()){int[] p=clips.peek();x=Math.max(x,p[0]);y=Math.max(y,p[1]);right=Math.min(right,p[2]);bottom=Math.min(bottom,p[3]);}clips.push(new int[]{x,y,Math.max(x,right),Math.max(y,bottom)});applyClip();}
 public void disableScissor(){if(!clips.isEmpty())clips.pop();applyClip();}
 private void applyClip(){if(clips.isEmpty()){RenderSystem.disableScissor();return;}int[] r=clips.peek();double s=mc.getWindow().getGuiScale();RenderSystem.enableScissor((int)(r[0]*s),(int)(mc.getWindow().getHeight()-r[3]*s),(int)((r[2]-r[0])*s),(int)((r[3]-r[1])*s));}
}
