package net.foundations.pl4.compat;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
public final class SingleRecipeInput extends Inventory {
 public SingleRecipeInput(ItemStack item){super(item);}
 public ItemStack item(){return getItem(0);}
}
