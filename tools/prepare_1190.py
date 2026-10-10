"""Prepare independent Minecraft 1.19 / Forge 41.1.0 native PL4 port."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]; p=R/'BUILD_STATUS.json';status=json.loads(p.read_text())
if status.get('minecraft')=='1.19':
    assert status['loader_version']=='41.1.0'
    print('Native Forge41 target already prepared')
    raise SystemExit(0)
if status.get('minecraft')!='1.19.1' or status.get('loader_version')!='42.0.9':
    raise SystemExit('Refusing non-1.19.1 source baseline')
b=R/'build.gradle';s=b.read_text()
assert '1.19.1-42.0.9' in s and 'versionRange="[1.19.1,1.19.2)"' in s
b.write_text(s.replace('1.19.1','1.19').replace('42.0.9','41.1.0').replace('versionRange="[1.19,1.19.2)"','versionRange="[1.19,1.19.1)"'),encoding='utf-8')
p=R/'src/main/resources/META-INF/mods.toml';s=p.read_text()
assert '1.19.1' in s and '42.0.9' in s
s=s.replace('1.19.1','1.19').replace('42.0.9','41.1.0')
s=s.replace('loaderVersion="[42,43)"','loaderVersion="[41,42)"')
s=s.replace('versionRange="[41.1.0,43)"','versionRange="[41.1.0,42)"')
s=s.replace('versionRange="[1.19,1.19.2)"','versionRange="[1.19,1.19.1)"')
p.write_text(s,encoding='utf-8')
p=R/'src/main/resources/pack.mcmeta';obj=json.loads(p.read_text())
assert obj['pack']['pack_format']==9
obj['pack']['description']=obj['pack']['description'].replace('1.19.1','1.19')
p.write_text(json.dumps(obj,indent=2)+'\n',encoding='utf-8')
p=R/'tools/run_port_checks.py';s=p.read_text()
assert "status['minecraft']=='1.19.1'" in s
p.write_text(s.replace("status['minecraft']=='1.19.1'","status['minecraft']=='1.19'"),encoding='utf-8')
for filename in ('README.md','docs/FIELD_GUIDE.md'):
    p=R/filename
    if p.exists():p.write_text(p.read_text().replace('1.19.1','1.19').replace('42.0.9','41.1.0'),encoding='utf-8')
p=R/'src/main/resources/assets/foundations_pl4/guide/en_us.json'
if p.exists():p.write_text(p.read_text().replace('1.19.1','1.19'),encoding='utf-8')
status.update(minecraft='1.19',loader='Forge',loader_version='41.1.0',
     payload_protocol='forge-1.19-5',port_status='NATIVE_API_PORT_PENDING',
     native_compilation='PENDING',expected_native_tests=191,release_published=False,
     installed_runtime_acceptance='PENDING',installed_optional_provider_acceptance='PENDING',
     client_visual_acceptance='PENDING',live_multiplayer_acceptance='PENDING',
     full_pl2_parity=False,further_api_testing_required=True)
(R/'BUILD_STATUS.json').write_text(json.dumps(status,indent=2)+'\n')
(R/'PORT_TARGET.json').write_text(json.dumps({
     'minecraft':'1.19','loader':'Forge','forge':'41.1.0','java':17,
     'source_baseline':'mc/1.19.1','required_native_tests':191,
     'release_allowed':False,'client_visuals':'PENDING','installed_optional_apis':'PENDING'},indent=2)+'\n')
(R/'README.md').write_text('''# Foundations PL4 1.19 — Forge41 source port

This branch independently targets **Minecraft 1.19 / Forge 41.1.0 / Java 17**
using the separately tested full-feature 1.19.1 source. It is not a
relabelled 1.19.1 JAR. The full multipart network, storage, displays/editor,
energy and fluid routing, ownership and saved transfers remain in source.

A playable alpha needs independent Forge41 compilation, artifact metadata and
reobfuscation checks, and **191 original native GameTests passed**. Real-client,
installed optional APIs, live multiplayer, and sustained modpack performance
remain separate acceptance gates; third-party energy conversions must be
grounded in actual adapters and conservation semantics.

**Further API testing is required.**
''',encoding='utf-8')
(R/'docs/releases/0.2a-port.1.md').write_text('''# Foundations PL4 1.19 / Forge41 — 0.2a-port.1

Native Forge 41.1.0 target, derived from full 1.19.1 source with multipart
cables, displays/editor, energy/fluid/item transport, wireless item storage
and saved escrow. GitHub publication is gated on independent 191/191 native
GameTests and exact source/JAR checksums; CurseForge submission follows
only an accepted GitHub Release. Not a stable compatibility certification.
**Further installed optional API, real-client and multiplayer testing needed.**
''',encoding='utf-8')
print('Prepared exact Minecraft 1.19 / Forge41; 191 native scenarios required')
