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
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Internal replacement for PL2's DefinedRecipeHelper / RecipeOreStack dependency.
 * Uses 1.21.1 recipe reload/sync and ingredient tags; no Sonar Core class is loaded.
 */
public record ForgingRecipe(Ingredient ingredient,int inputCount,ItemStack result,int processingTicks,int cooldownTicks) implements Recipe<SingleRecipeInput> {
    public ForgingRecipe {
        if(inputCount<1||inputCount>64)throw new IllegalArgumentException("Input count must be 1..64");
        if(result.isEmpty()||result.getCount()>result.getMaxStackSize())throw new IllegalArgumentException("Result must fit one output slot");
        if(processingTicks<1||processingTicks>72000||cooldownTicks<0||cooldownTicks>72000)throw new IllegalArgumentException("Invalid forging duration");
        result=result.copy();
    }
    @Override public ItemStack result(){return result.copy();}
    @Override public boolean matches(SingleRecipeInput input,Level level){return input.item().getCount()>=inputCount&&ingredient.test(input.item());}
    @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries){return result.copy();}
    @Override public ItemStack getResultItem(HolderLookup.Provider registries){return result.copy();}
    @Override public NonNullList<Ingredient> getIngredients(){return NonNullList.of(Ingredient.EMPTY,ingredient);}
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=1;}
    @Override public boolean isSpecial(){return true;}
    @Override public ItemStack getToastSymbol(){return new ItemStack(FoundationsPL4.HAMMER.get());}
    @Override public RecipeSerializer<?> getSerializer(){return CoreRecipes.HAMMER_SERIALIZER.get();}
    @Override public RecipeType<?> getType(){return CoreRecipes.HAMMER.get();}

    public static final class Serializer implements RecipeSerializer<ForgingRecipe> {
        private static final MapCodec<ForgingRecipe> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ForgingRecipe::ingredient),
            Codec.intRange(1,64).optionalFieldOf("input_count",1).forGetter(ForgingRecipe::inputCount),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ForgingRecipe::result),
            Codec.intRange(1,72000).optionalFieldOf("processing_ticks",100).forGetter(ForgingRecipe::processingTicks),
            Codec.intRange(0,72000).optionalFieldOf("cooldown_ticks",200).forGetter(ForgingRecipe::cooldownTicks)
        ).apply(instance,ForgingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf,ForgingRecipe> STREAM=StreamCodec.of((buffer,recipe)->{
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer,recipe.ingredient());buffer.writeVarInt(recipe.inputCount());
            ItemStack.STREAM_CODEC.encode(buffer,recipe.result());buffer.writeVarInt(recipe.processingTicks());buffer.writeVarInt(recipe.cooldownTicks());
        },buffer->new ForgingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),buffer.readVarInt(),ItemStack.STREAM_CODEC.decode(buffer),buffer.readVarInt(),buffer.readVarInt()));
        @Override public MapCodec<ForgingRecipe> codec(){return CODEC;}
        @Override public StreamCodec<RegistryFriendlyByteBuf,ForgingRecipe> streamCodec(){return STREAM;}
    }
}
