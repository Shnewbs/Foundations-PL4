"""Finish test-world cleanup without altering the published legacy runtime sources."""
from pathlib import Path
import json
import shutil
import sys

root=Path(sys.argv[1]);target=sys.argv[2]
if target not in {'1.12.2','1.6.4'}:raise SystemExit('Unsupported target')
fixture=root/'src/legacyTest/java/net/foundations/pl4/legacy/tests/NativeScenarios.java'
text=fixture.read_text(encoding='utf-8')
marker='native fixture cleanup before world save'
if marker not in text:
    # test() resets the disposable area before its body, removing the unregistered provider fixture.
    added='            test("'+marker+'",new Runnable(){public void run(){check(destination()!=null,"registered vanilla destination after reset");System.out.println("PL4 LEGACY FIXTURE CLEANUP: PASS");}});\n'
    anchor='        }finally'
    if text.count(anchor)!=1:raise SystemExit('Unexpected native scenario structure')
    text=text.replace(anchor,added+anchor,1)
if text.count('test("')!=52:raise SystemExit('Expected 52 native scenarios')
fixture.write_text(text,encoding='utf-8')
script=root/'tools/validate_legacy_fluids.sh'
text=script.read_text(encoding='utf-8').replace('==51','==52').replace('all 51 isolated','all 52 isolated')
if 'PL4_VERIFICATION_RUNTIME' not in text:
    name='FoundationsPL4-'+target+'-0.2a-legacy-preview.2'
    if target=='1.6.4':
        old='cp build/libs/'+name+'-srg.jar run-legacy/mods/'
        new='cp "${PL4_VERIFICATION_RUNTIME:-build/libs/'+name+'-srg.jar}" run-legacy/mods/'+name+'-srg.jar'
    else:
        old='cp build/libs/'+name+'.jar build/libs/'+name+'-native-scenarios.jar run-legacy/mods/'
        new='cp "${PL4_VERIFICATION_RUNTIME:-build/libs/'+name+'.jar}" run-legacy/mods/'+name+'.jar\ncp build/libs/'+name+'-native-scenarios.jar run-legacy/mods/'
    if text.count(old)!=1:raise SystemExit('Unexpected runtime installation command')
    text=text.replace(old,new)
if 'check_legacy_fluid_verification.py' not in text:
    text+='\npython3 tools/check_legacy_fluid_verification.py '+target+'\n'
script.write_text(text,encoding='utf-8')
shutil.copyfile(Path(__file__).with_name('check_legacy_fluid_verification.py'),root/'tools/check_legacy_fluid_verification.py')
status=root/'LEGACY_FLUID_STATUS.json'
data=json.loads(status.read_text());data['native_scenarios_expected']=52
status.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
readme=root/'README.md';text=readme.read_text(encoding='utf-8')
text=text.replace('executes 21 isolated scenarios','executes 52 isolated scenarios')
text=text.replace('51 isolated native scenarios','52 isolated native scenarios')
text=text.replace('legacy schema 1','legacy schema 2 (with the earlier item fields retained)')
text=text.replace('Legacy block IDs default to 3500/3501/3502 and','Legacy block IDs default to 3500/3501/3502/3503/3504/3505 and')
text=text.replace('Native scenario run: `bash gradlew clean build runServer -PlegacyScenarios` only in the workflow-created disposable test world.','Native scenarios: run `bash tools/validate_legacy_fluids.sh` in a disposable workspace with Java 8; this installs and exercises official Forge rather than the incompatible development runServer path.')
heading='## Verification-only follow-up'
if heading not in text:
    text+='\n'+heading+'\n\nThe first preview-2 runtime passed 51 scenarios, but the separate test mod left an unregistered external tank in the disposable world at shutdown. A final cleanup scenario now removes that fixture before saving, and the log guard rejects missing-mapping errors. The suite now requires 52 scenarios. This follow-up changes tests, tools and documentation only; published runtime tags and original release assets remain unchanged. Supplemental verification records the actual installed runtime hash and compares rebuilt archive entries with the published JAR.\n'
readme.write_text(text,encoding='utf-8')
print('Prepared 52-scenario verification and documentation; production source unchanged.')
