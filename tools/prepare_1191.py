"""Prepare independent Minecraft 1.19.1 / Forge42.0.9 full-feature target."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
p=R/'BUILD_STATUS.json'
status=json.loads(p.read_text())
if status.get('minecraft')=='1.19.1':
    if status.get('loader_version')!='42.0.9':raise SystemExit('Wrong existing Forge target')
    print('Exact Forge42 source identity already prepared')
    raise SystemExit(0)
if status.get('minecraft')!='1.19.2' or status.get('loader_version')!='43.5.2':
    raise SystemExit('Unexpected native source baseline')
gradle=R/'build.gradle';s=gradle.read_text()
assert "net.minecraftforge:forge:1.19.2-43.5.2" in s
assert 'versionRange="[1.19.2,1.19.3)"' in s
gradle.write_text(s.replace('1.19.2','1.19.1').replace('43.5.2','42.0.9')
                    .replace('versionRange="[1.19.1,1.19.3)"','versionRange="[1.19.1,1.19.2)"'),encoding='utf-8')
p=R/'src/main/resources/META-INF/mods.toml';s=p.read_text()
assert '1.19.2' in s and '43.5.2' in s
s=s.replace('1.19.2','1.19.1').replace('43.5.2','42.0.9')
s=s.replace('loaderVersion="[43,)"','loaderVersion="[42,43)"')
s=s.replace('versionRange="[42.0.9,44)"','versionRange="[42.0.9,43)"')
s=s.replace('versionRange="[1.19.1,1.19.3)"','versionRange="[1.19.1,1.19.2)"')
p.write_text(s,encoding='utf-8')
p=R/'src/main/resources/pack.mcmeta';meta=json.loads(p.read_text())
assert meta['pack']['pack_format']==9
meta['pack']['description']=meta['pack']['description'].replace('1.19.2','1.19.1')
p.write_text(json.dumps(meta,indent=2)+'\n',encoding='utf-8')
p=R/'tools/run_port_checks.py';s=p.read_text()
assert "status['minecraft']=='1.19.2'" in s
p.write_text(s.replace("status['minecraft']=='1.19.2'","status['minecraft']=='1.19.1'"),encoding='utf-8')
for filename in ['README.md','docs/FIELD_GUIDE.md']:
    p=R/filename
    p.write_text(p.read_text().replace('1.19.2','1.19.1').replace('43.5.2','42.0.9'),encoding='utf-8')
p=R/'src/main/resources/assets/foundations_pl4/guide/en_us.json'
if p.exists():p.write_text(p.read_text().replace('1.19.2','1.19.1'),encoding='utf-8')
status.update(minecraft='1.19.1',loader='Forge',loader_version='42.0.9',java=17,
    payload_protocol='forge-1.19.1-5',expected_native_tests=191,
    port_status='INDEPENDENT_NATIVE_ACCEPTANCE_PENDING',release_published=False,
    installed_runtime_acceptance='PENDING',installed_optional_provider_acceptance='PENDING',
    client_visual_acceptance='PENDING',live_multiplayer_acceptance='PENDING',
    full_pl2_parity=False,further_api_testing_required=True)
(R/'BUILD_STATUS.json').write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')
(R/'PORT_TARGET.json').write_text(json.dumps({
    'minecraft':'1.19.1','forge':'42.0.9','loader':'Forge','java':17,
    'baseline':'mc/1.19.2','required_native_tests':191,'release_allowed':False,
    'installed_optional_apis':'PENDING','client_visuals':'PENDING','multiplayer':'PENDING'
},indent=2)+'\n',encoding='utf-8')
(R/'README.md').write_text('''# Foundations PL4 — Minecraft 1.19.1 / Forge42

Minecraft **1.19.1** / Forge **42.0.9** / Java **17**, independently
ported from the full-feature 1.19.2 source.

This is not a relabeled 1.19.2 JAR. Native API and gameplay tests must pass
before release. Original multipart cable/data networks, displays and editors,
owner-separated wireless storage, Forge-sided item/fluid/energy transfers and
saved escrow, sapphire ore and forging work remain the feature baseline.

Publication is gated on exact-version Forge compilation, reobfuscation,
191/191 native GameTests, resource identity and SHA256-verified JAR/sources.
Installed optional third-party APIs, real-client visuals, live multiplayer
and sustained modpack performance remain independent acceptance steps.

**Further API testing is still required.**
''',encoding='utf-8')
(R/'docs/releases/0.2a-port.1.md').write_text('''# Foundations PL4 1.19.1 — 0.2a-port.1

Native full-feature Forge **42.0.9** / Minecraft **1.19.1** / Java17
alpha candidate. Carries PL4 multipart logic, network readers/displays,
owner checks, item/fluid/energy transfer, wireless network storage,
saved resources and forging. A binary can only be published after exact
build/reobfuscation and all 191 native GameTests pass. CurseForge submission
then uses the actual matching runtime JAR and game version.

Real-client and installed optional-provider validation remain open.
**Further API testing is still required.**
''',encoding='utf-8')
print('Prepared independently versioned Minecraft 1.19.1 / Forge 42.0.9 source')
