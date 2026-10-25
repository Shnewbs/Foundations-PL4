package net.foundations.pl4.legacy;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyWirelessScreen extends GuiContainer {
    private static final ResourceLocation TEXTURE=new ResourceLocation("textures/gui/container/generic_54.png");
    private net.minecraft.client.gui.GuiTextField searchField;private int pendingRevision=-1;
    public LegacyWirelessScreen(LegacyWirelessContainer container){super(container);xSize=176;ySize=224;}
    @Override public void initGui(){super.initGui();String text=searchField==null?((LegacyWirelessContainer)inventorySlots).search():searchField.getText();searchField=new net.minecraft.client.gui.GuiTextField(fontRenderer,guiLeft+46,guiTop+207,122,14);searchField.setMaxStringLength(LegacyWirelessStorage.MAX_SEARCH);searchField.setText(text);buttonList.add(new GuiButton(2,guiLeft+8,guiTop+3,106,13,"Sort: Network"));buttonList.add(new GuiButton(0,guiLeft+119,guiTop+3,24,13,"<"));buttonList.add(new GuiButton(1,guiLeft+145,guiTop+3,24,13,">"));}
    @Override protected void actionPerformed(GuiButton button){
        int revision=((LegacyWirelessContainer)inventorySlots).revision;if(revision<0||searchPending())return;
        if(button.id==2)mc.playerController.windowClick(inventorySlots.windowId,LegacyWirelessContainer.SORT_CONTROL,revision<<1,0,mc.thePlayer);
        else mc.playerController.sendEnchantPacket(inventorySlots.windowId,(revision<<1)|button.id);
    }
    private boolean searchPending(){return searchField!=null&&!searchField.getText().equals(((LegacyWirelessContainer)inventorySlots).search());}
    @Override public void updateScreen(){super.updateScreen();searchField.updateCursorCounter();
        LegacyWirelessContainer container=(LegacyWirelessContainer)inventorySlots;
        String desired=searchField.getText(),actual=container.search();
        // One native control per acknowledged revision: fast typing/paste cannot lose characters.
        if(container.revision>=0&&container.revision!=pendingRevision&&!desired.equals(actual)){
            int control=LegacyWirelessContainer.SEARCH_CLEAR;
            if(desired.startsWith(actual))control=LegacyWirelessContainer.SEARCH_CHARACTER+desired.charAt(actual.length())-32;
            else if(actual.startsWith(desired))control=LegacyWirelessContainer.SEARCH_BACKSPACE;
            pendingRevision=container.revision;
            mc.playerController.windowClick(inventorySlots.windowId,control,container.revision<<1,0,mc.thePlayer);
        }
        for(Object raw:buttonList){GuiButton button=(GuiButton)raw;if(button.id==2)button.displayString="Sort: "+new String[]{"Network","Name","Count"}[((LegacyWirelessContainer)inventorySlots).sortOrder()];}}
    @Override protected void handleMouseClick(net.minecraft.inventory.Slot slot,int slotId,int button,int type){
        if((button!=0&&button!=1)||!(type==0||type==1))return;
        int revision=((LegacyWirelessContainer)inventorySlots).revision;if(revision<0||searchPending())return;
        mc.playerController.windowClick(inventorySlots.windowId,slotId,(revision<<1)|button,type,mc.thePlayer);
    }
    @Override protected void keyTyped(char typed,int key){
        if(key==1){super.keyTyped(typed,key);return;}
        if(searchField.isFocused()){
            searchField.textboxKeyTyped(typed,key); // Native Delete uses char 127; sanitize text after handling editing keys.
            String text=searchField.getText();StringBuilder bounded=new StringBuilder();
            for(int i=0;i<text.length();i++)if(text.charAt(i)>=32&&text.charAt(i)<=126)bounded.append(text.charAt(i));
            if(!bounded.toString().equals(text))searchField.setText(bounded.toString());return;
        }super.keyTyped(typed,key);
    }
    @Override protected void mouseClicked(int x,int y,int button){super.mouseClicked(x,y,button);searchField.mouseClicked(x,y,button);}
    @Override public void drawScreen(int mouseX,int mouseY,float partial){drawDefaultBackground();super.drawScreen(mouseX,mouseY,partial);searchField.drawTextBox();}
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY){fontRenderer.drawString("Inventory: click to deposit",8,110,0x404040);fontRenderer.drawString(searchPending()?"Find...":"Find:",8,210,0x404040);}
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY){
        org.lwjgl.opengl.GL11.glColor4f(1,1,1,1);mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft,guiTop,0,0,176,108);drawTexturedModalRect(guiLeft,guiTop+108,0,126,176,96);drawRect(guiLeft,guiTop+204,guiLeft+176,guiTop+224,0xffc6c6c6);
    }
}
