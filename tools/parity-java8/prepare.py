"""Install the reviewed Java-8 artifact pipeline on an exact 1.16.x source branch."""
from pathlib import Path
import json,re,shutil,sys
root=Path(sys.argv[1]).resolve();target=sys.argv[2]
if target not in {'1.16.4','1.16.5'}:raise SystemExit('Unsupported target')
statuspath=root/'BUILD_STATUS.json';status=json.loads(statuspath.read_text())
if status['minecraft']!=target:raise SystemExit('Wrong branch content')
new='0.2a-port.1' if target=='1.16.4' else '0.2a-port.2'
old=status['version']
if old not in {'0.2a-port.1',new}:raise SystemExit('Unexpected version; reconcile branch before applying')
for name in ['runtime.gradle','runtime.py']:
 dest=root/'tools/java8'/name;dest.parent.mkdir(parents=True,exist_ok=True)
 shutil.copyfile(Path(__file__).parent/'java8'/name,dest)
build=root/'build.gradle';text=build.read_text()
text=text.replace("version = '"+old+"'","version = '"+new+"'",1)
text=text.replace("apply from: 'gradle/forge35-java17.gradle'\n",'')
if "apply from: 'tools/java8/runtime.gradle'" not in text:text+="\napply from: 'tools/java8/runtime.gradle'\n"
build.write_text(text)
status.update(version=new,java=8,build_java=17,port_status='JAVA8_NATIVE_ACCEPTANCE_PENDING',
 installed_runtime_acceptance='PENDING',release_notes='docs/releases/'+new+'.md',
 release_gate=['production rule suites','native compilation','Java 8 conversion and reobfuscation','isolated installed Forge scenarios','exact target archive and license checks'])
statuspath.write_text(json.dumps(status,indent=2)+'\n')
guard=root/'tools/run_port_checks.py';text=guard.read_text().replace("status['java']==17","status['java']==8 and status['build_java']==17")
guard.write_text(text)
for p in [root/'docs/FIELD_GUIDE.md',root/'src/main/resources/assets/foundations_pl4/guide/en_us.json']:
 p.write_text(p.read_text().replace('Foundations PL4 '+old,'Foundations PL4 '+new))
notes='''# Foundations PL4 TARGET VERSION - native Java 8 compatibility

Alpha feature port. **Further API testing is still required.**

Retains the modern-source cable, multipart, reader/display, editor, item network storage,
item/fluid/energy routing, ownership, persistence and field-guide implementation already
adapted for this Minecraft target. This update converts records, sealed classes and newer
JDK API usages at build time before Forge reobfuscation instead of requiring a patched
Forge scanner or runtime Java agent.

Runtime: Minecraft TARGET / Forge FORGE / Java 8. Build: Java 17.
The published runtime is built from the `-java8.jar` artifact, NOT the unconverted intermediate
JAR. Only required relocated JvmDowngrader 2.0.1 API helpers are included, with notices,
LGPL-2.1 license, matching source asset, and reproducible relinking scripts.

Publication requires the complete existing native scenario suite on an installed server
using the packaged runtime. Exact results and artifact hashes are in java8-summary.json;
this note alone is not evidence that tests passed. Real-client visuals, third-party API
mod combinations and live multiplayer acceptance remain pending. This does not add missing
external provider APIs or claim full PL2 parity. Use matching client/server PL4 builds and
back up worlds. Do not install source, input, unconverted or scenario JARs.

Reproduction in a clean disposable checkout with JAVA_HOME_17_X64 and JAVA_HOME_8_X64:
`JAVA_HOME="$JAVA_HOME_17_X64" bash gradlew --no-daemon clean build`
`python3 tools/java8/runtime.py native`

The source remains Java 17 for maintainability; the tested release output targets Java 8.
'''.replace('TARGET',target).replace('VERSION',new).replace('FORGE',status['loader_version'])
p=root/'docs/releases'/(new+'.md');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(notes)
(root/'README.md').write_text(notes+'\nSee docs/FIELD_GUIDE.md for usage, and docs/API_TESTING.md for remaining integration acceptance.\n')
ignore=root/'.gitignore';text=ignore.read_text()
for line in ['.java8-toolchain/','run-java8/']:
 if line not in text.splitlines():text+='\n'+line+'\n'
ignore.write_text(text)
print('Prepared exact '+target+' '+new+' Java 8 artifact path. Native acceptance is still pending.')
