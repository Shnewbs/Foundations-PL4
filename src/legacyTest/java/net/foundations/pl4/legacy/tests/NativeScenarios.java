package net.foundations.pl4.legacy.tests;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import net.foundations.pl4.legacy.*;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.FakePlayerFactory;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
/** Separate test mod, only installed into the disposable acceptance server. */
@Mod(modid="foundations_pl4_legacytests",name="PL4 Legacy Scenarios",version="1",dependencies="required-after:foundations_pl4")
public final class NativeScenarios {
    private WorldServer world;private int total,passed;private List<String> failures=new ArrayList<String>();
    private static final String OWNER="pl4owner";
    private static final BoundedNetwork.Point EXPORT=new BoundedNetwork.Point(0,200,0),CABLE=new BoundedNetwork.Point(1,200,0),SINK=new BoundedNetwork.Point(2,200,0);
    @Mod.EventHandler public void started(FMLServerStartedEvent event)throws Exception {
        if(!Boolean.getBoolean("foundations_pl4.legacyScenarios"))throw new IllegalStateException("Refusing non-scenario server");
        MinecraftServer server=MinecraftServer.getServer();if(!"pl4-legacy-test-world".equals(server.getFolderName()))throw new IllegalStateException("Refusing non-test world");world=server.worldServerForDimension(0);
        try {
            test("registered native blocks and recipes",new Runnable(){public void run(){check(Block.blocksList[LegacyPL4.CABLE.blockID]==LegacyPL4.CABLE,"registered block");int n=0;for(Object o:CraftingManager.getInstance().getRecipeList())if(o instanceof IRecipe){ItemStack r=((IRecipe)o).getRecipeOutput();if(r!=null&&(r.itemID==LegacyPL4.CABLE.blockID||r.itemID==LegacyPL4.EXPORT.blockID||r.itemID==LegacyPL4.IMPORT.blockID))n++;}check(n==4,"four actual recipes");}});
            test("physical graph",new Runnable(){public void run(){check(node(EXPORT).network().nodes.size()==3,"three hosts");}});
            test("bounded conserved transfer",new Runnable(){public void run(){source().setInventorySlotContents(0,new ItemStack(Item.ingotIron,32));check(node(EXPORT).transferOnce()==8,"rate");check(source().getStackInSlot(0).stackSize==24&&destination().getStackInSlot(0).stackSize==8,"conservation");}});
            test("full destination",new Runnable(){public void run(){source().setInventorySlotContents(0,new ItemStack(Item.ingotIron,32));for(int i=0;i<27;i++)destination().setInventorySlotContents(i,new ItemStack(Item.stick,64));check(node(EXPORT).transferOnce()==0&&source().getStackInSlot(0).stackSize==32,"no extract to full sink");}});
            test("disabled configuration",new Runnable(){public void run(){source().setInventorySlotContents(0,new ItemStack(Item.ingotIron,32));LegacyPL4.enabled=false;check(node(EXPORT).transferOnce()==0,"disabled");}});
            test("foreign receiver",new Runnable(){public void run(){source().setInventorySlotContents(0,new ItemStack(Item.ingotIron,32));node(SINK).owner="other";check(node(EXPORT).transferOnce()==0,"foreign owner");}});
            test("foreign cable",new Runnable(){public void run(){node(CABLE).owner="other";check(node(EXPORT).network().nodes.size()==1,"partition");}});
            test("removed cable",new Runnable(){public void run(){source().setInventorySlotContents(0,new ItemStack(Item.ingotIron,32));world.setBlockToAir(1,200,0);check(node(EXPORT).transferOnce()==0,"disconnected");}});
            test("oversized network",new Runnable(){public void run(){LegacyPL4.nodeLimit=2;check(node(EXPORT).network().overflow&&node(EXPORT).transferOnce()==0,"fail closed");}});
            test("NBT variants",new Runnable(){public void run(){ItemStack a=new ItemStack(Item.ingotIron,16);a.setItemName("A");ItemStack b=new ItemStack(Item.ingotIron,10);b.setItemName("B");source().setInventorySlotContents(0,a);destination().setInventorySlotContents(0,b);check(node(EXPORT).transferOnce()==8,"move variant");check(destination().getStackInSlot(0).stackSize==10&&destination().getStackInSlot(1).getDisplayName().equals("A"),"variants isolated");}});
            test("native escrow serialization",new Runnable(){public void run(){node(EXPORT).pending=new ItemStack(Item.diamond,5);NBTTagCompound t=new NBTTagCompound();node(EXPORT).writeToNBT(t);LegacyTile copy=new LegacyTile();copy.readFromNBT(t);check(copy.pending.stackSize==5&&copy.pending.itemID==Item.diamond.itemID&&copy.owner.equals(OWNER)&&copy.side==4,"persistent buffer");}});
            test("source missing with escrow",new Runnable(){public void run(){node(EXPORT).pending=new ItemStack(Item.diamond,5);world.setBlockToAir(-1,200,0);check(node(EXPORT).transferOnce()==5&&node(EXPORT).pending==null&&destination().getStackInSlot(0).stackSize==5,"buffer drains");}});
            test("invalid side clamped",new Runnable(){public void run(){NBTTagCompound t=new NBTTagCompound();node(EXPORT).writeToNBT(t);t.setInteger("Side",500);LegacyTile copy=new LegacyTile();copy.readFromNBT(t);check(copy.side>=0&&copy.side<6,"bounded direction");}});
            test("no chunk force loading",new Runnable(){public void run(){int before=world.getChunkProvider().getLoadedChunkCount();node(EXPORT).network();check(before==world.getChunkProvider().getLoadedChunkCount(),"chunks unchanged");}});
            test("name ownership",new Runnable(){public void run(){check(node(EXPORT).canEdit(FakePlayerFactory.get(world,"Pl4Owner")),"normalized owner");check(!node(EXPORT).canEdit(FakePlayerFactory.get(world,"OtherPerson")),"stranger denied");}});
            test("export redstone pause",new Runnable(){public void run(){source().setInventorySlotContents(0,new ItemStack(Item.ingotIron,32));world.setBlock(0,201,0,Block.blockRedstone.blockID,0,3);check(node(EXPORT).transferOnce()==0,"export pause");}});
            test("import redstone pause",new Runnable(){public void run(){source().setInventorySlotContents(0,new ItemStack(Item.ingotIron,32));world.setBlock(2,201,0,Block.blockRedstone.blockID,0,3);check(node(EXPORT).transferOnce()==0,"import pause");}});
            test("native sided furnace",new Runnable(){public void run(){world.setBlock(-1,200,0,Block.furnaceIdle.blockID,0,3);TileEntityFurnace f=(TileEntityFurnace)world.getBlockTileEntity(-1,200,0);f.setInventorySlotContents(0,new ItemStack(Block.oreIron,5));check(node(EXPORT).inventory().extract(0,1,true)==null,"sided access");}});
            test("slot limit",new Runnable(){public void run(){LegacyPL4.slotLimit=1;source().setInventorySlotContents(5,new ItemStack(Item.ingotIron,32));check(node(EXPORT).transferOnce()==0,"source slot cap");}});
            test("empty inventories",new Runnable(){public void run(){check(node(EXPORT).transferOnce()==0&&node(EXPORT).pending==null,"empty");}});
            test("removed escrow drops once",new Runnable(){public void run(){node(EXPORT).pending=new ItemStack(Item.diamond,5);world.setBlockToAir(0,200,0);int n=0;for(Object o:world.getEntitiesWithinAABB(EntityItem.class,area())){EntityItem e=(EntityItem)o;if(!e.isDead&&e.getEntityItem().itemID==Item.diamond.itemID)n+=e.getEntityItem().stackSize;}check(n==5,"buffer dropped");}});
        }finally {
            FileWriter out=new FileWriter("legacy-scenarios.json");try{out.write("{\"minecraft\":\"1.6.4\",\"total\":"+total+",\"passed\":"+passed+",\"failed\":"+(total-passed)+"}\n");}finally{out.close();}
            System.out.println("PL4 LEGACY SCENARIOS: "+passed+"/"+total);for(String failure:failures)System.out.println("FAILED "+failure);server.initiateShutdown();
        }
    }
    private void test(String name,Runnable body){total++;try{reset();body.run();passed++;System.out.println("PASS "+name);}catch(Throwable error){failures.add(name+": "+error.toString());error.printStackTrace();}}
    private static AxisAlignedBB area(){return AxisAlignedBB.getBoundingBox(-3,198,-3,6,204,3);}
    private void reset(){LegacyPL4.enabled=true;LegacyPL4.nodeLimit=256;LegacyPL4.slotLimit=256;LegacyPL4.itemRate=8;for(int x=-2;x<=4;x++)for(int y=199;y<=202;y++)for(int z=-1;z<=1;z++){world.getChunkFromBlockCoords(x,z);world.setBlockToAir(x,y,z);}for(Object o:world.getEntitiesWithinAABB(EntityItem.class,area()))((EntityItem)o).setDead();world.setBlock(0,200,0,LegacyPL4.EXPORT.blockID,0,3);world.setBlock(1,200,0,LegacyPL4.CABLE.blockID,0,3);world.setBlock(2,200,0,LegacyPL4.IMPORT.blockID,0,3);for(BoundedNetwork.Point p:new BoundedNetwork.Point[]{EXPORT,CABLE,SINK})node(p).owner=OWNER;node(EXPORT).side=4;node(SINK).side=5;world.setBlock(-1,200,0,Block.chest.blockID,0,3);world.setBlock(3,200,0,Block.chest.blockID,0,3);}
    private LegacyTile node(BoundedNetwork.Point p){return (LegacyTile)world.getBlockTileEntity(p.x,p.y,p.z);}
    private TileEntityChest source(){return (TileEntityChest)world.getBlockTileEntity(-1,200,0);}
    private TileEntityChest destination(){return (TileEntityChest)world.getBlockTileEntity(3,200,0);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
