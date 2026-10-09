package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.*;

@Mod(FoundationsPL4.ID)
public final class FoundationsPL4 {
    public static final String ID = "foundations_pl4";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK,ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM,ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU,ID);
    public static final RegistryObject<MenuType<HammerMenu>> HAMMER_MENU = MENUS.register("hammer", () -> new MenuType<>(HammerMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ID);
    public static final EnumMap<Kind, RegistryObject<PartModelBlock>> MODELS = new EnumMap<>(Kind.class);
    public static final EnumMap<Kind, RegistryObject<PartItem>> PART_ITEMS = new EnumMap<>(Kind.class);
    public static final Map<String,RegistryObject<CableModelBlock>> CABLE_MODELS = new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> MATERIALS = new LinkedHashMap<>();
    public static final RegistryObject<PartModelBlock> KINETIC_READER_MODEL = BLOCKS.register("kinetic_reader_model", () -> new PartModelBlock(Kind.ENERGY_READER,BlockBehaviour.Properties.of().noOcclusion().noCollission()));
    public static final RegistryObject<PartModelBlock> AE2_READER_MODEL = BLOCKS.register("ae2_reader_model", () -> new PartModelBlock(Kind.ENERGY_READER,BlockBehaviour.Properties.of().noOcclusion().noCollission()));
    public static final RegistryObject<HostBlock> HOST = BLOCKS.register("multipart_host", () -> new HostBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(.5F,25).noOcclusion().dynamicShape()));
    public static final RegistryObject<LargeDisplayModelBlock> LARGE_MODEL = BLOCKS.register("large_display_model", () -> new LargeDisplayModelBlock(BlockBehaviour.Properties.of().noOcclusion().noCollission()));
    public static final RegistryObject<Block> ORE = BLOCKS.register("sapphireore", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3,5).requiresCorrectToolForDrops()));
    public static final RegistryObject<HammerBlock> HAMMER = BLOCKS.register("hammer", () -> new HammerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<HammerSpaceBlock> HAMMER_SPACE = BLOCKS.register("hammer_air", () -> new HammerSpaceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<BlockEntityType<HostEntity>> HOST_ENTITY = ENTITIES.register("host", () -> BlockEntityType.Builder.of(HostEntity::new,HOST.get()).build(null));
    public static final RegistryObject<BlockEntityType<HammerEntity>> HAMMER_ENTITY = ENTITIES.register("hammer", () -> BlockEntityType.Builder.of(HammerEntity::new,HAMMER.get()).build(null));
    static {
        for (Kind k : Kind.values()) {
            MODELS.put(k,BLOCKS.register(k.id, () -> new PartModelBlock(k,BlockBehaviour.Properties.of().noOcclusion().strength(.5F))));
            PART_ITEMS.put(k,ITEMS.register(k.id, () -> new PartItem(k,new Item.Properties())));
        }
        for (String material : List.of("data", "redstone_off", "redstone_on")) {
            for (String connector : List.of("cable", "internal", "half", "centre")) {
                String name = "cable_model_" + material + "_" + connector;
                CABLE_MODELS.put(material + "_" + connector, BLOCKS.register(name,
                    () -> new CableModelBlock(BlockBehaviour.Properties.of().noOcclusion().noCollission())));
            }
        }
        for(String material:List.of("data","redstone_off","redstone_on"))for(String depth:List.of("1","15","2","3","4","6")){
            String key=material+"_lead_"+depth;
            CABLE_MODELS.put(key,BLOCKS.register("cable_model_"+key,()->new CableModelBlock(BlockBehaviour.Properties.of().noOcclusion().noCollission())));
        }
        for (String id : List.of("sapphire","sapphiredust","stoneplate","etchedplate","signallingplate","wirelessplate")) MATERIALS.put(id,ITEMS.register(id, () -> new Item(new Item.Properties())));
        ITEMS.register("sapphireore", () -> new BlockItem(ORE.get(),new Item.Properties())); ITEMS.register("hammer", () -> new BlockItem(HAMMER.get(),new Item.Properties()));
        ITEMS.register("operator", () -> new ToolItem(ToolItem.Mode.OPERATOR,new Item.Properties().stacksTo(1)));
        ITEMS.register("transceiver", () -> new ToolItem(ToolItem.Mode.BLOCK_LINK,new Item.Properties().stacksTo(1)));
        ITEMS.register("entitytransceiver", () -> new ToolItem(ToolItem.Mode.ENTITY_LINK,new Item.Properties().stacksTo(1)));
        ITEMS.register("wirelessstorage", () -> new ToolItem(ToolItem.Mode.STORAGE,new Item.Properties().stacksTo(1)));
        ITEMS.register("plguide", () -> new ToolItem(ToolItem.Mode.GUIDE,new Item.Properties().stacksTo(1)));
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.literal("Foundations PL4")).icon(() -> new ItemStack(item("sapphire"))).displayItems((parameters, output) -> ITEMS.getEntries().forEach(h -> output.accept(h.get()))).build());
    }
    public FoundationsPL4() {
        IEventBus bus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        var container=net.minecraftforge.fml.ModLoadingContext.get();
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TABS.register(bus); MENUS.register(bus);
        net.foundations.pl4.core.CoreRecipes.register(bus);
        container.registerConfig(ModConfig.Type.SERVER,PLConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT,PLClientConfig.SPEC);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e)->{
            PLPackets.register(new net.foundations.pl4.compat.RegisterPayloadHandlersEvent());
            var caps=new net.foundations.pl4.compat.RegisterCapabilitiesEvent();
            HammerEntity.capabilities(caps);NativeEnergyInput.register(caps);
        });
        MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.world.level.block.entity.BlockEntity.class,net.foundations.pl4.compat.PortCapabilities::attach);
        bus.addListener(PLGameTests::register);
        MinecraftForge.EVENT_BUS.addListener(NetworkEngine::tick);
        MinecraftForge.EVENT_BUS.addListener(NetworkEngine::stopped);
    }
    public static ResourceLocation id(String path) { return new ResourceLocation(ID,path); }
    public static Item item(String id) { return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id(id)); }
}
