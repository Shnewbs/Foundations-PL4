package net.foundations.pl4;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeGenerationSettings;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.placement.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.WorldEvent;

/** Forge 33 has no BiomeLoadingEvent. Copy only its feature list at setup/world load.
 * No game classes are replaced, and no reflection or biome scanning occurs per tick.
 */
public final class PortWorldgen {
 private static ConfiguredFeature<?,?> sapphire;
 private static ConfiguredFeature<OreFeatureConfig,?> ore;
 private static Field featuresField;
 public static void register(){
  if(sapphire!=null)return;
  ore=Feature.ORE.configured(new OreFeatureConfig(OreFeatureConfig.FillerBlockType.NATURAL_STONE,FoundationsPL4.ORE.get().defaultBlockState(),6));
  sapphire=Registry.register(WorldGenRegistries.CONFIGURED_FEATURE,FoundationsPL4.id("sapphire_ore"),ore.decorated(Placement.RANGE.configured(new TopSolidRangeConfig(1,0,30))).squared().count(15));
  for(Biome biome:WorldGenRegistries.BIOME)attach(biome);
 }
 public static void world(WorldEvent.Load event){
  if(event.getWorld() instanceof ServerWorld level)
   for(Biome biome:level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY))attach(biome);
 }
 public static void attach(Biome biome){
  if(sapphire==null||biome.getBiomeCategory()==Biome.Category.NETHER||biome.getBiomeCategory()==Biome.Category.THEEND)return;
  BiomeGenerationSettings settings=biome.getGenerationSettings();
  List<List<Supplier<ConfiguredFeature<?,?>>>> current=settings.features();
  int step=GenerationStage.Decoration.UNDERGROUND_ORES.ordinal();
  if(current.size()>step&&current.get(step).stream().anyMatch(value->isSapphire(value.get())))return;
  List<List<Supplier<ConfiguredFeature<?,?>>>> copy=new ArrayList<>(current);
  while(copy.size()<=step)copy.add(Collections.emptyList());
  List<Supplier<ConfiguredFeature<?,?>>> ores=new ArrayList<>(copy.get(step));ores.add(()->sapphire);
  copy.set(step,Collections.unmodifiableList(ores));
  try{
   Field field=featuresField();
   if(field.get(settings)!=current)throw new IllegalStateException("Unexpected Forge 33 feature getter contract");
   field.set(settings,Collections.unmodifiableList(copy));
  }catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot attach PL4 ore to the actual 1.16.2 biome",failure);}
 }
 private static Field featuresField(){
  if(featuresField!=null)return featuresField;
  Field found=null;
  // A nested List is unique here; names differ between mapped and installed runtimes.
  for(Field candidate:BiomeGenerationSettings.class.getDeclaredFields()){
   if(Modifier.isStatic(candidate.getModifiers())||candidate.getType()!=List.class)continue;
   if(!(candidate.getGenericType() instanceof ParameterizedType outer))continue;
   var args=outer.getActualTypeArguments();
   if(args.length!=1||!(args[0] instanceof ParameterizedType inner)||inner.getRawType()!=List.class)continue;
   if(found!=null)throw new IllegalStateException("Ambiguous biome feature-list contract");found=candidate;
  }
  if(found==null)throw new IllegalStateException("Missing Forge 33 biome feature-list contract");
  found.setAccessible(true);featuresField=found;return found;
 }
 public static boolean isSapphire(ConfiguredFeature<?,?> feature){return feature==sapphire||FoundationsPL4.id("sapphire_ore").equals(WorldGenRegistries.CONFIGURED_FEATURE.getKey(feature));}
 public static ConfiguredFeature<OreFeatureConfig,?> ore(){return ore;}
 private PortWorldgen(){}
}
