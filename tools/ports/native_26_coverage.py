"""Distinguish PL4 fixture coverage from the 26.x server's total test count."""
import hashlib
import re


def coverage(text, expected):
    batches = []
    seen = set()
    for environment, batch, count in re.findall(
            r"Running test environment '([^']+)' batch (\d+) \((\d+) tests\)", text):
        identity = (environment, int(batch))
        if identity in seen:
            raise ValueError('Duplicate native environment batch: ' + environment)
        seen.add(identity)
        batches.append(dict(environment=environment, batch=int(batch), tests=int(count)))
    pl4 = sum(row['tests'] for row in batches if row['environment'].startswith('foundations_pl4:'))
    total = sum(row['tests'] for row in batches)
    passed = re.findall(r'All (\d+) required tests passed', text)
    if pl4 != expected or not expected:
        raise ValueError(f'PL4 native coverage mismatch: expected {expected}, observed {pl4}')
    if len(passed) != 1 or int(passed[0]) != total:
        raise ValueError('Required-test success count does not match attributed environment batches')
    return dict(pl4_fixtures=pl4, other_fixtures=total-pl4, total_required_passed=total,
                environment_batches=batches, native_log_sha256=hashlib.sha256(text.encode()).hexdigest())
