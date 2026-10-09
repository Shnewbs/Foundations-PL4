package net.foundations.pl4.legacy.tests;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import net.foundations.pl4.legacy.*;
import net.minecraftforge.fluids.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
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
            test("registered native blocks and recipes",new Runnable(){public void run(){check(Block.blocksList[LegacyPL4.CABLE.blockID]==LegacyPL4.CABLE,"registered block");int n=0;for(Object o:CraftingManager.getInstance().getRecipeList())if(o instanceof IRecipe){ItemStack r=((IRecipe)o).getRecipeOutput();if(r!=null&&(r.itemID==LegacyPL4.CABLE.blockID||r.itemID==LegacyPL4.EXPORT.blockID||r.itemID==LegacyPL4.IMPORT.blockID||r.itemID==LegacyPL4.FLUID_EXPORT.blockID||r.itemID==LegacyPL4.FLUID_IMPORT.blockID||r.itemID==LegacyPL4.TANK.blockID))n++;}check(n==8,"eight actual recipes");}});
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
                    test("tank capacity and simulation",new Runnable(){public void run(){setupFluids();check(fluidNode(-1).fluid.fill(water(20000),true)==16000&&fluidNode(-1).fluid.storedAmount()==0,"fill simulation");check(fluidNode(-1).fluid.fill(water(20000),false)==16000,"tank capacity");check(fluidNode(-1).fluid.drain(1000,true).amount==1000&&fluidNode(-1).fluid.storedAmount()==16000,"drain simulation");}});
            test("tank does not bridge physical data network",new Runnable(){public void run(){setupFluids();check(fluidNode(0).network().nodes.size()==3,"only cable and fluid endpoints in graph");}});
            test("native fluid transfer rate and conservation",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);check(fluidNode(0).transferOnce()==250,"fluid rate");check(fluidNode(-1).fluid.storedAmount()==750&&fluidNode(3).fluid.storedAmount()==250,"fluid conservation");}});
            test("full fluid destination leaves source untouched",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);fluidNode(3).fluid.fill(water(16000),false);check(fluidNode(0).transferOnce()==0&&fluidNode(-1).fluid.storedAmount()==1000,"full tank");}});
            test("different fluid does not mix",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);fluidNode(3).fluid.fill(new FluidStack(FluidRegistry.LAVA,1000),false);check(fluidNode(0).transferOnce()==0&&fluidNode(-1).fluid.storedAmount()==1000,"no mixing");}});
            test("fluid NBT variants stay separate",new Runnable(){public void run(){setupFluids();FluidStack tagged=water(1000);tagged.tag=new NBTTagCompound();tagged.tag.setString("Grade","A");fluidNode(-1).fluid.fill(tagged,false);fluidNode(3).fluid.fill(water(1000),false);check(fluidNode(0).transferOnce()==0,"NBT variant cannot coalesce");}});
            test("fluid enable flag is respected",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);LegacyPL4.fluidsEnabled=false;check(fluidNode(0).transferOnce()==0&&fluidNode(-1).fluid.storedAmount()==1000,"fluid switch");}});
            test("global transfer flag stops fluids",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);LegacyPL4.enabled=false;check(fluidNode(0).transferOnce()==0,"global switch");}});
            test("foreign tank cannot receive PL4 fluids",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);fluidNode(3).owner="other";check(fluidNode(0).transferOnce()==0&&fluidNode(-1).fluid.storedAmount()==1000,"foreign tank");}});
            test("foreign fluid source tank is rejected",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);fluidNode(-1).owner="other";check(fluidNode(0).transferOnce()==0,"source tank ownership");}});
            test("removed cable disconnects fluids",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);fluidAir(1,200);check(fluidNode(0).transferOnce()==0,"fluid disconnect");}});
            test("oversized fluid network fails closed",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);LegacyPL4.nodeLimit=2;check(fluidNode(0).transferOnce()==0&&fluidNode(0).network().overflow,"fluid topology cap");}});
            test("fluid exporter redstone pause",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);fluidPower(0);check(fluidNode(0).transferOnce()==0,"fluid export pause");}});
            test("fluid importer redstone pause",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);fluidPower(2);check(fluidNode(0).transferOnce()==0,"fluid import pause");}});
            test("fluid escrow survives source removal",new Runnable(){public void run(){setupFluids();fluidNode(0).fluid.set(water(123));fluidAir(-1,200);check(fluidNode(0).transferOnce()==123&&fluidNode(0).fluid.get()==null&&fluidNode(3).fluid.storedAmount()==123,"drain orphan escrow");}});
            test("fluid state persists through native NBT",new Runnable(){public void run(){setupFluids();fluidNode(0).fluid.set(water(99));fluidNode(0).fluid.block("native persistence test");NBTTagCompound tag=saveFluid(fluidNode(0));LegacyTile copy=new LegacyTile();copy.readFromNBT(tag);check(copy.fluid.get().amount==99&&copy.fluid.blocked(),"fluid fault and amount persisted");}});
            test("built-in tank content persists through native NBT",new Runnable(){public void run(){setupFluids();fluidNode(3).fluid.fill(water(3456),false);LegacyTile copy=new LegacyTile();copy.readFromNBT(saveFluid(fluidNode(3)));check(copy.fluid.storedAmount()==3456,"tank content persisted");}});
            test("unknown fluid NBT survives round trip",new Runnable(){public void run(){setupFluids();NBTTagCompound raw=new NBTTagCompound();raw.setString("FluidName","pl4_missing_test_fluid");raw.setInteger("Amount",1234);NBTTagCompound tag=saveFluid(fluidNode(3));tag.setTag("FluidTank",raw);LegacyTile copy=new LegacyTile();copy.readFromNBT(tag);NBTTagCompound saved=saveFluid(copy);check(saved.getCompoundTag("FluidTank").getString("FluidName").equals("pl4_missing_test_fluid")&&saved.getCompoundTag("FluidTank").getInteger("Amount")==1234,"unresolved fluid retained");}});
            test("fluid removal creates exactly one recovery cell",new Runnable(){public void run(){setupFluids();fluidNode(3).fluid.fill(water(1234),false);fluidAir(3,200);check(fluidCells()==1,"one sealed cell drop");}});
            test("healthy recovery cell restores fluid",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1234),false);ItemStack cell=new ItemStack(LegacyPL4.FLUID_CELL);cell.setTagCompound(fluidNode(-1).fluid.recovery().get(0));check(LegacyFluidPlatform.interact(fluidNode(3),fluidPlayer(),cell)&&fluidNode(3).fluid.storedAmount()==1234&&!cell.getTagCompound().hasKey("Contents"),"cell restoration");}});
            test("partial recovery leaves remainder in cell",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(250),false);fluidNode(3).fluid.fill(water(15990),false);NBTTagCompound cell=fluidNode(-1).fluid.recovery().get(0);check(fluidNode(3).fluid.restore(cell)==10&&cell.getCompoundTag("Contents").getInteger("Amount")==240&&fluidNode(3).fluid.storedAmount()==16000,"partial cell");}});
            test("quarantined cell cannot replay uncertain fluid",new Runnable(){public void run(){setupFluids();fluidNode(0).fluid.set(water(250));fluidNode(0).fluid.block("uncertain provider operation");NBTTagCompound cell=fluidNode(0).fluid.recovery().get(0);check(fluidNode(3).fluid.restore(cell)==0&&cell.getCompoundTag("Contents").getInteger("Amount")==250,"quarantine cannot restore");}});
            test("owner check protects tank interaction",new Runnable(){public void run(){setupFluids();fluidNode(3).owner="other";ItemStack bucket=waterBucket();check(!LegacyFluidPlatform.interact(fluidNode(3),fluidPlayer(),bucket)&&fluidNode(3).fluid.storedAmount()==0,"stranger cannot use tank");}});
            test("vanilla bucket fills built-in tank",new Runnable(){public void run(){setupFluids();ItemStack bucket=waterBucket();check(LegacyFluidPlatform.interact(fluidNode(3),fluidPlayer(),bucket)&&fluidNode(3).fluid.storedAmount()==1000,"bucket fill");}});
            test("tagged fluid cannot become plain vanilla bucket",new Runnable(){public void run(){setupFluids();FluidStack tagged=water(1000);tagged.tag=new NBTTagCompound();tagged.tag.setString("Grade","A");fluidNode(3).fluid.fill(tagged,false);LegacyFluidPlatform.interact(fluidNode(3),fluidPlayer(),emptyBucket());check(fluidNode(3).fluid.storedAmount()==1000,"bucket preserves tags");}});
            test("fluid scans do not add loaded chunks",new Runnable(){public void run(){setupFluids();fluidNode(-1).fluid.fill(water(1000),false);int before=world.getChunkProvider().getLoadedChunkCount();fluidNode(0).transferOnce();check(world.getChunkProvider().getLoadedChunkCount()==before,"fluid scan loaded-only");}});
            test("native sided external fluid handler",new Runnable(){public void run(){setupFluids();ExternalTank dest=externalTank();fluidNode(-1).fluid.fill(water(1000),false);check(fluidNode(0).transferOnce()==250&&dest.tank.getFluidAmount()==250,"external native fluid API");}});
            test("external handler denied face does not fall back",new Runnable(){public void run(){setupFluids();ExternalTank dest=externalTank();dest.deny=true;fluidNode(-1).fluid.fill(water(1000),false);check(fluidNode(0).transferOnce()==0&&fluidNode(-1).fluid.storedAmount()==1000,"no unsided fallback");}});
            test("actual refusal preserves and later drains escrow",new Runnable(){public void run(){setupFluids();ExternalTank dest=externalTank();dest.refuse=true;fluidNode(-1).fluid.fill(water(1000),false);check(fluidNode(0).transferOnce()==0&&fluidNode(0).fluid.get().amount==250&&!fluidNode(0).fluid.blocked(),"refusal escrow");dest.refuse=false;check(fluidNode(0).transferOnce()==250&&fluidNode(-1).fluid.storedAmount()==750,"no repeated extraction");}});
            test("native provider exception quarantines without retry",new Runnable(){public void run(){setupFluids();ExternalTank dest=externalTank();dest.fail=true;fluidNode(-1).fluid.fill(water(1000),false);try{fluidNode(0).transferOnce();throw new AssertionError("provider exception expected");}catch(IllegalStateException expected){}check(fluidNode(0).fluid.blocked()&&fluidNode(0).fluid.get().amount==250,"quarantined escrow");int calls=dest.executed;check(fluidNode(0).transferOnce()==0&&dest.executed==calls,"no retry after exception");}});
        }finally {
            FileWriter out=new FileWriter("legacy-scenarios.json");try{out.write("{\"minecraft\":\"1.6.4\",\"total\":"+total+",\"passed\":"+passed+",\"failed\":"+(total-passed)+"}\n");}finally{out.close();}
            System.out.println("PL4 LEGACY SCENARIOS: "+passed+"/"+total);for(String failure:failures)System.out.println("FAILED "+failure);server.initiateShutdown();
        }
    }
    private void test(String name,Runnable body){total++;try{reset();body.run();passed++;System.out.println("PASS "+name);}catch(Throwable error){failures.add(name+": "+error.toString());error.printStackTrace();}}
    private static AxisAlignedBB area(){return AxisAlignedBB.getBoundingBox(-3,198,-3,6,204,3);}
    private void reset(){LegacyPL4.fluidsEnabled=true;LegacyPL4.fluidRate=250;LegacyPL4.enabled=true;LegacyPL4.nodeLimit=256;LegacyPL4.slotLimit=256;LegacyPL4.itemRate=8;for(int x=-2;x<=4;x++)for(int y=199;y<=202;y++)for(int z=-1;z<=1;z++){world.getChunkFromBlockCoords(x,z);world.setBlockToAir(x,y,z);}for(Object o:world.getEntitiesWithinAABB(EntityItem.class,area()))((EntityItem)o).setDead();world.setBlock(0,200,0,LegacyPL4.EXPORT.blockID,0,3);world.setBlock(1,200,0,LegacyPL4.CABLE.blockID,0,3);world.setBlock(2,200,0,LegacyPL4.IMPORT.blockID,0,3);for(BoundedNetwork.Point p:new BoundedNetwork.Point[]{EXPORT,CABLE,SINK})node(p).owner=OWNER;node(EXPORT).side=4;node(SINK).side=5;world.setBlock(-1,200,0,Block.chest.blockID,0,3);world.setBlock(3,200,0,Block.chest.blockID,0,3);}
    private LegacyTile node(BoundedNetwork.Point p){return (LegacyTile)world.getBlockTileEntity(p.x,p.y,p.z);}
    private TileEntityChest source(){return (TileEntityChest)world.getBlockTileEntity(-1,200,0);}
    private TileEntityChest destination(){return (TileEntityChest)world.getBlockTileEntity(3,200,0);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}

    private static FluidStack water(int amount){return new FluidStack(FluidRegistry.WATER,amount);}
    private void setupFluids(){fluidBlock(-1,LegacyPL4.TANK);fluidBlock(0,LegacyPL4.FLUID_EXPORT);fluidBlock(2,LegacyPL4.FLUID_IMPORT);fluidBlock(3,LegacyPL4.TANK);for(int x:new int[]{-1,0,1,2,3})fluidNode(x).owner=fluidOwner();fluidNode(0).side=4;fluidNode(2).side=5;}

    private String fluidOwner(){return OWNER;}
    private EntityPlayer fluidPlayer(){return FakePlayerFactory.get(world,"Pl4Owner");}
    private LegacyTile fluidNode(int x){return (LegacyTile)world.getBlockTileEntity(x,200,0);}
    private void fluidBlock(int x,LegacyBlock block){world.setBlock(x,200,0,block.blockID,0,3);}
    private void fluidAir(int x,int y){world.setBlockToAir(x,y,0);}
    private void fluidPower(int x){world.setBlock(x,201,0,Block.blockRedstone.blockID,0,3);}
    private NBTTagCompound saveFluid(LegacyTile tile){NBTTagCompound tag=new NBTTagCompound();tile.writeToNBT(tag);return tag;}
    private ItemStack waterBucket(){return new ItemStack(Item.bucketWater);}
    private ItemStack emptyBucket(){return new ItemStack(Item.bucketEmpty);}
    private int fluidCells(){int n=0;for(Object raw:world.getEntitiesWithinAABB(EntityItem.class,area())){EntityItem item=(EntityItem)raw;if(!item.isDead&&item.getEntityItem().itemID==LegacyPL4.FLUID_CELL.itemID&&item.getEntityItem().getTagCompound().getCompoundTag("Contents").getInteger("Amount")==1234)n++;}return n;}
    private ExternalTank externalTank(){ExternalTank tile=new ExternalTank();world.setBlockTileEntity(3,200,0,tile);return tile;}
    public static final class ExternalTank extends TileEntity implements net.minecraftforge.fluids.IFluidHandler {
        final FluidTank tank=new FluidTank(16000);boolean deny,refuse,fail;int executed;
        public int fill(net.minecraftforge.common.ForgeDirection side,FluidStack f,boolean actual){if(deny||side!=net.minecraftforge.common.ForgeDirection.WEST)return 0;if(actual){executed++;if(fail)throw new IllegalStateException("native provider failure");if(refuse)return 0;}return tank.fill(f,actual);}
        public FluidStack drain(net.minecraftforge.common.ForgeDirection side,FluidStack f,boolean actual){return f!=null&&f.isFluidEqual(tank.getFluid())?drain(side,f.amount,actual):null;}
        public FluidStack drain(net.minecraftforge.common.ForgeDirection side,int n,boolean actual){return deny||side!=net.minecraftforge.common.ForgeDirection.WEST?null:tank.drain(n,actual);}
        public boolean canFill(net.minecraftforge.common.ForgeDirection side,Fluid f){return !deny&&side==net.minecraftforge.common.ForgeDirection.WEST;}
        public boolean canDrain(net.minecraftforge.common.ForgeDirection side,Fluid f){return !deny&&side==net.minecraftforge.common.ForgeDirection.WEST;}
        public FluidTankInfo[] getTankInfo(net.minecraftforge.common.ForgeDirection side){return new FluidTankInfo[]{tank.getInfo()};}
    }
}
