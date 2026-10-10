package net.foundations.pl4;
import com.google.gson.*;
import net.foundations.pl4.compat.scenarios.*;
import net.foundations.pl4.compat.PortAssertions;
import net.foundations.pl4.core.ForgingRecipe;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.util.ResourceLocation;
@PrefixGameTestTemplate(false)
public final class Port115GameTests {
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void nativeOverworldSapphireAttachment(GameTestHelper h){
        PortAssertions.check(PortWorldgen.sapphire()!=null&&PortWorldgen.ore().config.size==6,"Native six-block sapphire feature must be registered");
        int eligible=0;
        for(Biome biome:ForgeRegistries.BIOMES.getValues()){
            boolean present=biome.getFeaturesForStep(GenerationStage.Decoration.UNDERGROUND_ORES).contains(PortWorldgen.sapphire());
            boolean overworld=biome.getBiomeCategory()!=Biome.Category.NETHER&&biome.getBiomeCategory()!=Biome.Category.THEEND;
            PortAssertions.check(present==overworld,"Sapphire biome attachment mismatch: "+biome.getRegistryName());
            if(overworld)eligible++;
        }
        PortAssertions.check(eligible>0,"At least one real overworld biome must be tested");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void optionalJsonFieldsRemainStrictWithoutModernCodecs(GameTestHelper h){
        var serializer=net.foundations.pl4.core.CoreRecipes.HAMMER_SERIALIZER.get();
        String base="{\"ingredient\":{\"item\":\"minecraft:diamond\"},\"result\":{\"item\":\"minecraft:emerald\",\"count\":4}}";
        ResourceLocation id=FoundationsPL4.id("strict_json_fixture");
        ForgingRecipe defaults=serializer.fromJson(id,new JsonParser().parse(base).getAsJsonObject());
        PortAssertions.check(defaults.inputCount()==1&&defaults.processingTicks()==100&&defaults.cooldownTicks()==200,"Absent fields retain native defaults");
        for(String key:new String[]{"input_count","processing_ticks","cooldown_ticks"}){
            for(String bad:new String[]{"null","true","\"1\"","1.5","-1","2147483648"}){
                JsonObject json=new JsonParser().parse(base).getAsJsonObject();json.add(key,new JsonParser().parse(bad));
                boolean rejected=false;try{serializer.fromJson(id,json);}catch(IllegalArgumentException|JsonParseException expected){rejected=true;}
                PortAssertions.check(rejected,"Malformed optional field was accepted: "+key+"="+bad);
            }
        }
        h.succeed();
    }
}
