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


def inventory_digest(runtime):
    """Fingerprint every generated runtime path and file before permitting deletion.

    A matching ownership marker alone is insufficient: a player or another tool
    may have added a save, changed a file, or linked in another directory after
    the last benchmark trial. Refuse those cases instead of deleting them.
    """
    digest = hashlib.sha256()
    def fail_scan(error):
        raise ValueError("Cannot inspect complete benchmark runtime: " + str(error)) from error
    for directory, dirs, files in os.walk(runtime, followlinks=False, onerror=fail_scan):
        dirs.sort()
        files.sort()
        base = Path(directory)
        for name in sorted(dirs + files):
            entry = base / name
            if entry == runtime / ".pl4-benchmark-owned":
                continue
            if entry.is_symlink():
                raise ValueError("Refusing linked content inside benchmark run: " + str(entry))
            relative = entry.relative_to(runtime).as_posix().encode("utf-8", "surrogateescape")
            if entry.is_dir():
                digest.update(b"D\0" + relative + b"\0")
            elif entry.is_file():
                file_digest = hashlib.sha256()
                length = 0
                with entry.open("rb") as stream:
                    while True:
                        chunk = stream.read(1024 * 1024)
                        if not chunk:
                            break
                        length += len(chunk)
                        file_digest.update(chunk)
                digest.update(b"F\0" + relative + b"\0" +
                              str(length).encode("ascii") + b"\0" + file_digest.digest())
            else:
                raise ValueError("Refusing special file inside benchmark run: " + str(entry))
    return digest.hexdigest()


def replace_owned_run(root, previous_trial, source):
    """Delete only an unchanged runtime produced by this benchmark's prior trial."""
    runtime = root / "run"
    if previous_trial == 0:
        if runtime.exists() or runtime.is_symlink():
            raise ValueError("Unowned Minecraft run directory: " + str(runtime))
        return
    marker = runtime / ".pl4-benchmark-owned"
    if (runtime.is_symlink() or not runtime.is_dir() or
            marker.is_symlink() or not marker.is_file()):
        raise ValueError("Refusing to delete an unowned or changed Minecraft run: " + str(runtime))
    try:
        previous = json.loads(marker.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        raise ValueError("Refusing invalid benchmark ownership marker: " + str(marker)) from exc
    if (not isinstance(previous, dict) or
            previous.get("source") != source or previous.get("trial") != previous_trial or
            not isinstance(previous.get("inventory_sha256"), str)):
        raise ValueError("Refusing to delete an unowned or changed Minecraft run: " + str(runtime))
    if inventory_digest(runtime) != previous["inventory_sha256"]:
        raise ValueError("Refusing to delete a benchmark run changed after its trial: " + str(runtime))
    shutil.rmtree(runtime)


def mark_owned_run(root, trial, source):
    runtime = root / "run"
    if runtime.is_symlink() or not runtime.is_dir():
        raise ValueError("Native benchmark did not create a regular run directory")
    inventory = inventory_digest(runtime)
    with (runtime / ".pl4-benchmark-owned").open("x", encoding="utf-8") as marker:
        json.dump({"source": source, "trial": trial, "inventory_sha256": inventory},
                  marker, sort_keys=True)
        marker.write("\n")



def validate_native_report(report, source, trial, reference_schema=None):
    """Reject malformed or incomparable native timing receipts before summarizing."""
    if not isinstance(report, dict) or report.get('source_commit') != source or report.get('trial') != str(trial):
        raise ValueError('Native probe source/trial mismatch')
    workloads = report.get('workloads')
    if not isinstance(workloads, list) or not workloads:
        raise ValueError('Native probe has no workloads')
    schema = {}
    seen_networks = set()
    for workload in workloads:
        if not isinstance(workload, dict):
            raise ValueError('Malformed native workload')
        network = workload.get('network')
        if not isinstance(network, str) or not network or network in seen_networks:
            raise ValueError('Missing or duplicate native network')
        seen_networks.add(network)
        for count_key in ('host_count', 'endpoint_count'):
            value = workload.get(count_key)
            if type(value) is not int or value <= 0:
                raise ValueError('Invalid native workload ' + count_key)
        measurements = workload.get('measurements')
        if not isinstance(measurements, list) or not measurements:
            raise ValueError('Native workload has no measurements')
        for measurement in measurements:
            if not isinstance(measurement, dict):
                raise ValueError('Malformed native measurement')
            operation = measurement.get('operation')
            key = (network, operation)
            if not isinstance(operation, str) or not operation or key in schema:
                raise ValueError('Missing or duplicate native operation')
            operations = measurement.get('operations_per_round')
            warmup = measurement.get('warmup_operations')
            if type(operations) is not int or operations <= 0 or type(warmup) is not int or warmup < 0:
                raise ValueError('Invalid native operation counts')
            rounds = None
            for metric in ('elapsed_ns', 'cpu_ns', 'allocated_bytes'):
                values = measurement.get(metric)
                if not isinstance(values, list) or len(values) < 3 or (rounds is not None and len(values) != rounds):
                    raise ValueError('Missing or mismatched native metric rounds: ' + metric)
                if any(type(value) is not int or value < (1 if metric == 'elapsed_ns' else 0) for value in values):
                    raise ValueError('Invalid native metric values: ' + metric)
                rounds = len(values)
            schema[key] = (workload['host_count'], workload['endpoint_count'], operations, warmup, rounds)
    if reference_schema is not None and schema != reference_schema:
        raise ValueError('Native benchmark workloads/operations differ across revisions or trials')
    return schema

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('baseline',type=Path);parser.add_argument('candidate',type=Path);parser.add_argument('output',type=Path)
    args=parser.parse_args();roots,out=require_disposable_workspaces(args.baseline,args.candidate,args.output);out.mkdir(parents=True,exist_ok=False)
    fixture=Path(__file__).with_name('ReleasePerformanceGameTests.java').read_text()
    records=[];sources={};reference_schema=None
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
            schema=validate_native_report(report,sources[label],trial,reference_schema)
            if reference_schema is None:reference_schema=schema
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
