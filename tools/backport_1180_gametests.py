"""Register all 193 native GameTests through Minecraft 1.18/Forge38's registry.

Forge38 predates RegisterGameTestsEvent, PrefixGameTestTemplate and the
templateNamespace annotation attribute. Keep every scenario executable,
use the actual namespace on the structure template, and preserve test count.
"""
from pathlib import Path
import json
import re

ROOT=Path(__file__).resolve().parents[1]
status_path=ROOT/'BUILD_STATUS.json'
status=json.loads(status_path.read_text())
if status.get('minecraft')!='1.18' or status.get('loader_version')!='38.0.17':
    raise SystemExit('Refusing non-Forge38 port')
if (status.get('native_test_registration')=='forge38-193-v1'
    and 'GameTestRegistry.register(' in (ROOT/'src/main/java/net/foundations/pl4/PLGameTests.java').read_text()
    and 'PLGameTests.register();' in (ROOT/'src/main/java/net/foundations/pl4/FoundationsPL4.java').read_text()):
    print('Forge38 GameTest adapter is committed in the actual Java sources')
    raise SystemExit(0)
# An earlier job committed only status metadata; ensure the production Java
# source is migrated and committed before compiler/native verification.

source=ROOT/'src/main/java/net/foundations/pl4'
sources=list(source.glob('*GameTests.java'))
if len(sources)<10:raise SystemExit('Expected all original 193 scenario test classes')
total=0
for p in sources:
    s=p.read_text(encoding='utf-8')
    total+=s.count('@GameTest(')
    s=s.replace('import net.minecraftforge.gametest.PrefixGameTestTemplate;\n','')
    s=s.replace('@PrefixGameTestTemplate(false)\n','')
    if 'templateNamespace=FoundationsPL4.ID' in s:
        s=s.replace('template="empty",templateNamespace=FoundationsPL4.ID',
                    'template="foundations_pl4:empty"')
    if 'templateNamespace=' in s or '@PrefixGameTestTemplate' in s:
        raise SystemExit('Unadapted native test annotation in '+str(p))
    if p.name=='PLGameTests.java':
        s=s.replace('import net.minecraftforge.event.RegisterGameTestsEvent;\n','')
        before='public static void register(RegisterGameTestsEvent e)'
        after='public static void register()'
        if before not in s:raise SystemExit('Unexpected GameTest registration API')
        s=s.replace(before,after)
        s=s.replace('e.register(', 'GameTestRegistry.register(')
    p.write_text(s,encoding='utf-8')
if total!=193:raise SystemExit('Unexpected test count: '+str(total))

mod=source/'FoundationsPL4.java'
s=mod.read_text(encoding='utf-8')
before='bus.addListener(PLGameTests::register);'
if s.count(before)!=1:raise SystemExit('Missing original Forge39 test-registration hook')
s=s.replace(before, 'PLGameTests.register();')
mod.write_text(s,encoding='utf-8')

checks=ROOT/'tools/run_port_checks.py'
s=checks.read_text(encoding='utf-8')
before=r"registered=re.findall(r'e\.register\((\w+)\.class\)',(source/'PLGameTests.java').read_text())"
after=r"registered=re.findall(r'GameTestRegistry\.register\((\w+)\.class\)',(source/'PLGameTests.java').read_text())"
if s.count(before)!=1:raise SystemExit('Native test counting rule changed unexpectedly')
s=s.replace(before,after)
checks.write_text(s,encoding='utf-8')

status['native_test_registration']='forge38-193-v1'
status['expected_native_tests']=193
status['port_status']='FORGE38_GAME_TEST_API_BACKPORT'
status['native_compilation']='PENDING'
status['further_api_testing_required']=True
status_path.write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')
print('Retained 193 exact native test methods and adapted Forge38 GameTestRegistry registration')
