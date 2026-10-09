#!/usr/bin/env bash
set -euo pipefail
set -euo pipefail
mkdir -p verification-logs
bash gradlew --no-daemon --console=plain clean build 2>&1 | tee verification-logs/build.log
python3 - <<'PY'
import json,zipfile,struct
from pathlib import Path
jar=Path('build/libs/FoundationsPL4-1.6.4-0.2a-legacy-preview.2-srg.jar')
with zipfile.ZipFile(jar) as z:
    assert json.loads(z.read('mcmod.info'))[0]['version']=='0.2a-legacy-preview.2'
    for n in z.namelist():
        assert '/tests/' not in n and not n.endswith('GameTests.class') and not n.endswith('.jar'),n
        if n.endswith('.class'):
            b=z.read(n);assert struct.unpack('>H',b[6:8])[0]<=51,n
            assert b'net/neoforged/' not in b and b'sonar/core/' not in b and b'mcmultipart/' not in b,n
    for name in ['legacy_cable','legacy_export','legacy_import']:
        assert 'assets/foundations_pl4/textures/blocks/'+name+'.png' in z.namelist()
print('PASS isolated Java 7 runtime packaging and legacy atlas resources')
PY


set -euo pipefail
mkdir -p run-legacy/mods
base='https://maven.minecraftforge.net/net/minecraftforge/forge/1.6.4-9.11.1.1345/forge-1.6.4-9.11.1.1345-installer.jar'
curl --fail --location --retry 3 "$base" -o forge-installer.jar
printf '7a57c0febe74260bfdee1a5d4968982d8863a02c  forge-installer.jar\n' | sha1sum -c -
sha256sum forge-installer.jar > verification-logs/installer-SHA256.txt
bash tools/install_legacy164.sh 2>&1 | tee verification-logs/install.log
printf 'eula=true\n' > run-legacy/eula.txt
printf 'server-ip=127.0.0.1\nonline-mode=false\nlevel-name=pl4-legacy-test-world\nlevel-type=FLAT\nview-distance=3\nmax-players=1\nspawn-protection=0\n' > run-legacy/server.properties
cp "${PL4_VERIFICATION_RUNTIME:-build/libs/FoundationsPL4-1.6.4-0.2a-legacy-preview.2-srg.jar}" run-legacy/mods/FoundationsPL4-1.6.4-0.2a-legacy-preview.2-srg.jar
mapfile -t fixtures < <(find build/libs -maxdepth 1 -type f -name '*native-scenarios*-srg.jar')
test "${#fixtures[@]}" -eq 1
cp "${fixtures[0]}" run-legacy/mods/pl4-native-scenarios.jar
mapfile -t launchers < <(find run-legacy -maxdepth 1 -type f -name '*forge*9.11.1.1345*.jar')
test "${#launchers[@]}" -eq 1
launcher=$(basename "${launchers[0]}")
(cd run-legacy && timeout 300 "$JAVA_HOME_7_X64/bin/java" -Xmx2G -Dfoundations_pl4.legacyScenarios=true -jar "$launcher" nogui) 2>&1 | tee verification-logs/native.log
python3 - <<'PY'
import json
from pathlib import Path
report=json.loads(Path('run-legacy/legacy-scenarios.json').read_text())
assert report['minecraft']=='1.6.4' and report['total']==52 and report['passed']==52 and report['failed']==0,report
report.update(build_api_forge='9.11.1.960',runtime_forge='9.11.1.1345',runtime_java=7)
Path('run-legacy/legacy-scenarios.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS all 52 isolated native 1.6.4 transport scenarios on Forge 1345')
PY


python3 tools/check_legacy_fluid_verification.py 1.6.4
