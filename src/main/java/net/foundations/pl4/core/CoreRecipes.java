package net.foundations.pl4.core;

import net.foundations.pl4.FoundationsPL4;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registered through the PL4 mod event bus and packaged in the same JAR. */
public final class CoreRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,FoundationsPL4.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,FoundationsPL4.ID);
    public static final DeferredHolder<RecipeType<?>,RecipeType<ForgingRecipe>> HAMMER=TYPES.register("forging_hammer",()->new RecipeType<>(){
        @Override public String toString(){return FoundationsPL4.ID+":forging_hammer";}
    });
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<ForgingRecipe>> HAMMER_SERIALIZER=SERIALIZERS.register("forging_hammer",ForgingRecipe.Serializer::create);
    public static void register(IEventBus bus){TYPES.register(bus);SERIALIZERS.register(bus);}
    private CoreRecipes(){}
}
