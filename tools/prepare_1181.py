"""Independently version full-feature PL4 for Minecraft 1.18.1 / Forge 39.1.2."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
s=R/'BUILD_STATUS.json';status=json.loads(s.read_text())
if status['minecraft']=='1.18.1':
 assert status['loader_version']=='39.1.2'
 print('Exact 1.18.1 target already configured')
 raise SystemExit(0)
if status['minecraft']!='1.18.2':raise SystemExit('Unexpected source baseline')
b=R/'build.gradle';text=b.read_text()
assert "net.minecraftforge:forge:1.18.2-40.3.12" in text
text=text.replace('1.18.2','1.18.1').replace('40.3.12','39.1.2').replace('0.2a-port.2','0.2a-port.1')
text=text.replace('versionRange="[1.18.1,1.18.3)"','versionRange="[1.18.1,1.18.2)"')
b.write_text(text,encoding='utf-8')
p=R/'src/main/resources/META-INF/mods.toml';text=p.read_text()
assert '1.18.2' in text and '40.3.12' in text
text=text.replace('1.18.2','1.18.1').replace('40.3.12','39.1.2')
text=text.replace('loaderVersion="[40,)"','loaderVersion="[39,40)"')
text=text.replace('versionRange="[39.1.2,41)"','versionRange="[39.1.2,40)"')
text=text.replace('versionRange="[1.18.1,1.18.3)"','versionRange="[1.18.1,1.18.2)"')
p.write_text(text,encoding='utf-8')
p=R/'src/main/resources/pack.mcmeta';data=json.loads(p.read_text())
assert data['pack']['pack_format']==8
data['pack']['description']=data['pack']['description'].replace('1.18.2','1.18.1')
p.write_text(json.dumps(data,indent=2)+'\n')
for name in ['README.md','docs/FIELD_GUIDE.md']:
 p=R/name
 p.write_text(p.read_text().replace('1.18.2','1.18.1').replace('40.3.12','39.1.2').replace('0.2a-port.2','0.2a-port.1'),encoding='utf-8')
p=R/'tools/run_port_checks.py';text=p.read_text()
assert "status['minecraft']=='1.18.2'" in text
p.write_text(text.replace("status['minecraft']=='1.18.2'","status['minecraft']=='1.18.1'"),encoding='utf-8')
p=R/'release-version.txt'
if p.exists():p.write_text('0.2a-port.1\n')
p=R/'src/main/resources/assets/foundations_pl4/guide/en_us.json'
if p.exists():p.write_text(p.read_text().replace('1.18.2','1.18.1').replace('0.2a-port.2','0.2a-port.1'))
status.update(minecraft='1.18.1',loader='Forge',loader_version='39.1.2',version='0.2a-port.1',
 payload_protocol='forge-1.18.1-5',port_status='NATIVE_ACCEPTANCE_PENDING',
 expected_native_tests=193,release_published=False,release_notes='docs/releases/0.2a-port.1.md',
 installed_runtime_acceptance='PENDING',installed_optional_provider_acceptance='PENDING',
 client_visual_acceptance='PENDING',live_multiplayer_acceptance='PENDING',
 full_pl2_parity=False,further_api_testing_required=True)
s.write_text(json.dumps(status,indent=2)+'\n')
(R/'docs/releases/0.2a-port.1.md').write_text('''# Foundations PL4 Minecraft 1.18.1 / 0.2a-port.1 (Forge 39)

Independent full-feature source port: multipart cables, readers, displays,
wireless item storage, Forge sided item/fluid/energy routing and saved escrow,
forging recipes, guide and sapphire overworld worldgen. Target Minecraft 1.18.1,
Forge 39.1.2, Java 17. Every published runtime must pass compilation,
reobfuscation, portable rules and 193 installed native GameTests before release.

**Further API testing required:** real client visuals, installed optional
mod/provider combinations, multiplayer and sustained performance profiling.
Do not install any neighboring version's JAR or intermediate sources.
''')
print('Prepared independent Minecraft 1.18.1 / Forge 39.1.2 source; native tests remain mandatory')
