package net.foundations.pl4.compat;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
public class Button extends net.minecraft.client.gui.components.Button {
 public interface OnPress {void onPress(Button button);}
 public static final Object DEFAULT_NARRATION=new Object();
 private Tooltip tooltip;
 public Button(int x,int y,int width,int height,Component message,OnPress press,Object narration){
  super(x,y,width,height,message,b->press.onPress((Button)b),net.minecraft.client.gui.components.Button.DEFAULT_NARRATION);
 }
 public static Builder pl4Builder(Component message,OnPress press){return new Builder(message,press);}
 public void setTooltip(Tooltip value){tooltip=value;}
 /** The native 1.19.4 GUI entrypoint changed from renderButton to renderWidget.
  * Preserve GuideButton's custom low-chrome paint path via virtual dispatch. */
 @Override protected void renderWidget(net.minecraft.client.gui.GuiGraphics nativeG,int x,int y,float partial){
  if(getClass()==Button.class)super.renderWidget(nativeG,x,y,partial);
  else renderWidget(new GuiGraphics(nativeG.pose()),x,y,partial);
  if(tooltip!=null&&isMouseOver(x,y))nativeG.renderTooltip(Minecraft.getInstance().font,tooltip.text(),x,y);
 }
 protected void renderWidget(GuiGraphics g,int x,int y,float partial){
  g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),0xFF34444F);
  g.drawString(Minecraft.getInstance().font,getMessage(),getX()+4,getY()+5,0xFFF3F7F8,false);
 }
 public static final class Builder {
  private final Component message;private final OnPress press;private int x,y,w=150,h=20;private Tooltip tip;
  Builder(Component message,OnPress press){this.message=message;this.press=press;}
  public Builder bounds(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;return this;}
  public Builder tooltip(Tooltip value){tip=value;return this;}
  public Button build(){Button b=new Button(x,y,w,h,message,press,DEFAULT_NARRATION);b.setTooltip(tip);return b;}
 }
}
