"""Boot the standalone production JAR without the GameTest artifact on installed Forge 44."""
from pathlib import Path
import hashlib, json, os, re, shutil, subprocess, sys, time

ROOT = Path(__file__).resolve().parents[1]
INSTALL = ROOT / 'run-installed-1193'
WORK = ROOT / 'run-installed-1193-production'
LOGS = ROOT / 'verification-logs'


def main():
    evidence = LOGS / 'installed-1193-summary.json'
    if not evidence.is_file():
        raise ValueError('Native installed GameTests have not passed')
    summary = json.loads(evidence.read_text())
    if (summary['native_total'], summary['native_passed'], summary['native_failed']) != (191, 191, 0):
        raise ValueError('Native installed GameTests are incomplete')
    name = 'FoundationsPL4-1.19.3-' + summary['version'] + '.jar'
    runtime = ROOT / 'build/libs' / name
    digest = hashlib.sha256(runtime.read_bytes()).hexdigest()
    if digest != summary['production_runtime_sha256']:
        raise ValueError('Production JAR has changed since native fixture comparison')
    args = INSTALL / 'libraries/net/minecraftforge/forge/1.19.3-44.1.23/unix_args.txt'
    if not args.is_file():
        raise ValueError('Installed Forge 44 arguments missing')
    if WORK.exists():
        raise ValueError('Use a fresh disposable production-only directory')
    WORK.mkdir()
    (WORK / 'libraries').symlink_to(INSTALL / 'libraries', target_is_directory=True)
    (WORK / 'mods').mkdir()
    installed = WORK / 'mods' / name
    shutil.copyfile(runtime, installed)
    if list((WORK / 'mods').iterdir()) != [installed]:
        raise ValueError('Production server contains an extra mod')
    (WORK / 'eula.txt').write_text('eula=true\n')
    (WORK / 'server.properties').write_text('server-ip=127.0.0.1\nonline-mode=false\nlevel-name=pl4-production-world\nview-distance=3\nmax-players=1\n')
    java = Path(os.environ['JAVA_HOME']) / 'bin/java'
    smoke = LOGS / 'installed-1193-production-boot.log'
    with smoke.open('w') as output:
        process = subprocess.Popen([str(java), '-Xms512M', '-Xmx3G', '@' + str(args), 'nogui'],
                                   cwd=WORK, stdin=subprocess.PIPE, stdout=output,
                                   stderr=subprocess.STDOUT, text=True)
        try:
            deadline = time.monotonic() + 240
            while process.poll() is None and time.monotonic() < deadline:
                if re.search(r'Done \([\d.,]+s\)!', smoke.read_text(errors='replace')):
                    break
                time.sleep(1)
            else:
                raise ValueError('Production-only server did not reach ready')
            process.stdin.write('stop\n')
            process.stdin.flush()
            process.wait(timeout=90)
        finally:
            if process.poll() is None:
                process.kill()
                process.wait()
    log = smoke.read_text(errors='replace')
    if process.returncode or 'Stopping server' not in log or 'PL4 NATIVE START' in log:
        raise ValueError('Production-only boot/shutdown failed')
    if hashlib.sha256(installed.read_bytes()).hexdigest() != digest:
        raise ValueError('Installed production JAR bytes changed')
    summary['production_only_boot_and_shutdown'] = 'PASS'
    summary['production_release_clearance'] = True
    summary.pop('reason', None)
    evidence.write_text(json.dumps(summary, indent=2) + '\n')
    print('PASS standalone production JAR boot and shutdown on installed Forge 44')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, KeyError, subprocess.SubprocessError) as error:
        sys.exit('PRODUCTION 1.19.3 VERIFICATION FAILED: ' + str(error))
