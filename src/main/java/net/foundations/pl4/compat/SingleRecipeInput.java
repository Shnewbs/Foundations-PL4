package net.foundations.pl4.compat;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
public final class SingleRecipeInput extends SimpleContainer {
 public SingleRecipeInput(ItemStack item){super(item);}
 public ItemStack item(){return getItem(0);}
}
