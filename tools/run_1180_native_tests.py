"""Execute actual PL4 GameTests through the historical Forge 38 dedicated server.

Forge 38.0.17 predates the Forge 39 GameTestServer launcher. A standard server
supports the /test runall command. This runner sends that command after startup,
captures actual server output, and requires a genuine 193/193 completion marker.
It NEVER substitutes compile counts or portable assertions for native tests.
"""
from pathlib import Path
import json, os, queue, re, subprocess, sys, threading, time, socket, struct, secrets



def remote_command(command, password, port=25575):
    """Authenticated local-only Minecraft RCON using its length-prefixed protocol."""
    def packet(identity, kind, payload):
        data=struct.pack('<ii', identity, kind)+payload.encode('utf-8')+b'\x00\x00'
        return struct.pack('<i',len(data))+data
    def receive(sock):
        size_data=b''
        while len(size_data)<4:
            chunk=sock.recv(4-len(size_data))
            if not chunk:raise ConnectionError('RCON closed before size')
            size_data+=chunk
        length=struct.unpack('<i',size_data)[0]
        if length<10 or length>1_048_576:raise ValueError('Invalid RCON response length')
        data=b''
        while len(data)<length:
            chunk=sock.recv(length-len(data))
            if not chunk:raise ConnectionError('RCON closed before payload')
            data+=chunk
        ident,kind=struct.unpack('<ii',data[:8])
        return ident, data[8:-2].decode('utf-8',errors='replace')
    with socket.create_connection(('127.0.0.1',port),timeout=5) as connection:
        connection.settimeout(5)
        connection.sendall(packet(37,3,password))
        auth_id,_=receive(connection)
        if auth_id not in (37,-1):raise RuntimeError('Unexpected RCON authorization response')
        # Some protocol versions send an empty SERVERDATA_RESPONSE_VALUE first.
        if auth_id==-1:raise PermissionError('Minecraft RCON rejected credentials')
        connection.sendall(packet(38,2,command))
        for _ in range(3):
            ident,response=receive(connection)
            if ident==38:return response
        raise RuntimeError('No authenticated RCON command response')


def send_rcon(command,password):
    for attempt in range(20):
        try:return remote_command(command,password)
        except (OSError,ConnectionError):
            if attempt==19:raise
            time.sleep(0.5)

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
password=secrets.token_urlsafe(28)
(W/'server.properties').write_text(
    'server-ip=127.0.0.1\nonline-mode=false\nlevel-name=pl4-gametest-world\n'
    'level-type=flat\nview-distance=3\nmax-players=1\nspawn-protection=0\n'
    'enable-rcon=true\nrcon.port=25575\nrcon.password='+password+'\n')
cmd=['bash','gradlew','--no-daemon','--console=plain','runServer']
events=queue.Queue()
lines=[]
started=False;sent=False;complete=False;stopped=False
start=time.monotonic()
deadline=start+240
command_at=None
last_server_line=None

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
                if '[Server thread/' in line:last_server_line=time.monotonic()
                print(line.rstrip(),flush=True)
                if 'Unknown or incomplete command' in line or 'test runall<--[HERE]' in line:
                    print('Forge38 native /test runner unavailable: refusing false 193-test certification',flush=True)
                    if process.stdin:
                        process.stdin.write('stop\n')
                        process.stdin.flush()
                    break
                if ('Done (' in line and 'For help' in line) and not started:
                    started=True
                    try:
                        response=send_rcon('test runall',password)
                        print('Native RCON test command response: '+response[:500],flush=True)
                        sent=True;command_at=time.monotonic()
                        if re.search(r'unknown|incomplete|not found',response,re.I):
                            print('Forge38 server lacks a compatible GameTest command; refusing acceptance',flush=True)
                            break
                    except (OSError,ValueError,RuntimeError,PermissionError) as e:
                        print('Could not issue authenticated native test command: '+str(e),flush=True)
                        break
                if re.search(r'All 193 required tests passed',line):
                    complete=True
                    process.stdin.write('stop\n')
                    process.stdin.flush()
                if 'Stopping server' in line:stopped=True
                if sent and not complete and command_at is not None and time.monotonic()-command_at>90:
                    print('Native Forge38 server did not return 193-test completion within 90 seconds; refusing release',flush=True)
                    process.stdin.write('stop\n');process.stdin.flush();break
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
