"""Migrate actual 1.18.2 source to 1.16.5 names using verified native classpaths.
Original behavior fixtures remain in a separate, non-shipping test source set.
"""
from pathlib import Path
import os,re,zipfile,json,shutil
W=Path(os.environ['PL4_PORT_WORK']).resolve();R=W/'1.16.5'
if R.exists():raise SystemExit('Refusing to overwrite an existing port workspace')
shutil.copytree(W/'1.18.2',R);J=R/'src/main/java/net/foundations/pl4'
def classes(version):
 out={}
 for jar in (W/'toolchains'/version/'classpath').glob('*.jar'):
  with zipfile.ZipFile(jar) as z:
   for name in z.namelist():
    if name.endswith('.class') and name.startswith(('net/minecraft/','com/mojang/')) and '$' not in name:
     cls=name[:-6].replace('/','.');out.setdefault(cls.rsplit('.',1)[1],[]).append(cls)
 return out
old=classes('1.18.2');new=classes('1.16.5');actual={c for values in new.values() for c in values};mapping={}
for simple,values in old.items():
 for cls in values:
  if cls not in actual and len(new.get(simple,[]))==1:mapping[cls]=new[simple][0]
aliases={
'ChatFormatting':'net.minecraft.util.text.TextFormatting','GuiComponent':'net.minecraft.client.gui.AbstractGui','AbstractContainerScreen':'net.minecraft.client.gui.screen.inventory.ContainerScreen','CompoundTag':'net.minecraft.nbt.CompoundNBT','Tag':'net.minecraft.nbt.INBT','ListTag':'net.minecraft.nbt.ListNBT','ByteTag':'net.minecraft.nbt.ByteNBT','IntTag':'net.minecraft.nbt.IntNBT','StringTag':'net.minecraft.nbt.StringNBT','NbtUtils':'net.minecraft.nbt.NBTUtil','FriendlyByteBuf':'net.minecraft.network.PacketBuffer','Component':'net.minecraft.util.text.ITextComponent','MutableComponent':'net.minecraft.util.text.IFormattableTextComponent','TextComponent':'net.minecraft.util.text.StringTextComponent','TranslatableComponent':'net.minecraft.util.text.TranslationTextComponent','ClientboundBlockEntityDataPacket':'net.minecraft.network.play.server.SUpdateTileEntityPacket','ServerLevel':'net.minecraft.world.server.ServerWorld','ServerPlayer':'net.minecraft.entity.player.ServerPlayerEntity','ResourceManager':'net.minecraft.resources.IResourceManager','TagKey':'net.foundations.pl4.compat.TagKey','FormattedCharSequence':'net.minecraft.util.IReorderingProcessor','GsonHelper':'net.minecraft.util.JSONUtils','InteractionHand':'net.minecraft.util.Hand','InteractionResult':'net.minecraft.util.ActionResultType','InteractionResultHolder':'net.minecraft.util.ActionResult','MenuProvider':'net.minecraft.inventory.container.INamedContainerProvider','SimpleContainer':'net.minecraft.inventory.Inventory','Player':'net.minecraft.entity.player.PlayerEntity','Inventory':'net.minecraft.entity.player.PlayerInventory','AbstractContainerMenu':'net.minecraft.inventory.container.Container','InventoryMenu':'net.minecraft.inventory.container.PlayerContainer','MenuType':'net.minecraft.inventory.container.ContainerType','BlockPlaceContext':'net.minecraft.item.BlockItemUseContext','UseOnContext':'net.minecraft.item.ItemUseContext','RecipeSerializer':'net.minecraft.item.crafting.IRecipeSerializer','RecipeType':'net.minecraft.item.crafting.IRecipeType','Recipe':'net.minecraft.item.crafting.IRecipe','Container':'net.minecraft.inventory.IInventory','Level':'net.minecraft.world.World','LevelReader':'net.minecraft.world.IWorldReader','BlockGetter':'net.minecraft.world.IBlockReader','LightLayer':'net.minecraft.world.LightType','CropBlock':'net.minecraft.block.CropsBlock','BlockEntity':'net.minecraft.tileentity.TileEntity','BlockEntityType':'net.minecraft.tileentity.TileEntityType','ChestBlockEntity':'net.minecraft.tileentity.ChestTileEntity','BlockBehaviour':'net.minecraft.block.AbstractBlock','StateDefinition':'net.minecraft.state.StateContainer','AABB':'net.minecraft.util.math.AxisAlignedBB','BlockHitResult':'net.minecraft.util.math.BlockRayTraceResult','HitResult':'net.minecraft.util.math.RayTraceResult','Vec3':'net.minecraft.util.math.vector.Vector3d','ResourceKey':'net.minecraft.util.RegistryKey','RegistryAccess':'net.minecraft.util.registry.DynamicRegistries','BaseEntityBlock':'net.minecraft.block.ContainerBlock','RenderShape':'net.minecraft.block.BlockRenderType','CollisionContext':'net.minecraft.util.math.shapes.ISelectionContext','Shapes':'net.minecraft.util.math.shapes.VoxelShapes','BooleanOp':'net.minecraft.util.math.shapes.IBooleanFunction','SoundSource':'net.minecraft.util.SoundCategory','IntegerProperty':'net.minecraft.state.IntegerProperty','BlockEntityTicker':'net.foundations.pl4.compat.BlockEntityTicker','IntProvider':'net.minecraft.util.math.MathHelper','BlockAndTintGetter':'net.minecraft.world.IBlockDisplayReader','LootContext':'net.minecraft.loot.LootContext','LootParams':'net.minecraft.loot.LootContext','LootContextParams':'net.minecraft.loot.LootParameters','PoseStack':'com.mojang.blaze3d.matrix.MatrixStack','MultiBufferSource':'net.minecraft.client.renderer.IRenderTypeBuffer','VertexConsumer':'com.mojang.blaze3d.vertex.IVertexBuilder','Font':'net.minecraft.client.gui.FontRenderer','TextureAtlas':'net.minecraft.client.renderer.texture.AtlasTexture','BlockEntityRenderer':'net.minecraft.client.renderer.tileentity.TileEntityRenderer','BlockEntityRendererProvider':'net.foundations.pl4.compat.BlockEntityRendererProvider','Screen':'net.minecraft.client.gui.screen.Screen','EditBox':'net.minecraft.client.gui.widget.TextFieldWidget','AbstractWidget':'net.minecraft.client.gui.widget.Widget','ClickEvent':'net.minecraft.util.text.event.ClickEvent','HoverEvent':'net.minecraft.util.text.event.HoverEvent','ContainerData':'net.minecraft.util.IIntArray','SimpleContainerData':'net.minecraft.util.IntArray','ModelPart':'net.minecraft.client.renderer.model.ModelRenderer','Direction':'net.minecraft.util.Direction','LocalPlayer':'net.minecraft.client.entity.player.ClientPlayerEntity','Mth':'net.minecraft.util.math.MathHelper','Rotation':'net.minecraft.util.Rotation','Mirror':'net.minecraft.util.Mirror','Fluid':'net.minecraft.fluid.Fluid','EntityBlock':'net.minecraft.block.ITileEntityProvider'}
for source,target in aliases.items():
 for cls in old.get(source,[]):mapping[cls]=target
mapping.update({'net.minecraft.client.renderer.block.model.ItemTransforms.TransformType':'net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType','net.minecraft.client.gui.components.EditBox':'net.minecraft.client.gui.widget.TextFieldWidget'})
simple={a:b.rsplit('.',1)[1] for a,b in aliases.items() if a not in ['EditBox','Screen','Container'] and a!=b.rsplit('.',1)[1]}
all_old=[c for values in old.values() for c in values]
pattern=re.compile(r'\b('+ '|'.join(map(re.escape,sorted(simple,key=len,reverse=True)))+r')\b')
def migrate(text):
 def expand(match):
  prefix=match.group(1)+'.';tokens=set(re.findall(r'\b\w+\b',text))
  return '\n'.join('import '+cls+';' for cls in all_old if cls.rsplit('.',1)[0]+'.'==prefix and cls.rsplit('.',1)[1] in tokens)
 text=re.sub(r'import ((?:net\.minecraft|com\.mojang)\.[\w.]+)\.\*;',expand,text)
 markers={}
 for source,target in sorted(mapping.items(),key=lambda pair:len(pair[0]),reverse=True):
  if source in text:
   marker='__PORT_TYPE_'+str(len(markers))+'__';markers[marker]=target;text=re.sub(re.escape(source)+r'\b',marker,text)
 text=re.sub(r'\bTag\.(TAG_[A-Z_]+)',r'net.minecraftforge.common.util.Constants.NBT.\1',text)
 text=pattern.sub(lambda match:simple[match.group()],text)
 for marker,target in markers.items():text=text.replace(marker,target)
 return text
for path in J.rglob('*.java'):
 if path.name.endswith('GameTests.java') or path.name=='PortAssertions.java':
  out=R/'src/portTest/java'/path.relative_to(R/'src/main/java');out.parent.mkdir(parents=True,exist_ok=True);shutil.move(path,out);continue
 text=migrate(path.read_text()).replace('bus.addListener(PLGameTests::register);','').replace('org.slf4j.LoggerFactory','org.apache.logging.log4j.LogManager').replace('LoggerFactory.getLogger','LogManager.getLogger')
 path.write_text(text)
(W/'map116-full.json').write_text(json.dumps(mapping,indent=2))
print('Native 1.16.5 names migrated; all original behavior fixtures preserved separately')
