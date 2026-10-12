"""Reject a green server run that silently omitted native PL4 fixtures."""
from pathlib import Path
import json,re,sys
root=Path(__file__).resolve().parents[1]
expected=sum(p.read_text().count('@PortGameTest(') for p in (root/'src/main/java/net/foundations/pl4').glob('*GameTests.java'))
status=json.loads((root/'BUILD_STATUS.json').read_text())
if status['expected_native_tests']!=expected:raise SystemExit(f"Release metadata mismatch: BUILD_STATUS.json declares {status['expected_native_tests']} but source registers {expected}")
log=Path(sys.argv[1]).read_text()
counts=re.findall(r"Running test environment 'foundations_pl4:[^']+' batch \d+ \((\d+) tests\)",log)
actual=sum(map(int,counts))
if actual!=expected:raise SystemExit(f'Native fixture coverage mismatch: expected {expected}, ran {actual}')
if not re.search(r'All \d+ required tests passed',log):raise SystemExit('Native suite did not report success')
print(f'PASS native fixture coverage: all {expected} PL4 regression tests executed.')
