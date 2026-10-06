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
    public static void passiveNodeFeedsAddTransferNode(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(add,Direction.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==17,"Normal Node must feed an ADD Transfer Node");h.succeed();
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
        var restored=Part.load(source.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
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

}
