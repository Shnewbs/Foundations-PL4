package net.foundations.pl4;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.foundations.pl4.compat.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.foundations.pl4.compat.ItemContainerContents;
import net.minecraft.world.level.material.Fluids;
import net.foundations.pl4.core.*;
import net.minecraftforge.common.util.*;
import net.minecraftforge.fluids.FluidStack;

/** Minecraft/NeoForge integration fixtures. Not executed by the dependency-free runner. */
@net.minecraftforge.gametest.GameTestHolder(namespace=FoundationsPL4.ID)
@net.minecraftforge.gametest.GameTestDontPrefix
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
    @GameTest(template="empty")
    public static void allTypedElementsPersist(GameTestHelper h){
        Part p=new Part(Kind.DISPLAY,Direction.NORTH,owner(h));p.displayMode=DisplayElements.Mode.CUSTOM;p.displayPage=3;
        for(var type:DisplayElements.Type.values())p.elements.add(new Part.Element(spec(type).onPage(3)));
        Part copy=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(copy.displayMode==p.displayMode&&copy.displayPage==3&&copy.elements.equals(p.elements),"All seven types and stable element IDs/page must survive NBT");h.succeed();
    }
    @GameTest(template="empty")
    public static void oldTextAndBarMigrateWithoutInventoryFallback(GameTestHelper h){
        Part p=new Part(Kind.DISPLAY,Direction.UP,owner(h));CompoundTag saved=p.save(h.getLevel().registryAccess(),false);saved.remove("displayMode");
        ListTag elements=new ListTag();for(boolean bar:new boolean[]{false,true}){CompoundTag e=new CompoundTag();e.putString("text","Legacy");e.putString("key","storage");e.putInt("x",10);e.putInt("y",bar?30:10);e.putInt("color",0xAAFFEE);e.putBoolean("bar",bar);elements.add(e);}saved.put("elements",elements);
        Part copy=Part.load(saved,h.getLevel().registryAccess());
        h.assertTrue(copy.displayMode==DisplayElements.Mode.CUSTOM&&copy.elements.get(0).spec().type()==DisplayElements.Type.TEXT&&copy.elements.get(1).spec().type()==DisplayElements.Type.BAR,"Legacy elements become typed custom layout");h.succeed();
    }
    @GameTest(template="empty")
    public static void emptyCustomLayoutDoesNotResurrectList(GameTestHelper h){
        Part p=new Part(Kind.DISPLAY,Direction.WEST,owner(h));p.displayMode=DisplayElements.Mode.CUSTOM;
        Part copy=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());h.assertTrue(copy.elements.isEmpty()&&copy.displayMode==DisplayElements.Mode.CUSTOM,"Saved blank custom canvas is not automatic list");h.succeed();
    }
    @GameTest(template="empty")
    public static void visualItemCountIsSeparateFromPicture(GameTestHelper h){
        VisualSamples samples=new VisualSamples();samples.item(new ItemStack(Items.STONE,64));samples.item(new ItemStack(Items.STONE,33));var rows=samples.rows(h.getLevel().registryAccess(),true,128,0);
        h.assertTrue(rows.size()==1&&rows.get(0).value()==97&&rows.get(0).item().getCount()==1&&rows.get(0).hasBlock(),"Real total is not capped by one-item picture");h.succeed();
    }
    @GameTest(template="empty")
    public static void boundedVisualComponentVariantsStaySeparate(GameTestHelper h){
        var a=new ItemStack(Items.STONE,5);net.foundations.pl4.compat.PortData.set(a,DataComponents.CUSTOM_NAME,Component.literal("Alpha"));var b=new ItemStack(Items.STONE,7);net.foundations.pl4.compat.PortData.set(b,DataComponents.CUSTOM_NAME,Component.literal("Beta"));
        VisualSamples samples=new VisualSamples();samples.item(a);samples.item(b);var rows=samples.rows(h.getLevel().registryAccess(),true,128,0);
        h.assertTrue(rows.size()==2&&!rows.get(0).key().equals(rows.get(1).key())&&rows.get(0).value()==7,"Named visual variants have separate counts and keys");h.succeed();
    }
    @GameTest(template="empty")
    public static void visualRowsRoundTripNativeItemComponents(GameTestHelper h){
        var item=new ItemStack(Items.DIAMOND_SWORD,2);net.foundations.pl4.compat.PortData.set(item,DataComponents.CUSTOM_NAME,Component.literal("Model fixture"));VisualSamples samples=new VisualSamples();samples.item(item);
        Part.Row row=samples.rows(h.getLevel().registryAccess(),true,128,0).get(0);var copy=Part.Row.load(row.save(),h.getLevel().registryAccess());
        h.assertTrue(copy.hasItem()&&!copy.hasBlock()&&copy.value()==2&&copy.item().getHoverName().getString().equals("Model fixture")&&copy.key().equals(row.key()),"Synced visual picture retains bounded native components");h.succeed();
    }
    @GameTest(template="empty")
    public static void nestedInventoriesAreNotSentAsPictureMetadata(GameTestHelper h){
        ItemStack box=new ItemStack(Items.SHULKER_BOX);net.foundations.pl4.compat.PortData.set(box,DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,64))));
        VisualSamples samples=new VisualSamples();samples.item(box);var row=samples.rows(h.getLevel().registryAccess(),true,128,0).get(0);
        h.assertTrue(!net.foundations.pl4.compat.PortData.has(row.item(),DataComponents.CONTAINER)&&!net.foundations.pl4.compat.PortData.has(row.item(),DataComponents.BLOCK_ENTITY_DATA),"A display picture must not contain nested machine/inventory contents");h.succeed();
    }
    @GameTest(template="empty")
    public static void hugeVisualComponentFallsBackWithinCap(GameTestHelper h){
        ItemStack item=new ItemStack(Items.STONE,12);net.foundations.pl4.compat.PortData.set(item,DataComponents.CUSTOM_NAME,Component.literal("x".repeat(8192)));VisualSamples samples=new VisualSamples();samples.item(item);
        Part.Row row=samples.rows(h.getLevel().registryAccess(),true,128,0).get(0);
        h.assertTrue(row.value()==12&&row.item().is(Items.STONE)&&!net.foundations.pl4.compat.PortData.has(row.item(),DataComponents.CUSTOM_NAME)&&VisualSamples.bounded(row.previewItem(),4096)!=null,"Oversized pictures preserve total and fall back to base item");h.succeed();
    }
    @GameTest(template="empty")
    public static void fluidRowsCarryTypeAmountAndCapacity(GameTestHelper h){
        VisualSamples samples=new VisualSamples();samples.fluid(new FluidStack(Fluids.WATER,500),1000);samples.fluid(new FluidStack(Fluids.WATER,250),1000);samples.fluid(new FluidStack(Fluids.LAVA,100),1000);
        var rows=samples.rows(h.getLevel().registryAccess(),true,128,0);var row=Part.Row.load(rows.get(0).save(),h.getLevel().registryAccess());
        h.assertTrue(rows.size()==2&&row.fluid().getFluid()==Fluids.WATER&&row.fluid().getAmount()==1&&row.value()==750&&row.capacity()==2000,"Fluid preview is separate from aggregate amount/capacity");h.succeed();
    }
    @GameTest(template="empty")
    public static void elementJsonRejectsUnknownTypeAndFractionalGeometry(GameTestHelper h){
        var s=spec(DisplayElements.Type.TEXT).textStyle(DisplayElements.TextAlign.CENTER,true,1.5F);
        h.assertTrue(ElementJson.decode(ElementJson.encode(s)).equals(s),"JSON property codec must round trip text style");
        var saved=new Part.Element(s).save();var loaded=Part.Element.load(saved);
        h.assertTrue(loaded.spec().textAlign()==DisplayElements.TextAlign.CENTER&&loaded.spec().wrap()&&loaded.spec().textScale()==1.5F,"Text style persists with element data");
        var legacy=ElementJson.decode(ElementJson.encode(s).replace(",\"textAlign\":\"CENTER\",\"wrap\":true,\"textScale\":1.5",""));
        h.assertTrue(legacy.textAlign()==DisplayElements.TextAlign.LEFT&&!legacy.wrap()&&legacy.textScale()==1F,"Legacy JSON defaults text to left/no-wrap/1x scale");
        String valid=ElementJson.encode(s);boolean badType=false,badNumber=false;
        try{ElementJson.decode(valid.replace("\""+s.type().name()+"\"","\"CLASSLOADER\""));}catch(RuntimeException expected){badType=true;}
        var parsed=com.google.gson.JsonParser.parseString(valid).getAsJsonObject();parsed.addProperty("x",1.5);try{ElementJson.decode(parsed.toString());}catch(RuntimeException expected){badNumber=true;}
        h.assertTrue(badType&&badNumber,"Whitelist types and exact integer geometry are required");h.succeed();
    }
    @GameTest(template="empty")
    public static void ownedTypedEditCommitsAndSelectsCustom(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var s=spec(DisplayElements.Type.BLOCK);var p=player(h,host,true);
        PLPackets.editLayout(p,packet(host,0,"add",s,""));
        h.assertTrue(part(host).elements.size()==1&&part(host).elements.get(0).spec().type()==DisplayElements.Type.BLOCK&&part(host).displayMode==DisplayElements.Mode.CUSTOM&&part(host).layoutRevision==1,"Block selection must commit a block renderer, not just a reader mode");h.succeed();
    }
    @GameTest(template="empty")
    public static void staleTypedEditCannotOverwriteLayout(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);part(host).layoutRevision=5;var p=player(h,host,true);
        PLPackets.editLayout(p,packet(host,4,"add",spec(DisplayElements.Type.ITEM),""));h.assertTrue(part(host).layoutRevision==5&&part(host).elements.isEmpty(),"Stale edit is rejected before mutation");h.succeed();
    }
    @GameTest(template="empty")
    public static void otherOwnerCannotEditDisplay(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);PLPackets.editLayout(player(h,host,false),packet(host,0,"add",spec(DisplayElements.Type.BLOCK),""));
        h.assertTrue(part(host).elements.isEmpty()&&part(host).layoutRevision==0,"Non-owner must not edit another display");h.succeed();
    }
    @GameTest(template="empty")
    public static void distantEditCannotMutateDisplay(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=player(h,host,true);p.setPos(host.getBlockPos().getX()+20,host.getBlockPos().getY(),host.getBlockPos().getZ());PLPackets.editLayout(p,packet(host,0,"add",spec(DisplayElements.Type.BLOCK),""));
        h.assertTrue(part(host).elements.isEmpty(),"Out-of-range anchor must be rejected");h.succeed();
    }
    @GameTest(template="empty")
    public static void replacedAnchorRejectsOldIdentity(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var stale=packet(host,0,"add",spec(DisplayElements.Type.BLOCK),"");part(host).identity=UUID.randomUUID();host.changed();PLPackets.editLayout(player(h,host,true),stale);
        h.assertTrue(part(host).elements.isEmpty(),"Reusing coordinates must not reuse edit authority");h.succeed();
    }
    @GameTest(template="empty")
    public static void invisibleReaderBindingCannotBeInjected(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var base=spec(DisplayElements.Type.ITEM);var s=new DisplayElements.Spec(base.id(),base.type(),"",UUID.randomUUID().toString(),"","",base.bounds(),base.color(),true,false,8,0,0,false,false);
        PLPackets.editLayout(player(h,host,true),packet(host,0,"add",s,""));h.assertTrue(part(host).elements.isEmpty(),"Arbitrary unrelated reader UUID is not a source binding");h.succeed();
    }
    @GameTest(template="empty")
    public static void replaceActionRestoresWholeLayoutSnapshot(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var a=spec(DisplayElements.Type.ITEM);var p=player(h,host,true);
        PLPackets.editLayout(p,packet(host,0,"add",a,""));
        var snapshot=part(host).elements.stream().map(Part.Element::spec).toList();
        var b=spec(DisplayElements.Type.BLOCK);PLPackets.editLayout(p,packet(host,1,"add",b,""));
        h.assertTrue(part(host).elements.size()==2,"Second add must have applied before the undo restore");
        var restore=new PLPackets.LayoutEdit(host.getBlockPos(),part(host).slot(),part(host).identity,part(host).layoutRevision,"replace",new UUID(0,0),ElementJson.encodeList(snapshot));
        PLPackets.editLayout(p,restore);
        h.assertTrue(part(host).elements.size()==1&&part(host).elements.get(0).spec().equals(a)&&part(host).layoutRevision==3,"Replace must restore the exact prior snapshot as an atomic, revision-fenced edit");h.succeed();
    }
    @GameTest(template="empty")
    public static void replaceActionRejectsOversizedSnapshot(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=player(h,host,true);
        List<DisplayElements.Spec> oversized=new ArrayList<>();for(int i=0;i<DisplayElements.MAX_ELEMENTS+1;i++)oversized.add(spec(DisplayElements.Type.ITEM).identity(UUID.randomUUID()));
        var command=new PLPackets.LayoutEdit(host.getBlockPos(),part(host).slot(),part(host).identity,0,"replace",new UUID(0,0),ElementJson.encodeList(oversized));
        PLPackets.editLayout(p,command);h.assertTrue(part(host).elements.isEmpty()&&part(host).layoutRevision==0,"Oversized snapshot restore must be rejected, not truncated");h.succeed();
    }
    @GameTest(template="empty")
    public static void deleteLastElementKeepsBlankCustomMode(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var s=spec(DisplayElements.Type.ITEM);part(host).elements.add(new Part.Element(s));part(host).displayMode=DisplayElements.Mode.CUSTOM;var p=player(h,host,true);
        var command=new PLPackets.LayoutEdit(host.getBlockPos(),part(host).slot(),part(host).identity,0,"delete",s.id(),"");PLPackets.editLayout(p,command);
        h.assertTrue(part(host).elements.isEmpty()&&part(host).displayMode==DisplayElements.Mode.CUSTOM&&part(host).layoutRevision==1,"Delete is not auto-list mode");h.succeed();
    }
    @GameTest(template="empty")
    public static void modeSwitchKeepsTypedLayout(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var s=spec(DisplayElements.Type.FLUID);part(host).elements.add(new Part.Element(s));part(host).displayMode=DisplayElements.Mode.CUSTOM;
        PLPackets.editLayout(player(h,host,true),packet(host,0,"mode",null,"AUTO_LIST"));
        h.assertTrue(part(host).displayMode==DisplayElements.Mode.AUTO_LIST&&part(host).elements.get(0).spec().equals(s),"Switching view must preserve user layout");h.succeed();
    }
    @GameTest(template="empty")
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
    @GameTest(template="empty")
    public static void componentVariantKeyIsStableAcrossSave(GameTestHelper h){
        var item=new ItemStack(Items.LEATHER_CHESTPLATE,4);net.foundations.pl4.compat.PortData.set(item,DataComponents.CUSTOM_NAME,Component.literal("Stable picture"));VisualSamples first=new VisualSamples();first.item(item);var r=first.rows(h.getLevel().registryAccess(),true,128,0).get(0);
        var restored=net.foundations.pl4.compat.PortData.parseItem(h.getLevel().registryAccess(),(CompoundTag)net.foundations.pl4.compat.PortData.save(item,h.getLevel().registryAccess()));VisualSamples second=new VisualSamples();second.item(restored);var r2=second.rows(h.getLevel().registryAccess(),true,128,0).get(0);
        h.assertTrue(r.key().equals(r2.key())&&r.value()==r2.value(),"Variant bindings must not drift simply because an item was saved");h.succeed();
    }
    @GameTest(template="empty")
    public static void arrangementUsesServerCanvasAndPreservesStyle(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var s=spec(DisplayElements.Type.TEXT).bounds(new DisplayElements.Rect(24,20,30,18)).textStyle(DisplayElements.TextAlign.RIGHT,true,1.5F);
        var p=part(host);p.elements.add(new Part.Element(s));p.displayMode=DisplayElements.Mode.CUSTOM;
        var user=player(h,host,true);
        PLPackets.editLayout(user,new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,0,"align_right",new UUID(0,0),s.id().toString()));
        var arranged=part(host).elements.get(0).spec();
        h.assertTrue(arranged.bounds().right()==p.layoutWidth&&arranged.wrap()&&arranged.textScale()==1.5F&&arranged.textAlign()==DisplayElements.TextAlign.RIGHT&&p.layoutRevision==1,"Server canvas dimensions and style must be retained");h.succeed();
    }
    @GameTest(template="empty")
    public static void arrangementRejectsStaleAndOtherPageSelection(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var s=spec(DisplayElements.Type.TEXT).onPage(1);var p=part(host);p.elements.add(new Part.Element(s));p.layoutRevision=2;
        var user=player(h,host,true);
        PLPackets.editLayout(user,new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,1,"align_left",new UUID(0,0),s.id().toString()));
        PLPackets.editLayout(user,new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,2,"align_left",new UUID(0,0),s.id().toString()));
        h.assertTrue(p.layoutRevision==2&&p.elements.get(0).spec().equals(s),"Stale and cross-page edits must leave the layout unchanged");h.succeed();
    }
    @GameTest(template="empty")
    public static void arrangementRejectsUnauthorizedPlayer(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var s=spec(DisplayElements.Type.TEXT);var p=part(host);p.elements.add(new Part.Element(s));p.owner=UUID.randomUUID();
        PLPackets.editLayout(player(h,host,false),new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,0,"align_left",new UUID(0,0),s.id().toString()));
        h.assertTrue(p.layoutRevision==0&&p.elements.get(0).spec().equals(s),"Arrangement must enforce the existing ownership gate");h.succeed();
    }
    @GameTest(template="empty")
    public static void multiMoveIsAtomicAndClampsSharedDelta(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=part(host);var a=spec(DisplayElements.Type.TEXT).bounds(new DisplayElements.Rect(10,20,30,18)).textStyle(DisplayElements.TextAlign.RIGHT,true,1.5F);var b=spec(DisplayElements.Type.BAR).bounds(new DisplayElements.Rect(60,40,20,12));
        p.elements.add(new Part.Element(a));p.elements.add(new Part.Element(b));var user=player(h,host,true);
        PLPackets.editLayout(user,new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,0,"move_selection",new UUID(0,0),"400;400;"+a.id()+","+b.id()));
        var ma=p.elements.get(0).spec();var mb=p.elements.get(1).spec();
        h.assertTrue(p.layoutRevision==1&&mb.bounds().right()==p.layoutWidth&&mb.bounds().bottom()==p.layoutHeight&&mb.bounds().x()-ma.bounds().x()==50&&ma.wrap()&&ma.textScale()==1.5F,"Move commits once using server bounds and preserves selection spacing/style");h.succeed();
    }
    @GameTest(template="empty")
    public static void multiMoveRejectsStaleMissingAndForeignOwner(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=part(host);var a=spec(DisplayElements.Type.TEXT);p.elements.add(new Part.Element(a));p.layoutRevision=2;var user=player(h,host,true);
        PLPackets.editLayout(user,new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,1,"move_selection",new UUID(0,0),"4;4;"+a.id()));
        PLPackets.editLayout(user,new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,2,"move_selection",new UUID(0,0),"4;4;"+a.id()+","+UUID.randomUUID()));
        p.owner=UUID.randomUUID();PLPackets.editLayout(player(h,host,false),new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,2,"move_selection",new UUID(0,0),"4;4;"+a.id()));
        h.assertTrue(p.layoutRevision==2&&p.elements.get(0).spec().equals(a),"Invalid moves cannot partially change selection");h.succeed();
    }
    @GameTest(template="empty")
    public static void multiPasteRejectsInvisibleReaderAndAcceptsWholeBatch(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=part(host);var a=spec(DisplayElements.Type.TEXT);var b=spec(DisplayElements.Type.BAR);var user=player(h,host,true);
        var invisible=new DisplayElements.Spec(UUID.randomUUID(),a.type(),a.text(),"hidden-reader",a.key(),a.asset(),a.bounds(),a.color(),a.count(),a.names(),a.columns(),a.offset(),a.page(),a.vertical(),a.compact());
        PLPackets.editLayout(user,new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,0,"paste",new UUID(0,0),ElementJson.encodeList(List.of(a,invisible))));
        h.assertTrue(p.elements.isEmpty()&&p.layoutRevision==0,"Invisible binding rejects entire paste");
        PLPackets.editLayout(user,new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,0,"paste",new UUID(0,0),ElementJson.encodeList(List.of(a,b))));
        h.assertTrue(p.elements.size()==2&&p.layoutRevision==1&&p.elements.get(0).spec().equals(a)&&p.elements.get(1).spec().equals(b),"Whole batch added in one revision");h.succeed();
    }
    @GameTest(template="empty")
    public static void layerSelectionIsAtomicPageLocalAndPersists(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=part(host);var a=spec(DisplayElements.Type.TEXT).textStyle(DisplayElements.TextAlign.RIGHT,true,1.5F);var b=spec(DisplayElements.Type.BAR);var c=spec(DisplayElements.Type.ITEM);var other=spec(DisplayElements.Type.TEXT).onPage(1);
        p.displayMode=DisplayElements.Mode.CUSTOM;for(var s:List.of(a,other,b,c))p.elements.add(new Part.Element(s));
        PLPackets.editLayout(player(h,host,true),packet(host,0,"layer_front",null,b.id()+","+a.id()));
        var expected=List.of(c,other,a,b);h.assertTrue(p.layoutRevision==1&&p.elements.stream().map(Part.Element::spec).toList().equals(expected),"Atomic layer selection preserves styles and other page slots");
        var copy=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(copy.elements.equals(p.elements)&&copy.layoutRevision==1,"Layer order survives native NBT reload");h.succeed();
    }
    @GameTest(template="empty")
    public static void layerSelectionRejectsStaleMissingOtherPageAndMalformedIds(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=part(host);var a=spec(DisplayElements.Type.TEXT);var b=spec(DisplayElements.Type.BAR);var other=spec(DisplayElements.Type.ITEM).onPage(1);for(var s:List.of(a,b,other))p.elements.add(new Part.Element(s));p.layoutRevision=2;var user=player(h,host,true);
        PLPackets.editLayout(user,packet(host,1,"layer_front",null,a.id().toString()));
        PLPackets.editLayout(user,packet(host,2,"layer_front",null,a.id()+","+UUID.randomUUID()));
        PLPackets.editLayout(user,packet(host,2,"layer_front",null,a.id()+","+other.id()));
        PLPackets.editLayout(user,packet(host,2,"layer_front",null,a.id()+",invalid"));
        h.assertTrue(p.layoutRevision==2&&p.elements.stream().map(Part.Element::spec).toList().equals(List.of(a,b,other)),"Invalid selection must never reorder any element");h.succeed();
    }
    @GameTest(template="empty")
    public static void layerSelectionRejectsForeignOwnerAndWrongDisplayIdentity(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=part(host);var a=spec(DisplayElements.Type.TEXT);var b=spec(DisplayElements.Type.BAR);for(var s:List.of(a,b))p.elements.add(new Part.Element(s));
        PLPackets.editLayout(player(h,host,true),new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),UUID.randomUUID(),0,"layer_front",new UUID(0,0),a.id().toString()));
        p.owner=UUID.randomUUID();PLPackets.editLayout(player(h,host,false),packet(host,0,"layer_front",null,a.id().toString()));
        h.assertTrue(p.layoutRevision==0&&p.elements.get(0).spec().equals(a),"Layer controls retain both identity and ownership fences");h.succeed();
    }

    @GameTest(template="empty")
    public static void pageCopyAndClearPersistAndKeepOtherPages(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=part(host);var a=spec(DisplayElements.Type.TEXT).textStyle(DisplayElements.TextAlign.RIGHT,true,1.5F);var other=spec(DisplayElements.Type.BAR).onPage(2);p.elements.add(new Part.Element(a));p.elements.add(new Part.Element(other));var user=player(h,host,true);
        PLPackets.editLayout(user,packet(host,0,"page_copy",null,"7"));
        h.assertTrue(p.layoutRevision==1&&p.elements.size()==3&&p.elements.get(2).spec().page()==7&&!p.elements.get(2).id().equals(a.id())&&p.elements.get(2).spec().identity(a.id()).onPage(0).equals(a),"Copy page once with fresh identity and retained styles");
        PLPackets.editLayout(user,packet(host,1,"page_clear",null,""));
        h.assertTrue(p.layoutRevision==2&&p.elements.size()==2&&p.elements.get(0).spec().equals(other)&&p.elements.get(1).spec().page()==7,"Clear only source page");
        var copy=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());h.assertTrue(copy.elements.equals(p.elements)&&copy.layoutRevision==2,"Page edits persist natively");h.succeed();
    }
    @GameTest(template="empty")
    public static void pageCopyRejectsOverwriteStaleCapacityAndForeignOwner(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=part(host);var a=spec(DisplayElements.Type.TEXT);var other=spec(DisplayElements.Type.BAR).onPage(1);p.elements.add(new Part.Element(a));p.elements.add(new Part.Element(other));p.layoutRevision=2;var user=player(h,host,true);
        PLPackets.editLayout(user,packet(host,1,"page_copy",null,"2"));PLPackets.editLayout(user,packet(host,2,"page_copy",null,"1"));
        h.assertTrue(p.layoutRevision==2&&p.elements.size()==2,"No stale copy or occupied-page overwrite");
        while(p.elements.size()<32)p.elements.add(new Part.Element(a.identity(UUID.randomUUID())));
        PLPackets.editLayout(user,packet(host,2,"page_copy",null,"7"));
        p.owner=UUID.randomUUID();PLPackets.editLayout(player(h,host,false),packet(host,2,"page_clear",null,""));
        h.assertTrue(p.layoutRevision==2&&p.elements.size()==32,"Capacity and permission rejection leave all pages unchanged");h.succeed();
    }

    @GameTest(template="empty")
    public static void r3OrganizationPersistsAndRejectsUnauthorizedEdits(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=part(host);var a=spec(DisplayElements.Type.TEXT);var b=spec(DisplayElements.Type.BAR);p.elements.add(new Part.Element(a));p.elements.add(new Part.Element(b));
        var user=player(h,host,true);String ids=a.id()+","+b.id();
        PLPackets.editLayout(user,packet(host,0,"group",null,ids));PLPackets.editLayout(user,packet(host,1,"lock",null,ids));PLPackets.editLayout(user,packet(host,2,"hide",null,ids));
        PLPackets.editLayout(user,packet(host,3,"page_name",null,"Production"));
        var restored=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(restored.elements.equals(p.elements)&&restored.pageName(0).equals("Production"),"Metadata and page names survive save/load");
        h.assertTrue(p.elements.get(0).spec().options().locked()&&p.elements.get(0).spec().options().hidden(),"Server applied metadata");
        var json=ElementJson.decodeList(ElementJson.encodeList(p.elements.stream().map(Part.Element::spec).toList()));
        h.assertTrue(json.equals(p.elements.stream().map(Part.Element::spec).toList()),"JSON snapshots preserve metadata");
        PLPackets.editLayout(user,new PLPackets.LayoutEdit(host.getBlockPos(),p.slot(),p.identity,4,"delete",a.id(),""));
        h.assertTrue(p.elements.size()==2&&p.layoutRevision==4,"Locked delete rejected");
        p.owner=UUID.randomUUID();PLPackets.editLayout(player(h,host,false),packet(host,4,"unlock",null,ids));
        h.assertTrue(p.layoutRevision==4&&p.elements.get(0).spec().options().locked(),"Foreign owner cannot unlock");h.succeed();
    }

    @GameTest(template="empty")
    public static void r3PageActionUsesVisibleFrontmostElementAndPermissions(GameTestHelper h){
        var host=display(h,2,Kind.DISPLAY);var p=part(host);p.displayMode=DisplayElements.Mode.CUSTOM;
        var e=spec(DisplayElements.Type.TEXT).bounds(new DisplayElements.Rect(100,40,60,40)).options(new DisplayElements.Options("",false,false,-1,-1,1));p.elements.add(new Part.Element(e));
        var user=player(h,host,true);var pos=host.getBlockPos();user.setPos(pos.getX()+.5,pos.getY()+.5-user.getEyeHeight(),pos.getZ()+3);user.setYRot(180);user.setXRot(0);
        h.assertTrue(DisplayActions.activate(user,host,p)&&p.displayPage==1&&p.layoutRevision==1,"Owner click switches page using server ray");
        p.displayPage=0;p.elements.add(new Part.Element(e.identity(UUID.randomUUID()).options(DisplayElements.Options.DEFAULT)));
        h.assertTrue(!DisplayActions.activate(user,host,p)&&p.displayPage==0,"Non-action front layer blocks link below");
        net.foundations.pl4.compat.PortLists.removeLast(p.elements);p.elements.set(0,new Part.Element(e.options(new DisplayElements.Options("",false,true,-1,-1,1))));
        h.assertTrue(!DisplayActions.activate(user,host,p),"Hidden action cannot fire");
        p.elements.set(0,new Part.Element(e));p.owner=UUID.randomUUID();
        h.assertTrue(!DisplayActions.activate(user,host,p)&&p.displayPage==0,"Foreign owner cannot change displayed page");h.succeed();
    }

}
