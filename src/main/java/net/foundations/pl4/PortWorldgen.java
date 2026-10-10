package net.foundations.pl4;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.placement.*;
/** 1.16.1 attaches configured ore directly during deferred common setup. */
public final class PortWorldgen {
 private static ConfiguredFeature<?,?> sapphire;
 public static void register(){
  if(sapphire!=null)return;
  sapphire=Feature.ORE.configured(new OreFeatureConfig(OreFeatureConfig.FillerBlockType.NATURAL_STONE,FoundationsPL4.ORE.get().defaultBlockState(),6))
    .decorated(Placement.COUNT_RANGE.configured(new CountRangeConfig(15,1,0,30)));
  for(Biome biome:net.minecraftforge.registries.ForgeRegistries.BIOMES.getValues())
   if(biome.getBiomeCategory()!=Biome.Category.NETHER&&biome.getBiomeCategory()!=Biome.Category.THEEND)
    biome.addFeature(GenerationStage.Decoration.UNDERGROUND_ORES,sapphire);
 }
 public static ConfiguredFeature<?,?> sapphire(){return sapphire;}
 private PortWorldgen(){}
}
