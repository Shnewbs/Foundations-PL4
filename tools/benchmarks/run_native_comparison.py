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
    # Two different paths can still be nested Git checkouts. Rewriting a
    # child checkout would mutate the parent's tree and invalidate both
    # supposedly independent baselines, even if both appear clean.
    if roots[0] in roots[1].parents or roots[1] in roots[0].parents:
        raise ValueError("Benchmark requires distinct, non-nested checkouts")
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

NATIVE_ENVIRONMENT_FIELDS = ("java", "java_vendor", "vm_name", "gc_collectors",
                             "os", "os_arch", "processors",
                             "heap_initial_bytes", "heap_max_bytes")


def validate_native_environment(report, reference_report=None, *, require_fixed_heap=False):
    """Require comparable native JVM, GC policy, heap and CPU across all trials.

    Native operation ratios are not meaningful if the JVM vendor, collector,
    heap limits or available processors change between revisions or trials.
    """
    if not isinstance(report, dict):
        raise ValueError("Missing native benchmark environment")
    environment = {field: report.get(field) for field in NATIVE_ENVIRONMENT_FIELDS}
    for field in ("java", "java_vendor", "vm_name", "os", "os_arch"):
        value = environment[field]
        if not isinstance(value, str) or not value.strip():
            raise ValueError("Invalid native benchmark environment: " + field)
    collectors = environment["gc_collectors"]
    if (not isinstance(collectors, list) or not collectors or
            any(not isinstance(gc, str) or not gc.strip() for gc in collectors) or
            collectors != sorted(set(collectors))):
        raise ValueError("Invalid native benchmark environment: gc_collectors")
    for field in ("processors", "heap_initial_bytes", "heap_max_bytes"):
        value = environment[field]
        if type(value) is not int or value <= 0:
            raise ValueError("Invalid native benchmark environment: " + field)
    if environment["heap_initial_bytes"] > environment["heap_max_bytes"]:
        raise ValueError("Native benchmark initial heap exceeds maximum heap")
    if require_fixed_heap and environment["heap_initial_bytes"] != environment["heap_max_bytes"]:
        raise ValueError("Native benchmark requires a fixed heap: pin both GameTest JVM -Xms and -Xmx equally")
    if reference_report is not None:
        reference = validate_native_environment(reference_report, require_fixed_heap=require_fixed_heap)
        if environment != reference:
            changed = [field for field in NATIVE_ENVIRONMENT_FIELDS
                       if environment[field] != reference[field]]
            raise ValueError("Native benchmark environment differs across trials: " +
                             ", ".join(changed))
    return environment


def finish_native_trial(root, report_path, output_path, source, trial, label,
                        fixture_sha256, reference_schema=None, reference_environment=None):
    """Validate and persist the native report before sealing the disposable run.

    The GameTest may write its report under run/verification-logs. Removing that
    report *after* mark_owned_run would invalidate the SHA-256 inventory and
    prevent the next interleaved trial from safely replacing its own world.
    """
    # Accept only the two locations produced by the isolated GameTest runner.
    # A recursive report search may otherwise find an unrelated world or a
    # linked verification-logs directory and unlink someone else's report.
    root = Path(root).resolve()
    report_path = Path(report_path)
    allowed = {
        root / "verification-logs" / "performance-probe.json",
        root / "run" / "verification-logs" / "performance-probe.json",
    }
    if (report_path.is_symlink() or report_path.parent.is_symlink()
            or (root / "run").is_symlink()):
        raise ValueError("Linked native benchmark report or directory")
    if report_path.resolve() not in allowed:
        raise ValueError("Native benchmark report is outside the expected probe directories")
    if not report_path.is_file():
        raise ValueError("Missing native benchmark report")
    report = json.loads(report_path.read_text())
    report.update(label=label, fixture_sha256=fixture_sha256)
    schema = validate_native_report(report, source, trial, reference_schema)
    # Equal environments alone are insufficient: matching auto-sized heaps
    # can vary with host RAM and invalidate a cross-version comparison.
    validate_native_environment(report, reference_environment, require_fixed_heap=True)
    with output_path.open("x") as stream:
        stream.write(json.dumps(report, indent=2) + "\n")
    report_path.unlink()
    # No benchmark-generated file is intentionally removed after this marker.
    mark_owned_run(root, trial, source)
    return report, schema




def locate_native_probe_report(root):
    """Select only the two runner-owned report paths, never a recursive glob.

    Old or unrelated world directories can contain identically named reports.
    They must not make a valid benchmark fail or become eligible for deletion.
    """
    root = Path(root).resolve()
    candidates = (
        root / "verification-logs" / "performance-probe.json",
        root / "run" / "verification-logs" / "performance-probe.json",
    )
    found = []
    for path in candidates:
        if (path.is_symlink() or path.parent.is_symlink()
                or (root / "run").is_symlink()):
            raise ValueError("Linked native benchmark report or directory")
        if path.exists():
            if not path.is_file():
                raise ValueError("Native benchmark report is not a regular file")
            found.append(path)
    if len(found) != 1:
        raise ValueError(f"Expected one native probe report, found {len(found)}")
    return found[0]


def require_unlinked_checkout_path(root, path):
    """Reject symlinks in any component of a checkout-relative source path.

    A clean Git checkout can contain a tracked symlink. Path.read_text and
    Path.write_text follow it, potentially overwriting a file outside the
    disposable benchmark checkout when the registration is rewritten.
    """
    current = root
    for part in path.relative_to(root).parts:
        current = current / part
        if current.is_symlink():
            raise ValueError("Linked native benchmark checkout path: " + str(current))


def preflight_native_checkouts(roots):
    """Validate *both* exact revisions before modifying either checkout.

    A bad second checkout must not leave the first checkout's GameTest
    registration rewritten, or create a misleading empty results directory.
    Benchmarking identical source commits also wastes six native runs.
    """
    plans = []
    registration = (r'public static void register\(RegisterGameTestsEvent e\)'
                    r'\{[^}]*\}')
    replacement = ('public static void register(RegisterGameTestsEvent e)'
                   '{e.register(ReleasePerformanceGameTests.class);}')
    for label, root in zip(('baseline', 'candidate'), roots):
        source = subprocess.check_output(
            ['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
        if not re.fullmatch(r'[0-9a-f]{40}(?:[0-9a-f]{24})?', source):
            raise ValueError('Benchmark source is not an exact Git commit')
        status_path = root / 'BUILD_STATUS.json'
        require_unlinked_checkout_path(root, status_path)
        status = json.loads(status_path.read_text(encoding='utf-8'))
        if status.get('minecraft') != '1.21.1':
            raise ValueError('Exact Minecraft 1.21.1 benchmark target required')
        # This directory may be gitignored and therefore escape the clean
        # checkout check. A stale report could otherwise be mistaken for a
        # fresh GameTest result, or an existing report could be unlinked.
        report_dir = root / 'verification-logs'
        if report_dir.exists() or report_dir.is_symlink():
            raise ValueError('Pre-existing native benchmark report directory: ' + str(report_dir))
        candidates = [
            root / 'src/portTest/java/net/foundations/pl4/PLGameTests.java',
            root / 'src/main/java/net/foundations/pl4/PLGameTests.java',
        ]
        registries = [path for path in candidates if path.is_file() or path.is_symlink()]
        if not registries:
            raise ValueError('Missing isolated GameTest registration source')
        if len(registries) != 1:
            raise ValueError('Ambiguous isolated GameTest registration locations')
        registry = registries[0]
        require_unlinked_checkout_path(root, registry)
        # The runner injects this class via write_text. Never overwrite an
        # existing (possibly gitignored) source or follow a fixture symlink.
        fixture_target = registry.with_name('ReleasePerformanceGameTests.java')
        if fixture_target.exists() or fixture_target.is_symlink():
            raise ValueError('Existing performance fixture would be overwritten: ' + str(fixture_target))
        original = registry.read_text(encoding='utf-8')
        if len(re.findall(registration, original)) != 1:
            raise ValueError('Unsupported or ambiguous isolated registration source')
        rewritten = re.sub(registration, lambda _: replacement, original, count=1)
        plans.append({'label': label, 'root': root, 'source': source,
                      'registry': registry, 'registration': rewritten})
    if plans[0]['source'] == plans[1]['source']:
        raise ValueError('Native comparison requires distinct source commits')
    return plans

def native_trial_environment(source, trial, inherited=None):
    """Pin a reproducible GameTest heap without overriding user JVM heap flags.

    JAVA_TOOL_OPTIONS reaches the forked game JVM, unlike Gradle-only heap
    settings. The native report still verifies measured initial/max heap.
    """
    env = dict(os.environ if inherited is None else inherited)
    raw = env.pop('PL4_BENCHMARK_HEAP_MB', '2048')
    if not isinstance(raw, str) or not re.fullmatch(r'[0-9]+', raw):
        raise ValueError('PL4_BENCHMARK_HEAP_MB must be an integer number of MiB')
    heap_mb = int(raw)
    if not 512 <= heap_mb <= 16384:
        raise ValueError('PL4_BENCHMARK_HEAP_MB must be between 512 and 16384 MiB')
    for key in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS'):
        options = env.get(key, '')
        if re.search(r'(?<!\S)(?:-Xm[sx]\S*|-XX:(?:InitialHeapSize|MaxHeapSize|InitialRAMPercentage|MaxRAMPercentage)=\S+)', options):
            raise ValueError('Conflicting inherited JVM heap option in ' + key)
    env['JAVA_TOOL_OPTIONS'] = (env.get('JAVA_TOOL_OPTIONS', '').strip() +
                                f' -Xms{heap_mb}m -Xmx{heap_mb}m').strip()
    env['PL4_BENCHMARK_SOURCE'] = source
    env['PL4_BENCHMARK_TRIAL'] = str(trial)
    return env



def preflight_native_trial_environments(plans, inherited=None):
    """Validate and freeze all six GameTest JVM environments before any writes.

    Invalid heap controls or inherited Java options must not leave disposable
    checkouts modified or create an unusable results directory.
    """
    snapshot = dict(os.environ if inherited is None else inherited)
    environments = {}
    for trial in range(1, 4):
        for plan in plans:
            environments[(plan['label'], trial)] = native_trial_environment(
                plan['source'], trial, snapshot)
    return environments

def validate_native_trial_set(records):
    """Require one receipt for each interleaved revision/trial pair.

    Counting three records per label is not enough: duplicated or mislabeled
    trials could hide an absent run and make a comparison appear reproducible.
    """
    if not isinstance(records, list) or not records:
        raise ValueError('Native benchmark has no trial records')
    expected = {(label, str(trial))
                for label in ('baseline', 'candidate') for trial in range(1, 4)}
    seen = set()
    for record in records:
        if not isinstance(record, dict):
            raise ValueError('Malformed native benchmark trial receipt')
        key = (record.get('label'), record.get('trial'))
        if key not in expected:
            raise ValueError('Unexpected native benchmark trial receipt: ' + repr(key))
        if key in seen:
            raise ValueError('Duplicate native benchmark trial receipt: ' + repr(key))
        seen.add(key)
    if seen != expected:
        raise ValueError('Expected exactly three native benchmark trials per revision')
    return True


def summarize_native_measurements(records):
    """Show CPU, allocation and trial spread; never infer RC clearance."""
    validate_native_trial_set(records)
    summaries = []
    for workload in records[0]['workloads']:
        for measurement in workload['measurements']:
            name = measurement['operation']
            row = {'network': workload['network'], 'operation': name}
            for label in ('baseline', 'candidate'):
                selected = [m for record in records if record['label'] == label
                            for w in record['workloads'] if w['network'] == workload['network']
                            for m in w['measurements'] if m['operation'] == name]
                if len(selected) != 3:
                    raise ValueError('Expected three native benchmark trials per revision: ' + label)
                for metric in ('elapsed_ns', 'cpu_ns', 'allocated_bytes'):
                    per_trial = [statistics.median(m[metric]) / m['operations_per_round']
                                 for m in selected]
                    median = statistics.median(per_trial)
                    row[label + '_' + metric + '_per_operation'] = median
                    row[label + '_' + metric + '_trial_medians'] = per_trial
                    # Surface trial-to-trial noise without inventing a pass
                    # threshold or mistaking one lucky run for a speedup.
                    spread = max(per_trial) - min(per_trial)
                    row[label + '_' + metric + '_trial_range'] = spread
                    row[label + '_' + metric + '_trial_relative_range'] = (
                        spread / median if median else None)
            for metric in ('elapsed_ns', 'cpu_ns', 'allocated_bytes'):
                baseline = row['baseline_' + metric + '_per_operation']
                candidate = row['candidate_' + metric + '_per_operation']
                row[metric + '_ratio'] = candidate / baseline if baseline else None
                row[metric + '_delta_per_operation'] = candidate - baseline
            row['elapsed_ratio'] = row['elapsed_ns_ratio']  # Preserve existing consumer contract.
            summaries.append(row)
    return summaries


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('baseline',type=Path);parser.add_argument('candidate',type=Path);parser.add_argument('output',type=Path)
    args=parser.parse_args();roots,out=require_disposable_workspaces(args.baseline,args.candidate,args.output)
    plans=preflight_native_checkouts(roots)
    environments=preflight_native_trial_environments(plans)
    fixture=Path(__file__).with_name('ReleasePerformanceGameTests.java').read_text()
    # Only after both checkouts and the fixture pass preflight may we mutate.
    out.mkdir(parents=True,exist_ok=False)
    records=[];sources={plan['label']:plan['source'] for plan in plans};reference_schema=None
    for plan in plans:
        root=plan['root'];registry=plan['registry']
        registry.write_text(plan['registration'],encoding='utf-8')
        registry.with_name('ReleasePerformanceGameTests.java').write_text(fixture,encoding='utf-8')
        # The probe is development-only; all publication is absent from this workflow.
        (root/'verification-logs').mkdir(exist_ok=True)
    for trial in range(1,4):
        order=[('baseline',roots[0]),('candidate',roots[1])]
        if trial%2==0:order.reverse()
        for label,root in order:
            root=root.resolve();replace_owned_run(root,trial-1,sources[label])
            env=environments[(label, trial)]
            log=out/f'{trial}-{label}.log';print(f'RUN trial {trial} {label} {sources[label]}',flush=True)
            with log.open('x') as output:
                run=subprocess.run(['bash','gradlew','--no-daemon','--console=plain','runGameTestServer'],cwd=root,env=env,stdout=output,stderr=subprocess.STDOUT,timeout=1800)
            if run.returncode:raise RuntimeError(f'{log}: native probe failed: '+log.read_text(errors='replace')[-14000:])
            report_path=locate_native_probe_report(root)
            target=out/f'{trial}-{label}.json'
            report,schema=finish_native_trial(root,report_path,target,sources[label],trial,label,
                                              hashlib.sha256(fixture.encode()).hexdigest(),reference_schema,
                                              reference_environment=records[0] if records else None)
            if reference_schema is None:reference_schema=schema
            records.append(report)
    summaries=summarize_native_measurements(records)
    result={'target':'1.21.1','sources':sources,'trials_per_revision':3,'fixture_sha256':hashlib.sha256(fixture.encode()).hexdigest(),
            'benchmark_environment':validate_native_environment(records[0]),'scope':'Loaded wired/wireless operation timing and allocation; separate from full-server MSPT, client FPS, active providers and other ports. Shared CI hardware remains noisy.','measurements':summaries}
    with (out/'comparison.json').open('x') as stream: stream.write(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2),flush=True)


if __name__=='__main__':main()
