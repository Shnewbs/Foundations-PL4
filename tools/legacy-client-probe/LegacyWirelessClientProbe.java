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
    private int phase,scale=1,oldRevision,lastLogged=-1;private long due,started=System.nanoTime();
    private final AtomicReference<String> serverResult=new AtomicReference<String>();
    private boolean worldChecked;
    private final List<String> captures=new ArrayList<String>();
    @Mod.EventHandler public void init(FMLPreInitializationEvent event){MinecraftForge.EVENT_BUS.register(this);}
    private File output(){File out=new File(System.getenv("PL4_CLIENT_OUTPUT"));out.mkdirs();return out;}
    private void schedule(final Runnable action){serverResult.set(null);Minecraft.getMinecraft().getIntegratedServer().addScheduledTask(new Runnable(){public void run(){try{action.run();serverResult.set("PASS");}catch(Throwable error){serverResult.set(error.toString());error.printStackTrace();}}});}
    private EntityPlayerMP serverPlayer(){Minecraft mc=Minecraft.getMinecraft();return mc.getIntegratedServer().getPlayerList().getPlayerByUUID(mc.player.getUniqueID());}
    private void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private TileEntityChest source(){return (TileEntityChest)serverPlayer().getServerWorld().getTileEntity(new BlockPos(-1,200,0));}
    private int diamonds(){EntityPlayerMP p=serverPlayer();int n=0;for(int i=0;i<36;i++){ItemStack s=p.inventory.getStackInSlot(i);if(!s.isEmpty()&&s.getItem()==Items.DIAMOND)n+=s.getCount();}return n;}
    private void setup(){
        EntityPlayerMP player=serverPlayer();WorldServer world=player.getServerWorld();world.getGameRules().setOrCreateGameRule("doMobSpawning","false");
        for(int x=-2;x<=4;x++)for(int z=-2;z<=5;z++)world.setBlockState(new BlockPos(x,198,z),Blocks.STONE.getDefaultState(),3);
        world.setBlockState(new BlockPos(0,200,0),LegacyPL4.EXPORT.getDefaultState(),3);world.setBlockState(new BlockPos(1,200,0),LegacyPL4.CABLE.getDefaultState(),3);world.setBlockState(new BlockPos(2,200,0),LegacyPL4.IMPORT.getDefaultState(),3);
        for(int x=0;x<3;x++){LegacyTile tile=(LegacyTile)world.getTileEntity(new BlockPos(x,200,0));tile.owner=player.getUniqueID().toString();tile.side=x==0?4:5;tile.markDirty();}
        world.setBlockState(new BlockPos(-1,200,0),Blocks.CHEST.getDefaultState(),3);world.setBlockState(new BlockPos(3,200,0),Blocks.CHEST.getDefaultState(),3);
        TileEntityChest destination=(TileEntityChest)world.getTileEntity(new BlockPos(3,200,0));
        for(int i=0;i<27;i++){source().setInventorySlotContents(i,new ItemStack(Items.IRON_INGOT,32));destination.setInventorySlotContents(i,new ItemStack(Items.GOLD_INGOT,32));}
        source().setInventorySlotContents(0,new ItemStack(Items.DIAMOND,64));
        player.inventory.clear();player.inventory.currentItem=0;player.inventory.setInventorySlotContents(0,new ItemStack(LegacyPL4.WIRELESS_STORAGE));player.inventory.setInventorySlotContents(9,new ItemStack(Items.IRON_INGOT,32));
        player.connection.setPlayerLocation(.5,199,3.5,180,0);LegacyPL4.enabled=false;
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
        int left=(resolution.getScaledWidth()-176)/2,top=(resolution.getScaledHeight()-204)/2;
        java.awt.Robot robot=new java.awt.Robot();robot.mouseMove((left+logicalX)*factor,(top+logicalY)*factor);
        int button=right?java.awt.event.InputEvent.BUTTON3_DOWN_MASK:java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
        robot.mousePress(button);robot.mouseRelease(button);
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
            if(System.nanoTime()-started>300_000_000_000L)throw new IllegalStateException("Timed out in probe phase "+phase);
            String result=serverResult.get();if(result!=null&&!"PASS".equals(result))throw new IllegalStateException(result);
            if(System.nanoTime()<due)return;
            if(phase==0){if(!(mc.currentScreen instanceof GuiMainMenu))return;phase=1;mc.launchIntegratedServer("pl4-wireless-client-probe","PL4 Wireless Probe",new WorldSettings(0L,GameType.CREATIVE,false,false,WorldType.FLAT));waitUntilNext();return;}
            if(phase==1){if(mc.player==null||mc.world==null||mc.getIntegratedServer()==null)return;schedule(new Runnable(){public void run(){setup();}});phase=2;waitUntilNext();return;}
            if(phase==2){if(!(mc.currentScreen instanceof LegacyWirelessScreen)||!"PASS".equals(result))return;
                mc.gameSettings.guiScale=scale;mc.resize(mc.displayWidth,mc.displayHeight);new java.awt.Robot().mouseMove(8,mc.displayHeight-8);phase=3;waitUntilNext();return;}
            if(phase==3){
                ScaledResolution resolution=new ScaledResolution(mc);check(resolution.getScaleFactor()==scale,"wrong actual GUI scale");
                String name="legacy-wireless-storage-scale-"+scale+".png";ImageIO.write(ScreenShotHelper.createScreenshot(mc.displayWidth,mc.displayHeight,mc.getFramebuffer()),"png",new File(output(),name));captures.add(name);
                if(++scale<=4){phase=2;return;}
                ScaledResolution hover=new ScaledResolution(mc);int factor=hover.getScaleFactor();
                new java.awt.Robot().mouseMove(((hover.getScaledWidth()-176)/2+16)*factor,((hover.getScaledHeight()-204)/2+26)*factor);
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
            if(phase==15){check("PASS".equals(result),"display setup failed");check(clientDisplay().displayFacing==3&&clientDisplay().displayLines[1].equals("Private"),"private native projection missing: facing="+clientDisplay().displayFacing+" lines="+java.util.Arrays.toString(clientDisplay().displayLines));capture(mc,"legacy-private-display.png");schedule(new Runnable(){public void run(){LegacyTile display=(LegacyTile)serverPlayer().getServerWorld().getTileEntity(new BlockPos(1,200,0));display.publishReadings=true;display.displayChanged();}});phase=16;waitUntilNext();return;}
            if(phase==16){check("PASS".equals(result),"display publication failed");check(clientDisplay().displayLines[0].equals("PL4 Network")&&clientDisplay().displayLines[1].equals("Hosts: 4"),"public native projection missing");capture(mc,"legacy-public-display.png");schedule(new Runnable(){public void run(){LegacyTile display=(LegacyTile)serverPlayer().getServerWorld().getTileEntity(new BlockPos(1,200,0));display.publishReadings=false;display.displayChanged();}});phase=17;waitUntilNext();return;}
            if(phase==17){check("PASS".equals(result)&&clientDisplay().displayLines[1].equals("Private"),"hiding failed to clear projected text");worldChecked=true;phase=12;}
            if(phase==12){check("PASS".equals(result),"page check missing");
                if(!worldChecked){mc.gameSettings.pauseOnLostFocus=false;mc.player.closeScreen();schedule(new Runnable(){public void run(){setupDisplay();}});phase=15;waitUntilNext();return;}

                com.google.gson.JsonObject report=new com.google.gson.JsonObject();report.addProperty("minecraft","1.12.2");report.addProperty("source_commit",System.getenv("PL4_CLIENT_SOURCE"));report.addProperty("result","PASS");
                report.addProperty("native_wireless_withdrawal",true);report.addProperty("stale_replay_rejected",true);report.addProperty("native_wireless_deposit",true);report.addProperty("native_paging",true);report.addProperty("signed_byte_revision",70);report.addProperty("native_packet_codec_round_trip",true);report.addProperty("actual_mouse_withdraw_deposit_page",true);report.addProperty("tooltip_capture","legacy-wireless-storage-tooltip.png");report.addProperty("native_display_projection_and_hide",true);com.google.gson.JsonArray displays=new com.google.gson.JsonArray();displays.add("legacy-private-display.png");displays.add("legacy-public-display.png");report.add("display_captures",displays);
                com.google.gson.JsonArray images=new com.google.gson.JsonArray();for(String name:captures)images.add(name);report.add("captures",images);report.addProperty("scope","Actual stock Forge client with the native-qualified packaged mod, a separate test mod, private generated integrated world, four GUI scales, real mouse actions, native window handlers/packet codecs and a private/public network-reader world display; not dedicated multiplayer, installed third-party providers or 1.6.4 client acceptance.");
                try(FileWriter file=new FileWriter(new File(output(),"legacy-wireless-client.json"))){new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report,file);}phase=14;mc.shutdown();
            }
        }catch(Exception error){throw new IllegalStateException("Legacy wireless client probe failed",error);}
    }
}
