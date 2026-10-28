"""Offline fail-closed release preflight tests, including transient API failures."""
import unittest
from urllib import error

from verify_legacy_release_absence import require_release_absent


class Response:
    status = 200

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return False


class ReleasePreflightTests(unittest.TestCase):
    repo = "Shnewbs/Foundations-PL4"
    tag = "mc1.6.4-v0.2.0-rc.2"

    def opener(self, release_status=404, repo_status=200):
        calls = []

        def fetch(req, timeout):
            self.assertEqual(timeout, 20)
            self.assertTrue(req.get_header("Authorization").startswith("Bearer "))
            calls.append(req.full_url)
            code = release_status if "/releases/tags/" in req.full_url else repo_status
            if code != 200:
                raise error.HTTPError(req.full_url, code, "test", None, None)
            return Response()
        return fetch, calls

    def test_only_authenticated_repository_200_and_release_404_passes(self):
        fetch, calls = self.opener()
        self.assertTrue(require_release_absent(self.repo, self.tag, "test-token", fetch))
        self.assertEqual(len(calls), 2)
        self.assertTrue(calls[1].endswith("/releases/tags/" + self.tag))

    def test_existing_or_ambiguous_release_statuses_fail_closed(self):
        for code in (200, 401, 403, 429, 500, 502):
            fetch, _ = self.opener(release_status=code)
            with self.subTest(code=code), self.assertRaises(RuntimeError):
                require_release_absent(self.repo, self.tag, "test-token", fetch)

    def test_unverified_repository_identity_fails_even_for_release_404(self):
        for code in (401, 403, 404, 500):
            fetch, calls = self.opener(repo_status=code)
            with self.subTest(code=code), self.assertRaises(RuntimeError):
                require_release_absent(self.repo, self.tag, "test-token", fetch)
            self.assertEqual(len(calls), 1)

    def test_network_errors_are_not_absence_proof(self):
        def offline(req, timeout):
            raise error.URLError("network unavailable")
        with self.assertRaises(RuntimeError):
            require_release_absent(self.repo, self.tag, "test-token", offline)

    def test_missing_token_wrong_tag_and_wrong_repo_rejected_without_network(self):
        fetch, calls = self.opener()
        for repo, tag, token in ((self.repo, self.tag, ""), ("wrongrepo", self.tag, "x"),
                                  (self.repo, "mc1.12.1-v1.0.0-rc.1", "x"),
                                  (self.repo, "mc1.6.4-v1.0;echo bad", "x")):
            with self.assertRaises(ValueError):
                require_release_absent(repo, tag, token, fetch)
        self.assertEqual(calls, [])

    def test_other_maintained_legacy_exact_tag_supported(self):
        fetch, _ = self.opener()
        self.assertTrue(require_release_absent(self.repo, "mc1.12.2-v0.2.0-rc.2", "test-token", fetch))

    def test_obsolete_generic_release_workflow_is_frozen(self):
        from pathlib import Path
        workflow = Path(".github/workflows/release.yml").read_text()
        self.assertIn("jobs:", workflow)
        self.assertIn("  release:", workflow)
        self.assertTrue(any(row == "    if: false" for row in workflow.splitlines()))
        native = Path(".github/workflows/legacy164-native.yml").read_text()
        self.assertIn("rc_publication_cleared", native)
        self.assertIn("verify_legacy_release_absence.py", native)


if __name__ == "__main__":
    unittest.main()
