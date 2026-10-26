import json, tempfile, unittest
from pathlib import Path
from hourly_builds import validate_live_evidence_cases
NAMES = ["live retained evidence blocks after external fault clearing","live retained evidence rejects face edit with empty escrow","live retained evidence prevents transfer after owner rewrite","live retained evidence reload schedules admission without fault text","live retained evidence reload preserves wide escrow count","live retained evidence rewritten context freezes on reload admission","live retained evidence removal seals after fault clearing","live retained evidence removal rejects rewritten owner context"]
class LiveEvidenceCasesTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        (self.root / "run-legacy").mkdir()
        (self.root / "verification-logs").mkdir()
        self.rows = [{"name": n, "passed": True} for n in NAMES]
        self.lines = ["PASS " + n for n in NAMES]
    def validate(self, mc="1.12.2"):
        (self.root / "run-legacy/legacy-scenarios.json").write_text(json.dumps({"results": self.rows}))
        (self.root / "verification-logs/native.log").write_text("\n".join(self.lines))
        validate_live_evidence_cases(self.root, mc)
    def test_exact_named_cases(self): self.validate()
    def test_exact_164_markers(self): self.validate("1.6.4")
    def test_164_native_timestamp_prefix(self):
        self.lines = ["2026-10-26 21:15:59 [INFO] [STDOUT] " + line for line in self.lines]
        self.validate("1.6.4")
    def test_missing_case(self):
        self.rows.pop()
        with self.assertRaises(ValueError): self.validate()
    def test_duplicate_case(self):
        self.rows.append(self.rows[0])
        with self.assertRaises(ValueError): self.validate()
    def test_failed_case(self):
        self.rows[0]["passed"] = False
        with self.assertRaises(ValueError): self.validate()
    def test_truthy_is_not_pass(self):
        self.rows[0]["passed"] = 1
        with self.assertRaises(ValueError): self.validate()
    def test_164_missing_marker(self):
        self.lines.pop()
        with self.assertRaises(ValueError): self.validate("1.6.4")
    def test_164_duplicate_marker(self):
        self.lines.append(self.lines[0])
        with self.assertRaises(ValueError): self.validate("1.6.4")
    def test_164_substring_is_not_marker(self):
        self.lines[0] = "prefix " + self.lines[0]
        with self.assertRaises(ValueError): self.validate("1.6.4")
    def test_other_target_rejected(self):
        with self.assertRaises(ValueError): self.validate("1.16.5")
if __name__ == "__main__": unittest.main()
