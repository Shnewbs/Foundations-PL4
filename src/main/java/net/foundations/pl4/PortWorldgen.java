package net.foundations.pl4;
import java.util.List;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraftforge.event.world.BiomeLoadingEvent;
/** Forge 1.18.2 requires native biome integration instead of newer biome-modifier JSON. */
public final class PortWorldgen {
 private static PlacedFeature sapphire;
 public static void register(){
  var configured=FeatureUtils.register(FoundationsPL4.id("sapphire_ore").toString(),new ConfiguredFeature<>(Feature.ORE,new OreConfiguration(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),FoundationsPL4.ORE.get().defaultBlockState(),6)));
  sapphire=PlacementUtils.register(FoundationsPL4.id("sapphire_ore").toString(),configured,List.of(CountPlacement.of(15),InSquarePlacement.spread(),HeightRangePlacement.uniform(VerticalAnchor.absolute(1),VerticalAnchor.absolute(29)),BiomeFilter.biome()));
 }
 public static void biome(BiomeLoadingEvent event){
  if(sapphire!=null&&event.getCategory()!=Biome.BiomeCategory.NETHER&&event.getCategory()!=Biome.BiomeCategory.THEEND)
   event.getGeneration().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES).add(()->sapphire);
 }
 public static PlacedFeature sapphire(){return sapphire;}
 private PortWorldgen(){}
}
