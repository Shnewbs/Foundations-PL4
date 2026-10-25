package net.foundations.pl4.legacy;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;
@net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
public final class LegacyWirelessScreen extends GuiContainer {
    private static final ResourceLocation TEXTURE=new ResourceLocation("textures/gui/container/generic_54.png");
    public LegacyWirelessScreen(LegacyWirelessContainer container){super(container);xSize=176;ySize=204;}
    @Override public void initGui(){super.initGui();buttonList.add(new GuiButton(2,guiLeft+8,guiTop+3,106,13,"Sort: Network"));buttonList.add(new GuiButton(0,guiLeft+119,guiTop+3,24,13,"<"));buttonList.add(new GuiButton(1,guiLeft+145,guiTop+3,24,13,">"));}
    @Override protected void actionPerformed(GuiButton button){
        int revision=((LegacyWirelessContainer)inventorySlots).revision;if(revision<0)return;
        if(button.id==2)mc.playerController.windowClick(inventorySlots.windowId,LegacyWirelessContainer.SORT_CONTROL,revision<<1,net.minecraft.inventory.ClickType.PICKUP,mc.player);
        else mc.playerController.sendEnchantPacket(inventorySlots.windowId,(revision<<1)|button.id);
    }
    @Override public void updateScreen(){super.updateScreen();for(Object raw:buttonList){GuiButton button=(GuiButton)raw;if(button.id==2)button.displayString="Sort: "+new String[]{"Network","Name","Count"}[((LegacyWirelessContainer)inventorySlots).sortOrder()];}}
    @Override protected void handleMouseClick(net.minecraft.inventory.Slot slot,int slotId,int button,net.minecraft.inventory.ClickType type){
        if((button!=0&&button!=1)||!(type==net.minecraft.inventory.ClickType.PICKUP||type==net.minecraft.inventory.ClickType.QUICK_MOVE))return;
        int revision=((LegacyWirelessContainer)inventorySlots).revision;if(revision<0)return;
        mc.playerController.windowClick(inventorySlots.windowId,slotId,(revision<<1)|button,type,mc.player);
    }
    @Override public void drawScreen(int mouseX,int mouseY,float partial){drawDefaultBackground();super.drawScreen(mouseX,mouseY,partial);renderHoveredToolTip(mouseX,mouseY);}
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY){fontRenderer.drawString("Inventory: click to deposit",8,110,0x404040);}
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY){
        org.lwjgl.opengl.GL11.glColor4f(1,1,1,1);mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft,guiTop,0,0,176,108);drawTexturedModalRect(guiLeft,guiTop+108,0,126,176,96);
    }
}
