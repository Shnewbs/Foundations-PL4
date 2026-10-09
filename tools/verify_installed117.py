"""Native production-class scenarios plus independent production-only boot on installed Forge 37."""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,sys,time,urllib.request,zipfile
R=Path(__file__).resolve().parents[1]
MC='1.17.1'; FORGE='37.1.1'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    status=json.loads((R/'BUILD_STATUS.json').read_text());version=status['version'];name='FoundationsPL4-'+MC+'-'+version
    prod=R/'build/libs'/(name+'.jar');tests=R/'build/libs'/(name+'-native-scenarios.jar')
    compared=0
    with zipfile.ZipFile(prod) as runtime,zipfile.ZipFile(tests) as fixture:
        for entry in runtime.namelist():
            if entry.endswith('/') or entry in {'META-INF/mods.toml','META-INF/MANIFEST.MF'}:continue
            if entry.endswith('GameTests.class') or '/compat/scenarios/' in entry:raise ValueError('Fixture leaked into published runtime')
            if runtime.read(entry)!=fixture.read(entry):raise ValueError('Production/test artifact content differs: '+entry)
            compared+=1
        if compared<100:raise ValueError('Missing production classes/resources')
    work=R/'run-installed-117'
    if work.exists():raise ValueError('Use a clean disposable run-installed-117 directory')
    work.mkdir();(work/'mods').mkdir();(work/'config').mkdir()
    (work/'config/fml.toml').write_text('versionCheck=true\nsplashscreen=true\nmaxThreads=1\ndefaultConfigPath="defaultconfigs"\n')
    (work/'eula.txt').write_text('eula=true\n')
    (work/'server.properties').write_text('server-ip=127.0.0.1\nonline-mode=false\nlevel-name=scenario-world\nlevel-type=flat\nview-distance=3\nmax-players=1\nspawn-protection=0\n')
    java=Path(os.environ['JAVA_HOME'])/'bin/java';installer=work/'forge-installer.jar'
    url=f'https://maven.minecraftforge.net/net/minecraftforge/forge/{MC}-{FORGE}/forge-{MC}-{FORGE}-installer.jar'
    with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'FoundationsPL4-native-tests'}),timeout=120) as response:
        with installer.open('wb') as out:shutil.copyfileobj(response,out)
    subprocess.run([str(java),'-jar',str(installer),'--installServer'],cwd=work,check=True,timeout=300)
    argsfile=work/'libraries/net/minecraftforge/forge'/(MC+'-'+FORGE)/'unix_args.txt'
    if not argsfile.is_file():raise ValueError('Missing installed native Forge arguments')
    logs=R/'verification-logs';logs.mkdir(exist_ok=True)
    combined=work/'mods'/tests.name;shutil.copyfile(tests,combined)
    command=[str(java),'-Xms512M','-Xmx3G','-Dfoundations_pl4.portScenarioServer=true','@'+str(argsfile),'nogui']
    with (logs/'installed-scenarios.log').open('w') as out:result=subprocess.run(command,cwd=work,stdout=out,stderr=subprocess.STDOUT,timeout=900)
    text=(logs/'installed-scenarios.log').read_text(errors='replace');print(text[-18000:],flush=True)
    if result.returncode:raise ValueError('Scenario server returned '+str(result.returncode))
    report=json.loads((work/'port-scenarios.json').read_text())
    if report['minecraft']!=MC or (report['total'],report['passed'],report['failed'])!=(193,193,0):raise ValueError('Native failures: '+str(report))
    if 'PL4 SCENARIOS SUCCESS: All 193 required native scenarios passed' not in text:raise ValueError('Missing scenario completion marker')
    if not re.search(r'Starting minecraft server version 1\.17\.1\b',text,re.I):raise ValueError('Native target mismatch')
    shutil.copyfile(work/'port-scenarios.json',logs/'port-scenarios.json')
    # Only the disposable test module is removed. Production is independently booted without tests.
    combined.unlink();installed=work/'mods'/prod.name;shutil.copyfile(prod,installed)
    smoke=logs/'production-boot.log'
    with smoke.open('w') as out:
        process=subprocess.Popen([str(java),'-Xms512M','-Xmx3G','@'+str(argsfile),'nogui'],cwd=work,stdin=subprocess.PIPE,stdout=out,stderr=subprocess.STDOUT,text=True)
        try:
            deadline=time.monotonic()+180;ready=False
            while process.poll() is None and time.monotonic()<deadline:
                if re.search(r'Done \([0-9.,]+s\)!',smoke.read_text(errors='replace')):ready=True;break
                time.sleep(1)
            if not ready:raise ValueError('Production-only server never reached ready')
            process.stdin.write('stop\n');process.stdin.flush();process.wait(timeout=90)
            if process.returncode:raise ValueError('Production-only server failed shutdown')
        finally:
            if process.poll() is None:process.kill();process.wait()
    boot=smoke.read_text(errors='replace');print(boot[-5000:])
    if 'PL4 SCENARIO START' in boot or 'Stopping server' not in boot:raise ValueError('Invalid production-only boot/shutdown')
    if sha(prod)!=sha(installed):raise ValueError('Installed runtime bytes changed')
    summary={'minecraft':MC,'forge':FORGE,'version':version,'java':17,'native_total':193,'native_passed':193,'native_failed':0,
        'harness':'Combined test-only JPMS module; all production class/resource bytes compared against the standalone runtime',
        'production_entries_compared':compared,'production_only_boot_and_shutdown':'PASS','runtime_sha256':sha(prod),
        'test_artifact_sha256':sha(tests),'source_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=R,text=True).strip(),
        'client_visuals':'PENDING','installed_optional_apis':'PENDING','multiplayer':'PENDING','further_api_testing_required':True}
    (logs/'installed-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    print('PASS all 193 installed native scenarios, identical production entries and independent production-only boot/shutdown')
if __name__=='__main__':
    try:main()
    except (OSError,ValueError,KeyError,subprocess.SubprocessError,zipfile.BadZipFile) as error:sys.exit('INSTALLED 1.17.1 VALIDATION FAILED: '+str(error))
