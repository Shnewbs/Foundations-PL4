package net.foundations.pl4;

import java.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.foundations.pl4.compat.scenarios.GameTest;
import net.foundations.pl4.compat.scenarios.GameTestHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityChest;
import net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate;

/** Native item-path acceptance for the R16 passive-node transfer pool. */
@PrefixGameTestTemplate(false)
public final class R16GameTests {
    private static final UUID OWNER=UUID.fromString("aaaa0000-0000-0000-0000-000000000016");
    private static HostEntity host(GameTestHelper h,BlockPos p,Kind kind,EnumFacing face){
        h.setBlock(p,FoundationsPL4.HOST.get());HostEntity host=(HostEntity)h.getBlockEntity(p);
        Part part=new Part(kind,face,OWNER);host.parts.put(part.slot(),part);
        host.parts.put(6,new Part(Kind.DATA_CABLE,EnumFacing.DOWN,OWNER));host.changed();return host;
    }
    private static Part part(HostEntity host,EnumFacing face){return host.parts.get(face.ordinal());}
    private static TileEntityChest chest(GameTestHelper h,BlockPos p){h.setBlock(p,Blocks.CHEST);return (TileEntityChest)h.getBlockEntity(p);}
    private static NetworkEngine.Ref ref(HostEntity h,EnumFacing face){return new NetworkEngine.Ref(h,part(h,face));}

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void passiveNodeFeedsAddTransferNode(GameTestHelper h){
        TileEntityChest from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,EnumFacing.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,EnumFacing.EAST);part(add,EnumFacing.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,EnumFacing.WEST),ref(add,EnumFacing.EAST)));
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==17,"Normal Node must feed an ADD Transfer Node");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void removeTransferNodeFeedsPassiveNode(GameTestHelper h){
        TileEntityChest from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.EMERALD,13));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,EnumFacing.WEST),node=host(h,new BlockPos(4,1,1),Kind.NODE,EnumFacing.EAST);part(remove,EnumFacing.WEST).transferMode=2;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,EnumFacing.WEST),ref(node,EnumFacing.EAST)));
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==13,"REMOVE Transfer Node must feed a normal Node endpoint");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void explicitAddBeatsPassiveNodeOnTie(GameTestHelper h){
        TileEntityChest from=chest(h,new BlockPos(1,1,1)),explicit=chest(h,new BlockPos(5,1,1)),passive=chest(h,new BlockPos(8,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,EnumFacing.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,EnumFacing.EAST),node=host(h,new BlockPos(7,1,1),Kind.NODE,EnumFacing.EAST);
        part(remove,EnumFacing.WEST).transferMode=2;part(add,EnumFacing.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,EnumFacing.WEST),ref(node,EnumFacing.EAST),ref(add,EnumFacing.EAST)));
        net.foundations.pl4.compat.PortAssertions.check(explicit.getItem(0).getCount()==17&&passive.getItem(0).isEmpty(),"Explicit ADD peer must win an equal-priority passive endpoint");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void fullPassiveDestinationDoesNotExtract(GameTestHelper h){
        TileEntityChest from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));for(int i=0;i<to.getContainerSize();i++)to.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,EnumFacing.WEST),node=host(h,new BlockPos(4,1,1),Kind.NODE,EnumFacing.EAST);part(remove,EnumFacing.WEST).transferMode=2;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,EnumFacing.WEST),ref(node,EnumFacing.EAST)));
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).getCount()==17&&part(remove,EnumFacing.WEST).pendingItem.isEmpty(),"Full passive endpoint must not extract or escrow source items");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void addFilterAppliesToPassiveSource(GameTestHelper h){
        TileEntityChest from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.IRON_INGOT,8));from.setItem(1,new ItemStack(Items.DIAMOND,7));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,EnumFacing.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,EnumFacing.EAST);Part p=part(add,EnumFacing.EAST);p.transferMode=1;p.filter="minecraft:diamond";
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,EnumFacing.WEST),ref(add,EnumFacing.EAST)));
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).getCount()==8&&from.getItem(1).isEmpty()&&(to.getItem(0).getItem()==Items.DIAMOND)&&to.getItem(0).getCount()==7,"ADD filter must constrain passive source imports");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void bidirectionalDoesNotBlindlyUsePassivePool(GameTestHelper h){
        TileEntityChest from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,9));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,EnumFacing.WEST),both=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,EnumFacing.EAST);part(both,EnumFacing.EAST).transferMode=3;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,EnumFacing.WEST),ref(both,EnumFacing.EAST)));
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).getCount()==9&&to.getItem(0).isEmpty(),"ADD/REMOVE must be peer-only until directional filters/channels exist");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void bidirectionalCanFeedExplicitAddPeer(GameTestHelper h){
        TileEntityChest from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.GOLD_INGOT,12));
        HostEntity both=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,EnumFacing.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,EnumFacing.EAST);part(both,EnumFacing.WEST).transferMode=3;part(add,EnumFacing.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(both,EnumFacing.WEST),ref(add,EnumFacing.EAST)));
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==12,"ADD/REMOVE may act as source for an explicit ADD peer");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void removeCanFeedBidirectionalExplicitPeer(GameTestHelper h){
        TileEntityChest from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.REDSTONE,21));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,EnumFacing.WEST),both=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,EnumFacing.EAST);part(remove,EnumFacing.WEST).transferMode=2;part(both,EnumFacing.EAST).transferMode=3;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,EnumFacing.WEST),ref(both,EnumFacing.EAST)));
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==21,"REMOVE may feed an explicit ADD/REMOVE peer");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void r3ChannelIsolationAndPersistence(GameTestHelper h){
        var from=chest(h,new BlockPos(1,1,1));var to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        var remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,EnumFacing.WEST);var node=host(h,new BlockPos(4,1,1),Kind.NODE,EnumFacing.EAST);
        var source=part(remove,EnumFacing.WEST);var sink=part(node,EnumFacing.EAST);source.transferMode=2;source.outputChannel="ore";sink.inputChannel="fuel";
        var plan=TransferEngine.prepare(List.of(ref(remove,EnumFacing.WEST),ref(node,EnumFacing.EAST)));
        TransferEngine.run(h.getLevel().getServer(),plan);
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).getCount()==17&&to.getItem(0).isEmpty()&&source.pendingItem.isEmpty(),"Different channels cannot extract or deliver");
        sink.inputChannel="ore";TransferEngine.run(h.getLevel().getServer(),plan);
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==17,"Matching live channel works without rebuilding plan");
        var restored=Part.load(source.save(null,false),null);
        net.foundations.pl4.compat.PortAssertions.check(restored.outputChannel.equals("ore")&&restored.inputChannel.isEmpty(),"Directional channels persist");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void r3EqualPrioritySinksShareRepeatedSingleSlotExports(GameTestHelper h){
        int old=PLConfig.NETWORK_ITEM_RATE.get();try{
            PLConfig.NETWORK_ITEM_RATE.set(1);
            var from=chest(h,new BlockPos(1,1,1));var a=chest(h,new BlockPos(5,1,1));var b=chest(h,new BlockPos(8,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,20));
            var source=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,EnumFacing.WEST);part(source,EnumFacing.WEST).transferMode=2;
            var sa=host(h,new BlockPos(4,1,1),Kind.NODE,EnumFacing.EAST);var sb=host(h,new BlockPos(7,1,1),Kind.NODE,EnumFacing.EAST);
            var plan=TransferEngine.prepare(List.of(ref(source,EnumFacing.WEST),ref(sa,EnumFacing.EAST),ref(sb,EnumFacing.EAST)));
            for(int n=0;n<20;n++)TransferEngine.run(h.getLevel().getServer(),plan);
            net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).isEmpty()&&a.getItem(0).getCount()==10&&b.getItem(0).getCount()==10&&part(source,EnumFacing.WEST).pendingItem.isEmpty(),"Equal sinks share 20 bounded cycles independently of source slot cursor");h.succeed();
        }finally{PLConfig.NETWORK_ITEM_RATE.set(old);}
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void r3ComponentControlsPreserveOtherLinksAndClockSettings(GameTestHelper h){
        var array=host(h,new BlockPos(2,1,1),Kind.ARRAY,EnumFacing.WEST);var p=part(array,EnumFacing.WEST);
        var a=new Part.Link("minecraft:overworld",new BlockPos(1,1,1),EnumFacing.UP,null,null);var b=new Part.Link("minecraft:overworld",new BlockPos(5,1,1),EnumFacing.DOWN,null,null);p.links.add(a);p.links.add(b);
        var user=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-R3-Test"));var at=array.getBlockPos();user.setPos(at.getX(),at.getY()+1,at.getZ());
        var remove=new PLPackets.Edit(at,p.slot(),p.identity,"remove_link",ReaderChannels.id(a));PLPackets.edit(user,remove);PLPackets.edit(user,remove);
        net.foundations.pl4.compat.PortAssertions.check(p.links.equals(List.of(b)),"Repeated stale removal cannot delete the next link");
        var clock=host(h,new BlockPos(4,1,1),Kind.CLOCK,EnumFacing.WEST);var c=part(clock,EnumFacing.WEST);var cp=clock.getBlockPos();user.setPos(cp.getX(),cp.getY()+1,cp.getZ());
        PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_pulse","12"));PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_phase","30"));PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_paused","true"));
        var restored=Part.load(c.save(null,false),null);
        net.foundations.pl4.compat.PortAssertions.check(restored.clockPulse==12&&restored.clockPhase==30&&restored.clockPaused,"Clock controls persist");
        PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_pulse","-1"));net.foundations.pl4.compat.PortAssertions.check(c.clockPulse==12,"Invalid clock setting rejected");
        c.owner=UUID.randomUUID();PLPackets.edit(user,new PLPackets.Edit(cp,c.slot(),c.identity,"clock_phase","7"));net.foundations.pl4.compat.PortAssertions.check(c.clockPhase==30,"Foreign owner cannot edit clock");h.succeed();
    }


    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void directionalFiltersConstrainPassiveImportsAndExports(GameTestHelper h){
        TileEntityChest from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.IRON_INGOT,8));from.setItem(1,new ItemStack(Items.DIAMOND,7));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,EnumFacing.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,EnumFacing.EAST);
        Part source=part(node,EnumFacing.WEST),sink=part(add,EnumFacing.EAST);sink.transferMode=1;
        source.outputFilterMode="ALLOW";source.outputFilter="minecraft:diamond";sink.inputFilterMode="DENY";sink.inputFilter="minecraft:diamond";
        var network=List.of(ref(node,EnumFacing.WEST),ref(add,EnumFacing.EAST));TransferEngine.run(h.getLevel().getServer(),network);
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).getCount()==8&&from.getItem(1).getCount()==7&&to.getItem(0).isEmpty(),"Both directional filters must agree before extracting");
        sink.inputFilter="minecraft:iron_ingot";TransferEngine.run(h.getLevel().getServer(),network);
        net.foundations.pl4.compat.PortAssertions.check(from.getItem(0).getCount()==8&&from.getItem(1).isEmpty()&&to.getItem(0).getCount()==7,"Passive output and ADD input filters route only matching items");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void directionalFiltersApplyToFluidsAndRetainLegacyDefaults(GameTestHelper h){
        Part p=new Part(Kind.TRANSFER_NODE,EnumFacing.UP,OWNER);p.filter="minecraft:water";
        var water=new net.minecraftforge.fluids.FluidStack(net.minecraft.init.Fluids.WATER,1000);
        var lava=new net.minecraftforge.fluids.FluidStack(net.minecraft.init.Fluids.LAVA,1000);
        net.foundations.pl4.compat.PortAssertions.check(TransferFilters.fluids(water,p,true)&&!TransferFilters.fluids(lava,p,true),"INHERIT keeps the legacy filter");
        p.outputFilterMode="ALLOW";p.outputFilter="minecraft:lava";
        net.foundations.pl4.compat.PortAssertions.check(TransferFilters.fluids(lava,p,false)&&!TransferFilters.fluids(water,p,false)&&TransferFilters.fluids(water,p,true),"Receive and send fluid filters are independent");
        p.outputFilterMode="DENY";net.foundations.pl4.compat.PortAssertions.check(!TransferFilters.fluids(lava,p,false)&&TransferFilters.fluids(water,p,false),"Mode changes do not leave stale cached results");
        Part saved=Part.load(p.save(null,false),null);
        net.foundations.pl4.compat.PortAssertions.check(saved.outputFilterMode.equals("DENY")&&saved.outputFilter.equals("minecraft:lava"),"Directional filters survive restart serialization");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void escrowBlocksRouteAndFilterEditsAndSyncsToClient(GameTestHelper h){
        HostEntity host=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,EnumFacing.UP);Part p=part(host,EnumFacing.UP);p.transferMode=2;p.pendingItem=new ItemStack(Items.DIAMOND,3);
        var user=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-01-Test"));var at=host.getBlockPos();user.setPos(at.getX(),at.getY()+1,at.getZ());
        for(var entry:Map.of("input_filter","minecraft:stone","output_filter_mode","DENY","transfer","1","input_channel","other","filter","minecraft:dirt","whitelist","false").entrySet())PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,entry.getKey(),entry.getValue()));
        net.foundations.pl4.compat.PortAssertions.check(p.inputFilter.isEmpty()&&p.outputFilterMode.equals("INHERIT")&&p.transferMode==2&&p.inputChannel.isEmpty()&&p.filter.isEmpty()&&p.whitelist,"Buffered resources protect route semantics from settings edits");
        Part client=Part.load(p.save(null,true),null);
        net.foundations.pl4.compat.PortAssertions.check(client.pendingItem.isEmpty()&&!client.routeEditable(),"Client knows escrow exists without receiving inventory contents");
        p.pendingItem=ItemStack.EMPTY;PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"output_filter_mode","DENY"));
        net.foundations.pl4.compat.PortAssertions.check(p.outputFilterMode.equals("DENY"),"Drained route becomes editable");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void signallerStatementsHandleMissingAndAmbiguousReaders(GameTestHelper h){
        HostEntity a=host(h,new BlockPos(1,1,1),Kind.INFO_READER,EnumFacing.UP),b=host(h,new BlockPos(3,1,1),Kind.INFO_READER,EnumFacing.UP);
        Part first=part(a,EnumFacing.UP),second=part(b,EnumFacing.UP);first.label=second.label="same";first.rows.add(new Part.Row("value","Value",10,20,""));second.rows.add(new Part.Row("value","Value",5,20,""));
        var readers=List.of(ref(a,EnumFacing.UP),ref(b,EnumFacing.UP));Part signal=new Part(Kind.SIGNALLER,EnumFacing.UP,OWNER);signal.signalStrength=7;
        signal.statements.add(new net.foundations.pl4.core.SignalRules.Statement(UUID.randomUUID(),first.identity.toString(),"value",">=",10));
        signal.statements.add(new net.foundations.pl4.core.SignalRules.Statement(UUID.randomUUID(),second.identity.toString(),"missing","!=",0));
        net.foundations.pl4.compat.PortAssertions.check(SignallerLogic.evaluate(signal,readers)==0,"Missing metric is false even for !=");signal.statementsAll=false;
        net.foundations.pl4.compat.PortAssertions.check(SignallerLogic.evaluate(signal,readers)==7,"ANY returns configured strength when a statement matches");
        signal.statements.clear();signal.selected="same";signal.threshold=1;
        net.foundations.pl4.compat.PortAssertions.check(SignallerLogic.evaluate(signal,readers)==0,"Duplicate reader labels cannot silently pick a different reader");
        signal.selected=first.identity.toString();net.foundations.pl4.compat.PortAssertions.check(SignallerLogic.evaluate(signal,readers)==7,"Stable reader UUID restores the legacy single condition");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void signallerStatementPacketsAreBoundedPersistentAndIdentitySafe(GameTestHelper h){
        HostEntity host=host(h,new BlockPos(2,1,1),Kind.SIGNALLER,EnumFacing.UP);Part p=part(host,EnumFacing.UP);
        var user=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-01-Rules"));var at=host.getBlockPos();user.setPos(at.getX(),at.getY()+1,at.getZ());
        for(String invalid:List.of("{}","null","[]","{\"threshold\":true}"))PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"statement_add",invalid));
        net.foundations.pl4.compat.PortAssertions.check(p.statements.isEmpty(),"Malformed statements are rejected without mutation");
        var json=new com.google.gson.JsonObject();json.addProperty("reader","");json.addProperty("key","value");json.addProperty("operator",">=");json.addProperty("threshold",2);
        for(int i=0;i<20;i++)PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"statement_add",json.toString()));
        net.foundations.pl4.compat.PortAssertions.check(p.statements.size()==16,"Statements are capped at sixteen");
        PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"statements_all","false"));PLPackets.edit(user,new PLPackets.Edit(at,p.slot(),p.identity,"signal_strength","9"));
        Part saved=Part.load(p.save(null,false),null);
        net.foundations.pl4.compat.PortAssertions.check(saved.statements.equals(p.statements)&&!saved.statementsAll&&saved.signalStrength==9,"Statement identities, conditions and mode survive save/reload");
        String id=p.statements.get(0).id().toString();var remove=new PLPackets.Edit(at,p.slot(),p.identity,"statement_remove",id);PLPackets.edit(user,remove);PLPackets.edit(user,remove);
        net.foundations.pl4.compat.PortAssertions.check(p.statements.size()==15,"Repeated stale removal does not remove a different statement");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void providerApiRejectsWrongDimensionBeforeInvokingAddons(GameTestHelper h){
        int[] calls={0};
        try(var provider=net.foundations.pl4.api.InfoProviders.register("pl4_test:alpha_dimension",(context,out)->calls[0]++)){
            var wrong=new Part.Link("pl4_test:missing_dimension",h.absolutePos(new BlockPos(1,1,1)),EnumFacing.UP,null,null);
            net.foundations.pl4.compat.PortAssertions.check(net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),wrong).isEmpty()&&calls[0]==0,"Wrong-dimension targets cannot sample the same coordinates in another world");
            var ids=net.foundations.pl4.api.InfoProviders.registeredIds();net.foundations.pl4.compat.PortAssertions.check(ids.contains("pl4_test:alpha_dimension")&&ids.contains("foundations_pl4:vanilla"),"API exposes builtin and registered provider identities");
            boolean immutable=false;try{ids.clear();}catch(UnsupportedOperationException expected){immutable=true;}net.foundations.pl4.compat.PortAssertions.check(immutable,"Diagnostic inventory is immutable");
        }
        net.foundations.pl4.compat.PortAssertions.check(!net.foundations.pl4.api.InfoProviders.registeredIds().contains("pl4_test:alpha_dimension"),"Closing provider removes its diagnostic entry");h.succeed();
    }

    private record StorageFixture(HostEntity host,Part node,TileEntityChest chest,net.minecraft.entity.player.EntityPlayerMP player,ItemStack tool){}
    private static StorageFixture storageFixture(GameTestHelper h,String name){
        TileEntityChest chest=chest(h,new BlockPos(1,1,1));HostEntity host=host(h,new BlockPos(2,1,1),Kind.NODE,EnumFacing.WEST);Part node=part(host,EnumFacing.WEST);
        var user=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,name));user.inventory.clearContent();
        ItemStack tool=new ItemStack(FoundationsPL4.item("wirelessstorage"));var binding=new Part.Link(h.getLevel().dimension.getType().getRegistryName().toString(),host.getBlockPos(),node.face,null,node.identity);
        net.foundations.pl4.compat.CustomData.update(net.foundations.pl4.compat.DataComponents.CUSTOM_DATA,tool,t->t.put("pl_link",binding.save()));
        user.setItemInHand(net.minecraft.util.EnumHand.MAIN_HAND,tool);return new StorageFixture(host,node,chest,user,tool);
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageWithdrawalCannotReplay(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-A");f.chest().setItem(0,new ItemStack(Items.DIAMOND,17));
        UUID token=WirelessStorage.open(f.player(),net.minecraft.util.EnumHand.MAIN_HAND);net.foundations.pl4.compat.PortAssertions.check(token!=null,"Owned loaded Node opens a storage session");
        var packet=new PLPackets.StorageRequest(token,"withdraw",0,1);WirelessStorage.request(f.player(),packet);WirelessStorage.request(f.player(),packet);
        int count=0;for(int slot=0;slot<36;slot++)if((f.player().inventory.getItem(slot).getItem()==Items.DIAMOND))count+=f.player().inventory.getItem(slot).getCount();
        net.foundations.pl4.compat.PortAssertions.check(f.chest().getItem(0).getCount()==16&&count==1,"Single-use token withdraws exactly once");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageRechecksSlotsFiltersAndHeldTool(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-B");f.chest().setItem(0,new ItemStack(Items.DIAMOND,17));
        UUID token=WirelessStorage.open(f.player(),net.minecraft.util.EnumHand.MAIN_HAND);f.chest().setItem(0,new ItemStack(Items.EMERALD,9));
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));net.foundations.pl4.compat.PortAssertions.check(f.chest().getItem(0).getCount()==9,"Stale slot does not withdraw a different resource");
        token=WirelessStorage.open(f.player(),net.minecraft.util.EnumHand.MAIN_HAND);f.node().outputFilterMode="DENY";f.node().outputFilter="minecraft:emerald";
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));net.foundations.pl4.compat.PortAssertions.check(f.chest().getItem(0).getCount()==9,"Current send filter is rechecked at action time");
        token=WirelessStorage.open(f.player(),net.minecraft.util.EnumHand.MAIN_HAND);f.player().setItemInHand(net.minecraft.util.EnumHand.MAIN_HAND,ItemStack.EMPTY);
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));net.foundations.pl4.compat.PortAssertions.check(f.chest().getItem(0).getCount()==9,"Removing the held tool revokes the session");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageDepositConservesOffhandAndHonorsFilters(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-C");f.player().setItemInHand(net.minecraft.util.EnumHand.OFF_HAND,new ItemStack(Items.DIAMOND,11));f.node().inputFilterMode="DENY";f.node().inputFilter="minecraft:diamond";
        UUID token=WirelessStorage.open(f.player(),net.minecraft.util.EnumHand.MAIN_HAND);WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"deposit",0,0));
        net.foundations.pl4.compat.PortAssertions.check(f.player().getOffhandItem().getCount()==11&&f.chest().getItem(0).isEmpty(),"Receive filter rejects deposit without consuming offhand items");
        f.node().inputFilter="";token=WirelessStorage.open(f.player(),net.minecraft.util.EnumHand.MAIN_HAND);WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"deposit",0,0));
        net.foundations.pl4.compat.PortAssertions.check(f.player().getOffhandItem().isEmpty()&&f.chest().getItem(0).getCount()==11,"Deposit moves exactly the accepted offhand stack");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void wirelessStorageRechecksOwnerAndPartIdentity(GameTestHelper h){
        var f=storageFixture(h,"PL4-Storage-D");f.chest().setItem(0,new ItemStack(Items.DIAMOND,17));
        UUID token=WirelessStorage.open(f.player(),net.minecraft.util.EnumHand.MAIN_HAND);f.node().owner=UUID.randomUUID();
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));net.foundations.pl4.compat.PortAssertions.check(f.chest().getItem(0).getCount()==17,"Owner changes invalidate storage access");
        f.node().owner=OWNER;token=WirelessStorage.open(f.player(),net.minecraft.util.EnumHand.MAIN_HAND);f.node().identity=UUID.randomUUID();
        WirelessStorage.request(f.player(),new PLPackets.StorageRequest(token,"withdraw",0,64));net.foundations.pl4.compat.PortAssertions.check(f.chest().getItem(0).getCount()==17,"Replacing a Node cannot inherit another binding's authority");h.succeed();
    }
    private record NetworkStorageFixture(StorageFixture first,HostEntity secondHost,Part second,TileEntityChest secondChest){}
    private static NetworkStorageFixture networkStorage(GameTestHelper h,String name){
        var first=storageFixture(h,name);var secondChest=chest(h,new BlockPos(4,1,1));var secondHost=host(h,new BlockPos(3,1,1),Kind.NODE,EnumFacing.EAST);
        NetworkEngine.rebuild(h.getLevel().getServer());return new NetworkStorageFixture(first,secondHost,part(secondHost,EnumFacing.EAST),secondChest);
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStorageAggregatesAndWithdrawsAcrossNodes(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-A");f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,17));f.secondChest().setItem(0,new ItemStack(Items.DIAMOND,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.util.EnumHand.MAIN_HAND);
        net.foundations.pl4.compat.PortAssertions.check(WirelessStorage.visibleCounts(f.first().player()).equals(List.of(40L)),"Matching variants combine across connected inventories");
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"withdraw",0,64));
        int count=0;for(int slot=0;slot<36;slot++)if((f.first().player().inventory.getItem(slot).getItem()==Items.DIAMOND))count+=f.first().player().inventory.getItem(slot).getCount();
        net.foundations.pl4.compat.PortAssertions.check(count==40&&f.first().chest().getItem(0).isEmpty()&&f.secondChest().getItem(0).isEmpty(),"Withdrawal conserves all 40 items across sources");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStorageRevokesNewlyForeignEndpoint(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-B");f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,5));f.secondChest().setItem(0,new ItemStack(Items.DIAMOND,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.util.EnumHand.MAIN_HAND);f.second().owner=UUID.randomUUID();
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"withdraw",0,64));
        net.foundations.pl4.compat.PortAssertions.check(f.first().chest().getItem(0).isEmpty()&&f.secondChest().getItem(0).getCount()==23,"Every remote Node ownership is rechecked, not just the anchor");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStorageDropsDisconnectedSources(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-C");f.secondChest().setItem(0,new ItemStack(Items.DIAMOND,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.util.EnumHand.MAIN_HAND);
        f.first().host().parts.get(6).blockedFaces=1<<EnumFacing.EAST.ordinal();f.secondHost().parts.get(6).blockedFaces=1<<EnumFacing.WEST.ordinal();f.first().host().changed();f.secondHost().changed();
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"withdraw",0,64));
        net.foundations.pl4.compat.PortAssertions.check(f.secondChest().getItem(0).getCount()==23,"Stale session cannot reach an endpoint after cable disconnection");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStorageSearchAndCountSort(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-D");f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,5));f.secondChest().setItem(0,new ItemStack(Items.EMERALD,23));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.util.EnumHand.MAIN_HAND);
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"search",0,0,"","COUNT"));
        net.foundations.pl4.compat.PortAssertions.check(WirelessStorage.visibleCounts(f.first().player()).equals(List.of(23L,5L)),"Count sort orders combined counts descending");
        token=WirelessStorage.open(f.first().player(),net.minecraft.util.EnumHand.MAIN_HAND);
        WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"search",0,0,"minecraft:emerald","NAME"));
        net.foundations.pl4.compat.PortAssertions.check(WirelessStorage.visibleCounts(f.first().player()).equals(List.of(23L)),"Search matches item registry identifiers");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStorageDepositsPastFullInventory(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-E");for(int i=0;i<27;i++)f.first().chest().setItem(i,new ItemStack(Items.STONE,64));
        f.first().player().setItemInHand(net.minecraft.util.EnumHand.OFF_HAND,new ItemStack(Items.DIAMOND,11));
        UUID token=WirelessStorage.open(f.first().player(),net.minecraft.util.EnumHand.MAIN_HAND);WirelessStorage.request(f.first().player(),new PLPackets.StorageRequest(token,"deposit",0,0));
        net.foundations.pl4.compat.PortAssertions.check(f.first().player().getOffhandItem().isEmpty()&&f.secondChest().getItem(0).getCount()==11,"Deposit finds space on a connected Node and conserves items");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void networkStoragePreservesComponentVariants(GameTestHelper h){
        var f=networkStorage(h,"PL4-Network-F");ItemStack named=new ItemStack(Items.DIAMOND,7);net.foundations.pl4.compat.PortData.set(named,net.foundations.pl4.compat.DataComponents.CUSTOM_NAME,new net.minecraft.util.text.TextComponentString("Named diamond"));
        f.first().chest().setItem(0,new ItemStack(Items.DIAMOND,5));f.secondChest().setItem(0,named);
        WirelessStorage.open(f.first().player(),net.minecraft.util.EnumHand.MAIN_HAND);
        net.foundations.pl4.compat.PortAssertions.check(WirelessStorage.visibleCounts(f.first().player()).size()==2,"Distinct item components never collapse into one withdrawable variant");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void componentLinksValidateTypesAndDeduplicate(GameTestHelper h){
        var f=storageFixture(h,"PL4-Links-A");Part array=new Part(Kind.ARRAY,EnumFacing.NORTH,OWNER);f.host().parts.put(array.slot(),array);
        Part entity=new Part(Kind.ENTITY_NODE,EnumFacing.SOUTH,OWNER);f.host().parts.put(entity.slot(),entity);
        var block=new Part.Link(h.getLevel().dimension.getType().getRegistryName().toString(),f.chest().getBlockPos(),EnumFacing.UP,null,null);
        net.foundations.pl4.compat.PortAssertions.check(ComponentLinks.add(f.player(),f.host(),array,block),"Array accepts a permitted loaded block link");
        net.foundations.pl4.compat.PortAssertions.check(!ComponentLinks.add(f.player(),f.host(),array,block)&&array.links.size()==1,"Duplicate links do not consume another Array slot");
        net.foundations.pl4.compat.PortAssertions.check(!ComponentLinks.add(f.player(),f.host(),entity,block),"Entity Node rejects block links");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void componentLinksRejectForeignEmitters(GameTestHelper h){
        var f=storageFixture(h,"PL4-Links-B");Part receiver=new Part(Kind.DATA_RECEIVER,EnumFacing.NORTH,OWNER);f.host().parts.put(receiver.slot(),receiver);
        var remote=host(h,new BlockPos(4,1,1),Kind.DATA_EMITTER,EnumFacing.UP);Part emitter=part(remote,EnumFacing.UP);
        var link=new Part.Link(h.getLevel().dimension.getType().getRegistryName().toString(),remote.getBlockPos(),emitter.face,null,emitter.identity);
        emitter.owner=UUID.randomUUID();net.foundations.pl4.compat.PortAssertions.check(!ComponentLinks.add(f.player(),f.host(),receiver,link),"Foreign emitter never grants a wireless network edge");
        emitter.owner=OWNER;net.foundations.pl4.compat.PortAssertions.check(ComponentLinks.add(f.player(),f.host(),receiver,link),"Owned matching emitter can be selected");h.succeed();
    }

}
