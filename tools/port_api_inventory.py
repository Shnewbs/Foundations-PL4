from pathlib import Path
import os,zipfile,subprocess
cache=Path(os.environ['HOME'])/'.gradle/caches/neoformruntime/intermediate_results'
jars=list(cache.glob('applyDevTransforms*_output.jar'))
if not jars: raise SystemExit('No generated target API jar')
jar=max(jars,key=lambda p:p.stat().st_mtime)
classes=zipfile.ZipFile(jar).namelist()
ends=['CompoundTag','ListTag','NbtUtils','UUIDUtil','ItemStack','ItemStackTemplate','BlockEntity','BlockEntityType','BlockBehaviour$Properties','PushReaction','Registry','ValueInput','ValueOutput','TagValueInput','TagValueOutput','Recipe','RecipeSerializer','RecipeType','Ingredient','RecipeManager','RecipeHolder','Screen','GuiGraphicsExtractor','AbstractContainerScreen','Minecraft','ScreenManager','ClientInput','MouseButtonEvent','KeyEvent','CharacterEvent','BlockEntityRenderer','BlockEntityRendererProvider','BlockEntityRenderState','SubmitNodeCollector','GameTestInstance','FunctionGameTestInstance','TestData','GameTestHelper','GameTestEnvironment','ClickEvent$OpenUrl','HoverEvent$ShowText','Player','ServerPlayer','PermissionSet','Permission','Permissions','Block','EntityBlock','BlockStateProperties','SimpleContainer','ItemDisplayContext','ItemModelResolver','RenderType','RenderTypes','TestInstanceBlockEntity']
selected=[c[:-6].replace('/','.') for c in classes if c.endswith('.class') and c.rsplit('/',1)[-1][:-6] in ends]
out=Path('port-diagnostics');out.mkdir(exist_ok=True)
(out/'class-names.txt').write_text('\n'.join(classes))
with (out/'target-api.txt').open('w') as f:
 for name in selected:
  subprocess.run(['javap','-p','-classpath',str(jar),name],stdout=f,stderr=f)
