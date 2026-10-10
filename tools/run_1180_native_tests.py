"""Execute actual PL4 GameTests through the historical Forge 38 dedicated server.

Forge 38.0.17 predates the Forge 39 GameTestServer launcher. A standard server
supports the /test runall command. This runner sends that command after startup,
captures actual server output, and requires a genuine 193/193 completion marker.
It NEVER substitutes compile counts or portable assertions for native tests.
"""
from pathlib import Path
import json, os, queue, re, subprocess, sys, threading, time

R=Path(__file__).resolve().parents[1]
W=R/'run-1180-native'
OUT=R/'verification-logs'
status=json.loads((R/'BUILD_STATUS.json').read_text())
if status.get('minecraft')!='1.18' or status.get('loader_version')!='38.0.17':
    raise SystemExit('Wrong target, expected exact Minecraft 1.18 / Forge 38.0.17')
if status.get('expected_native_tests')!=193:
    raise SystemExit('Native scenario fixture count changed')
if W.exists():
    raise SystemExit('Refusing a non-disposable test world')
W.mkdir()
OUT.mkdir(exist_ok=True)
(W/'eula.txt').write_text('eula=true\n')
(W/'server.properties').write_text(
    'server-ip=127.0.0.1\nonline-mode=false\nlevel-name=pl4-gametest-world\n'
    'level-type=flat\nview-distance=3\nmax-players=1\nspawn-protection=0\n')
cmd=['bash','gradlew','--no-daemon','--console=plain','runServer']
events=queue.Queue()
lines=[]
started=False;sent=False;complete=False;stopped=False
start=time.monotonic()
deadline=start+540

with subprocess.Popen(cmd,cwd=R,stdin=subprocess.PIPE,stdout=subprocess.PIPE,
                      stderr=subprocess.STDOUT,text=True,bufsize=1) as process:
    def capture():
        for line in process.stdout:events.put(line)
        events.put(None)
    threading.Thread(target=capture,daemon=True).start()
    try:
        with (OUT/'native.log').open('w',encoding='utf-8') as log:
            while time.monotonic()<deadline:
                try:line=events.get(timeout=2)
                except queue.Empty:
                    if process.poll() is not None:break
                    continue
                if line is None:break
                lines.append(line);log.write(line);log.flush()
                print(line.rstrip(),flush=True)
                if 'Unknown or incomplete command' in line or 'test runall<--[HERE]' in line:
                    print('Forge38 native /test runner unavailable: refusing false 193-test certification',flush=True)
                    if process.stdin:
                        process.stdin.write('stop\n')
                        process.stdin.flush()
                    break
                if ('Done (' in line and 'For help' in line) and not started:
                    started=True
                    process.stdin.write('test runall\n')
                    process.stdin.flush()
                    sent=True
                if re.search(r'All 193 required tests passed',line):
                    complete=True
                    process.stdin.write('stop\n')
                    process.stdin.flush()
                if 'Stopping server' in line:stopped=True
                if re.search(r'failed required tests|Game Tests Complete',line,re.I) and not complete:
                    # A completion report without a verified pass marker is not
                    # enough to issue a release; stop and inspect its exact log.
                    process.stdin.write('stop\n')
                    process.stdin.flush()
            if process.poll() is None and complete:process.wait(timeout=50)
    finally:
        if process.poll() is None:
            try:
                process.stdin.write('stop\n')
                process.stdin.flush()
                process.wait(timeout=30)
            except (BrokenPipeError,subprocess.TimeoutExpired):
                process.kill();process.wait()
text=''.join(lines)
summary={
  'minecraft':'1.18','forge':'38.0.17','expected_native_tests':193,
  'server_started':started,'native_runall_command_sent':sent,
  'native_completion_marker_verified':complete,
  'clean_stop':stopped,'process_exit':process.returncode,
  'mode':'Vanilla GameTest command on actual Forge38 userdev dedicated server',
  'client_visuals':'PENDING','installed_optional_apis':'PENDING',
  'multiplayer':'PENDING','further_api_testing_required':True
}
(OUT/'native-1180-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
if not started or not sent or not complete or not stopped or process.returncode:
    raise SystemExit('Forge38 GameTest acceptance is not confirmed: see verification-logs/native.log and native-1180-summary.json')
print('All 193 required tests passed — actual Minecraft 1.18 / Forge38 dedicated server')
