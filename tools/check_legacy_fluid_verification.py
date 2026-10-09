"""Check native cleanup, scenario results and the exact runtime installed for verification."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys
import zipfile

target=sys.argv[1]
if target not in {'1.12.2','1.6.4'}:raise SystemExit('Unsupported target')
root=Path.cwd();name='FoundationsPL4-'+target+'-0.2a-legacy-preview.3'
suffix='-srg.jar' if target=='1.6.4' else '.jar'
built=root/'build/libs'/(name+suffix)
installed=root/'run-legacy/mods'/(name+suffix)
report=json.loads((root/'run-legacy/legacy-scenarios.json').read_text())
if report['minecraft']!=target or report['total']!=89 or report['passed']!=89 or report['failed']!=0:
    raise SystemExit('Native scenario failure: '+str(report))
log=(root/'verification-logs/native.log').read_text(encoding='utf-8',errors='replace')
if 'PL4 LEGACY FIXTURE CLEANUP: PASS' not in log or 'missing a mapping' in log or 'It will not persist' in log:
    raise SystemExit('Native test fixture cleanup/save validation failed')
reference=Path(os.environ.get('PL4_VERIFICATION_RUNTIME',str(built)))
if installed.read_bytes()!=reference.read_bytes():raise SystemExit('Installed runtime differs from selected runtime')
def entries(path):
    with zipfile.ZipFile(path) as z:
        return {name:z.read(name) for name in z.namelist() if not name.endswith('/')}
if entries(built)!=entries(reference):raise SystemExit('Rebuilt runtime contents differ from the selected published runtime')
summary={
    'minecraft':target,'version':'0.2a-legacy-preview.3','total':89,'passed':89,'failed':0,
    'fixture_cleanup':'PASS','fixture_save_mapping_error':False,
    'published_runtime_used':bool(os.environ.get('PL4_VERIFICATION_RUNTIME')),
    'runtime_sha256':hashlib.sha256(installed.read_bytes()).hexdigest(),
    'rebuilt_archive_entries_match':True,
    'verification_commit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),
    'runtime_forge':'9.11.1.1345' if target=='1.6.4' else '14.23.5.2864',
    'runtime_java':7 if target=='1.6.4' else 8,
    'build_api_forge':'9.11.1.960' if target=='1.6.4' else '14.23.5.2864',
    'further_api_testing_required':True,
    'scope':'Isolated native scenarios; not installed third-party mod, real-client or multiplayer acceptance'
}
(root/'verification-logs/clean-native-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
print('PASS 89 native scenarios, cleanup/save guard, exact installed runtime hash and rebuilt archive contents')
