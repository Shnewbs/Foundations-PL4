package net.foundations.pl4;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.placement.*;
import net.minecraftforge.event.world.BiomeLoadingEvent;
/** Native world generation; this target cannot consume newer biome-modifier JSON. */
public final class PortWorldgen {
 private static ConfiguredFeature<?,?> sapphire;
 public static void register(){
  sapphire=Registry.register(WorldGenRegistries.CONFIGURED_FEATURE,FoundationsPL4.id("sapphire_ore"),Feature.ORE.configured(new OreFeatureConfig(OreFeatureConfig.FillerBlockType.NATURAL_STONE,FoundationsPL4.ORE.get().defaultBlockState(),6))
    .decorated(Placement.RANGE.configured(new TopSolidRangeConfig(1,0,30))).squared().count(15));
 }
 public static void biome(BiomeLoadingEvent event){
  if(sapphire!=null&&event.getCategory()!=Biome.Category.NETHER&&event.getCategory()!=Biome.Category.THEEND)
    event.getGeneration().getFeatures(GenerationStage.Decoration.UNDERGROUND_ORES).add(()->sapphire);
 }
 private PortWorldgen(){}
}
