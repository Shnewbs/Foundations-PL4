package net.foundations.pl4.compat;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
/** Native MatrixStack-backed GUI bridge; no replacement Minecraft classes are injected. */
public final class GuiGraphics {
 private final MatrixStack pose;private final Minecraft mc=Minecraft.getInstance();
 private final java.util.Deque<int[]> clips=new java.util.ArrayDeque<>();
 public GuiGraphics(MatrixStack pose){this.pose=pose;}
 public MatrixStack pose(){return pose;}
 public void fill(int x,int y,int right,int bottom,int color){AbstractGui.fill(pose,x,y,right,bottom,color);}
 public void renderOutline(int x,int y,int w,int h,int color){fill(x,y,x+w,y+1,color);fill(x,y+h-1,x+w,y+h,color);fill(x,y,x+1,y+h,color);fill(x+w-1,y,x+w,y+h,color);}
 public int drawString(FontRenderer font,String text,int x,int y,int color,boolean shadow){return shadow?font.drawShadow(pose,text,x,y,color):font.draw(pose,text,x,y,color);}
 public int drawString(FontRenderer font,ITextComponent text,int x,int y,int color,boolean shadow){return drawString(font,(ITextProperties)text,x,y,color,shadow);}
 public int drawString(FontRenderer font,ITextProperties text,int x,int y,int color,boolean shadow){return shadow?font.drawShadow(pose,text,x,y,color):font.draw(pose,text,x,y,color);}
 public void blit(ResourceLocation texture,int x,int y,int u,int v,int w,int h){mc.getTextureManager().bind(texture);AbstractGui.blit(pose,x,y,u,v,w,h,256,256);}
 public void renderTooltip(FontRenderer font,ITextComponent text,int x,int y){if(mc.screen!=null)mc.screen.renderTooltip(pose,text,x,y);}
 public void renderItem(ItemStack stack,int x,int y){
  RenderSystem.pushMatrix();RenderSystem.multMatrix(pose.last().pose());
  try{mc.getItemRenderer().renderAndDecorateFakeItem(stack,x,y);}finally{RenderSystem.popMatrix();}
 }
 public void enableScissor(int x,int y,int right,int bottom){if(!clips.isEmpty()){int[] p=clips.peek();x=Math.max(x,p[0]);y=Math.max(y,p[1]);right=Math.min(right,p[2]);bottom=Math.min(bottom,p[3]);}clips.push(new int[]{x,y,Math.max(x,right),Math.max(y,bottom)});applyClip();}
 public void disableScissor(){if(!clips.isEmpty())clips.pop();applyClip();}
 private void applyClip(){if(clips.isEmpty()){org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);return;}int[] r=clips.peek();double s=mc.getWindow().getGuiScale();org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glScissor((int)(r[0]*s),(int)(mc.getWindow().getHeight()-r[3]*s),(int)((r[2]-r[0])*s),(int)((r[3]-r[1])*s));}
}
