"""Migrate the reviewed 1.19.2 tree to the actual Forge 1.18.2 interfaces."""
from pathlib import Path
import os,re,shutil
W=Path(os.environ['PL4_PORT_WORK']).resolve();R=W/'1.18.2';J=R/'src/main/java/net/foundations/pl4'
if R.exists():raise SystemExit('Refusing to overwrite migration workspace')
shutil.copytree(W/'1.19.2',R)
for p in J.rglob('*.java'):
 s=p.read_text()
 for old,new in [('literal','TextComponent'),('translatable','TranslatableComponent')]:s=re.sub(r'(?:net\.minecraft\.network\.chat\.)?Component\.'+old+r'\(', 'new net.minecraft.network.chat.'+new+'(',s)
 s=re.sub(r'(?:net\.minecraft\.network\.chat\.)?Component\.empty\(\)','new net.minecraft.network.chat.TextComponent("")',s)
 s=s.replace('ClientPlayerNetworkEvent.LoggingIn','ClientPlayerNetworkEvent.LoggedInEvent').replace('import net.minecraft.core.HolderLookup;','')
 s=s.replace('import net.minecraftforge.common.capabilities.ForgeCapabilities;','')
 s=s.replace('ForgeCapabilities.ITEM_HANDLER','net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY').replace('ForgeCapabilities.FLUID_HANDLER','net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY').replace('ForgeCapabilities.ENERGY','net.minecraftforge.energy.CapabilityEnergy.ENERGY')
 s=s.replace('MinecraftServer server=e.getServer();','MinecraftServer server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;')
 s=s.replace('import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;','').replace('IClientFluidTypeExtensions.of(sample.fluid().getFluid())','sample.fluid().getFluid().getAttributes()').replace('ext.getTintColor(sample.fluid())','ext.getColor(sample.fluid())')
 s=s.replace('class Serializer implements RecipeSerializer<ForgingRecipe>','class Serializer extends net.minecraftforge.registries.ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<ForgingRecipe>')
 s=s.replace('RecipeManager.CachedCheck<','net.foundations.pl4.compat.PortRecipeCache<').replace('RecipeManager.createCheck(','net.foundations.pl4.compat.PortRecipeCache.create(')
 p.write_text(s,encoding='utf-8')
p=J/'compat/PortScreen.java';s=p.read_text().replace(' protected PortScreen(Component title)', ' protected void rebuildWidgets(){clearWidgets();setFocused(null);init();}\n protected PortScreen(Component title)');p.write_text(s)
(J/'compat/PortRecipeCache.java').write_text('''package net.foundations.pl4.compat;
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
''',encoding='utf-8')
p=J/'client/HammerRenderer.java';p.write_text(p.read_text().replace('items=context.getItemRenderer()','items=net.minecraft.client.Minecraft.getInstance().getItemRenderer()'))
p=J/'client/GuideResources.java';s=p.read_text();s=s.replace('var resource=manager.getResource(FoundationsPL4.id("guide/"+safe+".json"));\n            if(resource.isEmpty())resource=manager.getResource(FoundationsPL4.id("guide/en_us.json"));\n            try(var in=resource.orElseThrow(()->new IOException("Missing PL4 guide resource")).open()){','var id=FoundationsPL4.id("guide/"+safe+".json");\n            if(!manager.hasResource(id))id=FoundationsPL4.id("guide/en_us.json");\n            try(var resource=manager.getResource(id);var in=resource.getInputStream()){');p.write_text(s)
print('Native 1.18.2 source interfaces applied; build and runtime tests remain separate')
