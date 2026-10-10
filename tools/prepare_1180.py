"""Prepare the exact Minecraft 1.18 / Forge 38.0.17 full-feature port.

Minecraft 1.18 is a distinct release from 1.18.1. No neighboring JAR is renamed.
This script changes target metadata only; Forge38 API fixes require native CI.
"""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
status_file=ROOT/'BUILD_STATUS.json'
status=json.loads(status_file.read_text())
if status.get('minecraft')=='1.18':
    if status.get('loader_version')!='38.0.17':raise SystemExit('Unexpected Forge 38 target configuration')
    print('Exact 1.18 port already prepared')
    raise SystemExit(0)
if status.get('minecraft')!='1.18.1' or status.get('loader_version')!='39.1.2':
    raise SystemExit('Refusing incompatible baseline: expected reviewed 1.18.1 / Forge 39.1.2 source')

build=ROOT/'build.gradle'
s=build.read_text()
if "minecraft 'net.minecraftforge:forge:1.18.1-39.1.2'" not in s or 'versionRange="[1.18.1,1.18.2)"' not in s:
    raise SystemExit('Unexpected Gradle Forge39 source')
build.write_text(s.replace('1.18.1','1.18').replace('39.1.2','38.0.17')
    .replace('versionRange="[1.18,1.18.2)"','versionRange="[1.18,1.18.1)"'),encoding='utf-8')

metadata=ROOT/'src/main/resources/META-INF/mods.toml'
s=metadata.read_text()
if '39.1.2' not in s or '1.18.1' not in s:raise SystemExit('Incorrect Forge39 metadata baseline')
s=s.replace('39.1.2','38.0.17').replace('1.18.1','1.18')
s=s.replace('loaderVersion="[39,40)"','loaderVersion="[38,39)"')
s=s.replace('versionRange="[38.0.17,40)"','versionRange="[38.0.17,39)"')
s=s.replace('versionRange="[1.18,1.18.2)"','versionRange="[1.18,1.18.1)"')
metadata.write_text(s,encoding='utf-8')

pack=ROOT/'src/main/resources/pack.mcmeta'
obj=json.loads(pack.read_text())
if obj['pack']['pack_format']!=8:raise SystemExit('Unexpected pack_format on 1.18.1')
obj['pack']['description']=obj['pack']['description'].replace('1.18.1','1.18')
pack.write_text(json.dumps(obj,indent=2)+'\n',encoding='utf-8')

p=ROOT/'tools/run_port_checks.py'
s=p.read_text()
if "status['minecraft']=='1.18.1'" not in s:raise SystemExit('Unexpected native port rules')
p.write_text(s.replace("status['minecraft']=='1.18.1'","status['minecraft']=='1.18'"),encoding='utf-8')

for name in ['README.md','docs/FIELD_GUIDE.md']:
    p=ROOT/name
    p.write_text(p.read_text().replace('1.18.1','1.18').replace('39.1.2','38.0.17'),encoding='utf-8')
guide=ROOT/'src/main/resources/assets/foundations_pl4/guide/en_us.json'
if guide.exists():guide.write_text(guide.read_text().replace('1.18.1','1.18'),encoding='utf-8')

status.update(minecraft='1.18',loader='Forge',loader_version='38.0.17',java=17,build_java=17,
    payload_protocol='forge-1.18-5',port_status='NATIVE_API_ACCEPTANCE_PENDING',release_published=False,
    installed_runtime_acceptance='PENDING',installed_optional_provider_acceptance='PENDING',
    client_visual_acceptance='PENDING',live_multiplayer_acceptance='PENDING',
    full_pl2_parity=False,expected_native_tests=193,further_api_testing_required=True)
status_file.write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')
(ROOT/'PORT_TARGET.json').write_text(json.dumps(
  dict(minecraft='1.18',loader='Forge',loader_version='38.0.17',runtime_java=17,
       source_baseline='mc/1.18.1',expected_native_tests=193,release_allowed=False,
       installed_client='PENDING',optional_api_acceptance='PENDING'),indent=2)+'\n',encoding='utf-8')
(ROOT/'README.md').write_text('''# Foundations PL4 — Minecraft 1.18 / Forge 38

Exact Forge 38.0.17 / Minecraft 1.18 / Java 17 target. This is full-feature
source derived from the independently tested 1.18.1 branch, not a renamed
1.18.1 binary. No release exists until native tests pass.

Target functionality: multipart cables, displays/editors, network item storage,
saved transfer escrow, energy/fluid handling, forging recipes, sapphire ore
generation and guides. Older APIs require a separate compatibility audit.

Publishing requires independent target compilation, reobfuscation, all 193
original native gameplay tests, verified checksum/source assets and no accidental
third-party dependency bundling. Installed optional API, real-client visual
and live multiplayer acceptance remain separate milestones.

**Further API testing is still required.**
''',encoding='utf-8')
(ROOT/'docs/releases/0.2a-port.1.md').write_text('''# Foundations PL4 1.18 — 0.2a-port.1

**Alpha candidate. Minecraft 1.18 / Forge 38.0.17 / Java 17.**

The full PL4-derived Forge source retains cables, readers, displays, editing,
wireless inventory storage, items/fluids/energy, safety escrow, hammer crafting
and native sapphire overworld generation. Release is conditional on native
compilation and 193/193 installed Forge38 GameTests; a relabeled 1.18.1 binary
is not compatible.

**Further API testing is required** for graphical rendering, installed optional
providers, multiplayer and sustained modpack performance.
''',encoding='utf-8')
print('Prepared exact Minecraft 1.18 / Forge 38.0.17 source; native compilation and 193 GameTests pending')
