package net.foundations.pl4.core;

import com.google.gson.*;
import net.foundations.pl4.FoundationsPL4;
import net.foundations.pl4.compat.SingleRecipeInput;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.network.PacketBuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.world.World;

/** Native Forge-31 JSON/network recipe contract; no newer Mojang codec dependency. */
public record ForgingRecipe(Ingredient ingredient,int inputCount,ItemStack result,int processingTicks,int cooldownTicks,ResourceLocation id) implements IRecipe<SingleRecipeInput> {
    public ForgingRecipe(Ingredient ingredient,int inputCount,ItemStack result,int processingTicks,int cooldownTicks){this(ingredient,inputCount,result,processingTicks,cooldownTicks,FoundationsPL4.id("unregistered"));}
    public ForgingRecipe {
        if(ingredient==null||ingredient.isEmpty()||inputCount<1||inputCount>64||result==null||result.isEmpty()||result.getCount()>result.getMaxStackSize()
           ||processingTicks<1||processingTicks>72000||cooldownTicks<0||cooldownTicks>72000||id==null)throw new IllegalArgumentException("Invalid forging recipe");
        result=result.copy();
    }
    @Override public ItemStack result(){return result.copy();}
    public ForgingRecipe value(){return this;}
    @Override public ResourceLocation getId(){return id;}
    @Override public boolean matches(SingleRecipeInput input,World level){return input.item().getCount()>=inputCount&&ingredient.test(input.item());}
    @Override public ItemStack assemble(SingleRecipeInput input){return result.copy();}
    public ItemStack assemble(SingleRecipeInput input,Object registry){return result.copy();}
    @Override public ItemStack getResultItem(){return result.copy();}
    public ItemStack getResultItem(Object registry){return result.copy();}
    @Override public NonNullList<Ingredient> getIngredients(){return NonNullList.of(Ingredient.EMPTY,ingredient);}
    @Override public boolean canCraftInDimensions(int width,int height){return width>0&&height>0;}
    @Override public boolean isSpecial(){return true;}
    @Override public ItemStack getToastSymbol(){return new ItemStack(FoundationsPL4.HAMMER.get());}
    @Override public IRecipeSerializer<?> getSerializer(){return CoreRecipes.HAMMER_SERIALIZER.get();}
    @Override public IRecipeType<?> getType(){return CoreRecipes.HAMMER.get();}
    public static final class Serializer extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ForgingRecipe> {
        /** Missing is a default; malformed, fractional, null, wrong-type and out-of-range are errors. */
        public static int integer(JsonObject object,String key,int fallback,int min,int max){
            if(!object.has(key))return fallback;
            JsonElement raw=object.get(key);
            if(raw==null||!raw.isJsonPrimitive()||!raw.getAsJsonPrimitive().isNumber())throw new JsonSyntaxException("Expected integer "+key);
            try {
                int value=raw.getAsBigDecimal().intValueExact();
                if(value<min||value>max)throw new ArithmeticException("outside bounds");
                return value;
            }catch(ArithmeticException|NumberFormatException failure){throw new JsonSyntaxException("Invalid "+key+"; expected "+min+".."+max,failure);}
        }
        @Override public ForgingRecipe fromJson(ResourceLocation id,JsonObject json){
            JsonElement output=json.get("result");
            if(output==null||!output.isJsonObject())throw new JsonSyntaxException("Missing result object");
            JsonObject result=new JsonParser().parse(output.toString()).getAsJsonObject();
            if(!result.has("item")&&result.has("id"))result.add("item",result.get("id"));
            int count=integer(result,"count",1,1,64);result.addProperty("count",count);
            ItemStack stack=net.minecraftforge.common.crafting.CraftingHelper.getItemStack(result,true);
            return new ForgingRecipe(Ingredient.fromJson(json.get("ingredient")),integer(json,"input_count",1,1,64),stack,
                integer(json,"processing_ticks",100,1,72000),integer(json,"cooldown_ticks",200,0,72000),id);
        }
        public JsonObject toJson(ForgingRecipe recipe){
            JsonObject out=new JsonObject();out.add("ingredient",recipe.ingredient().toJson());out.addProperty("input_count",recipe.inputCount());
            JsonObject result=new JsonObject();result.addProperty("item",net.minecraft.util.registry.Registry.ITEM.getKey(recipe.result.getItem()).toString());
            result.addProperty("count",recipe.result.getCount());if(recipe.result.hasTag())result.addProperty("nbt",recipe.result.getTag().toString());
            out.add("result",result);out.addProperty("processing_ticks",recipe.processingTicks());out.addProperty("cooldown_ticks",recipe.cooldownTicks());
            return out;
        }
        @Override public ForgingRecipe fromNetwork(ResourceLocation id,PacketBuffer buffer){return new ForgingRecipe(Ingredient.fromNetwork(buffer),buffer.readVarInt(),buffer.readItem(),buffer.readVarInt(),buffer.readVarInt(),id);}
        @Override public void toNetwork(PacketBuffer buffer,ForgingRecipe recipe){recipe.ingredient().toNetwork(buffer);buffer.writeVarInt(recipe.inputCount());buffer.writeItem(recipe.result());buffer.writeVarInt(recipe.processingTicks());buffer.writeVarInt(recipe.cooldownTicks());}
    }
}
