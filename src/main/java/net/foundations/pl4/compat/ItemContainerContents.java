package net.foundations.pl4.compat;
import java.util.List;
import net.minecraft.world.item.ItemStack;
public record ItemContainerContents(List<ItemStack> items){
 public ItemContainerContents {items=items.stream().map(ItemStack::copy).toList();}
 public static ItemContainerContents fromItems(List<ItemStack> items){return new ItemContainerContents(items);}
}
