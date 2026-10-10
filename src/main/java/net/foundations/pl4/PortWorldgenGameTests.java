package net.foundations.pl4;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.foundations.pl4.compat.PortAssertions;
public final class PortWorldgenGameTests {
 @GameTest(template="foundations_pl4:empty")
 public static void sapphireIsPresentInOverworldBiomes(GameTestHelper h){
  var registry=h.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
  var plains=registry.getOrThrow(Biomes.PLAINS);
  PortAssertions.check(hasOre(plains),"Sapphire placement must actually be attached to plains biomes");
  PortAssertions.check(!hasOre(registry.getOrThrow(Biomes.NETHER_WASTES)),"Nether must not get overworld sapphire placement");
  PortAssertions.check(!hasOre(registry.getOrThrow(Biomes.THE_END)),"End must not get overworld sapphire placement");h.succeed();
 }
 private static boolean hasOre(Biome biome){return biome.getGenerationSettings().features().stream().flatMap(set->set.stream()).anyMatch(holder->holder.get()==PortWorldgen.sapphire());}
 @GameTest(template="foundations_pl4:empty")
 public static void sapphireOreConfigurationIsNative(GameTestHelper h){
  var registry=h.getLevel().registryAccess().registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY);
  var configured=registry.get(FoundationsPL4.id("sapphire_ore"));
  PortAssertions.check(configured!=null&&configured.config() instanceof OreConfiguration,"Native configured sapphire feature must exist");
  OreConfiguration ore=(OreConfiguration)configured.config();
  PortAssertions.check(ore.size==6&&ore.targetStates.size()==1&&ore.targetStates.get(0).state.is(FoundationsPL4.ORE.get()),"Sapphire vein size and output must match the actual ore block");h.succeed();
 }
}
