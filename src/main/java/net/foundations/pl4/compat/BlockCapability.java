package net.foundations.pl4.compat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import java.util.*;
public record BlockCapability<T,C>(ResourceLocation name,Class<T> typeClass,Class<C> contextClass,Capability<T> nativeCapability){
 private static final List<BlockCapability<?,?>> FOREIGN=new ArrayList<>();
 public static synchronized <T> void registerForeign(String id,Class<T> type,Capability<T> capability){
  ResourceLocation name=new ResourceLocation(id);if(FOREIGN.stream().anyMatch(c->c.name().equals(name)))throw new IllegalArgumentException("Duplicate PL4 provider "+id);
  FOREIGN.add(new BlockCapability<>(name,type,Direction.class,capability));
 }
 public static synchronized List<BlockCapability<?,?>> getAll(){return List.copyOf(FOREIGN);}
}
