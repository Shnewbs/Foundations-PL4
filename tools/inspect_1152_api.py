"""Record public native API signatures for exact-version port diagnostics; never copy dependency JARs."""
from pathlib import Path
import hashlib, json, os, subprocess, zipfile
root=Path(__file__).resolve().parents[1]
cache=Path.home()/'.gradle/caches/forge_gradle/minecraft_user_repo'
candidates=list(cache.glob('net/minecraftforge/forge/*mapped_official_1.15.2/*mapped_official_1.15.2.jar'))
if len(candidates)!=1:raise SystemExit('Expected one exact mapped Forge 1.15.2 API: '+str(candidates))
jar=candidates[0]
with zipfile.ZipFile(jar) as z:
    classes=[name[:-6].replace('/','.') for name in z.namelist() if name.endswith('.class')]
wanted=set('World ServerWorld MinecraftServer Dimension DimensionType Entity LivingEntity PlayerEntity ServerPlayerEntity TileEntity Block AbstractBlock BlockState ActionResultType ActionResult Vec3d Vector3d Vector3f Vector4f Matrix4f Matrix3f Quaternion FontRenderer Screen AbstractGui TextFieldWidget Widget Button Style ITextComponent StringTextComponent Registry Tag ItemTags FluidTags ITag ConfiguredFeature Feature Placement OreFeatureConfig TopSolidRangeConfig CountRangeConfig Biome BiomeLoadingEvent IBlockReader ClientWorld RenderSystem IVertexBuilder MatrixStack'.split())
selected=sorted(name for name in classes if name.split('.')[-1].split('$')[0] in wanted and (name.startswith('net.minecraft.') or name.startswith('net.minecraftforge.')))
out=root/'verification-logs';out.mkdir(exist_ok=True)
javap=Path(os.environ['JAVA_HOME'])/'bin/javap'
with (out/'native-api-signatures.txt').open('w') as dest:
    dest.write('Exact mapped dependency: '+jar.name+'\nSHA256: '+hashlib.sha256(jar.read_bytes()).hexdigest()+'\n')
    for name in selected:
        result=subprocess.run([str(javap),'-protected','-classpath',str(jar),name],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=30)
        dest.write('\n### '+name+'\n'+result.stdout)
(out/'native-api-class-index.json').write_text(json.dumps(classes,indent=2)+'\n')
print('Recorded '+str(len(selected))+' public/protected native class signatures; no game/loader binaries exported.')
