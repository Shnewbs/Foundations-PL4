package net.foundations.pl4.compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
public class EditBox extends net.minecraft.client.gui.components.EditBox {
 private Tooltip tooltip;
 public EditBox(Font font,int x,int y,int width,int height,Component message){super(font,x,y,width,height,message);}
 public void setTooltip(Tooltip tip){tooltip=tip;}
 @Override public void renderWidget(com.mojang.blaze3d.vertex.PoseStack pose,int x,int y,float partial){
  super.renderWidget(pose,x,y,partial);
  if(tooltip!=null&&isMouseOver(x,y))new GuiGraphics(pose).renderTooltip(Minecraft.getInstance().font,tooltip.text(),x,y);
 }
 public void renderWidget(GuiGraphics g,int x,int y,float partial){
  g.renderOutline(getX(),getY(),getWidth(),getHeight(),0xFF99A7AE);
  g.drawString(Minecraft.getInstance().font,getValue(),getX()+4,getY()+5,0xFFFFFFFF,false);
 }
}
