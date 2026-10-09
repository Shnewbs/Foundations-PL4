package net.foundations.pl4.core;
import net.foundations.pl4.FoundationsPL4;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
/** Forge 37 serializers plus the version-native vanilla recipe-type registry. */
public final class CoreRecipes {
 private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS,FoundationsPL4.ID);
 public static final java.util.function.Supplier<RecipeType<ForgingRecipe>> HAMMER=new java.util.function.Supplier<>() {
  private final RecipeType<ForgingRecipe> value=new RecipeType<>(){@Override public String toString(){return FoundationsPL4.ID+":forging_hammer";}};
  @Override public RecipeType<ForgingRecipe> get(){return value;}
 };
 public static final RegistryObject<ForgingRecipe.Serializer> HAMMER_SERIALIZER=SERIALIZERS.register("forging_hammer",ForgingRecipe.Serializer::new);
 public static void register(IEventBus bus){
  bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->net.minecraft.core.Registry.register(net.minecraft.core.Registry.RECIPE_TYPE,FoundationsPL4.id("forging_hammer"),HAMMER.get())));
  SERIALIZERS.register(bus);
 }
 private CoreRecipes(){}
}
