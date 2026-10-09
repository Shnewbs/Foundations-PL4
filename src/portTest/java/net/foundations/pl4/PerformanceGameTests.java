package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.foundations.pl4.compat.scenarios.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate;

/** Operation-count regressions and native correctness for the cable/tick performance pass. */
@PrefixGameTestTemplate(false)
public final class PerformanceGameTests {
    private static final UUID OWNER=UUID.fromString("aaaa0000-0000-0000-0000-000000000018");
    private static HostEntity host(GameTestHelper h,BlockPos pos,Kind kind,Direction face){
        h.setBlock(pos,FoundationsPL4.HOST.get());var host=(HostEntity)h.getBlockEntity(pos);
        Part part=new Part(kind,face,OWNER);host.parts.put(part.slot(),part);host.changed();return host;
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void idleCableAvoidsSerializationAndInvalidatesOnEdits(GameTestHelper h){
        var host=host(h,new BlockPos(2,2,2),Kind.DATA_CABLE,Direction.DOWN);
        host.syncIfChanged();long initial=host.syncTagBuildCount();
        for(int i=0;i<1000;i++)host.syncIfChanged();
        net.foundations.pl4.compat.PortAssertions.check(host.syncTagBuildCount()==initial,"1000 unchanged cable checks must allocate zero sync snapshots");
        Part cable=host.parts.get(6);cable.label="Edited cable";host.changed();host.syncIfChanged();
        net.foundations.pl4.compat.PortAssertions.check(host.syncTagBuildCount()==initial+1,"Persistent edits must invalidate the cable shortcut");
        host.setConnections(new int[]{0,0,0,0,0,1});host.syncIfChanged();
        net.foundations.pl4.compat.PortAssertions.check(host.syncTagBuildCount()==initial+2,"Connection geometry must invalidate immediately");
        host.setExternalLeads(1);host.syncIfChanged();
        net.foundations.pl4.compat.PortAssertions.check(host.syncTagBuildCount()==initial+3,"External lead geometry must invalidate immediately");
        var saved=host.getUpdateTag(h.getLevel().registryAccess());host.loadAdditional(saved,h.getLevel().registryAccess());host.syncIfChanged();
        net.foundations.pl4.compat.PortAssertions.check(host.syncTagBuildCount()==initial+4,"Reload must invalidate even when serialized content matches");
        Part reader=new Part(Kind.NETWORK_READER,Direction.NORTH,OWNER);host.parts.put(reader.slot(),reader);host.changed();
        host.syncIfChanged();reader.status="Live reader";host.syncIfChanged();
        net.foundations.pl4.compat.PortAssertions.check(host.syncTagBuildCount()==initial+6,"Multipart hosts must retain full live synchronization");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void redstoneCableStillSynchronizesSignalAndRows(GameTestHelper h){
        var host=host(h,new BlockPos(2,2,2),Kind.REDSTONE_CABLE,Direction.DOWN);Part cable=host.parts.get(6);
        cable.status="Connected";cable.rows.add(new Part.Row("signal","Redstone",0,15,""));host.syncIfChanged();
        long initial=host.syncTagBuildCount();
        for(int i=0;i<1000;i++){cable.rows.clear();cable.rows.add(new Part.Row("signal","Redstone",0,15,""));host.syncIfChanged();}
        net.foundations.pl4.compat.PortAssertions.check(host.syncTagBuildCount()==initial,"Equivalent freshly sampled rows must not serialize again");
        cable.signal=15;host.syncIfChanged();
        net.foundations.pl4.compat.PortAssertions.check(host.syncTagBuildCount()==initial+1,"A changed redstone signal must synchronize");
        cable.rows.clear();cable.rows.add(new Part.Row("signal","Redstone",15,15,""));host.syncIfChanged();
        net.foundations.pl4.compat.PortAssertions.check(host.syncTagBuildCount()==initial+2,"Row changes must synchronize independently of the signal");
        cable.status="Provider error";host.syncIfChanged();
        net.foundations.pl4.compat.PortAssertions.check(host.syncTagBuildCount()==initial+3,"Error/status changes must synchronize");h.succeed();
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
        net.foundations.pl4.compat.PortAssertions.check(plan.endpoints().size()==2&&plan.drivers().size()==1,"4096 cable refs must be absent from repeated transfer work");
        TransferEngine.run(h.getLevel().getServer(),plan);
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==17,"Cached plan must conserve REMOVE exports");
        driver.transferMode=1; // Reuse membership, but evaluate live mode in the next cycle.
        TransferEngine.run(h.getLevel().getServer(),plan);
        net.foundations.pl4.compat.PortAssertions.check(to.getItem(0).isEmpty()&&from.getItem(0).getCount()==17&&driver.pendingItem.isEmpty(),"Cached plan must honor changed ADD mode and preserve escrow conservation");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void repeatedFiltersCompileOnceAndHonorEdits(GameTestHelper h){
        Part p=new Part(Kind.TRANSFER_NODE,Direction.DOWN,OWNER);p.filter=" minecraft:diamond , #minecraft:planks , #invalid tag ";
        var diamond=new ItemStack(Items.DIAMOND);var plank=new ItemStack(Items.OAK_PLANKS);long before=DataSampler.filterCompileCount();
        for(int i=0;i<1000;i++)net.foundations.pl4.compat.PortAssertions.check(DataSampler.matches(diamond,p)&&DataSampler.matches(plank,p),"Both ID and tag matching must remain live");
        net.foundations.pl4.compat.PortAssertions.check(DataSampler.filterCompileCount()==before+1,"2000 matches should parse the filter once");
        p.whitelist=false;net.foundations.pl4.compat.PortAssertions.check(!DataSampler.matches(diamond,p),"Whitelist inversion must apply without recompilation");
        net.foundations.pl4.compat.PortAssertions.check(DataSampler.filterCompileCount()==before+1,"Whitelist edits must not require parsing the same tokens");
        p.whitelist=true;p.filter="diamond";net.foundations.pl4.compat.PortAssertions.check(!DataSampler.matches(diamond,p),"Bare item IDs must preserve the old exact-ID semantics");
        p.filter="minecraft:water";net.foundations.pl4.compat.PortAssertions.check(DataSampler.matches(new FluidStack(Fluids.WATER,1000),p),"Filter edit must recompile for fluid IDs");
        p.filter="#minecraft:water";net.foundations.pl4.compat.PortAssertions.check(DataSampler.matches(new FluidStack(Fluids.WATER,1000),p),"Fluid tag matching must remain live");
        p.filter="";p.whitelist=false;net.foundations.pl4.compat.PortAssertions.check(DataSampler.matches(diamond,p),"An empty filter must still allow everything");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void localGeometryRepairsStaleArmSnapshotsWithoutGraphRebuild(GameTestHelper h){
        var a=host(h,new BlockPos(1,2,2),Kind.DATA_CABLE,Direction.DOWN);
        var b=host(h,new BlockPos(2,2,2),Kind.DATA_CABLE,Direction.DOWN);
        var c=host(h,new BlockPos(3,2,2),Kind.DATA_CABLE,Direction.DOWN);
        var d=host(h,new BlockPos(4,2,2),Kind.DATA_CABLE,Direction.DOWN);
        NetworkEngine.rebuild(h.getLevel().getServer());long builds=NetworkEngine.topologyBuildCount();
        // Reproduce a received snapshot with valid parts but old/disconnected geometry.
        var stale=b.getUpdateTag(h.getLevel().registryAccess());stale.putIntArray("cableConnections",new int[6]);
        b.loadAdditional(stale,h.getLevel().registryAccess());
        a.setConnections(new int[6]);CableGeometry.refresh(b);
        net.foundations.pl4.compat.PortAssertions.check(a.connection(Direction.EAST)==1&&b.connection(Direction.WEST)==1&&b.connection(Direction.EAST)==1&&c.connection(Direction.WEST)==1,"Both ends must connect from parts in the same local refresh");
        net.foundations.pl4.compat.PortAssertions.check(c.connection(Direction.EAST)==1&&d.connection(Direction.WEST)==1,"Updating a neighbour must preserve its farther connection");
        stale=a.getUpdateTag(h.getLevel().registryAccess());stale.putIntArray("cableConnections",new int[6]);
        a.loadAdditional(stale,h.getLevel().registryAccess());CableGeometry.refresh(a);
        net.foundations.pl4.compat.PortAssertions.check(a.connection(Direction.EAST)==1&&b.connection(Direction.WEST)==1,"A later stale arm snapshot must not reopen the gap");
        net.foundations.pl4.compat.PortAssertions.check(NetworkEngine.topologyBuildCount()==builds,"Local geometry must not rebuild the global network");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void localGeometryHonorsBlockedPortsAndCableFamilies(GameTestHelper h){
        var a=host(h,new BlockPos(2,2,2),Kind.DATA_CABLE,Direction.DOWN);
        var b=host(h,new BlockPos(3,2,2),Kind.DATA_CABLE,Direction.DOWN);
        b.parts.get(6).blockedFaces=1<<Direction.WEST.ordinal();CableGeometry.refresh(b);
        net.foundations.pl4.compat.PortAssertions.check(a.connection(Direction.EAST)==0&&b.connection(Direction.WEST)==0,"Disabled neighbour port must block both arms");
        b.parts.get(6).blockedFaces=0;CableGeometry.refresh(b);
        net.foundations.pl4.compat.PortAssertions.check(a.connection(Direction.EAST)==1&&b.connection(Direction.WEST)==1,"Enabling port must restore both arms immediately");
        b.parts.put(6,new Part(Kind.REDSTONE_CABLE,Direction.DOWN,OWNER));CableGeometry.refresh(b);
        net.foundations.pl4.compat.PortAssertions.check(a.connection(Direction.EAST)==0&&b.connection(Direction.WEST)==0,"Different cable families must never visually connect");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void localGeometryReconcilesMultipartLeadsAndRemoval(GameTestHelper h){
        var cable=host(h,new BlockPos(2,2,2),Kind.DATA_CABLE,Direction.DOWN);
        var device=host(h,new BlockPos(3,2,2),Kind.NODE,Direction.EAST);
        Part node=device.parts.get(Direction.EAST.ordinal());CableGeometry.refresh(device);
        net.foundations.pl4.compat.PortAssertions.check(cable.connection(Direction.EAST)==0&&!device.externalLead(node),"Endpoint without a placed centre cable must not extend its neighbor");
        device.parts.clear();CableGeometry.refresh(device);
        net.foundations.pl4.compat.PortAssertions.check(cable.connection(Direction.EAST)==0&&!device.externalLead(node),"Removed part must clear both arm and lead immediately");
        device.parts.put(6,new Part(Kind.DATA_CABLE,Direction.DOWN,OWNER));CableGeometry.refresh(device);
        net.foundations.pl4.compat.PortAssertions.check(cable.connection(Direction.EAST)==1,"New cable must join without waiting for sampling");
        device.setRemoved();CableGeometry.refresh(device);
        net.foundations.pl4.compat.PortAssertions.check(cable.connection(Direction.EAST)==0,"Removed/unloaded host must be excluded even before its world slot disappears");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkItemCapIsSharedAcrossDriversAndResetsPerCycle(GameTestHelper h){
        int old=PLConfig.NETWORK_ITEM_RATE.get();try{
            PLConfig.NETWORK_ITEM_RATE.set(5);
            h.setBlock(new BlockPos(1,1,1),Blocks.CHEST);h.setBlock(new BlockPos(1,1,4),Blocks.CHEST);h.setBlock(new BlockPos(5,1,1),Blocks.CHEST);
            var a=(ChestBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));var b=(ChestBlockEntity)h.getBlockEntity(new BlockPos(1,1,4));var to=(ChestBlockEntity)h.getBlockEntity(new BlockPos(5,1,1));a.setItem(0,new ItemStack(Items.DIAMOND,16));b.setItem(0,new ItemStack(Items.DIAMOND,16));
            var ah=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST);var bh=host(h,new BlockPos(2,1,4),Kind.TRANSFER_NODE,Direction.WEST);var sink=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);
            var ar=new NetworkEngine.Ref(ah,ah.parts.get(Direction.WEST.ordinal()));var br=new NetworkEngine.Ref(bh,bh.parts.get(Direction.WEST.ordinal()));ar.part().transferMode=2;br.part().transferMode=2;
            var plan=TransferEngine.prepare(List.of(ar,br,new NetworkEngine.Ref(sink,sink.parts.get(Direction.EAST.ordinal()))));
            TransferEngine.run(h.getLevel().getServer(),plan);net.foundations.pl4.compat.PortAssertions.check(to.getItem(0).getCount()==5&&a.getItem(0).getCount()+b.getItem(0).getCount()==27,"Multiple drivers share one five-item delivery cap");
            TransferEngine.run(h.getLevel().getServer(),plan);net.foundations.pl4.compat.PortAssertions.check(to.getItem(0).getCount()==10&&a.getItem(0).getCount()+b.getItem(0).getCount()==22,"Next cycle renews cap without duplicating or losing items");
            h.succeed();
        }finally{PLConfig.NETWORK_ITEM_RATE.set(old);}
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkItemCapBoundsEscrowAndIndependentNetworks(GameTestHelper h){
        int old=PLConfig.NETWORK_ITEM_RATE.get();try{
            PLConfig.NETWORK_ITEM_RATE.set(3);
            h.setBlock(new BlockPos(5,1,1),Blocks.CHEST);h.setBlock(new BlockPos(5,1,4),Blocks.CHEST);
            var toA=(ChestBlockEntity)h.getBlockEntity(new BlockPos(5,1,1));var toB=(ChestBlockEntity)h.getBlockEntity(new BlockPos(5,1,4));
            var a=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST);var b=host(h,new BlockPos(2,1,4),Kind.TRANSFER_NODE,Direction.WEST);var sinkA=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);var sinkB=host(h,new BlockPos(4,1,4),Kind.NODE,Direction.EAST);
            var ar=new NetworkEngine.Ref(a,a.parts.get(Direction.WEST.ordinal()));var br=new NetworkEngine.Ref(b,b.parts.get(Direction.WEST.ordinal()));ar.part().transferMode=2;br.part().transferMode=2;ar.part().pendingItem=new ItemStack(Items.DIAMOND,12);br.part().pendingItem=new ItemStack(Items.DIAMOND,12);
            TransferEngine.run(h.getLevel().getServer(),List.of(ar,new NetworkEngine.Ref(sinkA,sinkA.parts.get(Direction.EAST.ordinal()))));
            TransferEngine.run(h.getLevel().getServer(),List.of(br,new NetworkEngine.Ref(sinkB,sinkB.parts.get(Direction.EAST.ordinal()))));
            net.foundations.pl4.compat.PortAssertions.check(toA.getItem(0).getCount()==3&&toB.getItem(0).getCount()==3&&ar.part().pendingItem.getCount()==9&&br.part().pendingItem.getCount()==9,"Escrow cannot bypass cap; separate networks each receive a full budget");h.succeed();
        }finally{PLConfig.NETWORK_ITEM_RATE.set(old);}
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void nodeItemLimitStillAppliesUnderHigherSharedCap(GameTestHelper h){
        int oldCap=PLConfig.NETWORK_ITEM_RATE.get(),oldNode=PLConfig.ITEM_RATE.get();try{
            PLConfig.NETWORK_ITEM_RATE.set(9);PLConfig.ITEM_RATE.set(2);
            h.setBlock(new BlockPos(1,1,1),Blocks.CHEST);h.setBlock(new BlockPos(5,1,1),Blocks.CHEST);var from=(ChestBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));var to=(ChestBlockEntity)h.getBlockEntity(new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,10));
            var source=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST);var sink=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);var driver=new NetworkEngine.Ref(source,source.parts.get(Direction.WEST.ordinal()));driver.part().transferMode=2;
            TransferEngine.run(h.getLevel().getServer(),List.of(driver,new NetworkEngine.Ref(sink,sink.parts.get(Direction.EAST.ordinal()))));
            net.foundations.pl4.compat.PortAssertions.check(to.getItem(0).getCount()==2&&from.getItem(0).getCount()==8,"Per-node extraction limit remains stricter when below shared cap");h.succeed();
        }finally{PLConfig.NETWORK_ITEM_RATE.set(oldCap);PLConfig.ITEM_RATE.set(oldNode);}
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkItemCapAlsoBoundsAddImportsAndIncomingEscrow(GameTestHelper h){
        int old=PLConfig.NETWORK_ITEM_RATE.get();try{
            PLConfig.NETWORK_ITEM_RATE.set(3);h.setBlock(new BlockPos(1,1,1),Blocks.CHEST);h.setBlock(new BlockPos(5,1,1),Blocks.CHEST);h.setBlock(new BlockPos(5,1,4),Blocks.CHEST);
            var from=(ChestBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));var toA=(ChestBlockEntity)h.getBlockEntity(new BlockPos(5,1,1));var toB=(ChestBlockEntity)h.getBlockEntity(new BlockPos(5,1,4));from.setItem(0,new ItemStack(Items.DIAMOND,20));
            var source=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST);var a=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);var b=host(h,new BlockPos(4,1,4),Kind.TRANSFER_NODE,Direction.EAST);
            var ar=new NetworkEngine.Ref(a,a.parts.get(Direction.EAST.ordinal()));var br=new NetworkEngine.Ref(b,b.parts.get(Direction.EAST.ordinal()));ar.part().transferMode=1;br.part().transferMode=1;
            var plan=TransferEngine.prepare(List.of(new NetworkEngine.Ref(source,source.parts.get(Direction.WEST.ordinal())),ar,br));TransferEngine.run(h.getLevel().getServer(),plan);
            net.foundations.pl4.compat.PortAssertions.check(toA.getItem(0).getCount()+toB.getItem(0).getCount()==3&&from.getItem(0).getCount()==17,"ADD imports across multiple drivers share delivery cap");
            ar.part().pendingItem=new ItemStack(Items.DIAMOND,10);int before=toA.getItem(0).getCount()+toB.getItem(0).getCount();TransferEngine.run(h.getLevel().getServer(),plan);
            net.foundations.pl4.compat.PortAssertions.check(toA.getItem(0).getCount()+toB.getItem(0).getCount()==before+3&&ar.part().pendingItem.getCount()==7&&from.getItem(0).getCount()==17,"Incoming ADD escrow must consume the same shared budget before new extraction");h.succeed();
        }finally{PLConfig.NETWORK_ITEM_RATE.set(old);}
    }

}
