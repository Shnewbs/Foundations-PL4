package net.foundations.pl4;
import net.minecraft.core.*;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.foundations.pl4.compat.PortAssertions;
@PrefixGameTestTemplate(false)
public final class PortWorldgenGameTests {
 @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
 public static void sapphireIsPresentInOverworldBiomes(GameTestHelper h){
  var registry=h.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
  PortAssertions.check(hasOre(registry.getOrThrow(Biomes.PLAINS)),"Sapphire must actually be attached to plains biomes");
  PortAssertions.check(!hasOre(registry.getOrThrow(Biomes.NETHER_WASTES)),"Nether must not get overworld sapphire placement");
  PortAssertions.check(!hasOre(registry.getOrThrow(Biomes.THE_END)),"End must not get overworld sapphire placement");h.succeed();
 }
 private static boolean hasOre(Biome biome){return biome.getGenerationSettings().features().stream().flatMap(set->set.stream()).anyMatch(feature->feature.get()==PortWorldgen.sapphire());}
 @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
 public static void sapphireOreConfigurationIsNative(GameTestHelper h){
  var configured=BuiltinRegistries.CONFIGURED_FEATURE.get(FoundationsPL4.id("sapphire_ore"));
  PortAssertions.check(configured!=null&&configured==PortWorldgen.sapphire(),"Native decorated sapphire feature must exist");
  PortAssertions.check(configured.getFeatures().anyMatch(feature->feature.config instanceof OreConfiguration&&((OreConfiguration)feature.config).size==6&&((OreConfiguration)feature.config).targetStates.stream().anyMatch(state->state.state.is(FoundationsPL4.ORE.get()))),"Sapphire vein size and actual output block must match");h.succeed();
 }
}
