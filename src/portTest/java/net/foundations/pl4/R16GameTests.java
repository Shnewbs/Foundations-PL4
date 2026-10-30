package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native item-path acceptance for the R16 passive-node transfer pool. */
@PrefixGameTestTemplate(false)
public final class R16GameTests {
    private static final UUID OWNER=UUID.fromString("aaaa0000-0000-0000-0000-000000000016");
    private static HostEntity host(GameTestHelper h,BlockPos p,Kind kind,Direction face){
        h.setBlock(p,FoundationsPL4.HOST.get());HostEntity host=(HostEntity)h.getBlockEntity(p);
        Part part=new Part(kind,face,OWNER);host.parts.put(part.slot(),part);
        host.parts.put(6,new Part(Kind.DATA_CABLE,Direction.DOWN,OWNER));host.changed();return host;
    }
    private static Part part(HostEntity host,Direction face){return host.parts.get(face.ordinal());}
    private static ChestBlockEntity chest(GameTestHelper h,BlockPos p){h.setBlock(p,Blocks.CHEST);return (ChestBlockEntity)h.getBlockEntity(p);}
    private static NetworkEngine.Ref ref(HostEntity h,Direction face){return new NetworkEngine.Ref(h,part(h,face));}

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void staleLoadedHostCannotReenterAReplacedInventoryAlias(GameTestHelper h){
        BlockPos position=new BlockPos(2,1,1);
        HostEntity original=host(h,position,Kind.TRANSFER_NODE,Direction.WEST);
        var server=h.getLevel().getServer();
        NetworkEngine.rebuild(server);
        h.assertTrue(NetworkEngine.all(server).stream().anyMatch(r->r.host()==original),
            "Initially installed source host must appear in the loaded native graph");
        Part originalNode=part(original,Direction.WEST);
        h.assertTrue(NetworkEngine.storageNodes(server,originalNode).stream()
            .anyMatch(r->r.host()==original&&r.part()==originalNode),
            "Attached native storage node must be reachable in the cached physical graph");
        // Model a provider callback detaching a part without sending a topology event.
        original.parts.remove(originalNode.slot());
        h.assertTrue(NetworkEngine.storageNodes(server,originalNode).isEmpty(),
            "Cached storage aliases must reject a detached part before the next rebuild");
        original.parts.put(originalNode.slot(),originalNode);
        NetworkEngine.rebuild(server);
        h.setBlock(position,Blocks.CHEST);
        // Model a late callback restoring a stale registration after ordinary removal.
        original.clearRemoved();
        NetworkEngine.add(original);
        h.assertTrue(NetworkEngine.all(server).stream().noneMatch(r->r.host()==original),
            "Stale loaded HostEntity at an occupied position must never regain network authority");
        NetworkEngine.rebuild(server);
        h.assertTrue(NetworkEngine.storageNodes(server,part(original,Direction.WEST)).isEmpty(),
            "The stale node must have no cached reachable inventory aliases");
        NetworkEngine.remove(original);
        h.succeed();
    }

    /** Reader telemetry must not leak stale inventory targets across an owner or cable
     * revocation before the next scheduled network sample. */
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void readerExportsDropRevokedAndDetachedInventoryAliases(GameTestHelper h){
        var server=h.getLevel().getServer();
        HostEntity readerHost=host(h,new BlockPos(2,1,1),Kind.INVENTORY_READER,Direction.WEST);
        HostEntity remoteHost=host(h,new BlockPos(3,1,1),Kind.NODE,Direction.EAST);
        Part reader=part(readerHost,Direction.WEST), remote=part(remoteHost,Direction.EAST);
        BlockPos remoteInventory=remoteHost.getBlockPos().east();
        NetworkEngine.rebuild(server);
        h.assertTrue(NetworkEngine.targetsFor(server,reader).stream()
                .anyMatch(link->link.pos().equals(remoteInventory)),
            "Initially connected reader must see its owned network's inventory target");
        // Do not call changed(): a foreign provider callback can revoke an owner
        // between scheduled graph samples, leaving a previously valid cached group.
        remote.owner=UUID.randomUUID();
        h.assertTrue(NetworkEngine.targetsFor(server,reader).stream()
                .noneMatch(link->link.pos().equals(remoteInventory)),
            "Unannounced remote owner revocation must hide its old reader target immediately");
        remote.owner=OWNER;remoteHost.changed();
        h.assertTrue(NetworkEngine.targetsFor(server,reader).stream()
                .anyMatch(link->link.pos().equals(remoteInventory)),
            "Restored matching owner should rejoin only the live network");
        Part cable=remoteHost.parts.remove(6);
        h.assertTrue(NetworkEngine.targetsFor(server,reader).stream()
                .noneMatch(link->link.pos().equals(remoteInventory)),
            "Detached intermediate cable cannot expose the stale remote inventory alias");
        remoteHost.parts.put(6,cable);remoteHost.changed();
        h.assertTrue(NetworkEngine.targetsFor(server,reader).stream()
                .anyMatch(link->link.pos().equals(remoteInventory)),
            "Valid reader export must recover after cable reattachment");
        readerHost.parts.remove(reader.slot());
        h.assertTrue(NetworkEngine.targetsFor(server,reader).isEmpty(),
            "Detached reader part cannot reuse cached target exports");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void passiveNodeFeedsAddTransferNode(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(add,Direction.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==17,"Normal Node must feed an ADD Transfer Node");itemQuarantineChecks(ref(add,Direction.EAST));resourceQuarantineChecks(ref(add,Direction.EAST));
        from.setItem(0,new ItemStack(Items.DIAMOND,5));part(add,Direction.EAST).resourceTransferFault="Interrupted provider mutation";
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        if(from.getItem(0).getCount()!=5||to.getItem(0).getCount()!=17)throw new IllegalStateException("Cycle fence must exclude a quarantined driver");
        part(add,Direction.EAST).resourceTransferFault="";part(node,Direction.WEST).resourceTransferFault="Unresolved passive endpoint";
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        if(from.getItem(0).getCount()!=5||to.getItem(0).getCount()!=17)throw new IllegalStateException("Cycle fence must exclude a quarantined passive endpoint");
        part(node,Direction.WEST).resourceTransferFault="";
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        if(!from.getItem(0).isEmpty()||to.getItem(0).getCount()!=22)throw new IllegalStateException("Healthy endpoint resumes without rebuilding the plan");wirelessHandoffQuarantine(ref(node,Direction.WEST));h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void removeTransferNodeFeedsPassiveNode(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.EMERALD,13));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),node=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);part(remove,Direction.WEST).transferMode=2;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==13,"REMOVE Transfer Node must feed a normal Node endpoint");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void explicitAddBeatsPassiveNodeOnTie(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),explicit=chest(h,new BlockPos(5,1,1)),passive=chest(h,new BlockPos(8,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST),node=host(h,new BlockPos(7,1,1),Kind.NODE,Direction.EAST);
        part(remove,Direction.WEST).transferMode=2;part(add,Direction.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST),ref(add,Direction.EAST)));
        h.assertTrue(explicit.getItem(0).getCount()==17&&passive.getItem(0).isEmpty(),"Explicit ADD peer must win an equal-priority passive endpoint");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void quarantinedPassiveItemEndpointNeverAcceptsDelivery(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));
        from.setItem(0,new ItemStack(Items.DIAMOND,13));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST);
        HostEntity node=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);
        Part source=part(remove,Direction.WEST),sink=part(node,Direction.EAST);
        source.transferMode=2;
        var plan=TransferEngine.prepare(List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST)));
        sink.itemTransferFault="Uncertain external insertion; inspect provider";
        TransferEngine.run(h.getLevel().getServer(),plan);
        h.assertTrue(from.getItem(0).getCount()==13&&to.getItem(0).isEmpty()&&source.pendingItem.isEmpty(),
            "Unresolved passive item endpoint must not receive new delivery or trigger extraction");
        sink.itemTransferFault="";
        sink.opaqueItemEscrow=new net.minecraft.nbt.CompoundTag();
        TransferEngine.run(h.getLevel().getServer(),plan);
        h.assertTrue(from.getItem(0).getCount()==13&&to.getItem(0).isEmpty()&&source.pendingItem.isEmpty(),
            "Opaque saved item escrow cannot become a passive destination");
        sink.opaqueItemEscrow=null;
        TransferEngine.run(h.getLevel().getServer(),plan);
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==13&&source.pendingItem.isEmpty(),
            "Explicitly restored healthy passive destination resumes the same cached plan");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void fullPassiveDestinationDoesNotExtract(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));for(int i=0;i<to.getContainerSize();i++)to.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),node=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);part(remove,Direction.WEST).transferMode=2;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST)));
        h.assertTrue(from.getItem(0).getCount()==17&&part(remove,Direction.WEST).pendingItem.isEmpty(),"Full passive endpoint must not extract or escrow source items");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void addFilterAppliesToPassiveSource(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.IRON_INGOT,8));from.setItem(1,new ItemStack(Items.DIAMOND,7));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);Part p=part(add,Direction.EAST);p.transferMode=1;p.filter="minecraft:diamond";
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        h.assertTrue(from.getItem(0).getCount()==8&&from.getItem(1).isEmpty()&&to.getItem(0).is(Items.DIAMOND)&&to.getItem(0).getCount()==7,"ADD filter must constrain passive source imports");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void bidirectionalDoesNotBlindlyUsePassivePool(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,9));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST),both=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(both,Direction.EAST).transferMode=3;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(both,Direction.EAST)));
        h.assertTrue(from.getItem(0).getCount()==9&&to.getItem(0).isEmpty(),"ADD/REMOVE must be peer-only until directional filters/channels exist");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void bidirectionalCanFeedExplicitAddPeer(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.GOLD_INGOT,12));
        HostEntity both=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(both,Direction.WEST).transferMode=3;part(add,Direction.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(both,Direction.WEST),ref(add,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==12,"ADD/REMOVE may act as source for an explicit ADD peer");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void removeCanFeedBidirectionalExplicitPeer(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.REDSTONE,21));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),both=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(remove,Direction.WEST).transferMode=2;part(both,Direction.EAST).transferMode=3;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(both,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==21,"REMOVE may feed an explicit ADD/REMOVE peer");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void r3ChannelIsolationAndPersistence(GameTestHelper h){
        var from=chest(h,new BlockPos(1,1,1));var to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        var remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST);var node=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);
        var source=part(remove,Direction.WEST);var sink=part(node,Direction.EAST);source.transferMode=2;source.outputChannel="ore";sink.inputChannel="fuel";
        var plan=TransferEngine.prepare(List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST)));
        TransferEngine.run(h.getLevel().getServer(),plan);
        h.assertTrue(from.getItem(0).getCount()==17&&to.getItem(0).isEmpty()&&source.pendingItem.isEmpty(),"Different channels cannot extract or deliver");
        sink.inputChannel="ore";TransferEngine.run(h.getLevel().getServer(),plan);
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==17,"Matching live channel works without rebuilding plan");

        source.resourceTransferFault="Interrupted fluid insertion";source.pendingFluid=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,725);
        source.itemTransferFault="Interrupted item insertion";source.pendingItem=new ItemStack(Items.DIAMOND,4096);
        var restored=Part.load(source.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        if(!ItemTransferSafety.blocked(restored)||restored.pendingItem.getCount()!=4096||restored.routeEditable())throw new IllegalStateException("Item quarantine/count must survive native save reload");
        if(!ResourceTransferSafety.blocked(restored)||restored.pendingFluid.getAmount()!=725||restored.energyRouteEditable())throw new IllegalStateException("Resource quarantine survives native reload");
        h.assertTrue(restored.outputChannel.equals("ore")&&restored.inputChannel.isEmpty(),"Directional channels persist");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void r3EqualPrioritySinksShareRepeatedSingleSlotExports(GameTestHelper h){
        int old=PLConfig.NETWORK_ITEM_RATE.get();try{
            PLConfig.NETWORK_ITEM_RATE.set(1);
            var from=chest(h,new BlockPos(1,1,1));var a=chest(h,new BlockPos(5,1,1));var b=chest(h,new BlockPos(8,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,20));
            var source=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST);part(source,Direction.WEST).transferMode=2;
            var sa=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);var sb=host(h,new BlockPos(7,1,1),Kind.NODE,Direction.EAST);
            var plan=TransferEngine.prepare(List.of(ref(source,Direction.WEST),ref(sa,Direction.EAST),ref(sb,Direction.EAST)));
            for(int n=0;n<20;n++)TransferEngine.run(h.getLevel().getServer(),plan);
            h.assertTrue(from.getItem(0).isEmpty()&&a.getItem(0).getCount()==10&&b.getItem(0).getCount()==10&&part(source,Direction.WEST).pendingItem.isEmpty(),"Equal sinks share 20 bounded cycles independently of source slot cursor");h.succeed();
        }finally{PLConfig.NETWORK_ITEM_RATE.set(old);}
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void r3ComponentControlsPreserveOtherLinksAndClockSettings(GameTestHelper h){
        var array=host(h,new BlockPos(2,1,1),Kind.ARRAY,Direction.WEST);var p=part(array,Direction.WEST);
        var a=new Part.Link("minecraft:overworld",new BlockPos(1,1,1),Direction.UP,null,null);var b=new Part.Link("minecraft:overworld",new BlockPos(5,1,1),Direction.DOWN,null,null);p.links.add(a);p.links.add(b);
        var user=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-R3-Test"));var at=array.getBlockPos();user.setPos(at.getX(),at.getY()+1,at.getZ());
        var remove=new PLPackets.Edit(at,p.slot(),p.identity,"remove_link",ReaderChannels.id(a));PLPackets.edit(user,remove);PLPackets.edit(user,remove);
        h.assertTrue(p.links.equals(List.of(b)),"Repeated stale removal cannot delete the next link");
        var clock=host(h,new BlockPos(4,1,1),Kind.CLOCK,Direction.WEST);var c=part(clock,Direction.WEST);var cp=clock.getBlockPos();user.setPos(cp.getX(),cp.getY()+1,cp.getZ());
        PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_pulse","12"));PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_phase","30"));PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_paused","true"));
        var restored=Part.load(c.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(restored.clockPulse==12&&restored.clockPhase==30&&restored.clockPaused,"Clock controls persist");
        PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_pulse","-1"));h.assertTrue(c.clockPulse==12,"Invalid clock setting rejected");
        c.owner=UUID.randomUUID();PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_phase","7"));h.assertTrue(c.clockPhase==30,"Foreign owner cannot edit clock");h.succeed();
    }


    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void directionalFiltersConstrainPassiveImportsAndExports(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.IRON_INGOT,8));from.setItem(1,new ItemStack(Items.DIAMOND,7));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);
        Part source=part(node,Direction.WEST),sink=part(add,Direction.EAST);sink.transferMode=1;
        source.outputFilterMode="ALLOW";source.outputFilter="minecraft:diamond";sink.inputFilterMode="DENY";sink.inputFilter="minecraft:diamond";
        var network=List.of(ref(node,Direction.WEST),ref(add,Direction.EAST));TransferEngine.run(h.getLevel().getServer(),network);
        h.assertTrue(from.getItem(0).getCount()==8&&from.getItem(1).getCount()==7&&to.getItem(0).isEmpty(),"Both directional filters must agree before extracting");
        sink.inputFilter="minecraft:iron_ingot";TransferEngine.run(h.getLevel().getServer(),network);
        h.assertTrue(from.getItem(0).getCount()==8&&from.getItem(1).isEmpty()&&to.getItem(0).getCount()==7,"Passive output and ADD input filters route only matching items");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void directionalFiltersApplyToFluidsAndRetainLegacyDefaults(GameTestHelper h){
        Part p=new Part(Kind.TRANSFER_NODE,Direction.UP,OWNER);p.filter="minecraft:water";
        var water=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000);
        var lava=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000);
        h.assertTrue(TransferFilters.fluids(water,p,true)&&!TransferFilters.fluids(lava,p,true),"INHERIT keeps the legacy filter");
        p.outputFilterMode="ALLOW";p.outputFilter="minecraft:lava";
        h.assertTrue(TransferFilters.fluids(lava,p,false)&&!TransferFilters.fluids(water,p,false)&&TransferFilters.fluids(water,p,true),"Receive and send fluid filters are independent");
        p.outputFilterMode="DENY";h.assertTrue(!TransferFilters.fluids(lava,p,false)&&TransferFilters.fluids(water,p,false),"Mode changes do not leave stale cached results");
        Part saved=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(saved.outputFilterMode.equals("DENY")&&saved.outputFilter.equals("minecraft:lava"),"Directional filters survive restart serialization");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void escrowBlocksRouteAndFilterEditsAndSyncsToClient(GameTestHelper h){
        HostEntity host=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.UP);Part p=part(host,Direction.UP);p.transferMode=2;p.pendingItem=new ItemStack(Items.DIAMOND,3);
        var user=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-01-Test"));var at=host.getBlockPos();user.setPos(at.getX(),at.getY()+1,at.getZ());
        for(var entry:Map.of("input_filter","minecraft:stone","output_filter_mode","DENY","transfer","1","input_channel","other","filter","minecraft:dirt","whitelist","false").entrySet())PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,entry.getKey(),entry.getValue()));
        h.assertTrue(p.inputFilter.isEmpty()&&p.outputFilterMode.equals("INHERIT")&&p.transferMode==2&&p.inputChannel.isEmpty()&&p.filter.isEmpty()&&p.whitelist,"Buffered resources protect route semantics from settings edits");
        Part client=Part.load(p.save(h.getLevel().registryAccess(),true),h.getLevel().registryAccess());
        h.assertTrue(client.pendingItem.isEmpty()&&!client.routeEditable(),"Client knows escrow exists without receiving inventory contents");
        p.pendingItem=ItemStack.EMPTY;PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"output_filter_mode","DENY"));
        h.assertTrue(p.outputFilterMode.equals("DENY"),"Drained route becomes editable");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void signallerStatementsHandleMissingAndAmbiguousReaders(GameTestHelper h){
        HostEntity a=host(h,new BlockPos(1,1,1),Kind.INFO_READER,Direction.UP),b=host(h,new BlockPos(3,1,1),Kind.INFO_READER,Direction.UP);
        Part first=part(a,Direction.UP),second=part(b,Direction.UP);first.label=second.label="same";first.rows.add(new Part.Row("value","Value",10,20,""));second.rows.add(new Part.Row("value","Value",5,20,""));
        var readers=List.of(ref(a,Direction.UP),ref(b,Direction.UP));Part signal=new Part(Kind.SIGNALLER,Direction.UP,OWNER);signal.signalStrength=7;
        signal.statements.add(new net.foundations.pl4.core.SignalRules.Statement(UUID.randomUUID(),first.identity.toString(),"value",">=",10));
        signal.statements.add(new net.foundations.pl4.core.SignalRules.Statement(UUID.randomUUID(),second.identity.toString(),"missing","!=",0));
        h.assertTrue(SignallerLogic.evaluate(signal,readers)==0,"Missing metric is false even for !=");signal.statementsAll=false;
        h.assertTrue(SignallerLogic.evaluate(signal,readers)==7,"ANY returns configured strength when a statement matches");
        signal.statements.clear();signal.selected="same";signal.threshold=1;
        h.assertTrue(SignallerLogic.evaluate(signal,readers)==0,"Duplicate reader labels cannot silently pick a different reader");
        signal.selected=first.identity.toString();h.assertTrue(SignallerLogic.evaluate(signal,readers)==7,"Stable reader UUID restores the legacy single condition");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void signallerStatementPacketsAreBoundedPersistentAndIdentitySafe(GameTestHelper h){
        HostEntity host=host(h,new BlockPos(2,1,1),Kind.SIGNALLER,Direction.UP);Part p=part(host,Direction.UP);
        var user=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-01-Rules"));var at=host.getBlockPos();user.setPos(at.getX(),at.getY()+1,at.getZ());
        for(String invalid:List.of("{}","null","[]","{\"threshold\":true}"))PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"statement_add",invalid));
        h.assertTrue(p.statements.isEmpty(),"Malformed statements are rejected without mutation");
        var json=new com.google.gson.JsonObject();json.addProperty("reader","");json.addProperty("key","value");json.addProperty("operator",">=");json.addProperty("threshold",2);
        for(int i=0;i<20;i++)PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"statement_add",json.toString()));
        h.assertTrue(p.statements.size()==16,"Statements are capped at sixteen");
        PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"statements_all","false"));PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"signal_strength","9"));
        Part saved=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(saved.statements.equals(p.statements)&&!saved.statementsAll&&saved.signalStrength==9,"Statement identities, conditions and mode survive save/reload");
        String id=p.statements.getFirst().id().toString();var remove=new PLPackets.Edit(at,p.slot(),p.identity,"statement_remove",id);PLPackets.edit(user,remove);PLPackets.edit(user,remove);
        h.assertTrue(p.statements.size()==15,"Repeated stale removal does not remove a different statement");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void providerApiRejectsWrongDimensionBeforeInvokingAddons(GameTestHelper h){
        int[] calls={0};
        try(var provider=net.foundations.pl4.api.InfoProviders.register("pl4_test:alpha_dimension",(context,out)->calls[0]++)){
            var wrong=new Part.Link("pl4_test:missing_dimension",h.absolutePos(new BlockPos(1,1,1)),Direction.UP,null,null);
            h.assertTrue(net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),wrong).isEmpty()&&calls[0]==0,"Wrong-dimension targets cannot sample the same coordinates in another world");
            var ids=net.foundations.pl4.api.InfoProviders.registeredIds();h.assertTrue(ids.contains("pl4_test:alpha_dimension")&&ids.contains("foundations_pl4:vanilla"),"API exposes builtin and registered provider identities");
            boolean immutable=false;try{ids.clear();}catch(UnsupportedOperationException expected){immutable=true;}h.assertTrue(immutable,"Diagnostic inventory is immutable");
        }
        h.assertTrue(!net.foundations.pl4.api.InfoProviders.registeredIds().contains("pl4_test:alpha_dimension"),"Closing provider removes its diagnostic entry");h.succeed();
    }

    private record StorageFixture(HostEntity host,Part node,ChestBlockEntity chest,net.minecraft.server.level.ServerPlayer player,ItemStack tool){}
    private static StorageFixture storageFixture(GameTestHelper h,String name){
        ChestBlockEntity chest=chest(h,new BlockPos(1,1,1));HostEntity host=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST);Part node=part(host,Direction.WEST);
        var user=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,name));user.getInventory().clearContent();
        ItemStack tool=new ItemStack(FoundationsPL4.item("wirelessstorage"));var binding=new Part.Link(h.getLevel().dimension().location().toString(),host.getBlockPos(),node.face,null,node.identity);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,tool,t->t.put("pl_link",binding.save()));
        user.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);return new StorageFixture(host,node,chest,user,tool);
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageWithdrawalCannotReplay(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-A");f.chest().setItem(0,new ItemStack(Items.DIAMOND,17));
        UUID token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);h.assertTrue(token!=null,"Owned loaded Node opens a storage session");
        var packet=new PLPackets.StorageRequest(token,"withdraw",0,1);WirelessStorage.request(f.player(),packet);WirelessStorage.request(f.player(),packet);
        int count=0;for(int slot=0;slot<36;slot++)if(f.player().getInventory().getItem(slot).is(Items.DIAMOND))count+=f.player().getInventory().getItem(slot).getCount();
        h.assertTrue(f.chest().getItem(0).getCount()==16&&count==1,"Single-use token withdraws exactly once");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageConcurrentSessionsConserveItems(GameTestHelper h){
        var first=storageFixture(h,"PL4-Storage-Concurrent-A");first.chest().setItem(0,new ItemStack(Items.DIAMOND,10));
        var second=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-Storage-Concurrent-B"));
        second.getInventory().clearContent();second.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,first.tool().copy());
        h.assertTrue(first.player()!=second,"Concurrent sessions need distinct server players");
        UUID a=WirelessStorage.open(first.player(),net.minecraft.world.InteractionHand.MAIN_HAND);
        UUID b=WirelessStorage.open(second,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(a!=null&&b!=null,"Both owners can open an independent server session");
        WirelessStorage.request(first.player(),new PLPackets.StorageRequest(a,"withdraw",0,8));
        WirelessStorage.request(second,new PLPackets.StorageRequest(b,"withdraw",0,8));
        int owned=0;for(var player:List.of(first.player(),second))for(int slot=0;slot<36;slot++){
            ItemStack stack=player.getInventory().getItem(slot);if(stack.is(Items.DIAMOND))owned+=stack.getCount();
        }
        h.assertTrue(owned==10&&first.chest().getItem(0).isEmpty(),"Two stale snapshots withdraw only available items without loss or duplication");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageRechecksSlotsFiltersAndHeldTool(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-B");f.chest().setItem(0,new ItemStack(Items.DIAMOND,17));
        UUID token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.chest().setItem(0,new ItemStack(Items.EMERALD,9));
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));h.assertTrue(f.chest().getItem(0).getCount()==9,"Stale slot does not withdraw a different resource");
        token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.node().outputFilterMode="DENY";f.node().outputFilter="minecraft:emerald";
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));h.assertTrue(f.chest().getItem(0).getCount()==9,"Current send filter is rechecked at action time");
        token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.player().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));h.assertTrue(f.chest().getItem(0).getCount()==9,"Removing the held tool revokes the session");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageDepositConservesOffhandAndHonorsFilters(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-C");f.player().setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.DIAMOND,11));f.node().inputFilterMode="DENY";f.node().inputFilter="minecraft:diamond";
        UUID token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"deposit",0,0));
        h.assertTrue(f.player().getOffhandItem().getCount()==11&&f.chest().getItem(0).isEmpty(),"Receive filter rejects deposit without consuming offhand items");
        f.node().inputFilter="";token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"deposit",0,0));
        h.assertTrue(f.player().getOffhandItem().isEmpty()&&f.chest().getItem(0).getCount()==11,"Deposit moves exactly the accepted offhand stack");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageRechecksOwnerAndPartIdentity(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-D");f.chest().setItem(0,new ItemStack(Items.DIAMOND,17));
        UUID token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.node().owner=UUID.randomUUID();
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));h.assertTrue(f.chest().getItem(0).getCount()==17,"Owner changes invalidate storage access");
        f.node().owner=OWNER;token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.node().identity=UUID.randomUUID();
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));h.assertTrue(f.chest().getItem(0).getCount()==17,"Replacing a Node cannot inherit another binding's authority");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageCapabilityReadCannotKeepRevokedRoot(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-LiveRoot");
        var binding=ToolItem.link(f.tool());var ref=new NetworkEngine.Ref(f.host(),f.node());
        h.assertTrue(WirelessStorage.authorizedTarget(f.player(),binding,ref),"Original bound root is authorized");
        f.node().owner=UUID.randomUUID();
        h.assertTrue(!WirelessStorage.authorizedTarget(f.player(),binding,ref),"A provider read cannot preserve a revoked controller owner");
        f.node().owner=OWNER;
        f.chest().setItem(0,new ItemStack(Items.DIAMOND,9));
        ItemStack extracted=ItemTransferSafety.extract(ref,new ItemStack(Items.DIAMOND,4),4,()->{
            f.node().owner=UUID.randomUUID();
            return f.chest().removeItem(0,4);
        });
        boolean sealed=false;
        try{WirelessStorage.requirePostExtractionAuthority(f.player(),binding,ref,ref.adjacent());}
        catch(IllegalStateException expected){sealed=true;}
        h.assertTrue(sealed&&extracted.getCount()==4&&f.node().pendingItem.getCount()==4
            &&ItemTransferSafety.blocked(f.node())&&f.chest().getItem(0).getCount()==5,
            "Revocation after actual extraction seals the captured stack without recrediting or losing it");
        f.node().owner=OWNER;f.host().parts.remove(f.node().slot());
        h.assertTrue(!WirelessStorage.authorizedTarget(f.player(),binding,ref),"Removed controller part cannot be authorized by a stale reference");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageRevokesReplacedBackingDuringExtraction(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-BackingSwap");
        var binding=ToolItem.link(f.tool());
        var ref=new NetworkEngine.Ref(f.host(),f.node());
        BlockPos location=new BlockPos(1,1,1);
        f.chest().setItem(0,new ItemStack(Items.DIAMOND,5));
        TransferEngine.ItemTarget backing=TransferEngine.itemTarget(h.getLevel().getServer(),ref);
        h.assertTrue(backing!=null&&WirelessStorage.liveBackingTarget(f.player(),binding,ref,backing),
            "Fresh wireless capability belongs to its originally loaded backing inventory");
        ItemStack removed=ItemTransferSafety.extract(ref,new ItemStack(Items.DIAMOND,5),5,()->{
            ItemStack actual=f.chest().removeItem(0,5);
            h.setBlock(location,Blocks.AIR);h.setBlock(location,Blocks.CHEST);
            return actual;
        });
        boolean sealed=false;
        try{WirelessStorage.requirePostExtractionBackingAuthority(f.player(),binding,ref,backing);}
        catch(IllegalStateException expected){sealed=true;}
        var replacement=h.getBlockEntity(location);
        h.assertTrue(sealed&&replacement!=f.chest()&&removed.getCount()==5
            &&f.node().pendingItem.getCount()==5&&ItemTransferSafety.blocked(f.node())
            &&WirelessStorage.authorizedTarget(f.player(),binding,ref)
            &&!WirelessStorage.liveBackingTarget(f.player(),binding,ref,backing),
            "Wireless source with same owner and position must seal extracted escrow after backing swap");
        var newBacking=TransferEngine.itemTarget(h.getLevel().getServer(),ref);
        h.assertTrue(newBacking!=null&&newBacking.entity()==replacement
            &&WirelessStorage.liveBackingTarget(f.player(),binding,ref,newBacking),
            "Replacement inventory needs new capability discovery; previous extraction remains quarantined");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageCapabilityReadCannotKeepRevokedEndpoint(GameTestHelper h){
        var f=networkStorage(h,"PL4-Storage-LiveEndpoint");
        var binding=ToolItem.link(f.first().tool());var ref=new NetworkEngine.Ref(f.secondHost(),f.second());
        h.assertTrue(WirelessStorage.authorizedTarget(f.first().player(),binding,ref),"Initially owned loaded endpoint is authorized");
        f.second().owner=UUID.randomUUID();
        h.assertTrue(!WirelessStorage.authorizedTarget(f.first().player(),binding,ref),"A foreign-owned endpoint cannot survive capability-read authorization");
        f.second().owner=OWNER;f.secondHost().parts.remove(f.second().slot());
        h.assertTrue(!WirelessStorage.authorizedTarget(f.first().player(),binding,ref),"Replaced endpoint cannot inherit stale capability permission");
        h.succeed();
    }
    private record NetworkStorageFixture(StorageFixture first,HostEntity secondHost,Part second,ChestBlockEntity secondChest){}
    private static NetworkStorageFixture networkStorage(GameTestHelper h,String name){
        var first=storageFixture(h,name);var secondChest=chest(h,new BlockPos(4,1,1));var secondHost=host(h,new BlockPos(3,1,1),Kind.NODE,Direction.EAST);
        NetworkEngine.rebuild(h.getLevel().getServer());return new NetworkStorageFixture(first,secondHost,part(secondHost,Direction.EAST),secondChest);
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStorageAggregatesAndWithdrawsAcrossNodes(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-A");f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,17));f.secondChest().setItem(0,new ItemStack(Items.DIAMOND,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(WirelessStorage.visibleCounts(f.first().player()).equals(List.of(40L)),"Matching variants combine across connected inventories");
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"withdraw",0,64));
        int count=0;for(int slot=0;slot<36;slot++)if(f.first().player().getInventory().getItem(slot).is(Items.DIAMOND))count+=f.first().player().getInventory().getItem(slot).getCount();
        h.assertTrue(count==40&&f.first().chest().getItem(0).isEmpty()&&f.secondChest().getItem(0).isEmpty(),"Withdrawal conserves all 40 items across sources");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStorageRevokesNewlyForeignEndpoint(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-B");f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,5));f.secondChest().setItem(0,new ItemStack(Items.DIAMOND,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.second().owner=UUID.randomUUID();
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"withdraw",0,64));
        h.assertTrue(f.first().chest().getItem(0).isEmpty()&&f.secondChest().getItem(0).getCount()==23,"Every remote Node ownership is rechecked, not just the anchor");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void physicalCableRejectsForeignOwnerAfterLiveTransfer(GameTestHelper h){
        var server=h.getLevel().getServer();
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(4,1,1));
        from.setItem(0,new ItemStack(Items.DIAMOND,32));
        HostEntity left=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST);
        HostEntity right=host(h,new BlockPos(3,1,1),Kind.TRANSFER_NODE,Direction.EAST);
        Part source=part(left,Direction.WEST),sink=part(right,Direction.EAST);
        source.transferMode=2;sink.transferMode=1;left.changed();right.changed();
        var connected=NetworkEngine.storageNodes(server,source);
        h.assertTrue(connected.size()==2,"Matching-owner physical endpoints must connect");
        TransferEngine.run(server,connected);
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==32,
            "Native cable must deliver when both endpoint owners match");
        Part detachedCable=right.parts.remove(6);
        var detached=NetworkEngine.storageNodes(server,source);
        h.assertTrue(detached.size()==1&&detached.getFirst().part()==source,
            "Detached intermediate cable must exclude remote aliases while retaining the local source");
        right.parts.put(6,detachedCable);
        NetworkEngine.rebuild(server);
        connected=NetworkEngine.storageNodes(server,source);
        h.assertTrue(connected.size()==2,
            "Restored loaded cable must rejoin both owned endpoints after rebuild");
        from.setItem(0,new ItemStack(Items.DIAMOND,12));
        // Reuse the actual previously authorized native transfer plan before any
        // dirty-rebuild notification. Its cached endpoints must not authorize
        // newly foreign inventory even if the rest of the graph is unchanged.
        var stalePlan=TransferEngine.prepare(connected);
        UUID original=sink.owner;sink.owner=UUID.randomUUID();
        TransferEngine.run(server,stalePlan);
        h.assertTrue(from.getItem(0).getCount()==12&&to.getItem(0).getCount()==32
            &&source.pendingItem.isEmpty()&&sink.pendingItem.isEmpty(),
            "Stale native transfer plan must reject a newly foreign endpoint before topology rebuild");
        right.changed();
        var isolated=NetworkEngine.storageNodes(server,source);
        h.assertTrue(isolated.size()==1&&isolated.getFirst().part()==source,
            "Foreign-owned endpoint must be removed from the cached physical cable graph");
        TransferEngine.run(server,isolated);
        h.assertTrue(from.getItem(0).getCount()==12&&to.getItem(0).getCount()==32
            &&source.pendingItem.isEmpty(),"Revoking a live cable endpoint must prevent transfer or escrow");
        sink.owner=original;right.changed();
        var rejoined=NetworkEngine.storageNodes(server,source);
        h.assertTrue(rejoined.size()==2,"Restoring owner reopens only the matching cable graph");
        TransferEngine.run(server,rejoined);
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==44,
            "Reauthorized cable resumes without losing or minting items");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStorageDropsDisconnectedSources(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-C");f.secondChest().setItem(0,new ItemStack(Items.DIAMOND,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);
        f.first().host().parts.get(6).blockedFaces=1<<Direction.EAST.ordinal();f.secondHost().parts.get(6).blockedFaces=1<<Direction.WEST.ordinal();f.first().host().changed();f.secondHost().changed();
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"withdraw",0,64));
        h.assertTrue(f.secondChest().getItem(0).getCount()==23,"Stale session cannot reach an endpoint after cable disconnection");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStorageSearchAndCountSort(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-D");f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,5));f.secondChest().setItem(0,new ItemStack(Items.EMERALD,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"search",0,0,"","COUNT"));
        h.assertTrue(WirelessStorage.visibleCounts(f.first().player()).equals(List.of(23L,5L)),"Count sort orders combined counts descending");
        token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"search",0,0,"minecraft:emerald","NAME"));
        h.assertTrue(WirelessStorage.visibleCounts(f.first().player()).equals(List.of(23L)),"Search matches item registry identifiers");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStorageDepositsPastFullInventory(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-E");for(int i=0;i<27;i++)f.first().chest().setItem(i,new ItemStack(Items.STONE,64));
        f.first().player().setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.DIAMOND,11));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"deposit",0,0));
        h.assertTrue(f.first().player().getOffhandItem().isEmpty()&&f.secondChest().getItem(0).getCount()==11,"Deposit finds space on a connected Node and conserves items");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStoragePreservesComponentVariants(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-F");ItemStack named=new ItemStack(Items.DIAMOND,7);named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Named diamond"));
        f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,5));f.secondChest().setItem(0,named);
        WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(WirelessStorage.visibleCounts(f.first().player()).size()==2,"Distinct item components never collapse into one withdrawable variant");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void componentLinksValidateTypesAndDeduplicate(GameTestHelper h){
        var f=storageFixture(h,"PL4-Links-A");Part array=new Part(Kind.ARRAY,Direction.NORTH,OWNER);f.host().parts.put(array.slot(),array);
        Part entity=new Part(Kind.ENTITY_NODE,Direction.SOUTH,OWNER);f.host().parts.put(entity.slot(),entity);
        var block=new Part.Link(h.getLevel().dimension().location().toString(),f.chest().getBlockPos(),Direction.UP,null,null);
        h.assertTrue(ComponentLinks.add(f.player(),f.host(),array,block),"Array accepts a permitted loaded block link");
        h.assertTrue(!ComponentLinks.add(f.player(),f.host(),array,block)&&array.links.size()==1,"Duplicate links do not consume another Array slot");
        h.assertTrue(!ComponentLinks.add(f.player(),f.host(),entity,block),"Entity Node rejects block links");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void componentLinksRejectForeignEmitters(GameTestHelper h){
        var f=storageFixture(h,"PL4-Links-B");Part receiver=new Part(Kind.DATA_RECEIVER,Direction.NORTH,OWNER);f.host().parts.put(receiver.slot(),receiver);
        var remote=host(h,new BlockPos(4,1,1),Kind.DATA_EMITTER,Direction.UP);Part emitter=part(remote,Direction.UP);
        var link=new Part.Link(h.getLevel().dimension().location().toString(),remote.getBlockPos(),emitter.face,null,emitter.identity);
        emitter.owner=UUID.randomUUID();h.assertTrue(!ComponentLinks.add(f.player(),f.host(),receiver,link),"Foreign emitter never grants a wireless network edge");
        emitter.owner=OWNER;h.assertTrue(ComponentLinks.add(f.player(),f.host(),receiver,link),"Owned matching emitter can be selected");h.succeed();
    }


    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void itemProviderCallbackRevocationRetainsEscrow(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));
        from.setItem(0,new ItemStack(Items.DIAMOND,13));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),
            add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);
        part(remove,Direction.WEST).transferMode=2;part(add,Direction.EAST).transferMode=1;
        NetworkEngine.Ref source=ref(remove,Direction.WEST),sink=ref(add,Direction.EAST);
        var server=h.getLevel().getServer();
        h.assertTrue(TransferEngine.liveItemRoute(server,source,sink),"Loaded owned endpoints must be authorized");
        boolean extractionRevoked=false;
        try{
            ItemTransferSafety.extract(sink,new ItemStack(Items.DIAMOND,7),7,
                ()->TransferEngine.liveItemRoute(server,source,sink),()->{
                    source.part().owner=UUID.randomUUID();
                    return from.removeItem(0,7);
                });
        }catch(IllegalStateException expected){extractionRevoked=true;}
        h.assertTrue(extractionRevoked&&ItemTransferSafety.blocked(sink.part())
            &&sink.part().pendingItem.getCount()==7&&from.getItem(0).getCount()==6&&to.getItem(0).isEmpty(),
            "Actual provider extraction followed by owner revocation must retain uncertain items and quarantine");
        int[] callbacks={0};
        ItemStack denied=ItemTransferSafety.insert(sink,sink.part().pendingItem.copy(),
            ()->TransferEngine.liveItemRoute(server,source,sink),()->{callbacks[0]++;return ItemStack.EMPTY;});
        h.assertTrue(denied.getCount()==7&&callbacks[0]==0&&sink.part().pendingItem.getCount()==7,
            "Revoked route must not invoke another provider or spend retained escrow");
        source.part().owner=OWNER;sink.part().itemTransferFault="";
        boolean insertionRevoked=false;
        try{
            ItemTransferSafety.insert(sink,sink.part().pendingItem.copy(),
                ()->TransferEngine.liveItemRoute(server,source,sink),()->{
                    to.setItem(0,new ItemStack(Items.DIAMOND,7));
                    source.part().owner=UUID.randomUUID();
                    return ItemStack.EMPTY;
                });
        }catch(IllegalStateException expected){insertionRevoked=true;}
        h.assertTrue(insertionRevoked&&ItemTransferSafety.blocked(sink.part())
            &&sink.part().pendingItem.getCount()==7&&to.getItem(0).getCount()==7,
            "Revocation after actual insertion keeps escrow uncertain rather than automatically settling it");
        source.part().owner=OWNER;
        Part attached=remove.parts.remove(source.part().slot());
        h.assertTrue(!TransferEngine.liveItemRoute(server,source,sink),"Detached cached source cannot authorize mutation");
        remove.parts.put(attached.slot(),attached);
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void replacedCapabilityBackingCannotReuseCachedItemHandler(GameTestHelper h){
        var server=h.getLevel().getServer();
        BlockPos input=new BlockPos(1,1,1),output=new BlockPos(5,1,1);
        ChestBlockEntity oldSource=chest(h,input),destination=chest(h,output);
        oldSource.setItem(0,new ItemStack(Items.DIAMOND,7));
        HostEntity sourceHost=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST);
        HostEntity sinkHost=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);
        part(sourceHost,Direction.WEST).transferMode=2;
        part(sinkHost,Direction.EAST).transferMode=1;
        var source=ref(sourceHost,Direction.WEST);
        var sink=ref(sinkHost,Direction.EAST);
        TransferEngine.ItemTarget cached=TransferEngine.itemTarget(server,source);
        h.assertTrue(cached!=null&&cached.live(server,source)
            &&TransferEngine.liveItemRoute(server,source,sink),"Initially loaded capability is bound to its original block entity");
        int[] operations={0};
        boolean revoked=false;
        try{
            ItemTransferSafety.extract(sink,new ItemStack(Items.DIAMOND,7),7,
                ()->cached.live(server,source)&&TransferEngine.liveItemRoute(server,source,sink),()->{
                    operations[0]++;
                    ItemStack taken=oldSource.removeItem(0,7);
                    h.setBlock(input,Blocks.AIR);
                    h.setBlock(input,Blocks.CHEST);
                    return taken;
                });
        }catch(IllegalStateException expected){revoked=true;}
        ChestBlockEntity newSource=(ChestBlockEntity)h.getBlockEntity(input);
        h.assertTrue(revoked&&operations[0]==1&&newSource!=oldSource&&!cached.live(server,source)
            &&newSource.getItem(0).isEmpty()&&destination.getItem(0).isEmpty()
            &&sink.part().pendingItem.getCount()==7&&ItemTransferSafety.blocked(sink.part()),
            "Replacing backing inventory after real extraction must retain escrow and revoke stale capability");
        ItemStack denied=ItemTransferSafety.insert(sink,sink.part().pendingItem.copy(),
            ()->cached.live(server,source),()->{operations[0]++;return ItemStack.EMPTY;});
        h.assertTrue(denied.getCount()==7&&operations[0]==1,
            "Stale provider alias must not perform any second mutation");
        TransferEngine.ItemTarget refreshed=TransferEngine.itemTarget(server,source);
        h.assertTrue(refreshed!=null&&refreshed.live(server,source)&&refreshed.entity()==newSource,
            "Replaced inventory requires a newly acquired handler, never a stale alias");
        h.succeed();
    }

    /** Foreign fluid callbacks must not automatically settle escrow if ownership changed mid-call. */
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void fluidCallbackOwnerRevocationQuarantinesEscrow(GameTestHelper h){
        chest(h,new BlockPos(1,1,1));chest(h,new BlockPos(5,1,1));
        HostEntity sourceHost=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),
            sinkHost=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);
        part(sourceHost,Direction.WEST).transferMode=2;
        part(sinkHost,Direction.EAST).transferMode=1;
        var source=ref(sourceHost,Direction.WEST);var sink=ref(sinkHost,Direction.EAST);
        var server=h.getLevel().getServer();
        var water=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,500);
        h.assertTrue(TransferEngine.liveItemRoute(server,source,sink),"Fresh fluid route must be live");
        boolean revoked=false;
        try{
            ResourceTransferSafety.extractFluid(sink,water,
                ()->TransferEngine.liveItemRoute(server,source,sink),()->{
                    source.part().owner=UUID.randomUUID();return water.copy();
                });
        }catch(IllegalStateException expected){revoked=true;}
        h.assertTrue(revoked&&ResourceTransferSafety.blocked(sink.part())&&sink.part().pendingFluid.getAmount()==500,
            "Fluid extraction after owner revocation must retain the uncertain 500 mB in escrow");
        int[] callbacks={0};
        int denied=ResourceTransferSafety.insertFluid(sink,water,
            ()->TransferEngine.liveItemRoute(server,source,sink),()->{callbacks[0]++;return 500;});
        h.assertTrue(denied==0&&callbacks[0]==0&&sink.part().pendingFluid.getAmount()==500,
            "Revoked route must not invoke another fluid provider callback");
        source.part().owner=OWNER;sink.part().resourceTransferFault="";
        boolean insertionRevoked=false;
        try{
            ResourceTransferSafety.insertFluid(sink,water,
                ()->TransferEngine.liveItemRoute(server,source,sink),()->{
                    source.part().owner=UUID.randomUUID();return 500;
                });
        }catch(IllegalStateException expected){insertionRevoked=true;}
        h.assertTrue(insertionRevoked&&ResourceTransferSafety.blocked(sink.part())&&sink.part().pendingFluid.getAmount()==500,
            "Fluid insertion owner revocation must preserve escrow for manual resolution");
        h.succeed();
    }

    /** FE callbacks obey the same live-route fence as item and fluid callbacks. */
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void energyCallbackOwnerRevocationQuarantinesEscrow(GameTestHelper h){
        chest(h,new BlockPos(1,1,1));chest(h,new BlockPos(5,1,1));
        HostEntity sourceHost=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),
            sinkHost=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);
        part(sourceHost,Direction.WEST).transferMode=2;
        part(sinkHost,Direction.EAST).transferMode=1;
        var source=ref(sourceHost,Direction.WEST);var sink=ref(sinkHost,Direction.EAST);
        var server=h.getLevel().getServer();
        h.assertTrue(TransferEngine.liveItemRoute(server,source,sink),"Fresh FE route must be live");
        boolean revoked=false;
        try{
            ResourceTransferSafety.extractEnergy(sink,300,
                ()->TransferEngine.liveItemRoute(server,source,sink),()->{
                    source.part().owner=UUID.randomUUID();return 200L;
                });
        }catch(IllegalStateException expected){revoked=true;}
        h.assertTrue(revoked&&ResourceTransferSafety.blocked(sink.part())&&sink.part().pendingEnergy==200,
            "Revoked FE extraction must retain 200 FE as uncertain escrow");
        int[] callbacks={0};
        int denied=ResourceTransferSafety.insertEnergy(sink,200,
            ()->TransferEngine.liveItemRoute(server,source,sink),()->{callbacks[0]++;return 200L;});
        h.assertTrue(denied==0&&callbacks[0]==0&&sink.part().pendingEnergy==200,
            "Revoked FE route must not call a second provider");
        source.part().owner=OWNER;sink.part().resourceTransferFault="";
        boolean insertionRevoked=false;
        try{
            ResourceTransferSafety.insertEnergy(sink,200,
                ()->TransferEngine.liveItemRoute(server,source,sink),()->{
                    source.part().owner=UUID.randomUUID();return 200L;
                });
        }catch(IllegalStateException expected){insertionRevoked=true;}
        h.assertTrue(insertionRevoked&&ResourceTransferSafety.blocked(sink.part())&&sink.part().pendingEnergy==200,
            "FE acceptance after owner revocation must leave escrow quarantined");
        h.succeed();
    }

    /** A replaced block entity cannot reuse even the same-position resource backing identity. */
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void resourceBackingRejectsSamePositionReplacement(GameTestHelper h){
        var server=h.getLevel().getServer();
        BlockPos backingPos=new BlockPos(1,1,1);
        ChestBlockEntity oldChest=chest(h,backingPos);
        HostEntity host=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST);
        var endpoint=ref(host,Direction.WEST);
        TransferEngine.ResourceBacking initial=TransferEngine.resourceBacking(server,endpoint);
        h.assertTrue(initial!=null&&initial.live(server,endpoint),"Attached loaded resource backing initially valid");
        h.setBlock(backingPos,Blocks.AIR);h.setBlock(backingPos,Blocks.CHEST);
        ChestBlockEntity replacement=(ChestBlockEntity)h.getBlockEntity(backingPos);
        h.assertTrue(replacement!=oldChest&&!initial.live(server,endpoint),
            "Replacing a block entity at identical coordinates must invalidate cached capability backing");
        TransferEngine.ResourceBacking refreshed=TransferEngine.resourceBacking(server,endpoint);
        h.assertTrue(refreshed!=null&&refreshed.live(server,endpoint),
            "Newly captured resource backing must recover without reauthorizing the stale handle");
        h.succeed();
    }

    private static void itemQuarantineChecks(NetworkEngine.Ref owner){
        Part p=owner.part();p.pendingItem=new ItemStack(Items.DIAMOND,8);int[] calls={0};
        boolean failed=false;
        try{ItemTransferSafety.insert(owner,p.pendingItem.copy(),()->{calls[0]++;throw new IllegalStateException("provider after mutation");});}catch(IllegalStateException expected){failed=true;}
        if(!failed||!ItemTransferSafety.blocked(p)||calls[0]!=1)throw new IllegalStateException("Uncertain insertion must persist quarantine");
        try{ItemTransferSafety.insert(owner,p.pendingItem.copy(),()->{calls[0]++;return ItemStack.EMPTY;});throw new AssertionError("Quarantine was replayed");}catch(IllegalStateException expected){}
        if(calls[0]!=1)throw new IllegalStateException("Quarantined provider must not be called again");
        p.itemTransferFault="";p.pendingItem=ItemStack.EMPTY;
        try{ItemTransferSafety.extract(owner,new ItemStack(Items.DIAMOND,8),8,()->new ItemStack(Items.EMERALD,8));throw new AssertionError("Wrong variant accepted");}catch(IllegalStateException expected){}
        if(!ItemTransferSafety.blocked(p)||p.pendingItem.getItem()!=Items.EMERALD||p.pendingItem.getCount()!=8)throw new IllegalStateException("Unexpected extraction retained without forwarding");
        p.itemTransferFault="";p.pendingItem=new ItemStack(Items.DIAMOND,8);
        try{ItemTransferSafety.insert(owner,p.pendingItem.copy(),()->new ItemStack(Items.EMERALD,1));throw new AssertionError("Malformed remainder accepted");}catch(IllegalStateException expected){}
        if(!ItemTransferSafety.blocked(p))throw new IllegalStateException("Invalid actual remainder must stay quarantined");
        p.itemTransferFault="";
        ItemStack refused=ItemTransferSafety.insert(owner,p.pendingItem.copy(),()->p.pendingItem.copy());
        if(refused.getCount()!=8)throw new IllegalStateException("Healthy refusal loses escrow");ItemTransferSafety.completed(owner);
        if(ItemTransferSafety.blocked(p)||p.pendingItem.getCount()!=8)throw new IllegalStateException("Healthy refusal must allow a later retry");
        p.pendingItem=ItemStack.EMPTY;p.itemEscrowCount=0;
    }

    private static void resourceQuarantineChecks(NetworkEngine.Ref owner){
        Part p=owner.part();p.pendingFluid=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,8);int[] calls={0};
        try{ResourceTransferSafety.insertFluid(owner,p.pendingFluid.copy(),()->{calls[0]++;throw new IllegalStateException("fluid provider mutated");});throw new AssertionError("Missing fluid failure");}catch(IllegalStateException expected){}
        if(!ResourceTransferSafety.blocked(p)||p.routeEditable())throw new IllegalStateException("Uncertain fluid insertion must remain fenced");
        try{ResourceTransferSafety.insertFluid(owner,p.pendingFluid.copy(),()->{calls[0]++;return 8;});throw new AssertionError("Replayed fluid");}catch(IllegalStateException expected){}
        if(calls[0]!=1)throw new IllegalStateException("Quarantined fluid callback replayed");
        p.resourceTransferFault="";int refused=ResourceTransferSafety.insertFluid(owner,p.pendingFluid.copy(),()->0);ResourceTransferSafety.completed(owner);
        if(refused!=0||ResourceTransferSafety.blocked(p)||p.pendingFluid.getAmount()!=8)throw new IllegalStateException("Healthy fluid refusal must be retryable");
        try{ResourceTransferSafety.extractFluid(owner,p.pendingFluid.copy(),()->new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,8));throw new AssertionError("Wrong fluid accepted");}catch(IllegalStateException expected){}
        if(!ResourceTransferSafety.blocked(p)||p.pendingFluid.getFluid()!=net.minecraft.world.level.material.Fluids.LAVA)throw new IllegalStateException("Unexpected fluid variant must be retained but not forwarded");
        p.resourceTransferFault="";
        try{ResourceTransferSafety.insertFluid(owner,p.pendingFluid.copy(),()->9);throw new AssertionError("Over-reported fluid insertion accepted");}catch(IllegalStateException expected){}
        if(!ResourceTransferSafety.blocked(p))throw new IllegalStateException("Invalid fluid insertion stays quarantined");
        p.resourceTransferFault="";p.pendingFluid=net.neoforged.neoforge.fluids.FluidStack.EMPTY;p.pendingEnergy=8;
        try{ResourceTransferSafety.insertEnergy(owner,8,()->{calls[0]++;throw new IllegalStateException("FE provider mutated");});throw new AssertionError("Missing FE failure");}catch(IllegalStateException expected){}
        try{ResourceTransferSafety.insertEnergy(owner,8,()->{calls[0]++;return 8;});throw new AssertionError("Replayed FE");}catch(IllegalStateException expected){}
        if(calls[0]!=2||!ResourceTransferSafety.blocked(p))throw new IllegalStateException("Uncertain FE must not replay");
        p.resourceTransferFault="";
        try{ResourceTransferSafety.extractEnergy(owner,8,()->9);throw new AssertionError("Over-extraction accepted");}catch(IllegalStateException expected){}
        if(!ResourceTransferSafety.blocked(p)||p.pendingEnergy!=9)throw new IllegalStateException("Over-extracted FE remains quarantined");
        p.resourceTransferFault="";p.pendingEnergy=0;
        if(ResourceTransferSafety.insertEnergy(owner,8,()->0)!=0)throw new IllegalStateException("Refusal changed amount");ResourceTransferSafety.completed(owner);
        if(ResourceTransferSafety.blocked(p))throw new IllegalStateException("Healthy FE refusal must be retryable");
    }

    private static void wirelessHandoffQuarantine(NetworkEngine.Ref owner){
        Part p=owner.part();ItemStack hand=new ItemStack(Items.DIAMOND,19);int[] actual={0};
        try{WirelessItemTransfers.deposit(owner,hand,new ItemStack(Items.DIAMOND,7),sent->{actual[0]+=sent.getCount();throw new IllegalStateException("Destination mutated before failure");});throw new AssertionError("Missing deposit failure");}catch(IllegalStateException expected){}
        if(hand.getCount()!=12||actual[0]!=7||p.pendingItem.getCount()!=7||!ItemTransferSafety.blocked(p))throw new IllegalStateException("Uncertain deposit must remove spendable offhand items and quarantine the node");
        try{WirelessItemTransfers.deposit(owner,hand,new ItemStack(Items.DIAMOND,7),sent->{actual[0]+=sent.getCount();return ItemStack.EMPTY;});throw new AssertionError("Replayed wireless deposit");}catch(IllegalStateException expected){}
        if(actual[0]!=7)throw new IllegalStateException("Quarantined wireless deposit cannot replay");
        p.itemTransferFault="";p.pendingItem=ItemStack.EMPTY;p.itemEscrowCount=0;
        int refused=WirelessItemTransfers.deposit(owner,hand,new ItemStack(Items.DIAMOND,7),sent->sent);
        if(refused!=0||hand.getCount()!=12||!p.pendingItem.isEmpty()||ItemTransferSafety.blocked(p))throw new IllegalStateException("Healthy wireless refusal must return the staged hand items");
        ItemStack fullHand=new ItemStack(Items.DIAMOND,7);
        if(WirelessItemTransfers.deposit(owner,fullHand,fullHand.copy(),sent->sent)!=0||fullHand.getCount()!=7||fullHand.getItem()!=Items.DIAMOND)throw new IllegalStateException("Complete offhand staging must restore a healthy refusal");
        int moved=WirelessItemTransfers.deposit(owner,hand,new ItemStack(Items.DIAMOND,7),sent->ItemStack.EMPTY);
        if(moved!=7||hand.getCount()!=5||!p.pendingItem.isEmpty())throw new IllegalStateException("Valid wireless deposit settles exactly once");
        p.pendingItem=new ItemStack(Items.DIAMOND,8);
        try{WirelessItemTransfers.deliver(owner,sent->{actual[0]+=sent.getCount();throw new IllegalStateException("Player inventory mutated before failure");});throw new AssertionError("Missing delivery failure");}catch(IllegalStateException expected){}
        if(!ItemTransferSafety.blocked(p)||p.pendingItem.getCount()!=8||actual[0]!=15)throw new IllegalStateException("Uncertain player delivery must remain fenced");
        p.itemTransferFault="";p.pendingItem=ItemStack.EMPTY;p.itemEscrowCount=0;
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void installedProviderExceptionDiscardsPartialRows(GameTestHelper h){
        BlockPos position=new BlockPos(2,1,1);
        chest(h,position);
        var target=new Part.Link(h.getLevel().dimension().location().toString(),position,Direction.UP,null,null);
        int[] laterCalls={0};
        try(var failing=net.foundations.pl4.api.InfoProviders.register("pl4_test:exception_guard",(context,out)->{
            out.add("partial","Partial sample",1,1,"unit");
            throw new IllegalStateException("intentional provider failure");
        });var healthy=net.foundations.pl4.api.InfoProviders.register("pl4_test:exception_followup",(context,out)->{
            laterCalls[0]++;
            out.add("healthy","Healthy sample",2,2,"unit");
        })){
            var rows=net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),target);
            h.assertTrue(rows.stream().noneMatch(r->r.key().equals("pl4_test:exception_guard/partial")),
                "Provider rows collected before an exception must be discarded");
            h.assertTrue(rows.stream().anyMatch(r->r.key().equals("pl4_test:exception_followup/healthy"))&&laterCalls[0]==1,
                "A failing provider must not prevent later healthy providers from sampling");
        }
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void installedProviderSampleIsDiscardedAfterEntityUnload(GameTestHelper h){
        var entity=h.spawn(net.minecraft.world.entity.EntityType.ARMOR_STAND,new BlockPos(2,1,1));
        var target=new Part.Link(h.getLevel().dimension().location().toString(),entity.blockPosition(),Direction.UP,entity.getUUID(),null);
        try(var provider=net.foundations.pl4.api.InfoProviders.register("pl4_test:unload_guard",(context,out)->{
            context.entity().remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            out.add("late","Late sample",1,1,"unit");
        })){
            var rows=net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),target);
            h.assertTrue(rows.stream().noneMatch(r->r.key().equals("pl4_test:unload_guard/late")),
                "Provider rows collected after entity unload must not be published");
        }
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void providerApiRejectsUnloadedTargetWithoutForceLoading(GameTestHelper h){
        int[] calls={0};
        BlockPos far=new BlockPos(1024,1,1024);
        var target=new Part.Link(h.getLevel().dimension().location().toString(),far,Direction.UP,null,null);
        h.assertTrue(!h.getLevel().hasChunkAt(far),"Fixture target must begin outside the loaded test chunks");
        try(var provider=net.foundations.pl4.api.InfoProviders.register("pl4_test:no_force_load",(context,out)->{
            calls[0]++;
            out.add("unexpected","Unexpected sample",1,1,"unit");
        })){
            var rows=net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),target);
            h.assertTrue(rows.isEmpty()&&calls[0]==0,
                "Unloaded targets return no rows without invoking providers or force-loading chunks");
        }
        h.assertTrue(!h.getLevel().hasChunkAt(far),"Sampling must not force-load the target chunk");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void installedProviderSampleIsDiscardedAfterBlockEntityReplacement(GameTestHelper h){
        BlockPos position=new BlockPos(2,1,1);
        h.setBlock(position,Blocks.CHEST);
        var target=new Part.Link(h.getLevel().dimension().location().toString(),position,Direction.UP,null,null);
        try(var provider=net.foundations.pl4.api.InfoProviders.register("pl4_test:block_replacement_guard",(context,out)->{
            h.setBlock(position,Blocks.AIR);
            h.setBlock(position,Blocks.CHEST);
            out.add("late","Late block sample",1,1,"unit");
        })){
            var rows=net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),target);
            h.assertTrue(rows.stream().noneMatch(r->r.key().equals("pl4_test:block_replacement_guard/late")),
                "Provider rows collected after same-position block-entity replacement must not be published");
        }
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void installedProviderSampleIsDiscardedAfterEntityReplacement(GameTestHelper h){
        BlockPos position=new BlockPos(2,1,1);
        var original=h.spawn(net.minecraft.world.entity.EntityType.ARMOR_STAND,position);
        var target=new Part.Link(h.getLevel().dimension().location().toString(),position,Direction.UP,original.getUUID(),null);
        try(var provider=net.foundations.pl4.api.InfoProviders.register("pl4_test:entity_replacement_guard",(context,out)->{
            context.entity().remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            h.spawn(net.minecraft.world.entity.EntityType.ARMOR_STAND,position);
            out.add("late","Late entity sample",1,1,"unit");
        })){
            var rows=net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),target);
            h.assertTrue(rows.stream().noneMatch(r->r.key().equals("pl4_test:entity_replacement_guard/late")),
                "Provider rows collected after entity replacement must not be published");
        }
        h.succeed();
    }

}
