package net.foundations.pl4.compat;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
public abstract class PortScreen extends Screen {
 protected void rebuildWidgets(){buttons.clear();children.clear();setFocused(null);init();}
 protected <T extends net.minecraft.client.gui.widget.Widget> T addRenderableWidget(T widget){return addButton(widget);}
 protected void removeWidget(net.minecraft.client.gui.widget.Widget widget){buttons.remove(widget);children.remove(widget);if(getFocused()==widget)setFocused(null);}
 protected PortScreen(ITextComponent title){super(title);}
 public void renderBackground(GuiGraphics g,int x,int y,float partial){super.renderBackground(g.pose());}
 public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g,x,y,partial);super.render(g.pose(),x,y,partial);}
 @Override public final void render(MatrixStack pose,int x,int y,float partial){render(new GuiGraphics(pose),x,y,partial);}
 public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return super.mouseScrolled(x,y,vertical);}
 @Override public boolean mouseScrolled(double x,double y,double vertical){return mouseScrolled(x,y,0,vertical);}
}
