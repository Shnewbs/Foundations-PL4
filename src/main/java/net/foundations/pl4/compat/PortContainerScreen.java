package net.foundations.pl4.compat;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
public abstract class PortContainerScreen<T extends Container> extends ContainerScreen<T> {
 protected PortContainerScreen(T menu,PlayerInventory inventory,ITextComponent title){super(menu,inventory,title);}
 @Override protected final void renderBg(MatrixStack pose,float partial,int x,int y){renderBg(new GuiGraphics(pose),partial,x,y);}
 protected abstract void renderBg(GuiGraphics g,float partial,int x,int y);
 @Override protected final void renderLabels(MatrixStack pose,int x,int y){renderLabels(new GuiGraphics(pose),x,y);}
 protected void renderLabels(GuiGraphics g,int x,int y){super.renderLabels(g.pose(),x,y);}
 @Override public final void render(MatrixStack pose,int x,int y,float partial){render(new GuiGraphics(pose),x,y,partial);}
 public void render(GuiGraphics g,int x,int y,float partial){super.render(g.pose(),x,y,partial);}
 public void renderBackground(GuiGraphics g){super.renderBackground(g.pose());}
 protected void renderTooltip(GuiGraphics g,int x,int y){super.renderTooltip(g.pose(),x,y);}
}
