"""Import old native faulted tiles without reconstructing their unknown routes.

Historical bytes are pinned by source/runtime/archive hashes. New hourly artifacts
retain those bytes and their native report so expiry of the first upload need not
turn into an attempt to run an obsolete publishing workflow.
"""
from pathlib import Path
import hashlib,io,json,os,re,shutil,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
BASELINES={
 '1.12.2':dict(artifact=12617877227,source='cc6a7b2aefda7362215848a348b93b9a2950764d',archive='0f3ba07e9ff432d8a00818bc5dfbe72368948c809e5933865ea603e3fcef3bb5',runtime='a16797ab3f1ac367578057725976f5818ae3a7a56ca5d242a4e1d2e3c0f243ff',count=253),
 '1.6.4':dict(artifact=12617856905,source='0dc2f1242308ccf5c062b1b1659011923589c0aa',archive='1cbec90d178d08b6123fb4c07f3a2be862bcc6ef753550dfb176aec1b7a3bd94',runtime='52b34855549eca1a64e85b39cf92f6bf59cf9613aecd2e65ddca4758990bf282',count=230)}
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def qualified_baseline(raw,mc,original):
    pin=BASELINES[mc]
    if original and hashlib.sha256(raw).hexdigest()!=pin['archive']:raise ValueError('Original baseline archive changed')
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        report=json.loads(z.read('verification-logs/hourly-native.json' if original else 'verification-logs/preupgrade-baseline-native.json'))
        name=f"FoundationsPL4-{mc}-0.2.0-rc.1-dev"+('-srg.jar' if mc=='1.6.4' else '.jar')
        runtime=z.read('build/libs/'+name if original else 'verification-logs/preupgrade-baseline-runtime.jar')
    if (report.get('minecraft')!=mc or report.get('source_commit')!=pin['source'] or report.get('native_gate')!='PASS'
        or report.get('expected_fixtures')!=pin['count'] or report.get('artifact_sha256',{}).get(name)!=pin['runtime']
        or hashlib.sha256(runtime).hexdigest()!=pin['runtime']):raise ValueError('Unqualified historical runtime')
    return runtime,report
def api(path):return json.loads(subprocess.check_output(['gh','api',path],text=True))
def baseline(mc,logs):
    repo=os.environ['GITHUB_REPOSITORY'];pin=BASELINES[mc];prefix=f'repos/{repo}/actions/'
    first=api(prefix+'artifacts/'+str(pin['artifact']))
    candidates=[first] if not first.get('expired') else []
    if not candidates:
        candidates=[a for a in api(prefix+'artifacts?per_page=100')['artifacts'] if not a.get('expired') and a['name'].startswith('hourly-native-'+mc+'-')]
    for a in candidates:
        original=a['id']==pin['artifact']
        run=api(prefix+'runs/'+str(a['workflow_run']['id']))
        if run.get('path')!='.github/workflows/hourly-builds.yml' or run.get('head_branch')!='main' or run.get('status')!='completed':continue
        raw=subprocess.check_output(['gh','api',prefix+'artifacts/'+str(a['id'])+'/zip'])
        if hashlib.sha256(raw).hexdigest()!=a['digest'].removeprefix('sha256:'):raise ValueError('Retained archive checksum mismatch')
        try:runtime,report=qualified_baseline(raw,mc,original)
        except (ValueError,KeyError):
            if original:raise
            continue
        (logs/'preupgrade-baseline-runtime.jar').write_bytes(runtime)
        (logs/'preupgrade-baseline-native.json').write_text(json.dumps(report,indent=2)+'\n')
        origin=dict(original_artifact_id=pin['artifact'],original_archive_sha256=pin['archive'],retrieved_artifact_id=a['id'],retrieved_archive_sha256=a['digest'],source_commit=pin['source'],runtime_sha256=pin['runtime'])
        (logs/'preupgrade-baseline-origin.json').write_text(json.dumps(origin,indent=2)+'\n')
        return origin
    raise ValueError('No retained qualified historical fault runtime; refresh this test input without publishing or relabeling a JAR')
def verify():
    status=json.loads((ROOT/'BUILD_STATUS.json').read_text());mc=status['minecraft'];pin=BASELINES[mc]
    suffix='-srg.jar' if mc=='1.6.4' else '.jar';runtime=ROOT/'build/libs'/f"FoundationsPL4-{mc}-{status['version']}{suffix}"
    fixtures=list((ROOT/'build/libs').glob('*-migration-scenarios'+suffix))
    if len(fixtures)!=1:raise ValueError('Expected isolated exact migration fixture')
    source=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip();logs=ROOT/'verification-logs';logs.mkdir(exist_ok=True)
    origin=baseline(mc,logs);work=ROOT/'run-legacy-preupgrade-item-faults'
    if work.exists():raise ValueError('Fresh private fault migration directory required')
    work.mkdir()
    for p in (ROOT/'run-legacy').iterdir():
        if p.is_file() and p.suffix=='.jar':shutil.copyfile(p,work/p.name)
        elif p.name in {'libraries','lib','coremods'}:shutil.copytree(p,work/p.name)
    (work/'mods').mkdir();shutil.copyfile(fixtures[0],work/'mods/pl4-migration-scenarios.jar')
    (work/'eula.txt').write_text('eula=true\n');(work/'server.properties').write_text('server-ip=127.0.0.1\nonline-mode=false\nlevel-name=pl4-preupgrade-fault-world\nlevel-type=FLAT\nlevel-seed=0\nview-distance=3\nmax-players=1\nspawn-protection=0\n')
    forge='9.11.1.1345' if mc=='1.6.4' else '14.23.5.2864';launchers=list(work.glob('*forge*'+forge+'*.jar'))
    if len(launchers)!=1:raise ValueError('Expected exact native Forge launcher')
    java=Path(os.environ['JAVA_HOME_7_X64' if mc=='1.6.4' else 'JAVA_HOME_8_X64'])/'bin/java';rows=[]
    for phase,selected,count in [('fault-write',logs/'preupgrade-baseline-runtime.jar',4),('fault-upgrade',runtime,10),('fault-verify',runtime,10)]:
        shutil.copyfile(selected,work/'mods/FoundationsPL4-runtime.jar')
        if digest(work/'mods/FoundationsPL4-runtime.jar')!=digest(selected):raise ValueError('Installed runtime changed')
        logfile=logs/('preupgrade-'+phase+'.log')
        with logfile.open('w') as output:
            with subprocess.Popen([str(java),'-Xms512M','-Xmx2G','-Dfoundations_pl4.legacyMigration='+phase,'-jar',str(launchers[0]),'nogui'],cwd=work,stdout=output,stderr=subprocess.STDOUT) as proc:
                try:code=proc.wait(timeout=420)
                finally:
                    if proc.poll() is None:proc.kill();proc.wait()
                pid=proc.pid
        report=json.loads((work/('legacy-migration-'+phase+'.json')).read_text());text=logfile.read_text(errors='replace')
        if code!=0 or report.get('passed') is not True or report.get('minecraft')!=mc or report.get('phase')!=phase or report.get('checks')!=count or not report.get('process','').startswith(str(pid)+'@'):raise ValueError('Historical native phase failed: '+phase)
        if 'PL4 LEGACY MIGRATION '+phase+': PASS' not in text or 'Stopping server' not in text or not re.search('Starting minecraft server version '+re.escape(mc)+r'\b',text,re.I):raise ValueError('Missing native phase/process markers')
        for marker in ['missing a mapping','It will not persist','Exception in server tick loop','UnsupportedClassVersionError']:
            if marker in text:raise ValueError('Native save/runtime error: '+marker)
        rows.append(dict(phase=phase,pid=pid,exit_code=code,runtime_sha256=digest(selected),log_sha256=digest(logfile),report=report))
        (logs/('preupgrade-'+phase+'.json')).write_text(json.dumps(report,indent=2)+'\n')
    if len({r['pid'] for r in rows})!=3 or len(set(rows[1]['report']['tokens']))!=2 or rows[1]['report']['tokens']!=rows[2]['report']['tokens']:raise ValueError('Independent process/token proof failed')
    archive=logs/'preupgrade-item-fault-world.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for p in sorted((work/'pl4-preupgrade-fault-world').rglob('*')):
            if p.is_file() and not p.is_symlink():z.write(p,p.relative_to(work).as_posix())
    summary=dict(minecraft=mc,source_commit=source,runtime_sha256=digest(runtime),fixture_sha256=digest(fixtures[0]),baseline=origin,processes=rows,preupgrade_unlocated_fault_import='PASS',candidate_restart='PASS',world_archive=dict(file=archive.name,sha256=digest(archive)),scope='Generated faults saved by actual pre-journal native runtime, candidate chunk import and clean candidate restart after host removal. Unknown original targets/phases remain unknown. Loaded observed owner fenced dimension-wide; unowned faults fail closed for all owners. Pre-existing loose unlinked receipts remain unauthenticated. Not arbitrary user/modpack migration, provider-private outcomes or crash atomicity. No recovery/replay/mint/clear API.')
    (logs/'preupgrade-item-fault-summary.json').write_text(json.dumps(summary,indent=2)+'\n');print('PASS '+mc+' pre-journal native world import and candidate process restart',flush=True)
if __name__=='__main__':verify()
