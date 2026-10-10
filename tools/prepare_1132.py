"""Prepare an independently versioned Forge 25.0.223 / Minecraft 1.13.2 port.

This is a research/build scaffold ONLY. A successful 1.14.4 binary is not a
1.13.2 binary. Carry full-feature sources forward and require native acceptance
before any public port JAR is tagged.
"""
from pathlib import Path
import json

R=Path(__file__).resolve().parents[1]
sp=R/'BUILD_STATUS.json'
status=json.loads(sp.read_text())
if status.get('minecraft')=='1.13.2':
    if status.get('loader_version')!='25.0.223':raise SystemExit('Unexpected preconfigured target')
    print('Minecraft 1.13.2 already prepared for independent native investigation')
    raise SystemExit(0)
if status.get('minecraft')!='1.14.4' or status.get('loader_version')!='28.2.26':
    raise SystemExit('Unreviewed source baseline; expected the tested 1.14.4 feature port')

for directory in ['src','tools/java8']:
    for p in (R/directory).rglob('*'):
        if p.is_file() and p.suffix in {'.java','.py','.json','.toml','.mcmeta'}:
            old=p.read_text(encoding='utf-8')
            new=old.replace('1.14.4','1.13.2')
            if p.name=='mods.toml':
                new=new.replace('[28,29)','[25,26)').replace('[28.2.26,29)','[25.0.223,26)').replace('28.2.26','25.0.223')
            if new!=old:p.write_text(new,encoding='utf-8')

gradle=R/'build.gradle'
s=gradle.read_text()
if "minecraft 'net.minecraftforge:forge:1.14.4-28.2.26'" not in s:
    raise SystemExit('Unexpected Forge28 Gradle build baseline')
gradle.write_text(s.replace('1.14.4','1.13.2').replace('28.2.26','25.0.223'),encoding='utf-8')

checks=R/'tools/run_port_checks.py'
s=checks.read_text()
checks.write_text(s.replace('1.14.4','1.13.2').replace('28.2.26','25.0.223'),encoding='utf-8')
p=R/'tools/java8/runtime.py'
s=p.read_text()
# Forge25 runs on Java8; JDK17 is only for source/bytecode tooling.
if "{'1.16.4','1.13.2'}" in s:pass
elif "{'1.16.4','1.14.4'}" in s:s=s.replace("{'1.16.4','1.14.4'}","{'1.16.4','1.13.2'}")
else:raise SystemExit('Unexpected Java8 target guard')
p.write_text(s,encoding='utf-8')

status.update(minecraft='1.13.2',loader='Forge',loader_version='25.0.223',
 version='0.2a-port.1',java=8,build_java=17,payload_protocol='forge-1.13.2-5',
 port_status='NATIVE_SOURCE_PORT_IN_PROGRESS',native_compilation='PENDING',
 native_scenarios='PENDING',installed_runtime_acceptance='PENDING',
 installed_optional_provider_acceptance='PENDING',client_visual_acceptance='PENDING',
 live_multiplayer_acceptance='PENDING',release_published=False,
 full_pl2_parity=False,further_api_testing_required=True,
 expected_native_scenarios=194,native_verification_run=None,release_notes='docs/releases/0.2a-port.1.md')
sp.write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')

(R/'PORT_TARGET.json').write_text(json.dumps(dict(minecraft='1.13.2',loader='Forge',
 loader_version='25.0.223',runtime_java=8,source_base='mc/1.14.4',
 production_feature_baseline='full-feature, not limited legacy',release_allowed=False,
 native_scenarios_expected=194,visuals='PENDING',third_party_apis='PENDING'),indent=2)+'\n')
(R/'README.md').write_text('''# Foundations PL4 / Minecraft 1.13.2 — Forge25 port

**Unreleased native port in progress.** This branch derives from the 1.14.4
full-feature implementation and independently targets Minecraft 1.13.2,
Forge 25.0.223 and Java 8. Forge's official download list identifies
25.0.223 as the latest 1.13.2 build (check source below).

The inherited full-feature code still needs target-specific API adaptation
for forge registries, world/recipe data, GUI rendering, block interaction,
worldgen, networking and capabilities. Existing source presence is NOT proof
that any of those features run on installed Forge25.

Native gates: independent compiler/reobfuscation, Java8 archive verification,
194 exact-target installed-server scenarios, no-fixture production startup
and stop, then real-client, optional API and multiplayer acceptance. A binary
from another Minecraft target must NEVER be renamed and published as 1.13.2.

**Further API testing is required.** No versioned release has been certified.
Official loader: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.13.2.html
''',encoding='utf-8')
p=R/'docs/releases/0.2a-port.1.md'
p.write_text('''# Foundations PL4 1.13.2 / 0.2a-port.1 — candidate only

This branch targets Minecraft 1.13.2 / Forge 25.0.223 / Java 8,
ported from the fuller 1.14.4 implementation. A working runtime has
NOT been verified or published. Native APIs, data files, compiled
artifacts, 194 gameplay scenarios, production-only startup and installed
optional API combinations remain acceptance gates. No borrowed binary,
omitted display code, or guessed power ratios qualify as compatibility.

**Further API testing is required.**
''',encoding='utf-8')
print('Prepared independent Forge25 source configuration; publication remains prohibited')
