"""Create a world with the immutable preview.3 JAR, then verify it with this candidate."""
from pathlib import Path
import hashlib
import json
import os
import re
import shutil
import subprocess
import zipfile

ROOT=Path(__file__).resolve().parents[1]

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def verify():
    status=json.loads((ROOT/'BUILD_STATUS.json').read_text())
    mc=status['minecraft']
    if mc not in {'1.12.2','1.6.4'}:
        raise ValueError('Unsupported migration target')
    baseline='0.2a-legacy-preview.3'
    tag=f'mc{mc}-v{baseline}'
    published_name=f'FoundationsPL4-{mc}-{baseline}.jar'
    suffix='-srg.jar' if mc=='1.6.4' else '.jar'
    runtime=ROOT/'build/libs'/f"FoundationsPL4-{mc}-{status['version']}{suffix}"
    fixtures=list((ROOT/'build/libs').glob('*-migration-scenarios'+suffix))
    if len(fixtures)!=1 or not runtime.is_file():
        raise ValueError('Expected exact candidate and isolated migration fixture JARs')
    with zipfile.ZipFile(runtime) as archive:
        if any('/tests/' in n or 'MigrationScenarios' in n for n in archive.namelist()):
            raise ValueError('Migration fixture contaminated the production JAR')
    work=ROOT/'run-legacy-migration'
    if work.exists():
        raise ValueError('Use a fresh disposable migration directory')
    work.mkdir()
    for path in (ROOT/'run-legacy').iterdir():
        if path.is_file() and path.suffix=='.jar':
            shutil.copyfile(path,work/path.name)
        elif path.name in {'libraries','lib','coremods'}:
            shutil.copytree(path,work/path.name)
    (work/'mods').mkdir()
    old=work/'baseline-release'
    old.mkdir()
    subprocess.run(['gh','release','download',tag,'--repo',os.environ['GITHUB_REPOSITORY'],
                    '--pattern',published_name,'--pattern','SHA256SUMS.txt','--dir',str(old)],check=True)
    old_runtime=old/published_name
    expected=[]
    for line in (old/'SHA256SUMS.txt').read_text(encoding='ascii').splitlines():
        parts=line.split(maxsplit=1)
        if len(parts)==2 and parts[1].lstrip('*')==published_name:
            expected.append(parts[0].lower())
    if expected!=[digest(old_runtime)]:
        raise ValueError('Baseline runtime does not match its immutable release checksum')
    (work/'server.properties').write_text('server-ip=127.0.0.1\nonline-mode=false\nlevel-name=pl4-legacy-migration-world\nlevel-type=FLAT\nview-distance=3\nmax-players=1\nspawn-protection=0\n')
    (work/'eula.txt').write_text('eula=true\n')
    shutil.copyfile(fixtures[0],work/'mods/pl4-migration-scenarios.jar')
    forge='9.11.1.1345' if mc=='1.6.4' else '14.23.5.2864'
    launchers=list(work.glob('*forge*'+forge+'*.jar'))
    if len(launchers)!=1:
        raise ValueError('Expected one exact installed Forge launcher')
    java=Path(os.environ['JAVA_HOME_7_X64' if mc=='1.6.4' else 'JAVA_HOME_8_X64'])/'bin/java'
    logs=ROOT/'verification-logs'
    for phase,selected in [('write',old_runtime),('upgrade',runtime)]:
        shutil.copyfile(selected,work/'mods/FoundationsPL4-runtime.jar')
        if digest(work/'mods/FoundationsPL4-runtime.jar')!=digest(selected):
            raise ValueError('Installed migration runtime hash mismatch')
        with (logs/f'migration-{phase}.log').open('w') as output:
            result=subprocess.run([str(java),'-Xms512M','-Xmx2G',f'-Dfoundations_pl4.legacyMigration={phase}',
                                   '-jar',str(launchers[0]),'nogui'],cwd=work,stdout=output,stderr=subprocess.STDOUT,timeout=420)
        report=json.loads((work/f'legacy-migration-{phase}.json').read_text())
        log=(logs/f'migration-{phase}.log').read_text(errors='replace')
        if result.returncode or report!={'minecraft':mc,'phase':phase,'passed':True}:
            print(log[-16000:],flush=True)
            raise ValueError('Migration phase failed: '+phase)
        if f'PL4 LEGACY MIGRATION {phase}: PASS' not in log or not re.search('Starting minecraft server version '+re.escape(mc)+r'\b',log,re.I):
            raise ValueError('Missing exact-target migration success: '+phase)
        for marker in ['missing a mapping','It will not persist','Exception in server tick loop','UnsupportedClassVersionError']:
            if marker in log:
                raise ValueError('Migration runtime/save error: '+marker)
        if not (work/'pl4-legacy-migration-world/level.dat').is_file():
            raise ValueError('No persisted world after '+phase)
        print(f'PASS {mc} migration {phase}; installed JAR {digest(selected)}',flush=True)
    summary={'minecraft':mc,'baseline_tag':tag,'baseline_runtime_sha256':digest(old_runtime),
             'candidate_version':status['version'],'candidate_runtime_sha256':digest(runtime),
             'migration_fixture_sha256':digest(fixtures[0]),'forge':forge,'runtime_java':7 if mc=='1.6.4' else 8,
             'source_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
             'generated_preview3_world_upgrade':'PASS',
             'preserved':['owner','facing','item escrow','fluid tank','fluid escrow','energy storage','energy escrow','vanilla inventory'],
             'new_monitoring_on_existing_inventory':'PASS','old_hosts_default_private':'PASS',
             'real_user_worlds':'PENDING','installed_optional_mods':'PENDING','client_visuals':'PENDING'}
    (logs/'legacy-migration-summary.json').write_text(json.dumps(summary,indent=2)+'\n')

if __name__=='__main__':
    verify()
