"""Exercise candidate item uncertainty through separate native JVMs in disposable worlds."""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,zipfile,time
ROOT=Path(__file__).resolve().parents[1]
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def retain_phase_report(work,logs,prefix,phase,pid,code,logfile):
    """Preserve exact raw phase bytes and process facts before any acceptance check."""
    source=work/(prefix+'-'+phase+'.json');target=logs/source.name
    if source.is_file():shutil.copyfile(source,target)
    observation=dict(phase=phase,pid=pid,exit_code=code,log_sha256=digest(logfile),report_present=source.is_file(),report_sha256=digest(target) if source.is_file() else None)
    (logs/(prefix+'-'+phase+'-process.json')).write_text(json.dumps(observation,indent=2)+'\n')
    if not source.is_file():raise ValueError('Missing restart phase report: '+phase+'; inspect '+str(logfile))
    return json.loads(target.read_text())
def require_legacy_decode_placements(report,phase,minecraft):
    """Bind 1.6.4 decode admission to two real native host placements."""
    if minecraft!='1.6.4' or not phase.endswith('write') or not str(report.get('suite','')).startswith('decode-'):
        return
    rows=report.get('placements')
    if not isinstance(rows,list):raise ValueError('Missing native decode placement observations')
    hosts=[row for row in rows if isinstance(row,dict) and row.get('x') in (64,80)]
    if len(hosts)!=2 or {row['x'] for row in hosts}!={64,80}:
        raise ValueError('Missing or duplicated exact native decode host placements')
    for row in hosts:
        if (row.get('y')!=200 or row.get('z')!=32 or row.get('stage')!='complete'
            or row.get('placement_flags')!=2 or row.get('set_block_changed') is not True
            or row.get('expected_block')!=3501 or row.get('actual_block')!=3501
            or row.get('tile_class')!='net.foundations.pl4.legacy.LegacyTile'
            or row.get('tile_invalid') is not False or row.get('block_exists_after') is not True):
            raise ValueError('Unqualified native decode host placement at x='+str(row.get('x')))

def verify(suite="item"):
    decode=suite in {"decode-raw","decode-missing","decode-compound","decode-owner","decode-dimension","decode-scope","decode-rewrite","decode-live","decode-live-context","decode-live-owner"}
    if suite!="item" and not decode:raise ValueError("Unknown restart suite")
    prefix="item-"+suite+"-restart" if decode else "item-restart"
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
        work=ROOT/('run-legacy-'+prefix+'-'+mode)
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
            if decode:cmd+=['-Dfoundations_pl4.decodeRestart='+suite.removeprefix('decode-')]
            if phase=='crash-write':cmd+=['-Dfoundations_pl4.restartHalt=true']
            cmd+=['-jar',str(launchers[0]),'nogui'];logfile=logs/(prefix+'-'+phase+'.log')
            with logfile.open('w') as out:
                with subprocess.Popen(cmd,cwd=work,stdout=out,stderr=subprocess.STDOUT) as proc:
                    try:
                        if phase=='crash-write':
                            deadline=time.monotonic()+420
                            while proc.poll() is None and time.monotonic()<deadline:
                                ready=work/(prefix+'-'+phase+'.json')
                                if ready.is_file() and 'PL4 ITEM RESTART KILL READY' in logfile.read_text(errors='replace'):
                                    proof=json.loads(ready.read_text())
                                    if proof.get('passed') is True and proof.get('process','').startswith(str(proc.pid)+'@'):
                                        proc.kill();break
                                time.sleep(.1)
                        code=proc.wait(timeout=60 if phase=='crash-write' else 420)
                    finally:
                        if proc.poll() is None:proc.kill();proc.wait()
                    pid=proc.pid
            text=logfile.read_text(errors='replace');expected=-9 if phase=='crash-write' else 0
            report=retain_phase_report(work,logs,prefix,phase,pid,code,logfile)
            require_legacy_decode_placements(report,phase,mc)
            if code!=expected or report['minecraft']!=mc or report['phase']!=phase or report['passed'] is not True or not report['assertions']:raise ValueError('Restart phase failed: '+phase+'; inspect '+str(logfile))
            if not report['process'].startswith(str(pid)+'@') or 'PL4 ITEM RESTART '+phase+': PASS' not in text or not re.search('Starting minecraft server version '+re.escape(mc)+r'\b',text,re.I):raise ValueError('Missing process/native identity')
            if ('Stopping server' in text)==(phase=='crash-write'):raise ValueError('Unexpected shutdown path')
            for marker in ['missing a mapping','It will not persist','Exception in server tick loop','UnsupportedClassVersionError']:
                if marker in text:raise ValueError('Runtime/save error: '+marker)
            if not (work/'pl4-item-restart-world/level.dat').is_file():raise ValueError('No saved level')
            processes.append(dict(phase=phase,pid=pid,exit_code=code,log_sha256=digest(logfile)))
            reports[phase]=report
        if reports[phases[0]]['tokens']!=reports[phases[1]]['tokens'] or len(set(reports[phases[0]]['tokens']))!=2:raise ValueError('Tokens changed across JVMs')
        if decode:
            if reports[phases[0]].get('decode_outcomes')!=reports[phases[1]].get('decode_outcomes') or len(reports[phases[0]].get('decode_outcomes',[]))!=2:raise ValueError('Decode evidence changed across JVMs')
            if any(reports[p].get('suite')!=suite for p in phases):raise ValueError('Decode suite identity mismatch')
        if not decode and status.get('item_provider_fixture') is True:
            if reports[phases[0]].get('provider_outcomes')!=reports[phases[1]].get('provider_outcomes') or len(reports[phases[0]].get('provider_outcomes',[]))!=2:raise ValueError('Provider private state changed across JVMs')
        archive=logs/(prefix+'-'+mode+'-world.zip')
        with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
            for p in sorted((work/'pl4-item-restart-world').rglob('*')):
                if p.is_file() and not p.is_symlink():z.write(p,p.relative_to(work).as_posix())
        archives[mode]=dict(file=archive.name,sha256=digest(archive))
    if len({p['pid'] for p in processes})!=4:raise ValueError('Processes were not distinct')
    summary=dict(suite=suite,minecraft=mc,source_commit=source,runtime_sha256=digest(runtime),fixture_sha256=digest(fixtures[0]),clean_restart='PASS',save_barrier_abrupt_restart='PASS',processes=processes,phase_reports=reports,world_archives=archives,scope='Separate candidate JVMs; clean stop and Parent SIGKILL after completed native world save/storage save/flush and explicit ready marker. Not arbitrary pre-save crashes, power loss, pre-upgrade faults, user worlds, installed providers, client or multiplayer acceptance. Registered fixture provider and its own private identity/owner/counters remain installed across all four JVMs. This is not third-party provider acceptance. Source post-removal exception leaves 8 units unaccounted for and fenced; insertion exception leaves 32+8 observed spendable units plus a sealed uncertain 8-unit claim. No recovery/replay/minting.')
    if decode:summary['scope']='Exact suite-selected malformed or rewritten context, original evidence, sealed removal receipt and opaque dimension barrier across separate clean and completed-save/flush parent-SIGKILL JVMs. No pre-save crash, power-loss, installed-provider, recovery, multiplayer or RC clearance.'
    (logs/('legacy-'+prefix+'-summary.json')).write_text(json.dumps(summary,indent=2)+'\n')
    print('PASS '+mc+' separate JVM item/receipt restart and controlled post-save halt',flush=True)
if __name__=='__main__':
    verify()
    if json.loads((ROOT/'BUILD_STATUS.json').read_text()).get('item_decode_restart_fixture') is True:
        verify('decode-raw');verify('decode-missing')
    if json.loads((ROOT/'BUILD_STATUS.json').read_text()).get('item_compound_restart_fixture') is True:verify('decode-compound')

    if json.loads((ROOT/'BUILD_STATUS.json').read_text()).get('item_field_restart_fixture') is True:
        for suite in ['decode-owner','decode-dimension','decode-scope','decode-rewrite']:verify(suite)

    if json.loads((ROOT/'BUILD_STATUS.json').read_text()).get('item_live_mutation_restart_fixture') is True:verify('decode-live')

    if json.loads((ROOT/'BUILD_STATUS.json').read_text()).get('item_context_mutation_restart_fixture') is True:verify('decode-live-context')

    if json.loads((ROOT/'BUILD_STATUS.json').read_text()).get('item_owner_mutation_restart_fixture') is True:verify('decode-live-owner')
