package net.foundations.pl4;

import java.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.foundations.pl4.compat.scenarios.GameTest;
import net.foundations.pl4.compat.scenarios.GameTestHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate;

/** Native fixtures: separate from the executed, dependency-free planner tests. */
@PrefixGameTestTemplate(false)
public final class R7GameTests {
    private static final UUID OWNER=UUID.fromString("77770000-0000-0000-0000-000000000007");
    private static HostEntity host(GameTestHelper h,BlockPos pos){h.setBlock(pos,FoundationsPL4.HOST.get());return (HostEntity)h.getBlockEntity(pos);}
    private static Part put(HostEntity host,Kind kind,EnumFacing face){Part p=new Part(kind,face,OWNER);p.displayOutward=true;host.parts.put(p.slot(),p);host.changed();return p;}
    private static void chest(GameTestHelper h,BlockPos pos,int count){h.setBlock(pos,Blocks.CHEST);((TileEntityChest)h.getBlockEntity(pos)).setItem(0,new ItemStack(Items.DIAMOND,count));}
    private static boolean amount(Part p,int count){return p.rows.stream().anyMatch(r->r.key().equals("minecraft:diamond")&&r.value()==count);}
    private static void compact(GameTestHelper h,EnumFacing face,boolean external){
        BlockPos pos=new BlockPos(3,3,3);var target=host(h,pos);
        var reader=put(target,Kind.INVENTORY_READER,face);Part panel=new Part(Kind.LARGE_DISPLAY,face,OWNER);panel.displayOutward=true;
        net.foundations.pl4.compat.PortAssertions.check(HostBlock.canAdd(target,panel),"Reader and display need independent same-face slots and non-overlapping paired geometry");
        target.parts.put(panel.slot(),panel);target.changed();
        var input=external?host(h,pos.relative(face.getOpposite())):target;
        put(input,Kind.DATA_CABLE,EnumFacing.DOWN);put(input,Kind.NODE,face.getOpposite());
        chest(h,external?pos.relative(face.getOpposite(),2):pos.relative(face.getOpposite()),17);
        NetworkEngine.ensureCurrent(h.getLevel().getServer());
        net.foundations.pl4.compat.PortAssertions.check(target.readerHasDisplay(reader),"Paired reader skin must activate");
        net.foundations.pl4.compat.PortAssertions.check(!target.externalLead(reader),"Empty endpoint centre must never generate a free cable lead");
        net.foundations.pl4.compat.PortAssertions.check(external?!target.parts.containsKey(6):target.parts.containsKey(6),"Bare endpoint must not acquire an item/cable silently");
        h.runAtTickTime(45,()->{
            if(external){
                net.foundations.pl4.compat.PortAssertions.check(!amount(reader,17)&&!amount(panel,17),"Missing local cable must leave reader disconnected on "+face);
                put(target,Kind.DATA_CABLE,EnumFacing.DOWN);
                h.runAtTickTime(85,()->{
                    net.foundations.pl4.compat.PortAssertions.check(amount(reader,17)&&amount(panel,17),"Placing the missing cable must complete the run on "+face);
                    h.succeed();
                });
            }else{
                net.foundations.pl4.compat.PortAssertions.check(amount(reader,17)&&amount(panel,17),"Compact pair must receive Node inventory on "+face);
                h.succeed();
            }
        });
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void compactDown(GameTestHelper h){compact(h,EnumFacing.DOWN,false);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void compactUp(GameTestHelper h){compact(h,EnumFacing.UP,false);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void compactNorth(GameTestHelper h){compact(h,EnumFacing.NORTH,false);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void compactSouth(GameTestHelper h){compact(h,EnumFacing.SOUTH,false);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void compactWest(GameTestHelper h){compact(h,EnumFacing.WEST,false);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void compactEast(GameTestHelper h){compact(h,EnumFacing.EAST,false);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void bareRearDown(GameTestHelper h){compact(h,EnumFacing.DOWN,true);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void bareRearUp(GameTestHelper h){compact(h,EnumFacing.UP,true);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void bareRearNorth(GameTestHelper h){compact(h,EnumFacing.NORTH,true);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void bareRearSouth(GameTestHelper h){compact(h,EnumFacing.SOUTH,true);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void bareRearWest(GameTestHelper h){compact(h,EnumFacing.WEST,true);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void bareRearEast(GameTestHelper h){compact(h,EnumFacing.EAST,true);}

    private static void isolated(GameTestHelper h,EnumFacing front){
        BlockPos pos=new BlockPos(3,3,3);var input=host(h,pos);put(input,Kind.DATA_CABLE,EnumFacing.DOWN);
        Part reader=put(input,Kind.INVENTORY_READER,front),source=put(input,Kind.TRANSFER_NODE,front.getOpposite());source.transferMode=2;
        chest(h,pos.relative(front.getOpposite()),17);
        BlockPos outputPos=pos.relative(front);var output=host(h,outputPos);put(output,Kind.DATA_CABLE,EnumFacing.DOWN);
        EnumFacing side=front.getAxis()==EnumFacing.Axis.Y?EnumFacing.EAST:EnumFacing.UP;
        EnumFacing readerSide=front.getAxis()==EnumFacing.Axis.Z?EnumFacing.EAST:EnumFacing.NORTH;
        Part destination=put(output,Kind.TRANSFER_NODE,side);destination.transferMode=1;
        Part secondReader=put(output,Kind.INVENTORY_READER,readerSide);
        Part display=put(output,Kind.DISPLAY,side.getOpposite());display.selected=reader.identity.toString();
        chest(h,outputPos.relative(side),29);input.changed();output.changed();
        h.runAtTickTime(45,()->{
            net.foundations.pl4.compat.PortAssertions.check(amount(reader,17)&&amount(secondReader,29),"Visual port cannot merge source and destination inventories");
            net.foundations.pl4.compat.PortAssertions.check(amount(display,17),"Remote screen must see the reader through its visual output");
            net.foundations.pl4.compat.PortAssertions.check(source.pendingItem.isEmpty(),"Visual-only link must not authorize transfer extraction");
            h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void visualIsolationDown(GameTestHelper h){isolated(h,EnumFacing.DOWN);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void visualIsolationUp(GameTestHelper h){isolated(h,EnumFacing.UP);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void visualIsolationNorth(GameTestHelper h){isolated(h,EnumFacing.NORTH);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void visualIsolationSouth(GameTestHelper h){isolated(h,EnumFacing.SOUTH);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void visualIsolationWest(GameTestHelper h){isolated(h,EnumFacing.WEST);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=100) public static void visualIsolationEast(GameTestHelper h){isolated(h,EnumFacing.EAST);}

    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void r6SaveReindexesDisplaysWithoutLosingState(GameTestHelper h){
        var target=host(h,new BlockPos(2,2,2));Part reader=new Part(Kind.ENERGY_READER,EnumFacing.DOWN,OWNER);reader.energySystem="EU";
        Part screen=new Part(Kind.LARGE_DISPLAY,EnumFacing.NORTH,OWNER);screen.label="Saved display";screen.selected=reader.identity.toString();screen.displayOutward=true;
        screen.elements.add(new Part.Element("EU","","storage:eu",3,7,0xABCDE0,false));
        NBTTagList parts=new NBTTagList();parts.add(reader.save(null,false));parts.add(screen.save(null,false));
        NBTTagCompound old=new NBTTagCompound();old.putInt("schema",1);old.put("parts",parts);target.loadAdditional(old,null);
        Part loaded=target.parts.get(9);
        net.foundations.pl4.compat.PortAssertions.check(target.parts.get(0).energySystem.equals("EU")&&!target.parts.containsKey(2),"Only display indices migrate");
        net.foundations.pl4.compat.PortAssertions.check(loaded.identity.equals(screen.identity)&&loaded.elements.equals(screen.elements)&&loaded.displayOutward&&loaded.selected.equals(screen.selected),"All R6 identity/front/reader/layout data survives");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void codecPreservesAllThirteenSlots(GameTestHelper h){
        var target=host(h,new BlockPos(2,2,2));put(target,Kind.DATA_CABLE,EnumFacing.DOWN);
        for(EnumFacing f:EnumFacing.values()){put(target,Kind.INVENTORY_READER,f);put(target,Kind.MINI_DISPLAY,f);}
        NBTTagCompound saved=new NBTTagCompound();target.saveAdditional(saved,null);
        var loaded=new HostEntity(target.getBlockPos(),target.getBlockState());loaded.loadAdditional(saved,null);
        net.foundations.pl4.compat.PortAssertions.check(loaded.parts.size()==13,"NBT loading must not truncate at the former seven-part limit");
        for(var e:target.parts.entrySet())net.foundations.pl4.compat.PortAssertions.check(loaded.parts.get(e.getKey()).identity.equals(e.getValue().identity),"Slot/identity round trip");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void pairedGeometryAndHitSelection(GameTestHelper h){
        var target=host(h,new BlockPos(3,3,3));
        for(EnumFacing face:EnumFacing.values()){
            target.parts.clear();Part reader=put(target,Kind.INVENTORY_READER,face);Part panel=new Part(Kind.DISPLAY,face,OWNER);
            net.foundations.pl4.compat.PortAssertions.check(HostBlock.canAdd(target,panel),"Thin screen fits the original ReaderWithDisplay model");target.parts.put(panel.slot(),panel);target.changed();
            net.foundations.pl4.compat.PortAssertions.check(!VoxelShapes.joinIsNotEmpty(MultipartShapes.part(target.parts.values(),reader),MultipartShapes.part(target.parts.values(),panel),IBooleanFunction.AND),"No reader/display collision overlap");
            var point=net.foundations.pl4.compat.PortVectors.atCenterOf(target.getBlockPos()).add(face.getStepX()*.4999,face.getStepY()*.4999,face.getStepZ()*.4999);
            var hit=new RayTraceResult(point,face,target.getBlockPos(),false);
            net.foundations.pl4.compat.PortAssertions.check(target.hit(hit)==panel&&target.interactionTarget(hit,true)==reader,"Front click hits screen; empty-hand sneak click addresses covered reader");
            net.foundations.pl4.compat.PortAssertions.check(!HostBlock.canAdd(target,new Part(Kind.MINI_DISPLAY,face,OWNER)),"Two displays cannot claim one display slot");
            target.parts.remove(panel.slot());target.changed();net.foundations.pl4.compat.PortAssertions.check(!target.readerHasDisplay(reader),"Reader skin restores when screen is removed");
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=120)
    public static void remoteDisplayDisconnectClearsRows(GameTestHelper h){
        var input=host(h,new BlockPos(2,2,2));put(input,Kind.DATA_CABLE,EnumFacing.DOWN);Part reader=put(input,Kind.INVENTORY_READER,EnumFacing.EAST);put(input,Kind.NODE,EnumFacing.WEST);chest(h,new BlockPos(1,2,2),17);
        var output=host(h,new BlockPos(3,2,2));Part cable=put(output,Kind.DATA_CABLE,EnumFacing.DOWN);Part panel=put(output,Kind.LARGE_DISPLAY,EnumFacing.NORTH);
        h.runAtTickTime(40,()->{net.foundations.pl4.compat.PortAssertions.check(amount(panel,17),"Remote output initially readable");cable.blockedFaces=1<<EnumFacing.WEST.ordinal();output.changed();});
        h.runAtTickTime(80,()->{net.foundations.pl4.compat.PortAssertions.check(panel.rows.isEmpty(),"Disabled visual input must clear stale data, not freeze the last good reading");h.succeed();});
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void panelItemStaysOnReaderAndDuplicateDoesNotConsume(GameTestHelper h){
        var target=host(h,new BlockPos(3,3,3));Part reader=put(target,Kind.INVENTORY_READER,EnumFacing.EAST);put(target,Kind.DATA_CABLE,EnumFacing.DOWN);
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-R7-Test"));
        player.inventory.clearContent();player.abilities.instabuild=false;
        Vec3d front=net.foundations.pl4.compat.PortVectors.atCenterOf(target.getBlockPos()).add(.5,0,0);player.setPos(front.x+1,front.y,front.z);
        ItemStack stack=new ItemStack(FoundationsPL4.PART_ITEMS.get(Kind.DISPLAY).get(),2);player.setItemInHand(net.minecraft.util.EnumHand.MAIN_HAND,stack);
        var hit=new RayTraceResult(front,EnumFacing.EAST,target.getBlockPos(),false);
        var context=new net.minecraft.item.ItemUseContext(player,net.minecraft.util.EnumHand.MAIN_HAND,hit);
        stack.getItem().useOn(context);
        net.foundations.pl4.compat.PortAssertions.check(target.parts.containsKey(12)&&target.parts.get(12).displayOutward&&stack.getCount()==1,"Display item must occupy same host/front, not move into adjacent air");
        net.foundations.pl4.compat.PortAssertions.check(h.getLevel().isEmptyBlock(target.getBlockPos().east()),"No adjacent host or extra cable branch created");
        // Explicit reader front target when occupied must not consume or replace the existing panel.
        Part duplicate=new Part(Kind.MINI_DISPLAY,EnumFacing.EAST,OWNER);
        net.foundations.pl4.compat.PortAssertions.check(!HostBlock.canAdd(target,duplicate),"Display layer occupancy is exclusive");
        net.foundations.pl4.compat.PortAssertions.check(target.parts.get(5)==reader,"Pairing cannot replace the reader");h.succeed();
    }

}
