package net.foundations.pl4;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.placement.*;
import net.minecraftforge.registries.ForgeRegistries;
/** Exact 1.15 biome-list API; configured features are not global dynamic registries yet. */
public final class PortWorldgen {
    private static ConfiguredFeature<?> sapphire;
    private static ConfiguredFeature<OreFeatureConfig> ore;
    public static void register(){
        if(sapphire!=null)return;
        ore=new ConfiguredFeature<>(Feature.ORE,new OreFeatureConfig(OreFeatureConfig.FillerBlockType.NATURAL_STONE,FoundationsPL4.ORE.get().defaultBlockState(),6));
        sapphire=new ConfiguredFeature<>(Feature.DECORATED,new DecoratedFeatureConfig(ore,new ConfiguredPlacement<>(Placement.COUNT_RANGE,new CountRangeConfig(15,1,0,30))));
        for(Biome biome:ForgeRegistries.BIOMES.getValues()){
            if(biome.getBiomeCategory()!=Biome.Category.NETHER&&biome.getBiomeCategory()!=Biome.Category.THEEND)
                biome.addFeature(GenerationStage.Decoration.UNDERGROUND_ORES,sapphire);
        }
    }
    public static ConfiguredFeature<?> sapphire(){return sapphire;}
    public static ConfiguredFeature<OreFeatureConfig> ore(){return ore;}
    private PortWorldgen(){}
}
