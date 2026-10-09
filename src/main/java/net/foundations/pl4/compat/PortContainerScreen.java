package net.foundations.pl4.compat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
public abstract class PortContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
 protected PortContainerScreen(T menu,Inventory inventory,Component title){super(menu,inventory,title);}
 @Override protected final void renderBg(PoseStack pose,float partial,int x,int y){renderBg(new GuiGraphics(pose),partial,x,y);}
 protected abstract void renderBg(GuiGraphics g,float partial,int x,int y);
 @Override protected final void renderLabels(PoseStack pose,int x,int y){renderLabels(new GuiGraphics(pose),x,y);}
 protected void renderLabels(GuiGraphics g,int x,int y){super.renderLabels(g.pose(),x,y);}
 @Override public final void render(PoseStack pose,int x,int y,float partial){render(new GuiGraphics(pose),x,y,partial);}
 public void render(GuiGraphics g,int x,int y,float partial){super.render(g.pose(),x,y,partial);}
 public void renderBackground(GuiGraphics g){super.renderBackground(g.pose());}
 protected void renderTooltip(GuiGraphics g,int x,int y){super.renderTooltip(g.pose(),x,y);}
}
