"""Independently port the tested full-feature Forge44 release to 1.19.4 / Forge45."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
status_path=R/'BUILD_STATUS.json'
s=json.loads(status_path.read_text())
if s.get('minecraft')=='1.19.4':
    assert s.get('loader_version')=='45.4.5'
    print('Independent 1.19.4 / Forge45 identity already prepared')
    raise SystemExit(0)
if s.get('minecraft')!='1.19.3' or s.get('loader_version')!='44.1.23':
    raise SystemExit('Unexpected baseline; refusing to reuse a neighboring runtime')
p=R/'build.gradle'
t=p.read_text()
assert '1.19.3-44.1.23' in t and 'versionRange="[1.19.3,1.19.4)"' in t
p.write_text(t.replace('1.19.3','1.19.4').replace('44.1.23','45.4.5')
    .replace('versionRange="[1.19.4,1.19.4)"','versionRange="[1.19.4,1.19.5)"'),encoding='utf-8')
p=R/'src/main/resources/META-INF/mods.toml'
t=p.read_text()
assert '1.19.3' in t and '44.1.23' in t
t=t.replace('1.19.3','1.19.4').replace('44.1.23','45.4.5')
t=t.replace('loaderVersion="[44,45)"','loaderVersion="[45,46)"')
t=t.replace('versionRange="[45.4.5,45)"','versionRange="[45.4.5,46)"')
t=t.replace('versionRange="[1.19.4,1.19.4)"','versionRange="[1.19.4,1.19.5)"')
p.write_text(t,encoding='utf-8')
p=R/'src/main/resources/pack.mcmeta';pack=json.loads(p.read_text())
assert pack['pack']['pack_format']==12
pack['pack']['pack_format']=13
pack['pack']['description']=pack['pack']['description'].replace('1.19.3','1.19.4')
p.write_text(json.dumps(pack,indent=2)+'\n',encoding='utf-8')
p=R/'tools/run_port_checks.py';t=p.read_text()
assert "status['minecraft']=='1.19.3'" in t
p.write_text(t.replace("status['minecraft']=='1.19.3'","status['minecraft']=='1.19.4'").replace("['pack_format']==12","['pack_format']==13"),encoding='utf-8')
for filename in ('README.md','docs/FIELD_GUIDE.md'):
    p=R/filename
    p.write_text(p.read_text().replace('1.19.3','1.19.4').replace('44.1.23','45.4.5'),encoding='utf-8')
p=R/'src/main/resources/assets/foundations_pl4/guide/en_us.json'
if p.exists():p.write_text(p.read_text().replace('1.19.3','1.19.4'),encoding='utf-8')
s.update(minecraft='1.19.4',loader_version='45.4.5',loader='Forge',
         payload_protocol='forge-1.19.4-5',port_status='NATIVE_ACCEPTANCE_PENDING',
         expected_native_tests=191,release_published=False,
         native_api_backport='forge45-source-baseline',native_compilation='PENDING',
         installed_runtime_acceptance='PENDING',
         installed_optional_provider_acceptance='PENDING',
         client_visual_acceptance='PENDING',live_multiplayer_acceptance='PENDING',
         full_pl2_parity=False,further_api_testing_required=True)
status_path.write_text(json.dumps(s,indent=2)+'\n',encoding='utf-8')
(R/'PORT_TARGET.json').write_text(json.dumps(dict(minecraft='1.19.4',loader='Forge',
       loader_version='45.4.5',java=17,release_allowed=False,
       source_baseline='mc/1.19.3',expected_native_tests=191),indent=2)+'\n')
(R/'README.md').write_text('''# Foundations PL4 — Minecraft 1.19.4 / Forge 45

Native source baseline: full-feature 1.19.3, independently retargeted to
Minecraft 1.19.4, Forge 45.4.5, Java 17. Not a renamed 1.19.3 JAR.

Complete PL4 multipart networks/displays/editors, wireless storage,
item/fluid/energy transfers, escrow and guides remain the intended baseline.
The alpha is released only if it compiles against its own APIs and all
191 exact-target GameTests succeed. An absent optional mod must not prevent
the tested built-in PL4 functionality; foreign energy conversion is never
guessed from missing APIs.

**Further API testing is required** for graphics, optional installed mods,
live multiplayer and modpack-scale performance.
''',encoding='utf-8')
(R/'docs/releases/0.2a-port.1.md').write_text('''# Foundations PL4 1.19.4 / Forge45 — 0.2a-port.1 alpha

Target: Minecraft **1.19.4**, Forge **45.4.5**, Java **17**.
The port retains full-feature PL4 multipart cables, displays, editors,
wireless inventory storage, item/fluid/energy adapters, saved escrow, and
the forging hammer. It must pass independent 191/191 native GameTests,
packaging/checksum verification and exact loader resource identity.
This is not an installed-mod parity certification. Further API,
real-client, multiplayer and performance testing is required.
''',encoding='utf-8')
print('Prepared independent Minecraft 1.19.4 Forge45 source and native test gates')
