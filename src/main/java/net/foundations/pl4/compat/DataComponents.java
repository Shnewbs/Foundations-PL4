package net.foundations.pl4.compat;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.nbt.CompoundNBT;
/** PL4-owned bridge tokens; this does not add Minecraft data components to older games. */
public final class DataComponents {
 public record Key<T>(String name){}
 public static final Key<CustomData> CUSTOM_DATA=new Key<>("custom_data");
 public static final Key<ITextComponent> CUSTOM_NAME=new Key<>("custom_name");
 public static final Key<ItemContainerContents> CONTAINER=new Key<>("container");
 public static final Key<CompoundNBT> BLOCK_ENTITY_DATA=new Key<>("block_entity_data");
 private DataComponents(){}
}
