package net.foundations.pl4;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.foundations.pl4.compat.scenarios.GameTest;
import net.foundations.pl4.compat.scenarios.GameTestHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResultType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.util.*;
import net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate;

/** Native server fixtures. These require an actual Minecraft/NeoForge build and are NOT offline passes. */
@PrefixGameTestTemplate(false)
public final class R8GameTests {
    private static final UUID OWNER=UUID.fromString("aaaa0000-0000-0000-0000-000000000008");
    private static HostEntity panel(GameTestHelper h,BlockPos position,Direction face){
        h.setBlock(position,FoundationsPL4.HOST.get());var host=(HostEntity)h.getBlockEntity(position);
        var part=new Part(Kind.LARGE_DISPLAY,face,OWNER);part.displayOutward=true;host.parts.put(part.slot(),part);host.changed();return host;
    }
    private static Part part(HostEntity host){return host.parts.values().stream().filter(p->p.kind==Kind.LARGE_DISPLAY).findFirst().orElseThrow();}
    private static FakePlayer player(GameTestHelper h,HostEntity at,ItemStack stack){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(OWNER,"PL4-R8-Test"));p.inventory.clearContent();p.abilities.instabuild=false;p.setShiftKeyDown(false);
        var centre=net.minecraft.util.math.vector.Vector3d.atCenterOf(at.getBlockPos());p.setPos(centre.x+1,centre.y,centre.z+1);p.setItemInHand(Hand.MAIN_HAND,stack);return p;
    }
    private static void configure(Part p){p.label="Saved machine board";p.selected="power_main";p.color=0x53AACC;p.elements.add(new Part.Element("EU","","storage:eu",7,19,0xABDEEF,false));p.layoutRevision=20;}
    private static void extend(GameTestHelper h,Direction mount){
        var source=panel(h,new BlockPos(3,3,3),mount);Part original=part(source);configure(original);source.changed();
        for(Direction side:Direction.values())if(side.getAxis()!=mount.getAxis()){
            NetworkEngine.ensureCurrent(h.getLevel().getServer());
            ItemStack stack=new ItemStack(FoundationsPL4.PART_ITEMS.get(Kind.LARGE_DISPLAY).get(),2);var player=player(h,source,stack);
            Vector3d centre=net.minecraft.util.math.vector.Vector3d.atCenterOf(source.getBlockPos());Vector3d hitPoint=centre.add(mount.getStepX()*.4999+side.getStepX()*.5,mount.getStepY()*.4999+side.getStepY()*.5,mount.getStepZ()*.4999+side.getStepZ()*.5);
            var result=stack.getItem().useOn(new ItemUseContext(player,Hand.MAIN_HAND,new BlockRayTraceResult(hitPoint,side,source.getBlockPos(),false)));
            BlockPos destination=source.getBlockPos().relative(side);
            net.foundations.pl4.compat.PortAssertions.check(result.consumesAction()&&h.getLevel().getBlockEntity(destination) instanceof HostEntity,"Side click must create a neighboring host, not a perpendicular face");
            var added=(HostEntity)h.getLevel().getBlockEntity(destination);Part next=part(added);
            net.foundations.pl4.compat.PortAssertions.check(next.face==mount&&next.displayOutward==original.displayOutward&&Objects.equals(next.owner,original.owner),"Mount/front/owner match original");
            net.foundations.pl4.compat.PortAssertions.check(stack.getCount()==1&&original.canvasWidth*original.canvasHeight==2,"Exactly one item consumed and rectangle joins immediately");
            var root=DisplayNetworks.controller(source,original).part();net.foundations.pl4.compat.PortAssertions.check(root.displaySettings().equals(original.displaySettings())&&root.selected.equals("power_main"),"All four growth directions preserve layout");
            h.getLevel().removeBlock(destination,false);NetworkEngine.ensureCurrent(h.getLevel().getServer());
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID) public static void edgeExtensionDown(GameTestHelper h){extend(h,Direction.DOWN);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID) public static void edgeExtensionUp(GameTestHelper h){extend(h,Direction.UP);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID) public static void edgeExtensionNorth(GameTestHelper h){extend(h,Direction.NORTH);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID) public static void edgeExtensionSouth(GameTestHelper h){extend(h,Direction.SOUTH);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID) public static void edgeExtensionWest(GameTestHelper h){extend(h,Direction.WEST);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID) public static void edgeExtensionEast(GameTestHelper h){extend(h,Direction.EAST);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void frontRimAndSavedItemAdoptCanvas(GameTestHelper h){
        var source=panel(h,new BlockPos(3,3,3),Direction.NORTH);configure(part(source));source.changed();
        Part saved=new Part(Kind.LARGE_DISPLAY,Direction.DOWN,OWNER);saved.displayOutward=false;saved.label="Do not override active board";saved.layoutRevision=99;
        ItemStack stack=PartItem.savedStack(saved,h.getLevel().registryAccess());var p=player(h,source,stack);
        Vector3d point=net.minecraft.util.math.vector.Vector3d.atCenterOf(source.getBlockPos()).add(.49,0,-.4999);
        stack.getItem().useOn(new ItemUseContext(p,Hand.MAIN_HAND,new BlockRayTraceResult(point,Direction.NORTH,source.getBlockPos(),false)));
        var next=(HostEntity)h.getLevel().getBlockEntity(source.getBlockPos().east());
        net.foundations.pl4.compat.PortAssertions.check(next!=null&&part(next).face==Direction.NORTH&&part(next).displayOutward,"Front rim extends east in original plane");
        net.foundations.pl4.compat.PortAssertions.check(part(next).selected.equals("power_main")&&part(next).label.equals("Saved machine board"),"Explicit extension uses source settings even for a saved item");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void centreClickDoesNotConsumeOrRotate(GameTestHelper h){
        var source=panel(h,new BlockPos(3,3,3),Direction.NORTH);ItemStack stack=new ItemStack(FoundationsPL4.PART_ITEMS.get(Kind.LARGE_DISPLAY).get(),2);var p=player(h,source,stack);
        var point=net.minecraft.util.math.vector.Vector3d.atCenterOf(source.getBlockPos()).add(0,0,-.4999);
        var result=stack.getItem().useOn(new ItemUseContext(p,Hand.MAIN_HAND,new BlockRayTraceResult(point,Direction.NORTH,source.getBlockPos(),false)));
        net.foundations.pl4.compat.PortAssertions.check(result==ActionResultType.FAIL&&stack.getCount()==2&&h.getLevel().isEmptyBlock(source.getBlockPos().north()),"Ambiguous centre does not create a perpendicular or offset panel");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void occupiedExtensionCannotReplaceOrConsume(GameTestHelper h){
        var source=panel(h,new BlockPos(3,3,3),Direction.NORTH);h.setBlock(new BlockPos(4,3,3),Blocks.DIAMOND_BLOCK);
        ItemStack stack=new ItemStack(FoundationsPL4.PART_ITEMS.get(Kind.LARGE_DISPLAY).get(),2);var p=player(h,source,stack);
        var point=net.minecraft.util.math.vector.Vector3d.atCenterOf(source.getBlockPos()).add(.5,0,-.4999);
        stack.getItem().useOn(new ItemUseContext(p,Hand.MAIN_HAND,new BlockRayTraceResult(point,Direction.EAST,source.getBlockPos(),false)));
        net.foundations.pl4.compat.PortAssertions.check(stack.getCount()==2&&(h.getLevel().getBlockState(source.getBlockPos().east()).getBlock()==Blocks.DIAMOND_BLOCK),"Solid destination remains intact");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void removedControllerKeepsSharedLayout(GameTestHelper h){
        var a=panel(h,new BlockPos(2,2,2),Direction.SOUTH);var b=panel(h,new BlockPos(3,2,2),Direction.SOUTH);configure(part(a));a.changed();
        NetworkEngine.ensureCurrent(h.getLevel().getServer());var settings=part(a).displaySettings();
        net.foundations.pl4.compat.PortAssertions.check(part(b).displaySettings().equals(settings),"Shared settings must be copied to member before removal");
        var single=net.foundations.pl4.core.DynamicCanvasLayout.large(1,1);
        var elements=settings.elements().stream().map(e->new Part.Element(net.foundations.pl4.core.DynamicCanvasLayout.migrate(
            e.spec(),settings.layoutWidth(),settings.layoutHeight(),single.width(),single.height()))).toList();
        var expected=new Part.DisplaySettings(settings.label(),settings.selected(),settings.metric(),settings.color(),elements,
            settings.displayMode(),settings.displayPage(),single.width(),single.height());
        long revision=part(b).layoutRevision;
        if(settings.layoutWidth()!=single.width()||settings.layoutHeight()!=single.height())
            revision=net.foundations.pl4.core.CanvasContinuity.next(revision);
        h.getLevel().removeBlock(a.getBlockPos(),false);NetworkEngine.ensureCurrent(h.getLevel().getServer());
        net.foundations.pl4.compat.PortAssertions.check(part(b).displaySettings().equals(expected)&&part(b).canvasWidth==1&&part(b).layoutRevision==revision,
            "Removing visual top-left keeps settings and proportionally resizes content on survivor");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void editedSharedLayoutSurvivesReload(GameTestHelper h){
        var a=panel(h,new BlockPos(2,2,2),Direction.SOUTH);var b=panel(h,new BlockPos(3,2,2),Direction.SOUTH);configure(part(a));a.changed();NetworkEngine.ensureCurrent(h.getLevel().getServer());
        Part root=DisplayNetworks.controller(a,part(a)).part();root.label="Latest edit";root.selected="inventory_main";DisplayNetworks.layoutEdited(a,root);
        for(var host:List.of(a,b)){
            CompoundNBT saved=new CompoundNBT();host.saveAdditional(saved,h.getLevel().registryAccess());host.loadAdditional(saved,h.getLevel().registryAccess());host.changed();
        }
        NetworkEngine.ensureCurrent(h.getLevel().getServer());
        net.foundations.pl4.compat.PortAssertions.check(part(a).label.equals("Latest edit")&&part(b).selected.equals("inventory_main")&&part(b).layoutRevision>20,"Save reload retains mirrored edit and revision");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void mergeUsesNewestStoredSettings(GameTestHelper h){
        var a=panel(h,new BlockPos(2,2,2),Direction.SOUTH);configure(part(a));var b=panel(h,new BlockPos(3,2,2),Direction.SOUTH);part(b).label="Newer canvas";part(b).layoutRevision=30;b.changed();
        NetworkEngine.ensureCurrent(h.getLevel().getServer());
        net.foundations.pl4.compat.PortAssertions.check(part(a).label.equals("Newer canvas")&&part(b).label.equals("Newer canvas")&&part(a).elements.isEmpty(),"Newer explicit layout (including clear) wins deterministically");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void hologramViewPersistenceAndLegacyDefault(GameTestHelper h){
        for(Kind kind:List.of(Kind.HOLOGRAM,Kind.ADVANCED_HOLOGRAM))for(Direction face:Direction.values()){
            Part p=new Part(kind,face,OWNER);p.hologramView=Direction.WEST.ordinal();p.label="Projection";
            var saved=p.save(h.getLevel().registryAccess(),false);var loaded=Part.load(saved,h.getLevel().registryAccess());
            int expected=net.foundations.pl4.core.HologramProjection.view(face.ordinal(),Direction.WEST.ordinal());
            net.foundations.pl4.compat.PortAssertions.check(loaded.hologramView==expected&&loaded.identity.equals(p.identity),"Hologram view and identity persist");
            saved.remove("hologramView");loaded=Part.load(saved,h.getLevel().registryAccess());
            net.foundations.pl4.compat.PortAssertions.check(loaded.hologramView==net.foundations.pl4.core.HologramProjection.view(face.ordinal(),3),"Legacy projectors load without moving mount or slots");
        }h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void hologramBaseAndRotatedOutlineAgree(GameTestHelper h){
        for(Kind kind:List.of(Kind.HOLOGRAM,Kind.ADVANCED_HOLOGRAM))for(Direction mount:List.of(Direction.DOWN,Direction.UP)){
            Part p=new Part(kind,mount,OWNER);p.hologramView=3;var source=MultipartShapes.part(List.of(p),p).bounds();
            p.hologramView=4;var rotated=MultipartShapes.part(List.of(p),p).bounds();
            net.foundations.pl4.compat.PortAssertions.check(Math.abs(rotated.minX-source.minZ)<.0001&&Math.abs(rotated.maxZ-(1-source.minX))<.0001,"Outline follows the same 90-degree base yaw as renderer");
        }h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void explicitFlipKeepsActiveLayoutWhenJoiningNewerNeighbor(GameTestHelper h){
        var a=panel(h,new BlockPos(2,2,2),Direction.SOUTH);configure(part(a));
        var b=panel(h,new BlockPos(3,2,2),Direction.SOUTH);part(b).displayOutward=false;part(b).label="Newer neighbor";part(b).layoutRevision=50;b.changed();
        NetworkEngine.ensureCurrent(h.getLevel().getServer());var active=part(a).displaySettings();
        var p=player(h,a,ItemStack.EMPTY);
        net.foundations.pl4.compat.PortAssertions.check(DisplayNetworks.flip(p,a,part(a),false),"Authorized flat canvas can flip into matching neighbor");
        net.foundations.pl4.compat.PortAssertions.check(part(a).displaySettings().equals(active)&&part(b).displaySettings().equals(active)&&part(b).layoutRevision>50,"Explicit flip snapshots active settings before rebuilding and mirrors the edit to the new canvas");
        h.succeed();
    }

}
