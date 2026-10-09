"""Add the explicit launcher compatibility requirement after shared Java 8 preparation."""
from pathlib import Path
import json,shutil,sys
root=Path(sys.argv[1]).resolve()
status=json.loads((root/'BUILD_STATUS.json').read_text())
if status['minecraft']!='1.16.4':raise SystemExit('This profile is only for Forge 35.1.37')
shutil.copyfile(Path(__file__).parent/'java8/forge35_profile.py',root/'tools/java8/forge35_profile.py')
p=root/'tools/java8/runtime.py';text=p.read_text()
old="    cmd=[str(java),'-Xms512M','-Xmx3G','-Dfoundations_pl4.portScenarioServer=true','-jar',str(launch),'--nogui']"
new="""    from forge35_profile import command
    # Avoid the native loader's first-run async-config-write race in the test workspace.
    (work/'config').mkdir(exist_ok=True)
    (work/'config/fml.toml').write_text('maxThreads=1\\ndefaultConfigPath="defaultconfigs"\\n')
    cmd,profile=command(work,str(java),['-Xms512M','-Xmx3G','-Dfoundations_pl4.portScenarioServer=true'])
    (logs/'launch-profile.json').write_text(json.dumps(profile,indent=2)+'\\n')"""
if old not in text:raise SystemExit('Unexpected shared native launcher command')
text=text.replace(old,new).replace("'stock_forge_libraries':True", "'stock_forge_libraries':False,'modlauncher':'8.1.3'")
p.write_text(text)
status['runtime_profile']='Forge 35.1.37 / Java 8 with explicit ModLauncher 8.1.3 precedence'
status['stock_forge_runtime_compatible']=False
(root/'BUILD_STATUS.json').write_text(json.dumps(status,indent=2)+'\n')
notice='''
## Required Forge 35 launch profile

Forge 35.1.37 bundles ModLauncher 8.0.9, which calls a JDK-internal constructor
removed from current Java 8 updates. This release therefore requires **ModLauncher
8.1.3 to precede the bundled launcher on the classpath**. This is NOT a stock
Forge-35/current-Java-8 acceptance claim. Do not downgrade Java to avoid this issue.

A standalone helper is included as `forge35_profile.py` in the release. With Python
3 and an installed Forge 1.16.4-35.1.37 server, run:

`python forge35_profile.py --server /path/to/server --java /path/to/java8 --launch`

Without `--launch` it only fetches the pinned-version official library, checks the
upstream checksum, records its SHA256 and prints the launch command. It does not
replace or rename any installed Forge library. Clients need equivalent library
precedence in their launcher; a ready-made client-launcher profile and graphical
acceptance are still pending. The mod alone does not repair a broken launcher.
'''
for rel in ['README.md','docs/releases/'+status['version']+'.md']:
 p=root/rel;p.write_text(p.read_text()+notice)
print('Prepared explicit Forge-35 ModLauncher profile; no stock-runtime claim.')
