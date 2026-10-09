"""Native 1.16.5 hooks after forge116_names.py. No foreign Minecraft classes are injected."""
from pathlib import Path
import os,re,subprocess
W=Path(os.environ['PL4_PORT_WORK']).resolve();R=W/'1.16.5';J=R/'src/main/java/net/foundations/pl4'
def patch(name,old,new):
 p=J/name;s=p.read_text();assert old in s,(name,old);p.write_text(s.replace(old,new))
def write(name,text):(J/name).write_text(text)
for p in J.rglob('*.java'):
 s=p.read_text().replace('net.minecraftforge.event.server.ServerStoppedEvent','net.minecraftforge.fml.event.server.FMLServerStoppedEvent').replace('ServerStoppedEvent e','FMLServerStoppedEvent e')
 s=s.replace('net.minecraftforge.network.','net.minecraftforge.fml.network.').replace('net.minecraftforge.registries.RegistryObject','net.minecraftforge.fml.RegistryObject')
 s=s.replace('net.minecraft.world.item.CreativeModeTab','net.minecraft.item.ItemGroup').replace('CreativeModeTab','ItemGroup')
 s=s.replace('net.minecraft.client.gui.screens.MenuScreens','net.minecraft.client.gui.ScreenManager')
 s=s.replace('net.minecraft.nbt.NbtIo','net.minecraft.nbt.CompressedStreamTools').replace('NbtIo.write','CompressedStreamTools.write')
 s=s.replace('ItemStack.isSameItemSameTags(', 'net.foundations.pl4.compat.PortData.sameItem(')
 s=s.replace('player.getInventory()','player.inventory').replace('player.getAbilities()','player.abilities').replace('player.getEyePosition()','player.getEyePosition(1.0F)')
 s=s.replace('net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity','net.minecraft.tileentity.LockableLootTileEntity')
 s=re.sub(r'(?:net\.minecraft\.nbt\.)?INBT.TAG_([A-Z_]+)',r'net.minecraftforge.common.util.Constants.NBT.TAG_\1',s)
 s=s.replace('com.google.gson.JsonParser.parseString(','new com.google.gson.JsonParser().parse(').replace('JsonParser.parseString(','new JsonParser().parse(').replace('JsonParser.parseReader(', 'new JsonParser().parse(')
 s=s.replace('LightTexture.FULL_BRIGHT','15728880').replace('.color(color)', '.color((color>>>16)&255,(color>>>8)&255,color&255,(color>>>24)&255)')
 if 'import net.foundations.pl4.compat.Button;' in s:s=s.replace('import net.minecraft.client.gui.widget.button.Button;','')
 s=s.replace('import net.minecraft.client.renderer.entity.ItemRenderer;','import net.minecraft.client.renderer.ItemRenderer;')
 if 'RegistryObject<' in s and 'import net.minecraftforge.fml.RegistryObject;' not in s:s=s.replace('package net.foundations.pl4;', 'package net.foundations.pl4;\nimport net.minecraftforge.fml.RegistryObject;')
 for key,field in [('BLOCK','BLOCKS'),('ITEM','ITEMS'),('BLOCK_ENTITY_TYPE','TILE_ENTITIES'),('MENU','CONTAINERS'),('RECIPE_SERIALIZER','RECIPE_SERIALIZERS')]:s=s.replace('DeferredRegister.create(Registries.'+key+',','DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.'+field+',')
 p.write_text(s)
patch('compat/PortRecipeCache.java','extends Container','extends net.minecraft.inventory.IInventory')
p=J/'core/CoreRecipes.java';s=p.read_text();s=re.sub(r'    private static final DeferredRegister<IRecipeType<\?>> TYPES[^\n]*\n','',s)
s=s.replace('public static final RegistryObject<IRecipeType<ForgingRecipe>> HAMMER=TYPES.register("forging_hammer",()->new IRecipeType<>(){','public static final java.util.function.Supplier<IRecipeType<ForgingRecipe>> HAMMER=new java.util.function.Supplier<>() {\n        private final IRecipeType<ForgingRecipe> value=new IRecipeType<>(){')
s=s.replace('    });','    };\n        @Override public IRecipeType<ForgingRecipe> get(){return value;}\n    };',1)
s=s.replace('TYPES.register(bus);','bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.RECIPE_TYPE,FoundationsPL4.id("forging_hammer"),HAMMER.get())));');p.write_text(s)
for name,type in [('HostEntity.java','HOST_ENTITY'),('HammerEntity.java','HAMMER_ENTITY')]:
 p=J/name;s=p.read_text();cn=name[:-5]
 s=s.replace('public '+cn+'(BlockPos p,BlockState s){super(FoundationsPL4.'+type+'.get(),p,s);}', 'public '+cn+'(){super(FoundationsPL4.'+type+'.get());}\n    public '+cn+'(BlockPos p,BlockState s){this();setPosition(p);}')
 s=s.replace('super.saveAdditional(t);','super.save(t);').replace('super.saveAdditional(tag);','super.save(tag);').replace('super.load(t);','').replace('super.load(tag);','')
 s=s.replace('@Override protected void saveAdditional(CompoundNBT tag){saveAdditional(tag,null);}','@Override public CompoundNBT save(CompoundNBT tag){saveAdditional(tag,null);return tag;}')
 s=s.replace('@Override public void load(CompoundNBT tag){loadAdditional(tag,null);}','@Override public void load(BlockState state,CompoundNBT tag){super.load(state,tag);loadAdditional(tag,null);}')
 s=s.replace('SUpdateTileEntityPacket.create(this)','new SUpdateTileEntityPacket(getBlockPos(),0,getUpdateTag())')
 s=s.replace('@Override public CompoundNBT getUpdateTag(){return getUpdateTag(null);}', '@Override public CompoundNBT getUpdateTag(){CompoundNBT t=super.getUpdateTag();t.merge(getUpdateTag(null));return t;}\n    @Override public void onDataPacket(net.minecraft.network.NetworkManager manager,SUpdateTileEntityPacket packet){load(getBlockState(),packet.getTag());}')
 if name=='HammerEntity.java':
  s=s.replace('implements INamedContainerProvider','implements INamedContainerProvider,net.minecraft.tileentity.ITickableTileEntity')
  s=s.rstrip()[:-1]+'    @Override public void tick(){if(level!=null)tick(level,worldPosition,getBlockState(),this);}\n}\n'
 p.write_text(s)
for name,entity in [('HostBlock.java','HostEntity'),('HammerBlock.java','HammerEntity')]:
 p=J/name;s=p.read_text().replace('@Override public TileEntity newBlockEntity(BlockPos p,BlockState s){return new '+entity+'(p,s);}', '@Override public TileEntity newBlockEntity(IBlockReader world){return new '+entity+'();}')
 s=s.replace('import net.foundations.pl4.compat.BlockEntityTicker;','')
 if name=='HammerBlock.java':s=re.sub(r'    @Override public <T extends TileEntity> BlockEntityTicker<T> getTicker\([^\n]*\{.*?\n    }','',s,flags=re.S)
 p.write_text(s)
write('client/HammerModel.java','''package net.foundations.pl4.client;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.foundations.pl4.core.HammerGeometry;
/** Same model boxes, UVs and animation rules on the native pre-layer model API. */
public final class HammerModel {
 private final java.util.Map<String,ModelRenderer> parts=new java.util.LinkedHashMap<>();
 public HammerModel(){for(var piece:HammerGeometry.PIECES){
  ModelRenderer part=new ModelRenderer(HammerGeometry.TEXTURE_WIDTH,HammerGeometry.TEXTURE_HEIGHT,piece.u(),piece.v());
  part.addBox(piece.x(),piece.y(),piece.z(),piece.width(),piece.height(),piece.depth());part.setPos(piece.px(),piece.py(),piece.pz());
  part.xRot=piece.rx();part.yRot=piece.ry();part.zRot=piece.rz();parts.put(piece.name(),part);
 }}
 public void render(MatrixStack pose,IVertexBuilder vertices,int light,int overlay,double progress,boolean assembled){for(var piece:HammerGeometry.PIECES){
  var part=parts.get(piece.name());part.visible=!piece.upper()||assembled;part.y=piece.py()+(piece.moving()?HammerGeometry.TRAVEL*(float)progress:0);part.render(pose,vertices,light,overlay);
 }}
}
''')
for name in ['HostRenderer.java','HammerRenderer.java']:
 p=J/'client'/name;s=p.read_text().replace('import net.foundations.pl4.compat.BlockEntityRendererProvider;','import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;').replace('implements TileEntityRenderer','extends TileEntityRenderer')
 s=s.replace('HostRenderer(BlockEntityRendererProvider.Context c){','HostRenderer(TileEntityRendererDispatcher c){super(c);')
 s=s.replace('HammerRenderer(BlockEntityRendererProvider.Context context){model=new HammerModel(context.bakeLayer(HammerModel.LAYER));','HammerRenderer(TileEntityRendererDispatcher context){super(context);model=new HammerModel();')
 s=s.replace('@Override public int getViewDistance()','public int getViewDistance()');p.write_text(s)
p=J/'client/PLClient.java';s=p.read_text().replace('import net.minecraftforge.client.event.EntityRenderersEvent;','');s=re.sub(r'    @SubscribeEvent public static void (renderers|layers)\([^\n]*\n','',s)
s=s.replace('e.enqueueWork(()->{','e.enqueueWork(()->{net.minecraftforge.fml.client.registry.ClientRegistry.bindTileEntityRenderer(FoundationsPL4.HOST_ENTITY.get(),HostRenderer::new);net.minecraftforge.fml.client.registry.ClientRegistry.bindTileEntityRenderer(FoundationsPL4.HAMMER_ENTITY.get(),HammerRenderer::new);');p.write_text(s)
p=J/'core/ForgingRecipe.java';p.write_text(p.read_text().replace('JSONUtils.getAsJsonObject(json,"result").deepCopy()','new com.google.gson.JsonParser().parse(JSONUtils.getAsJsonObject(json,"result").toString()).getAsJsonObject()'))
write('compat/TagKey.java','''package net.foundations.pl4.compat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;
/** Look up current tag contents on each membership test, so reloads cannot retain old values. */
public record TagKey<T>(RegistryKey<? extends Registry<T>> registry,ResourceLocation location){
 public static <T> TagKey<T> create(RegistryKey<? extends Registry<T>> registry,ResourceLocation location){return new TagKey<>(registry,location);}
 public boolean contains(T value){
  if(registry.equals(Registries.ITEM))return net.minecraft.tags.ItemTags.getAllTags().getTagOrEmpty(location).contains((net.minecraft.item.Item)value);
  if(registry.equals(Registries.FLUID))return net.minecraft.tags.FluidTags.getAllTags().getTagOrEmpty(location).contains((net.minecraft.fluid.Fluid)value);
  return false;
 }
}
''')
for name in ['DataSampler.java','TransferFilters.java']:
 p=J/name;p.write_text(p.read_text().replace('stack.is(tag)','tag.contains(stack.getItem())').replace('stack.getFluid().is(tag)','tag.contains(stack.getFluid())'))
p=J/'compat/PortScreen.java';s=p.read_text().replace('clearWidgets();','buttons.clear();children.clear();')
s=s.replace(' protected PortScreen(ITextComponent title)', ' protected <T extends net.minecraft.client.gui.widget.Widget> T addRenderableWidget(T widget){return addButton(widget);}\n protected void removeWidget(net.minecraft.client.gui.widget.Widget widget){buttons.remove(widget);children.remove(widget);if(getFocused()==widget)setFocused(null);}\n protected PortScreen(ITextComponent title)');p.write_text(s)
write('compat/PortMatrices.java','''package net.foundations.pl4.compat;
import net.minecraft.util.math.vector.Matrix4f;
/** Active-editor picking captures the real fixed-function matrices, not guessed camera transforms. */
public final class PortMatrices {
 public static Matrix4f projection(){return matrix(org.lwjgl.opengl.GL11.GL_PROJECTION_MATRIX);}
 public static Matrix4f modelView(){return matrix(org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX);}
 private static Matrix4f matrix(int key){var buffer=org.lwjgl.BufferUtils.createFloatBuffer(16);org.lwjgl.opengl.GL11.glGetFloatv(key,buffer);float[] values=new float[16];buffer.get(values);Matrix4f matrix=new Matrix4f(values);matrix.transpose();return matrix;}
 private PortMatrices(){}
}
''')
p=J/'client/DisplayEditorScreen.java';p.write_text(p.read_text().replace('RenderSystem.getProjectionMatrix()','net.foundations.pl4.compat.PortMatrices.projection()').replace('RenderSystem.getModelViewMatrix()','net.foundations.pl4.compat.PortMatrices.modelView()'))
p=J/'compat/GuiGraphics.java';s=p.read_text().replace('RenderSystem.setShaderTexture(0,texture)','mc.getTextureManager().bind(texture)').replace('var model=RenderSystem.getModelViewStack();model.pushPose();model.mulPoseMatrix(pose.last().pose());RenderSystem.applyModelViewMatrix();','RenderSystem.pushMatrix();RenderSystem.multMatrix(pose.last().pose());').replace('model.popPose();RenderSystem.applyModelViewMatrix();','RenderSystem.popMatrix();');p.write_text(s)
p=J/'compat/PortData.java';p.write_text(p.read_text().replace(' private PortData(){}',' public static boolean sameItem(ItemStack a,ItemStack b){return a.getItem()==b.getItem()&&ItemStack.tagMatches(a,b);}\n private PortData(){}'))
p=J/'compat/Button.java';p.write_text(p.read_text().replace(' public int getX()',' public boolean isHoveredOrFocused(){return isHovered()||isFocused();}\n public int getX()'))
# Native item identity tests use getItem; block-state identity tests use getBlock below.
a=Path(__file__).with_name('RewriteCalls.java').read_text().replace('public class RewriteCalls','public class Rewrite116')
a=a.replace('case "getLast" ->','case "is" -> {if(a.size()==1&&(!a.get(0).equals("tag")))replacement="("+receiver+".getItem()=="+a.get(0)+")";}\n                                    case "getLast" ->')
(W/'Rewrite116.java').write_text(a);subprocess.run(['javac','-d',str(W),str(W/'Rewrite116.java')],check=True);subprocess.run(['java','-cp',str(W),'Rewrite116',str(J)],check=True)
for p in J.rglob('*.java'):
 s=p.read_text().replace('net.minecraftforge.server.ServerLifecycleHooks','net.minecraftforge.fml.server.ServerLifecycleHooks')
 s=s.replace('net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity','net.minecraft.tileentity.AbstractFurnaceTileEntity')
 s=s.replace('furnace.saveWithoutMetadata()','furnace.save(new net.minecraft.nbt.CompoundNBT())')
 s=s.replace('pose,buffers,0)','pose,buffers)').replace('pose,buffer,hammer.getBlockPos().hashCode())','pose,buffer)')
 s=s.replace('@Override public ItemStack getCloneItemStack(BlockState s,RayTraceResult target,IBlockReader l,BlockPos p,PlayerEntity player)','@Override public ItemStack getPickBlock(BlockState s,RayTraceResult target,IBlockReader l,BlockPos p,PlayerEntity player)')
 s=s.replace('boolean onDestroyedByPlayer(','boolean removedByPlayer(').replace('state.getItem()','state.getBlock()').replace('s.getItem()==next.getBlock()','s.getBlock()==next.getBlock()').replace('old.getItem()==this','old.getBlock()==this')
 s=re.sub(r'(getBlockState\([^\n;]+?\))\.getItem\(\)',r'\1.getBlock()',s);p.write_text(s)
print('Native 1.16.5 lifecycle, registry, NBT, packet and renderer hooks applied')
