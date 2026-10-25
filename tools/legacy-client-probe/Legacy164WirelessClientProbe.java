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
    private int phase,scale=1,oldRevision,lastLogged=-1;private long due,started=System.nanoTime();
    private final AtomicReference<String> serverResult=new AtomicReference<String>();
    private boolean worldChecked,sortChecked;
    private final List<String> captures=new ArrayList<String>();
    @Mod.EventHandler public void init(FMLPreInitializationEvent event){TickRegistry.registerTickHandler(this,Side.CLIENT);TickRegistry.registerTickHandler(this,Side.SERVER);}
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
        int left=(resolution.getScaledWidth()-176)/2,top=(resolution.getScaledHeight()-204)/2;
        java.awt.Robot robot=new java.awt.Robot();robot.mouseMove((left+logicalX)*factor,(top+logicalY)*factor);
        int button=right?java.awt.event.InputEvent.BUTTON3_DOWN_MASK:java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
        robot.mousePress(button);robot.mouseRelease(button);
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
            if(System.nanoTime()-started>300_000_000_000L)throw new IllegalStateException("Timed out in probe phase "+phase);
            String result=serverResult.get();if(result!=null&&!"PASS".equals(result))throw new IllegalStateException(result);
            if(System.nanoTime()<due)return;
            if(phase==0){if(!(mc.currentScreen instanceof GuiMainMenu))return;phase=1;mc.launchIntegratedServer("pl4-wireless-client-probe","PL4 Wireless Probe",new WorldSettings(0L,EnumGameType.CREATIVE,false,false,WorldType.FLAT));waitUntilNext();return;}
            if(phase==1){if(mc.thePlayer==null||mc.theWorld==null||mc.getIntegratedServer()==null)return;schedule(new Runnable(){public void run(){setup();}});phase=2;waitUntilNext();return;}
            if(phase==2){if(!(mc.currentScreen instanceof LegacyWirelessScreen)||!"PASS".equals(result))return;
                mc.gameSettings.guiScale=scale;ScaledResolution resized=new ScaledResolution(mc.gameSettings,mc.displayWidth,mc.displayHeight);mc.currentScreen.setWorldAndResolution(mc,resized.getScaledWidth(),resized.getScaledHeight());new java.awt.Robot().mouseMove(8,mc.displayHeight-8);phase=3;waitUntilNext();return;}
            if(phase==3){
                ScaledResolution resolution=new ScaledResolution(mc.gameSettings,mc.displayWidth,mc.displayHeight);check(resolution.getScaleFactor()==scale,"wrong actual GUI scale");
                String name="legacy-wireless-storage-scale-"+scale+".png";capture(mc,name);captures.add(name);
                if(++scale<=4){phase=2;return;}
                ScaledResolution hover=new ScaledResolution(mc.gameSettings,mc.displayWidth,mc.displayHeight);int factor=hover.getScaleFactor();
                new java.awt.Robot().mouseMove(((hover.getScaledWidth()-176)/2+16)*factor,((hover.getScaledHeight()-204)/2+26)*factor);
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
            if(phase==20){schedule(new Runnable(){public void run(){check(diamonds()==1&&source().getStackInSlot(0).stackSize==63,"pre-sort replay mutated storage");}});phase=21;waitUntilNext();return;}
            if(phase==21){check("PASS".equals(result),"sort replay check missing");mouseClick(50,9,false);phase=22;waitUntilNext();return;}
            if(phase==22){schedule(new Runnable(){public void run(){check(((LegacyWirelessContainer)serverPlayer().openContainer).sortOrder()==2,"native count sort failed");}});phase=23;waitUntilNext();return;}
            if(phase==23){check("PASS".equals(result)&&clientContainer().sortOrder()==2,"client count sort metadata missing");check(clientContainer().getSlot(1).getStack().stackSize==33,"actual client count ordering mismatch");capture(mc,"legacy-wireless-storage-sorted.png");sortChecked=true;phase=12;}
            if(phase==15){check("PASS".equals(result),"display setup failed");check(clientDisplay().displayFacing==3&&clientDisplay().displayLines[1].equals("Private"),"private native projection missing: facing="+clientDisplay().displayFacing+" lines="+java.util.Arrays.toString(clientDisplay().displayLines));capture(mc,"legacy-private-display.png");schedule(new Runnable(){public void run(){LegacyTile display=(LegacyTile)serverPlayer().worldObj.getBlockTileEntity(1,200,0);display.publishReadings=true;display.displayChanged();}});phase=16;waitUntilNext();return;}
            if(phase==16){check("PASS".equals(result),"display publication failed");check(clientDisplay().displayLines[0].equals("PL4 Network")&&clientDisplay().displayLines[1].equals("Hosts: 4"),"public native projection missing");capture(mc,"legacy-public-display.png");schedule(new Runnable(){public void run(){LegacyTile display=(LegacyTile)serverPlayer().worldObj.getBlockTileEntity(1,200,0);display.publishReadings=false;display.displayChanged();}});phase=17;waitUntilNext();return;}
            if(phase==17){check("PASS".equals(result)&&clientDisplay().displayLines[1].equals("Private"),"hiding failed to clear projected text");worldChecked=true;phase=12;}
            if(phase==12){check("PASS".equals(result),"page check missing");
                if(!sortChecked){oldRevision=clientContainer().revision;mouseClick(50,9,false);phase=18;waitUntilNext();return;}
                if(!worldChecked){mc.gameSettings.hideGUI=true;mc.gameSettings.pauseOnLostFocus=false;mc.thePlayer.closeScreen();schedule(new Runnable(){public void run(){setupDisplay();}});phase=15;waitUntilNext();return;}

                com.google.gson.JsonObject report=new com.google.gson.JsonObject();report.addProperty("minecraft","1.6.4");report.addProperty("source_commit",System.getenv("PL4_CLIENT_SOURCE"));report.addProperty("result","PASS");
                report.addProperty("native_wireless_withdrawal",true);report.addProperty("stale_replay_rejected",true);report.addProperty("native_wireless_deposit",true);report.addProperty("native_paging",true);report.addProperty("native_sorting",true);report.addProperty("stale_sort_replay_rejected",true);report.addProperty("sorted_capture","legacy-wireless-storage-sorted.png");report.addProperty("signed_byte_revision",70);report.addProperty("native_packet_codec_round_trip",true);report.addProperty("actual_mouse_withdraw_deposit_page",true);report.addProperty("tooltip_capture","legacy-wireless-storage-tooltip.png");report.addProperty("native_display_projection_and_hide",true);com.google.gson.JsonArray displays=new com.google.gson.JsonArray();displays.add(new com.google.gson.JsonPrimitive("legacy-private-display.png"));displays.add(new com.google.gson.JsonPrimitive("legacy-public-display.png"));report.add("display_captures",displays);
                com.google.gson.JsonArray images=new com.google.gson.JsonArray();for(String name:captures)images.add(new com.google.gson.JsonPrimitive(name));report.add("captures",images);report.addProperty("scope","Actual stock Forge client with autonomous item transport disabled to isolate manual storage actions, the native-qualified packaged mod, a separate test mod, private generated integrated world, four GUI scales, real mouse actions, native window handlers/packet codecs and a private/public network-reader world display; not dedicated multiplayer, installed third-party providers or other exact Minecraft targets.");
                try(FileWriter file=new FileWriter(new File(output(),"legacy-wireless-client.json"))){new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report,file);}phase=14;mc.shutdown();
            }
        }catch(Exception error){throw new IllegalStateException("Legacy wireless client probe failed",error);}
    }
}
