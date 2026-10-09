package net.foundations.pl4.compat;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
public class Button extends net.minecraft.client.gui.widget.button.Button {
 public interface OnPress {void onPress(Button button);}
 public static final Object DEFAULT_NARRATION=new Object();
 private Tooltip tooltip;
 public Button(int x,int y,int width,int height,ITextComponent message,OnPress press,Object narration){super(x,y,width,height,message,b->press.onPress((Button)b));}
 public static Builder builder(ITextComponent message,OnPress press){return new Builder(message,press);}
 public boolean isHoveredOrFocused(){return isHovered()||isFocused();}
 public int getX(){return x;} public int getY(){return y;} public void setX(int value){x=value;} public void setY(int value){y=value;}
 public void setTooltip(Tooltip value){tooltip=value;}
 @Override public void renderButton(MatrixStack pose,int x,int y,float partial){renderWidget(new GuiGraphics(pose),x,y,partial);if(tooltip!=null&&isMouseOver(x,y))new GuiGraphics(pose).renderTooltip(Minecraft.getInstance().font,tooltip.text(),x,y);}
 protected void renderWidget(GuiGraphics graphics,int x,int y,float partial){super.renderButton(graphics.pose(),x,y,partial);}
 public static final class Builder {
  private final ITextComponent message;private final OnPress press;private int x,y,w=150,h=20;private Tooltip tip;
  Builder(ITextComponent message,OnPress press){this.message=message;this.press=press;}
  public Builder bounds(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;return this;}
  public Builder tooltip(Tooltip value){tip=value;return this;}
  public Button build(){Button b=new Button(x,y,w,h,message,press,DEFAULT_NARRATION);b.setTooltip(tip);return b;}
 }
}
