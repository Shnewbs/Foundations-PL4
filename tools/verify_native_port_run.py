"""Reject a green server run that silently omitted native PL4 fixtures."""
from pathlib import Path
import re,sys
root=Path(__file__).resolve().parents[1]
expected=sum(p.read_text().count('@PortGameTest(') for p in (root/'src/main/java/net/foundations/pl4').glob('*GameTests.java'))
log=Path(sys.argv[1]).read_text()
counts=re.findall(r"Running test environment 'foundations_pl4:[^']+' batch \d+ \((\d+) tests\)",log)
actual=sum(map(int,counts))
if actual!=expected:raise SystemExit(f'Native fixture coverage mismatch: expected {expected}, ran {actual}')
if not re.search(r'All \d+ required tests passed',log):raise SystemExit('Native suite did not report success')
print(f'PASS native fixture coverage: all {expected} PL4 regression tests executed.')
