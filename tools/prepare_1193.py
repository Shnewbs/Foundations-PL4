"""Independently target Minecraft 1.19.3 / Forge 44.1.23 from 1.19.2 source."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
status_path=ROOT/'BUILD_STATUS.json'
status=json.loads(status_path.read_text())
if status.get('minecraft')=='1.19.3':
    if status.get('loader_version')!='44.1.23':raise SystemExit('Wrong prepared loader')
    print('Already prepared exact Forge44 target');raise SystemExit(0)
if status.get('minecraft')!='1.19.2' or status.get('loader_version')!='43.5.2':
    raise SystemExit('Unsupported source baseline; do not relabel binaries')

gradle=ROOT/'build.gradle';s=gradle.read_text()
if "net.minecraftforge:forge:1.19.2-43.5.2" not in s or 'versionRange="[1.19.2,1.19.3)"' not in s:
    raise SystemExit('Source target build settings have changed')
gradle.write_text(s.replace('1.19.2','1.19.3').replace('43.5.2','44.1.23')
  .replace('versionRange="[1.19.3,1.19.3)"','versionRange="[1.19.3,1.19.4)"'),encoding='utf-8')

mods=ROOT/'src/main/resources/META-INF/mods.toml'
s=mods.read_text()
if '1.19.2' not in s or '43.5.2' not in s:raise SystemExit('Unexpected mod metadata')
s=s.replace('1.19.2','1.19.3').replace('43.5.2','44.1.23')
s=s.replace('loaderVersion="[43,)"','loaderVersion="[44,45)"')
s=s.replace('versionRange="[44.1.23,44)"','versionRange="[44.1.23,45)"')
s=s.replace('versionRange="[1.19.3,1.19.3)"','versionRange="[1.19.3,1.19.4)"')
mods.write_text(s,encoding='utf-8')

pack=ROOT/'src/main/resources/pack.mcmeta';obj=json.loads(pack.read_text())
if obj['pack']['pack_format']!=9:raise SystemExit('Unexpected 1.19.2 resource format')
obj['pack']['pack_format']=12
obj['pack']['description']=obj['pack']['description'].replace('1.19.2','1.19.3')
pack.write_text(json.dumps(obj,indent=2)+'\n',encoding='utf-8')

checks=ROOT/'tools/run_port_checks.py';s=checks.read_text()
if "status['minecraft']=='1.19.2'" not in s:raise SystemExit('Unexpected version guard')
checks.write_text(s.replace("status['minecraft']=='1.19.2'","status['minecraft']=='1.19.3'")
  .replace("['pack_format']==9","['pack_format']==12"),encoding='utf-8')
for name in ['README.md','docs/FIELD_GUIDE.md']:
    p=ROOT/name
    if p.exists():p.write_text(p.read_text().replace('1.19.2','1.19.3').replace('43.5.2','44.1.23'),encoding='utf-8')
guide=ROOT/'src/main/resources/assets/foundations_pl4/guide/en_us.json'
if guide.exists():guide.write_text(guide.read_text().replace('1.19.2','1.19.3'))
status.update(minecraft='1.19.3',loader='Forge',loader_version='44.1.23',
    payload_protocol='forge-1.19.3-5',java=17,build_java=17,
    port_status='INDEPENDENT_NATIVE_API_PORT_PENDING',expected_native_tests=191,
    release_published=False,installed_runtime_acceptance='PENDING',
    installed_optional_provider_acceptance='PENDING',client_visual_acceptance='PENDING',
    live_multiplayer_acceptance='PENDING',further_api_testing_required=True,full_pl2_parity=False)
status_path.write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')
(ROOT/'PORT_TARGET.json').write_text(json.dumps(dict(
  minecraft='1.19.3',forge='44.1.23',loader='Forge',java=17,
  baseline='mc/1.19.2',required_native_tests=191,
  release_allowed=False,optional_apis='PENDING',multiplayer='PENDING'
),indent=2)+'\n',encoding='utf-8')
(ROOT/'README.md').write_text('''# Foundations PL4 — Minecraft 1.19.3 / Forge 44

Exact Forge 44.1.23 source port, derived from separately tested Minecraft
1.19.2. The branch carries full PL4 cable/network/display/wireless
storage/transfer/energy and forging features. Native API and installed
gameplay testing must pass before any release.

The code is compiled independently, not copied from or relabeled as a
1.19.2 binary. The pack format is 12. Native Forge44 compilation,
reobfuscation and all 191 original GameTests are hard release gates.

Real-client display behavior, installed optional providers, live multiplayer
and sustained performance testing remain separate.

**Further API testing is still required.**
''',encoding='utf-8')
(ROOT/'docs/releases/0.2a-port.1.md').write_text('''# Foundations PL4 1.19.3 — 0.2a-port.1 candidate

Native Minecraft 1.19.3 / Forge 44.1.23 / Java 17 feature port.
Retains modern-derived PL4 networks, readers, display editors, item/fluid/
energy transfer, wireless storage and escrow, fabrication and sapphire worldgen.

Publication only after independent compilation, 191/191 actual native
GameTests and exact-metadata SHA256-verified GitHub artifacts. CurseForge
sync then submits the exact accepted GitHub release when configured.

**Further API testing required:** client graphics, installed APIs,
multiplayer and sustained modpack performance.
''',encoding='utf-8')
print('Prepared distinct 1.19.3 / Forge44 full-feature source with 191-test release gate')
