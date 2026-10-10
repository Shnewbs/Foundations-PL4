package net.foundations.pl4.compat;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
public abstract class PortScreen extends Screen {
 protected void rebuildWidgets(){buttons.clear();children.clear();setFocused(null);init();}
 protected <T extends net.minecraft.client.gui.widget.Widget> T addRenderableWidget(T widget){return addButton(widget);}
 protected void removeWidget(net.minecraft.client.gui.widget.Widget widget){buttons.remove(widget);children.remove(widget);if(getFocused()==widget)setFocused(null);}
 protected PortScreen(ITextComponent title){super(title);}
 public void renderBackground(GuiGraphics g,int x,int y,float partial){super.renderBackground();}
 public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g,x,y,partial);super.render(x,y,partial);}
 @Override public final void render(int x,int y,float partial){render(new GuiGraphics(),x,y,partial);}
 public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return super.mouseScrolled(x,y,vertical);}
 @Override public boolean mouseScrolled(double x,double y,double vertical){return mouseScrolled(x,y,0,vertical);}
}
