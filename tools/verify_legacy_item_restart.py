"""Exercise candidate item uncertainty through separate native JVMs in disposable worlds."""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def verify():
    status=json.loads((ROOT/'BUILD_STATUS.json').read_text());mc=status['minecraft']
    if mc not in {'1.12.2','1.6.4'}:raise ValueError('Unsupported exact restart target')
    suffix='-srg.jar' if mc=='1.6.4' else '.jar'
    runtime=ROOT/'build/libs'/f"FoundationsPL4-{mc}-{status['version']}{suffix}"
    fixtures=list((ROOT/'build/libs').glob('*-restart-scenarios'+suffix))
    if len(fixtures)!=1 or not runtime.is_file():raise ValueError('Expected isolated restart fixture and exact runtime')
    with zipfile.ZipFile(runtime) as z:
        if any('/tests/' in n or 'ItemRestartScenarios' in n for n in z.namelist()):raise ValueError('Production runtime contains fixture classes')
    source=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    forge='9.11.1.1345' if mc=='1.6.4' else '14.23.5.2864'
    java=Path(os.environ['JAVA_HOME_7_X64' if mc=='1.6.4' else 'JAVA_HOME_8_X64'])/'bin/java'
    logs=ROOT/'verification-logs';logs.mkdir(exist_ok=True);processes=[];reports={};archives={}
    for mode,phases in [('clean',['write','verify']),('barrier',['crash-write','crash-verify'])]:
        work=ROOT/('run-legacy-item-restart-'+mode)
        if work.exists():raise ValueError('Use a fresh disposable restart directory')
        work.mkdir()
        for p in (ROOT/'run-legacy').iterdir():
            if p.is_file() and p.suffix=='.jar':shutil.copyfile(p,work/p.name)
            elif p.name in {'libraries','lib','coremods'}:shutil.copytree(p,work/p.name)
        (work/'mods').mkdir();shutil.copyfile(runtime,work/'mods/FoundationsPL4-runtime.jar');shutil.copyfile(fixtures[0],work/'mods/pl4-item-restart-scenarios.jar')
        (work/'eula.txt').write_text('eula=true\n')
        (work/'server.properties').write_text('server-ip=127.0.0.1\nonline-mode=false\nlevel-name=pl4-item-restart-world\nlevel-type=FLAT\nlevel-seed=0\nview-distance=3\nmax-players=1\nspawn-protection=0\n')
        launchers=list(work.glob('*forge*'+forge+'*.jar'))
        if len(launchers)!=1:raise ValueError('Expected exact installed Forge launcher')
        for phase in phases:
            if digest(work/'mods/FoundationsPL4-runtime.jar')!=digest(runtime):raise ValueError('Installed runtime changed')
            cmd=[str(java),'-Xms512M','-Xmx2G','-Dfoundations_pl4.itemRestart='+phase]
            if phase=='crash-write':cmd+=['-Dfoundations_pl4.restartHalt=true']
            cmd+=['-jar',str(launchers[0]),'nogui'];logfile=logs/('item-restart-'+phase+'.log')
            with logfile.open('w') as out:
                with subprocess.Popen(cmd,cwd=work,stdout=out,stderr=subprocess.STDOUT) as proc:
                    try:code=proc.wait(timeout=420)
                    finally:
                        if proc.poll() is None:proc.kill();proc.wait()
                    pid=proc.pid
            text=logfile.read_text(errors='replace');expected=73 if phase=='crash-write' else 0
            report=json.loads((work/('item-restart-'+phase+'.json')).read_text())
            if code!=expected or report['minecraft']!=mc or report['phase']!=phase or report['passed'] is not True or not report['assertions']:raise ValueError('Restart phase failed: '+phase+'; inspect '+str(logfile))
            if not report['process'].startswith(str(pid)+'@') or 'PL4 ITEM RESTART '+phase+': PASS' not in text or not re.search('Starting minecraft server version '+re.escape(mc)+r'\b',text,re.I):raise ValueError('Missing process/native identity')
            if ('Stopping server' in text)==(phase=='crash-write'):raise ValueError('Unexpected shutdown path')
            for marker in ['missing a mapping','It will not persist','Exception in server tick loop','UnsupportedClassVersionError']:
                if marker in text:raise ValueError('Runtime/save error: '+marker)
            if not (work/'pl4-item-restart-world/level.dat').is_file():raise ValueError('No saved level')
            processes.append(dict(phase=phase,pid=pid,exit_code=code,log_sha256=digest(logfile)))
            reports[phase]=report
            (logs/('item-restart-'+phase+'.json')).write_text(json.dumps(report,indent=2)+'\n')
        if reports[phases[0]]['tokens']!=reports[phases[1]]['tokens'] or len(set(reports[phases[0]]['tokens']))!=2:raise ValueError('Tokens changed across JVMs')
        archive=logs/('item-restart-'+mode+'-world.zip')
        with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
            for p in sorted((work/'pl4-item-restart-world').rglob('*')):
                if p.is_file() and not p.is_symlink():z.write(p,p.relative_to(work).as_posix())
        archives[mode]=dict(file=archive.name,sha256=digest(archive))
    if len({p['pid'] for p in processes})!=4:raise ValueError('Processes were not distinct')
    summary=dict(minecraft=mc,source_commit=source,runtime_sha256=digest(runtime),fixture_sha256=digest(fixtures[0]),clean_restart='PASS',save_barrier_abrupt_restart='PASS',processes=processes,phase_reports=reports,world_archives=archives,scope='Separate candidate JVMs; clean stop and Runtime.halt(73) after completed native world save/storage save/flush. Not arbitrary pre-save crashes, power loss, pre-upgrade faults, user worlds, installed providers, client or multiplayer acceptance. Source post-removal exception leaves 8 units unaccounted for and fenced; insertion exception leaves 32+8 observed spendable units plus a sealed uncertain 8-unit claim. No recovery/replay/minting.')
    (logs/'legacy-item-restart-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    print('PASS '+mc+' separate JVM item/receipt restart and controlled post-save halt',flush=True)
if __name__=='__main__':verify()
