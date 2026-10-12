"""Run isolated GameTests on installed Forge 45 and compare production bytes.

Passing native tests is not release clearance until the production-only boot
is separately verified.
"""
from pathlib import Path
import hashlib, json, os, re, secrets, shutil, socket, struct, subprocess, sys, time, urllib.request, zipfile

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / 'run-installed-1194'
OUT = ROOT / 'verification-logs'
MC, FORGE = '1.19.4', '45.4.5'


def rcon(command, password):
    def packet(identity, kind, value):
        data = struct.pack('<ii', identity, kind) + value.encode() + b'\0\0'
        return struct.pack('<i', len(data)) + data

    def receive(connection):
        size = connection.recv(4)
        if len(size) != 4:
            raise ConnectionError('RCON response header missing')
        length = struct.unpack('<i', size)[0]
        data = b''
        while len(data) < length:
            chunk = connection.recv(length - len(data))
            if not chunk:
                raise ConnectionError('RCON response ended early')
            data += chunk
        identity, kind = struct.unpack('<ii', data[:8])
        return identity, data[8:-2].decode(errors='replace')

    for attempt in range(20):
        try:
            connection = socket.create_connection(('127.0.0.1', 25578), timeout=5)
            break
        except OSError:
            if attempt == 19:
                raise
            time.sleep(0.5)
    # The native command can synchronously prepare 100 structures. A timeout
    # after sending must fail, never submit the command a second time.
    with connection:
        connection.settimeout(300)
        connection.sendall(packet(71, 3, password))
        identity, _ = receive(connection)
        if identity != 71:
            raise PermissionError('RCON rejected authentication')
        connection.sendall(packet(72, 2, command))
        for _ in range(3):
            identity, response = receive(connection)
            if identity == 72:
                return response
        raise ConnectionError('No matching command response')


def main():
    status = json.loads((ROOT / 'BUILD_STATUS.json').read_text())
    if status['minecraft'] != MC or status['loader_version'] != FORGE:
        raise ValueError('Unexpected target metadata')
    base = 'FoundationsPL4-1.19.4-' + status['version']
    production = ROOT / 'build/libs' / (base + '.jar')
    artifact = ROOT / 'build/libs' / (base + '-native-scenarios.jar')
    reuse = '--reuse-installed' in sys.argv[1:]
    if not artifact.is_file() or not production.is_file() or (WORK.exists() and not reuse):
        raise ValueError('Missing production/test artifact or non-disposable work directory')
    compared = 0
    with zipfile.ZipFile(production) as runtime, zipfile.ZipFile(artifact) as fixture:
        for entry in runtime.namelist():
            if entry.endswith('/') or entry in {'META-INF/mods.toml', 'META-INF/MANIFEST.MF'}:
                continue
            if 'GameTests' in entry or 'PortAssertions' in entry or 'PLPortTestMod' in entry:
                raise ValueError('Test class leaked into production JAR')
            if runtime.read(entry) != fixture.read(entry):
                raise ValueError('Production/test artifact content differs: ' + entry)
            compared += 1
    if compared < 100:
        raise ValueError('Missing production classes/resources')
    if reuse:
        if not (WORK / 'libraries/net/minecraftforge/forge' / f'{MC}-{FORGE}' / 'unix_args.txt').is_file():
            raise ValueError('Cannot reuse incomplete Forge installation')
        shutil.rmtree(WORK / 'pl4-gametest-world', ignore_errors=True)
        shutil.rmtree(WORK / 'mods')
    else:
        WORK.mkdir()
    (WORK / 'mods').mkdir()
    OUT.mkdir(exist_ok=True)
    shutil.copyfile(artifact, WORK / 'mods' / artifact.name)
    password = secrets.token_urlsafe(28)
    (WORK / 'eula.txt').write_text('eula=true\n')
    (WORK / 'server.properties').write_text(
        'server-ip=127.0.0.1\nonline-mode=false\nlevel-name=pl4-gametest-world\n'
        'level-type=flat\nview-distance=3\nmax-players=1\nspawn-protection=0\n'
        'enable-rcon=true\nrcon.port=25578\nrcon.password=' + password + '\n')
    # Supply vanilla class-prefixed SNBT fallback fixtures alongside the
    # namespaced empty structure packaged in the isolated test artifact.
    structures = WORK / 'gameteststructures'
    structures.mkdir(exist_ok=True)
    empty = '{DataVersion:2975,size:[3,10,10],palette:[{Name:"minecraft:air"}],blocks:[],entities:[]}'
    fixtures = list((ROOT / 'src/portTest/java/net/foundations/pl4').glob('*GameTests.java'))
    if len(fixtures) != 12:
        raise ValueError('Expected 12 GameTest fixture classes')
    for fixture in fixtures:
        (structures / (fixture.stem.lower() + '.empty.snbt')).write_text(empty + '\n')
    java = Path(os.environ['JAVA_HOME']) / 'bin/java'
    url = f'https://maven.minecraftforge.net/net/minecraftforge/forge/{MC}-{FORGE}/forge-{MC}-{FORGE}-installer.jar'
    if not reuse:
        with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': 'FoundationsPL4-native-tests'}), timeout=120) as source:
            with (WORK / 'forge-installer.jar').open('wb') as target:
                shutil.copyfileobj(source, target)
        with (OUT / 'installed-1194-installer.log').open('w') as log:
            subprocess.run([str(java), '-jar', 'forge-installer.jar', '--installServer'], cwd=WORK,
                           stdout=log, stderr=subprocess.STDOUT, check=True, timeout=450)
    args = WORK / 'libraries/net/minecraftforge/forge' / f'{MC}-{FORGE}' / 'unix_args.txt'
    if not args.is_file():
        raise ValueError('Installed Forge server arguments missing')
    log_path = OUT / 'installed-1194-native.log'
    command = [str(java), '-Xms512M', '-Xmx3G', '-Dfoundations_pl4.portTestServer=true', '-Dforge.enableGameTest=true',
               '-Dfoundations_pl4.isolatedGameTestConfig=true', '@' + str(args), 'nogui']
    with log_path.open('w') as log:
        process = subprocess.Popen(command, cwd=WORK, stdin=subprocess.PIPE, stdout=log,
                                   stderr=subprocess.STDOUT, text=True)
        try:
            started = False
            response = None
            deadline = time.monotonic() + 900
            while process.poll() is None and time.monotonic() < deadline:
                text = log_path.read_text(errors='replace')
                if not started and re.search(r'Done \([\d.,]+s\)!', text):
                    started = True
                    response = rcon('pl4native', password)
                    print('Native RCON response: ' + response[:600], flush=True)
                    if re.search(r'unknown|incomplete|not found|unexpected error', response, re.I):
                        raise ValueError('Installed server rejected native test command: ' + response[:600])
                if 'PL4 NATIVE SUCCESS: All 191 required tests passed' in text:
                    break
                if started and 'PL4 NATIVE FAILURE:' in text:
                    raise ValueError('Native test runner ended without exact pass marker')
                time.sleep(1)
            else:
                raise ValueError('Installed server did not complete 191 tests')
            process.stdin.write('stop\n')
            process.stdin.flush()
            process.wait(timeout=90)
        finally:
            if process.poll() is None:
                process.kill()
                process.wait()
    text = log_path.read_text(errors='replace')
    if process.returncode or 'Stopping server' not in text:
        raise ValueError('Server did not shut down cleanly')
    summary = {'minecraft': MC, 'forge': FORGE, 'version': status['version'],
               'native_total': 191, 'native_passed': 191, 'native_failed': 0,
               'production_runtime_sha256': hashlib.sha256(production.read_bytes()).hexdigest(),
               'scenario_artifact_sha256': hashlib.sha256(artifact.read_bytes()).hexdigest(),
               'production_entries_compared': compared,
               'mode': 'Installed Forge 45 with isolated combined GameTest JAR',
               'production_release_clearance': False,
               'reason': 'Production-only boot/shutdown is still pending'}
    (OUT / 'installed-1194-summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    print('PASS 191 native GameTests on installed Forge 45; production packaging remains pending')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, KeyError, subprocess.SubprocessError) as error:
        sys.exit('INSTALLED 1.19.4 NATIVE VERIFICATION FAILED: ' + str(error))
