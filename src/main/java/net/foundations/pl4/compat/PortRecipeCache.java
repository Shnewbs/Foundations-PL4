package net.foundations.pl4.compat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.Optional;
/** Resolve the remembered ID from the current manager, so recipe reloads cannot retain stale objects. */
public final class PortRecipeCache<C extends Container,T extends Recipe<C>> {
 private final RecipeType<T> type;private ResourceLocation last;
 private PortRecipeCache(RecipeType<T> type){this.type=type;}
 public static <C extends Container,T extends Recipe<C>> PortRecipeCache<C,T> create(RecipeType<T> type){return new PortRecipeCache<>(type);}
 @SuppressWarnings("unchecked") public Optional<T> getRecipeFor(C input,Level level){
  if(last!=null){var value=level.getRecipeManager().byKey(last);if(value.isPresent()&&value.get().getType()==type){T recipe=(T)value.get();if(recipe.matches(input,level))return Optional.of(recipe);}}
  Optional<T> found=level.getRecipeManager().getRecipeFor(type,input,level);last=found.map(Recipe::getId).orElse(null);return found;
 }
}
