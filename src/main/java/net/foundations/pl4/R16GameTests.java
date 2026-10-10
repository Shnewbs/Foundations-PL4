package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Native item-path acceptance for the R16 passive-node transfer pool. */
@net.minecraftforge.gametest.GameTestHolder(namespace=FoundationsPL4.ID)
@net.minecraftforge.gametest.GameTestDontPrefix
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

    @GameTest(template="empty")
    public static void passiveNodeFeedsAddTransferNode(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(add,Direction.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==17,"Normal Node must feed an ADD Transfer Node");h.succeed();
    }

    @GameTest(template="empty")
    public static void removeTransferNodeFeedsPassiveNode(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.EMERALD,13));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),node=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);part(remove,Direction.WEST).transferMode=2;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==13,"REMOVE Transfer Node must feed a normal Node endpoint");h.succeed();
    }

    @GameTest(template="empty")
    public static void explicitAddBeatsPassiveNodeOnTie(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),explicit=chest(h,new BlockPos(5,1,1)),passive=chest(h,new BlockPos(8,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST),node=host(h,new BlockPos(7,1,1),Kind.NODE,Direction.EAST);
        part(remove,Direction.WEST).transferMode=2;part(add,Direction.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST),ref(add,Direction.EAST)));
        h.assertTrue(explicit.getItem(0).getCount()==17&&passive.getItem(0).isEmpty(),"Explicit ADD peer must win an equal-priority passive endpoint");h.succeed();
    }

    @GameTest(template="empty")
    public static void fullPassiveDestinationDoesNotExtract(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));for(int i=0;i<to.getContainerSize();i++)to.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),node=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);part(remove,Direction.WEST).transferMode=2;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST)));
        h.assertTrue(from.getItem(0).getCount()==17&&part(remove,Direction.WEST).pendingItem.isEmpty(),"Full passive endpoint must not extract or escrow source items");h.succeed();
    }

    @GameTest(template="empty")
    public static void addFilterAppliesToPassiveSource(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.IRON_INGOT,8));from.setItem(1,new ItemStack(Items.DIAMOND,7));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);Part p=part(add,Direction.EAST);p.transferMode=1;p.filter="minecraft:diamond";
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        h.assertTrue(from.getItem(0).getCount()==8&&from.getItem(1).isEmpty()&&to.getItem(0).is(Items.DIAMOND)&&to.getItem(0).getCount()==7,"ADD filter must constrain passive source imports");h.succeed();
    }

    @GameTest(template="empty")
    public static void bidirectionalDoesNotBlindlyUsePassivePool(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,9));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST),both=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(both,Direction.EAST).transferMode=3;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(both,Direction.EAST)));
        h.assertTrue(from.getItem(0).getCount()==9&&to.getItem(0).isEmpty(),"ADD/REMOVE must be peer-only until directional filters/channels exist");h.succeed();
    }

    @GameTest(template="empty")
    public static void bidirectionalCanFeedExplicitAddPeer(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.GOLD_INGOT,12));
        HostEntity both=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(both,Direction.WEST).transferMode=3;part(add,Direction.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(both,Direction.WEST),ref(add,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==12,"ADD/REMOVE may act as source for an explicit ADD peer");h.succeed();
    }

    @GameTest(template="empty")
    public static void removeCanFeedBidirectionalExplicitPeer(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.REDSTONE,21));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),both=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(remove,Direction.WEST).transferMode=2;part(both,Direction.EAST).transferMode=3;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(both,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==21,"REMOVE may feed an explicit ADD/REMOVE peer");h.succeed();
    }
    @GameTest(template="empty")
    public static void r3ChannelIsolationAndPersistence(GameTestHelper h){
        var from=chest(h,new BlockPos(1,1,1));var to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        var remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST);var node=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);
        var source=part(remove,Direction.WEST);var sink=part(node,Direction.EAST);source.transferMode=2;source.outputChannel="ore";sink.inputChannel="fuel";
        var plan=TransferEngine.prepare(List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST)));
        TransferEngine.run(h.getLevel().getServer(),plan);
        h.assertTrue(from.getItem(0).getCount()==17&&to.getItem(0).isEmpty()&&source.pendingItem.isEmpty(),"Different channels cannot extract or deliver");
        sink.inputChannel="ore";TransferEngine.run(h.getLevel().getServer(),plan);
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==17,"Matching live channel works without rebuilding plan");
        var restored=Part.load(source.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(restored.outputChannel.equals("ore")&&restored.inputChannel.isEmpty(),"Directional channels persist");h.succeed();
    }

    @GameTest(template="empty")
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

    @GameTest(template="empty")
    public static void r3ComponentControlsPreserveOtherLinksAndClockSettings(GameTestHelper h){
        var array=host(h,new BlockPos(2,1,1),Kind.ARRAY,Direction.WEST);var p=part(array,Direction.WEST);
        var a=new Part.Link("minecraft:overworld",new BlockPos(1,1,1),Direction.UP,null,null);var b=new Part.Link("minecraft:overworld",new BlockPos(5,1,1),Direction.DOWN,null,null);p.links.add(a);p.links.add(b);
        var user=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-R3-Test"));var at=array.getBlockPos();user.setPos(at.getX(),at.getY()+1,at.getZ());
        var remove=new PLPackets.Edit(at,p.slot(),p.identity,"remove_link",ReaderChannels.id(a));PLPackets.edit(user,remove);PLPackets.edit(user,remove);
        h.assertTrue(p.links.equals(List.of(b)),"Repeated stale removal cannot delete the next link");
        var clock=host(h,new BlockPos(4,1,1),Kind.CLOCK,Direction.WEST);var c=part(clock,Direction.WEST);var cp=clock.getBlockPos();user.setPos(cp.getX(),cp.getY()+1,cp.getZ());
        PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_pulse","12"));PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_phase","30"));PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_paused","true"));
        var restored=Part.load(c.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(restored.clockPulse==12&&restored.clockPhase==30&&restored.clockPaused,"Clock controls persist");
        PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_pulse","-1"));h.assertTrue(c.clockPulse==12,"Invalid clock setting rejected");
        c.owner=UUID.randomUUID();PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_phase","7"));h.assertTrue(c.clockPhase==30,"Foreign owner cannot edit clock");h.succeed();
    }


    @GameTest(template="empty")
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
    @GameTest(template="empty")
    public static void directionalFiltersApplyToFluidsAndRetainLegacyDefaults(GameTestHelper h){
        Part p=new Part(Kind.TRANSFER_NODE,Direction.UP,OWNER);p.filter="minecraft:water";
        var water=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000);
        var lava=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000);
        h.assertTrue(TransferFilters.fluids(water,p,true)&&!TransferFilters.fluids(lava,p,true),"INHERIT keeps the legacy filter");
        p.outputFilterMode="ALLOW";p.outputFilter="minecraft:lava";
        h.assertTrue(TransferFilters.fluids(lava,p,false)&&!TransferFilters.fluids(water,p,false)&&TransferFilters.fluids(water,p,true),"Receive and send fluid filters are independent");
        p.outputFilterMode="DENY";h.assertTrue(!TransferFilters.fluids(lava,p,false)&&TransferFilters.fluids(water,p,false),"Mode changes do not leave stale cached results");
        Part saved=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(saved.outputFilterMode.equals("DENY")&&saved.outputFilter.equals("minecraft:lava"),"Directional filters survive restart serialization");h.succeed();
    }
    @GameTest(template="empty")
    public static void escrowBlocksRouteAndFilterEditsAndSyncsToClient(GameTestHelper h){
        HostEntity host=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.UP);Part p=part(host,Direction.UP);p.transferMode=2;p.pendingItem=new ItemStack(Items.DIAMOND,3);
        var user=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-01-Test"));var at=host.getBlockPos();user.setPos(at.getX(),at.getY()+1,at.getZ());
        for(var entry:Map.of("input_filter","minecraft:stone","output_filter_mode","DENY","transfer","1","input_channel","other","filter","minecraft:dirt","whitelist","false").entrySet())PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,entry.getKey(),entry.getValue()));
        h.assertTrue(p.inputFilter.isEmpty()&&p.outputFilterMode.equals("INHERIT")&&p.transferMode==2&&p.inputChannel.isEmpty()&&p.filter.isEmpty()&&p.whitelist,"Buffered resources protect route semantics from settings edits");
        Part client=Part.load(p.save(h.getLevel().registryAccess(),true),h.getLevel().registryAccess());
        h.assertTrue(client.pendingItem.isEmpty()&&!client.routeEditable(),"Client knows escrow exists without receiving inventory contents");
        p.pendingItem=ItemStack.EMPTY;PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"output_filter_mode","DENY"));
        h.assertTrue(p.outputFilterMode.equals("DENY"),"Drained route becomes editable");h.succeed();
    }
    @GameTest(template="empty")
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
    @GameTest(template="empty")
    public static void signallerStatementPacketsAreBoundedPersistentAndIdentitySafe(GameTestHelper h){
        HostEntity host=host(h,new BlockPos(2,1,1),Kind.SIGNALLER,Direction.UP);Part p=part(host,Direction.UP);
        var user=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-01-Rules"));var at=host.getBlockPos();user.setPos(at.getX(),at.getY()+1,at.getZ());
        for(String invalid:List.of("{}","null","[]","{\"threshold\":true}"))PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"statement_add",invalid));
        h.assertTrue(p.statements.isEmpty(),"Malformed statements are rejected without mutation");
        var json=new com.google.gson.JsonObject();json.addProperty("reader","");json.addProperty("key","value");json.addProperty("operator",">=");json.addProperty("threshold",2);
        for(int i=0;i<20;i++)PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"statement_add",json.toString()));
        h.assertTrue(p.statements.size()==16,"Statements are capped at sixteen");
        PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"statements_all","false"));PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"signal_strength","9"));
        Part saved=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(saved.statements.equals(p.statements)&&!saved.statementsAll&&saved.signalStrength==9,"Statement identities, conditions and mode survive save/reload");
        String id=p.statements.get(0).id().toString();var remove=new PLPackets.Edit(at,p.slot(),p.identity,"statement_remove",id);PLPackets.edit(user,remove);PLPackets.edit(user,remove);
        h.assertTrue(p.statements.size()==15,"Repeated stale removal does not remove a different statement");h.succeed();
    }

    @GameTest(template="empty")
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
        var user=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,name));user.getInventory().clearContent();
        ItemStack tool=new ItemStack(FoundationsPL4.item("wirelessstorage"));var binding=new Part.Link(h.getLevel().dimension().location().toString(),host.getBlockPos(),node.face,null,node.identity);
        net.foundations.pl4.compat.CustomData.update(net.foundations.pl4.compat.DataComponents.CUSTOM_DATA,tool,t->t.put("pl_link",binding.save()));
        user.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);return new StorageFixture(host,node,chest,user,tool);
    }
    @GameTest(template="empty")
    public static void wirelessStorageWithdrawalCannotReplay(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-A");f.chest().setItem(0,new ItemStack(Items.DIAMOND,17));
        UUID token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);h.assertTrue(token!=null,"Owned loaded Node opens a storage session");
        var packet=new PLPackets.StorageRequest(token,"withdraw",0,1);WirelessStorage.request(f.player(),packet);WirelessStorage.request(f.player(),packet);
        int count=0;for(int slot=0;slot<36;slot++)if(f.player().getInventory().getItem(slot).is(Items.DIAMOND))count+=f.player().getInventory().getItem(slot).getCount();
        h.assertTrue(f.chest().getItem(0).getCount()==16&&count==1,"Single-use token withdraws exactly once");h.succeed();
    }
    @GameTest(template="empty")
    public static void wirelessStorageRechecksSlotsFiltersAndHeldTool(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-B");f.chest().setItem(0,new ItemStack(Items.DIAMOND,17));
        UUID token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.chest().setItem(0,new ItemStack(Items.EMERALD,9));
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));h.assertTrue(f.chest().getItem(0).getCount()==9,"Stale slot does not withdraw a different resource");
        token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.node().outputFilterMode="DENY";f.node().outputFilter="minecraft:emerald";
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));h.assertTrue(f.chest().getItem(0).getCount()==9,"Current send filter is rechecked at action time");
        token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.player().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));h.assertTrue(f.chest().getItem(0).getCount()==9,"Removing the held tool revokes the session");h.succeed();
    }
    @GameTest(template="empty")
    public static void wirelessStorageDepositConservesOffhandAndHonorsFilters(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-C");f.player().setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.DIAMOND,11));f.node().inputFilterMode="DENY";f.node().inputFilter="minecraft:diamond";
        UUID token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"deposit",0,0));
        h.assertTrue(f.player().getOffhandItem().getCount()==11&&f.chest().getItem(0).isEmpty(),"Receive filter rejects deposit without consuming offhand items");
        f.node().inputFilter="";token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"deposit",0,0));
        h.assertTrue(f.player().getOffhandItem().isEmpty()&&f.chest().getItem(0).getCount()==11,"Deposit moves exactly the accepted offhand stack");h.succeed();
    }
    @GameTest(template="empty")
    public static void wirelessStorageRechecksOwnerAndPartIdentity(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-D");f.chest().setItem(0,new ItemStack(Items.DIAMOND,17));
        UUID token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.node().owner=UUID.randomUUID();
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));h.assertTrue(f.chest().getItem(0).getCount()==17,"Owner changes invalidate storage access");
        f.node().owner=OWNER;token=WirelessStorage.open(f.player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.node().identity=UUID.randomUUID();
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));h.assertTrue(f.chest().getItem(0).getCount()==17,"Replacing a Node cannot inherit another binding's authority");h.succeed();
    }
    private record NetworkStorageFixture(StorageFixture first,HostEntity secondHost,Part second,ChestBlockEntity secondChest){}
    private static NetworkStorageFixture networkStorage(GameTestHelper h,String name){
        var first=storageFixture(h,name);var secondChest=chest(h,new BlockPos(4,1,1));var secondHost=host(h,new BlockPos(3,1,1),Kind.NODE,Direction.EAST);
        NetworkEngine.rebuild(h.getLevel().getServer());return new NetworkStorageFixture(first,secondHost,part(secondHost,Direction.EAST),secondChest);
    }
    @GameTest(template="empty")
    public static void networkStorageAggregatesAndWithdrawsAcrossNodes(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-A");f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,17));f.secondChest().setItem(0,new ItemStack(Items.DIAMOND,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(WirelessStorage.visibleCounts(f.first().player()).equals(List.of(40L)),"Matching variants combine across connected inventories");
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"withdraw",0,64));
        int count=0;for(int slot=0;slot<36;slot++)if(f.first().player().getInventory().getItem(slot).is(Items.DIAMOND))count+=f.first().player().getInventory().getItem(slot).getCount();
        h.assertTrue(count==40&&f.first().chest().getItem(0).isEmpty()&&f.secondChest().getItem(0).isEmpty(),"Withdrawal conserves all 40 items across sources");h.succeed();
    }
    @GameTest(template="empty")
    public static void networkStorageRevokesNewlyForeignEndpoint(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-B");f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,5));f.secondChest().setItem(0,new ItemStack(Items.DIAMOND,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);f.second().owner=UUID.randomUUID();
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"withdraw",0,64));
        h.assertTrue(f.first().chest().getItem(0).isEmpty()&&f.secondChest().getItem(0).getCount()==23,"Every remote Node ownership is rechecked, not just the anchor");h.succeed();
    }
    @GameTest(template="empty")
    public static void networkStorageDropsDisconnectedSources(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-C");f.secondChest().setItem(0,new ItemStack(Items.DIAMOND,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);
        f.first().host().parts.get(6).blockedFaces=1<<Direction.EAST.ordinal();f.secondHost().parts.get(6).blockedFaces=1<<Direction.WEST.ordinal();f.first().host().changed();f.secondHost().changed();
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"withdraw",0,64));
        h.assertTrue(f.secondChest().getItem(0).getCount()==23,"Stale session cannot reach an endpoint after cable disconnection");h.succeed();
    }
    @GameTest(template="empty")
    public static void networkStorageSearchAndCountSort(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-D");f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,5));f.secondChest().setItem(0,new ItemStack(Items.EMERALD,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"search",0,0,"","COUNT"));
        h.assertTrue(WirelessStorage.visibleCounts(f.first().player()).equals(List.of(23L,5L)),"Count sort orders combined counts descending");
        token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"search",0,0,"minecraft:emerald","NAME"));
        h.assertTrue(WirelessStorage.visibleCounts(f.first().player()).equals(List.of(23L)),"Search matches item registry identifiers");h.succeed();
    }
    @GameTest(template="empty")
    public static void networkStorageDepositsPastFullInventory(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-E");for(int i=0;i<27;i++)f.first().chest().setItem(i,new ItemStack(Items.STONE,64));
        f.first().player().setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.DIAMOND,11));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"deposit",0,0));
        h.assertTrue(f.first().player().getOffhandItem().isEmpty()&&f.secondChest().getItem(0).getCount()==11,"Deposit finds space on a connected Node and conserves items");h.succeed();
    }
    @GameTest(template="empty")
    public static void networkStoragePreservesComponentVariants(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-F");ItemStack named=new ItemStack(Items.DIAMOND,7);net.foundations.pl4.compat.PortData.set(named,net.foundations.pl4.compat.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Named diamond"));
        f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,5));f.secondChest().setItem(0,named);
        WirelessStorage.open(f.first().player(),net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(WirelessStorage.visibleCounts(f.first().player()).size()==2,"Distinct item components never collapse into one withdrawable variant");h.succeed();
    }
    @GameTest(template="empty")
    public static void componentLinksValidateTypesAndDeduplicate(GameTestHelper h){
        var f=storageFixture(h,"PL4-Links-A");Part array=new Part(Kind.ARRAY,Direction.NORTH,OWNER);f.host().parts.put(array.slot(),array);
        Part entity=new Part(Kind.ENTITY_NODE,Direction.SOUTH,OWNER);f.host().parts.put(entity.slot(),entity);
        var block=new Part.Link(h.getLevel().dimension().location().toString(),f.chest().getBlockPos(),Direction.UP,null,null);
        h.assertTrue(ComponentLinks.add(f.player(),f.host(),array,block),"Array accepts a permitted loaded block link");
        h.assertTrue(!ComponentLinks.add(f.player(),f.host(),array,block)&&array.links.size()==1,"Duplicate links do not consume another Array slot");
        h.assertTrue(!ComponentLinks.add(f.player(),f.host(),entity,block),"Entity Node rejects block links");h.succeed();
    }
    @GameTest(template="empty")
    public static void componentLinksRejectForeignEmitters(GameTestHelper h){
        var f=storageFixture(h,"PL4-Links-B");Part receiver=new Part(Kind.DATA_RECEIVER,Direction.NORTH,OWNER);f.host().parts.put(receiver.slot(),receiver);
        var remote=host(h,new BlockPos(4,1,1),Kind.DATA_EMITTER,Direction.UP);Part emitter=part(remote,Direction.UP);
        var link=new Part.Link(h.getLevel().dimension().location().toString(),remote.getBlockPos(),emitter.face,null,emitter.identity);
        emitter.owner=UUID.randomUUID();h.assertTrue(!ComponentLinks.add(f.player(),f.host(),receiver,link),"Foreign emitter never grants a wireless network edge");
        emitter.owner=OWNER;h.assertTrue(ComponentLinks.add(f.player(),f.host(),receiver,link),"Owned matching emitter can be selected");h.succeed();
    }

}
