"""Run an identical isolated native probe on two frozen 1.21.1 revisions, interleaved."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import statistics
import subprocess


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('baseline',type=Path);parser.add_argument('candidate',type=Path);parser.add_argument('output',type=Path)
    args=parser.parse_args();out=args.output.resolve();out.mkdir(parents=True,exist_ok=True)
    fixture=Path(__file__).with_name('ReleasePerformanceGameTests.java').read_text()
    records=[];sources={}
    for label,root in [('baseline',args.baseline.resolve()),('candidate',args.candidate.resolve())]:
        source=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip();sources[label]=source
        if json.loads((root/'BUILD_STATUS.json').read_text())['minecraft']!='1.21.1':raise ValueError('Exact Minecraft target required')
        candidates=[root/'src/portTest/java/net/foundations/pl4/PLGameTests.java',root/'src/main/java/net/foundations/pl4/PLGameTests.java']
        registry=next(p for p in candidates if p.exists());text=registry.read_text()
        text,count=re.subn(r'public static void register\(RegisterGameTestsEvent e\)\{[^}]*\}', 'public static void register(RegisterGameTestsEvent e){e.register(ReleasePerformanceGameTests.class);}',text,count=1)
        if count!=1:raise ValueError('Unsupported isolated registration source')
        registry.write_text(text);registry.with_name('ReleasePerformanceGameTests.java').write_text(fixture)
        # The probe is development-only; all publication is absent from this workflow.
        (root/'verification-logs').mkdir(exist_ok=True)
    for trial in range(1,4):
        order=[('baseline',args.baseline),('candidate',args.candidate)]
        if trial%2==0:order.reverse()
        for label,root in order:
            root=root.resolve();shutil.rmtree(root/'run',ignore_errors=True)
            env=dict(os.environ,PL4_BENCHMARK_SOURCE=sources[label],PL4_BENCHMARK_TRIAL=str(trial))
            log=out/f'{trial}-{label}.log';print(f'RUN trial {trial} {label} {sources[label]}',flush=True)
            with log.open('w') as output:
                run=subprocess.run(['bash','gradlew','--no-daemon','--console=plain','runGameTestServer'],cwd=root,env=env,stdout=output,stderr=subprocess.STDOUT,timeout=1800)
            if run.returncode:raise RuntimeError(f'{log}: native probe failed: '+log.read_text(errors='replace')[-14000:])
            paths=list(root.glob('**/verification-logs/performance-probe.json'))
            if len(paths)!=1:raise ValueError(f'Expected one native probe report, found {paths}')
            report=json.loads(paths[0].read_text());report.update(label=label,fixture_sha256=hashlib.sha256(fixture.encode()).hexdigest())
            if report['source_commit']!=sources[label] or report['trial']!=str(trial):raise ValueError('Stale probe report')
            target=out/f'{trial}-{label}.json';target.write_text(json.dumps(report,indent=2)+'\n');records.append(report);paths[0].unlink()
    summaries=[]
    for workload in records[0]['workloads']:
        for measurement in workload['measurements']:
            name=measurement['operation'];row={'network':workload['network'],'operation':name}
            for label in ['baseline','candidate']:
                selected=[m for record in records if record['label']==label for w in record['workloads'] if w['network']==workload['network'] for m in w['measurements'] if m['operation']==name]
                for metric in ['elapsed_ns','cpu_ns','allocated_bytes']:
                    per_trial=[statistics.median(m[metric])/m['operations_per_round'] for m in selected]
                    row[label+'_'+metric+'_per_operation']=statistics.median(per_trial)
                    row[label+'_'+metric+'_trial_medians']=per_trial
            baseline=row['baseline_elapsed_ns_per_operation'];row['elapsed_ratio']=row['candidate_elapsed_ns_per_operation']/baseline if baseline else None
            summaries.append(row)
    result={'target':'1.21.1','sources':sources,'trials_per_revision':3,'fixture_sha256':hashlib.sha256(fixture.encode()).hexdigest(),'scope':'Loaded wired/wireless operation timing and allocation; separate from full-server MSPT, client FPS, active providers and other ports. Shared CI hardware remains noisy.','measurements':summaries}
    (out/'comparison.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2),flush=True)


if __name__=='__main__':main()
