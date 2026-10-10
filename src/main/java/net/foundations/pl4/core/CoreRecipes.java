package net.foundations.pl4.core;

import net.foundations.pl4.FoundationsPL4;
import net.foundations.pl4.compat.Registries;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/** Registered through the PL4 mod event bus and packaged in the same JAR. */
public final class CoreRecipes {
    private static final DeferredRegister<IRecipeSerializer<?>> SERIALIZERS=new DeferredRegister<>(net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS,FoundationsPL4.ID);
    public static final java.util.function.Supplier<IRecipeType<ForgingRecipe>> HAMMER=new java.util.function.Supplier<>() {
        private final IRecipeType<ForgingRecipe> value=new IRecipeType<>(){
        @Override public String toString(){return FoundationsPL4.ID+":forging_hammer";}
    };
        @Override public IRecipeType<ForgingRecipe> get(){return value;}
    };
    public static final RegistryObject<ForgingRecipe.Serializer> HAMMER_SERIALIZER=SERIALIZERS.register("forging_hammer",ForgingRecipe.Serializer::new);
    public static void register(IEventBus bus){bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->net.minecraftforge.fml.DeferredWorkQueue.runLater(()->net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.RECIPE_TYPE,FoundationsPL4.id("forging_hammer"),HAMMER.get())));SERIALIZERS.register(bus);}
    private CoreRecipes(){}
}
