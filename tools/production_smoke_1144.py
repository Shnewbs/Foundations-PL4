"""Prove stock Forge 28 starts and stops with only the PL4 production runtime."""
from pathlib import Path
import hashlib,json,os,queue,re,shutil,subprocess,sys,threading,time
ROOT=Path(__file__).resolve().parents[1]
def smoke():
    status=json.loads((ROOT/'BUILD_STATUS.json').read_text())
    target=status['minecraft'];version=status['version'];name=f'FoundationsPL4-{target}-{version}'
    source=ROOT/'run-java8';work=ROOT/'run-java8-production'
    if work.exists():raise ValueError('Use a fresh disposable smoke directory')
    work.mkdir();(work/'mods').mkdir()
    for p in source.iterdir():
        if p.is_file() and p.suffix=='.jar':shutil.copyfile(p,work/p.name)
        elif p.name == 'libraries':shutil.copytree(p,work/p.name)
    runtime=ROOT/'build/libs'/(name+'-java8.jar');shutil.copyfile(runtime,work/'mods'/(name+'.jar'))
    (work/'eula.txt').write_text('eula=true\n')
    (work/'server.properties').write_text('server-ip=127.0.0.1\nonline-mode=false\nlevel-name=production-world\nlevel-type=flat\nview-distance=3\nmax-players=1\nspawn-protection=0\n')
    java=Path(os.environ['JAVA_HOME_8_X64'])/'bin/java';forge=work/('forge-'+target+'-'+status['loader_version']+'.jar');
    if not forge.is_file():raise ValueError('Stock Forge28 server JAR not present')
    args=[str(java),'-Xms512M','-Xmx3G','-jar',str(forge),'--nogui'];profile={'stock_forge_server':True,'forge':status['loader_version'],'runtime_java':8}
    logs=ROOT/'verification-logs';logs.mkdir(exist_ok=True)
    started=False;stopped=False;lines=[];pending=queue.Queue()
    with subprocess.Popen(args,cwd=work,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1) as process:
        def reader():
            for line in process.stdout:pending.put(line)
            pending.put(None)
        threading.Thread(target=reader,daemon=True).start();deadline=time.monotonic()+300
        try:
            with (logs/'production-smoke.log').open('w') as out:
                while time.monotonic()<deadline:
                    try:line=pending.get(timeout=1)
                    except queue.Empty:
                        if process.poll() is not None:break
                        continue
                    if line is None:break
                    lines.append(line);out.write(line);out.flush()
                    if 'Done (' in line and 'For help' in line and not started:
                        started=True;process.stdin.write('stop\n');process.stdin.flush()
                    if 'Stopping server' in line:stopped=True
                if process.poll() is None:process.wait(timeout=60)
        finally:
            if process.poll() is None:process.kill();process.wait()
    text=''.join(lines)
    if process.returncode or not started or not stopped:raise ValueError('Production-only startup/shutdown failed')
    if not re.search(r'Starting minecraft server version '+re.escape(target)+r'\b',text,re.I):raise ValueError('Wrong native target')
    if 'foundations_pl4_porttests' in text or (work/'port-scenarios.json').exists():raise ValueError('Test module present in production smoke')
    for error in ['UnsupportedClassVersionError','Record requires ASM8','Failed to load data packs','Errors in currently selected datapacks','Exception in server tick loop']:
        if error in text:raise ValueError('Runtime error: '+error)
    digest=hashlib.sha256(runtime.read_bytes()).hexdigest()
    if hashlib.sha256((work/'mods'/(name+'.jar')).read_bytes()).hexdigest()!=digest:raise ValueError('Smoke artifact mismatch')
    report={'minecraft':target,'forge':status['loader_version'],'runtime_java':8,'runtime_sha256':digest,'production_only_startup':'PASS','clean_stop':'PASS','scenario_mod_installed':False,'runtime_profile':profile,'source_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'client_visuals':'PENDING','installed_optional_apis':'PENDING','multiplayer':'PENDING','further_api_testing_required':True}
    (logs/'production-smoke.json').write_text(json.dumps(report,indent=2)+'\n')
    print('PASS production-only boot, clean shutdown and exact JAR hash for',target)
if __name__=='__main__':
    try:smoke()
    except (OSError,ValueError,subprocess.SubprocessError) as error:sys.exit('SMOKE FAILED: '+str(error))
