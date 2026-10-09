"""Target-native Forge hooks; preserves the original behavioral fixtures."""
from pathlib import Path
import os,re
R=Path(os.environ['PL4_PORT_WORK']).resolve()/'1.20.1';J=R/'src/main/java/net/foundations/pl4'
def patch(n,a,b):
 p=J/n;s=p.read_text();assert a in s,(n,a);p.write_text(s.replace(a,b))
def write(n,s):(J/n).write_text(s)
patch('compat/RegisterPayloadHandlersEvent.java','public ServerPlayer player()','public net.minecraft.world.entity.player.Player player()')
patch('compat/PortLists.java',' private PortLists(){}',' public static <T> T removeLast(java.util.List<T> list){if(list.isEmpty())throw new java.util.NoSuchElementException();return list.remove(list.size()-1);}\n public static <T> java.util.List<T> reversed(java.util.List<T> list){var copy=new java.util.ArrayList<>(list);java.util.Collections.reverse(copy);return copy;}\n private PortLists(){}')
patch('PLPackets.java','p.links.addFirst(link)','p.links.add(0,link)')
patch('R9GameTests.java','p.elements.removeLast()','net.foundations.pl4.compat.PortLists.removeLast(p.elements)')
patch('client/DisplayEditorScreen.java','selectionOnPage().reversed()','net.foundations.pl4.compat.PortLists.reversed(selectionOnPage())')
patch('BuiltinInfoProvider.java','saveWithoutMetadata(l.registryAccess())','saveWithoutMetadata()')
patch('EnergyIntegrationGameTests.java','h.getLevel().invalidateCapabilities(absolute)','net.foundations.pl4.compat.PortCapabilities.invalidate(to)')
patch('PLClientConfig.java','List.<String>of(),()->"00000000-0000-0000-0000-000000000000",entry->','List.<String>of(),entry->')
for n,arg in [('HostEntity.java','t'),('HammerEntity.java','tag')]:
 p=J/n;s=p.read_text();s=s.replace('@Override protected void saveAdditional(CompoundTag '+arg+',net.minecraft.core.RegistryAccess r)', 'protected void saveAdditional(CompoundTag '+arg+',net.minecraft.core.RegistryAccess r)')
 s=s.replace('super.saveAdditional('+arg+',r)','super.saveAdditional('+arg+')')
 s=s.replace('@Override protected void loadAdditional(CompoundTag '+arg+',net.minecraft.core.RegistryAccess r)', 'protected void loadAdditional(CompoundTag '+arg+',net.minecraft.core.RegistryAccess r)')
 s=s.replace('super.loadAdditional('+arg+',r)','super.load('+arg+')')
 s=s.replace('@Override public CompoundTag getUpdateTag(net.minecraft.core.RegistryAccess r)', 'public CompoundTag getUpdateTag(net.minecraft.core.RegistryAccess r)')
 s=s.replace('inventory.serializeNBT(r)','inventory.serializeNBT()').replace('inventory.deserializeNBT(r,','inventory.deserializeNBT(')
 wrappers='''
    @Override protected void saveAdditional(CompoundTag tag){saveAdditional(tag,net.minecraft.core.RegistryAccess.EMPTY);}
    @Override public void load(CompoundTag tag){loadAdditional(tag,net.minecraft.core.RegistryAccess.EMPTY);}
    @Override public CompoundTag getUpdateTag(){return getUpdateTag(net.minecraft.core.RegistryAccess.EMPTY);}
'''
 s=s.rstrip()[:-1]+wrappers+'}\n';p.write_text(s)
write('compat/PortScreen.java','''package net.foundations.pl4.compat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public abstract class PortScreen extends Screen {
 protected PortScreen(Component title){super(title);}
 public void renderBackground(GuiGraphics g,int x,int y,float partial){super.renderBackground(g);}
 @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g,x,y,partial);super.render(g,x,y,partial);}
 public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return super.mouseScrolled(x,y,vertical);}
 @Override public boolean mouseScrolled(double x,double y,double vertical){return mouseScrolled(x,y,0,vertical);}
}
''')
for p in (J/'client').glob('*.java'):p.write_text(p.read_text().replace('extends Screen','extends net.foundations.pl4.compat.PortScreen'))
patch('client/HammerScreen.java','super.render(g,mouseX,mouseY,partial);','renderBackground(g);super.render(g,mouseX,mouseY,partial);')
p=J/'client/DisplayCanvas.java';s=p.read_text();s=s.replace('.addVertex(p,','.vertex(p.pose(),').replace('.setColor(','.color(').replace('.setUv(','.uv(').replace('.setOverlay(','.overlayCoords(').replace('.setLight(','.uv2(').replace('.setNormal(p,0,0,1);','.normal(p.normal(),0,0,1).endVertex();');p.write_text(s)
patch('client/HostRenderer.java','@Override public net.minecraft.world.phys.AABB getRenderBoundingBox(HostEntity host)','public static net.minecraft.world.phys.AABB getRenderBoundingBox(HostEntity host)')
patch('client/HammerRenderer.java','@Override public AABB getRenderBoundingBox(HammerEntity hammer)','public AABB getRenderBoundingBox(HammerEntity hammer)')
patch('HostEntity.java','public final class HostEntity extends BlockEntity {','''public final class HostEntity extends BlockEntity {
    public static java.util.function.Function<HostEntity,net.minecraft.world.phys.AABB> clientRenderBounds=h->new net.minecraft.world.phys.AABB(h.getBlockPos());
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return clientRenderBounds.apply(this);}
''')
patch('HammerEntity.java','public final class HammerEntity extends BlockEntity implements MenuProvider {','''public final class HammerEntity extends BlockEntity implements MenuProvider {
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(getBlockPos()).expandTowards(0,2,0);}
''')
patch('client/PLClient.java','e.enqueueWork(()->{','e.enqueueWork(()->{HostEntity.clientRenderBounds=HostRenderer::getRenderBoundingBox;')
for n in ['HostBlock.java','HammerBlock.java','HammerSpaceBlock.java']:
 p=J/n;s=p.read_text();s=re.sub(r'    public static final MapCodec<HostBlock> CODEC=.*?;\n','',s)
 s=re.sub(r'    @Override protected MapCodec[^\n]+\n','',s)
 s=s.replace('@Override protected ','@Override public ')
 s=s.replace('@Override public InteractionResult useWithoutItem','public InteractionResult useWithoutItem')
 start=s.index('    @Override public ItemInteractionResult useItemOn(');end=s.index('\n    }',start)+6
 if n=='HostBlock.java':
  replacement='''    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        ItemStack held=player.getItemInHand(hand);
        if(held.getItem() instanceof PartItem||held.getItem() instanceof ToolItem)return InteractionResult.PASS;
        return useWithoutItem(s,l,p,player,hit);
    }'''
 else:replacement='''    @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){return useWithoutItem(s,l,pos,p,hit);}'''
 s=s[:start]+replacement+s[end:]
 s=s.replace('HitResult target,LevelReader l','HitResult target,BlockGetter l');p.write_text(s)
p=J/'R5GameTests.java';s=p.read_text().replace('ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION','InteractionResult.PASS').replace('useItemOn(stack,a.getBlockState(),','use(a.getBlockState(),');p.write_text(s)
write('compat/SingleRecipeInput.java','''package net.foundations.pl4.compat;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
public final class SingleRecipeInput extends SimpleContainer {
 public SingleRecipeInput(ItemStack item){super(item);}
 public ItemStack item(){return getItem(0);}
}
''')
for p in J.rglob('*.java'):
 if p.name=='SingleRecipeInput.java':continue
 s=p.read_text().replace('net.minecraft.world.item.crafting.SingleRecipeInput','net.foundations.pl4.compat.SingleRecipeInput')
 s=re.sub(r'(?<![.\w])SingleRecipeInput\b','net.foundations.pl4.compat.SingleRecipeInput',s)
 s=s.replace('Optional<RecipeHolder<ForgingRecipe>>','Optional<ForgingRecipe>')
 if p.name=='PLGameTests.java':s=s.replace('.getOrThrow()','.getOrThrow(false,message->{throw new IllegalArgumentException(message);})')
 p.write_text(s)
write('core/ForgingRecipe.java','''package net.foundations.pl4.core;
import com.google.gson.JsonObject;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.foundations.pl4.FoundationsPL4;
import net.foundations.pl4.compat.SingleRecipeInput;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public record ForgingRecipe(Ingredient ingredient,int inputCount,ItemStack result,int processingTicks,int cooldownTicks,ResourceLocation id) implements Recipe<SingleRecipeInput> {
 public ForgingRecipe(Ingredient ingredient,int inputCount,ItemStack result,int processingTicks,int cooldownTicks){this(ingredient,inputCount,result,processingTicks,cooldownTicks,FoundationsPL4.id("unregistered"));}
 public ForgingRecipe {
  if(inputCount<1||inputCount>64)throw new IllegalArgumentException("Input count must be 1..64");
  if(ingredient.isEmpty()||result.isEmpty()||result.getCount()>result.getMaxStackSize())throw new IllegalArgumentException("Invalid forging ingredient/result");
  if(processingTicks<1||processingTicks>72000||cooldownTicks<0||cooldownTicks>72000)throw new IllegalArgumentException("Invalid forging duration");
  result=result.copy();
 }
 @Override public ItemStack result(){return result.copy();}
 public ForgingRecipe value(){return this;}
 @Override public ResourceLocation getId(){return id;}
 @Override public boolean matches(SingleRecipeInput input,Level level){return input.item().getCount()>=inputCount&&ingredient.test(input.item());}
 @Override public ItemStack assemble(SingleRecipeInput input,RegistryAccess registries){return result.copy();}
 @Override public ItemStack getResultItem(RegistryAccess registries){return result.copy();}
 @Override public NonNullList<Ingredient> getIngredients(){return NonNullList.of(Ingredient.EMPTY,ingredient);}
 @Override public boolean canCraftInDimensions(int width,int height){return width>0&&height>0;}
 @Override public boolean isSpecial(){return true;}
 @Override public ItemStack getToastSymbol(){return new ItemStack(FoundationsPL4.HAMMER.get());}
 @Override public RecipeSerializer<?> getSerializer(){return CoreRecipes.HAMMER_SERIALIZER.get();}
 @Override public RecipeType<?> getType(){return CoreRecipes.HAMMER.get();}
 public static final class Serializer implements RecipeSerializer<ForgingRecipe> {
  private static final Codec<Ingredient> INGREDIENT=Codec.PASSTHROUGH.comapFlatMap(dynamic->{try{return DataResult.success(Ingredient.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue(),false));}catch(RuntimeException bad){return DataResult.error(bad::getMessage);}},ingredient->new Dynamic<>(JsonOps.INSTANCE,ingredient.toJson()));
  private static final MapCodec<ForgingRecipe> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
   INGREDIENT.fieldOf("ingredient").forGetter(ForgingRecipe::ingredient),
   Codec.intRange(1,64).optionalFieldOf("input_count",1).forGetter(ForgingRecipe::inputCount),
   ItemStack.CODEC.fieldOf("result").forGetter(ForgingRecipe::result),
   Codec.intRange(1,72000).optionalFieldOf("processing_ticks",100).forGetter(ForgingRecipe::processingTicks),
   Codec.intRange(0,72000).optionalFieldOf("cooldown_ticks",200).forGetter(ForgingRecipe::cooldownTicks)
  ).apply(instance,ForgingRecipe::new));
  public MapCodec<ForgingRecipe> codec(){return CODEC;}
  @Override public ForgingRecipe fromJson(ResourceLocation id,JsonObject json){
   JsonObject result=GsonHelper.getAsJsonObject(json,"result").deepCopy();
   if(result.has("id")&&!result.has("item"))result.add("item",result.remove("id"));
   return new ForgingRecipe(Ingredient.fromJson(json.get("ingredient"),false),GsonHelper.getAsInt(json,"input_count",1),net.minecraftforge.common.crafting.CraftingHelper.getItemStack(result,true),GsonHelper.getAsInt(json,"processing_ticks",100),GsonHelper.getAsInt(json,"cooldown_ticks",200),id);
  }
  @Override public ForgingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return new ForgingRecipe(Ingredient.fromNetwork(buffer),buffer.readVarInt(),buffer.readItem(),buffer.readVarInt(),buffer.readVarInt(),id);}
  @Override public void toNetwork(FriendlyByteBuf buffer,ForgingRecipe recipe){recipe.ingredient().toNetwork(buffer);buffer.writeVarInt(recipe.inputCount());buffer.writeItem(recipe.result());buffer.writeVarInt(recipe.processingTicks());buffer.writeVarInt(recipe.cooldownTicks());}
 }
}
''')
print('Native Forge API hooks applied; compile and runtime validation remain separate')
