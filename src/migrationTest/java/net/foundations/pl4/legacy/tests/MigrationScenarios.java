package net.foundations.pl4.legacy.tests;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.foundations.pl4.legacy.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import java.io.FileWriter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraftforge.fml.common.FMLCommonHandler;

/** Separate disposable-world fixture: common preview.3 APIs only in the write phase. */
@Mod(modid="foundations_pl4_migrationtests",name="PL4 legacy migration acceptance",version="1",dependencies="required-after:foundations_pl4")
public final class MigrationScenarios {
    private WorldServer world;
    private static final String OWNER="fae1b877-c6b4-4538-a26a-630190b54205";
    @Mod.EventHandler public void started(FMLServerStartedEvent event)throws Exception{
        String phase=System.getProperty("foundations_pl4.legacyMigration","");
        if(!"write".equals(phase)&&!"upgrade".equals(phase))throw new IllegalStateException("Disposable migration phase required");
        MinecraftServer server=FMLCommonHandler.instance().getMinecraftServerInstance();
        if(!"pl4-legacy-migration-world".equals(server.getFolderName()))throw new IllegalStateException("Refusing non-migration world");
        world=server.getWorld(0);
        boolean passed=false;
        try{
            if("write".equals(phase))writeBaseline();else verifyUpgrade();
            passed=true;System.out.println("PL4 LEGACY MIGRATION "+phase+": PASS");
        }finally{
            FileWriter out=new FileWriter("legacy-migration-"+phase+".json");
            try{out.write("{\"minecraft\":\"1.12.2\",\"phase\":\""+phase+"\",\"passed\":"+passed+"}\n");}finally{out.close();}
            server.initiateShutdown();
        }
    }
    private void writeBaseline(){
        for(int x=0;x<=5;x++){world.getChunkFromBlockCoords(new BlockPos(x,200,32));world.setBlockToAir(new BlockPos(x,200,32));}
        place(0,LegacyPL4.EXPORT);node(0).pending=diamond(7);
        place(1,LegacyPL4.TANK);check(node(1).fluid.fill(new FluidStack(FluidRegistry.WATER,4321),false)==4321,"baseline tank");
        place(2,LegacyPL4.ENERGY_BUFFER);check(node(2).energy.receive(4567,false)==4567,"baseline energy");node(2).energy.set(33);
        place(3,LegacyPL4.FLUID_EXPORT);node(3).fluid.set(new FluidStack(FluidRegistry.WATER,321));
        place(4,LegacyPL4.ENERGY_EXPORT);node(4).energy.set(22);
        world.setBlockState(new BlockPos(5,200,32),Blocks.CHEST.getDefaultState(),3);
        chest().setInventorySlotContents(0,diamond(13));
        for(int x=0;x<=4;x++)node(x).fluidDirty();
    }
    private void verifyUpgrade(){
        check(node(0).owner.equals(OWNER)&&node(0).side==4,"saved owner and face");
        check(count(node(0).pending)==7,"item escrow");
        check(node(1).fluid.storedAmount()==4321&&!node(1).fluid.blocked(),"saved fluid tank");
        check(node(2).energy.stored()==4567&&node(2).energy.get()==33&&!node(2).energy.blocked(),"stored and pending energy");
        check(node(3).fluid.get()!=null&&node(3).fluid.get().amount==321&&!node(3).fluid.blocked(),"pending fluid");
        check(node(4).energy.get()==22&&!node(4).energy.blocked(),"export energy escrow");
        check(count(chest().getStackInSlot(0))==13,"vanilla inventory persistence");
        for(int x=0;x<=4;x++)check(!node(x).publishReadings&&node(x).readerIndex==0,"old hosts default private");
        place(6,LegacyPL4.ITEM_READER);node(6).side=4;
        check(LegacyReadings.sample(node(6))[1].equals("Items: 13"),"new reader on existing saved inventory");
        place(7,LegacyPL4.DISPLAY);
        check(!node(7).publishReadings&&LegacyReadings.clientTag(node(7)).getString("Line1").equals("Private"),"new display defaults private");
        check(count(node(0).pending)==7&&node(1).fluid.storedAmount()==4321&&node(2).energy.stored()==4567&&count(chest().getStackInSlot(0))==13,"upgrade monitoring conserves all stored resources");
    }
    private void place(int x,LegacyBlock block){world.setBlockState(new BlockPos(x,200,32),block.getDefaultState(),3);node(x).owner=OWNER;node(x).side=4;node(x).fluidDirty();}
    private LegacyTile node(int x){return (LegacyTile)world.getTileEntity(new BlockPos(x,200,32));}
    private TileEntityChest chest(){return (TileEntityChest)world.getTileEntity(new BlockPos(5,200,32));}
    private ItemStack diamond(int n){return new ItemStack(Items.DIAMOND,n);}
    private int count(ItemStack s){return s==null||s.isEmpty()?0:s.getCount();}
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
