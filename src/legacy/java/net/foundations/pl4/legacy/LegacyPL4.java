package net.foundations.pl4.legacy;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
/** Experimental native 1.6.4 transport subset. No modern source is loaded. */
@Mod(modid=LegacyPL4.ID,name="Foundations PL4 Legacy Preview",version=LegacyPL4.VERSION,acceptedMinecraftVersions="[1.6.4]")
@cpw.mods.fml.common.network.NetworkMod(clientSideRequired=true,serverSideRequired=true,versionBounds="[0.2a-legacy-preview.3]")
public final class LegacyPL4 {
    public static final String ID="foundations_pl4",VERSION="0.2a-legacy-preview.3";
    public static LegacyBlock CABLE,EXPORT,IMPORT,FLUID_EXPORT,FLUID_IMPORT,TANK;
    public static Item FLUID_CELL,ENERGY_CELL;
    public static LegacyBlock ENERGY_EXPORT,ENERGY_IMPORT,ENERGY_BUFFER;
    public static int interval=10,itemRate=8,nodeLimit=256,slotLimit=256;
    public static boolean enabled=true,fluidsEnabled=true;
    public static int fluidRate=250;
    public static boolean energyEnabled=true; public static int energyRate=400,manualEnergy=40;
    public static java.util.logging.Logger logger;
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event){
        logger=event.getModLog();Configuration config=new Configuration(event.getSuggestedConfigurationFile());config.load();
        int cable=config.getBlock("legacyCable",3500).getInt(),export=config.getBlock("legacyExport",3501).getInt(),sink=config.getBlock("legacyImport",3502).getInt();
        int fluidExport=config.getBlock("legacyFluidExport",3503).getInt(),fluidImport=config.getBlock("legacyFluidImport",3504).getInt(),tank=config.getBlock("legacyTank",3505).getInt();
        int energyExport=config.getBlock("legacyEnergyExport",3506).getInt(),energyImport=config.getBlock("legacyEnergyImport",3507).getInt(),energyBuffer=config.getBlock("legacyEnergyBuffer",3508).getInt();
        java.util.Set<Integer> ids=new java.util.HashSet<Integer>();
        for(int id:new int[]{cable,export,sink,fluidExport,fluidImport,tank,energyExport,energyImport,energyBuffer})if(!ids.add(id))throw new IllegalStateException("PL4 legacy block IDs must be distinct");
        int cell=config.getItem("legacyFluidRecoveryCell",12500).getInt();
        int energyCell=config.getItem("legacyEnergyRecoveryCell",12501).getInt();
        if(energyCell<256||energyCell>Item.itemsList.length-257||energyCell==cell||Item.itemsList[energyCell+256]!=null||ids.contains(energyCell+256)||ids.contains(cell+256))throw new IllegalStateException("PL4 energy/fluid recovery item IDs must be free and distinct from block IDs");
        if(cell<256||cell+256>=Item.itemsList.length||Item.itemsList[cell+256]!=null)throw new IllegalStateException("PL4 recovery item ID unavailable; choose a free legacyFluidRecoveryCell ID");
        for(int id:new int[]{cable,export,sink,fluidExport,fluidImport,tank,energyExport,energyImport,energyBuffer})if(id<256||id>=Block.blocksList.length||Block.blocksList[id]!=null)throw new IllegalStateException("PL4 block ID unavailable: "+id+"; choose free IDs in the config");
        enabled=config.get("network","transfersEnabled",true).getBoolean(true);
        interval=bound(config.get("network","intervalTicks",10).getInt(),1,200);
        itemRate=bound(config.get("network","itemsPerOperation",8).getInt(),1,64);
        nodeLimit=bound(config.get("network","maxNetworkHosts",256).getInt(),1,4096);
        slotLimit=bound(config.get("network","maxInventorySlots",256).getInt(),1,4096);fluidsEnabled=config.get("network","fluidsEnabled",true).getBoolean(true);fluidRate=bound(config.get("network","fluidMilliBucketsPerOperation",250).getInt(),1,16000);energyEnabled=config.get("network","energyEnabled",true).getBoolean(true);energyRate=bound(config.get("network","energyPerOperation",400).getInt(),1,100000);manualEnergy=bound(config.get("network","manualEnergyPerCrank",40).getInt(),0,400);config.save();
        CABLE=new LegacyBlock(cable,"legacy_cable",0);EXPORT=new LegacyBlock(export,"legacy_export",1);IMPORT=new LegacyBlock(sink,"legacy_import",2);
        GameRegistry.registerBlock(CABLE,"legacy_cable");GameRegistry.registerBlock(EXPORT,"legacy_export");GameRegistry.registerBlock(IMPORT,"legacy_import");
        FLUID_EXPORT=new LegacyBlock(fluidExport,"legacy_fluid_export",3);FLUID_EXPORT.setTextureName("lapis_block");
        FLUID_IMPORT=new LegacyBlock(fluidImport,"legacy_fluid_import",4);FLUID_IMPORT.setTextureName("emerald_block");
        TANK=new LegacyBlock(tank,"legacy_tank",5);TANK.setTextureName("glass");
        FLUID_CELL=new LegacyFluidCell(cell);
        ENERGY_EXPORT=new LegacyBlock(energyExport,"legacy_energy_export",6);ENERGY_EXPORT.setTextureName("gold_block");
        ENERGY_IMPORT=new LegacyBlock(energyImport,"legacy_energy_import",7);ENERGY_IMPORT.setTextureName("redstone_block");
        ENERGY_BUFFER=new LegacyBlock(energyBuffer,"legacy_energy_buffer",8);ENERGY_BUFFER.setTextureName("iron_block");
        ENERGY_CELL=new LegacyEnergyCell(energyCell);
        GameRegistry.registerBlock(ENERGY_EXPORT,"legacy_energy_export");GameRegistry.registerBlock(ENERGY_IMPORT,"legacy_energy_import");GameRegistry.registerBlock(ENERGY_BUFFER,"legacy_energy_buffer");GameRegistry.registerItem(ENERGY_CELL,"legacy_energy_cell");
        LanguageRegistry.addName(ENERGY_EXPORT,"PL4 Legacy Energy Export Node");LanguageRegistry.addName(ENERGY_IMPORT,"PL4 Legacy Energy Import Node");LanguageRegistry.addName(ENERGY_BUFFER,"PL4 Legacy Energy Buffer");LanguageRegistry.addName(ENERGY_CELL,"PL4 Sealed Energy Recovery Cell");
        GameRegistry.registerBlock(FLUID_EXPORT,"legacy_fluid_export");GameRegistry.registerBlock(FLUID_IMPORT,"legacy_fluid_import");GameRegistry.registerBlock(TANK,"legacy_tank");GameRegistry.registerItem(FLUID_CELL,"legacy_fluid_cell");
        LanguageRegistry.addName(FLUID_EXPORT,"PL4 Legacy Fluid Export Node");LanguageRegistry.addName(FLUID_IMPORT,"PL4 Legacy Fluid Import Node");LanguageRegistry.addName(TANK,"PL4 Legacy Tank (16 buckets)");LanguageRegistry.addName(FLUID_CELL,"PL4 Sealed Fluid Recovery Cell");
        GameRegistry.registerTileEntity(LegacyTile.class,"foundations_pl4.legacy_host");MinecraftForge.EVENT_BUS.register(this);
        LanguageRegistry.addName(CABLE,"PL4 Legacy Data Cable");LanguageRegistry.addName(EXPORT,"PL4 Legacy Export Node");LanguageRegistry.addName(IMPORT,"PL4 Legacy Import Node");
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event){
        GameRegistry.addRecipe(new ItemStack(CABLE,8),"IRI",'I',Item.ingotIron,'R',Item.redstone);
        GameRegistry.addRecipe(new ItemStack(EXPORT),"HCH",'H',Block.hopperBlock,'C',CABLE);
        GameRegistry.addShapelessRecipe(new ItemStack(IMPORT),EXPORT);GameRegistry.addShapelessRecipe(new ItemStack(EXPORT),IMPORT);
        GameRegistry.addRecipe(new ItemStack(ENERGY_BUFFER),"IRI","RCR","IRI",'I',Item.ingotIron,'R',Item.redstone,'C',CABLE);
        GameRegistry.addShapelessRecipe(new ItemStack(ENERGY_EXPORT),EXPORT,Item.redstone);
        GameRegistry.addShapelessRecipe(new ItemStack(ENERGY_IMPORT),ENERGY_EXPORT);GameRegistry.addShapelessRecipe(new ItemStack(ENERGY_EXPORT),ENERGY_IMPORT);
        GameRegistry.addRecipe(new ItemStack(TANK),"IGI","G G","IGI",'I',Item.ingotIron,'G',Block.glass);
        GameRegistry.addShapelessRecipe(new ItemStack(FLUID_EXPORT),EXPORT,Item.bucketEmpty);
        GameRegistry.addShapelessRecipe(new ItemStack(FLUID_IMPORT),FLUID_EXPORT);GameRegistry.addShapelessRecipe(new ItemStack(FLUID_EXPORT),FLUID_IMPORT);
    }
    @ForgeSubscribe public void protect(BlockEvent.BreakEvent event){if(event.world.getBlockTileEntity(event.x,event.y,event.z) instanceof LegacyTile&&!((LegacyTile)event.world.getBlockTileEntity(event.x,event.y,event.z)).canEdit(event.getPlayer()))event.setCanceled(true);}
    @Mod.EventHandler public void starting(FMLServerStartingEvent event){event.registerServerCommand(new LegacyCommands());}
    private static int bound(int value,int min,int max){return Math.max(min,Math.min(max,value));}
}
