"""Prepare the exact full-feature 1.14.4 Forge 28 target, never rename another JAR."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
sp=R/'BUILD_STATUS.json'
status=json.loads(sp.read_text())
if status['minecraft']=='1.14.4':
 assert status['loader_version']=='28.2.26'
 print('Target 1.14.4 already configured')
 raise SystemExit(0)
if status['minecraft']!='1.15.2':raise SystemExit('Unreviewed baseline')
for dirname in ['src','tools/java8']:
 for p in (R/dirname).rglob('*'):
  if p.is_file() and p.suffix in {'.java','.py','.json','.toml','.mcmeta','.gradle'}:
   s=p.read_text(encoding='utf-8').replace('1.15.2','1.14.4')
   if p.name=='mods.toml':
    s=s.replace('[31,32)','[28,29)').replace('[31.2.62,32)','[28.2.26,29)').replace('31.2.62','28.2.26')
   if p.name=='pack.mcmeta':
    data=json.loads(s);data['pack']['pack_format']=4;s=json.dumps(data,indent=2)+'\n'
   p.write_text(s,encoding='utf-8')
build=R/'build.gradle';s=build.read_text()
assert '1.15.2-31.2.62' in s
build.write_text(s.replace('1.15.2','1.14.4').replace('31.2.62','28.2.26'))
p=R/'tools/run_port_checks.py';s=p.read_text()
p.write_text(s.replace('1.15.2','1.14.4').replace("['pack_format']==5","['pack_format']==4"))
p=R/'docs/FIELD_GUIDE.md';p.write_text(p.read_text().replace('1.15.2','1.14.4'))
status.update(minecraft='1.14.4',loader_version='28.2.26',java=8,build_java=17,
 payload_protocol='forge-1.14.4-5',port_status='SOURCE_PORTING',native_compilation='PENDING',
 installed_runtime_acceptance='PENDING',installed_optional_provider_acceptance='PENDING',
 client_visual_acceptance='PENDING',live_multiplayer_acceptance='PENDING',
 release_notes='docs/releases/0.2a-port.1.md',full_pl2_parity=False,
 further_api_testing_required=True,forge31_api_pass=False)
sp.write_text(json.dumps(status,indent=2)+'\n')
(R/'PORT_TARGET.json').write_text(json.dumps(dict(minecraft='1.14.4',loader='Forge',loader_version='28.2.26',java=8,release_allowed=False),indent=2)+'\n')
(R/'README.md').write_text('''# Foundations PL4 1.14.4 native port — in progress

Minecraft 1.14.4 / recommended Forge 28.2.26 / Java 8, built using Java 17
with conversion to Java 8 bytecode. Derived from full-feature 1.15.2 source,
not the limited legacy transport implementation. Actual native APIs, storage,
networking, visuals and recipes still require independent port validation.

**NOT a released runtime.** The workflow must pass target-native compilation,
all 194 isolated scenarios, archive checks and production-only startup before
a binary may be released. No 1.15.2 JAR is compatible by renaming.
**Further API testing is required.**
''',encoding='utf-8')
p=R/'docs/releases/0.2a-port.1.md'
p.write_text('''# Foundations PL4 1.14.4 — 0.2a-port.1 candidate

Forge 28.2.26 (Java 8 runtime) full-feature port. Still awaiting native
compilation, installed dedicated-server scenarios and production-only startup.
No release or third-party API acceptance is claimed. Conversion to Java 8
bytecode does not alone establish gameplay compatibility.
**Further API testing is required.**
''')
print('Prepared Minecraft 1.14.4 / Forge 28.2.26 source metadata; no binary release.')
