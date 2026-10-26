package net.foundations.pl4.legacy;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;


import net.minecraft.world.EnumGameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.WorldServer;
import java.util.EnumSet;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;



/** Disposable development harness, never included in a release. Exercises native client/server window packets. */
@Mod(modid="pl4_wireless_client_probe",name="PL4 Wireless Client Probe",version="1",dependencies="required-after:foundations_pl4")
public final class Legacy164WirelessClientProbe implements ITickHandler {
    private int opaquePage=1;private String[][] opaqueExpected;private final com.google.gson.JsonArray opaqueBlocks=new com.google.gson.JsonArray();
    private int phase,groupScale=1,scale=1,oldRevision,lastLogged=-1;private long due,started=System.nanoTime();
    private final AtomicReference<String> serverResult=new AtomicReference<String>();
    private boolean worldChecked,sortChecked,searchChecked,groupChecked,chatChecked;
    private final java.util.List<String> diagnosticChat=new java.util.concurrent.CopyOnWriteArrayList<String>();
    private String diagnosticToken,diagnosticJournalChat,diagnosticReceiptChat,importToken,importJournalChat,importReceiptChat;
    private final List<String> captures=new ArrayList<String>();
    @Mod.EventHandler public void init(FMLPreInitializationEvent event){net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);TickRegistry.registerTickHandler(this,Side.CLIENT);TickRegistry.registerTickHandler(this,Side.SERVER);}

    @net.minecraftforge.event.ForgeSubscribe public void diagnosticMessage(net.minecraftforge.client.event.ClientChatReceivedEvent event){
        String text=event.message;
        // Forge 1.6.4 exposes the native JSON component, while newer clients expose plain text.
        try{com.google.gson.JsonElement component=new com.google.gson.JsonParser().parse(text);
            if(component.isJsonObject()){com.google.gson.JsonElement nativeText=component.getAsJsonObject().get("text");
                if(nativeText!=null&&nativeText.isJsonPrimitive()&&nativeText.getAsJsonPrimitive().isString())text=nativeText.getAsString();}
        }catch(RuntimeException nonJsonChat){/* Other mods may emit legacy plain strings. */}
        text=text.replaceAll("(?i)§[0-9a-fk-or]","");if(text.contains("PL4 item ")||text.contains("Token ")||text.startsWith("PL4 opaque export ")||text.startsWith("Page ")||text.startsWith("Original target/phase unknown."))diagnosticChat.add(text);
    }
    private net.minecraft.nbt.NBTTagCompound savedJournal(LegacyWirelessSafety journal){net.minecraft.nbt.NBTTagCompound saved=new net.minecraft.nbt.NBTTagCompound();journal.writeToNBT(saved);return saved;}
    private boolean diagnosticContains(String text){for(String line:diagnosticChat)if(line.contains(text))return true;return false;}
    private File output(){File out=new File(System.getenv("PL4_CLIENT_OUTPUT"));out.mkdirs();return out;}
    private final AtomicReference<Runnable> pending=new AtomicReference<Runnable>();
    private void schedule(final Runnable action){serverResult.set(null);check(pending.compareAndSet(null,action),"server action already pending");}
    private EntityPlayerMP serverPlayer(){return MinecraftServer.getServer().getConfigurationManager().getPlayerForUsername("PL4LegacyProbe");}
    @Override public EnumSet<TickType> ticks(){return EnumSet.of(TickType.CLIENT,TickType.SERVER);}
    @Override public String getLabel(){return "PL4 isolated 1.6.4 client probe";}
    @Override public void tickStart(EnumSet<TickType> type,Object... data){}
    private void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private TileEntityChest source(){return (TileEntityChest)serverPlayer().worldObj.getBlockTileEntity(-1,200,0);}
    private int diamonds(){EntityPlayerMP p=serverPlayer();int n=0;for(int i=0;i<36;i++){ItemStack s=p.inventory.getStackInSlot(i);if(s!=null&&s.itemID==Item.diamond.itemID)n+=s.stackSize;}return n;}
    private void setup(){
        EntityPlayerMP player=serverPlayer();WorldServer world=(WorldServer)player.worldObj;world.getGameRules().setOrCreateGameRule("doMobSpawning","false");
        for(int x=-2;x<=4;x++)for(int z=-2;z<=5;z++)world.setBlock(x,198,z,Block.stone.blockID,0,3);
        world.setBlock(0,200,0,LegacyPL4.EXPORT.blockID,0,3);world.setBlock(1,200,0,LegacyPL4.CABLE.blockID,0,3);world.setBlock(2,200,0,LegacyPL4.IMPORT.blockID,0,3);
        for(int x=0;x<3;x++){LegacyTile tile=(LegacyTile)world.getBlockTileEntity(x,200,0);tile.owner=LegacyTile.identity(player);tile.side=x==0?4:5;tile.onInventoryChanged();}
        world.setBlock(-1,200,0,Block.chest.blockID,0,3);world.setBlock(3,200,0,Block.chest.blockID,0,3);
        TileEntityChest destination=(TileEntityChest)world.getBlockTileEntity(3,200,0);
        for(int i=0;i<27;i++){source().setInventorySlotContents(i,new ItemStack(Item.ingotIron,32));destination.setInventorySlotContents(i,new ItemStack(Item.ingotGold,32));}
        source().setInventorySlotContents(0,new ItemStack(Item.diamond,64));
        for(int i=0;i<36;i++)player.inventory.setInventorySlotContents(i,null);player.inventory.currentItem=0;player.inventory.setInventorySlotContents(0,new ItemStack(LegacyPL4.WIRELESS_STORAGE));player.inventory.setInventorySlotContents(9,new ItemStack(Item.ingotIron,32));
        player.playerNetServerHandler.setPlayerLocation(.5,199,3.5,180,0);LegacyPL4.enabled=false;LegacyPL4.itemRate=0; // Isolate manual window actions from autonomous node transfers.
        check(LegacyWirelessStorage.bind(player,player.inventory.getCurrentItem(),(LegacyTile)world.getBlockTileEntity(0,200,0)),"binding failed");
        player.openGui(LegacyPL4.instance,0,world,0,0,0);
    }
    private void setupDisplay(){
        EntityPlayerMP p=serverPlayer();WorldServer world=(WorldServer)p.worldObj;
        world.setBlock(1,200,0,LegacyPL4.DISPLAY.blockID,0,3);world.setBlock(1,200,-1,LegacyPL4.NETWORK_READER.blockID,0,3);
        LegacyTile display=(LegacyTile)world.getBlockTileEntity(1,200,0),reader=(LegacyTile)world.getBlockTileEntity(1,200,-1);
        display.owner=LegacyTile.identity(p);reader.owner=LegacyTile.identity(p);reader.onInventoryChanged();
        display.displayFacing=3;display.publishReadings=false;display.displayChanged();
        p.playerNetServerHandler.setPlayerLocation(1.5,199,2.2,180,0);
    }
    private LegacyTile clientDisplay(){return (LegacyTile)Minecraft.getMinecraft().theWorld.getBlockTileEntity(1,200,0);}
    private void mouseClick(int logicalX,int logicalY,boolean right)throws Exception{
        Minecraft mc=Minecraft.getMinecraft();ScaledResolution resolution=new ScaledResolution(mc.gameSettings,mc.displayWidth,mc.displayHeight);int factor=resolution.getScaleFactor();
        int left=(resolution.getScaledWidth()-176)/2,top=(resolution.getScaledHeight()-224)/2;
        java.awt.Robot robot=new java.awt.Robot();robot.mouseMove((left+logicalX)*factor,(top+logicalY)*factor);
        int button=right?java.awt.event.InputEvent.BUTTON3_DOWN_MASK:java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
        robot.mousePress(button);robot.mouseRelease(button);
    }
    private void typeSearch(String text)throws Exception{
        java.awt.Robot robot=new java.awt.Robot();
        for(int i=0;i<text.length();i++){int code=java.awt.event.KeyEvent.getExtendedKeyCodeForChar(Character.toUpperCase(text.charAt(i)));robot.keyPress(code);robot.keyRelease(code);}
    }
    private void clearSearch()throws Exception{
        java.awt.Robot robot=new java.awt.Robot();robot.keyPress(java.awt.event.KeyEvent.VK_END);robot.keyRelease(java.awt.event.KeyEvent.VK_END);for(int i=0;i<clientContainer().search().length();i++){robot.keyPress(java.awt.event.KeyEvent.VK_BACK_SPACE);robot.keyRelease(java.awt.event.KeyEvent.VK_BACK_SPACE);}
    }
    private String searchDebug()throws Exception{
        java.lang.reflect.Field field=LegacyWirelessScreen.class.getDeclaredField("searchField");field.setAccessible(true);
        net.minecraft.client.gui.GuiTextField text=(net.minecraft.client.gui.GuiTextField)field.get(Minecraft.getMinecraft().currentScreen);
        return " authoritative="+clientContainer().search()+" desired="+text.getText()+" revision="+clientContainer().revision;
    }
    private int iron(){EntityPlayerMP p=serverPlayer();int n=0;for(int i=0;i<36;i++){ItemStack stack=p.inventory.getStackInSlot(i);if(LegacyTile.STACKS.same(new ItemStack(Item.ingotIron),stack))n+=LegacyTile.STACKS.count(stack);}return n;}
    private void groupResize(Minecraft mc)throws Exception{
        mc.gameSettings.guiScale=groupScale;ScaledResolution resized=new ScaledResolution(mc.gameSettings,mc.displayWidth,mc.displayHeight);mc.currentScreen.setWorldAndResolution(mc,resized.getScaledWidth(),resized.getScaledHeight());
        new java.awt.Robot().mouseMove(8,mc.displayHeight-8);
        if(groupScale==4){ScaledResolution view=new ScaledResolution(mc.gameSettings,mc.displayWidth,mc.displayHeight);int factor=view.getScaleFactor();new java.awt.Robot().mouseMove(((view.getScaledWidth()-176)/2+16)*factor,((view.getScaledHeight()-224)/2+26)*factor);}
    }
    private void packetRoundTrip()throws Exception{
        byte[] click={1,0,0,(byte)141,0,7,0,(byte)255,(byte)255};
        net.minecraft.network.packet.Packet102WindowClick window=new net.minecraft.network.packet.Packet102WindowClick();
        window.readPacketData(new java.io.DataInputStream(new java.io.ByteArrayInputStream(click)));
        java.io.ByteArrayOutputStream result=new java.io.ByteArrayOutputStream();window.writePacketData(new java.io.DataOutputStream(result));
        check(java.util.Arrays.equals(click,result.toByteArray()),"native signed-byte window codec failed");
        byte[] page={1,(byte)141};net.minecraft.network.packet.Packet108EnchantItem enchant=new net.minecraft.network.packet.Packet108EnchantItem();
        enchant.readPacketData(new java.io.DataInputStream(new java.io.ByteArrayInputStream(page)));result.reset();enchant.writePacketData(new java.io.DataOutputStream(result));
        check(java.util.Arrays.equals(page,result.toByteArray()),"native signed-byte page codec failed");
    }
    private void capture(Minecraft mc,String name)throws Exception{
        int width=mc.displayWidth,height=mc.displayHeight;java.nio.ByteBuffer pixels=org.lwjgl.BufferUtils.createByteBuffer(width*height*4);
        org.lwjgl.opengl.GL11.glReadPixels(0,0,width,height,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,pixels);
        java.awt.image.BufferedImage picture=new java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){int i=(y*width+x)*4;picture.setRGB(x,height-1-y,((pixels.get(i)&255)<<16)|((pixels.get(i+1)&255)<<8)|(pixels.get(i+2)&255));}
        ImageIO.write(picture,"png",new File(output(),name));
    }
    private LegacyWirelessContainer clientContainer(){return (LegacyWirelessContainer)Minecraft.getMinecraft().thePlayer.openContainer;}
    private void waitUntilNext(){due=System.nanoTime()+2_000_000_000L;}
    @Override public void tickEnd(EnumSet<TickType> type,Object... data){
        if(type.contains(TickType.SERVER)){Runnable action=pending.getAndSet(null);if(action!=null)try{action.run();serverResult.set("PASS");}catch(Throwable error){serverResult.set(error.toString());error.printStackTrace();}return;}
        if(!type.contains(TickType.CLIENT))return;Minecraft mc=Minecraft.getMinecraft();
        try{
            // Keep the first-world inventory tutorial from covering native paging controls in captures.
            for(java.lang.reflect.Field field:mc.guiAchievement.getClass().getDeclaredFields())if(net.minecraft.stats.Achievement.class.isAssignableFrom(field.getType())){field.setAccessible(true);field.set(mc.guiAchievement,null);}

            if(lastLogged!=phase){lastLogged=phase;System.out.println("PL4 WIRELESS CLIENT PHASE: "+phase);}
            if(System.nanoTime()-started>300_000_000_000L){capture(mc,"legacy-wireless-timeout.png");throw new IllegalStateException("Timed out in probe phase "+phase+(mc.currentScreen instanceof LegacyWirelessScreen?searchDebug():""));}
            String result=serverResult.get();if(result!=null&&!"PASS".equals(result))throw new IllegalStateException(result);
            if(System.nanoTime()<due)return;
            if(phase==0){if(!(mc.currentScreen instanceof GuiMainMenu))return;phase=1;mc.launchIntegratedServer("pl4-wireless-client-probe","PL4 Wireless Probe",new WorldSettings(0L,EnumGameType.CREATIVE,false,false,WorldType.FLAT).enableCommands());waitUntilNext();return;}
            if(phase==1){if(mc.thePlayer==null||mc.theWorld==null||mc.getIntegratedServer()==null)return;schedule(new Runnable(){public void run(){setup();}});phase=2;waitUntilNext();return;}
            if(phase==2){if(!(mc.currentScreen instanceof LegacyWirelessScreen)||!"PASS".equals(result))return;
                mc.gameSettings.guiScale=scale;ScaledResolution resized=new ScaledResolution(mc.gameSettings,mc.displayWidth,mc.displayHeight);mc.currentScreen.setWorldAndResolution(mc,resized.getScaledWidth(),resized.getScaledHeight());new java.awt.Robot().mouseMove(8,mc.displayHeight-8);phase=3;waitUntilNext();return;}
            if(phase==3){
                ScaledResolution resolution=new ScaledResolution(mc.gameSettings,mc.displayWidth,mc.displayHeight);check(resolution.getScaleFactor()==scale,"wrong actual GUI scale");
                String name="legacy-wireless-storage-scale-"+scale+".png";capture(mc,name);captures.add(name);
                if(++scale<=4){phase=2;return;}
                ScaledResolution hover=new ScaledResolution(mc.gameSettings,mc.displayWidth,mc.displayHeight);int factor=hover.getScaleFactor();
                new java.awt.Robot().mouseMove(((hover.getScaledWidth()-176)/2+16)*factor,((hover.getScaledHeight()-224)/2+26)*factor);
                phase=13;waitUntilNext();return;
            }
            if(phase==13){
                capture(mc,"legacy-wireless-storage-tooltip.png");
                schedule(new Runnable(){public void run(){LegacyPL4.enabled=true;LegacyWirelessContainer c=(LegacyWirelessContainer)serverPlayer().openContainer;c.revision=70;c.detectAndSendChanges();}});phase=4;waitUntilNext();return;
            }
            if(phase==4){if(clientContainer().revision!=70)return;packetRoundTrip();oldRevision=clientContainer().revision;
                mouseClick(16,26,true);phase=5;waitUntilNext();return;}
            if(phase==5){schedule(new Runnable(){public void run(){check(diamonds()==1&&source().getStackInSlot(0).stackSize==63,"native signed-byte withdrawal failed");}});phase=6;waitUntilNext();return;}
            if(phase==6){check("PASS".equals(result),"withdrawal check missing");mc.playerController.windowClick(clientContainer().windowId,0,(oldRevision<<1)|1,0,mc.thePlayer);phase=7;waitUntilNext();return;}
            if(phase==7){schedule(new Runnable(){public void run(){check(diamonds()==1&&source().getStackInSlot(0).stackSize==63,"stale replay mutated storage");}});phase=8;waitUntilNext();return;}
            if(phase==8){check("PASS".equals(result),"replay check missing");mouseClick(16,130,true);phase=9;waitUntilNext();return;}
            if(phase==9){schedule(new Runnable(){public void run(){check(serverPlayer().inventory.getStackInSlot(9).stackSize==31&&source().getStackInSlot(1).stackSize==33,"native deposit failed");}});phase=10;waitUntilNext();return;}
            if(phase==10){check("PASS".equals(result),"deposit check missing");mouseClick(157,9,false);phase=11;waitUntilNext();return;}
            if(phase==11){schedule(new Runnable(){public void run(){check(((LegacyWirelessContainer)serverPlayer().openContainer).page==1,"native page request failed");}});phase=12;waitUntilNext();return;}
            if(phase==18){schedule(new Runnable(){public void run(){LegacyWirelessContainer c=(LegacyWirelessContainer)serverPlayer().openContainer;check(c.sortOrder()==1&&c.page==0,"native name sort/reset failed");}});phase=19;waitUntilNext();return;}
            if(phase==19){check("PASS".equals(result)&&clientContainer().sortOrder()==1,"client sort metadata missing");check(clientContainer().getSlot(1).getStack().itemID==Item.ingotGold.itemID,"actual client name ordering mismatch");mc.playerController.windowClick(clientContainer().windowId,0,(oldRevision<<1)|1,0,mc.thePlayer);phase=20;waitUntilNext();return;}
            if(phase==20){schedule(new Runnable(){public void run(){check(diamonds()==1&&source().getStackInSlot(0).stackSize==63,"pre-sort replay mutated storage");serverPlayer().inventory.getCurrentItem().writeToNBT(new net.minecraft.nbt.NBTTagCompound());check(((LegacyWirelessContainer)serverPlayer().openContainer).canInteractWith(serverPlayer()),"native ItemStack save changed tool authorization");}});phase=21;waitUntilNext();return;}
            if(phase==21){check("PASS".equals(result),"sort replay check missing");mouseClick(50,9,false);phase=22;waitUntilNext();return;}
            if(phase==22){schedule(new Runnable(){public void run(){check(((LegacyWirelessContainer)serverPlayer().openContainer).sortOrder()==2,"native count sort failed");}});phase=23;waitUntilNext();return;}
            if(phase==23){check("PASS".equals(result)&&clientContainer().sortOrder()==2,"client count sort metadata missing");check(clientContainer().getSlot(1).getStack().stackSize==33,"actual client count ordering mismatch");capture(mc,"legacy-wireless-storage-sorted.png");sortChecked=true;phase=12;}
            if(phase==24){check(org.lwjgl.input.Keyboard.areRepeatEventsEnabled(),"native search keyboard repeats disabled");typeSearch("gold");phase=25;waitUntilNext();return;}
            if(phase==25){
                if(!clientContainer().search().equals("gold"))return;
                check(clientContainer().page==0&&clientContainer().getSlot(0).getStack().itemID==Item.ingotGold.itemID,"actual filtered client rows mismatch");
                check(clientContainer().getSlot(27).getStack()==null,"search retained nonmatching or duplicate rows");
                capture(mc,"legacy-wireless-storage-search.png");
                mc.playerController.windowClick(clientContainer().windowId,0,(oldRevision<<1)|1,0,mc.thePlayer);
                schedule(new Runnable(){public void run(){LegacyWirelessContainer c=(LegacyWirelessContainer)serverPlayer().openContainer;check(c.search().equals("gold")&&c.page==0&&diamonds()==1&&source().getStackInSlot(0).stackSize==63,"search replay/fence failed");}});
                phase=26;waitUntilNext();return;
            }
            if(phase==26){check("PASS".equals(result),"search server assertion missing");java.awt.Robot edit=new java.awt.Robot();for(int key:new int[]{java.awt.event.KeyEvent.VK_HOME,java.awt.event.KeyEvent.VK_RIGHT,java.awt.event.KeyEvent.VK_DELETE}){edit.keyPress(key);edit.keyRelease(key);}typeSearch("o");phase=29;waitUntilNext();return;}
            if(phase==29){check(clientContainer().search().equals("gold"),"middle-of-query caret edit failed");clearSearch();typeSearch("xyz");phase=27;waitUntilNext();return;}
            if(phase==27){if(!clientContainer().search().equals("xyz"))return;check(clientContainer().getSlot(0).getStack()==null,"no-match search exposed a fallback row");capture(mc,"legacy-wireless-storage-search-empty.png");clearSearch();phase=28;waitUntilNext();return;}
            if(phase==28){if(!clientContainer().search().isEmpty())return;check(clientContainer().getSlot(0).getStack().stackSize==63,"clearing search failed to restore count sort");searchChecked=true;phase=12;}
            if(phase==30){schedule(new Runnable(){public void run(){LegacyWirelessContainer c=(LegacyWirelessContainer)serverPlayer().openContainer;check(c.grouped()&&c.page==0&&c.rowTotal(0)==864&&c.rowTotal(1)==833&&c.rowTotal(2)==63,"native aggregate grouping/reset failed");}});phase=31;waitUntilNext();return;}
            if(phase==31){check("PASS".equals(result)&&clientContainer().grouped()&&clientContainer().rowTotal(0)==864&&clientContainer().rowTotal(1)==833&&clientContainer().rowTotal(2)==63,"native grouped totals missing on actual client");check(LegacyTile.STACKS.count(clientContainer().getSlot(0).getStack())==1,"oversized grouped count sent in native stack");groupScale=1;groupResize(mc);phase=32;waitUntilNext();return;}
            if(phase==32){capture(mc,"legacy-wireless-grouped-scale-"+groupScale+".png");if(++groupScale<=4){groupResize(mc);waitUntilNext();return;}schedule(new Runnable(){public void run(){ItemStack variant=source().getStackInSlot(26);net.minecraft.nbt.NBTTagCompound tag=new net.minecraft.nbt.NBTTagCompound();tag.setString("ProbeVariant","B");variant.setTagCompound(tag);source().onInventoryChanged();((LegacyWirelessContainer)serverPlayer().openContainer).detectAndSendChanges();}});phase=33;waitUntilNext();return;}
            if(phase==33){check("PASS".equals(result)&&clientContainer().rowTotal(1)==801&&clientContainer().rowTotal(3)==32,"full-NBT variant was merged");check(!ItemStack.areItemStackTagsEqual(clientContainer().getSlot(1).getStack(),clientContainer().getSlot(3).getStack()),"client variant previews lost NBT");capture(mc,"legacy-wireless-grouped-variants.png");mouseClick(90,214,false);phase=34;waitUntilNext();return;}
            if(phase==34){typeSearch("iron");phase=35;waitUntilNext();return;}
            if(phase==35){if(!clientContainer().search().equals("iron"))return;check(clientContainer().rowTotal(0)==801&&clientContainer().rowTotal(1)==32,"grouping and search totals disagree");oldRevision=clientContainer().revision;mouseClick(16,26,false);phase=36;waitUntilNext();return;}
            if(phase==36){schedule(new Runnable(){public void run(){check(iron()==95&&LegacyTile.STACKS.count(source().getStackInSlot(1))==0&&LegacyTile.STACKS.count(source().getStackInSlot(2))==1&&LegacyTile.STACKS.count(source().getStackInSlot(26))==32,"group withdrawal did not conserve exact physical variants");check(((LegacyWirelessContainer)serverPlayer().openContainer).rowTotal(0)==737,"server aggregate not refreshed after withdrawal");}});phase=37;waitUntilNext();return;}
            if(phase==37){check("PASS".equals(result)&&clientContainer().rowTotal(0)==737&&clientContainer().rowTotal(1)==32,"client aggregate not refreshed after withdrawal");capture(mc,"legacy-wireless-grouped-withdrawal.png");mc.playerController.windowClick(clientContainer().windowId,0,(oldRevision<<1)|1,0,mc.thePlayer);phase=38;waitUntilNext();return;}
            if(phase==38){schedule(new Runnable(){public void run(){check(iron()==95&&LegacyTile.STACKS.count(source().getStackInSlot(2))==1,"stale grouped replay mutated storage");}});phase=39;waitUntilNext();return;}
            if(phase==39){check("PASS".equals(result),"group replay check missing");mouseClick(90,214,false);phase=40;waitUntilNext();return;}
            if(phase==40){clearSearch();phase=41;waitUntilNext();return;}
            if(phase==41){if(!clientContainer().search().isEmpty())return;mouseClick(94,9,false);phase=42;waitUntilNext();return;}
            if(phase==42){schedule(new Runnable(){public void run(){check(!((LegacyWirelessContainer)serverPlayer().openContainer).grouped()&&iron()==95,"group toggle back to physical slots failed");}});phase=43;waitUntilNext();return;}
            if(phase==43){check("PASS".equals(result)&&!clientContainer().grouped(),"physical view metadata missing");groupChecked=true;phase=12;}
            if(phase==15){check(!org.lwjgl.input.Keyboard.areRepeatEventsEnabled(),"native search keyboard repeats leaked after closing");check("PASS".equals(result),"display setup failed");check(clientDisplay().displayFacing==3&&clientDisplay().displayLines[1].equals("Private"),"private native projection missing: facing="+clientDisplay().displayFacing+" lines="+java.util.Arrays.toString(clientDisplay().displayLines));capture(mc,"legacy-private-display.png");schedule(new Runnable(){public void run(){LegacyTile display=(LegacyTile)serverPlayer().worldObj.getBlockTileEntity(1,200,0);display.publishReadings=true;display.displayChanged();}});phase=16;waitUntilNext();return;}
            if(phase==16){check("PASS".equals(result),"display publication failed");check(clientDisplay().displayLines[0].equals("PL4 Network")&&clientDisplay().displayLines[1].equals("Hosts: 4"),"public native projection missing");capture(mc,"legacy-public-display.png");schedule(new Runnable(){public void run(){LegacyTile display=(LegacyTile)serverPlayer().worldObj.getBlockTileEntity(1,200,0);display.publishReadings=false;display.displayChanged();}});phase=17;waitUntilNext();return;}
            if(phase==17){check("PASS".equals(result)&&clientDisplay().displayLines[1].equals("Private"),"hiding failed to clear projected text");worldChecked=true;phase=12;}

            if(phase==44){check("PASS".equals(result),"journal setup failed");diagnosticChat.clear();mc.thePlayer.sendChatMessage("/pl4legacy journal 0 200 0");phase=45;waitUntilNext();return;}
            if(phase==45){if(!diagnosticContains(diagnosticToken))return;check(diagnosticContains("1 unresolved"),"journal chat count missing");diagnosticJournalChat=diagnosticChat.toString();capture(mc,"legacy-item-journal-chat.png");schedule(new Runnable(){public void run(){EntityPlayerMP p=serverPlayer();WorldServer world=(WorldServer)p.worldObj;LegacyTile n=(LegacyTile)world.getBlockTileEntity(0,200,0);world.setBlockToAir(0,200,0);p.inventory.currentItem=0;boolean found=false;for(Object raw:world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,net.minecraft.util.AxisAlignedBB.getBoundingBox(-3,198,-3,6,204,3))){net.minecraft.entity.item.EntityItem e=(net.minecraft.entity.item.EntityItem)raw;if(!e.isDead&&e.getEntityItem().itemID==LegacyPL4.ITEM_CELL.itemID){p.inventory.setInventorySlotContents(0,e.getEntityItem().copy());e.setDead();found=true;break;}}check(found,"linked removal receipt missing");p.inventoryContainer.detectAndSendChanges();}});phase=46;waitUntilNext();return;}
            if(phase==46){check("PASS".equals(result),"receipt setup failed");diagnosticChat.clear();mc.thePlayer.sendChatMessage("/pl4legacy receipt");phase=47;waitUntilNext();return;}
            if(phase==47){if(!diagnosticContains("recorded phase wireless"))return;check(diagnosticContains(diagnosticToken)&&diagnosticContains("preserve receipt"),"receipt chat context missing");diagnosticReceiptChat=diagnosticChat.toString();capture(mc,"legacy-item-receipt-chat.png");schedule(new Runnable(){public void run(){check(LegacyWirelessSafety.data(serverPlayer().worldObj).blocked(LegacyTile.identity(serverPlayer()),new BoundedNetwork.Point(-1,200,0)),"chat inspection released world fence");}});phase=48;waitUntilNext();return;}
            if(phase==48){check("PASS".equals(result),"read-only chat fence check failed");schedule(new Runnable(){public void run(){EntityPlayerMP p=serverPlayer();WorldServer world=(WorldServer)p.worldObj;world.setBlock(0,200,0,LegacyPL4.EXPORT.blockID,0,3);LegacyTile n=(LegacyTile)world.getBlockTileEntity(0,200,0);n.owner=LegacyTile.identity(p);n.side=4;net.minecraft.nbt.NBTTagCompound old=new net.minecraft.nbt.NBTTagCompound();n.writeToNBT(old);old.setString("ItemTransferFault","Artificial saved legacy fault for native chat inspection");old.removeTag("ItemJournalContext");n.readFromNBT(old);LegacyItemSafety.importSaved(n);check("legacy-unknown".equals(n.itemJournalContext.getString("Scope"))&&!n.itemJournalContext.hasKey("X")&&!n.itemJournalContext.hasKey("Phase"),"unknown imported history reconstructed");importToken=n.itemJournalContext.getString("Token");}});phase=49;waitUntilNext();return;}
            if(phase==49){check("PASS".equals(result),"import chat setup failed");diagnosticChat.clear();mc.thePlayer.sendChatMessage("/pl4legacy journal 0 200 0");phase=50;waitUntilNext();return;}
            if(phase==50){if(!diagnosticContains(importToken)||!diagnosticContains("original target/phase unknown"))return;check(diagnosticContains("owner-wide fence"),"import journal scope missing");importJournalChat=diagnosticChat.toString();capture(mc,"legacy-item-import-journal-chat.png");schedule(new Runnable(){public void run(){EntityPlayerMP p=serverPlayer();WorldServer world=(WorldServer)p.worldObj;p.inventory.setInventorySlotContents(9,p.inventory.getStackInSlot(0).copy());world.setBlockToAir(0,200,0);boolean found=false;for(Object raw:world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,net.minecraft.util.AxisAlignedBB.getBoundingBox(-3,198,-3,6,204,3))){net.minecraft.entity.item.EntityItem e=(net.minecraft.entity.item.EntityItem)raw;if(!e.isDead&&e.getEntityItem().itemID==LegacyPL4.ITEM_CELL.itemID){p.inventory.setInventorySlotContents(0,e.getEntityItem().copy());e.setDead();found=true;break;}}check(found,"imported removal receipt missing");p.inventoryContainer.detectAndSendChanges();}});phase=51;waitUntilNext();return;}
            if(phase==51){check("PASS".equals(result),"import receipt setup failed");diagnosticChat.clear();mc.thePlayer.sendChatMessage("/pl4legacy receipt");phase=52;waitUntilNext();return;}
            if(phase==52){if(!diagnosticContains(importToken)||!diagnosticContains("original target/phase unknown"))return;check(diagnosticContains("observed host only")&&diagnosticContains("owner-wide fence"),"import receipt uncertainty scope missing");importReceiptChat=diagnosticChat.toString();capture(mc,"legacy-item-import-receipt-chat.png");schedule(new Runnable(){public void run(){EntityPlayerMP p=serverPlayer();check(LegacyWirelessSafety.data(p.worldObj).blocked(LegacyTile.identity(p),new BoundedNetwork.Point(80,200,80)),"import chat inspection released owner barrier");}});phase=53;waitUntilNext();return;}
            if(phase==53){check("PASS".equals(result),"import chat read-only fence check failed");schedule(new Runnable(){public void run(){
                net.minecraft.world.World world=serverPlayer().worldObj;LegacyWirelessSafety journal=LegacyWirelessSafety.data(world);
                net.minecraft.nbt.NBTTagCompound raw=new net.minecraft.nbt.NBTTagCompound();raw.setInteger("Schema",99);raw.setTag("PreviousJournal",savedJournal(journal));raw.setByteArray("OriginalEvidence",new byte[]{7,8,9});journal.readFromNBT(raw);
                String[] first=LegacyWirelessSafety.opaqueExport(world,serverPlayer(),1);int pages=Integer.parseInt(first[1].substring(first[1].indexOf('/')+1,first[1].indexOf(" hex ")));
                opaqueExpected=new String[pages][];for(int i=0;i<pages;i++)opaqueExpected[i]=LegacyWirelessSafety.opaqueExport(world,serverPlayer(),i+1);
            }});phase=54;waitUntilNext();return;}
            if(phase==54){check("PASS".equals(result),"opaque export setup failed");diagnosticChat.clear();mc.thePlayer.sendChatMessage("/pl4legacy journal-export "+opaquePage);phase=55;waitUntilNext();return;}
            if(phase==55){if(diagnosticChat.size()<3)return;check(diagnosticChat.size()==3,"unexpected opaque export response");com.google.gson.JsonArray block=new com.google.gson.JsonArray();for(int i=0;i<3;i++){check(diagnosticChat.get(i).equals(opaqueExpected[opaquePage-1][i]),"native chat export changed bytes/manifest");block.add(new com.google.gson.JsonPrimitive(diagnosticChat.get(i)));}opaqueBlocks.add(block);if(opaquePage==1)capture(mc,"legacy-opaque-journal-export-chat.png");if(++opaquePage<=opaqueExpected.length){phase=54;waitUntilNext();return;}schedule(new Runnable(){public void run(){check(LegacyWirelessSafety.opaqueExport(serverPlayer().worldObj,serverPlayer(),1)[0].equals(opaqueExpected[0][0]),"chat export changed original snapshot");check(LegacyWirelessSafety.data(serverPlayer().worldObj).blocked("unrelated-owner",new BoundedNetwork.Point(80,200,80)),"chat export released opaque barrier");}});phase=56;waitUntilNext();return;}
            if(phase==56){check("PASS".equals(result),"opaque export read-only check failed");chatChecked=true;phase=12;}


            if(phase==12){check("PASS".equals(result),"page check missing");
                if(!sortChecked){oldRevision=clientContainer().revision;mouseClick(50,9,false);phase=18;waitUntilNext();return;}
                if(!searchChecked){oldRevision=clientContainer().revision;mouseClick(90,214,false);phase=24;waitUntilNext();return;}
                if(!groupChecked){oldRevision=clientContainer().revision;mouseClick(94,9,false);phase=30;waitUntilNext();return;}
                if(!worldChecked){mc.gameSettings.hideGUI=true;mc.gameSettings.pauseOnLostFocus=false;mc.thePlayer.closeScreen();schedule(new Runnable(){public void run(){setupDisplay();}});phase=15;waitUntilNext();return;}


                if(!chatChecked){mc.gameSettings.hideGUI=false;mc.gameSettings.guiScale=2;mc.thePlayer.closeScreen();mc.displayGuiScreen(new net.minecraft.client.gui.GuiChat(""));schedule(new Runnable(){public void run(){EntityPlayerMP p=serverPlayer();WorldServer world=(WorldServer)p.worldObj;LegacyTile n=(LegacyTile)world.getBlockTileEntity(0,200,0);LegacyWirelessSafety.escrow(n).block("client chat inspection fixture");diagnosticToken=n.itemJournalContext.getString("Token");}});phase=44;waitUntilNext();return;}

                com.google.gson.JsonObject report=new com.google.gson.JsonObject();report.addProperty("minecraft","1.6.4");report.addProperty("source_commit",System.getenv("PL4_CLIENT_SOURCE"));report.addProperty("result","PASS");
                report.addProperty("opaque_journal_export_round_trip",true);report.addProperty("opaque_export_chat_encoding","FORGE_1_6_4_NATIVE_JSON_TEXT_DECODED");report.add("opaque_journal_export_blocks",opaqueBlocks);report.addProperty("opaque_export_capture","legacy-opaque-journal-export-chat.png");
                report.addProperty("native_diagnostic_chat_round_trip",true);report.addProperty("diagnostic_journal_capture","legacy-item-journal-chat.png");report.addProperty("diagnostic_receipt_capture","legacy-item-receipt-chat.png");report.addProperty("diagnostic_token",diagnosticToken);report.addProperty("diagnostic_journal_messages",diagnosticJournalChat);report.addProperty("diagnostic_chat_messages",diagnosticReceiptChat);report.addProperty("native_preupgrade_import_chat_round_trip",true);report.addProperty("preupgrade_import_token",importToken);report.addProperty("preupgrade_import_journal_messages",importJournalChat);report.addProperty("preupgrade_import_receipt_messages",importReceiptChat);report.addProperty("preupgrade_import_journal_capture","legacy-item-import-journal-chat.png");report.addProperty("preupgrade_import_receipt_capture","legacy-item-import-receipt-chat.png");report.addProperty("native_wireless_withdrawal",true);report.addProperty("stale_replay_rejected",true);report.addProperty("native_wireless_deposit",true);report.addProperty("native_paging",true);report.addProperty("native_grouping_totals_variants_and_conservation",true);report.addProperty("stale_group_replay_rejected",true);report.addProperty("group_variant_capture","legacy-wireless-grouped-variants.png");report.addProperty("group_withdrawal_capture","legacy-wireless-grouped-withdrawal.png");report.addProperty("native_keyboard_repeat_lifecycle",true);report.addProperty("native_search_caret_edit",true);report.addProperty("native_search_typing_clear_no_match",true);report.addProperty("stale_search_replay_rejected",true);report.addProperty("search_capture","legacy-wireless-storage-search.png");report.addProperty("search_empty_capture","legacy-wireless-storage-search-empty.png");report.addProperty("native_sorting",true);report.addProperty("stale_sort_replay_rejected",true);report.addProperty("sorted_capture","legacy-wireless-storage-sorted.png");report.addProperty("signed_byte_revision",70);report.addProperty("native_packet_codec_round_trip",true);report.addProperty("actual_mouse_withdraw_deposit_page",true);report.addProperty("tooltip_capture","legacy-wireless-storage-tooltip.png");report.addProperty("native_display_projection_and_hide",true);com.google.gson.JsonArray displays=new com.google.gson.JsonArray();displays.add(new com.google.gson.JsonPrimitive("legacy-private-display.png"));displays.add(new com.google.gson.JsonPrimitive("legacy-public-display.png"));report.add("display_captures",displays);
                com.google.gson.JsonArray images=new com.google.gson.JsonArray();for(String name:captures)images.add(new com.google.gson.JsonPrimitive(name));report.add("captures",images);com.google.gson.JsonArray groupedImages=new com.google.gson.JsonArray();for(int i=1;i<=4;i++)groupedImages.add(new com.google.gson.JsonPrimitive("legacy-wireless-grouped-scale-"+i+".png"));report.add("grouped_captures",groupedImages);report.addProperty("scope","Actual stock Forge client with autonomous item transport disabled to isolate manual storage actions, the native-qualified packaged mod, a separate test mod, private generated integrated world, four GUI scales, real mouse actions, native window handlers/packet codecs and a private/public network-reader world display; not dedicated multiplayer, installed third-party providers or other exact Minecraft targets.");
                try(FileWriter file=new FileWriter(new File(output(),"legacy-wireless-client.json"))){new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report,file);}phase=14;mc.shutdown();
            }
        }catch(Exception error){throw new IllegalStateException("Legacy wireless client probe failed",error);}
    }
}
