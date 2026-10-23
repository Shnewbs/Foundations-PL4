package net.foundations.pl4.legacy;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyWirelessScreen extends GuiContainer {
    private static final ResourceLocation TEXTURE=new ResourceLocation("textures/gui/container/generic_54.png");
    public LegacyWirelessScreen(LegacyWirelessContainer container){super(container);xSize=176;ySize=204;}
    @Override public void initGui(){super.initGui();buttonList.add(new GuiButton(0,guiLeft+119,guiTop+3,24,13,"<"));buttonList.add(new GuiButton(1,guiLeft+145,guiTop+3,24,13,">"));}
    @Override protected void actionPerformed(GuiButton button){mc.playerController.sendEnchantPacket(inventorySlots.windowId,(((LegacyWirelessContainer)inventorySlots).revision<<1)|button.id);}
    @Override protected void handleMouseClick(net.minecraft.inventory.Slot slot,int slotId,int button,int type){
        if((button!=0&&button!=1)||!(type==0||type==1))return;
        int revision=((LegacyWirelessContainer)inventorySlots).revision;if(revision<0)return;
        mc.playerController.windowClick(inventorySlots.windowId,slotId,(revision<<1)|button,type,mc.thePlayer);
    }
    @Override public void drawScreen(int mouseX,int mouseY,float partial){drawDefaultBackground();super.drawScreen(mouseX,mouseY,partial);}
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY){fontRenderer.drawString("Wireless Storage",8,6,0x404040);fontRenderer.drawString("Inventory: click to deposit",8,110,0x404040);}
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY){
        org.lwjgl.opengl.GL11.glColor4f(1,1,1,1);mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft,guiTop,0,0,176,108);drawTexturedModalRect(guiLeft,guiTop+108,0,126,176,96);
    }
}
