"""Explicit Forge 32.0.108 / current Java 8 launch profile; no binary patch or Java agent."""
from pathlib import Path
import argparse, hashlib, json, os, subprocess, urllib.request, zipfile

URL='https://maven.minecraftforge.net/cpw/mods/modlauncher/8.1.3/modlauncher-8.1.3.jar'
SHA256='4e0d846f75ffd0dd5042c9b1aa86b8fcc758acd27a004c259d26aebc100ffdf2'

def prepare(server: Path):
    server=server.resolve()
    forge=server/'forge-1.16.1-32.0.108.jar'
    if not forge.is_file():
        raise ValueError('Install exact Forge 1.16.1-32.0.108 into this directory first')
    with zipfile.ZipFile(forge) as z:
        manifest=z.read('META-INF/MANIFEST.MF').decode().replace('\r\n','\n').replace('\n ','')
    if 'ServerLaunchArgs:' not in manifest or '--fml.mcVersion 1.16.1' not in manifest:
        raise ValueError('Unexpected Forge server launcher manifest')
    library=server/'pl4-launch-profile/modlauncher-8.1.3.jar'
    library.parent.mkdir(exist_ok=True)
    if not library.exists() or hashlib.sha256(library.read_bytes()).hexdigest()!=SHA256:
        with urllib.request.urlopen(urllib.request.Request(URL,headers={'User-Agent':'FoundationsPL4-compat-profile'}),timeout=120) as r:
            data=r.read(2_000_001)
        if len(data)>2_000_000 or hashlib.sha256(data).hexdigest()!=SHA256:
            raise ValueError('Pinned ModLauncher SHA256 mismatch')
        temporary=library.with_suffix('.part');temporary.write_bytes(data);temporary.replace(library)
    info={'minecraft':'1.16.1','forge':'32.0.108','runtime_java':8,'modlauncher':'8.1.3',
          'modlauncher_url':URL,'modlauncher_sha256':SHA256,
          'stock_forge_libraries':False,'replaced_on_disk':False,'runtime_agent_required':False,
          'scope':'Explicit launch-time precedence over bundled ModLauncher 8.0.9; client profile acceptance pending'}
    (library.parent/'profile.json').write_text(json.dumps(info,indent=2)+'\n')
    return library,forge,info

def command(server: Path, java: str, jvm_args=(), game_args=('--nogui',)):
    library,forge,info=prepare(server)
    return [str(java),*jvm_args,'-cp',str(library)+os.pathsep+str(forge),
            'net.minecraftforge.server.ServerMain',*game_args],info

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--server',required=True,type=Path)
    p.add_argument('--java',default='java',help='Path to a Java 8 executable')
    p.add_argument('--launch',action='store_true',help='Start the server; otherwise prepare and print the command')
    a=p.parse_args()
    try:
        args,info=command(a.server,a.java,['-Xms512M','-Xmx3G'])
        print(json.dumps({'command':args,'profile':info},indent=2))
        if a.launch:raise SystemExit(subprocess.call(args,cwd=a.server.resolve()))
    except (OSError,ValueError,zipfile.BadZipFile) as error:p.exit(1,str(error)+'\n')
