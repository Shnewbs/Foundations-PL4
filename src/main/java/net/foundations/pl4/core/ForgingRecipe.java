package net.foundations.pl4.core;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.foundations.pl4.FoundationsPL4;
import net.foundations.pl4.compat.SingleRecipeInput;
import net.minecraft.util.NonNullList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.JSONUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.world.World;
public record ForgingRecipe(Ingredient ingredient,int inputCount,ItemStack result,int processingTicks,int cooldownTicks,ResourceLocation id) implements IRecipe<SingleRecipeInput> {
 public ForgingRecipe(Ingredient ingredient,int inputCount,ItemStack result,int processingTicks,int cooldownTicks){this(ingredient,inputCount,result,processingTicks,cooldownTicks,FoundationsPL4.id("unregistered"));}
 public ForgingRecipe {
  if(inputCount<1||inputCount>64)throw new IllegalArgumentException("Input count must be 1..64");
  if(ingredient.isEmpty()||result.isEmpty()||result.getCount()>result.getMaxStackSize())throw new IllegalArgumentException("Invalid forging ingredient/result");
  if(processingTicks<1||processingTicks>72000||cooldownTicks<0||cooldownTicks>72000)throw new IllegalArgumentException("Invalid forging duration");
  result=result.copy();
 }
 @Override public ItemStack result(){return result.copy();}
 @Override public ItemStack assemble(SingleRecipeInput input){return result.copy();}
 @Override public ItemStack getResultItem(){return result.copy();}
 public ForgingRecipe value(){return this;}
 @Override public ResourceLocation getId(){return id;}
 @Override public boolean matches(SingleRecipeInput input,World level){return input.item().getCount()>=inputCount&&ingredient.test(input.item());}
 public ItemStack assemble(SingleRecipeInput input,Object registries){return result.copy();}
 public ItemStack getResultItem(Object registries){return result.copy();}
 @Override public NonNullList<Ingredient> getIngredients(){return NonNullList.of(Ingredient.EMPTY,ingredient);}
 @Override public boolean canCraftInDimensions(int width,int height){return width>0&&height>0;}
 @Override public boolean isSpecial(){return true;}
 @Override public ItemStack getToastSymbol(){return new ItemStack(FoundationsPL4.HAMMER.get());}
 @Override public IRecipeSerializer<?> getSerializer(){return CoreRecipes.HAMMER_SERIALIZER.get();}
 @Override public IRecipeType<?> getType(){return CoreRecipes.HAMMER.get();}
 public static final class Serializer extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ForgingRecipe> {
  private static final Codec<Ingredient> INGREDIENT=Codec.PASSTHROUGH.comapFlatMap(dynamic->{try{return DataResult.success(Ingredient.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue()));}catch(RuntimeException bad){return DataResult.error(bad.getMessage());}},ingredient->new Dynamic<>(JsonOps.INSTANCE,ingredient.toJson()));
  private static final MapCodec<ForgingRecipe> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
   INGREDIENT.fieldOf("ingredient").forGetter(ForgingRecipe::ingredient),
   optional(intRange(1,64),"input_count",1).forGetter(ForgingRecipe::inputCount),
   ItemStack.CODEC.fieldOf("result").forGetter(ForgingRecipe::result),
   optional(intRange(1,72000),"processing_ticks",100).forGetter(ForgingRecipe::processingTicks),
   optional(intRange(0,72000),"cooldown_ticks",200).forGetter(ForgingRecipe::cooldownTicks)
  ).apply(instance,ForgingRecipe::new));
  private static Codec<Integer> intRange(int min,int max){return Codec.INT.flatXmap(value->value>=min&&value<=max?DataResult.success(value):DataResult.error("Integer outside "+min+".."+max),value->value>=min&&value<=max?DataResult.success(value):DataResult.error("Integer outside "+min+".."+max));}
  private static <A> MapCodec<A> optional(Codec<A> codec,String name,A fallback){return new MapCodec<>(){
   @Override public <T> DataResult<A> decode(DynamicOps<T> ops,MapLike<T> input){T value=input.get(name);if(value==null)return DataResult.success(fallback);DataResult<A> decoded=codec.parse(ops,value);if(decoded.error().isPresent())return DataResult.error(decoded.error().get().message());return decoded;}
   @Override public <T> RecordBuilder<T> encode(A value,DynamicOps<T> ops,RecordBuilder<T> prefix){return prefix.add(name,codec.encodeStart(ops,value));}
   @Override public <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops){return java.util.stream.Stream.of(ops.createString(name));}
  };}
  public MapCodec<ForgingRecipe> codec(){return CODEC;}
  @Override public ForgingRecipe fromJson(ResourceLocation id,JsonObject json){
   JsonObject result=new com.google.gson.JsonParser().parse(JSONUtils.getAsJsonObject(json,"result").toString()).getAsJsonObject();
   if(result.has("id")&&!result.has("item"))result.add("item",result.remove("id"));
   return new ForgingRecipe(Ingredient.fromJson(json.get("ingredient")),JSONUtils.getAsInt(json,"input_count",1),net.minecraftforge.common.crafting.CraftingHelper.getItemStack(result,true),JSONUtils.getAsInt(json,"processing_ticks",100),JSONUtils.getAsInt(json,"cooldown_ticks",200),id);
  }
  @Override public ForgingRecipe fromNetwork(ResourceLocation id,PacketBuffer buffer){return new ForgingRecipe(Ingredient.fromNetwork(buffer),buffer.readVarInt(),buffer.readItem(),buffer.readVarInt(),buffer.readVarInt(),id);}
  @Override public void toNetwork(PacketBuffer buffer,ForgingRecipe recipe){recipe.ingredient().toNetwork(buffer);buffer.writeVarInt(recipe.inputCount());buffer.writeItem(recipe.result());buffer.writeVarInt(recipe.processingTicks());buffer.writeVarInt(recipe.cooldownTicks());}
 }
}
