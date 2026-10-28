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
def require_legacy_decode_terrain_observations(report,phase,minecraft):
    """Reject missing or fabricated pre-cleanup native terrain/chunk receipts."""
    if minecraft!='1.6.4' or not phase.endswith('write') or not str(report.get('suite','')).startswith('decode-'):
        return
    rows=report.get('terrain_before_cleanup')
    if not isinstance(rows,list) or len(rows)!=4 or any(not isinstance(r,dict) for r in rows):
        raise ValueError('Missing or duplicated native pre-cleanup terrain observations')
    by_x={r.get('x'):r for r in rows}
    if len(by_x)!=4 or set(by_x)!={64,69,80,85}:
        raise ValueError('Wrong native pre-cleanup terrain columns')
    for x,r in by_x.items():
        if (type(r.get('chunk_x')) is not int or r['chunk_x']!=x>>4
            or type(r.get('chunk_z')) is not int or r['chunk_z']!=2
            or r.get('chunk_loaded') is not True or r.get('host_exists') is not True
            or any(type(r.get(k)) is not int or r[k]<0 for k in
                   ('host_block','overhead_falling_blocks','live_falling_entities'))):
            raise ValueError('Unqualified pre-cleanup terrain/chunk evidence x='+str(x)+' '+str(r)[:1000])

def require_legacy_decode_terrain_log_match(report,native_log,phase,minecraft):
    """Bind native pre-cleanup observations to their separately retained stdout, in order."""
    if minecraft!='1.6.4' or not phase.endswith('write') or not str(report.get('suite','')).startswith('decode-'):
        return
    require_legacy_decode_terrain_observations(report,phase,minecraft)
    encoded=re.findall(r'PL4 DECODE TERRAIN BEFORE ([^\r\n]+)',native_log)
    if len(encoded)!=4:
        raise ValueError('Expected four independent native pre-cleanup terrain log lines')
    try:
        logged=[json.loads(value) for value in encoded]
    except (TypeError,ValueError) as error:
        raise ValueError('Malformed native pre-cleanup terrain log JSON') from error
    if any(not isinstance(row,dict) for row in logged) or [row.get('x') for row in logged]!=[64,69,80,85]:
        raise ValueError('Missing, reordered or duplicated native terrain log columns')
    canonical=lambda value:json.dumps(value,sort_keys=True,separators=(',',':'))
    if canonical(logged)!=canonical(report['terrain_before_cleanup']):
        raise ValueError('Native pre-cleanup terrain log/report disagreement')
    for x in (64,69,80,85):
        prefix='PL4 DECODE TERRAIN BEFORE '
        trace=next((value for value in encoded if json.loads(value).get('x')==x),None)
        before=native_log.find(prefix+trace) if trace is not None else -1
        guard=native_log.find('PL4 DECODE GRAVITY GUARD x='+str(x)+' ')
        if before<0 or guard<0 or before>=guard:
            raise ValueError('Native terrain observation was not before cleanup x='+str(x))
    if report.get('suite')=='decode-rewrite':
        actor=native_log.find('PL4 DECODE GRAVITY ADVERSARY x=80 spawned=true removed=true')
        before=native_log.find('PL4 DECODE TERRAIN BEFORE '+encoded[2])
        if actor<0 or before<0 or before>=actor:
            raise ValueError('Native ambient terrain must precede deliberate rewrite gravel actor')

def require_legacy_decode_placements(report,phase,minecraft):
    """Bind the exact two native hosts to complete, ordered, unmodified placement receipts."""
    if minecraft!='1.6.4' or not phase.endswith('write') or not str(report.get('suite','')).startswith('decode-'):
        return
    rows=report.get('placements')
    if not isinstance(rows,list) or len(rows)!=2 or any(not isinstance(row,dict) for row in rows):
        raise ValueError('Missing, extra or malformed native decode host placements')
    if [row.get('x') for row in rows]!=[64,80]:
        raise ValueError('Missing, reordered or duplicated exact native decode host placements')
    for row in rows:
        x=row['x']
        if (type(x) is not int or type(row.get('y')) is not int or row['y']!=200
            or type(row.get('z')) is not int or row['z']!=32
            or type(row.get('chunk_x')) is not int or row['chunk_x']!=x>>4
            or type(row.get('chunk_z')) is not int or row['chunk_z']!=2
            or row.get('stage')!='complete' or row.get('block_exists_before') is not True
            or type(row.get('before_block')) is not int or row['before_block']!=0
            or type(row.get('placement_flags')) is not int or row['placement_flags']!=2
            or row.get('set_block_changed') is not True
            or type(row.get('expected_block')) is not int or row['expected_block']!=3501
            or type(row.get('actual_block')) is not int or row['actual_block']!=3501
            or type(row.get('metadata')) is not int or row['metadata']!=0
            or row.get('tile_class')!='net.foundations.pl4.legacy.LegacyTile'
            or row.get('tile_invalid') is not False or row.get('block_exists_after') is not True):
            details={key:row.get(key) for key in ('stage','chunk_x','chunk_z','block_exists_before','before_block','set_block_changed','placement_flags','expected_block','actual_block','metadata','tile_class','tile_invalid','block_exists_after','neighbor_nonair','falling_entities_near')}
            raise ValueError('Unqualified native decode host placement at x='+str(x)+' '+json.dumps(details,sort_keys=True,separators=(',',':'))[:3000])

def require_legacy_decode_placement_log_match(report,native_log,phase,minecraft):
    """Refuse JSON-only native placement evidence without matching ordered JVM stdout."""
    if minecraft!='1.6.4' or not phase.endswith('write') or not str(report.get('suite','')).startswith('decode-'):
        return
    require_legacy_decode_placements(report,phase,minecraft)
    encoded=re.findall(r'PL4 DECODE PLACEMENT ([^\r\n]+)',native_log)
    if len(encoded)!=2:
        raise ValueError('Expected exactly two independent native placement log rows')
    try:
        logged=[json.loads(value) for value in encoded]
    except (ValueError,TypeError) as error:
        raise ValueError('Malformed native placement log JSON') from error
    if any(not isinstance(row,dict) for row in logged) or [row.get('x') for row in logged]!=[64,80]:
        raise ValueError('Missing, reordered or duplicate native placement log hosts')
    canonical=lambda value:json.dumps(value,sort_keys=True,separators=(',',':'))
    if canonical(logged)!=canonical(report['placements']):
        raise ValueError('Native placement log/report disagreement')
    for i,(x,adjacent) in enumerate(((64,69),(80,85))):
        marker=native_log.find('PL4 DECODE PLACEMENT '+encoded[i])
        for guard_x in (x,adjacent):
            guard=native_log.find('PL4 DECODE GRAVITY GUARD x='+str(guard_x)+' ')
            if guard<0 or marker<0 or guard>=marker:
                raise ValueError('Native placement was not after gravity isolation x='+str(x))

def require_legacy_decode_gravity_guards(native_log,phase,minecraft,suite=None):
    """Require both native column isolation and a deliberately spawned actor in rewrite."""
    if minecraft!='1.6.4' or not phase.endswith('write'):return
    rows=re.findall(r'PL4 DECODE GRAVITY GUARD x=(\d+) removed_blocks=(\d+) removed_entities=(\d+) ceiling=(true|false)',native_log)
    if len(rows)!=4 or {int(row[0]) for row in rows}!={64,69,80,85} or any(row[3]!='true' for row in rows):
        raise ValueError('Missing, duplicated or failed native fixture gravity guards')
    if suite=='decode-rewrite':
        adversary=re.findall(r'PL4 DECODE GRAVITY ADVERSARY x=80 spawned=(true|false) removed=(true|false)',native_log)
        if adversary!=[('true','true')] or int(next(row[2] for row in rows if row[0]=='80'))<1:
            raise ValueError('Missing or unremoved real native rewrite falling-gravel adversary')

def retain_decode_placement_failure(logs,prefix,phase,pid,code,logfile,report,error):
    """Emit the exact failed placement observation even when the JVM's phase fails."""
    record=dict(suite=report.get('suite'),phase=phase,pid=pid,exit_code=code,
                log_sha256=digest(logfile),failure=str(error),placements=report.get('placements'),
                terrain_before_cleanup=report.get('terrain_before_cleanup'))
    destination=logs/(prefix+'-'+phase+'-placement-rejection.json')
    destination.write_text(json.dumps(record,indent=2,sort_keys=True)+'\n')
    print('PL4 NATIVE PLACEMENT REJECTION '+json.dumps(record,sort_keys=True)[:4096],flush=True)
    return destination
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
            try:
                require_legacy_decode_terrain_observations(report,phase,mc)
                require_legacy_decode_terrain_log_match(report,text,phase,mc)
                require_legacy_decode_placements(report,phase,mc)
                require_legacy_decode_placement_log_match(report,text,phase,mc)
            except ValueError as error:
                retain_decode_placement_failure(logs,prefix,phase,pid,code,logfile,report,error)
                raise
            if decode:require_legacy_decode_gravity_guards(text,phase,mc,suite)
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
