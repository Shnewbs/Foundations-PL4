package net.foundations.pl4.compat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public abstract class PortScreen extends Screen {
 protected PortScreen(Component title){super(title);}
 public void renderBackground(GuiGraphics g,int x,int y,float partial){super.renderBackground(g);}
 @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g,x,y,partial);super.render(g,x,y,partial);}
 public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return super.mouseScrolled(x,y,vertical);}
 @Override public boolean mouseScrolled(double x,double y,double vertical){return mouseScrolled(x,y,0,vertical);}
}
