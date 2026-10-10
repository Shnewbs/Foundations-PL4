"""Adapt pre-1.16.2 APIs while retaining the full feature source and every scenario."""
from pathlib import Path
import json,os,subprocess,sys,zipfile,hashlib
ROOT=Path(__file__).resolve().parents[1]
def backport(root):
    status_path=root/'BUILD_STATUS.json';status=json.loads(status_path.read_text())
    if status['minecraft']!='1.16.1':return
    if status.get('native_api_backport')=='1.16.1-v1':return
    changes={}
    for folder in ['src/main/java','src/portTest/java']:
        for p in (root/folder).rglob('*.java'):
            old=p.read_text();text=old.replace('import net.minecraft.util.registry.DynamicRegistries;\n','')
            text=text.replace('net.minecraft.util.registry.DynamicRegistries','Object').replace('DynamicRegistries','Object')
            for expr in ['minecraft.level.registryAccess()','mc.level.registryAccess()','host.getLevel().registryAccess()','ref.level().registryAccess()','h.getLevel().registryAccess()','c.getLevel().registryAccess()','level.registryAccess()','l.registryAccess()']:
                text=text.replace(expr,'null')
            text=text.replace('import net.minecraft.util.IReorderingProcessor;','import net.minecraft.util.text.ITextProperties;').replace('IReorderingProcessor','ITextProperties')
            text=text.replace('ITextProperties.forward(font.plainSubstrByWidth(value,scaledWidth),net.minecraft.util.text.Style.EMPTY)','new net.minecraft.util.text.StringTextComponent(font.plainSubstrByWidth(value,scaledWidth))')
            if p.name=='GuiGraphics.java':
                text=text.replace('text.getVisualOrderText()','(ITextProperties)text')
                text=text.replace('RenderSystem.disableScissor()','org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST)')
                text=text.replace('RenderSystem.enableScissor(','org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glScissor(')
            if p.name=='GuideButton.java':text=text.replace('h=getHeight()','h=height')
            if p.name in {'FoundationsPL4.java','CoreRecipes.java','PLClient.java'}:
                text=text.replace('e.enqueueWork(','net.minecraftforge.fml.DeferredWorkQueue.runLater(').replace('event.enqueueWork(','net.minecraftforge.fml.DeferredWorkQueue.runLater(')
            if p.name=='FoundationsPL4.java':text=text.replace('MinecraftForge.EVENT_BUS.addListener(PortWorldgen::biome);','')
            if p.name=='ForgingRecipe.java':
                text=text.replace('Codec.intRange(','intRange(')
                anchor='  private static <A> MapCodec<A> optional('
                if text.count(anchor)!=1:raise ValueError('Unexpected recipe codec')
                text=text.replace(anchor,'  private static Codec<Integer> intRange(int min,int max){return Codec.INT.flatXmap(value->value>=min&&value<=max?DataResult.success(value):DataResult.error("Integer outside "+min+".."+max),value->value>=min&&value<=max?DataResult.success(value):DataResult.error("Integer outside "+min+".."+max));}\n'+anchor)
            if old!=text:changes[p]=text
    changes[root/'src/main/java/net/foundations/pl4/PortWorldgen.java']='''package net.foundations.pl4;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.placement.*;
/** 1.16.1 attaches configured ore directly during deferred common setup. */
public final class PortWorldgen {
 private static ConfiguredFeature<?,?> sapphire;
 public static void register(){
  if(sapphire!=null)return;
  sapphire=Feature.ORE.configured(new OreFeatureConfig(OreFeatureConfig.FillerBlockType.NATURAL_STONE,FoundationsPL4.ORE.get().defaultBlockState(),6))
    .decorated(Placement.COUNT_RANGE.configured(new CountRangeConfig(15,1,0,30)));
  for(Biome biome:net.minecraftforge.registries.ForgeRegistries.BIOMES.getValues())
   if(biome.getBiomeCategory()!=Biome.Category.NETHER&&biome.getBiomeCategory()!=Biome.Category.THEEND)
    biome.addFeature(GenerationStage.Decoration.UNDERGROUND_ORES,sapphire);
 }
 public static ConfiguredFeature<?,?> sapphire(){return sapphire;}
 private PortWorldgen(){}
}
'''
    build=root/'build.gradle';script=build.read_text()
    if 'inspectExactPortApi' not in script:
        changes[build]=script+"\ntasks.register('inspectExactPortApi', Exec) { commandLine 'python3','tools/backport_1161_api.py','--inspect' }\ntasks.named('compileJava') { finalizedBy 'inspectExactPortApi' }\n"
    status['native_api_backport']='1.16.1-v1';changes[status_path]=json.dumps(status,indent=2)+'\n'
    for p,text in changes.items():p.write_text(text,encoding='utf-8')
    print('Applied 1.16.1 native registry context, text, lifecycle, codec and biome adapters; native validation pending')
def inspect(root):
    cache=Path.home()/'.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge'
    jars=list(cache.glob('*mapped_official_1.16.1/*mapped_official_1.16.1.jar'))
    if len(jars)!=1:
        print('Mapped API signatures unavailable:',jars);return
    jar=jars[0]
    with zipfile.ZipFile(jar) as z:classes=[n[:-6].replace('/','.') for n in z.namelist() if n.endswith('.class')]
    wanted={'FontRenderer','ITextProperties','ITextComponent','StringTextComponent','Placement','CountRangeConfig','Biome','ConfiguredFeature','Widget','DeferredWorkQueue'}
    out=root/'verification-logs';out.mkdir(exist_ok=True)
    with (out/'exact-api-signatures.txt').open('w') as dest:
        dest.write(jar.name+' SHA256 '+hashlib.sha256(jar.read_bytes()).hexdigest()+'\n')
        for name in sorted(n for n in classes if n.split('.')[-1].split('$')[0] in wanted):
            result=subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/javap'),'-protected','-classpath',str(jar),name],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=30)
            dest.write('\n### '+name+'\n'+result.stdout)
    print('Recorded exact 1.16.1 API signatures; no game binaries copied')
if __name__=='__main__':
    if '--inspect' in sys.argv:inspect(ROOT)
    else:backport(ROOT)
