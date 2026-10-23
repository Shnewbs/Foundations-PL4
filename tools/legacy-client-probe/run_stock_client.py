"""Launch a private stock 1.12.2 Forge client with the exact native-qualified production JAR.

Build tools and mapped development game JARs never enter this runtime classpath.
"""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import hashlib,json,os,shutil,subprocess,time,urllib.request,uuid,zipfile

root=Path.cwd();out=Path(os.environ['PL4_CLIENT_OUTPUT']);game=root/'stock-client';game.mkdir(exist_ok=True);(game/'mods').mkdir(exist_ok=True)
(game/'options.txt').write_text('tutorialStep:none\n')

def download(url,path,sha1=None):
    path=Path(path);path.parent.mkdir(parents=True,exist_ok=True)
    if path.exists() and (sha1 is None or hashlib.sha1(path.read_bytes()).hexdigest()==sha1):return path
    for attempt in range(5):
        try:
            subprocess.run(['curl','--fail','--location','--silent','--show-error','--retry','2','--max-time','90',url,'--output',str(path)],check=True)
            data=path.read_bytes()
            if sha1 and hashlib.sha1(data).hexdigest()!=sha1:raise RuntimeError('Checksum mismatch: '+path.name)
            path.write_bytes(data);return path
        except Exception:
            if attempt==4:raise
            time.sleep(2+attempt)


status=json.loads((root/'BUILD_STATUS.json').read_text());name='FoundationsPL4-1.12.2-'+status['version']+'.jar'
qualified=root/'qualified-native.zip'
assert hashlib.sha256(qualified.read_bytes()).hexdigest()=='ba7feeb795dfafceafc31fa9043fda2efea6472398db5138a939e227c36b71f3'
with zipfile.ZipFile(qualified) as archive:
    assert archive.read('verification-logs/SOURCE_COMMIT.txt').decode().strip()==os.environ['PL4_CLIENT_SOURCE'],'Artifact source differs from checked source'
    runtime=game/'mods'/name
    runtime.write_bytes(archive.read('build/libs/'+name))
runtime_hash=hashlib.sha256(runtime.read_bytes()).hexdigest()
assert runtime_hash==status['candidate_runtime_sha256'],'Qualified production artifact checksum mismatch'

probe=root/'build/libs'/('FoundationsPL4-1.12.2-'+status['version']+'-client-probe.jar');shutil.copyfile(probe,game/'mods/pl4-client-probe.jar')
with zipfile.ZipFile(runtime) as archive:assert not any('LegacyWirelessClientProbe' in n or '/tests/' in n for n in archive.namelist())

base='https://maven.minecraftforge.net/net/minecraftforge/forge/1.12.2-14.23.5.2864/forge-1.12.2-14.23.5.2864-installer.jar'
expected=subprocess.check_output(['curl','--fail','--location','--silent','--show-error','--retry','3',base+'.sha1'],text=True).strip().split()[0]
installer=download(base,root/'stock-forge-installer.jar',expected)
with (out/'forge-install.log').open('w') as log:subprocess.run(['java','-jar',str(installer),'--installServer',str(game)],check=True,stdout=log,stderr=subprocess.STDOUT)

manifest=json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json',timeout=60))
entry=next(v for v in manifest['versions'] if v['id']=='1.12.2')
version_file=download(entry['url'],root/'stock-version.json',entry['sha1']);version=json.loads(version_file.read_text())
client=download(version['downloads']['client']['url'],game/'minecraft-client-1.12.2.jar',version['downloads']['client']['sha1'])
assets=root/'stock-assets';index_file=download(version['assetIndex']['url'],assets/'indexes'/(version['assetIndex']['id']+'.json'),version['assetIndex']['sha1']);index=json.loads(index_file.read_text())
def get_asset(asset):
    digest=asset['hash'];relative=Path(digest[:2])/digest;target=assets/'objects'/relative
    cached=Path.home()/'.gradle/caches/forge_gradle/assets/objects'/relative
    if cached.exists() and hashlib.sha1(cached.read_bytes()).hexdigest()==digest:
        target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(cached,target);return target
    return download('https://resources.download.minecraft.net/'+relative.as_posix(),target,digest)
with ThreadPoolExecutor(max_workers=8) as pool:list(pool.map(get_asset,index['objects'].values()))


def allowed(library):
    rules=library.get('rules');permitted=not rules
    for rule in rules or []:
        system=rule.get('os',{})
        if system.get('name','linux')!='linux':continue
        permitted=rule['action']=='allow'
    return permitted

artifacts=[lib['downloads']['artifact'] for lib in version['libraries'] if allowed(lib) and 'artifact' in lib.get('downloads',{})]
def get_library(artifact):return download(artifact['url'],game/'libraries'/artifact['path'],artifact['sha1'])
with ThreadPoolExecutor(max_workers=8) as pool:list(pool.map(get_library,artifacts))

forge=game/'forge-1.12.2-14.23.5.2864-universal.jar'
if not forge.exists():
    candidates=list(game.glob('forge-1.12.2-14.23.5.2864*.jar'));assert len(candidates)==1,candidates;forge=candidates[0]
# Client classes precede any server-JAR reference in the installer-created Forge manifest.
classpath=[client,forge]+sorted((game/'libraries').rglob('*.jar'))
assert not any('mergetool' in str(p).lower() or 'mapped_snapshot' in str(p) for p in classpath)
(out/'stock-client-runtime.json').write_text(json.dumps(dict(minecraft='1.12.2',forge='14.23.5.2864',runtime_sha256=runtime_hash,source_commit=os.environ['PL4_CLIENT_SOURCE'],probe_sha256=hashlib.sha256(probe.read_bytes()).hexdigest(),client_sha1=version['downloads']['client']['sha1'],installer_sha1=expected,production_jar_matches_native_gate=True,development_game_classpath=False,scope='Stock client plus exact native-qualified production mod and a separate isolated test mod; private offline integrated world.'),indent=2)+'\n')
identity=str(uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:PL4LegacyProbe').digest(),version=3))
cmd=['java','-Xmx2G','-Djava.library.path='+str(root/'build/natives'),'-cp',os.pathsep.join(map(str,classpath)),'net.minecraft.launchwrapper.Launch','--version','1.12.2','--gameDir',str(game),'--assetsDir',str(assets),'--assetIndex',version['assetIndex']['id'],'--accessToken','0','--uuid',identity,'--username','PL4LegacyProbe','--userProperties','{}','--tweakClass','net.minecraftforge.fml.common.launcher.FMLTweaker','--width','1280','--height','960']
raise SystemExit(subprocess.call(cmd,cwd=game))
