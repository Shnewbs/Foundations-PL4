package net.foundations.pl4;
import net.minecraftforge.fml.RegistryObject;

import java.util.*;
import net.foundations.pl4.compat.Registries;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.container.ContainerType;

import net.minecraft.block.material.PushReaction;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.block.Block;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.*;

@Mod(FoundationsPL4.ID)
public final class FoundationsPL4 {
    public static final String ID = "foundations_pl4";
    public static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(net.minecraftforge.registries.ForgeRegistries.BLOCKS,ID);
    public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(net.minecraftforge.registries.ForgeRegistries.ITEMS,ID);
    public static final DeferredRegister<TileEntityType<?>> ENTITIES = new DeferredRegister<>(net.minecraftforge.registries.ForgeRegistries.TILE_ENTITIES,ID);
    public static final DeferredRegister<ContainerType<?>> MENUS = new DeferredRegister<>(net.minecraftforge.registries.ForgeRegistries.CONTAINERS,ID);
    public static final RegistryObject<ContainerType<HammerMenu>> HAMMER_MENU = MENUS.register("hammer", () -> new ContainerType<>(HammerMenu::new));
    public static final ItemGroup TAB=new ItemGroup("foundations_pl4"){
        @Override public ItemStack makeIcon(){return new ItemStack(item("sapphire"));}
        @Override public void fillItemList(net.minecraft.util.NonNullList<ItemStack> items){ITEMS.getEntries().forEach(entry->items.add(new ItemStack(entry.get())));}
    };
    public static final EnumMap<Kind, RegistryObject<PartModelBlock>> MODELS = new EnumMap<>(Kind.class);
    public static final EnumMap<Kind, RegistryObject<PartItem>> PART_ITEMS = new EnumMap<>(Kind.class);
    public static final Map<String,RegistryObject<CableModelBlock>> CABLE_MODELS = new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> MATERIALS = new LinkedHashMap<>();
    public static final RegistryObject<PartModelBlock> KINETIC_READER_MODEL = BLOCKS.register("kinetic_reader_model", () -> new PartModelBlock(Kind.ENERGY_READER,Block.Properties.of(net.minecraft.block.material.Material.STONE).noCollission()));
    public static final RegistryObject<PartModelBlock> AE2_READER_MODEL = BLOCKS.register("ae2_reader_model", () -> new PartModelBlock(Kind.ENERGY_READER,Block.Properties.of(net.minecraft.block.material.Material.STONE).noCollission()));
    public static final RegistryObject<HostBlock> HOST = BLOCKS.register("multipart_host", () -> new HostBlock(Block.Properties.of(net.minecraft.block.material.Material.STONE,MaterialColor.COLOR_BLUE).strength(.5F,25).dynamicShape()));
    public static final RegistryObject<LargeDisplayModelBlock> LARGE_MODEL = BLOCKS.register("large_display_model", () -> new LargeDisplayModelBlock(Block.Properties.of(net.minecraft.block.material.Material.STONE).noCollission()));
    public static final RegistryObject<Block> ORE = BLOCKS.register("sapphireore", () -> new Block(Block.Properties.of(net.minecraft.block.material.Material.STONE,MaterialColor.STONE).strength(3,5).harvestTool(net.minecraftforge.common.ToolType.PICKAXE).harvestLevel(2).harvestTool(net.minecraftforge.common.ToolType.PICKAXE).harvestLevel(2)));
    public static final RegistryObject<HammerBlock> HAMMER = BLOCKS.register("hammer", () -> new HammerBlock(Block.Properties.of(net.minecraft.block.material.Material.STONE,MaterialColor.WOOD).strength(2.5F)));
    public static final RegistryObject<HammerSpaceBlock> HAMMER_SPACE = BLOCKS.register("hammer_air", () -> new HammerSpaceBlock(Block.Properties.of(net.minecraft.block.material.Material.STONE,MaterialColor.WOOD).strength(2.5F)));
    public static final RegistryObject<TileEntityType<HostEntity>> HOST_ENTITY = ENTITIES.register("host", () -> TileEntityType.Builder.of(HostEntity::new,HOST.get()).build(null));
    public static final RegistryObject<TileEntityType<HammerEntity>> HAMMER_ENTITY = ENTITIES.register("hammer", () -> TileEntityType.Builder.of(HammerEntity::new,HAMMER.get()).build(null));
    static {
        for (Kind k : Kind.values()) {
            MODELS.put(k,BLOCKS.register(k.id, () -> new PartModelBlock(k,Block.Properties.of(net.minecraft.block.material.Material.STONE).strength(.5F))));
            PART_ITEMS.put(k,ITEMS.register(k.id, () -> new PartItem(k,new Item.Properties())));
        }
        for (String material : List.of("data", "redstone_off", "redstone_on")) {
            for (String connector : List.of("cable", "internal", "half", "centre")) {
                String name = "cable_model_" + material + "_" + connector;
                CABLE_MODELS.put(material + "_" + connector, BLOCKS.register(name,
                    () -> new CableModelBlock(Block.Properties.of(net.minecraft.block.material.Material.STONE).noCollission())));
            }
        }
        for(String material:List.of("data","redstone_off","redstone_on"))for(String depth:List.of("1","15","2","3","4","6")){
            String key=material+"_lead_"+depth;
            CABLE_MODELS.put(key,BLOCKS.register("cable_model_"+key,()->new CableModelBlock(Block.Properties.of(net.minecraft.block.material.Material.STONE).noCollission())));
        }
        for (String id : List.of("sapphire","sapphiredust","stoneplate","etchedplate","signallingplate","wirelessplate")) MATERIALS.put(id,ITEMS.register(id, () -> new Item(new Item.Properties())));
        ITEMS.register("sapphireore", () -> new BlockItem(ORE.get(),new Item.Properties())); ITEMS.register("hammer", () -> new BlockItem(HAMMER.get(),new Item.Properties()));
        ITEMS.register("operator", () -> new ToolItem(ToolItem.Mode.OPERATOR,new Item.Properties().maxStackSize(1)));
        ITEMS.register("transceiver", () -> new ToolItem(ToolItem.Mode.BLOCK_LINK,new Item.Properties().maxStackSize(1)));
        ITEMS.register("entitytransceiver", () -> new ToolItem(ToolItem.Mode.ENTITY_LINK,new Item.Properties().maxStackSize(1)));
        ITEMS.register("wirelessstorage", () -> new ToolItem(ToolItem.Mode.STORAGE,new Item.Properties().maxStackSize(1)));
        ITEMS.register("plguide", () -> new ToolItem(ToolItem.Mode.GUIDE,new Item.Properties().maxStackSize(1)));
    }
    public FoundationsPL4() {
        IEventBus bus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        var container=net.minecraftforge.fml.ModLoadingContext.get();
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
        net.foundations.pl4.core.CoreRecipes.register(bus);
        container.registerConfig(ModConfig.Type.SERVER,PLConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT,PLClientConfig.SPEC);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e)->{
            PLPackets.register(new net.foundations.pl4.compat.RegisterPayloadHandlersEvent());
            net.minecraftforge.fml.DeferredWorkQueue.runLater(PortWorldgen::register);
            var caps=new net.foundations.pl4.compat.RegisterCapabilitiesEvent();
            HammerEntity.capabilities(caps);NativeEnergyInput.register(caps);
        });
        MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.tileentity.TileEntity.class,net.foundations.pl4.compat.PortCapabilities::attach);
        
        MinecraftForge.EVENT_BUS.addListener(NetworkEngine::tick);
        MinecraftForge.EVENT_BUS.addListener(NetworkEngine::stopped);
    }
    public static ResourceLocation id(String path) { return new ResourceLocation(ID,path); }
    public static Item item(String id) { return net.minecraft.util.registry.Registry.ITEM.get(id(id)); }
}
