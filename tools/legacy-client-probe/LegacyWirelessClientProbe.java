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
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Disposable development harness, never included in a release. Exercises native client/server window packets. */
@Mod(modid="pl4_wireless_client_probe",name="PL4 Wireless Client Probe",version="1",dependencies="required-after:foundations_pl4",clientSideOnly=true)
public final class LegacyWirelessClientProbe {
    private int opaquePage=1;private String[][] opaqueExpected;private final com.google.gson.JsonArray opaqueBlocks=new com.google.gson.JsonArray();
    private int phase,groupScale=1,scale=1,oldRevision,lastLogged=-1;private long due,started=System.nanoTime();
    private final AtomicReference<String> serverResult=new AtomicReference<String>();
    private boolean worldChecked,sortChecked,searchChecked,groupChecked,chatChecked;
    private final java.util.List<String> diagnosticChat=new java.util.concurrent.CopyOnWriteArrayList<String>();
    private String diagnosticToken,diagnosticJournalChat,diagnosticReceiptChat,importToken,importJournalChat,importReceiptChat;
    private final List<String> captures=new ArrayList<String>();
    @Mod.EventHandler public void init(FMLPreInitializationEvent event){MinecraftForge.EVENT_BUS.register(this);}

    @SubscribeEvent public void diagnosticMessage(net.minecraftforge.client.event.ClientChatReceivedEvent event){
        String text=event.getMessage().getUnformattedText();if(text.contains("PL4 item ")||text.contains("Token ")||text.startsWith("PL4 opaque export ")||text.startsWith("Page ")||text.startsWith("Original target/phase unknown."))diagnosticChat.add(text);
    }
    private boolean diagnosticContains(String text){for(String line:diagnosticChat)if(line.contains(text))return true;return false;}
    private File output(){File out=new File(System.getenv("PL4_CLIENT_OUTPUT"));out.mkdirs();return out;}
    private void schedule(final Runnable action){serverResult.set(null);Minecraft.getMinecraft().getIntegratedServer().addScheduledTask(new Runnable(){public void run(){try{action.run();serverResult.set("PASS");}catch(Throwable error){serverResult.set(error.toString());error.printStackTrace();}}});}
    private EntityPlayerMP serverPlayer(){Minecraft mc=Minecraft.getMinecraft();return mc.getIntegratedServer().getPlayerList().getPlayerByUUID(mc.player.getUniqueID());}
    private void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private TileEntityChest source(){return (TileEntityChest)serverPlayer().getServerWorld().getTileEntity(new BlockPos(-1,200,0));}
    private int diamonds(){EntityPlayerMP p=serverPlayer();int n=0;for(int i=0;i<36;i++){ItemStack s=p.inventory.getStackInSlot(i);if(!s.isEmpty()&&s.getItem()==Items.DIAMOND)n+=s.getCount();}return n;}
    private void setup(){
        EntityPlayerMP player=serverPlayer();WorldServer world=player.getServerWorld();world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("announceAdvancements","false");
        for(int x=-2;x<=4;x++)for(int z=-2;z<=5;z++)world.setBlockState(new BlockPos(x,198,z),Blocks.STONE.getDefaultState(),3);
        world.setBlockState(new BlockPos(0,200,0),LegacyPL4.EXPORT.getDefaultState(),3);world.setBlockState(new BlockPos(1,200,0),LegacyPL4.CABLE.getDefaultState(),3);world.setBlockState(new BlockPos(2,200,0),LegacyPL4.IMPORT.getDefaultState(),3);
        for(int x=0;x<3;x++){LegacyTile tile=(LegacyTile)world.getTileEntity(new BlockPos(x,200,0));tile.owner=player.getUniqueID().toString();tile.side=x==0?4:5;tile.markDirty();}
        world.setBlockState(new BlockPos(-1,200,0),Blocks.CHEST.getDefaultState(),3);world.setBlockState(new BlockPos(3,200,0),Blocks.CHEST.getDefaultState(),3);
        TileEntityChest destination=(TileEntityChest)world.getTileEntity(new BlockPos(3,200,0));
        for(int i=0;i<27;i++){source().setInventorySlotContents(i,new ItemStack(Items.IRON_INGOT,32));destination.setInventorySlotContents(i,new ItemStack(Items.GOLD_INGOT,32));}
        source().setInventorySlotContents(0,new ItemStack(Items.DIAMOND,64));
        player.inventory.clear();player.inventory.currentItem=0;player.inventory.setInventorySlotContents(0,new ItemStack(LegacyPL4.WIRELESS_STORAGE));player.inventory.setInventorySlotContents(9,new ItemStack(Items.IRON_INGOT,32));
        player.connection.setPlayerLocation(.5,199,3.5,180,0);LegacyPL4.enabled=false;LegacyPL4.itemRate=0; // Isolate manual window actions from autonomous node transfers.
        check(LegacyWirelessStorage.bind(player,player.inventory.getCurrentItem(),(LegacyTile)world.getTileEntity(new BlockPos(0,200,0))),"binding failed");
        player.openGui(LegacyPL4.instance,0,world,0,0,0);
    }
    private void setupDisplay(){
        EntityPlayerMP p=serverPlayer();WorldServer world=p.getServerWorld();
        world.setBlockState(new BlockPos(1,200,0),LegacyPL4.DISPLAY.getDefaultState(),3);world.setBlockState(new BlockPos(1,200,-1),LegacyPL4.NETWORK_READER.getDefaultState(),3);
        LegacyTile display=(LegacyTile)world.getTileEntity(new BlockPos(1,200,0)),reader=(LegacyTile)world.getTileEntity(new BlockPos(1,200,-1));
        display.owner=p.getUniqueID().toString();reader.owner=p.getUniqueID().toString();reader.markDirty();display.displayFacing=3;display.publishReadings=false;display.displayChanged();
        p.connection.setPlayerLocation(1.5,199,2.2,180,0);
    }
    private LegacyTile clientDisplay(){return (LegacyTile)Minecraft.getMinecraft().world.getTileEntity(new BlockPos(1,200,0));}
    private void capture(Minecraft mc,String name)throws Exception{ImageIO.write(ScreenShotHelper.createScreenshot(mc.displayWidth,mc.displayHeight,mc.getFramebuffer()),"png",new File(output(),name));}
    private void mouseClick(int logicalX,int logicalY,boolean right)throws Exception{
        Minecraft mc=Minecraft.getMinecraft();ScaledResolution resolution=new ScaledResolution(mc);int factor=resolution.getScaleFactor();
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
    private int iron(){EntityPlayerMP p=serverPlayer();int n=0;for(int i=0;i<36;i++){ItemStack stack=p.inventory.getStackInSlot(i);if(LegacyTile.STACKS.same(new ItemStack(Items.IRON_INGOT),stack))n+=LegacyTile.STACKS.count(stack);}return n;}
    private void groupResize(Minecraft mc)throws Exception{
        mc.gameSettings.guiScale=groupScale;mc.resize(mc.displayWidth,mc.displayHeight);
        new java.awt.Robot().mouseMove(8,mc.displayHeight-8);
        if(groupScale==4){ScaledResolution view=new ScaledResolution(mc);int factor=view.getScaleFactor();new java.awt.Robot().mouseMove(((view.getScaledWidth()-176)/2+16)*factor,((view.getScaledHeight()-224)/2+26)*factor);}
    }
    private void packetRoundTrip()throws Exception{
        net.minecraft.network.PacketBuffer buffer=new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
        try{
            net.minecraft.network.play.client.CPacketClickWindow sent=new net.minecraft.network.play.client.CPacketClickWindow(1,0,141,ClickType.PICKUP,ItemStack.EMPTY,(short)7);
            sent.writePacketData(buffer);net.minecraft.network.play.client.CPacketClickWindow received=new net.minecraft.network.play.client.CPacketClickWindow();received.readPacketData(buffer);
            check((received.getUsedButton()&255)==141&&((received.getUsedButton()&255)>>>1)==70,"window packet revision round-trip failed");
            buffer.clear();net.minecraft.network.play.client.CPacketEnchantItem page=new net.minecraft.network.play.client.CPacketEnchantItem(1,141);page.writePacketData(buffer);
            net.minecraft.network.play.client.CPacketEnchantItem decoded=new net.minecraft.network.play.client.CPacketEnchantItem();decoded.readPacketData(buffer);check((decoded.getButton()&255)==141,"page packet revision round-trip failed");
        }finally{buffer.release();}
    }
    private LegacyWirelessContainer clientContainer(){return (LegacyWirelessContainer)Minecraft.getMinecraft().player.openContainer;}
    private void waitUntilNext(){due=System.nanoTime()+2_000_000_000L;}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getMinecraft();
        try{
            if(lastLogged!=phase){lastLogged=phase;System.out.println("PL4 WIRELESS CLIENT PHASE: "+phase);}
            if(System.nanoTime()-started>300_000_000_000L){capture(mc,"legacy-wireless-timeout.png");throw new IllegalStateException("Timed out in probe phase "+phase+(mc.currentScreen instanceof LegacyWirelessScreen?searchDebug():""));}
            String result=serverResult.get();if(result!=null&&!"PASS".equals(result))throw new IllegalStateException(result);
            if(System.nanoTime()<due)return;
            if(phase==0){if(!(mc.currentScreen instanceof GuiMainMenu))return;phase=1;mc.launchIntegratedServer("pl4-wireless-client-probe","PL4 Wireless Probe",new WorldSettings(0L,GameType.CREATIVE,false,false,WorldType.FLAT).enableCommands());waitUntilNext();return;}
            if(phase==1){if(mc.player==null||mc.world==null||mc.getIntegratedServer()==null)return;schedule(new Runnable(){public void run(){setup();}});phase=2;waitUntilNext();return;}
            if(phase==2){if(!(mc.currentScreen instanceof LegacyWirelessScreen)||!"PASS".equals(result))return;
                mc.gameSettings.guiScale=scale;mc.resize(mc.displayWidth,mc.displayHeight);new java.awt.Robot().mouseMove(8,mc.displayHeight-8);phase=3;waitUntilNext();return;}
            if(phase==3){
                ScaledResolution resolution=new ScaledResolution(mc);check(resolution.getScaleFactor()==scale,"wrong actual GUI scale");
                String name="legacy-wireless-storage-scale-"+scale+".png";ImageIO.write(ScreenShotHelper.createScreenshot(mc.displayWidth,mc.displayHeight,mc.getFramebuffer()),"png",new File(output(),name));captures.add(name);
                if(++scale<=4){phase=2;return;}
                ScaledResolution hover=new ScaledResolution(mc);int factor=hover.getScaleFactor();
                new java.awt.Robot().mouseMove(((hover.getScaledWidth()-176)/2+16)*factor,((hover.getScaledHeight()-224)/2+26)*factor);
                phase=13;waitUntilNext();return;
            }
            if(phase==13){
                ImageIO.write(ScreenShotHelper.createScreenshot(mc.displayWidth,mc.displayHeight,mc.getFramebuffer()),"png",new File(output(),"legacy-wireless-storage-tooltip.png"));
                schedule(new Runnable(){public void run(){LegacyPL4.enabled=true;LegacyWirelessContainer c=(LegacyWirelessContainer)serverPlayer().openContainer;c.revision=70;c.detectAndSendChanges();}});phase=4;waitUntilNext();return;
            }
            if(phase==4){if(clientContainer().revision!=70)return;packetRoundTrip();oldRevision=clientContainer().revision;
                mouseClick(16,26,true);phase=5;waitUntilNext();return;}
            if(phase==5){schedule(new Runnable(){public void run(){check(diamonds()==1&&source().getStackInSlot(0).getCount()==63,"native signed-byte withdrawal failed");}});phase=6;waitUntilNext();return;}
            if(phase==6){check("PASS".equals(result),"withdrawal check missing");mc.playerController.windowClick(clientContainer().windowId,0,(oldRevision<<1)|1,ClickType.PICKUP,mc.player);phase=7;waitUntilNext();return;}
            if(phase==7){schedule(new Runnable(){public void run(){check(diamonds()==1&&source().getStackInSlot(0).getCount()==63,"stale replay mutated storage");}});phase=8;waitUntilNext();return;}
            if(phase==8){check("PASS".equals(result),"replay check missing");mouseClick(16,130,true);phase=9;waitUntilNext();return;}
            if(phase==9){schedule(new Runnable(){public void run(){check(serverPlayer().inventory.getStackInSlot(9).getCount()==31&&source().getStackInSlot(1).getCount()==33,"native deposit failed");}});phase=10;waitUntilNext();return;}
            if(phase==10){check("PASS".equals(result),"deposit check missing");mouseClick(157,9,false);phase=11;waitUntilNext();return;}
            if(phase==11){schedule(new Runnable(){public void run(){check(((LegacyWirelessContainer)serverPlayer().openContainer).page==1,"native page request failed");}});phase=12;waitUntilNext();return;}
            if(phase==18){schedule(new Runnable(){public void run(){LegacyWirelessContainer c=(LegacyWirelessContainer)serverPlayer().openContainer;check(c.sortOrder()==1&&c.page==0,"native name sort/reset failed");}});phase=19;waitUntilNext();return;}
            if(phase==19){check("PASS".equals(result)&&clientContainer().sortOrder()==1,"client sort metadata missing");check(clientContainer().getSlot(1).getStack().getItem()==Items.GOLD_INGOT,"actual client name ordering mismatch");mc.playerController.windowClick(clientContainer().windowId,0,(oldRevision<<1)|1,ClickType.PICKUP,mc.player);phase=20;waitUntilNext();return;}
            if(phase==20){schedule(new Runnable(){public void run(){check(diamonds()==1&&source().getStackInSlot(0).getCount()==63,"pre-sort replay mutated storage");}});phase=21;waitUntilNext();return;}
            if(phase==21){check("PASS".equals(result),"sort replay check missing");mouseClick(50,9,false);phase=22;waitUntilNext();return;}
            if(phase==22){schedule(new Runnable(){public void run(){check(((LegacyWirelessContainer)serverPlayer().openContainer).sortOrder()==2,"native count sort failed");}});phase=23;waitUntilNext();return;}
            if(phase==23){check("PASS".equals(result)&&clientContainer().sortOrder()==2,"client count sort metadata missing");check(clientContainer().getSlot(1).getStack().getCount()==33,"actual client count ordering mismatch");capture(mc,"legacy-wireless-storage-sorted.png");sortChecked=true;phase=12;}
            if(phase==24){check(org.lwjgl.input.Keyboard.areRepeatEventsEnabled(),"native search keyboard repeats disabled");typeSearch("gold");phase=25;waitUntilNext();return;}
            if(phase==25){
                if(!clientContainer().search().equals("gold"))return;
                check(clientContainer().page==0&&clientContainer().getSlot(0).getStack().getItem()==Items.GOLD_INGOT,"actual filtered client rows mismatch");
                check(clientContainer().getSlot(27).getStack().isEmpty(),"search retained nonmatching or duplicate rows");
                capture(mc,"legacy-wireless-storage-search.png");
                mc.playerController.windowClick(clientContainer().windowId,0,(oldRevision<<1)|1,ClickType.PICKUP,mc.player);
                schedule(new Runnable(){public void run(){LegacyWirelessContainer c=(LegacyWirelessContainer)serverPlayer().openContainer;check(c.search().equals("gold")&&c.page==0&&diamonds()==1&&source().getStackInSlot(0).getCount()==63,"search replay/fence failed");}});
                phase=26;waitUntilNext();return;
            }
            if(phase==26){check("PASS".equals(result),"search server assertion missing");java.awt.Robot edit=new java.awt.Robot();for(int key:new int[]{java.awt.event.KeyEvent.VK_HOME,java.awt.event.KeyEvent.VK_RIGHT,java.awt.event.KeyEvent.VK_DELETE}){edit.keyPress(key);edit.keyRelease(key);}typeSearch("o");phase=29;waitUntilNext();return;}
            if(phase==29){check(clientContainer().search().equals("gold"),"middle-of-query caret edit failed");clearSearch();typeSearch("xyz");phase=27;waitUntilNext();return;}
            if(phase==27){if(!clientContainer().search().equals("xyz"))return;check(clientContainer().getSlot(0).getStack().isEmpty(),"no-match search exposed a fallback row");capture(mc,"legacy-wireless-storage-search-empty.png");clearSearch();phase=28;waitUntilNext();return;}
            if(phase==28){if(!clientContainer().search().isEmpty())return;check(clientContainer().getSlot(0).getStack().getCount()==63,"clearing search failed to restore count sort");searchChecked=true;phase=12;}
            if(phase==30){schedule(new Runnable(){public void run(){LegacyWirelessContainer c=(LegacyWirelessContainer)serverPlayer().openContainer;check(c.grouped()&&c.page==0&&c.rowTotal(0)==864&&c.rowTotal(1)==833&&c.rowTotal(2)==63,"native aggregate grouping/reset failed");}});phase=31;waitUntilNext();return;}
            if(phase==31){check("PASS".equals(result)&&clientContainer().grouped()&&clientContainer().rowTotal(0)==864&&clientContainer().rowTotal(1)==833&&clientContainer().rowTotal(2)==63,"native grouped totals missing on actual client");check(LegacyTile.STACKS.count(clientContainer().getSlot(0).getStack())==1,"oversized grouped count sent in native stack");groupScale=1;groupResize(mc);phase=32;waitUntilNext();return;}
            if(phase==32){capture(mc,"legacy-wireless-grouped-scale-"+groupScale+".png");if(++groupScale<=4){groupResize(mc);waitUntilNext();return;}schedule(new Runnable(){public void run(){ItemStack variant=source().getStackInSlot(26);net.minecraft.nbt.NBTTagCompound tag=new net.minecraft.nbt.NBTTagCompound();tag.setString("ProbeVariant","B");variant.setTagCompound(tag);source().markDirty();((LegacyWirelessContainer)serverPlayer().openContainer).detectAndSendChanges();}});phase=33;waitUntilNext();return;}
            if(phase==33){check("PASS".equals(result)&&clientContainer().rowTotal(1)==801&&clientContainer().rowTotal(3)==32,"full-NBT variant was merged");check(!ItemStack.areItemStackTagsEqual(clientContainer().getSlot(1).getStack(),clientContainer().getSlot(3).getStack()),"client variant previews lost NBT");capture(mc,"legacy-wireless-grouped-variants.png");mouseClick(90,214,false);phase=34;waitUntilNext();return;}
            if(phase==34){typeSearch("iron");phase=35;waitUntilNext();return;}
            if(phase==35){if(!clientContainer().search().equals("iron"))return;check(clientContainer().rowTotal(0)==801&&clientContainer().rowTotal(1)==32,"grouping and search totals disagree");oldRevision=clientContainer().revision;mouseClick(16,26,false);phase=36;waitUntilNext();return;}
            if(phase==36){schedule(new Runnable(){public void run(){check(iron()==95&&LegacyTile.STACKS.count(source().getStackInSlot(1))==0&&LegacyTile.STACKS.count(source().getStackInSlot(2))==1&&LegacyTile.STACKS.count(source().getStackInSlot(26))==32,"group withdrawal did not conserve exact physical variants");check(((LegacyWirelessContainer)serverPlayer().openContainer).rowTotal(0)==737,"server aggregate not refreshed after withdrawal");}});phase=37;waitUntilNext();return;}
            if(phase==37){check("PASS".equals(result)&&clientContainer().rowTotal(0)==737&&clientContainer().rowTotal(1)==32,"client aggregate not refreshed after withdrawal");capture(mc,"legacy-wireless-grouped-withdrawal.png");mc.playerController.windowClick(clientContainer().windowId,0,(oldRevision<<1)|1,ClickType.PICKUP,mc.player);phase=38;waitUntilNext();return;}
            if(phase==38){schedule(new Runnable(){public void run(){check(iron()==95&&LegacyTile.STACKS.count(source().getStackInSlot(2))==1,"stale grouped replay mutated storage");}});phase=39;waitUntilNext();return;}
            if(phase==39){check("PASS".equals(result),"group replay check missing");mouseClick(90,214,false);phase=40;waitUntilNext();return;}
            if(phase==40){clearSearch();phase=41;waitUntilNext();return;}
            if(phase==41){if(!clientContainer().search().isEmpty())return;mouseClick(94,9,false);phase=42;waitUntilNext();return;}
            if(phase==42){schedule(new Runnable(){public void run(){check(!((LegacyWirelessContainer)serverPlayer().openContainer).grouped()&&iron()==95,"group toggle back to physical slots failed");}});phase=43;waitUntilNext();return;}
            if(phase==43){check("PASS".equals(result)&&!clientContainer().grouped(),"physical view metadata missing");groupChecked=true;phase=12;}
            if(phase==15){check(!org.lwjgl.input.Keyboard.areRepeatEventsEnabled(),"native search keyboard repeats leaked after closing");mc.getToastGui().clear();check("PASS".equals(result),"display setup failed");check(clientDisplay().displayFacing==3&&clientDisplay().displayLines[1].equals("Private"),"private native projection missing: facing="+clientDisplay().displayFacing+" lines="+java.util.Arrays.toString(clientDisplay().displayLines));capture(mc,"legacy-private-display.png");schedule(new Runnable(){public void run(){LegacyTile display=(LegacyTile)serverPlayer().getServerWorld().getTileEntity(new BlockPos(1,200,0));display.publishReadings=true;display.displayChanged();}});phase=16;waitUntilNext();return;}
            if(phase==16){mc.getToastGui().clear();check("PASS".equals(result),"display publication failed");check(clientDisplay().displayLines[0].equals("PL4 Network")&&clientDisplay().displayLines[1].equals("Hosts: 4"),"public native projection missing");capture(mc,"legacy-public-display.png");schedule(new Runnable(){public void run(){LegacyTile display=(LegacyTile)serverPlayer().getServerWorld().getTileEntity(new BlockPos(1,200,0));display.publishReadings=false;display.displayChanged();}});phase=17;waitUntilNext();return;}
            if(phase==17){check("PASS".equals(result)&&clientDisplay().displayLines[1].equals("Private"),"hiding failed to clear projected text");worldChecked=true;phase=12;}

            if(phase==44){check("PASS".equals(result),"journal setup failed");diagnosticChat.clear();mc.player.sendChatMessage("/pl4legacy journal 0 200 0");phase=45;waitUntilNext();return;}
            if(phase==45){if(!diagnosticContains(diagnosticToken))return;check(diagnosticContains("1 unresolved"),"journal chat count missing");diagnosticJournalChat=diagnosticChat.toString();capture(mc,"legacy-item-journal-chat.png");schedule(new Runnable(){public void run(){EntityPlayerMP p=serverPlayer();WorldServer world=p.getServerWorld();LegacyTile n=(LegacyTile)world.getTileEntity(new BlockPos(0,200,0));world.setBlockToAir(new BlockPos(0,200,0));p.inventory.currentItem=0;boolean found=false;for(net.minecraft.entity.item.EntityItem e:world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,new net.minecraft.util.math.AxisAlignedBB(-3,198,-3,6,204,3)))if(!e.isDead&&e.getItem().getItem()==LegacyPL4.ITEM_CELL){p.inventory.setInventorySlotContents(0,e.getItem().copy());e.setDead();found=true;break;}check(found,"linked removal receipt missing");p.inventoryContainer.detectAndSendChanges();}});phase=46;waitUntilNext();return;}
            if(phase==46){check("PASS".equals(result),"receipt setup failed");diagnosticChat.clear();mc.player.sendChatMessage("/pl4legacy receipt");phase=47;waitUntilNext();return;}
            if(phase==47){if(!diagnosticContains("recorded phase wireless"))return;check(diagnosticContains(diagnosticToken)&&diagnosticContains("preserve receipt"),"receipt chat context missing");diagnosticReceiptChat=diagnosticChat.toString();capture(mc,"legacy-item-receipt-chat.png");schedule(new Runnable(){public void run(){check(LegacyWirelessSafety.data(serverPlayer().getServerWorld()).blocked(serverPlayer().getUniqueID().toString(),new BlockPos(-1,200,0)),"chat inspection released world fence");}});phase=48;waitUntilNext();return;}
            if(phase==48){check("PASS".equals(result),"read-only chat fence check failed");schedule(new Runnable(){public void run(){EntityPlayerMP p=serverPlayer();WorldServer world=p.getServerWorld();world.setBlockState(new BlockPos(0,200,0),LegacyPL4.EXPORT.getDefaultState(),3);LegacyTile n=(LegacyTile)world.getTileEntity(new BlockPos(0,200,0));n.owner=p.getUniqueID().toString();n.side=4;net.minecraft.nbt.NBTTagCompound old=new net.minecraft.nbt.NBTTagCompound();n.writeToNBT(old);old.setString("ItemTransferFault","Artificial saved legacy fault for native chat inspection");old.removeTag("ItemJournalContext");n.readFromNBT(old);LegacyItemSafety.importSaved(n);check("legacy-unknown".equals(n.itemJournalContext.getString("Scope"))&&!n.itemJournalContext.hasKey("X")&&!n.itemJournalContext.hasKey("Phase"),"unknown imported history reconstructed");importToken=n.itemJournalContext.getString("Token");}});phase=49;waitUntilNext();return;}
            if(phase==49){check("PASS".equals(result),"import chat setup failed");diagnosticChat.clear();mc.player.sendChatMessage("/pl4legacy journal 0 200 0");phase=50;waitUntilNext();return;}
            if(phase==50){if(!diagnosticContains(importToken)||!diagnosticContains("original target/phase unknown"))return;check(diagnosticContains("owner-wide fence"),"import journal scope missing");importJournalChat=diagnosticChat.toString();capture(mc,"legacy-item-import-journal-chat.png");schedule(new Runnable(){public void run(){EntityPlayerMP p=serverPlayer();WorldServer world=p.getServerWorld();p.inventory.setInventorySlotContents(9,p.inventory.getStackInSlot(0).copy());world.setBlockToAir(new BlockPos(0,200,0));boolean found=false;for(net.minecraft.entity.item.EntityItem e:world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,new net.minecraft.util.math.AxisAlignedBB(-3,198,-3,6,204,3))){if(!e.isDead&&e.getItem().getItem()==LegacyPL4.ITEM_CELL){p.inventory.setInventorySlotContents(0,e.getItem().copy());e.setDead();found=true;break;}}check(found,"imported removal receipt missing");p.inventoryContainer.detectAndSendChanges();}});phase=51;waitUntilNext();return;}
            if(phase==51){check("PASS".equals(result),"import receipt setup failed");diagnosticChat.clear();mc.player.sendChatMessage("/pl4legacy receipt");phase=52;waitUntilNext();return;}
            if(phase==52){if(!diagnosticContains(importToken)||!diagnosticContains("original target/phase unknown"))return;check(diagnosticContains("observed host only")&&diagnosticContains("owner-wide fence"),"import receipt uncertainty scope missing");importReceiptChat=diagnosticChat.toString();capture(mc,"legacy-item-import-receipt-chat.png");schedule(new Runnable(){public void run(){EntityPlayerMP p=serverPlayer();check(LegacyWirelessSafety.data(p.getServerWorld()).blocked(p.getUniqueID().toString(),new BlockPos(80,200,80)),"import chat inspection released owner barrier");}});phase=53;waitUntilNext();return;}
            if(phase==53){check("PASS".equals(result),"import chat read-only fence check failed");schedule(new Runnable(){public void run(){
                net.minecraft.world.World world=serverPlayer().getServerWorld();LegacyWirelessSafety journal=LegacyWirelessSafety.data(world);
                net.minecraft.nbt.NBTTagCompound raw=new net.minecraft.nbt.NBTTagCompound();raw.setInteger("Schema",99);raw.setTag("PreviousJournal",journal.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));raw.setByteArray("OriginalEvidence",new byte[]{7,8,9});journal.readFromNBT(raw);
                String[] first=LegacyWirelessSafety.opaqueExport(world,serverPlayer(),1);int pages=Integer.parseInt(first[1].substring(first[1].indexOf('/')+1,first[1].indexOf(" hex ")));
                opaqueExpected=new String[pages][];for(int i=0;i<pages;i++)opaqueExpected[i]=LegacyWirelessSafety.opaqueExport(world,serverPlayer(),i+1);
            }});phase=54;waitUntilNext();return;}
            if(phase==54){check("PASS".equals(result),"opaque export setup failed");diagnosticChat.clear();mc.player.sendChatMessage("/pl4legacy journal-export "+opaquePage);phase=55;waitUntilNext();return;}
            if(phase==55){if(diagnosticChat.size()<3)return;check(diagnosticChat.size()==3,"unexpected opaque export response");com.google.gson.JsonArray block=new com.google.gson.JsonArray();for(int i=0;i<3;i++){check(diagnosticChat.get(i).equals(opaqueExpected[opaquePage-1][i]),"native chat export changed bytes/manifest");block.add(new com.google.gson.JsonPrimitive(diagnosticChat.get(i)));}opaqueBlocks.add(block);if(opaquePage==1)capture(mc,"legacy-opaque-journal-export-chat.png");if(++opaquePage<=opaqueExpected.length){phase=54;waitUntilNext();return;}schedule(new Runnable(){public void run(){check(LegacyWirelessSafety.opaqueExport(serverPlayer().getServerWorld(),serverPlayer(),1)[0].equals(opaqueExpected[0][0]),"chat export changed original snapshot");check(LegacyWirelessSafety.data(serverPlayer().getServerWorld()).blocked("unrelated-owner",new BlockPos(80,200,80)),"chat export released opaque barrier");}});phase=56;waitUntilNext();return;}
            if(phase==56){check("PASS".equals(result),"opaque export read-only check failed");chatChecked=true;phase=12;}


            if(phase==12){check("PASS".equals(result),"page check missing");
                if(!sortChecked){oldRevision=clientContainer().revision;mouseClick(50,9,false);phase=18;waitUntilNext();return;}
                if(!searchChecked){oldRevision=clientContainer().revision;mouseClick(90,214,false);phase=24;waitUntilNext();return;}
                if(!groupChecked){oldRevision=clientContainer().revision;mouseClick(94,9,false);phase=30;waitUntilNext();return;}
                if(!worldChecked){mc.gameSettings.hideGUI=true;mc.gameSettings.pauseOnLostFocus=false;mc.player.closeScreen();schedule(new Runnable(){public void run(){setupDisplay();}});phase=15;waitUntilNext();return;}


                if(!chatChecked){mc.gameSettings.hideGUI=false;mc.gameSettings.guiScale=2;mc.player.closeScreen();mc.displayGuiScreen(new net.minecraft.client.gui.GuiChat(""));schedule(new Runnable(){public void run(){EntityPlayerMP p=serverPlayer();WorldServer world=p.getServerWorld();LegacyTile n=(LegacyTile)world.getTileEntity(new BlockPos(0,200,0));LegacyWirelessSafety.escrow(n).block("client chat inspection fixture");diagnosticToken=n.itemJournalContext.getString("Token");}});phase=44;waitUntilNext();return;}

                com.google.gson.JsonObject report=new com.google.gson.JsonObject();report.addProperty("minecraft","1.12.2");report.addProperty("source_commit",System.getenv("PL4_CLIENT_SOURCE"));report.addProperty("result","PASS");
                report.addProperty("opaque_journal_export_round_trip",true);report.add("opaque_journal_export_blocks",opaqueBlocks);report.addProperty("opaque_export_capture","legacy-opaque-journal-export-chat.png");
                report.addProperty("native_diagnostic_chat_round_trip",true);report.addProperty("diagnostic_journal_capture","legacy-item-journal-chat.png");report.addProperty("diagnostic_receipt_capture","legacy-item-receipt-chat.png");report.addProperty("diagnostic_token",diagnosticToken);report.addProperty("diagnostic_journal_messages",diagnosticJournalChat);report.addProperty("diagnostic_chat_messages",diagnosticReceiptChat);report.addProperty("native_preupgrade_import_chat_round_trip",true);report.addProperty("preupgrade_import_token",importToken);report.addProperty("preupgrade_import_journal_messages",importJournalChat);report.addProperty("preupgrade_import_receipt_messages",importReceiptChat);report.addProperty("preupgrade_import_journal_capture","legacy-item-import-journal-chat.png");report.addProperty("preupgrade_import_receipt_capture","legacy-item-import-receipt-chat.png");report.addProperty("native_wireless_withdrawal",true);report.addProperty("stale_replay_rejected",true);report.addProperty("native_wireless_deposit",true);report.addProperty("native_paging",true);report.addProperty("native_grouping_totals_variants_and_conservation",true);report.addProperty("stale_group_replay_rejected",true);report.addProperty("group_variant_capture","legacy-wireless-grouped-variants.png");report.addProperty("group_withdrawal_capture","legacy-wireless-grouped-withdrawal.png");report.addProperty("native_keyboard_repeat_lifecycle",true);report.addProperty("native_search_caret_edit",true);report.addProperty("native_search_typing_clear_no_match",true);report.addProperty("stale_search_replay_rejected",true);report.addProperty("search_capture","legacy-wireless-storage-search.png");report.addProperty("search_empty_capture","legacy-wireless-storage-search-empty.png");report.addProperty("native_sorting",true);report.addProperty("stale_sort_replay_rejected",true);report.addProperty("sorted_capture","legacy-wireless-storage-sorted.png");report.addProperty("signed_byte_revision",70);report.addProperty("native_packet_codec_round_trip",true);report.addProperty("actual_mouse_withdraw_deposit_page",true);report.addProperty("tooltip_capture","legacy-wireless-storage-tooltip.png");report.addProperty("native_display_projection_and_hide",true);com.google.gson.JsonArray displays=new com.google.gson.JsonArray();displays.add("legacy-private-display.png");displays.add("legacy-public-display.png");report.add("display_captures",displays);
                com.google.gson.JsonArray images=new com.google.gson.JsonArray();for(String name:captures)images.add(name);report.add("captures",images);com.google.gson.JsonArray groupedImages=new com.google.gson.JsonArray();for(int i=1;i<=4;i++)groupedImages.add("legacy-wireless-grouped-scale-"+i+".png");report.add("grouped_captures",groupedImages);report.addProperty("scope","Actual stock Forge client with autonomous item transport disabled to isolate manual storage actions, the native-qualified packaged mod, a separate test mod, private generated integrated world, four GUI scales, real mouse actions, native window handlers/packet codecs and a private/public network-reader world display; not dedicated multiplayer, installed third-party providers or 1.6.4 client acceptance.");
                try(FileWriter file=new FileWriter(new File(output(),"legacy-wireless-client.json"))){new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report,file);}phase=14;mc.shutdown();
            }
        }catch(Exception error){throw new IllegalStateException("Legacy wireless client probe failed",error);}
    }
}
