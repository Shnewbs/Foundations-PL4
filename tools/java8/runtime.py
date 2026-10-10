"""Build-time Java 8 compatibility, archive checks and isolated native acceptance for Forge 1.16.x."""
from __future__ import annotations
import argparse, hashlib, json, os, re, shutil, struct, subprocess, sys, urllib.request, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOL_VERSION = '2.0.1'
TOOL_COMMIT = '3a561342fb09fbb19022e0f910d765e51e9711e9'
TOOL_SHA256 = '60a5bd57846fb4be1756fb93e6f63b3d9a63c2759a63e763c44553b086326107'
PREFIX = 'net/foundations/pl4/internal/java8'

def run(*args, cwd=ROOT, timeout=1200):
    print('+ ' + ' '.join(map(str, args)), flush=True)
    subprocess.run(list(map(str, args)), cwd=cwd, check=True, timeout=timeout)

def metadata():
    status = json.loads((ROOT/'BUILD_STATUS.json').read_text())
    target = status['minecraft']
    if target not in {'1.16.4','1.14.4'}:
        raise ValueError('Unsupported Java 8 port target')
    version = re.search(r"^version\s*=\s*'([A-Za-z0-9._-]+)'", (ROOT/'build.gradle').read_text(), re.M).group(1)
    return status, target, version, 'FoundationsPL4-'+target+'-'+version

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def fetch(url, dest, expected=None):
    dest.parent.mkdir(parents=True, exist_ok=True)
    if not dest.is_file() or (expected and sha(dest)!=expected):
        temp=dest.with_suffix(dest.suffix+'.part')
        with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent':'FoundationsPL4-build'}), timeout=120) as response:
            with temp.open('wb') as out:
                shutil.copyfileobj(response,out)
        temp.replace(dest)
    if expected and sha(dest)!=expected:
        raise ValueError('Checksum mismatch: '+dest.name)
    return dest

def convert():
    _, target, version, name = metadata()
    work=ROOT/'build/java8';work.mkdir(parents=True,exist_ok=True)
    cache=ROOT/'.java8-toolchain'
    tool=fetch('https://github.com/unimined/JvmDowngrader/releases/download/'+TOOL_VERSION+'/jvmdowngrader-'+TOOL_VERSION+'-all.jar',cache/'tool.jar',TOOL_SHA256)
    source=fetch('https://codeload.github.com/unimined/JvmDowngrader/zip/'+TOOL_COMMIT,cache/'jvmdowngrader-source.zip')
    licenses={}
    with zipfile.ZipFile(source) as archive:
        for suffix in ['LICENSE.md','license/LGPLv2.1.md']:
            entry=next(n for n in archive.namelist() if n.split('/',1)[-1]==suffix)
            licenses['META-INF/licenses/jvmdowngrader/'+suffix.split('/')[-1]]=archive.read(entry)
    java=Path(os.environ.get('JAVA_HOME_17_X64',os.environ.get('JAVA_HOME','')))/'bin/java'
    if not java.is_file():raise ValueError('A Java 17 build JDK is required')
    cp=(work/'classpath.txt').read_text().strip()
    # Each input sees the mapped hierarchy. Production signatures do not expose newer-JDK
    # stub types; isolate test helpers to avoid duplicate, separately minimized API classes.
    for label,classifier in [('runtime','java8-input'),('tests','java8-tests-input')]:
        src=ROOT/'build/libs'/(name+'-'+classifier+'.jar')
        run(java,'-jar',tool,'-nc','-c','52','downgrade','-t',src,work/(label+'-downgraded.jar'),'-cp',cp)
        run(java,'-jar',tool,'-nc','-c','52','shade','-p',PREFIX+('/tests' if label=='tests' else ''),'-t',work/(label+'-downgraded.jar'),work/(label+'-shaded.jar'))
        with zipfile.ZipFile(work/(label+'-shaded.jar'),'a',compression=zipfile.ZIP_DEFLATED) as archive:
            for path,contents in licenses.items():archive.writestr(path,contents)
            archive.writestr('META-INF/PL4-JAVA8-NOTICE.txt',
                'Includes only required, relocated JvmDowngrader API compatibility classes.\n'
                'Copyright (C) 2024 William Gray; LGPL-2.1.\n'
                'Source commit: '+TOOL_COMMIT+'\n'
                'Tool version: '+TOOL_VERSION+'; SHA256: '+TOOL_SHA256+'\n'
                'PL4 sources and this build script allow rebuilding/relinking.\n'
                'Matching upstream source is included as a separate release asset.\n')
    # Duplicate API classes must agree byte-for-byte; tests may not replace production classes.
    with zipfile.ZipFile(work/'runtime-shaded.jar') as prod,zipfile.ZipFile(work/'tests-shaded.jar') as tests:
        common=set(prod.namelist())&set(tests.namelist())
        for entry in common:
            if entry.endswith('.class') and prod.read(entry)!=tests.read(entry):
                raise ValueError('Different duplicate class in fixtures: '+entry)
    (work/'toolchain.json').write_text(json.dumps({'tool':TOOL_VERSION,'tool_sha256':sha(tool),'source_commit':TOOL_COMMIT,'source_zip_sha256':sha(source),'minecraft':target,'version':version},indent=2)+'\n')

def verify():
    status,target,_,name=metadata()
    counts={}
    for kind,suffix in [('runtime','java8'),('scenarios','java8-scenarios')]:
        jar=ROOT/'build/libs'/(name+'-'+suffix+'.jar')
        with zipfile.ZipFile(jar) as z:
            names=z.namelist()
            if len(names)!=len(set(names)):raise ValueError('Duplicate ZIP entry')
            count=0
            for entry in names:
                if entry.endswith('.jar'):raise ValueError('Nested dependency JAR: '+entry)
                if entry.startswith(('net/minecraft/','net/minecraftforge/','cpw/mods/')):
                    raise ValueError('Bundled loader/game class: '+entry)
                if kind=='runtime' and (entry.endswith('GameTests.class') or '/compat/scenarios/' in entry):
                    raise ValueError('Native fixture leaked into production: '+entry)
                if entry.endswith('.class'):
                    data=z.read(entry);magic,minor,major=struct.unpack('>IHH',data[:8])
                    if magic!=0xCAFEBABE or major>52:raise ValueError('Non-Java8 class: '+entry)
                    count+=1
            if not count:raise ValueError('Empty runtime')
            toml=z.read('META-INF/mods.toml').decode()
            if kind=='runtime':
                if 'version="'+status['version']+'"' not in toml or 'versionRange="['+target not in toml:
                    raise ValueError('Wrong artifact identity')
                if 'META-INF/PL4-JAVA8-NOTICE.txt' not in names:raise ValueError('Missing compatibility notice')
            counts[kind]={'classes':count,'sha256':sha(jar)}
    (ROOT/'build/java8/archive-check.json').write_text(json.dumps(counts,indent=2)+'\n')
    print('PASS Java 8 archive versions, exact metadata, licenses and fixture isolation: '+str(counts),flush=True)

def native():
    status,target,version,name=metadata()
    work=ROOT/'run-java8'
    if work.exists():
        raise ValueError('Native acceptance requires a fresh disposable run-java8 directory')
    work.mkdir();(work/'mods').mkdir()
    java=Path(os.environ['JAVA_HOME_8_X64'])/'bin/java'
    forge=target+'-'+status['loader_version']
    installer=fetch('https://maven.minecraftforge.net/net/minecraftforge/forge/'+forge+'/forge-'+forge+'-installer.jar',ROOT/'.java8-toolchain'/('forge-'+forge+'-installer.jar'))
    run(java,'-jar',installer,'--installServer',cwd=work)
    runtime=ROOT/'build/libs'/(name+'-java8.jar')
    shutil.copyfile(runtime,work/'mods'/(name+'.jar'))
    shutil.copyfile(ROOT/'build/libs'/(name+'-java8-scenarios.jar'),work/'mods'/(name+'-scenarios.jar'))
    (work/'eula.txt').write_text('eula=true\n')
    (work/'server.properties').write_text('server-ip=127.0.0.1\nonline-mode=false\nlevel-name=scenario-world\nlevel-type=flat\nview-distance=3\nmax-players=1\nspawn-protection=0\n')
    launch=work/('forge-'+forge+'.jar')
    if not launch.is_file():raise ValueError('Installer did not create the expected Forge server JAR')
    logs=ROOT/'verification-logs';logs.mkdir(exist_ok=True)
    cmd=[str(java),'-Xms512M','-Xmx3G','-Dfoundations_pl4.portScenarioServer=true','-jar',str(launch),'--nogui']
    print('+ installed Java 8 server: '+' '.join(cmd),flush=True)
    with (logs/'installed-native.log').open('w') as out:
        process=subprocess.run(cmd,cwd=work,stdout=out,stderr=subprocess.STDOUT,timeout=900)
    log=(logs/'installed-native.log').read_text(errors='replace')
    print(log[-24000:],flush=True)
    if process.returncode:raise ValueError('Installed runtime exited '+str(process.returncode))
    report=json.loads((work/'port-scenarios.json').read_text())
    expected=status['expected_native_scenarios']
    if report['minecraft']!=target or (report['total'],report['passed'],report['failed'])!=(expected,expected,0):
        raise ValueError('Native scenario failure: '+str(report))
    if not re.search(r'Starting minecraft server version '+re.escape(target)+r'\b',log,re.I):raise ValueError('Missing native version evidence')
    if 'PL4 SCENARIOS SUCCESS: All '+str(expected)+' required native scenarios passed' not in log:raise ValueError('Missing completion marker')
    if 'Record requires ASM8' in log or 'UnsupportedClassVersionError' in log:raise ValueError('Java 8 loader compatibility failed')
    if sha(work/'mods'/(name+'.jar'))!=sha(runtime):raise ValueError('Installed runtime changed')
    summary={'minecraft':target,'version':version,'runtime_java':8,'build_java':17,'forge':status['loader_version'],
        'native_total':expected,'native_passed':report['passed'],'native_failed':report['failed'],'runtime_sha256':sha(runtime),
        'source_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        'installed_runtime':'PASS','stock_forge_libraries':True,'runtime_agent_required':False,
        'client_visuals':'PENDING','installed_optional_apis':'PENDING','multiplayer':'PENDING','further_api_testing_required':True}
    (logs/'java8-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    shutil.copyfile(work/'port-scenarios.json',logs/'port-scenarios.json')
    print('PASS installed Minecraft '+target+' / Forge '+status['loader_version']+' / Java 8 with '+str(expected)+' native scenarios',flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('action',choices=['convert','verify','native'])
    try:globals()[parser.parse_args().action]()
    except (OSError,ValueError,KeyError,StopIteration,subprocess.SubprocessError,zipfile.BadZipFile) as error:
        sys.exit('JAVA8 PORT FAILED: '+str(error))
