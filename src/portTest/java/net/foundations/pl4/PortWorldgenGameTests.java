package net.foundations.pl4;

import net.foundations.pl4.compat.scenarios.GameTest;
import net.foundations.pl4.compat.scenarios.GameTestHelper;
import net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.Feature;

/** Installed-world registry checks, not merely source guards for an unavailable event. */
@PrefixGameTestTemplate(false)
public final class PortWorldgenGameTests {
 @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
 public static void oreAttachedToActualWorldBiomesOnce(GameTestHelper h){
  int overworld=0,excluded=0,step=GenerationStage.Decoration.UNDERGROUND_ORES.ordinal();
  for(Biome biome:h.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY)){
   var features=biome.getGenerationSettings().features();
   long count=features.size()>step?features.get(step).stream().filter(value->PortWorldgen.isSapphire(value.get())).count():0;
   boolean allowed=biome.getBiomeCategory()!=Biome.Category.NETHER&&biome.getBiomeCategory()!=Biome.Category.THEEND;
   if(count!=(allowed?1:0))throw new AssertionError("Wrong native biome ore attachment: "+biome+" count="+count);
   PortWorldgen.attach(biome);
   if(features!=biome.getGenerationSettings().features())throw new AssertionError("Repeated attachment changed an already-correct biome");
   if(allowed)overworld++;else excluded++;
  }
  if(overworld==0||excluded==0)throw new AssertionError("Biome coverage fixture was empty");
  h.succeed();
 }
 @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
 public static void configuredOreRetainsSurvivalOutput(GameTestHelper h){
  var ore=PortWorldgen.ore();
  if(ore==null||ore.feature()!=Feature.ORE||ore.config().size!=6||!ore.config().state.equals(FoundationsPL4.ORE.get().defaultBlockState()))throw new AssertionError("Sapphire ore output or vein size changed");
  h.succeed();
 }
}
