"""Prove a legacy GitHub RC release is absent before creating an immutable tag.

Requires a successful authenticated repository probe and an explicit HTTP 404
for the exact tag. Every ambiguous/failed response is a publication blocker.
"""
import os
import re
import sys
from urllib import error, parse, request


def require_release_absent(repo, tag, token, opener=request.urlopen):
    if not isinstance(repo, str) or not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repo):
        raise ValueError("Invalid repository identity")
    if not isinstance(tag, str) or not re.fullmatch(r"mc1\.(?:6\.4|12\.2)-v[A-Za-z0-9][A-Za-z0-9._-]*", tag):
        raise ValueError("Invalid exact legacy release tag")
    if not token:
        raise ValueError("Missing authenticated GitHub release preflight")
    headers = {
        "Authorization": "Bearer " + token,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    base = "https://api.github.com/repos/" + repo

    def status(path):
        target = request.Request(base + path, headers=headers, method="GET")
        try:
            with opener(target, timeout=20) as response:
                return response.status
        except error.HTTPError as exc:
            return exc.code
        except (error.URLError, TimeoutError, OSError) as exc:
            raise RuntimeError("GitHub release preflight transport failed") from exc

    if status("") != 200:
        raise RuntimeError("Cannot authenticate and verify the expected GitHub repository")
    release_status = status("/releases/tags/" + parse.quote(tag, safe=""))
    if release_status != 404:
        raise RuntimeError("Release absence unproven (HTTP %s); reconcile receipts before publication" % release_status)
    return True


def main():
    if len(sys.argv) != 2:
        raise SystemExit("Expected exact legacy release tag")
    try:
        require_release_absent(os.environ.get("GITHUB_REPOSITORY"), sys.argv[1], os.environ.get("GH_TOKEN"))
    except (ValueError, RuntimeError) as exc:
        raise SystemExit(str(exc)) from exc
    print("Exact GitHub release tag is absent (authenticated HTTP 404); immutable publication preflight passed")


if __name__ == "__main__":
    main()
