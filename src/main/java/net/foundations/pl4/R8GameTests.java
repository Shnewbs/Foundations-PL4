package net.foundations.pl4;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(OWNER,"PL4-R8-Test"));p.getInventory().clearContent();p.getAbilities().instabuild=false;p.setShiftKeyDown(false);
        var centre=at.getBlockPos().getCenter();p.setPos(centre.x+1,centre.y,centre.z+1);p.setItemInHand(InteractionHand.MAIN_HAND,stack);return p;
    }
    private static void configure(Part p){p.label="Saved machine board";p.selected="power_main";p.color=0x53AACC;p.elements.add(new Part.Element("EU","","storage:eu",7,19,0xABDEEF,false));p.layoutRevision=20;}
    private static void extend(GameTestHelper h,Direction mount){
        var source=panel(h,new BlockPos(3,3,3),mount);Part original=part(source);configure(original);source.changed();
        for(Direction side:Direction.values())if(side.getAxis()!=mount.getAxis()){
            NetworkEngine.ensureCurrent(h.getLevel().getServer());
            ItemStack stack=new ItemStack(FoundationsPL4.PART_ITEMS.get(Kind.LARGE_DISPLAY).get(),2);var player=player(h,source,stack);
            Vec3 centre=source.getBlockPos().getCenter();Vec3 hitPoint=centre.add(mount.getStepX()*.4999+side.getStepX()*.5,mount.getStepY()*.4999+side.getStepY()*.5,mount.getStepZ()*.4999+side.getStepZ()*.5);
            var result=stack.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(hitPoint,side,source.getBlockPos(),false)));
            BlockPos destination=source.getBlockPos().relative(side);
            h.assertTrue(result.consumesAction()&&h.getLevel().getBlockEntity(destination) instanceof HostEntity,"Side click must create a neighboring host, not a perpendicular face");
            var added=(HostEntity)h.getLevel().getBlockEntity(destination);Part next=part(added);
            h.assertTrue(next.face==mount&&next.displayOutward==original.displayOutward&&Objects.equals(next.owner,original.owner),"Mount/front/owner match original");
            h.assertTrue(stack.getCount()==1&&original.canvasWidth*original.canvasHeight==2,"Exactly one item consumed and rectangle joins immediately");
            var root=DisplayNetworks.controller(source,original).part();h.assertTrue(root.displaySettings().equals(original.displaySettings())&&root.selected.equals("power_main"),"All four growth directions preserve layout");
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
        Vec3 point=source.getBlockPos().getCenter().add(.49,0,-.4999);
        stack.getItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(point,Direction.NORTH,source.getBlockPos(),false)));
        var next=(HostEntity)h.getLevel().getBlockEntity(source.getBlockPos().east());
        h.assertTrue(next!=null&&part(next).face==Direction.NORTH&&part(next).displayOutward,"Front rim extends east in original plane");
        h.assertTrue(part(next).selected.equals("power_main")&&part(next).label.equals("Saved machine board"),"Explicit extension uses source settings even for a saved item");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void centreClickDoesNotConsumeOrRotate(GameTestHelper h){
        var source=panel(h,new BlockPos(3,3,3),Direction.NORTH);ItemStack stack=new ItemStack(FoundationsPL4.PART_ITEMS.get(Kind.LARGE_DISPLAY).get(),2);var p=player(h,source,stack);
        var point=source.getBlockPos().getCenter().add(0,0,-.4999);
        var result=stack.getItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(point,Direction.NORTH,source.getBlockPos(),false)));
        h.assertTrue(result==InteractionResult.FAIL&&stack.getCount()==2&&h.getLevel().isEmptyBlock(source.getBlockPos().north()),"Ambiguous centre does not create a perpendicular or offset panel");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void occupiedExtensionCannotReplaceOrConsume(GameTestHelper h){
        var source=panel(h,new BlockPos(3,3,3),Direction.NORTH);h.setBlock(new BlockPos(4,3,3),Blocks.DIAMOND_BLOCK);
        ItemStack stack=new ItemStack(FoundationsPL4.PART_ITEMS.get(Kind.LARGE_DISPLAY).get(),2);var p=player(h,source,stack);
        var point=source.getBlockPos().getCenter().add(.5,0,-.4999);
        stack.getItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(point,Direction.EAST,source.getBlockPos(),false)));
        h.assertTrue(stack.getCount()==2&&h.getLevel().getBlockState(source.getBlockPos().east()).is(Blocks.DIAMOND_BLOCK),"Solid destination remains intact");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void removedControllerKeepsSharedLayout(GameTestHelper h){
        var a=panel(h,new BlockPos(2,2,2),Direction.SOUTH);var b=panel(h,new BlockPos(3,2,2),Direction.SOUTH);configure(part(a));a.changed();
        NetworkEngine.ensureCurrent(h.getLevel().getServer());var settings=part(a).displaySettings();
        h.assertTrue(part(b).displaySettings().equals(settings),"Shared settings must be copied to member before removal");
        h.getLevel().removeBlock(a.getBlockPos(),false);NetworkEngine.ensureCurrent(h.getLevel().getServer());
        h.assertTrue(part(b).displaySettings().equals(settings)&&part(b).canvasWidth==1,"Removing visual top-left keeps content on survivor");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void editedSharedLayoutSurvivesReload(GameTestHelper h){
        var a=panel(h,new BlockPos(2,2,2),Direction.SOUTH);var b=panel(h,new BlockPos(3,2,2),Direction.SOUTH);configure(part(a));a.changed();NetworkEngine.ensureCurrent(h.getLevel().getServer());
        Part root=DisplayNetworks.controller(a,part(a)).part();root.label="Latest edit";root.selected="inventory_main";DisplayNetworks.layoutEdited(a,root);
        for(var host:List.of(a,b)){
            CompoundTag saved=new CompoundTag();host.saveAdditional(saved,h.getLevel().registryAccess());host.loadAdditional(saved,h.getLevel().registryAccess());host.changed();
        }
        NetworkEngine.ensureCurrent(h.getLevel().getServer());
        h.assertTrue(part(a).label.equals("Latest edit")&&part(b).selected.equals("inventory_main")&&part(b).layoutRevision>20,"Save reload retains mirrored edit and revision");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void mergeUsesNewestStoredSettings(GameTestHelper h){
        var a=panel(h,new BlockPos(2,2,2),Direction.SOUTH);configure(part(a));var b=panel(h,new BlockPos(3,2,2),Direction.SOUTH);part(b).label="Newer canvas";part(b).layoutRevision=30;b.changed();
        NetworkEngine.ensureCurrent(h.getLevel().getServer());
        h.assertTrue(part(a).label.equals("Newer canvas")&&part(b).label.equals("Newer canvas")&&part(a).elements.isEmpty(),"Newer explicit layout (including clear) wins deterministically");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void hologramViewPersistenceAndLegacyDefault(GameTestHelper h){
        for(Kind kind:List.of(Kind.HOLOGRAM,Kind.ADVANCED_HOLOGRAM))for(Direction face:Direction.values()){
            Part p=new Part(kind,face,OWNER);p.hologramView=Direction.WEST.ordinal();p.label="Projection";
            var saved=p.save(h.getLevel().registryAccess(),false);var loaded=Part.load(saved,h.getLevel().registryAccess());
            int expected=net.foundations.pl4.core.HologramProjection.view(face.ordinal(),Direction.WEST.ordinal());
            h.assertTrue(loaded.hologramView==expected&&loaded.identity.equals(p.identity),"Hologram view and identity persist");
            saved.remove("hologramView");loaded=Part.load(saved,h.getLevel().registryAccess());
            h.assertTrue(loaded.hologramView==net.foundations.pl4.core.HologramProjection.view(face.ordinal(),3),"Legacy projectors load without moving mount or slots");
        }h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void hologramBaseAndRotatedOutlineAgree(GameTestHelper h){
        for(Kind kind:List.of(Kind.HOLOGRAM,Kind.ADVANCED_HOLOGRAM))for(Direction mount:List.of(Direction.DOWN,Direction.UP)){
            Part p=new Part(kind,mount,OWNER);p.hologramView=3;var source=MultipartShapes.part(List.of(p),p).bounds();
            p.hologramView=4;var rotated=MultipartShapes.part(List.of(p),p).bounds();
            h.assertTrue(Math.abs(rotated.minX-source.minZ)<.0001&&Math.abs(rotated.maxZ-(1-source.minX))<.0001,"Outline follows the same 90-degree base yaw as renderer");
        }h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void explicitFlipKeepsActiveLayoutWhenJoiningNewerNeighbor(GameTestHelper h){
        var a=panel(h,new BlockPos(2,2,2),Direction.SOUTH);configure(part(a));
        var b=panel(h,new BlockPos(3,2,2),Direction.SOUTH);part(b).displayOutward=false;part(b).label="Newer neighbor";part(b).layoutRevision=50;b.changed();
        NetworkEngine.ensureCurrent(h.getLevel().getServer());var active=part(a).displaySettings();
        var p=player(h,a,ItemStack.EMPTY);
        h.assertTrue(DisplayNetworks.flip(p,a,part(a),false),"Authorized flat canvas can flip into matching neighbor");
        h.assertTrue(part(a).displaySettings().equals(active)&&part(b).displaySettings().equals(active)&&part(b).layoutRevision>50,"Explicit flip snapshots active settings before rebuilding and mirrors the edit to the new canvas");
        h.succeed();
    }

}
