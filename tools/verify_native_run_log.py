"""Fail closed when the NeoForge GameTest server exits without running tests."""
import pathlib
import json
import re
import sys

log = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace")
started = re.findall(r"(\d+) tests are now running", log)
completed = re.findall(r"(\d+) GAME TESTS COMPLETE", log)
passed = re.findall(r"All (\d+) required tests passed", log)
expected = json.loads((pathlib.Path(__file__).resolve().parents[1] / "BUILD_STATUS.json").read_text())["expected_native_tests"]
if not started or not completed or not passed or len({started[-1], completed[-1], passed[-1]}) != 1 or int(passed[-1]) != expected:
    raise SystemExit("FAIL: native server did not complete and pass the required GameTests")
print(f"PASS: {passed[-1]} native GameTests completed")
