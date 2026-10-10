package net.foundations.pl4;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.foundations.pl4.compat.scenarios.GameTest;
import net.foundations.pl4.compat.scenarios.GameTestHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumActionResult;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.item.ItemUseContext;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.*;
import net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate;

/** Native server acceptance cases. Added in R5; NOT executed in the offline packaging environment. */
@PrefixGameTestTemplate(false)
public final class R5GameTests {
    private static final UUID OWNER=UUID.fromString("bbbb0000-0000-0000-0000-000000000005");
    private static HostEntity host(GameTestHelper h,BlockPos p,Kind kind,EnumFacing face,boolean cable){
        h.setBlock(p,FoundationsPL4.HOST.get());var host=(HostEntity)h.getBlockEntity(p);
        host.parts.put(net.foundations.pl4.core.MultipartTopology.slot(kind,face.ordinal()),new Part(kind,face,OWNER));
        if(cable&&!kind.cable())host.parts.put(6,new Part(kind.redstone()?Kind.REDSTONE_CABLE:Kind.DATA_CABLE,EnumFacing.DOWN,OWNER));
        host.changed();return host;
    }
    private static void rebuild(GameTestHelper h){NetworkEngine.rebuild(h.getLevel().getServer());}
    private static void chest(GameTestHelper h,BlockPos pos){h.setBlock(pos,Blocks.CHEST);((TileEntityChest)h.getBlockEntity(pos)).setItem(0,new ItemStack(Items.DIAMOND,17));}
    private static FakePlayer player(GameTestHelper h,BlockPos pos){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(OWNER,"PL4-R5-Test"));p.inventory.clearContent();p.abilities.instabuild=false;
        var absolute=h.absolutePos(pos);p.setPos(absolute.getX()+.5,absolute.getY(),absolute.getZ()+.5);return p;
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void cableArmsRespectDomains(GameTestHelper h){
        var a=host(h,new BlockPos(2,1,2),Kind.DATA_CABLE,EnumFacing.DOWN,false);
        var b=host(h,new BlockPos(3,1,2),Kind.DATA_CABLE,EnumFacing.DOWN,false);rebuild(h);
        net.foundations.pl4.compat.PortAssertions.check(a.connection(EnumFacing.EAST)==1&&b.connection(EnumFacing.WEST)==1,"Both cable ends need real external arms");
        b.parts.put(6,new Part(Kind.REDSTONE_CABLE,EnumFacing.DOWN,OWNER));b.changed();rebuild(h);
        net.foundations.pl4.compat.PortAssertions.check(a.connection(EnumFacing.EAST)==0&&b.connection(EnumFacing.WEST)==0,"Data and redstone cable families cannot connect");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100)
    public static void uncabledFaceDevicesDoNotRelay(GameTestHelper h){
        chest(h,new BlockPos(1,1,1));host(h,new BlockPos(2,1,1),Kind.NODE,EnumFacing.WEST,false);
        var r=host(h,new BlockPos(3,1,1),Kind.INVENTORY_READER,EnumFacing.DOWN,false);
        h.runAtTickTime(45,()->{net.foundations.pl4.compat.PortAssertions.check(r.parts.get(0).rows.isEmpty(),"Touching devices are not an implicit wired network");h.succeed();});
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=120)
    public static void disabledPortSplitsLiveNetwork(GameTestHelper h){
        chest(h,new BlockPos(1,1,1));host(h,new BlockPos(2,1,1),Kind.NODE,EnumFacing.WEST,true);
        var bridge=host(h,new BlockPos(3,1,1),Kind.DATA_CABLE,EnumFacing.DOWN,false);
        var reader=host(h,new BlockPos(4,1,1),Kind.INVENTORY_READER,EnumFacing.DOWN,true).parts.get(0);
        h.runAtTickTime(45,()->{
            net.foundations.pl4.compat.PortAssertions.check(reader.rows.stream().anyMatch(r->r.value()==17),"Fixture must first carry live data");
            bridge.parts.get(6).blockedFaces=1<<EnumFacing.EAST.ordinal();bridge.changed();
        });
        h.runAtTickTime(85,()->{net.foundations.pl4.compat.PortAssertions.check(reader.rows.isEmpty()&&bridge.connection(EnumFacing.EAST)==0,"Disabled port must remove data and the visual arm");h.succeed();});
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void collisionAndOutlineFollowConnections(GameTestHelper h){
        var a=host(h,new BlockPos(2,1,2),Kind.DATA_CABLE,EnumFacing.DOWN,false);rebuild(h);
        net.foundations.pl4.compat.PortAssertions.check(Math.abs(a.outline().bounds().maxX-.625)<.0001,"Isolated cable is not a full host cube");
        var b=host(h,new BlockPos(3,1,2),Kind.DATA_CABLE,EnumFacing.DOWN,false);rebuild(h);
        net.foundations.pl4.compat.PortAssertions.check(a.outline().bounds().maxX==1,"Connected arm must be included in picking/collision");
        h.getLevel().removeBlock(b.getBlockPos(),false);rebuild(h);
        net.foundations.pl4.compat.PortAssertions.check(Math.abs(a.outline().bounds().maxX-.625)<.0001,"Removed arm must leave no stale collision");
        var sideHit=new RayTraceResult(net.foundations.pl4.compat.PortVectors.atLowerCornerOf(a.getBlockPos()).add(.95,.5,.5),EnumFacing.UP,a.getBlockPos(),false);
        net.foundations.pl4.compat.PortAssertions.check(a.cableDirection(sideHit)==EnumFacing.EAST,"Clicking a rod side must select that rod, not its surface normal");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void partPlacementBypassesReaderGui(GameTestHelper h){
        var a=host(h,new BlockPos(2,1,2),Kind.DATA_CABLE,EnumFacing.DOWN,false);var player=player(h,new BlockPos(2,1,3));
        ItemStack stack=new ItemStack(FoundationsPL4.PART_ITEMS.get(Kind.NODE).get(),2);player.setItemInHand(EnumHand.MAIN_HAND,stack);
        var hit=new RayTraceResult(net.foundations.pl4.compat.PortVectors.atLowerCornerOf(a.getBlockPos()).add(.5,.625,.5),EnumFacing.UP,a.getBlockPos(),false);
        var result=FoundationsPL4.HOST.get().use(a.getBlockState(),h.getLevel(),a.getBlockPos(),player,EnumHand.MAIN_HAND,hit);
        net.foundations.pl4.compat.PortAssertions.check(!result,"Held parts must not be swallowed by the host GUI");
        stack.getItem().useOn(new ItemUseContext(player,EnumHand.MAIN_HAND,hit));
        net.foundations.pl4.compat.PortAssertions.check(a.parts.containsKey(EnumFacing.UP.ordinal())&&a.parts.containsKey(6)&&stack.getCount()==1,"Part must attach to the clicked cable host and consume exactly one item");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=20)
    public static void cablePlacementFillsMissingEndpointCell(GameTestHelper h){
        var a=host(h,new BlockPos(2,1,2),Kind.DATA_CABLE,EnumFacing.DOWN,false);
        var endpoint=host(h,new BlockPos(3,1,2),Kind.INVENTORY_READER,EnumFacing.EAST,false);rebuild(h);
        net.foundations.pl4.compat.PortAssertions.check(a.connection(EnumFacing.EAST)==0&&!endpoint.parts.containsKey(6),"Unplaced endpoint centre must not gain a cable arm");
        long builds=NetworkEngine.topologyBuildCount();
        var player=player(h,new BlockPos(2,1,3));
        ItemStack stack=new ItemStack(FoundationsPL4.PART_ITEMS.get(Kind.DATA_CABLE).get());
        player.setItemInHand(EnumHand.MAIN_HAND,stack);
        var hit=new RayTraceResult(net.foundations.pl4.compat.PortVectors.atLowerCornerOf(a.getBlockPos()).add(.5625,.5,.5),EnumFacing.EAST,a.getBlockPos(),false);
        stack.getItem().useOn(new ItemUseContext(player,EnumHand.MAIN_HAND,hit));
        net.foundations.pl4.compat.PortAssertions.check(endpoint.parts.containsKey(6)&&stack.isEmpty(),"Clicking the cable east face must fill the endpoint cell and consume exactly one cable");
        net.foundations.pl4.compat.PortAssertions.check(NetworkEngine.topologyBuildCount()==builds,"Cable placement must not synchronously rebuild the entire loaded network");
        net.foundations.pl4.compat.PortAssertions.check(a.connection(EnumFacing.EAST)==1&&endpoint.connection(EnumFacing.WEST)==1,"The placed cable must publish its local connection immediately");
        h.runAtTickTime(4,()->{net.foundations.pl4.compat.PortAssertions.check(NetworkEngine.topologyBuildCount()>builds,"The authoritative full network rebuild must follow on the next tick");h.succeed();});
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void unchangedTopologyIsReused(GameTestHelper h){
        host(h,new BlockPos(2,1,2),Kind.DATA_CABLE,EnumFacing.DOWN,false);NetworkEngine.ensureCurrent(h.getLevel().getServer());long count=NetworkEngine.topologyBuildCount();
        NetworkEngine.ensureCurrent(h.getLevel().getServer());NetworkEngine.ensureCurrent(h.getLevel().getServer());
        net.foundations.pl4.compat.PortAssertions.check(count==NetworkEngine.topologyBuildCount(),"Unchanged graph must not be rebuilt on every access");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=120)
    public static void topologyRebuildRefreshesCachedTargetPriority(GameTestHelper h){
        BlockPos leftChest=new BlockPos(2,1,2),rightChest=new BlockPos(4,1,2),hostPos=new BlockPos(3,1,2);
        chest(h,leftChest);h.setBlock(rightChest,Blocks.CHEST);((TileEntityChest)h.getBlockEntity(rightChest)).setItem(0,new ItemStack(Items.STONE,5));
        HostEntity host=host(h,hostPos,Kind.NODE,EnumFacing.WEST,true);
        Part left=host.parts.get(EnumFacing.WEST.ordinal()),right=new Part(Kind.NODE,EnumFacing.EAST,OWNER),reader=new Part(Kind.INVENTORY_READER,EnumFacing.DOWN,OWNER);
        left.priority=10;right.priority=0;reader.mode="CHANNEL";reader.index=1;
        host.parts.put(right.slot(),right);host.parts.put(reader.slot(),reader);host.changed();
        h.runAtTickTime(45,()->{
            net.foundations.pl4.compat.PortAssertions.check(reader.rows.stream().anyMatch(r->r.itemId().equals("minecraft:diamond")&&r.value()==17),"Cached priority order selects the higher-priority first target");
            left.priority=-1;right.priority=10;host.changed();
        });
        h.runAtTickTime(85,()->{
            net.foundations.pl4.compat.PortAssertions.check(reader.rows.stream().anyMatch(r->r.itemId().equals("minecraft:stone")&&r.value()==5),"A priority edit must invalidate and refresh cached target order");
            h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void cablePortStateRoundTrips(GameTestHelper h){
        Part cable=new Part(Kind.DATA_CABLE,EnumFacing.DOWN,OWNER);cable.blockedFaces=42;
        var loaded=Part.load(cable.save(null,false),null);
        net.foundations.pl4.compat.PortAssertions.check(loaded.blockedFaces==42&&loaded.identity.equals(cable.identity),"Port mask and part identity must persist");
        var old=cable.save(null,false);old.remove("blockedFaces");
        net.foundations.pl4.compat.PortAssertions.check(Part.load(old,null).blockedFaces==0,"R3/R4 cables default to all ports enabled");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void obstructedLegacyHammerDoesNotOverwriteBlocks(GameTestHelper h){
        BlockPos pos=new BlockPos(2,1,2);h.setBlock(pos.above(2),Blocks.STONE);h.setBlock(pos,FoundationsPL4.HAMMER.get());
        var hammer=(HammerEntity)h.getBlockEntity(pos);hammer.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND,17));hammer.progress=7;
        for(int i=0;i<30;i++)HammerEntity.tick(h.getLevel(),hammer.getBlockPos(),hammer.getBlockState(),hammer);
        net.foundations.pl4.compat.PortAssertions.check((h.getLevel().getBlockState(h.absolutePos(pos.above(2))).getBlock()==Blocks.STONE)&&h.getLevel().isEmptyBlock(h.absolutePos(pos.above())),"Check both upper cells before changing either");
        net.foundations.pl4.compat.PortAssertions.check(hammer.progress==7&&hammer.inventory.getStackInSlot(0).getCount()==17,"Blocked old hammer must preserve its state and inventory");
        h.setBlock(pos.above(2),Blocks.AIR);net.foundations.pl4.compat.PortAssertions.check(HammerStructure.ensure(h.getLevel(),hammer.getBlockPos()),"Clearing headroom permits safe structure creation");
        net.foundations.pl4.compat.PortAssertions.check(HammerStructure.complete(h.getLevel(),hammer.getBlockPos()),"Exactly two owned upper cells must exist");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void breakingHammerUpperDropsOnce(GameTestHelper h){
        BlockPos pos=new BlockPos(2,1,2);h.setBlock(pos,FoundationsPL4.HAMMER.get());var hammer=(HammerEntity)h.getBlockEntity(pos);
        hammer.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND,17));h.getLevel().destroyBlock(hammer.getBlockPos().above(2),true);
        for(int i=0;i<3;i++)net.foundations.pl4.compat.PortAssertions.check(h.getLevel().isEmptyBlock(h.absolutePos(pos.above(i))),"Breaking the top must remove all three machine cells");
        var items=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AxisAlignedBB(h.absolutePos(pos)).inflate(2));
        int machines=items.stream().filter(e->(e.getItem().getItem()==FoundationsPL4.HAMMER.get().asItem())).mapToInt(e->e.getItem().getCount()).sum();
        int diamonds=items.stream().filter(e->(e.getItem().getItem()==Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
        net.foundations.pl4.compat.PortAssertions.check(machines==1&&diamonds==17,"Upper blocks must not duplicate the base item or inventory");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void hammerMenuSlotsShiftClickAndDistance(GameTestHelper h){
        BlockPos pos=new BlockPos(2,1,2);h.setBlock(pos,FoundationsPL4.HAMMER.get());var hammer=(HammerEntity)h.getBlockEntity(pos);var player=player(h,new BlockPos(2,1,3));
        var menu=(HammerMenu)hammer.createMenu(1,player.inventory,player);
        net.foundations.pl4.compat.PortAssertions.check(menu.slots.size()==38&&menu.slots.get(0).x==53&&menu.slots.get(0).y==24&&menu.slots.get(1).x==107,"Original container slot layout");
        net.foundations.pl4.compat.PortAssertions.check(!menu.slots.get(1).mayPlace(new ItemStack(Items.DIAMOND)),"Output slot is extraction-only");
        player.inventory.setItem(9,new ItemStack(Items.DIAMOND,17));menu.quickMoveStack(player,2);
        net.foundations.pl4.compat.PortAssertions.check(hammer.inventory.getStackInSlot(0).getCount()==17&&player.inventory.getItem(9).isEmpty(),"Shift-click inserts into input exactly once");
        hammer.inventory.setStackInSlot(1,new ItemStack(FoundationsPL4.item("stoneplate"),4));menu.quickMoveStack(player,1);
        int plates=0;for(int i=0;i<player.inventory.getContainerSize();i++)if((player.inventory.getItem(i).getItem()==FoundationsPL4.item("stoneplate")))plates+=player.inventory.getItem(i).getCount();
        net.foundations.pl4.compat.PortAssertions.check(plates==4&&hammer.inventory.getStackInSlot(1).isEmpty(),"Shift-click extracts exactly four plates");
        player.setPos(hammer.getBlockPos().getX()+20,hammer.getBlockPos().getY(),hammer.getBlockPos().getZ());net.foundations.pl4.compat.PortAssertions.check(!menu.stillValid(player),"Out-of-range menus close");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void hammerInventoryAndProgressRoundTrip(GameTestHelper h){
        BlockPos pos=new BlockPos(2,1,2);h.setBlock(pos,FoundationsPL4.HAMMER.get());var hammer=(HammerEntity)h.getBlockEntity(pos);
        hammer.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND,17));hammer.progress=33;hammer.cooldown=9;
        NBTTagCompound saved=new NBTTagCompound();hammer.saveAdditional(saved,null);
        var loaded=new HammerEntity(hammer.getBlockPos(),hammer.getBlockState());loaded.setLevel(h.getLevel());loaded.setPosition(hammer.getBlockPos());loaded.loadAdditional(saved,null);
        net.foundations.pl4.compat.PortAssertions.check(loaded.progress==33&&loaded.cooldown==9&&loaded.inventory.getStackInSlot(0).getCount()==17,"Existing hammer state must survive save/load");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void joinedDisplaysPreserveSharedLayoutWhenSplit(GameTestHelper h){
        var a=host(h,new BlockPos(2,2,2),Kind.LARGE_DISPLAY,EnumFacing.NORTH,false);var b=host(h,new BlockPos(3,2,2),Kind.LARGE_DISPLAY,EnumFacing.NORTH,false);
        Part left=a.parts.get(9),right=b.parts.get(9);left.label="Left saved layout";right.label="Right saved layout";rebuild(h);
        net.foundations.pl4.compat.PortAssertions.check(left.canvasWidth==2&&left.canvasMask==2&&right.canvasColumn==1&&right.canvasMask==1,"Adjacent same-plane screens share one rectangular canvas");
        net.foundations.pl4.compat.PortAssertions.check(DisplayNetworks.controller(b,right).part()==left,"Any tile must resolve the same editor controller");
        h.getLevel().removeBlock(a.getBlockPos(),false);rebuild(h);
        net.foundations.pl4.compat.PortAssertions.check(right.canvasWidth==1&&right.canvasColumn==0&&right.label.equals("Left saved layout"),"R8 split retains the active shared layout mirrored to the surviving tile");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void irregularOrDifferentOwnerScreensStayIndependent(GameTestHelper h){
        var a=host(h,new BlockPos(2,2,2),Kind.LARGE_DISPLAY,EnumFacing.NORTH,false);var b=host(h,new BlockPos(3,2,2),Kind.LARGE_DISPLAY,EnumFacing.NORTH,false);
        var c=host(h,new BlockPos(2,3,2),Kind.LARGE_DISPLAY,EnumFacing.NORTH,false);rebuild(h);
        net.foundations.pl4.compat.PortAssertions.check(a.parts.get(9).canvasWidth==1&&b.parts.get(9).canvasWidth==1&&c.parts.get(9).canvasWidth==1,"Incomplete rectangles must not create a stretched or hidden canvas");
        h.getLevel().removeBlock(c.getBlockPos(),false);b.parts.get(9).owner=UUID.randomUUID();b.changed();rebuild(h);
        net.foundations.pl4.compat.PortAssertions.check(a.parts.get(9).canvasWidth==1&&b.parts.get(9).canvasWidth==1,"Different owners must not be joined");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100)
    public static void anyCanvasTileCanSupplyAReader(GameTestHelper h){
        var a=host(h,new BlockPos(3,1,2),Kind.LARGE_DISPLAY,EnumFacing.DOWN,false);
        host(h,new BlockPos(4,1,2),Kind.LARGE_DISPLAY,EnumFacing.DOWN,true);
        host(h,new BlockPos(4,1,3),Kind.NODE,EnumFacing.EAST,true);chest(h,new BlockPos(5,1,3));
        host(h,new BlockPos(4,1,4),Kind.INVENTORY_READER,EnumFacing.DOWN,true);
        h.runAtTickTime(45,()->{net.foundations.pl4.compat.PortAssertions.check(a.parts.get(7).canvasWidth==2&&a.parts.get(7).rows.stream().anyMatch(r->r.value()==17),"Shared controller must receive data from a reader connected to another tile");h.succeed();});
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100)
    public static void displayJoiningNeverBridgesWiredPartitions(GameTestHelper h){
        var reader=host(h,new BlockPos(2,1,2),Kind.INVENTORY_READER,EnumFacing.DOWN,true).parts.get(0);
        HostEntity a=host(h,new BlockPos(3,1,2),Kind.LARGE_DISPLAY,EnumFacing.DOWN,true),b=host(h,new BlockPos(4,1,2),Kind.LARGE_DISPLAY,EnumFacing.DOWN,true);
        a.parts.get(6).blockedFaces=1<<EnumFacing.EAST.ordinal();b.parts.get(6).blockedFaces=1<<EnumFacing.WEST.ordinal();
        b.parts.put(EnumFacing.EAST.ordinal(),new Part(Kind.NODE,EnumFacing.EAST,OWNER));a.changed();b.changed();chest(h,new BlockPos(5,1,2));
        h.runAtTickTime(45,()->{net.foundations.pl4.compat.PortAssertions.check(a.parts.get(7).canvasWidth==2&&reader.rows.isEmpty(),"A shared screen must not bypass a disconnected data cable");h.succeed();});
    }
}
