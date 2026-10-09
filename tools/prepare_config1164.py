"""Register the two extra native Forge-35 config regressions without dropping original scenarios."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
p=R/'src/portTest/java/net/foundations/pl4/PLGameTests.java';s=p.read_text()
if 'e.register(VersionConfigGameTests.class)' not in s:
    old='e.register(EnergyIntegrationGameTests.class);'
    assert s.count(old)==1
    p.write_text(s.replace(old,old+'e.register(VersionConfigGameTests.class);'))
for name in ['src/portTest/java/net/foundations/pl4/compat/scenarios/PortScenarioMod.java','tools/run_port_checks.py','README.md','docs/releases/0.2a-port.1.md']:
    p=R/name;p.write_text(p.read_text().replace('191','193').replace('193 original fixtures','193 required fixtures').replace('all 193 existing server scenarios','all 191 original scenarios plus two config regressions'))
p=R/'BUILD_STATUS.json';s=json.loads(p.read_text());s['expected_native_scenarios']=193
s['release_gate']=[v.replace('191','193') for v in s['release_gate']]
p.write_text(json.dumps(s,indent=2)+'\n')
print('Registered all 191 original scenarios plus two native config regressions; execution still required.')
