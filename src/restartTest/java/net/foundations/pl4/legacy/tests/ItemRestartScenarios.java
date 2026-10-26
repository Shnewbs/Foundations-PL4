package net.foundations.pl4.legacy.tests;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import net.foundations.pl4.legacy.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.entity.item.EntityItem;
import java.io.FileWriter;
import java.util.*;
/** Installed only in disposable, explicitly named acceptance worlds. Never part of production. */
@Mod(modid="foundations_pl4_restarttests",name="PL4 item restart acceptance",version="1",dependencies="required-after:foundations_pl4")
public final class ItemRestartScenarios {
    private WorldServer world;private final List<String> cases=new ArrayList<String>(),tokens=new ArrayList<String>();
    private static final String OWNER="pl4owner";
    @Mod.EventHandler public void started(FMLServerStartedEvent event)throws Exception{
        String phase=System.getProperty("foundations_pl4.itemRestart","");
        if(!Arrays.asList("write","verify","crash-write","crash-verify").contains(phase))throw new IllegalStateException("Disposable restart phase required");
        MinecraftServer server=MinecraftServer.getServer();
        if(!"pl4-item-restart-world".equals(server.getFolderName()))throw new IllegalStateException("Refusing non-restart world");
        if(phase.equals("crash-write")&&!Boolean.getBoolean("foundations_pl4.restartHalt"))throw new IllegalStateException("Explicit halt fixture opt-in required");
        world=server.worldServerForDimension(0);LegacyPL4.enabled=false;boolean passed=false;
        try{
            if(phase.endsWith("write")){prepare(0,false);prepare(16,true);world.saveAllChunks(true,null);world.perWorldStorage.saveAllData();world.getSaveHandler().flush();}
            else {verify(0,false);verify(16,true);}
            passed=true;System.out.println("PL4 ITEM RESTART "+phase+": PASS");
        }finally{
            Map<String,Object> report=new LinkedHashMap<String,Object>();report.put("minecraft","1.6.4");report.put("phase",phase);report.put("passed",passed);report.put("assertions",cases);report.put("tokens",tokens);report.put("process",java.lang.management.ManagementFactory.getRuntimeMXBean().getName());
            FileWriter out=new FileWriter("item-restart-"+phase+".json");try{new com.google.gson.Gson().toJson(report,out);}finally{out.close();}
            if(passed&&phase.equals("crash-write")){System.out.println("PL4 ITEM RESTART KILL READY");System.out.flush();System.err.flush();while(true)Thread.sleep(1000);}
            server.initiateShutdown();
        }
    }
    private void prepare(int x,boolean extraction){
        for(int a=x-1;a<=x+5;a++){world.getChunkFromBlockCoords(a,32);world.setBlockToAir(a,200,32);}
        place(x,LegacyPL4.EXPORT);place(x+1,LegacyPL4.CABLE);place(x+2,LegacyPL4.IMPORT);node(x).side=4;node(x+2).side=5;
        chestBlock(x-1);chestBlock(x+3);chestBlock(x+5);
        FaultyChest fault=new FaultyChest();world.setBlockTileEntity(extraction?x-1:x+3,200,32,fault);
        chest(x-1).setInventorySlotContents(0,new ItemStack(Item.ingotIron,40));fault.extract=extraction;fault.insert=!extraction;
        LegacyPL4.enabled=true;LegacyPL4.itemRate=8;boolean threw=false;try{node(x).transferOnce();}catch(RuntimeException expected){threw=true;}finally{LegacyPL4.enabled=false;}
        check(threw,"foreign post-mutation fault "+x);check(LegacyItemSafety.blocked(node(x)),"local uncertainty "+x);
        String phase=extraction?"extract":"insert";check(phase.equals(node(x).itemJournalContext.getString("Phase")),"executed phase "+x);
        // Replace the deliberately unregistered fault injector before saving; preserve its observed vanilla contents.
        TileEntityChest clean=new TileEntityChest();for(int i=0;i<fault.getSizeInventory();i++){ItemStack s=fault.getStackInSlot(i);clean.setInventorySlotContents(i,s==null?null:s.copy());}world.setBlockTileEntity(extraction?x-1:x+3,200,32,clean);clean.onInventoryChanged();
        world.setBlockToAir(x,200,32);ItemStack receipt=null;
        for(Object raw:world.getEntitiesWithinAABB(EntityItem.class,AxisAlignedBB.getBoundingBox(x-2,199,31,x+6,203,34))){EntityItem e=(EntityItem)raw;if(!e.isDead&&e.getEntityItem().getItem()==LegacyPL4.ITEM_CELL){check((receipt==null),"one receipt "+x);receipt=e.getEntityItem().copy();e.setDead();}}
        check(!(receipt==null),"sealed removal receipt "+x);chest(x+5).setInventorySlotContents(0,receipt);chest(x+5).onInventoryChanged();chest(x-1).onInventoryChanged();chest(x+3).onInventoryChanged();
        verify(x,extraction);
    }
    private void verify(int x,boolean extraction){
        world.getChunkFromBlockCoords(x-1,32);world.getChunkFromBlockCoords(x+5,32);
        check(!(world.getBlockTileEntity(x,200,32) instanceof LegacyTile),"removed original host "+x);
        check(count(chest(x-1).getStackInSlot(0))==32,"observed source after mutation "+x);
        check(count(chest(x+3).getStackInSlot(0))==(extraction?0:8),"observed destination after mutation "+x);
        ItemStack receipt=chest(x+5).getStackInSlot(0);check(!(receipt==null)&&receipt.getItem()==LegacyPL4.ITEM_CELL,"receipt inventory persistence "+x);
        NBTTagCompound tag=receipt.getTagCompound(),ctx=tag.getCompoundTag("ItemJournalContext");String token=ctx.getString("Token");tokens.add(token);
        check(java.util.UUID.fromString(token).toString().equals(token),"canonical token "+x);check(OWNER.equals(ctx.getString("Owner"))&&ctx.getInteger("Dimension")==0,"owner and dimension "+x);
        int target=extraction?x-1:x+3;String phase=extraction?"extract":"insert";
        check(ctx.getInteger("X")==target&&ctx.getInteger("Y")==200&&ctx.getInteger("Z")==32&&phase.equals(ctx.getString("Phase")),"captured endpoint and phase "+x);
        check(tag.getInteger("ItemEscrowCount")== (extraction?0:8),"uncertain receipt claim remains sealed "+x);
        LegacyWirelessSafety journal=LegacyWirelessSafety.data(world);
        check(journal.blocked(OWNER,pos(target))&&!journal.blocked("unrelated-owner",pos(target)),"persistent owner-scoped fence "+x);
        check(!journal.blocked(OWNER,pos(extraction?x+3:x-1)),"other endpoint not misattributed "+x);
        check(journal.receiptStatus(ctx).contains("unresolved token "+token)&&journal.receiptStatus(ctx).contains("recorded phase "+phase),"linked diagnostic "+x);
        world.setBlock(target,201,32,LegacyPL4.EXPORT.blockID,0,3);LegacyTile alias=(LegacyTile)world.getBlockTileEntity(target,201,32);alias.owner=OWNER;alias.side=0;
        check(alias.transferOnce()==0&&alias.status.contains("fenced"),"new host cannot bypass saved target fence "+x);
        world.setBlockToAir(target,201,32);
        check(count(chest(x-1).getStackInSlot(0))==32&&count(chest(x+3).getStackInSlot(0))==(extraction?0:8),"inspection never replays or mints "+x);
    }
    private BoundedNetwork.Point pos(int x){return new BoundedNetwork.Point(x,200,32);}
    private void place(int x,LegacyBlock b){world.setBlock(x,200,32,b.blockID,0,3);node(x).owner=OWNER;node(x).fluidDirty();}
    private void chestBlock(int x){world.setBlock(x,200,32,Block.chest.blockID,0,3);}
    private LegacyTile node(int x){return (LegacyTile)world.getBlockTileEntity(x,200,32);}
    private TileEntityChest chest(int x){return (TileEntityChest)world.getBlockTileEntity(x,200,32);}
    private int count(ItemStack s){return s==null?0:s.stackSize;}
    private void check(boolean value,String name){if(!value)throw new AssertionError(name);cases.add(name);}
    public static final class FaultyChest extends TileEntityChest {
        boolean extract,insert;
        @Override public ItemStack decrStackSize(int slot,int amount){ItemStack s=super.decrStackSize(slot,amount);if(extract)throw new IllegalStateException("fixture failure after extraction");return s;}
        @Override public void setInventorySlotContents(int slot,ItemStack value){super.setInventorySlotContents(slot,value);if(insert)throw new IllegalStateException("fixture failure after insertion");}
    }
}
