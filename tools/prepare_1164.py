"""One-time target migration from the verified Forge 1.16.5 port; native acceptance is separate."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
build=ROOT/'build.gradle'
text=build.read_text()
if "minecraft 'net.minecraftforge:forge:1.16.4-35.1.37'" in text:
    print('1.16.4 target is already configured; no source rewritten.')
    raise SystemExit(0)
if "minecraft 'net.minecraftforge:forge:1.16.5-36.2.42'" not in text:
    raise SystemExit('Unexpected source target; refusing blind migration')
paths=['build.gradle','src/main/resources/META-INF/mods.toml','src/main/resources/pack.mcmeta',
       'src/main/resources/assets/foundations_pl4/guide/en_us.json','docs/FIELD_GUIDE.md',
       'src/main/java/net/foundations/pl4/compat/RegisterPayloadHandlersEvent.java',
       'src/portTest/java/net/foundations/pl4/compat/scenarios/PortScenarioMod.java','tools/run_port_checks.py']
changes={}
for rel in paths:
    path=ROOT/rel
    value=path.read_text().replace('1.16.5','1.16.4').replace('36.2.42','35.1.37')
    if rel in {'build.gradle','src/main/resources/META-INF/mods.toml'}:
        value=value.replace('[1.16.4,1.17)','[1.16.4]')
    if rel.endswith('mods.toml'):
        value=value.replace('loaderVersion="[36,)"','loaderVersion="[35,)"').replace('[35.1.37,37)','[35.1.37,36)')
    changes[path]=value
status=json.loads((ROOT/'BUILD_STATUS.json').read_text())
status.update(minecraft='1.16.4',loader_version='35.1.37',payload_protocol='forge-1.16.4-5',
              port_status='NATIVE_SCENARIO_VALIDATION_REQUIRED',expected_native_scenarios=191,
              release_gate=['offline production rules','native compilation and reobfuscation','191 isolated native server scenarios','exact target metadata'],
              installed_runtime_acceptance='PENDING',further_api_testing_required=True)
changes[ROOT/'BUILD_STATUS.json']=json.dumps(status,indent=2)+'\n'
for path,value in changes.items():path.write_text(value,encoding='utf-8')
print('Configured Minecraft 1.16.4 / Forge 35.1.37 / Java 17. Native tests have not run yet.')
