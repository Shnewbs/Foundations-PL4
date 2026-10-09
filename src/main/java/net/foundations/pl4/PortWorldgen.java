package net.foundations.pl4;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraftforge.event.world.BiomeLoadingEvent;
/** Native 1.17 configured-feature decorations; no newer placed-feature or biome-modifier JSON. */
public final class PortWorldgen {
 private static ConfiguredFeature<?,?> sapphire;
 public static void register(){
  sapphire=Registry.register(BuiltinRegistries.CONFIGURED_FEATURE,FoundationsPL4.id("sapphire_ore"),
   Feature.ORE.configured(new OreConfiguration(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),FoundationsPL4.ORE.get().defaultBlockState(),6))
    .rangeUniform(VerticalAnchor.absolute(1),VerticalAnchor.absolute(29)).squared().count(15));
 }
 public static void biome(BiomeLoadingEvent event){
  if(sapphire!=null&&event.getCategory()!=Biome.BiomeCategory.NETHER&&event.getCategory()!=Biome.BiomeCategory.THEEND)
   event.getGeneration().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES).add(()->sapphire);
 }
 public static ConfiguredFeature<?,?> sapphire(){return sapphire;}
 private PortWorldgen(){}
}
