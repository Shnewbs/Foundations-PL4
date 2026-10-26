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
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.AxisAlignedBB;
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
        if(!"write".equals(phase)&&!"upgrade".equals(phase)&&!"fault-write".equals(phase)&&!"fault-upgrade".equals(phase)&&!"fault-verify".equals(phase))throw new IllegalStateException("Disposable migration phase required");
        MinecraftServer server=FMLCommonHandler.instance().getMinecraftServerInstance();
        if(!(phase.startsWith("fault-")?"pl4-preupgrade-fault-world":"pl4-legacy-migration-world").equals(server.getFolderName()))throw new IllegalStateException("Refusing non-migration world");
        world=server.getWorld(0);
        boolean passed=false;
        try{
            if("fault-write".equals(phase))writeFaults();else if("fault-upgrade".equals(phase))upgradeFaults();else if("fault-verify".equals(phase))verifyFaults();else if("write".equals(phase))writeBaseline();else verifyUpgrade();
            passed=true;System.out.println("PL4 LEGACY MIGRATION "+phase+": PASS");
        }finally{
            FileWriter out=new FileWriter("legacy-migration-"+phase+".json");
            try{if(phase.startsWith("fault-")){java.util.Map<String,Object> r=new java.util.LinkedHashMap<String,Object>();r.put("minecraft","1.12.2");r.put("phase",phase);r.put("passed",passed);r.put("checks",faultChecks);r.put("tokens",faultTokens);r.put("process",java.lang.management.ManagementFactory.getRuntimeMXBean().getName());new com.google.gson.Gson().toJson(r,out);}else out.write("{\"minecraft\":\"1.12.2\",\"phase\":\""+phase+"\",\"passed\":"+passed+"}\n");}finally{out.close();}
            server.initiateShutdown();
        }
    }

    private int faultChecks;private final java.util.List<String> faultTokens=new java.util.ArrayList<String>();
    private void faultCheck(boolean value,String reason){check(value,reason);faultChecks++;}
    private TileEntityChest faultChest(int x){return (TileEntityChest)world.getTileEntity(new BlockPos(x,200,32));}
    private void writeFaults(){
        place(8,LegacyPL4.EXPORT);node(8).pending=diamond(8);node(8).itemFault="Historical insertion outcome unknown";node(8).fluidDirty();
        place(10,LegacyPL4.EXPORT);node(10).itemFault="Historical extraction outcome unknown";node(10).fluidDirty();
        world.setBlockState(new BlockPos(12,200,32),Blocks.CHEST.getDefaultState(),3);world.setBlockState(new BlockPos(13,200,32),Blocks.CHEST.getDefaultState(),3);world.setBlockState(new BlockPos(14,200,32),Blocks.CHEST.getDefaultState(),3);
        ItemStack oldReceipt=new ItemStack(LegacyPL4.ITEM_CELL);NBTTagCompound tag=new NBTTagCompound();tag.setString("ItemTransferFault","Unlinked historical receipt");tag.setInteger("ItemEscrowCount",5);tag.setTag("Escrow",diamond(5).writeToNBT(new NBTTagCompound()));oldReceipt.setTagCompound(tag);faultChest(12).setInventorySlotContents(0,oldReceipt);
        faultCheck(LegacyItemSafety.blocked(node(8))&&count(node(8).pending)==8,"old insertion fault and held claim");
        faultCheck(LegacyItemSafety.blocked(node(10))&&count(node(10).pending)==0,"old source fault without substitute");
        NBTTagCompound saved=new NBTTagCompound();node(8).writeToNBT(saved);faultCheck(!saved.hasKey("ItemJournalContext"),"baseline has no operation context");
        faultCheck(!oldReceipt.getTagCompound().hasKey("ItemJournalContext"),"baseline loose receipt has no reliable owner or target");
    }
    private void upgradeFaults(){
        LegacyPL4.enabled=false;
        for(int x:new int[]{8,10}){
            LegacyTile n=node(x);NBTTagCompound c=n.itemJournalContext;
            faultCheck(c!=null&&"legacy-unknown".equals(c.getString("Scope"))&&!n.itemImportPending,"native chunk load imported historical fault");
            faultCheck(!c.hasKey("X")&&!c.hasKey("Phase")&&c.getInteger("HostX")==x,"only observed host, no invented target/phase");
            faultCheck(n.itemLegacyEvidence!=null&&LegacyItemSafety.blocked(n)&&count(n.pending)==(x==8?8:0),"raw evidence and uncertain claim retained");
            faultTokens.add(c.getString("Token"));world.setBlockToAir(new BlockPos(x,200,32));
            ItemStack receipt=ItemStack.EMPTY;for(EntityItem e:world.getEntitiesWithinAABB(EntityItem.class,new AxisAlignedBB(x-1,199,31,x+2,203,34))){if(!e.isDead&&e.getItem().getItem()==LegacyPL4.ITEM_CELL){receipt=e.getItem().copy();e.setDead();}}
            faultCheck(receipt!=null&&!receipt.isEmpty(),"sealed imported receipt after host removal");faultChest(x==8?13:14).setInventorySlotContents(0,receipt);faultChest(x==8?13:14).markDirty();
        }
        faultCheck(LegacyWirelessSafety.data(world).blocked(OWNER,new BlockPos(80,200,80))&&!LegacyWirelessSafety.data(world).blocked("unrelated-owner",new BlockPos(80,200,80)),"owner barrier active after original host removal");
        faultCheck(!faultChest(12).getStackInSlot(0).getTagCompound().hasKey("ItemJournalContext"),"old loose receipt remains unlinked");
    }
    private void verifyFaults(){
        LegacyPL4.enabled=false;
        for(int x:new int[]{8,10}){
            faultCheck(!(world.getTileEntity(new BlockPos(x,200,32)) instanceof LegacyTile),"removed original host persists");
            ItemStack receipt=faultChest(x==8?13:14).getStackInSlot(0);NBTTagCompound tag=receipt.getTagCompound(),c=tag.getCompoundTag("ItemJournalContext");faultTokens.add(c.getString("Token"));
            faultCheck(tag.getInteger("ItemEscrowCount")== (x==8?8:0)&&tag.hasKey("ItemLegacyEvidence"),"sealed receipt and evidence persist across candidate process restart");
            faultCheck("legacy-unknown".equals(c.getString("Scope"))&&!c.hasKey("X")&&!c.hasKey("Phase"),"unknown history stays unknown");
            faultCheck(LegacyWirelessSafety.data(world).receiptStatus(c).contains("original target/phase unknown"),"persisted journal links unknown receipt");
        }
        faultCheck(LegacyWirelessSafety.data(world).blocked(OWNER,new BlockPos(80,200,80))&&!LegacyWirelessSafety.data(world).blocked("unrelated-owner",new BlockPos(80,200,80)),"owner-wide barrier persists across fresh JVM");
        faultCheck(!faultChest(12).getStackInSlot(0).getTagCompound().hasKey("ItemJournalContext"),"unlinked historical loose receipt not authenticated");
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
