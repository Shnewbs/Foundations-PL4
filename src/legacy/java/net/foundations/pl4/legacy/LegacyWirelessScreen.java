package net.foundations.pl4.legacy;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;
@net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
public final class LegacyWirelessScreen extends GuiContainer {
    private static final ResourceLocation TEXTURE=new ResourceLocation("textures/gui/container/generic_54.png");
    private net.minecraft.client.gui.GuiTextField searchField;private int pendingRevision=-1;
    public LegacyWirelessScreen(LegacyWirelessContainer container){super(container);xSize=176;ySize=224;}
    @Override public void initGui(){super.initGui();org.lwjgl.input.Keyboard.enableRepeatEvents(true);String text=searchField==null?((LegacyWirelessContainer)inventorySlots).search():searchField.getText();searchField=new net.minecraft.client.gui.GuiTextField(3,fontRenderer,guiLeft+46,guiTop+207,122,14);searchField.setMaxStringLength(LegacyWirelessStorage.MAX_SEARCH);searchField.setText(text);buttonList.add(new GuiButton(2,guiLeft+8,guiTop+3,64,13,"Network"));buttonList.add(new GuiButton(3,guiLeft+74,guiTop+3,40,13,"Slots"));buttonList.add(new GuiButton(0,guiLeft+119,guiTop+3,24,13,"<"));buttonList.add(new GuiButton(1,guiLeft+145,guiTop+3,24,13,">"));}
    @Override public void onGuiClosed(){super.onGuiClosed();org.lwjgl.input.Keyboard.enableRepeatEvents(false);}
    @Override protected void actionPerformed(GuiButton button){
        int revision=((LegacyWirelessContainer)inventorySlots).revision;if(revision<0||searchPending())return;
        if(button.id==2)mc.playerController.windowClick(inventorySlots.windowId,LegacyWirelessContainer.SORT_CONTROL,revision<<1,net.minecraft.inventory.ClickType.PICKUP,mc.player);
        else if(button.id==3)mc.playerController.windowClick(inventorySlots.windowId,LegacyWirelessContainer.GROUP_CONTROL,revision<<1,net.minecraft.inventory.ClickType.PICKUP,mc.player);
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
            mc.playerController.windowClick(inventorySlots.windowId,control,container.revision<<1,net.minecraft.inventory.ClickType.PICKUP,mc.player);
        }
        for(Object raw:buttonList){GuiButton button=(GuiButton)raw;if(button.id==2)button.displayString=new String[]{"Network","Name","Count"}[((LegacyWirelessContainer)inventorySlots).sortOrder()];else if(button.id==3)button.displayString=container.grouped()?"Groups":"Slots";}}
    @Override protected void handleMouseClick(net.minecraft.inventory.Slot slot,int slotId,int button,net.minecraft.inventory.ClickType type){
        if((button!=0&&button!=1)||!(type==net.minecraft.inventory.ClickType.PICKUP||type==net.minecraft.inventory.ClickType.QUICK_MOVE))return;
        int revision=((LegacyWirelessContainer)inventorySlots).revision;if(revision<0||searchPending())return;
        mc.playerController.windowClick(inventorySlots.windowId,slotId,(revision<<1)|button,type,mc.player);
    }
    @Override protected void keyTyped(char typed,int key) throws java.io.IOException{
        if(key==1){super.keyTyped(typed,key);return;}
        if(searchField.isFocused()){
            searchField.textboxKeyTyped(typed,key); // Native Delete uses char 127; sanitize text after handling editing keys.
            String text=searchField.getText();StringBuilder bounded=new StringBuilder();
            for(int i=0;i<text.length();i++)if(text.charAt(i)>=32&&text.charAt(i)<=126)bounded.append(text.charAt(i));
            if(!bounded.toString().equals(text))searchField.setText(bounded.toString());return;
        }super.keyTyped(typed,key);
    }
    @Override protected void mouseClicked(int x,int y,int button) throws java.io.IOException{super.mouseClicked(x,y,button);searchField.mouseClicked(x,y,button);}
    @Override public void drawScreen(int mouseX,int mouseY,float partial){drawDefaultBackground();super.drawScreen(mouseX,mouseY,partial);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LIGHTING);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);searchField.drawTextBox();org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);renderHoveredToolTip(mouseX,mouseY);}
    private int hoveredStorage(int mouseX,int mouseY){
        for(int i=0;i<45;i++){int x=guiLeft+LegacyWirelessContainer.storageX(i),y=guiTop+LegacyWirelessContainer.storageY(i);if(mouseX>=x&&mouseX<x+16&&mouseY>=y&&mouseY<y+16)return i;}return -1;
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY){
        LegacyWirelessContainer container=(LegacyWirelessContainer)inventorySlots;int hover=hoveredStorage(mouseX,mouseY);
        fontRenderer.drawString(container.grouped()&&hover>=0&&container.rowTotal(hover)>0?"Total: "+container.rowTotal(hover):"Inventory: click to deposit",8,110,0x404040);
        fontRenderer.drawString(searchPending()?"Find...":"Find:",8,210,0x404040);
        if(container.grouped()){
            boolean lighting=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_LIGHTING),depth=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LIGHTING);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            try{for(int i=0;i<45;i++)if(container.rowTotal(i)>0){String count=LegacyWirelessStorage.compactTotal(container.rowTotal(i));int width=fontRenderer.getStringWidth(count);float scale=Math.min(1f,16f/Math.max(1,width));org.lwjgl.opengl.GL11.glPushMatrix();try{org.lwjgl.opengl.GL11.glTranslatef(LegacyWirelessContainer.storageX(i)+16,LegacyWirelessContainer.storageY(i)+9,0);org.lwjgl.opengl.GL11.glScalef(scale,scale,1);fontRenderer.drawStringWithShadow(count,-width,0,0xffffff);}finally{org.lwjgl.opengl.GL11.glPopMatrix();}}}
            finally{if(lighting)org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_LIGHTING);if(depth)org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);}
        }
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY){
        org.lwjgl.opengl.GL11.glColor4f(1,1,1,1);mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft,guiTop,0,0,176,108);drawTexturedModalRect(guiLeft,guiTop+108,0,126,176,96);drawRect(guiLeft,guiTop+204,guiLeft+176,guiTop+224,0xffc6c6c6);
    }
}
