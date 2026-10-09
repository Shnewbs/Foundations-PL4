"""Prepare exact 1.15.2 full-feature source; subsequent API fixes are never overwritten."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
status=json.loads((R/'BUILD_STATUS.json').read_text())
if status['minecraft']=='1.15.2':raise SystemExit(0)
if status['minecraft']!='1.16.5':raise SystemExit('Unexpected source baseline')
for root in [R/'src',R/'tools/java8']:
 for p in root.rglob('*'):
  if p.is_file() and p.suffix in {'.java','.py','.json','.toml','.mcmeta'}:
   s=p.read_text().replace('1.16.5','1.15.2').replace('0.2a-port.2','0.2a-port.1')
   if p.name=='mods.toml':s=s.replace('[36,)','[31,32)').replace('[36.2.42,37)','[31.2.62,32)').replace('[1.15.2,1.17)','[1.15.2]')
   if p.name=='pack.mcmeta':s=s.replace('"pack_format":6','"pack_format":5').replace('"pack_format": 6','"pack_format": 5')
   p.write_text(s)
p=R/'build.gradle';s=p.read_text().replace('1.16.5','1.15.2').replace('36.2.42','31.2.62').replace('0.2a-port.2','0.2a-port.1').replace('[1.15.2,1.17)','[1.15.2]');p.write_text(s)
p=R/'tools/run_port_checks.py';s=p.read_text().replace('1.16.5','1.15.2').replace("['pack_format']==6","['pack_format']==5");p.write_text(s)
p=R/'docs/FIELD_GUIDE.md';p.write_text(p.read_text().replace('1.16.5','1.15.2').replace('0.2a-port.2','0.2a-port.1'))
status.update(minecraft='1.15.2',loader_version='31.2.62',version='0.2a-port.1',java=8,build_java=17,
 payload_protocol='forge-1.15.2-5',port_status='SOURCE_API_PORTING',installed_runtime_acceptance='PENDING',
 release_notes='docs/releases/0.2a-port.1.md',expected_native_scenarios=191,further_api_testing_required=True)
(R/'BUILD_STATUS.json').write_text(json.dumps(status,indent=2)+'\n')
print('Prepared exact Minecraft 1.15.2 / Forge 31.2.62 / Java 8 output; API/runtime validation is pending.')
