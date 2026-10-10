"""Prepare independent Minecraft 1.20.0 / Forge46.0.14 source and metadata."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1];p=R/'BUILD_STATUS.json'
status=json.loads(p.read_text())
if status.get('minecraft')=='1.20':
    assert status.get('loader_version')=='46.0.14'
    print('Exact Minecraft 1.20 target already prepared');raise SystemExit(0)
if status.get('minecraft')!='1.20.1' or status.get('loader_version')!='47.4.26':
    raise SystemExit('Refusing unexpected full-feature source baseline')
build=R/'build.gradle';s=build.read_text()
assert '1.20.1-47.4.26' in s and 'versionRange="[1.20.1,1.20.2)"' in s
build.write_text(s.replace('1.20.1','1.20').replace('47.4.26','46.0.14')
   .replace('versionRange="[1.20,1.20.2)"','versionRange="[1.20,1.20.1)"'),encoding='utf-8')
p=R/'src/main/resources/META-INF/mods.toml';s=p.read_text()
assert '1.20.1' in s and '47.4.26' in s
s=s.replace('1.20.1','1.20').replace('47.4.26','46.0.14')
s=s.replace('loaderVersion="[47,)"','loaderVersion="[46,47)"')
s=s.replace('versionRange="[46.0.14,48)"','versionRange="[46.0.14,47)"')
s=s.replace('versionRange="[1.20,1.20.2)"','versionRange="[1.20,1.20.1)"')
p.write_text(s,encoding='utf-8')
p=R/'src/main/resources/pack.mcmeta';data=json.loads(p.read_text())
assert data['pack']['pack_format']==15
data['pack']['description']=data['pack']['description'].replace('1.20.1','1.20')
p.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
p=R/'tools/run_port_checks.py';s=p.read_text()
assert "status['minecraft']=='1.20.1'" in s
p.write_text(s.replace("status['minecraft']=='1.20.1'","status['minecraft']=='1.20'"),encoding='utf-8')
for name in ('README.md','docs/FIELD_GUIDE.md'):
    p=R/name
    if p.exists():p.write_text(p.read_text().replace('1.20.1','1.20').replace('47.4.26','46.0.14'),encoding='utf-8')
p=R/'src/main/resources/assets/foundations_pl4/guide/en_us.json'
if p.exists():p.write_text(p.read_text().replace('1.20.1','1.20'),encoding='utf-8')
status.update(minecraft='1.20',loader='Forge',loader_version='46.0.14',
              payload_protocol='forge-1.20-5',port_status='NATIVE_API_PORT_PENDING',
              native_compilation='PENDING',expected_native_tests=191,
              release_published=False,installed_runtime_acceptance='PENDING',
              installed_optional_provider_acceptance='PENDING',
              client_visual_acceptance='PENDING',live_multiplayer_acceptance='PENDING',
              full_pl2_parity=False,further_api_testing_required=True)
(R/'BUILD_STATUS.json').write_text(json.dumps(status,indent=2)+'\n')
(R/'PORT_TARGET.json').write_text(json.dumps(dict(minecraft='1.20',forge='46.0.14',
              loader='Forge',java=17,source_baseline='mc/1.20.1',
              required_native_tests=191,release_allowed=False),indent=2)+'\n')
(R/'README.md').write_text('''# Foundations PL4 Minecraft 1.20 (Forge46)

Independently sourced full-feature Forge46.0.14 / Minecraft 1.20 / Java17
port derived from the verified 1.20.1 version. No binary is considered
compatible by renaming another Minecraft version's JAR.

Multipart cables and displays, GUIs/editor, readers, owner-separated network
storage, item/fluid/energy transfer and saved escrow remain the production
feature baseline. Passing native GameTests, pack/metadata validation and
reobfuscated Java17 runtime verification are required before GitHub/CurseForge
publication.

Installed third-party mod APIs, client graphics, multiplayer and sustained
performance acceptance remain pending. **Further API testing is required.**
''',encoding='utf-8')
(R/'docs/releases/0.2a-port.1.md').write_text('''# Foundations PL4 1.20 — Forge46 0.2a-port.1 alpha candidate

Independent Minecraft 1.20 / Forge 46.0.14 / Java17 port.
Modern-derived multipart networks, reader/displays/editor, item/fluid/energy
transport, wireless inventory storage, safe escrow and forging remain
the functional baseline. Native compilation and all 191 original GameTests
must pass before publishing this exact-version JAR, source and checksums.
**Further installed-mod, client-rendering and multiplayer testing required.**
''',encoding='utf-8')
print('Prepared exact MC1.20 / Forge46.0.14 source and 191 native test gates')
