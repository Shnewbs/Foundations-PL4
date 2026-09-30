package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Operation-count regressions and native correctness for the cable/tick performance pass. */
@PrefixGameTestTemplate(false)
public final class PerformanceGameTests {
    private static final UUID OWNER=UUID.fromString("aaaa0000-0000-0000-000000000018");
    private static HostEntity host(GameTestHelper h,BlockPos pos,Kind kind,Direction face){
        h.setBlock(pos,FoundationsPL4.HOST.get());var host=(HostEntity)h.getBlockEntity(pos);
        Part part=new Part(kind,face,OWNER);host.parts.put(part.slot(),part);host.changed();return host;
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void idleCableAvoidsSerializationAndInvalidatesOnEdits(GameTestHelper h){
        var host=host(h,new BlockPos(2,2,2),Kind.DATA_CABLE,Direction.DOWN);
        host.syncIfChanged();long initial=host.syncTagBuildCount();
        for(int i=0;i<1000;i++)host.syncIfChanged();
        h.assertTrue(host.syncTagBuildCount()==initial,"1000 unchanged cable checks must allocate zero sync snapshots");
        Part cable=host.parts.get(6);cable.label="Edited cable";host.changed();host.syncIfChanged();
        h.assertTrue(host.syncTagBuildCount()==initial+1,"Persistent edits must invalidate the cable shortcut");
        host.setConnections(new int[]{0,0,0,0,0,1});host.syncIfChanged();
        h.assertTrue(host.syncTagBuildCount()==initial+2,"Connection geometry must invalidate immediately");
        host.setExternalLeads(1);host.syncIfChanged();
        h.assertTrue(host.syncTagBuildCount()==initial+3,"External lead geometry must invalidate immediately");
        var saved=host.getUpdateTag(h.getLevel().registryAccess());host.loadAdditional(saved,h.getLevel().registryAccess());host.syncIfChanged();
        h.assertTrue(host.syncTagBuildCount()==initial+4,"Reload must invalidate even when serialized content matches");
        Part reader=new Part(Kind.NETWORK_READER,Direction.NORTH,OWNER);host.parts.put(reader.slot(),reader);host.changed();
        host.syncIfChanged();reader.status="Live reader";host.syncIfChanged();
        h.assertTrue(host.syncTagBuildCount()==initial+6,"Multipart hosts must retain full live synchronization");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void redstoneCableStillSynchronizesSignalAndRows(GameTestHelper h){
        var host=host(h,new BlockPos(2,2,2),Kind.REDSTONE_CABLE,Direction.DOWN);Part cable=host.parts.get(6);
        cable.status="Connected";cable.rows.add(new Part.Row("signal","Redstone",0,15,""));host.syncIfChanged();
        long initial=host.syncTagBuildCount();
        for(int i=0;i<1000;i++){cable.rows.clear();cable.rows.add(new Part.Row("signal","Redstone",0,15,""));host.syncIfChanged();}
        h.assertTrue(host.syncTagBuildCount()==initial,"Equivalent freshly sampled rows must not serialize again");
        cable.signal=15;host.syncIfChanged();
        h.assertTrue(host.syncTagBuildCount()==initial+1,"A changed redstone signal must synchronize");
        cable.rows.clear();cable.rows.add(new Part.Row("signal","Redstone",15,15,""));host.syncIfChanged();
        h.assertTrue(host.syncTagBuildCount()==initial+2,"Row changes must synchronize independently of the signal");
        cable.status="Provider error";host.syncIfChanged();
        h.assertTrue(host.syncTagBuildCount()==initial+3,"Error/status changes must synchronize");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void preparedTransferPlanConservesItemsAndReadsLiveModes(GameTestHelper h){
        h.setBlock(new BlockPos(1,1,1),Blocks.CHEST);h.setBlock(new BlockPos(5,1,1),Blocks.CHEST);
        var from=(ChestBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));var to=(ChestBlockEntity)h.getBlockEntity(new BlockPos(5,1,1));
        from.setItem(0,new ItemStack(Items.DIAMOND,17));
        var source=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST);
        var sink=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);Part driver=source.parts.get(Direction.WEST.ordinal());driver.transferMode=2;
        var sourceRef=new NetworkEngine.Ref(source,driver);var sinkRef=new NetworkEngine.Ref(sink,sink.parts.get(Direction.EAST.ordinal()));
        List<NetworkEngine.Ref> network=new ArrayList<>();network.add(sourceRef);network.add(sinkRef);
        // A cable-heavy membership input exercises exclusion without needing a giant test structure.
        for(int i=0;i<4096;i++)network.add(new NetworkEngine.Ref(source,new Part(Kind.DATA_CABLE,Direction.DOWN,OWNER)));
        var plan=TransferEngine.prepare(network);
        h.assertTrue(plan.endpoints().size()==2&&plan.drivers().size()==1,"4096 cable refs must be absent from repeated transfer work");
        TransferEngine.run(h.getLevel().getServer(),plan);
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==17,"Cached plan must conserve REMOVE exports");
        driver.transferMode=1; // Reuse membership, but evaluate live mode in the next cycle.
        TransferEngine.run(h.getLevel().getServer(),plan);
        h.assertTrue(to.getItem(0).isEmpty()&&from.getItem(0).getCount()==17&&driver.pendingItem.isEmpty(),"Cached plan must honor changed ADD mode and preserve escrow conservation");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void repeatedFiltersCompileOnceAndHonorEdits(GameTestHelper h){
        Part p=new Part(Kind.TRANSFER_NODE,Direction.DOWN,OWNER);p.filter=" minecraft:diamond , #minecraft:planks , #invalid tag ";
        var diamond=new ItemStack(Items.DIAMOND);var plank=new ItemStack(Items.OAK_PLANKS);long before=DataSampler.filterCompileCount();
        for(int i=0;i<1000;i++)h.assertTrue(DataSampler.matches(diamond,p)&&DataSampler.matches(plank,p),"Both ID and tag matching must remain live");
        h.assertTrue(DataSampler.filterCompileCount()==before+1,"2000 matches should parse the filter once");
        p.whitelist=false;h.assertTrue(!DataSampler.matches(diamond,p),"Whitelist inversion must apply without recompilation");
        h.assertTrue(DataSampler.filterCompileCount()==before+1,"Whitelist edits must not require parsing the same tokens");
        p.whitelist=true;p.filter="diamond";h.assertTrue(!DataSampler.matches(diamond,p),"Bare item IDs must preserve the old exact-ID semantics");
        p.filter="minecraft:water";h.assertTrue(DataSampler.matches(new FluidStack(Fluids.WATER,1000),p),"Filter edit must recompile for fluid IDs");
        p.filter="#minecraft:water";h.assertTrue(DataSampler.matches(new FluidStack(Fluids.WATER,1000),p),"Fluid tag matching must remain live");
        p.filter="";p.whitelist=false;h.assertTrue(DataSampler.matches(diamond,p),"An empty filter must still allow everything");h.succeed();
    }
}
