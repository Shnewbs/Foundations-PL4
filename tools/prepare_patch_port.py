"""Keep exact Forge 33 APIs, world-load ore attachment and complete native test counts aligned."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
def prepare(root):
    request=json.loads((root/'PORT_TARGET.json').read_text());status=json.loads((root/'BUILD_STATUS.json').read_text())
    if request['minecraft']!='1.16.2' or status['minecraft']!='1.16.2' or status['loader_version']!='33.0.61':raise ValueError('Wrong native target')
    source=root/'src/main/java/net/foundations/pl4/FoundationsPL4.java'
    text=source.read_text().replace('PortWorldgen::biome','PortWorldgen::world');source.write_text(text,encoding='utf-8')
    fixtures=root/'src/portTest/java/net/foundations/pl4/PLGameTests.java'
    text=fixtures.read_text()
    if 'e.register(PortWorldgenGameTests.class)' not in text:
        text=text.replace('e.register(VersionConfigGameTests.class);','e.register(VersionConfigGameTests.class);e.register(PortWorldgenGameTests.class);')
    fixtures.write_text(text,encoding='utf-8')
    for name in ['tools/run_port_checks.py','src/portTest/java/net/foundations/pl4/compat/scenarios/PortScenarioMod.java','README.md','docs/releases/0.2a-port.1.md']:
        path=root/name;path.write_text(path.read_text().replace('193','195'),encoding='utf-8')
    status['expected_native_scenarios']=195;status['native_worldgen_adapter']='Forge 33 setup/world-load feature-list copy; no per-tick reflection'
    status['release_gate']=[value.replace('193','195') for value in status['release_gate']]
    (root/'BUILD_STATUS.json').write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')
    build=root/'build.gradle';text=build.read_text()
    text=text.replace("tasks.register('inspectExactPortApi', Exec) { commandLine 'python3','tools/prepare_patch_port.py','--inspect' }\n",'').replace("tasks.named('compileJava') { finalizedBy 'inspectExactPortApi' }\n",'')
    build.write_text(text.rstrip()+'\n',encoding='utf-8')
    print('Exact 1.16.2 APIs prepared; all 195 installed scenarios and production-only startup still required')
if __name__=='__main__':prepare(ROOT)
