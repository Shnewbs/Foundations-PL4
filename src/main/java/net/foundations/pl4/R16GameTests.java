package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;


/** Native item-path acceptance for the R16 passive-node transfer pool. */

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

    @PortGameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void passiveNodeFeedsAddTransferNode(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(add,Direction.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==17,"Normal Node must feed an ADD Transfer Node");h.succeed();
    }

    @PortGameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void removeTransferNodeFeedsPassiveNode(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.EMERALD,13));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),node=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);part(remove,Direction.WEST).transferMode=2;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==13,"REMOVE Transfer Node must feed a normal Node endpoint");h.succeed();
    }

    @PortGameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void explicitAddBeatsPassiveNodeOnTie(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),explicit=chest(h,new BlockPos(5,1,1)),passive=chest(h,new BlockPos(8,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST),node=host(h,new BlockPos(7,1,1),Kind.NODE,Direction.EAST);
        part(remove,Direction.WEST).transferMode=2;part(add,Direction.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST),ref(add,Direction.EAST)));
        h.assertTrue(explicit.getItem(0).getCount()==17&&passive.getItem(0).isEmpty(),"Explicit ADD peer must win an equal-priority passive endpoint");h.succeed();
    }

    @PortGameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void fullPassiveDestinationDoesNotExtract(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));for(int i=0;i<to.getContainerSize();i++)to.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),node=host(h,new BlockPos(4,1,1),Kind.NODE,Direction.EAST);part(remove,Direction.WEST).transferMode=2;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(node,Direction.EAST)));
        h.assertTrue(from.getItem(0).getCount()==17&&part(remove,Direction.WEST).pendingItem.isEmpty(),"Full passive endpoint must not extract or escrow source items");h.succeed();
    }

    @PortGameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void addFilterAppliesToPassiveSource(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.IRON_INGOT,8));from.setItem(1,new ItemStack(Items.DIAMOND,7));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);Part p=part(add,Direction.EAST);p.transferMode=1;p.filter="minecraft:diamond";
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        h.assertTrue(from.getItem(0).getCount()==8&&from.getItem(1).isEmpty()&&to.getItem(0).is(Items.DIAMOND)&&to.getItem(0).getCount()==7,"ADD filter must constrain passive source imports");h.succeed();
    }

    @PortGameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void bidirectionalDoesNotBlindlyUsePassivePool(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,9));
        HostEntity node=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST),both=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(both,Direction.EAST).transferMode=3;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(both,Direction.EAST)));
        h.assertTrue(from.getItem(0).getCount()==9&&to.getItem(0).isEmpty(),"ADD/REMOVE must be peer-only until directional filters/channels exist");h.succeed();
    }

    @PortGameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void bidirectionalCanFeedExplicitAddPeer(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.GOLD_INGOT,12));
        HostEntity both=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),add=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(both,Direction.WEST).transferMode=3;part(add,Direction.EAST).transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(both,Direction.WEST),ref(add,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==12,"ADD/REMOVE may act as source for an explicit ADD peer");h.succeed();
    }

    @PortGameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void removeCanFeedBidirectionalExplicitPeer(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.REDSTONE,21));
        HostEntity remove=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),both=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);part(remove,Direction.WEST).transferMode=2;part(both,Direction.EAST).transferMode=3;
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(remove,Direction.WEST),ref(both,Direction.EAST)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).getCount()==21,"REMOVE may feed an explicit ADD/REMOVE peer");h.succeed();
    }
}
