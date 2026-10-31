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



def require_disposable_workspaces(baseline, candidate, output):
    """Refuse to mutate active/dirty checkouts or overwrite retained results.

    This benchmark intentionally replaces a GameTest registration and runs a
    generated world. It must only operate on fresh, separate Git checkouts.
    """
    roots = [Path(baseline).resolve(), Path(candidate).resolve()]
    output = Path(output).resolve()
    control = Path(__file__).resolve().parents[2]
    if roots[0] == roots[1] or output.exists():
        raise ValueError("Benchmark needs two distinct checkouts and a new output directory")
    for root in roots:
        if (root == control or root == output or root in output.parents or
                output in root.parents or not (root / ".git").exists()):
            raise ValueError("Refusing to benchmark an active/non-disposable checkout")
        if (root / "run").exists() or (root / "run").is_symlink():
            raise ValueError("Refusing to remove a pre-existing Minecraft run/world: " + str(root))
        if subprocess.check_output(
                ["git", "status", "--porcelain", "--untracked-files=all"],
                cwd=root, text=True).strip():
            raise ValueError("Refusing to modify a dirty benchmark checkout: " + str(root))
    return roots, output


def replace_owned_run(root, previous_trial, source):
    """Only delete a runtime marked by this exact benchmark's prior trial."""
    runtime = root / "run"
    if previous_trial == 0:
        if runtime.exists() or runtime.is_symlink():
            raise ValueError("Unowned Minecraft run directory: " + str(runtime))
        return
    marker = runtime / ".pl4-benchmark-owned"
    if (runtime.is_symlink() or not runtime.is_dir() or
            marker.is_symlink() or not marker.is_file() or
            marker.read_text() != f"{source}:{previous_trial}\n"):
        raise ValueError("Refusing to delete an unowned or changed Minecraft run: " + str(runtime))
    shutil.rmtree(runtime)


def mark_owned_run(root, trial, source):
    runtime = root / "run"
    if runtime.is_symlink() or not runtime.is_dir():
        raise ValueError("Native benchmark did not create a regular run directory")
    with (runtime / ".pl4-benchmark-owned").open("x") as marker:
        marker.write(f"{source}:{trial}\n")


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('baseline',type=Path);parser.add_argument('candidate',type=Path);parser.add_argument('output',type=Path)
    args=parser.parse_args();roots,out=require_disposable_workspaces(args.baseline,args.candidate,args.output);out.mkdir(parents=True,exist_ok=False)
    fixture=Path(__file__).with_name('ReleasePerformanceGameTests.java').read_text()
    records=[];sources={}
    for label,root in [('baseline',roots[0]),('candidate',roots[1])]:
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
        order=[('baseline',roots[0]),('candidate',roots[1])]
        if trial%2==0:order.reverse()
        for label,root in order:
            root=root.resolve();replace_owned_run(root,trial-1,sources[label])
            env=dict(os.environ,PL4_BENCHMARK_SOURCE=sources[label],PL4_BENCHMARK_TRIAL=str(trial))
            log=out/f'{trial}-{label}.log';print(f'RUN trial {trial} {label} {sources[label]}',flush=True)
            with log.open('x') as output:
                run=subprocess.run(['bash','gradlew','--no-daemon','--console=plain','runGameTestServer'],cwd=root,env=env,stdout=output,stderr=subprocess.STDOUT,timeout=1800)
            if run.returncode:raise RuntimeError(f'{log}: native probe failed: '+log.read_text(errors='replace')[-14000:])
            mark_owned_run(root,trial,sources[label])
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
    with (out/'comparison.json').open('x') as stream: stream.write(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2),flush=True)


if __name__=='__main__':main()
