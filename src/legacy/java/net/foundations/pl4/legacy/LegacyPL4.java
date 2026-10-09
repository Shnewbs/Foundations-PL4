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
@cpw.mods.fml.common.network.NetworkMod(clientSideRequired=true,serverSideRequired=true,versionBounds="[0.2a-legacy-preview.1]")
public final class LegacyPL4 {
    public static final String ID="foundations_pl4",VERSION="0.2a-legacy-preview.1";
    public static LegacyBlock CABLE,EXPORT,IMPORT;
    public static int interval=10,itemRate=8,nodeLimit=256,slotLimit=256;
    public static boolean enabled=true;
    public static java.util.logging.Logger logger;
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event){
        logger=event.getModLog();Configuration config=new Configuration(event.getSuggestedConfigurationFile());config.load();
        int cable=config.getBlock("legacyCable",3500).getInt(),export=config.getBlock("legacyExport",3501).getInt(),sink=config.getBlock("legacyImport",3502).getInt();
        if(cable==export||cable==sink||export==sink)throw new IllegalStateException("PL4 legacy block IDs must be distinct");
        for(int id:new int[]{cable,export,sink})if(id<256||id>=Block.blocksList.length||Block.blocksList[id]!=null)throw new IllegalStateException("PL4 block ID unavailable: "+id+"; choose free IDs in the config");
        enabled=config.get("network","transfersEnabled",true).getBoolean(true);
        interval=bound(config.get("network","intervalTicks",10).getInt(),1,200);
        itemRate=bound(config.get("network","itemsPerOperation",8).getInt(),1,64);
        nodeLimit=bound(config.get("network","maxNetworkHosts",256).getInt(),1,4096);
        slotLimit=bound(config.get("network","maxInventorySlots",256).getInt(),1,4096);config.save();
        CABLE=new LegacyBlock(cable,"legacy_cable",0);EXPORT=new LegacyBlock(export,"legacy_export",1);IMPORT=new LegacyBlock(sink,"legacy_import",2);
        GameRegistry.registerBlock(CABLE,"legacy_cable");GameRegistry.registerBlock(EXPORT,"legacy_export");GameRegistry.registerBlock(IMPORT,"legacy_import");
        GameRegistry.registerTileEntity(LegacyTile.class,"foundations_pl4.legacy_host");MinecraftForge.EVENT_BUS.register(this);
        LanguageRegistry.addName(CABLE,"PL4 Legacy Data Cable");LanguageRegistry.addName(EXPORT,"PL4 Legacy Export Node");LanguageRegistry.addName(IMPORT,"PL4 Legacy Import Node");
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event){
        GameRegistry.addRecipe(new ItemStack(CABLE,8),"IRI",'I',Item.ingotIron,'R',Item.redstone);
        GameRegistry.addRecipe(new ItemStack(EXPORT),"HCH",'H',Block.hopperBlock,'C',CABLE);
        GameRegistry.addShapelessRecipe(new ItemStack(IMPORT),EXPORT);GameRegistry.addShapelessRecipe(new ItemStack(EXPORT),IMPORT);
    }
    @ForgeSubscribe public void protect(BlockEvent.BreakEvent event){if(event.world.getBlockTileEntity(event.x,event.y,event.z) instanceof LegacyTile&&!((LegacyTile)event.world.getBlockTileEntity(event.x,event.y,event.z)).canEdit(event.getPlayer()))event.setCanceled(true);}
    @Mod.EventHandler public void starting(FMLServerStartingEvent event){event.registerServerCommand(new LegacyCommands());}
    private static int bound(int value,int min,int max){return Math.max(min,Math.min(max,value));}
}
