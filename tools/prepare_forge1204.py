"""Prepare independent Minecraft 1.20.4 / Forge 49.2.8 full-feature port.

Retain all 191 native gameplay cases and reject installation claims for a
neighboring-version binary.
"""
from pathlib import Path
import json

R=Path(__file__).resolve().parents[1]; p=R/'BUILD_STATUS.json'
status=json.loads(p.read_text(encoding='utf-8'))
if status.get('minecraft')=='1.20.4':
    if status.get('loader_version')!='49.2.8':raise SystemExit('Wrong native Forge build configured')
    print('1.20.4 / Forge 49.2.8 already versioned')
    raise SystemExit(0)
if status.get('minecraft')!='1.20.1' or status.get('loader_version')!='47.4.26':
    raise SystemExit('Unexpected source baseline; do not borrow a neighboring JAR')
path=R/'build.gradle'
s=path.read_text(encoding='utf-8')
assert "net.minecraftforge:forge:1.20.1-47.4.26" in s
assert 'versionRange="[1.20.1,1.20.2)"' in s
path.write_text(s.replace('1.20.1','1.20.4').replace('47.4.26','49.2.8').replace(
    'versionRange="[1.20.4,1.20.2)"','versionRange="[1.20.4,1.20.5)"'),encoding='utf-8')
path=R/'src/main/resources/META-INF/mods.toml'
s=path.read_text(encoding='utf-8')
assert '1.20.1' in s and '47.4.26' in s
s=s.replace('1.20.1','1.20.4').replace('47.4.26','49.2.8')
s=s.replace('loaderVersion="[47,)"','loaderVersion="[49,50)"')
s=s.replace('versionRange="[49.2.8,48)"','versionRange="[49.2.8,50)"')
s=s.replace('versionRange="[1.20.4,1.20.2)"','versionRange="[1.20.4,1.20.5)"')
path.write_text(s,encoding='utf-8')
path=R/'src/main/resources/pack.mcmeta';meta=json.loads(path.read_text())
assert meta['pack']['pack_format']==15
meta['pack']['pack_format']=22
meta['pack']['description']=meta['pack']['description'].replace('1.20.1','1.20.4')
path.write_text(json.dumps(meta,indent=2)+'\n',encoding='utf-8')
path=R/'tools/run_port_checks.py';s=path.read_text()
assert "status['minecraft']=='1.20.1'" in s and "['pack_format']==15" in s
path.write_text(s.replace("status['minecraft']=='1.20.1'","status['minecraft']=='1.20.4'")
                 .replace("['pack_format']==15","['pack_format']==22"),encoding='utf-8')
for name in ('README.md','docs/FIELD_GUIDE.md'):
    path=R/name
    if path.exists():path.write_text(path.read_text().replace('1.20.1','1.20.4').replace('47.4.26','49.2.8'),encoding='utf-8')
path=R/'src/main/resources/assets/foundations_pl4/guide/en_us.json'
if path.exists():path.write_text(path.read_text().replace('1.20.1','1.20.4'),encoding='utf-8')
status.update(minecraft='1.20.4',loader='Forge',loader_version='49.2.8',java=17,build_java=17,
     payload_protocol='forge-1.20.4-5',port_status='NATIVE_SOURCE_PORT_PENDING',
     expected_native_tests=191,release_published=False,installed_runtime_acceptance='PENDING',
     installed_optional_provider_acceptance='PENDING',client_visual_acceptance='PENDING',
     live_multiplayer_acceptance='PENDING',full_pl2_parity=False,
     further_api_testing_required=True)
p.write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')
(R/'PORT_TARGET.json').write_text(json.dumps({
      'minecraft':'1.20.4','loader':'Forge','forge':'49.2.8','runtime_java':17,
      'source_baseline':'mc/1.20.1','expected_native_tests':191,
      'release_gates':['native compile/reobfuscation','191/191 real GameTests','exact checksummed JAR/source'],
      'installed_optional_mods':'PENDING'},indent=2)+'\n',encoding='utf-8')
(R/'README.md').write_text('''# Foundations PL4 — Minecraft 1.20.4 / Forge 49.2.8

Independent full-feature source port from 1.20.1. No mod JAR is
interchangeable with neighboring Minecraft versions. Minecraft 1.20.4,
Forge 49.2.8 and Java 17 are the exact build targets, not aliases.

Original multipart cables, data networks, item/fluid/energy transfer,
escrow, wireless inventory storage, readers, monitor GUI editor, forging
and sapphire resources are retained as the intended functionality.
Publication is gated on independent compilation and **191/191 GameTests**.
An unavailable optional mod must not crash PL4's built-in core.

Real client visuals, third-party installed APIs, multiplayer and sustained
performance remain separate acceptance milestones. **Further API testing is
required.**
''',encoding='utf-8')
(R/'docs/releases/0.2a-port.1.md').write_text('''# Foundations PL4 1.20.4 — 0.2a-port.1 alpha candidate

Minecraft 1.20.4 / Forge 49.2.8 / Java17 full-feature-derived port.
Native reobfuscation, production class/package checks, all 191 Minecraft
GameTests, and version-exact JAR/source SHA256 verification are required
before publishing. Optional third-party integrations, real-client visuals,
multiplayer and long-running performance still require acceptance testing.
**Further API testing is required.**
''',encoding='utf-8')
print('Prepared independent Minecraft 1.20.4 / Forge 49.2.8 source and 191-test gate')
