package net.foundations.pl4.legacy.tests;
import java.io.File;
import java.io.FileWriter;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.foundations.pl4.legacy.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
/** Disposable server only. This separate source set is never packaged in the runtime JAR. */
@Mod(modid="foundations_pl4_legacytests",name="PL4 legacy native scenarios",version="1",dependencies="required-after:foundations_pl4",acceptableRemoteVersions="*")
public final class NativeScenarios {
    private static final UUID OWNER=UUID.fromString("fae1b877-c6b4-4538-a26a-630190b54205");
    private WorldServer world;private int total,passed;private JsonArray results=new JsonArray();
    private final BlockPos export=new BlockPos(0,200,0),cable=new BlockPos(1,200,0),sink=new BlockPos(2,200,0);
    @Mod.EventHandler public void started(FMLServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("foundations_pl4.legacyScenarios"))throw new IllegalStateException("Test mod requires disposable scenario server");
        MinecraftServer server=FMLCommonHandler.instance().getMinecraftServerInstance();world=server.getWorld(0);
        if(!server.getFolderName().equals("pl4-legacy-test-world"))throw new IllegalStateException("Refusing non-test world");
        try{
            test("registered native blocks and recipes",()->{check(ForgeRegistries.BLOCKS.getValue(LegacyPL4.CABLE.getRegistryName())==LegacyPL4.CABLE,"cable registration");int count=0;for(net.minecraft.item.crafting.IRecipe r:ForgeRegistries.RECIPES)if(r.getRegistryName()!=null&&r.getRegistryName().getResourceDomain().equals(LegacyPL4.ID))count++;check(count==4,"actual recipe count");});
            test("owner separated physical topology",()->check(node(export).network().nodes.size()==3,"network size"));
            test("eight items transferred and conserved",()->{source().setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,32));check(node(export).transferOnce()==8,"move rate");check(source().getStackInSlot(0).getCount()==24&&destination().getStackInSlot(0).getCount()==8,"conservation");});
            test("full destination leaves source unchanged",()->{source().setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,32));for(int i=0;i<27;i++)destination().setInventorySlotContents(i,new ItemStack(Items.STICK,64));check(node(export).transferOnce()==0&&source().getStackInSlot(0).getCount()==32,"full target");});
            test("disabled transfer configuration",()->{source().setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,32));LegacyPL4.enabled=false;check(node(export).transferOnce()==0&&source().getStackInSlot(0).getCount()==32,"disabled transfer");});
            test("foreign owner cannot receive",()->{source().setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,32));node(sink).owner=UUID.randomUUID().toString();check(node(export).transferOnce()==0,"foreign import");});
            test("foreign cable partitions network",()->{node(cable).owner=UUID.randomUUID().toString();check(node(export).network().nodes.size()==1,"partition");});
            test("removed cable disconnects inventory",()->{source().setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,32));world.setBlockToAir(cable);check(node(export).transferOnce()==0,"disconnect");});
            test("oversized graph fails closed",()->{LegacyPL4.nodeLimit=2;source().setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,32));check(node(export).network().overflow&&node(export).transferOnce()==0,"overflow");});
            test("NBT variants do not coalesce",()->{ItemStack a=new ItemStack(Items.IRON_INGOT,16);a.setStackDisplayName("A");ItemStack b=new ItemStack(Items.IRON_INGOT,10);b.setStackDisplayName("B");source().setInventorySlotContents(0,a);destination().setInventorySlotContents(0,b);check(node(export).transferOnce()==8,"variant moved");check(destination().getStackInSlot(0).getCount()==10&&destination().getStackInSlot(1).getDisplayName().equals("A"),"variant isolation");});
            test("escrow survives native NBT serialization",()->{LegacyTile original=node(export);original.pending=new ItemStack(Items.DIAMOND,5);NBTTagCompound tag=original.writeToNBT(new NBTTagCompound());LegacyTile restored=new LegacyTile();restored.readFromNBT(tag);check(restored.pending.getCount()==5&&restored.pending.getItem()==Items.DIAMOND&&restored.owner.equals(original.owner)&&restored.side==4,"persisted escrow and identity");});
            test("escrow drains even when source disappears",()->{node(export).pending=new ItemStack(Items.DIAMOND,5);world.setBlockToAir(export.west());check(node(export).transferOnce()==5&&node(export).pending.isEmpty()&&destination().getStackInSlot(0).getCount()==5,"source unavailable escrow");});
            test("malformed direction clamped on load",()->{NBTTagCompound tag=node(export).writeToNBT(new NBTTagCompound());tag.setInteger("Side",500);LegacyTile restored=new LegacyTile();restored.readFromNBT(tag);check(restored.side>=0&&restored.side<6,"direction bound");});
            test("unloaded graph does not load chunks",()->{int before=world.getChunkProvider().getLoadedChunkCount();node(export).network();check(before==world.getChunkProvider().getLoadedChunkCount(),"no new chunks");});
            test("owner edit permitted and stranger denied",()->{check(node(export).canEdit(FakePlayerFactory.get(world,new GameProfile(OWNER,"Owner"))),"owner access");check(!node(export).canEdit(FakePlayerFactory.get(world,new GameProfile(UUID.fromString("7870f566-beb8-45ae-a0f6-d96b5678a529"),"Other"))),"stranger denied");});
            test("source redstone pauses transport",()->{source().setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,32));world.setBlockState(export.up(),Blocks.REDSTONE_BLOCK.getDefaultState(),3);check(world.isBlockPowered(export)&&node(export).transferOnce()==0,"redstone pause");});
            test("sink redstone pauses import",()->{source().setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,32));world.setBlockState(sink.up(),Blocks.REDSTONE_BLOCK.getDefaultState(),3);check(node(export).transferOnce()==0,"sink redstone");});
            test("native sided furnace extraction",()->{world.setBlockState(export.west(),Blocks.FURNACE.getDefaultState(),3);TileEntityFurnace f=(TileEntityFurnace)world.getTileEntity(export.west());f.setInventorySlotContents(0,new ItemStack(Blocks.IRON_ORE,5));check(node(export).inventory().extractItem(0,1,true).isEmpty(),"sided extraction guard");});
            test("source scan respects slot limit",()->{LegacyPL4.slotLimit=1;source().setInventorySlotContents(5,new ItemStack(Items.IRON_INGOT,32));check(node(export).transferOnce()==0,"slot cap");});
            test("empty native inventories are inert",()->check(node(export).transferOnce()==0&&node(export).pending.isEmpty(),"empty source"));
            // reset() discards fixture drops with setDead(). Until the next tick those removed entities
            // can remain in the spatial query; only live drops belong to this exact-once assertion.
            test("pending buffer released when host removed",()->{node(export).pending=new ItemStack(Items.DIAMOND,5);world.setBlockToAir(export);int count=0;for(EntityItem item:world.getEntitiesWithinAABB(EntityItem.class,new AxisAlignedBB(-2,199,-2,5,203,2)))if(!item.isDead&&item.getItem().getItem()==Items.DIAMOND)count+=item.getItem().getCount();check(count==5,"escrow drop exactly once (live count="+count+")");});
        }finally{
            JsonObject report=new JsonObject();report.addProperty("minecraft","1.12.2");report.addProperty("total",total);report.addProperty("passed",passed);report.addProperty("failed",total-passed);report.add("results",results);
            try(FileWriter file=new FileWriter(new File("legacy-scenarios.json"))){new Gson().toJson(report,file);}
            System.out.println("PL4 LEGACY SCENARIOS: "+passed+"/"+total);server.initiateShutdown();
        }
    }
    private void test(String name,Runnable body){total++;JsonObject result=new JsonObject();result.addProperty("name",name);try{reset();body.run();passed++;result.addProperty("passed",true);}catch(Throwable error){result.addProperty("passed",false);result.addProperty("error",error.toString());error.printStackTrace();}results.add(result);}
    private void reset(){LegacyPL4.enabled=true;LegacyPL4.nodeLimit=256;LegacyPL4.slotLimit=256;LegacyPL4.itemRate=8;for(int x=-2;x<=4;x++)for(int y=199;y<=202;y++)for(int z=-1;z<=1;z++){world.getChunkFromBlockCoords(new BlockPos(x,y,z));world.setBlockToAir(new BlockPos(x,y,z));}for(EntityItem item:world.getEntitiesWithinAABB(EntityItem.class,new AxisAlignedBB(-3,198,-3,6,204,3)))item.setDead();world.setBlockState(export,LegacyPL4.EXPORT.getDefaultState(),3);world.setBlockState(cable,LegacyPL4.CABLE.getDefaultState(),3);world.setBlockState(sink,LegacyPL4.IMPORT.getDefaultState(),3);for(BlockPos p:new BlockPos[]{export,cable,sink})node(p).owner=OWNER.toString();node(export).side=4;node(sink).side=5;world.setBlockState(export.west(),Blocks.CHEST.getDefaultState(),3);world.setBlockState(sink.east(),Blocks.CHEST.getDefaultState(),3);}
    private LegacyTile node(BlockPos p){return (LegacyTile)world.getTileEntity(p);}
    private TileEntityChest source(){return (TileEntityChest)world.getTileEntity(export.west());}
    private TileEntityChest destination(){return (TileEntityChest)world.getTileEntity(sink.east());}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
