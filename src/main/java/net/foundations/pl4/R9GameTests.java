package net.foundations.pl4;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.material.Fluids;
import net.foundations.pl4.core.*;
import net.neoforged.neoforge.common.util.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Minecraft/NeoForge integration fixtures. Not executed by the dependency-free runner. */
@PrefixGameTestTemplate(false)
public final class R9GameTests {
    private static UUID owner(GameTestHelper h){return UUID.nameUUIDFromBytes(("PL4-R9-"+h.absolutePos(BlockPos.ZERO)).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static HostEntity display(GameTestHelper h,int x,Kind kind){
        BlockPos position=new BlockPos(x,2,2);h.setBlock(position,FoundationsPL4.HOST.get());HostEntity host=(HostEntity)h.getBlockEntity(position);
        Part part=new Part(kind,Direction.SOUTH,owner(h));part.displayOutward=true;host.parts.put(part.slot(),part);host.changed();return host;
    }
    private static Part part(HostEntity host){return host.parts.values().stream().filter(p->p.kind.display()).findFirst().orElseThrow();}
    private static FakePlayer player(GameTestHelper h,HostEntity at,boolean allowed){
        UUID id=allowed?owner(h):UUID.nameUUIDFromBytes((owner(h)+"-other").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        FakePlayer player=FakePlayerFactory.get(h.getLevel(),new GameProfile(id,"PL4-R9-Test"));
        var p=at.getBlockPos().getCenter();player.setPos(p.x+1,p.y,p.z+1);return player;
    }
    private static PLPackets.LayoutEdit packet(HostEntity host,long revision,String action,DisplayElements.Spec spec,String value){
        Part p=part(host);return new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,revision,action,spec==null?new UUID(0,0):spec.id(),spec==null?value:ElementJson.encode(spec));
    }
    private static DisplayElements.Spec spec(DisplayElements.Type type){return DisplayElements.create(type,0);}
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void allTypedElementsPersist(GameTestHelper h){
        Part p=new Part(Kind.DISPLAY,Direction.NORTH,owner(h));p.displayMode=DisplayElements.Mode.CUSTOM;p.displayPage=3;
        for(var type:DisplayElements.Type.values())p.elements.add(new Part.Element(spec(type).onPage(3)));
        Part copy=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(copy.displayMode==p.displayMode&&copy.displayPage==3&&copy.elements.equals(p.elements),"All seven types and stable element IDs/page must survive NBT");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void oldTextAndBarMigrateWithoutInventoryFallback(GameTestHelper h){
        Part p=new Part(Kind.DISPLAY,Direction.UP,owner(h));CompoundTag saved=p.save(h.getLevel().registryAccess(),false);saved.remove("displayMode");
        ListTag elements=new ListTag();for(boolean bar:new boolean[]{false,true}){CompoundTag e=new CompoundTag();e.putString("text","Legacy");e.putString("key","storage");e.putInt("x",10);e.putInt("y",bar?30:10);e.putInt("color",0xAAFFEE);e.putBoolean("bar",bar);elements.add(e);}saved.put("elements",elements);
        Part copy=Part.load(saved,h.getLevel().registryAccess());
        h.assertTrue(copy.displayMode==DisplayElements.Mode.CUSTOM&&copy.elements.get(0).spec().type()==DisplayElements.Type.TEXT&&copy.elements.get(1).spec().type()==DisplayElements.Type.BAR,"Legacy elements become typed custom layout");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void emptyCustomLayoutDoesNotResurrectList(GameTestHelper h){
        Part p=new Part(Kind.DISPLAY,Direction.WEST,owner(h));p.displayMode=DisplayElements.Mode.CUSTOM;
        Part copy=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());h.assertTrue(copy.elements.isEmpty()&&copy.displayMode==DisplayElements.Mode.CUSTOM,"Saved blank custom canvas is not automatic list");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void visualItemCountIsSeparateFromPicture(GameTestHelper h){
        VisualSamples samples=new VisualSamples();samples.item(new ItemStack(Items.STONE,64));samples.item(new ItemStack(Items.STONE,33));var rows=samples.rows(h.getLevel().registryAccess(),true,128,0);
        h.assertTrue(rows.size()==1&&rows.getFirst().value()==97&&rows.getFirst().item().getCount()==1&&rows.getFirst().hasBlock(),"Real total is not capped by one-item picture");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void boundedVisualComponentVariantsStaySeparate(GameTestHelper h){
        var a=new ItemStack(Items.STONE,5);a.set(DataComponents.CUSTOM_NAME,Component.literal("Alpha"));var b=new ItemStack(Items.STONE,7);b.set(DataComponents.CUSTOM_NAME,Component.literal("Beta"));
        VisualSamples samples=new VisualSamples();samples.item(a);samples.item(b);var rows=samples.rows(h.getLevel().registryAccess(),true,128,0);
        h.assertTrue(rows.size()==2&&!rows.get(0).key().equals(rows.get(1).key())&&rows.get(0).value()==7,"Named visual variants have separate counts and keys");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void visualRowsRoundTripNativeItemComponents(GameTestHelper h){
        var item=new ItemStack(Items.DIAMOND_SWORD,2);item.set(DataComponents.CUSTOM_NAME,Component.literal("Model fixture"));VisualSamples samples=new VisualSamples();samples.item(item);
        Part.Row row=samples.rows(h.getLevel().registryAccess(),true,128,0).getFirst();var copy=Part.Row.load(row.save(),h.getLevel().registryAccess());
        h.assertTrue(copy.hasItem()&&!copy.hasBlock()&&copy.value()==2&&copy.item().getHoverName().getString().equals("Model fixture")&&copy.key().equals(row.key()),"Synced visual picture retains bounded native components");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void nestedInventoriesAreNotSentAsPictureMetadata(GameTestHelper h){
        ItemStack box=new ItemStack(Items.SHULKER_BOX);box.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,64))));
        VisualSamples samples=new VisualSamples();samples.item(box);var row=samples.rows(h.getLevel().registryAccess(),true,128,0).getFirst();
        h.assertTrue(!row.item().has(DataComponents.CONTAINER)&&!row.item().has(DataComponents.BLOCK_ENTITY_DATA),"A display picture must not contain nested machine/inventory contents");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void hugeVisualComponentFallsBackWithinCap(GameTestHelper h){
        ItemStack item=new ItemStack(Items.STONE,12);item.set(DataComponents.CUSTOM_NAME,Component.literal("x".repeat(8192)));VisualSamples samples=new VisualSamples();samples.item(item);
        Part.Row row=samples.rows(h.getLevel().registryAccess(),true,128,0).getFirst();
        h.assertTrue(row.value()==12&&row.item().is(Items.STONE)&&!row.item().has(DataComponents.CUSTOM_NAME)&&VisualSamples.bounded(row.previewItem(),4096)!=null,"Oversized pictures preserve total and fall back to base item");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void fluidRowsCarryTypeAmountAndCapacity(GameTestHelper h){
        VisualSamples samples=new VisualSamples();samples.fluid(new FluidStack(Fluids.WATER,500),1000);samples.fluid(new FluidStack(Fluids.WATER,250),1000);samples.fluid(new FluidStack(Fluids.LAVA,100),1000);
        var rows=samples.rows(h.getLevel().registryAccess(),true,128,0);var row=Part.Row.load(rows.getFirst().save(),h.getLevel().registryAccess());
        h.assertTrue(rows.size()==2&&row.fluid().getFluid()==Fluids.WATER&&row.fluid().getAmount()==1&&row.value()==750&&row.capacity()==2000,"Fluid preview is separate from aggregate amount/capacity");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void elementJsonRejectsUnknownTypeAndFractionalGeometry(GameTestHelper h){
        var s=spec(DisplayElements.Type.TEXT).textStyle(DisplayElements.TextAlign.CENTER,true);
        h.assertTrue(ElementJson.decode(ElementJson.encode(s)).equals(s),"JSON property codec must round trip text style");
        var saved=new Part.Element(s).save();var loaded=Part.Element.load(saved);
        h.assertTrue(loaded.spec().textAlign()==DisplayElements.TextAlign.CENTER&&loaded.spec().wrap(),"Text style persists with element data");
        var legacy=ElementJson.decode(ElementJson.encode(s).replace(",\"textAlign\":\"CENTER\",\"wrap\":true",""));
        h.assertTrue(legacy.textAlign()==DisplayElements.TextAlign.LEFT&&!legacy.wrap(),"Legacy JSON defaults text to left/no-wrap");
        String valid=ElementJson.encode(s);boolean badType=false,badNumber=false;
        try{ElementJson.decode(valid.replace("\""+s.type().name()+"\"","\"CLASSLOADER\""));}catch(RuntimeException expected){badType=true;}
        var parsed=com.google.gson.JsonParser.parseString(valid).getAsJsonObject();parsed.addProperty("x",1.5);try{ElementJson.decode(parsed.toString());}catch(RuntimeException expected){badNumber=true;}
        h.assertTrue(badType&&badNumber,"Whitelist types and exact integer geometry are required");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void ownedTypedEditCommitsAndSelectsCustom(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var s=spec(DisplayElements.Type.BLOCK);var p=player(h,host,true);
        PLPackets.editLayout(p,packet(host,0,"add",s,""));
        h.assertTrue(part(host).elements.size()==1&&part(host).elements.getFirst().spec().type()==DisplayElements.Type.BLOCK&&part(host).displayMode==DisplayElements.Mode.CUSTOM&&part(host).layoutRevision==1,"Block selection must commit a block renderer, not just a reader mode");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void staleTypedEditCannotOverwriteLayout(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);part(host).layoutRevision=5;var p=player(h,host,true);
        PLPackets.editLayout(p,packet(host,4,"add",spec(DisplayElements.Type.ITEM),""));h.assertTrue(part(host).layoutRevision==5&&part(host).elements.isEmpty(),"Stale edit is rejected before mutation");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void otherOwnerCannotEditDisplay(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);PLPackets.editLayout(player(h,host,false),packet(host,0,"add",spec(DisplayElements.Type.BLOCK),""));
        h.assertTrue(part(host).elements.isEmpty()&&part(host).layoutRevision==0,"Non-owner must not edit another display");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void distantEditCannotMutateDisplay(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=player(h,host,true);p.setPos(host.getBlockPos().getX()+20,host.getBlockPos().getY(),host.getBlockPos().getZ());PLPackets.editLayout(p,packet(host,0,"add",spec(DisplayElements.Type.BLOCK),""));
        h.assertTrue(part(host).elements.isEmpty(),"Out-of-range anchor must be rejected");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void replacedAnchorRejectsOldIdentity(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var stale=packet(host,0,"add",spec(DisplayElements.Type.BLOCK),"");part(host).identity=UUID.randomUUID();host.changed();PLPackets.editLayout(player(h,host,true),stale);
        h.assertTrue(part(host).elements.isEmpty(),"Reusing coordinates must not reuse edit authority");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void invisibleReaderBindingCannotBeInjected(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var base=spec(DisplayElements.Type.ITEM);var s=new DisplayElements.Spec(base.id(),base.type(),"",UUID.randomUUID().toString(),"","",base.bounds(),base.color(),true,false,8,0,0,false,false);
        PLPackets.editLayout(player(h,host,true),packet(host,0,"add",s,""));h.assertTrue(part(host).elements.isEmpty(),"Arbitrary unrelated reader UUID is not a source binding");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void deleteLastElementKeepsBlankCustomMode(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var s=spec(DisplayElements.Type.ITEM);part(host).elements.add(new Part.Element(s));part(host).displayMode=DisplayElements.Mode.CUSTOM;var p=player(h,host,true);
        var command=new PLPackets.LayoutEdit(host.getBlockPos(),part(host).slot(),part(host).identity,0,"delete",s.id(),"");PLPackets.editLayout(p,command);
        h.assertTrue(part(host).elements.isEmpty()&&part(host).displayMode==DisplayElements.Mode.CUSTOM&&part(host).layoutRevision==1,"Delete is not auto-list mode");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void modeSwitchKeepsTypedLayout(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var s=spec(DisplayElements.Type.FLUID);part(host).elements.add(new Part.Element(s));part(host).displayMode=DisplayElements.Mode.CUSTOM;
        PLPackets.editLayout(player(h,host,true),packet(host,0,"mode",null,"AUTO_LIST"));
        h.assertTrue(part(host).displayMode==DisplayElements.Mode.AUTO_LIST&&part(host).elements.getFirst().spec().equals(s),"Switching view must preserve user layout");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void typedJoinedSettingsSurviveRootRemoval(GameTestHelper h){
        var a=display(h,2,Kind.LARGE_DISPLAY);var b=display(h,3,Kind.LARGE_DISPLAY);NetworkEngine.ensureCurrent(h.getLevel().getServer());var root=DisplayNetworks.controller(a,part(a));
        var setting=new Part.DisplaySettings("Typed board","","",0xFFFFFF,List.of(new Part.Element(spec(DisplayElements.Type.BLOCK).onPage(2))),DisplayElements.Mode.CUSTOM,2,DisplayElements.WIDTH,DisplayElements.HEIGHT);
        DisplayNetworks.applyLayout(root.host(),root.part(),setting,12);root.host().changed();
        HostEntity survivor=root.host()==a?b:a;
        var single=net.foundations.pl4.core.DynamicCanvasLayout.large(1,1);
        var elements=setting.elements().stream().map(e->new Part.Element(net.foundations.pl4.core.DynamicCanvasLayout.migrate(
            e.spec(),setting.layoutWidth(),setting.layoutHeight(),single.width(),single.height()))).toList();
        var expected=new Part.DisplaySettings(setting.label(),setting.selected(),setting.metric(),setting.color(),elements,
            setting.displayMode(),setting.displayPage(),single.width(),single.height());
        h.getLevel().removeBlock(root.host().getBlockPos(),false);NetworkEngine.ensureCurrent(h.getLevel().getServer());
        h.assertTrue(part(survivor).displaySettings().equals(expected)&&part(survivor).layoutRevision==13,
            "All typed element fields survive proportional resizing after a controller change");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void componentVariantKeyIsStableAcrossSave(GameTestHelper h){
        var item=new ItemStack(Items.LEATHER_CHESTPLATE,4);item.set(DataComponents.CUSTOM_NAME,Component.literal("Stable picture"));VisualSamples first=new VisualSamples();first.item(item);var r=first.rows(h.getLevel().registryAccess(),true,128,0).getFirst();
        var restored=ItemStack.parseOptional(h.getLevel().registryAccess(),(CompoundTag)item.save(h.getLevel().registryAccess()));VisualSamples second=new VisualSamples();second.item(restored);var r2=second.rows(h.getLevel().registryAccess(),true,128,0).getFirst();
        h.assertTrue(r.key().equals(r2.key())&&r.value()==r2.value(),"Variant bindings must not drift simply because an item was saved");h.succeed();
    }
}
