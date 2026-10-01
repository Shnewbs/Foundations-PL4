package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;

@Mod(FoundationsPL4.ID)
public final class FoundationsPL4 {
    public static final String ID = "foundations_pl4";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU,ID);
    public static final DeferredHolder<MenuType<?>,MenuType<HammerMenu>> HAMMER_MENU = MENUS.register("hammer", () -> new MenuType<>(HammerMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ID);
    public static final EnumMap<Kind, DeferredBlock<PartModelBlock>> MODELS = new EnumMap<>(Kind.class);
    public static final EnumMap<Kind, DeferredItem<PartItem>> PART_ITEMS = new EnumMap<>(Kind.class);
    public static final Map<String,DeferredBlock<CableModelBlock>> CABLE_MODELS = new LinkedHashMap<>();
    public static final Map<String,DeferredItem<Item>> MATERIALS = new LinkedHashMap<>();
    public static final DeferredBlock<HostBlock> HOST = BLOCKS.register("multipart_host", registryId -> new HostBlock(BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK,registryId)).mapColor(MapColor.COLOR_BLUE).strength(.5F,25).noOcclusion().dynamicShape()));
    public static final DeferredBlock<LargeDisplayModelBlock> LARGE_MODEL = BLOCKS.register("large_display_model", registryId -> new LargeDisplayModelBlock(BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK,registryId)).noOcclusion().noCollision()));
    public static final DeferredBlock<Block> ORE = BLOCKS.register("sapphireore", registryId -> new Block(BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK,registryId)).mapColor(MapColor.STONE).strength(3,5).requiresCorrectToolForDrops()));
    public static final DeferredBlock<HammerBlock> HAMMER = BLOCKS.register("hammer", registryId -> new HammerBlock(BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK,registryId)).mapColor(MapColor.WOOD).strength(2.5F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE)));
    public static final DeferredBlock<HammerSpaceBlock> HAMMER_SPACE = BLOCKS.register("hammer_air", registryId -> new HammerSpaceBlock(BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK,registryId)).mapColor(MapColor.WOOD).strength(2.5F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE)));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<HostEntity>> HOST_ENTITY = ENTITIES.register("host", () -> new BlockEntityType<>(HostEntity::new,HOST.get()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<HammerEntity>> HAMMER_ENTITY = ENTITIES.register("hammer", () -> new BlockEntityType<>(HammerEntity::new,HAMMER.get()));
    static {
        for (Kind k : Kind.values()) {
            MODELS.put(k,BLOCKS.register(k.id, registryId -> new PartModelBlock(k,BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK,registryId)).noOcclusion().strength(.5F))));
            PART_ITEMS.put(k,ITEMS.register(k.id, registryId -> new PartItem(k,new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM,registryId)))));
        }
        for (String material : List.of("data", "redstone_off", "redstone_on")) {
            for (String connector : List.of("cable", "internal", "half", "centre")) {
                String name = "cable_model_" + material + "_" + connector;
                CABLE_MODELS.put(material + "_" + connector, BLOCKS.register(name,
                    registryId -> new CableModelBlock(BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK,registryId)).noOcclusion().noCollision())));
            }
        }
        for(String material:List.of("data","redstone_off","redstone_on"))for(String depth:List.of("1","15","2","3","4","6")){
            String key=material+"_lead_"+depth;
            CABLE_MODELS.put(key,BLOCKS.register("cable_model_"+key,registryId ->new CableModelBlock(BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK,registryId)).noOcclusion().noCollision())));
        }
        for (String id : List.of("sapphire","sapphiredust","stoneplate","etchedplate","signallingplate","wirelessplate")) MATERIALS.put(id,ITEMS.registerSimpleItem(id));
        ITEMS.registerSimpleBlockItem(ORE); ITEMS.registerSimpleBlockItem(HAMMER);
        ITEMS.register("operator", registryId -> new ToolItem(ToolItem.Mode.OPERATOR,new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM,registryId)).stacksTo(1)));
        ITEMS.register("transceiver", registryId -> new ToolItem(ToolItem.Mode.BLOCK_LINK,new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM,registryId)).stacksTo(1)));
        ITEMS.register("entitytransceiver", registryId -> new ToolItem(ToolItem.Mode.ENTITY_LINK,new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM,registryId)).stacksTo(1)));
        ITEMS.register("wirelessstorage", registryId -> new ToolItem(ToolItem.Mode.MONITOR,new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM,registryId)).stacksTo(1)));
        ITEMS.register("plguide", registryId -> new ToolItem(ToolItem.Mode.GUIDE,new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM,registryId)).stacksTo(1)));
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.literal("Foundations PL4")).icon(() -> new ItemStack(item("sapphire"))).displayItems((parameters, output) -> ITEMS.getEntries().forEach(h -> output.accept(h.get()))).build());
    }
    public FoundationsPL4(IEventBus bus, ModContainer container) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TABS.register(bus); MENUS.register(bus);
        net.foundations.pl4.core.CoreRecipes.register(bus);
        container.registerConfig(net.foundations.pl4.config.ConfigTypeCompat.serverType(ModConfig.Type.class),PLConfig.SPEC, "foundations_pl4-server.toml");
        container.registerConfig(ModConfig.Type.CLIENT,PLClientConfig.SPEC);
        bus.addListener(PLPackets::register);
        bus.addListener(HammerEntity::capabilities);
        bus.addListener(NativeEnergyInput::register);
        PortTestInstance.registerTypes(bus);bus.addListener(PLGameTests::register);
        NeoForge.EVENT_BUS.addListener(NetworkEngine::tick);
        NeoForge.EVENT_BUS.addListener(NetworkEngine::stopped);
    }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID,path); }
    public static Item item(String id) { return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id(id)); }
}
