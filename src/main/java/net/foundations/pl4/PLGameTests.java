package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.foundations.pl4.compat.Capabilities;
import net.minecraftforge.fluids.FluidStack;

@net.minecraftforge.gametest.GameTestHolder(value=FoundationsPL4.ID,namespace=FoundationsPL4.ID)
@net.minecraftforge.gametest.GameTestDontPrefix
public final class PLGameTests {
    /** CI-only isolation: transient test values must not race Forge's autosave file watcher.
     * Normal server configuration remains file-backed; no fixture assertions are relaxed. */
    @net.minecraft.gametest.framework.BeforeBatch(batch=FoundationsPL4.ID)
    public static void isolatedConfig(net.minecraft.server.level.ServerLevel level){
        if(!Boolean.getBoolean("foundations_pl4.isolatedGameTestConfig"))return;
        var memory=com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        PLConfig.SPEC.correct(memory);PLConfig.SPEC.setConfig(memory);
    }

    private static final UUID OWNER=UUID.fromString("aaaa0000-0000-0000-0000-000000000001");
    public static void register(RegisterGameTestsEvent e){e.register(PLGameTests.class);e.register(R5GameTests.class);e.register(R6GameTests.class);e.register(R7GameTests.class);e.register(R8GameTests.class);e.register(R9GameTests.class);e.register(R10GameTests.class);e.register(R11GameTests.class);e.register(R13GameTests.class);e.register(R16GameTests.class);e.register(PerformanceGameTests.class);e.register(EnergyIntegrationGameTests.class);}
    private static HostEntity host(GameTestHelper h,BlockPos p,Kind kind,Direction face){
        h.setBlock(p,FoundationsPL4.HOST.get());HostEntity host=(HostEntity)h.getBlockEntity(p);host.parts.put(net.foundations.pl4.core.MultipartTopology.slot(kind,face.ordinal()),new Part(kind,face,OWNER));
        // R5 fixtures explicitly include a centre cable: adjacent face devices are not implicit wires.
        if(!kind.cable())host.parts.put(6,new Part(kind.redstone()?Kind.REDSTONE_CABLE:Kind.DATA_CABLE,Direction.DOWN,OWNER));
        host.changed();return host;
    }
    private static ChestBlockEntity chest(GameTestHelper h,BlockPos p){h.setBlock(p,Blocks.CHEST);return (ChestBlockEntity)h.getBlockEntity(p);}
    @GameTest(template="empty")
    public static void hammerConservesItemsAndBlocksFullOutput(GameTestHelper h){
        BlockPos p=new BlockPos(1,1,1);h.setBlock(p,FoundationsPL4.HAMMER.get());HammerEntity hammer=(HammerEntity)h.getBlockEntity(p);
        hammer.inventory.setStackInSlot(0,new ItemStack(Blocks.STONE,2));
        for(int i=0;i<101;i++)HammerEntity.tick(h.getLevel(),h.absolutePos(p),hammer.getBlockState(),hammer);
        h.assertTrue(hammer.inventory.getStackInSlot(0).getCount()==1,"Hammer must consume exactly one stone");
        h.assertTrue(hammer.inventory.getStackInSlot(1).getCount()==4,"Hammer must produce four stone plates");
        hammer.inventory.setStackInSlot(1,new ItemStack(FoundationsPL4.item("stoneplate"),64));hammer.cooldown=0;
        for(int i=0;i<150;i++)HammerEntity.tick(h.getLevel(),h.absolutePos(p),hammer.getBlockState(),hammer);
        h.assertTrue(hammer.inventory.getStackInSlot(0).getCount()==1,"Full output must not consume input");h.succeed();
    }
    @GameTest(template="empty")
    public static void hammerAutomationCannotExtractInput(GameTestHelper h){
        BlockPos p=new BlockPos(1,1,1);h.setBlock(p,FoundationsPL4.HAMMER.get());HammerEntity hammer=(HammerEntity)h.getBlockEntity(p);
        hammer.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND));
        var capability=net.foundations.pl4.compat.PortCapabilities.get(h.getLevel(),Capabilities.ItemHandler.BLOCK,h.absolutePos(p),Direction.UP);
        h.assertTrue(capability!=null,"Item capability must exist");
        h.assertTrue(capability.extractItem(0,1,false).isEmpty(),"Automation must not extract hammer input");
        h.assertTrue(capability.insertItem(1,new ItemStack(Items.DIAMOND),false).getCount()==1,"Automation must not insert output");h.succeed();
    }
    @GameTest(template="empty")
    public static void persistentEscrowRoundTrip(GameTestHelper h){
        Part p=new Part(Kind.TRANSFER_NODE,Direction.WEST,OWNER);p.pendingItem=new ItemStack(Items.DIAMOND,17);p.pendingFluid=new FluidStack(Fluids.WATER,725);p.pendingEnergy=12345;p.transferMode=2;p.filter="#c:gems/diamond";
        p.links.add(new Part.Link("minecraft:overworld",new BlockPos(4,80,9),Direction.NORTH,null,UUID.randomUUID()));
        Part restored=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(restored.identity.equals(p.identity)&&restored.owner.equals(OWNER),"Identity and owner must persist");
        h.assertTrue(restored.pendingItem.getCount()==17&&restored.pendingFluid.getAmount()==725&&restored.pendingEnergy==12345,"All pending resources must persist");
        h.assertTrue(restored.links.equals(p.links)&&restored.transferMode==2&&restored.filter.equals(p.filter),"Links and settings must persist");h.succeed();
    }
    @GameTest(template="empty")
    public static void transferItemsConservesSeventeenDiamonds(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));
        HostEntity a=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),b=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);
        Part pa=a.parts.get(Direction.WEST.ordinal()),pb=b.parts.get(Direction.EAST.ordinal());pa.transferMode=2;pb.transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(new NetworkEngine.Ref(a,pa),new NetworkEngine.Ref(b,pb)));
        h.assertTrue(from.getItem(0).isEmpty()&&to.getItem(0).is(Items.DIAMOND)&&to.getItem(0).getCount()==17,"Exactly seventeen diamonds must arrive");
        h.assertTrue(pa.pendingItem.isEmpty(),"Escrow must clear after successful delivery");h.succeed();
    }
    @GameTest(template="empty")
    public static void fullDestinationDoesNotExtract(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,17));for(int i=0;i<to.getContainerSize();i++)to.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        HostEntity a=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),b=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);
        Part pa=a.parts.get(Direction.WEST.ordinal()),pb=b.parts.get(Direction.EAST.ordinal());pa.transferMode=2;pb.transferMode=1;
        TransferEngine.run(h.getLevel().getServer(),List.of(new NetworkEngine.Ref(a,pa),new NetworkEngine.Ref(b,pb)));
        h.assertTrue(from.getItem(0).getCount()==17&&pa.pendingItem.isEmpty(),"A full destination must not remove source contents");h.succeed();
    }
    @GameTest(template="empty")
    public static void restoredEscrowRetriesWithoutReextracting(GameTestHelper h){
        ChestBlockEntity from=chest(h,new BlockPos(1,1,1)),to=chest(h,new BlockPos(5,1,1));from.setItem(0,new ItemStack(Items.DIAMOND,11));
        HostEntity a=host(h,new BlockPos(2,1,1),Kind.TRANSFER_NODE,Direction.WEST),b=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);
        Part pa=a.parts.get(Direction.WEST.ordinal()),pb=b.parts.get(Direction.EAST.ordinal());pa.transferMode=2;pb.transferMode=1;pa.pendingItem=new ItemStack(Items.DIAMOND,6);
        TransferEngine.run(h.getLevel().getServer(),List.of(new NetworkEngine.Ref(a,pa),new NetworkEngine.Ref(b,pb)));
        h.assertTrue(from.getItem(0).getCount()==11&&to.getItem(0).getCount()==6&&pa.pendingItem.isEmpty(),"Retry must deliver only escrow without extracting again");h.succeed();
    }
    @GameTest(template="empty")
    public static void readerCountsAndFilters(GameTestHelper h){
        ChestBlockEntity chest=chest(h,new BlockPos(1,1,1));chest.setItem(0,new ItemStack(Items.DIAMOND,17));chest.setItem(1,new ItemStack(Items.IRON_INGOT,32));
        HostEntity host=host(h,new BlockPos(2,1,1),Kind.INVENTORY_READER,Direction.WEST);Part p=host.parts.get(Direction.WEST.ordinal());p.filter="minecraft:diamond";
        var ref=new NetworkEngine.Ref(host,p);var rows=DataSampler.sample(h.getLevel().getServer(),ref,List.of(ref.adjacent()),1);
        h.assertTrue(rows.size()==1&&rows.get(0).value()==17,"Reader must apply item filter and preserve count");
        p.whitelist=false;rows=DataSampler.sample(h.getLevel().getServer(),ref,List.of(ref.adjacent()),1);
        h.assertTrue(rows.size()==1&&rows.get(0).value()==32,"Exclusion filter must invert");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void networkUpdatesLiveDisplay(GameTestHelper h){
        ChestBlockEntity chest=chest(h,new BlockPos(1,1,1));chest.setItem(0,new ItemStack(Items.DIAMOND,17));
        host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST);host(h,new BlockPos(3,1,1),Kind.DATA_CABLE,Direction.DOWN);host(h,new BlockPos(4,1,1),Kind.INVENTORY_READER,Direction.DOWN);
        HostEntity screen=host(h,new BlockPos(5,1,1),Kind.DISPLAY,Direction.DOWN);Part display=screen.parts.get(7+Direction.DOWN.ordinal());
        h.runAtTickTime(45,()->{h.assertTrue(display.rows.stream().anyMatch(r->r.key().equals("minecraft:diamond")&&r.value()==17),"Display must receive reader data across connected hosts");h.succeed();});
    }
    @GameTest(template="empty")
    public static void upstreamCraftingRecipesLoad(GameTestHelper h){
        String[] ids={"plguide","datacable","redstonecable","inforeader","inventoryreader","fluidreader","energyreader","networkreader","hammer","node","array","entitynode","transfernode","displayscreen","minidisplay","largedisplayscreen","holographicdisplay","advancedholographicdisplay","dataemitter","datareceiver","redstoneemitter","redstonereceiver","redstonenode","redstonesignaller","clock","operator","transceiver","entitytransceiver","wirelessstorage"};
        for(String id:ids)h.assertTrue(h.getLevel().getRecipeManager().byKey(FoundationsPL4.id(id)).isPresent(),"Missing original recipe: "+id);h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void wirelessOwnerLinkCarriesData(GameTestHelper h){
        chest(h,new BlockPos(1,1,1)).setItem(0,new ItemStack(Items.DIAMOND,17));host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST);
        HostEntity emitter=host(h,new BlockPos(3,1,1),Kind.DATA_EMITTER,Direction.DOWN),receiver=host(h,new BlockPos(6,1,1),Kind.DATA_RECEIVER,Direction.DOWN),reader=host(h,new BlockPos(7,1,1),Kind.INVENTORY_READER,Direction.DOWN);
        Part ep=emitter.parts.get(0),rp=receiver.parts.get(0),read=reader.parts.get(0);
        rp.links.add(new Part.Link(h.getLevel().dimension().location().toString(),emitter.getBlockPos(),Direction.DOWN,null,ep.identity));
        h.runAtTickTime(45,()->{h.assertTrue(read.rows.stream().anyMatch(r->r.value()==17),"Owned wireless link must carry reader data");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void wirelessRejectsDifferentOwner(GameTestHelper h){
        chest(h,new BlockPos(1,1,1)).setItem(0,new ItemStack(Items.DIAMOND,17));host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST);
        HostEntity emitter=host(h,new BlockPos(3,1,1),Kind.DATA_EMITTER,Direction.DOWN),receiver=host(h,new BlockPos(6,1,1),Kind.DATA_RECEIVER,Direction.DOWN),reader=host(h,new BlockPos(7,1,1),Kind.INVENTORY_READER,Direction.DOWN);
        Part ep=emitter.parts.get(0),rp=receiver.parts.get(0),read=reader.parts.get(0);ep.owner=UUID.randomUUID();
        rp.links.add(new Part.Link(h.getLevel().dimension().location().toString(),emitter.getBlockPos(),Direction.DOWN,null,ep.identity));
        h.runAtTickTime(45,()->{h.assertTrue(read.rows.isEmpty(),"Forged cross-owner link must be ignored");h.succeed();});
    }
    @GameTest(template="empty")
    public static void readingUnloadedTargetDoesNotLoadChunk(GameTestHelper h){
        HostEntity host=host(h,new BlockPos(1,1,1),Kind.INVENTORY_READER,Direction.DOWN);Part p=host.parts.get(0);
        BlockPos distant=new BlockPos(1000000,80,1000000);h.assertTrue(!h.getLevel().hasChunkAt(distant),"Fixture must start unloaded");
        var rows=DataSampler.sample(h.getLevel().getServer(),new NetworkEngine.Ref(host,p),List.of(new Part.Link(h.getLevel().dimension().location().toString(),distant,Direction.UP,null,null)),1);
        h.assertTrue(rows.isEmpty()&&!h.getLevel().hasChunkAt(distant),"A read must never force-load a target chunk");h.succeed();
    }
    @GameTest(template="empty")
    public static void removedNodeItemRetainsAllEscrow(GameTestHelper h){
        Part p=new Part(Kind.TRANSFER_NODE,Direction.DOWN,OWNER);p.pendingItem=new ItemStack(Items.DIAMOND,17);p.pendingFluid=new FluidStack(Fluids.WATER,500);p.pendingEnergy=1200;
        ItemStack drop=PartItem.stack(p,h.getLevel().registryAccess());
        Part restored=Part.load(net.foundations.pl4.compat.PortData.get(drop,net.foundations.pl4.compat.DataComponents.CUSTOM_DATA).copyTag().getCompound("pl_part"),h.getLevel().registryAccess());
        h.assertTrue(restored.pendingItem.getCount()==17&&restored.pendingFluid.getAmount()==500&&restored.pendingEnergy==1200,"Breaking a node must preserve item, fluid and energy escrow in its dropped item");h.succeed();
    }
    @GameTest(template="empty")
    public static void internalForgingRecipesLoadAndMatchTags(GameTestHelper h){
        var type=net.foundations.pl4.core.CoreRecipes.HAMMER.get();
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(type).size()==6,"Six bundled forging recipes must load");
        Item[] inputs={FoundationsPL4.item("sapphire"),FoundationsPL4.ORE.get().asItem(),Blocks.STONE.asItem(),Items.DIAMOND,Items.REDSTONE,Items.ENDER_PEARL};
        String[] outputs={"sapphiredust","sapphiredust","stoneplate","etchedplate","signallingplate","wirelessplate"};int[] counts={1,2,4,4,4,4};
        for(int i=0;i<inputs.length;i++){
            var recipe=h.getLevel().getRecipeManager().getRecipeFor(type,new net.foundations.pl4.compat.SingleRecipeInput(new ItemStack(inputs[i])),h.getLevel());
            h.assertTrue(recipe.isPresent(),"Missing ingredient tag match: "+inputs[i]);
            var output=recipe.orElseThrow().value().result();
            h.assertTrue(output.is(FoundationsPL4.item(outputs[i]))&&output.getCount()==counts[i],"Incorrect bundled forging result");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void internalRecipeCodecPreservesCountsAndComponents(GameTestHelper h){
        ItemStack output=new ItemStack(Items.EMERALD,4);net.foundations.pl4.compat.PortData.set(output,net.foundations.pl4.compat.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Recipe codec fixture"));
        var recipe=new net.foundations.pl4.core.ForgingRecipe(net.minecraft.world.item.crafting.Ingredient.of(Items.DIAMOND),3,output,11,7);
        var codec=net.foundations.pl4.core.CoreRecipes.HAMMER_SERIALIZER.get().codec();
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,h.getLevel().registryAccess());
        var encoded=codec.encodeStart(ops,recipe).getOrThrow(false,message->{throw new IllegalArgumentException(message);});var decoded=codec.parse(ops,encoded).getOrThrow(false,message->{throw new IllegalArgumentException(message);});
        h.assertTrue(!decoded.matches(new net.foundations.pl4.compat.SingleRecipeInput(new ItemStack(Items.DIAMOND,2)),h.getLevel()),"Insufficient input count must not match");
        h.assertTrue(decoded.matches(new net.foundations.pl4.compat.SingleRecipeInput(new ItemStack(Items.DIAMOND,3)),h.getLevel()),"Required input count must match");
        h.assertTrue(decoded.processingTicks()==11&&decoded.cooldownTicks()==7&&ItemStack.isSameItemSameTags(decoded.result(),output),"Recipe fields and result components must survive codec round trip");
        decoded.result().shrink(4);h.assertTrue(decoded.result().getCount()==4,"Returned result stacks must not mutate the recipe");
        encoded.getAsJsonObject().addProperty("input_count",0);h.assertTrue(codec.parse(ops,encoded).result().isEmpty(),"Zero-input forging recipes must be rejected");h.succeed();
    }
    @GameTest(template="empty")
    public static void changingForgingRecipeResetsProgress(GameTestHelper h){
        BlockPos pos=new BlockPos(1,1,1);h.setBlock(pos,FoundationsPL4.HAMMER.get());HammerEntity hammer=(HammerEntity)h.getBlockEntity(pos);
        hammer.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND));for(int i=0;i<20;i++)HammerEntity.tick(h.getLevel(),h.absolutePos(pos),hammer.getBlockState(),hammer);
        h.assertTrue(hammer.progress==20,"Fixture must begin processing diamond recipe");
        hammer.inventory.setStackInSlot(0,new ItemStack(Items.REDSTONE));HammerEntity.tick(h.getLevel(),h.absolutePos(pos),hammer.getBlockState(),hammer);
        h.assertTrue(hammer.progress==1,"Changing recipe must reset elapsed work");
        for(int i=0;i<100;i++)HammerEntity.tick(h.getLevel(),h.absolutePos(pos),hammer.getBlockState(),hammer);
        h.assertTrue(hammer.inventory.getStackInSlot(1).is(FoundationsPL4.item("signallingplate")),"Changed input must produce the new recipe result");h.succeed();
    }
}
