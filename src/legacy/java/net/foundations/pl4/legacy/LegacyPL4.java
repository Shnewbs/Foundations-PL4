package net.foundations.pl4.legacy;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
/** Initial native transport subset. Advanced 0.2a display/multipart parity is separate work. */
@Mod(modid=LegacyPL4.ID,name="Foundations PL4",version=LegacyPL4.VERSION,acceptedMinecraftVersions="[1.12.2]",acceptableRemoteVersions="0.2a-legacy-preview.3")
@Mod.EventBusSubscriber(modid=LegacyPL4.ID)
public final class LegacyPL4 {
    public static final String ID="foundations_pl4",VERSION="0.2a-legacy-preview.3";
    public static final LegacyBlock CABLE=new LegacyBlock("legacy_cable",0),EXPORT=new LegacyBlock("legacy_export",1),IMPORT=new LegacyBlock("legacy_import",2);
    public static final LegacyBlock FLUID_EXPORT=new LegacyBlock("legacy_fluid_export",3),FLUID_IMPORT=new LegacyBlock("legacy_fluid_import",4),TANK=new LegacyBlock("legacy_tank",5);
    public static final Item FLUID_CELL=new Item().setRegistryName(ID,"legacy_fluid_cell").setUnlocalizedName(ID+".legacy_fluid_cell").setMaxStackSize(1);
    public static final LegacyBlock ENERGY_EXPORT=new LegacyBlock("legacy_energy_export",6),ENERGY_IMPORT=new LegacyBlock("legacy_energy_import",7),ENERGY_BUFFER=new LegacyBlock("legacy_energy_buffer",8);
    public static final Item ENERGY_CELL=new Item().setRegistryName(ID,"legacy_energy_cell").setUnlocalizedName(ID+".legacy_energy_cell").setMaxStackSize(1);
    public static final LegacyBlock[] BLOCKS={CABLE,EXPORT,IMPORT,FLUID_EXPORT,FLUID_IMPORT,TANK,ENERGY_EXPORT,ENERGY_IMPORT,ENERGY_BUFFER};
    public static int interval=10,itemRate=8,nodeLimit=256,slotLimit=256;
    public static boolean enabled=true,fluidsEnabled=true;
    public static int fluidRate=250;
    public static boolean energyEnabled=true; public static int energyRate=400,manualEnergy=40;
    public static org.apache.logging.log4j.Logger logger;
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event){
        logger=event.getModLog();Configuration config=new Configuration(event.getSuggestedConfigurationFile());config.load();
        enabled=config.getBoolean("transfersEnabled","network",true,"Enable native legacy item transport.");
        interval=config.getInt("intervalTicks","network",10,1,200,"Transfer cadence; lower values cost more server work.");
        itemRate=config.getInt("itemsPerOperation","network",8,1,64,"Maximum items moved by each exporter per operation.");
        nodeLimit=config.getInt("maxNetworkHosts","network",256,1,4096,"Oversized networks stop instead of scanning without a bound.");
        slotLimit=config.getInt("maxInventorySlots","network",256,1,4096,"Per-inventory scan limit.");fluidsEnabled=config.getBoolean("fluidsEnabled","network",true,"Enable native fluid transport.");
        fluidRate=config.getInt("fluidMilliBucketsPerOperation","network",250,1,16000,"Fluid rate per exporter per operation.");energyEnabled=config.getBoolean("energyEnabled","network",true,"Enable Forge Energy transport and buffer IO.");
        energyRate=config.getInt("energyPerOperation","network",400,1,100000,"Maximum FE per exporter per operation.");
        manualEnergy=config.getInt("manualEnergyPerCrank","network",40,0,400,"FE per manual crank; zero disables. Cooldown is 10 ticks.");config.save();
        GameRegistry.registerTileEntity(LegacyTile.class,new ResourceLocation(ID,"legacy_host"));
    }
    @SubscribeEvent public static void blocks(RegistryEvent.Register<Block> event){event.getRegistry().registerAll(BLOCKS);}
    @SubscribeEvent public static void items(RegistryEvent.Register<Item> event){event.getRegistry().register(ENERGY_CELL);event.getRegistry().register(FLUID_CELL);for(LegacyBlock block:BLOCKS)event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));}
    @SubscribeEvent public static void protection(BlockEvent.BreakEvent event){
        if(event.getWorld().getTileEntity(event.getPos()) instanceof LegacyTile){LegacyTile tile=(LegacyTile)event.getWorld().getTileEntity(event.getPos());if(!tile.canEdit(event.getPlayer()))event.setCanceled(true);}
    }
    @Mod.EventHandler public void starting(FMLServerStartingEvent event){event.registerServerCommand(new LegacyCommands());}
    private static final ResourceLocation GROUP=new ResourceLocation(ID,"legacy_transport");
    @SubscribeEvent public static void recipes(RegistryEvent.Register<net.minecraft.item.crafting.IRecipe> event){
        event.getRegistry().register(new net.minecraftforge.oredict.ShapedOreRecipe(GROUP,new ItemStack(CABLE,8),"IRI",'I',Items.IRON_INGOT,'R',Items.REDSTONE).setRegistryName(ID,"legacy_cable"));
        event.getRegistry().register(new net.minecraftforge.oredict.ShapedOreRecipe(GROUP,new ItemStack(EXPORT),"HCH",'H',Blocks.HOPPER,'C',CABLE).setRegistryName(ID,"legacy_export"));
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(GROUP,new ItemStack(IMPORT),EXPORT).setRegistryName(ID,"legacy_import"));
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(GROUP,new ItemStack(EXPORT),IMPORT).setRegistryName(ID,"legacy_export_return"));

        event.getRegistry().register(new net.minecraftforge.oredict.ShapedOreRecipe(GROUP,new ItemStack(TANK),"IGI","G G","IGI",'I',Items.IRON_INGOT,'G',Blocks.GLASS).setRegistryName(ID,"legacy_tank"));
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(GROUP,new ItemStack(FLUID_EXPORT),EXPORT,Items.BUCKET).setRegistryName(ID,"legacy_fluid_export"));
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(GROUP,new ItemStack(FLUID_IMPORT),FLUID_EXPORT).setRegistryName(ID,"legacy_fluid_import"));
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(GROUP,new ItemStack(FLUID_EXPORT),FLUID_IMPORT).setRegistryName(ID,"legacy_fluid_export_return"));
        event.getRegistry().register(new net.minecraftforge.oredict.ShapedOreRecipe(GROUP,new ItemStack(ENERGY_BUFFER),"IRI","RCR","IRI",'I',Items.IRON_INGOT,'R',Items.REDSTONE,'C',CABLE).setRegistryName(ID,"legacy_energy_buffer"));
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(GROUP,new ItemStack(ENERGY_EXPORT),EXPORT,Items.REDSTONE).setRegistryName(ID,"legacy_energy_export"));
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(GROUP,new ItemStack(ENERGY_IMPORT),ENERGY_EXPORT).setRegistryName(ID,"legacy_energy_import"));
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(GROUP,new ItemStack(ENERGY_EXPORT),ENERGY_IMPORT).setRegistryName(ID,"legacy_energy_export_return"));
    }
}
