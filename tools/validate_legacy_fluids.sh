#!/usr/bin/env bash
set -euo pipefail
set -euo pipefail
mkdir -p verification-logs
bash gradlew --no-daemon --console=plain clean build reobfLegacyTestJar 2>&1 | tee verification-logs/build.log


set -euo pipefail
mkdir -p run-legacy/mods
base='https://maven.minecraftforge.net/net/minecraftforge/forge/1.12.2-14.23.5.2864/forge-1.12.2-14.23.5.2864-installer.jar'
curl --fail --location --retry 3 "$base" -o forge-installer.jar
curl --fail --location --retry 3 "$base.sha1" -o installer.sha1
printf '%s  forge-installer.jar\n' "$(cat installer.sha1 | tr -d '\r\n ')" | sha1sum -c -
sha256sum forge-installer.jar > verification-logs/installer-SHA256.txt
java -jar forge-installer.jar --installServer run-legacy 2>&1 | tee verification-logs/install.log
printf 'eula=true\n' > run-legacy/eula.txt
printf 'server-ip=127.0.0.1\nonline-mode=false\nlevel-name=pl4-legacy-test-world\nlevel-type=FLAT\nview-distance=3\nmax-players=1\nspawn-protection=0\n' > run-legacy/server.properties
cp "${PL4_VERIFICATION_RUNTIME:-build/libs/FoundationsPL4-1.12.2-0.2a-legacy-preview.3.jar}" run-legacy/mods/FoundationsPL4-1.12.2-0.2a-legacy-preview.3.jar
cp build/libs/FoundationsPL4-1.12.2-0.2a-legacy-preview.3-native-scenarios.jar run-legacy/mods/
mapfile -t launchers < <(find run-legacy -maxdepth 1 -type f -name 'forge-1.12.2-14.23.5.2864*.jar')
test "${#launchers[@]}" -eq 1
launcher=$(basename "${launchers[0]}")
(cd run-legacy && timeout 300 java -Xmx2G -Dfoundations_pl4.legacyScenarios=true -jar "$launcher" nogui) 2>&1 | tee verification-logs/native.log
python3 - <<'PY'
import json,zipfile,struct
from pathlib import Path
report=json.loads(Path('run-legacy/legacy-scenarios.json').read_text())
assert report['minecraft']=='1.12.2' and report['total']==89 and report['passed']==89 and report['failed']==0,report
jar=Path('build/libs/FoundationsPL4-1.12.2-0.2a-legacy-preview.3.jar')
with zipfile.ZipFile(jar) as z:
    assert json.loads(z.read('mcmod.info'))[0]['version']=='0.2a-legacy-preview.3'
    for n in z.namelist():
        assert '/tests/' not in n and not n.endswith('GameTests.class') and not n.endswith('.jar'),n
        if n.endswith('.class'):
            b=z.read(n);assert struct.unpack('>H',b[6:8])[0]<=52,n
            assert b'net/neoforged/' not in b and b'sonar/core/' not in b and b'mcmultipart/' not in b,n
print('PASS native legacy subset on installed Forge, Java 8 artifact and isolated test packaging')
PY


python3 tools/check_legacy_fluid_verification.py 1.12.2

python3 tools/check_legacy_energy_packaging.py 1.12.2
