package net.foundations.pl4.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.foundations.pl4.FoundationsPL4;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Internal replacement for PL2's DefinedRecipeHelper / RecipeOreStack dependency.
 * Uses native recipe reload/sync and ingredient tags; no Sonar Core class is loaded.
 */
public record ForgingRecipe(Ingredient ingredient,int inputCount,ItemStackTemplate resultTemplate,int processingTicks,int cooldownTicks) implements Recipe<SingleRecipeInput> {
    public ForgingRecipe {
        if(inputCount<1||inputCount>64)throw new IllegalArgumentException("Input count must be 1..64");
        if(resultTemplate.count()<1||resultTemplate.count()>64)throw new IllegalArgumentException("Result must fit one output slot");
        if(processingTicks<1||processingTicks>72000||cooldownTicks<0||cooldownTicks>72000)throw new IllegalArgumentException("Invalid forging duration");
    }
    public ForgingRecipe(Ingredient ingredient,int inputCount,ItemStack result,int processingTicks,int cooldownTicks){this(ingredient,inputCount,ItemStackTemplate.fromNonEmptyStack(result),processingTicks,cooldownTicks);}
    public ItemStack result(){return resultTemplate.create();}
    @Override public boolean matches(SingleRecipeInput input,Level level){return input.item().getCount()>=inputCount&&ingredient.test(input.item());}
    @Override public ItemStack assemble(SingleRecipeInput input){return result();}
    @Override public boolean isSpecial(){return true;}
    @Override public boolean showNotification(){return false;}
    @Override public String group(){return "";}
    @Override public PlacementInfo placementInfo(){return PlacementInfo.create(ingredient);}
    @Override public RecipeBookCategory recipeBookCategory(){return RecipeBookCategories.CRAFTING_MISC;}
    @Override public RecipeSerializer<ForgingRecipe> getSerializer(){return CoreRecipes.HAMMER_SERIALIZER.get();}
    @Override public RecipeType<ForgingRecipe> getType(){return CoreRecipes.HAMMER.get();}

    public static final class Serializer {
        private static final MapCodec<ForgingRecipe> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(ForgingRecipe::ingredient),
            Codec.intRange(1,64).optionalFieldOf("input_count",1).forGetter(ForgingRecipe::inputCount),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(ForgingRecipe::resultTemplate),
            Codec.intRange(1,72000).optionalFieldOf("processing_ticks",100).forGetter(ForgingRecipe::processingTicks),
            Codec.intRange(0,72000).optionalFieldOf("cooldown_ticks",200).forGetter(ForgingRecipe::cooldownTicks)
        ).apply(instance,ForgingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf,ForgingRecipe> STREAM=StreamCodec.of((buffer,recipe)->{
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer,recipe.ingredient());buffer.writeVarInt(recipe.inputCount());
            ItemStackTemplate.STREAM_CODEC.encode(buffer,recipe.resultTemplate());buffer.writeVarInt(recipe.processingTicks());buffer.writeVarInt(recipe.cooldownTicks());
        },buffer->new ForgingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),buffer.readVarInt(),ItemStackTemplate.STREAM_CODEC.decode(buffer),buffer.readVarInt(),buffer.readVarInt()));
        public static RecipeSerializer<ForgingRecipe> create(){return new RecipeSerializer<>(CODEC,STREAM);}
    }
}
