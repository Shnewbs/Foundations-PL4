"""Run two real graphical clients against a private disposable development server."""
from pathlib import Path
import argparse
import json
import os
import shutil
import signal
import subprocess
import tarfile
import io
import time


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('source',type=Path);parser.add_argument('output',type=Path);args=parser.parse_args()
    source=args.source.resolve();out=args.output.resolve();out.mkdir(parents=True,exist_ok=True);fixture=Path(__file__).resolve().parent
    commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=source,text=True).strip();archive=subprocess.check_output(['git','archive','HEAD'],cwd=source)
    env=dict(os.environ,PL4_PROBE_SOURCE=commit,PL4_PROBE_OUTPUT=str(out));processes=[];logs=[]
    roots={}
    for name in ['server','PL4ProbeA','PL4ProbeB']:
        root=source.parent/('multiplayer-'+name);root.mkdir()
        with tarfile.open(fileobj=io.BytesIO(archive)) as tar:tar.extractall(root,filter='data')
        java=root/'src/main/java/net/foundations/pl4'
        shutil.copy2(fixture/'MultiplayerServerProbe.java',java/'MultiplayerServerProbe.java');shutil.copy2(fixture/'MultiplayerClientProbe.java',java/'client/MultiplayerClientProbe.java')
        (root/'gradle.properties').write_text('org.gradle.jvmargs=-Xmx768m\norg.gradle.daemon=false\norg.gradle.parallel=false\norg.gradle.workers.max=1\norg.gradle.caching=true\n')
        run=root/'run';run.mkdir(exist_ok=True)
        (run/'options.txt').write_text('onboardAccessibility:false\npauseOnLostFocus:false\nmaxFps:30\nguiScale:2\n')
        if name=='server':
            (run/'eula.txt').write_text('eula=true\n');(run/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25576\nonline-mode=false\nmax-players=2\nspawn-protection=0\nlevel-type=minecraft:flat\nview-distance=3\nsimulation-distance=3\nenforce-secure-profile=false\n')
            extra="neoForge.runs.server { jvmArguments.addAll('-Xms256m','-Xmx768m') }\n"
        else:
            extra="neoForge.runs.client { programArguments.addAll('--username','"+name+"','--quickPlayMultiplayer','127.0.0.1:25576','--width','960','--height','720'); jvmArguments.addAll('-Xms256m','-Xmx896m') }\n"
        with (root/'build.gradle').open('a') as f:f.write('\n'+extra)
        roots[name]=root
    def launch(name,task):
        log=(out/(name+'.log')).open('w');logs.append(log)
        process=subprocess.Popen(['bash','gradlew','--no-daemon','--console=plain',task],cwd=roots[name],env=env,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True,start_new_session=True);processes.append(process);return process
    try:
        server=launch('server','runServer');deadline=time.monotonic()+900
        while True:
            text=(out/'server.log').read_text(errors='replace')
            if 'Done (' in text and 'For help, type' in text:break
            if server.poll() is not None:raise RuntimeError('Server exited before ready: '+text[-10000:])
            if time.monotonic()>deadline:raise TimeoutError('Native server startup timed out')
            time.sleep(2)
        clients=[launch(name,'runClient') for name in ['PL4ProbeA','PL4ProbeB']];deadline=time.monotonic()+900
        while any(p.poll() is None for p in clients):
            for name,p in zip(['PL4ProbeA','PL4ProbeB'],clients):
                if p.poll() not in {None,0}:raise RuntimeError(name+' failed: '+(out/(name+'.log')).read_text(errors='replace')[-12000:])
            if time.monotonic()>deadline:raise TimeoutError('Real client probe timed out')
            time.sleep(2)
        for name in ['PL4ProbeA','PL4ProbeB']:
            proof=json.loads((out/(name+'-client-proof.json')).read_text());assert proof['items_received']==17 and proof['identical_packets_sent']==2 and proof['source_commit']==commit
            assert (out/(name+'-storage.png')).stat().st_size>1000
        proof=json.loads((out/'server-multiplayer-proof.json').read_text());assert proof['source_commit']==commit and proof['remaining_items']==94 and proof['items_per_player']==[17,17] and len(set(proof['real_client_uuids']))==2
        print('PASS: two actual client processes, distinct UUIDs, foreign-owner rejection and network replay conservation.',flush=True)
    finally:
        for p in processes:
            if p.poll() is None:
                try:p.stdin.write('stop\n');p.stdin.flush();p.wait(timeout=25)
                except (OSError,subprocess.TimeoutExpired):os.killpg(p.pid,signal.SIGTERM)
        for log in logs:log.close()


if __name__=='__main__':main()
