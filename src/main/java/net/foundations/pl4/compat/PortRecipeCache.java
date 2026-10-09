package net.foundations.pl4.compat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.world.World;
import java.util.Optional;
/** Resolve the remembered ID from the current manager, so recipe reloads cannot retain stale objects. */
public final class PortRecipeCache<C extends net.minecraft.inventory.IInventory,T extends IRecipe<C>> {
 private final IRecipeType<T> type;private ResourceLocation last;
 private PortRecipeCache(IRecipeType<T> type){this.type=type;}
 public static <C extends net.minecraft.inventory.IInventory,T extends IRecipe<C>> PortRecipeCache<C,T> create(IRecipeType<T> type){return new PortRecipeCache<>(type);}
 @SuppressWarnings("unchecked") public Optional<T> getRecipeFor(C input,World level){
  if(last!=null){var value=level.getRecipeManager().byKey(last);if(value.isPresent()&&value.get().getType()==type){T recipe=(T)value.get();if(recipe.matches(input,level))return Optional.of(recipe);}}
  Optional<T> found=level.getRecipeManager().getRecipeFor(type,input,level);last=found.map(IRecipe::getId).orElse(null);return found;
 }
}
