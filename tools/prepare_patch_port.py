"""Validate the prepared 1.16.2 target and retain exact API diagnostics.
This branch already contains the committed full-feature target adaptation.
"""
from pathlib import Path
import hashlib,json,os,subprocess,sys,zipfile
ROOT=Path(__file__).resolve().parents[1]
def prepare(root):
    request=json.loads((root/'PORT_TARGET.json').read_text());status=json.loads((root/'BUILD_STATUS.json').read_text())
    if request['minecraft']!='1.16.2' or status['minecraft']!='1.16.2' or status['loader_version']!='33.0.61':raise ValueError('Wrong native target')
    path=root/'build.gradle';text=path.read_text()
    if 'inspectExactPortApi' not in text:
        text+="\ntasks.register('inspectExactPortApi', Exec) { commandLine 'python3','tools/prepare_patch_port.py','--inspect' }\ntasks.named('compileJava') { finalizedBy 'inspectExactPortApi' }\n"
        path.write_text(text,encoding='utf-8')
    print('Exact target retained; biome API adaptation is not yet accepted')
def inspect(root):
    cache=Path.home()/'.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge'
    jars=list(cache.glob('*mapped_official_1.16.2/*mapped_official_1.16.2.jar'))
    if len(jars)!=1:
        print('Mapped API signatures unavailable:',jars);return
    jar=jars[0]
    with zipfile.ZipFile(jar) as z:classes=[n[:-6].replace('/','.') for n in z.namelist() if n.endswith('.class')]
    wanted={'Biome','BiomeGenerationSettings','IForgeBiome','WorldEvent','WorldGenRegistries','DynamicRegistries','WorldGenSettings','ServerWorld','FMLCommonSetupEvent','FeatureSpreadConfig','ConfiguredFeature'}
    out=root/'verification-logs';out.mkdir(exist_ok=True)
    with (out/'exact-api-signatures.txt').open('w') as dest:
        dest.write(jar.name+' SHA256 '+hashlib.sha256(jar.read_bytes()).hexdigest()+'\n')
        for name in sorted(n for n in classes if n.split('.')[-1].split('$')[0] in wanted):
            result=subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/javap'),'-private','-classpath',str(jar),name],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=30)
            dest.write('\n### '+name+'\n'+result.stdout)
    print('Recorded exact 1.16.2 API signatures; no game binaries copied')
if __name__=='__main__':
    if '--inspect' in sys.argv:inspect(ROOT)
    else:prepare(ROOT)
