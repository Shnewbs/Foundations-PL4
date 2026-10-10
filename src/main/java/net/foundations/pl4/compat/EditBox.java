package net.foundations.pl4.compat;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
public class EditBox extends net.minecraft.client.gui.widget.TextFieldWidget {
 private Tooltip tooltip;public void setTooltip(Tooltip tip){tooltip=tip;}
 public EditBox(FontRenderer font,int x,int y,int width,int height,ITextComponent message){super(font,x,y,width,height,message.getColoredString());}
 public int getX(){return x;}public int getY(){return y;}public void setX(int value){x=value;}public void setY(int value){y=value;}
 @Override public void renderButton(int x,int y,float partial){GuiGraphics g=new GuiGraphics(new MatrixStack());renderWidget(g,x,y,partial);if(tooltip!=null&&isMouseOver(x,y))g.renderTooltip(net.minecraft.client.Minecraft.getInstance().font,tooltip.text(),x,y);}
 public void renderWidget(GuiGraphics g,int x,int y,float partial){super.renderButton(x,y,partial);}
}
