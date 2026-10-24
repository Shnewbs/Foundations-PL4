"""Launch a private stock 1.12.2 Forge client with the exact native-qualified production JAR.

Build tools and mapped development game JARs never enter this runtime classpath.
"""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import hashlib,json,os,shutil,subprocess,time,urllib.request,uuid,zipfile

target=os.environ.get('PL4_CLIENT_MC','1.12.2')
profiles={
    '1.12.2':dict(forge='14.23.5.2864',artifact_sha256='ba7feeb795dfafceafc31fa9043fda2efea6472398db5138a939e227c36b71f3',runtime_suffix='',probe_suffix='client-probe',tweaker='net.minecraftforge.fml.common.launcher.FMLTweaker',java=8),
    '1.6.4':dict(forge='9.11.1.1345',artifact_sha256='11da07e6c8890f0240d36be0bf18d705dd9f6fed0863aeb4873416606ee83516',runtime_suffix='-srg',probe_suffix='client-probe-srg',tweaker='cpw.mods.fml.common.launcher.FMLTweaker',java=7),
}
profile=profiles[target]
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


status=json.loads((root/'BUILD_STATUS.json').read_text());name='FoundationsPL4-'+target+'-'+status['version']+profile['runtime_suffix']+'.jar'
qualified=root/'qualified-native.zip'
assert hashlib.sha256(qualified.read_bytes()).hexdigest()==profile['artifact_sha256']
with zipfile.ZipFile(qualified) as archive:
    assert archive.read('verification-logs/SOURCE_COMMIT.txt').decode().strip()==os.environ['PL4_CLIENT_SOURCE'],'Artifact source differs from checked source'
    runtime=game/'mods'/name
    runtime.write_bytes(archive.read('build/libs/'+name))
runtime_hash=hashlib.sha256(runtime.read_bytes()).hexdigest()
assert runtime_hash==status['candidate_runtime_sha256'],'Qualified production artifact checksum mismatch'

probe=root/'build/libs'/('FoundationsPL4-'+target+'-'+status['version']+'-'+profile['probe_suffix']+'.jar')
if target=='1.6.4' and not probe.exists():
    candidates=list((root/'build').rglob('*client-probe*-srg.jar'))
    assert len(candidates)==1,dict(message='Expected one remapped probe',jars=[str(p.relative_to(root)) for p in (root/'build').rglob('*.jar')])
    probe=candidates[0]
with zipfile.ZipFile(probe) as archive:
    marker='net/foundations/pl4/legacy/'+('Legacy164WirelessClientProbe' if target=='1.6.4' else 'LegacyWirelessClientProbe')+'.class'
    assert marker in archive.namelist(),'Missing isolated client harness'
shutil.copyfile(probe,game/'mods/pl4-client-probe.jar')
with zipfile.ZipFile(runtime) as archive:assert not any('WirelessClientProbe' in n or '/tests/' in n for n in archive.namelist())

forge_version=target+'-'+profile['forge']
base='https://maven.minecraftforge.net/net/minecraftforge/forge/'+forge_version+'/forge-'+forge_version+'-installer.jar'
expected=subprocess.check_output(['curl','--fail','--location','--silent','--show-error','--retry','3',base+'.sha1'],text=True).strip().split()[0]
installer=download(base,root/'stock-forge-installer.jar',expected)
installer_java=str(Path(os.environ['JAVA_HOME_8_X64'])/'bin/java') if target=='1.6.4' else 'java'
install_cmd=[installer_java,'-jar',str(installer),'--installServer']+([] if target=='1.6.4' else [str(game)])
with (out/'forge-install.log').open('w') as log:subprocess.run(install_cmd,cwd=game if target=='1.6.4' else root,check=True,stdout=log,stderr=subprocess.STDOUT)

manifest=json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json',timeout=60))
entry=next(v for v in manifest['versions'] if v['id']==target)
version_file=download(entry['url'],root/'stock-version.json',entry['sha1']);version=json.loads(version_file.read_text())
client=download(version['downloads']['client']['url'],game/('minecraft-client-'+target+'.jar'),version['downloads']['client']['sha1'])
assets=root/'stock-assets';index_file=download(version['assetIndex']['url'],assets/'indexes'/(version['assetIndex']['id']+'.json'),version['assetIndex']['sha1']);index=json.loads(index_file.read_text())
def get_asset(asset):
    digest=asset['hash'];relative=Path(digest[:2])/digest;target=assets/'objects'/relative
    cached=Path.home()/'.gradle/caches/forge_gradle/assets/objects'/relative
    if cached.exists() and hashlib.sha1(cached.read_bytes()).hexdigest()==digest:
        target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(cached,target);return target
    return download('https://resources.download.minecraft.net/'+relative.as_posix(),target,digest)
with ThreadPoolExecutor(max_workers=8) as pool:list(pool.map(get_asset,index['objects'].values()))
game_assets=assets
if index.get('virtual'):
    game_assets=assets/'virtual'/version['assetIndex']['id']
    for name,asset in index['objects'].items():
        relative=Path(name);assert not relative.is_absolute() and '..' not in relative.parts
        destination=game_assets/relative;destination.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(assets/'objects'/asset['hash'][:2]/asset['hash'],destination)



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

natives=root/'stock-natives';natives.mkdir(exist_ok=True)
for lib in version['libraries']:
    classifier=lib.get('natives',{}).get('linux')
    if not allowed(lib) or not classifier:continue
    classifier=classifier.replace('${arch}','64')
    artifact=lib['downloads']['classifiers'][classifier];archive=download(artifact['url'],game/'libraries'/artifact['path'],artifact['sha1'])
    with zipfile.ZipFile(archive) as z:
        for name in z.namelist():
            relative=Path(name)
            if name.startswith('META-INF/') or name.endswith('/'):continue
            assert not relative.is_absolute() and '..' not in relative.parts
            destination=natives/relative;destination.parent.mkdir(parents=True,exist_ok=True);destination.write_bytes(z.read(name))
forge_candidates=list(game.glob('*forge*'+profile['forge']+'*.jar'));assert len(forge_candidates)==1,forge_candidates;forge=forge_candidates[0]
# Client classes precede any server-JAR reference in the installer-created Forge manifest.
classpath=[client,forge]+sorted((game/'libraries').rglob('*.jar'))
assert not any('mergetool' in str(p).lower() or 'mapped_snapshot' in str(p) for p in classpath)
(out/'stock-client-runtime.json').write_text(json.dumps(dict(minecraft=target,forge=profile['forge'],runtime_java=profile['java'],runtime_sha256=runtime_hash,source_commit=os.environ['PL4_CLIENT_SOURCE'],probe_file=probe.name,probe_sha256=hashlib.sha256(probe.read_bytes()).hexdigest(),client_sha1=version['downloads']['client']['sha1'],installer_sha1=expected,production_jar_matches_native_gate=True,development_game_classpath=False,scope='Stock client plus exact native-qualified production mod and a separate isolated test mod; private offline integrated world.'),indent=2)+'\n')
identity=str(uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:PL4LegacyProbe').digest(),version=3))
game_java=str(Path(os.environ['JAVA_HOME_7_X64'])/'bin/java') if target=='1.6.4' else 'java'
cmd=[game_java,'-Xmx2G','-Djava.library.path='+str(natives),'-cp',os.pathsep.join(map(str,classpath)),'net.minecraft.launchwrapper.Launch','--version',target,'--gameDir',str(game),'--assetsDir',str(game_assets),'--username','PL4LegacyProbe','--tweakClass',profile['tweaker'],'--width','1280','--height','960']
cmd+=['--session','0'] if target=='1.6.4' else ['--assetIndex',version['assetIndex']['id'],'--accessToken','0','--uuid',identity,'--userProperties','{}']
raise SystemExit(subprocess.call(cmd,cwd=game))
