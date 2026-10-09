package net.foundations.pl4.compat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
public class EditBox extends net.minecraft.client.gui.components.EditBox {
 private Tooltip tooltip;public void setTooltip(Tooltip tip){tooltip=tip;}
 public EditBox(Font font,int x,int y,int width,int height,Component message){super(font,x,y,width,height,message);}
 public int getX(){return x;}public int getY(){return y;}public void setX(int value){x=value;}public void setY(int value){y=value;}
 @Override public void renderButton(PoseStack pose,int x,int y,float partial){renderWidget(new GuiGraphics(pose),x,y,partial);if(tooltip!=null&&isMouseOver(x,y))new GuiGraphics(pose).renderTooltip(net.minecraft.client.Minecraft.getInstance().font,tooltip.text(),x,y);}
 public void renderWidget(GuiGraphics g,int x,int y,float partial){super.renderButton(g.pose(),x,y,partial);}
}
