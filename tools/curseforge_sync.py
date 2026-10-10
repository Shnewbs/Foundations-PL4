"""Mirror checksum-verified GitHub releases to matching CurseForge Minecraft targets.

A backfill uses only the newest published PL4 release per Minecraft version,
never overwrites tags, and does not claim CurseForge moderator approval.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

from upload_curseforge import MARKER, gh, main as upload, parse_tag


def latest_per_target(releases):
    ordered = sorted(
        (r for r in releases if not r.get("isDraft")),
        key=lambda r: r.get("publishedAt") or r.get("createdAt") or "",
        reverse=True)
    selected = {}
    for release in ordered:
        tag = release.get("tagName", "")
        try:
            minecraft, _, _ = parse_tag(tag)
        except ValueError:
            continue
        selected.setdefault(minecraft, tag)
    return [(minecraft, selected[minecraft]) for minecraft in sorted(selected)]


def validate_checksum(jar, checksums):
    """Never send unverified bytes or an adjacent-target artifact to CurseForge."""
    entries = []
    for line in checksums.read_text(encoding="ascii").splitlines():
        columns = line.split(maxsplit=1)
        if len(columns) != 2:
            continue
        hex_hash, filename = columns
        if filename.lstrip("*").replace("\\", "/") == jar.name and len(hex_hash) == 64:
            entries.append(hex_hash.lower())
    if len(entries) != 1:
        raise RuntimeError("Missing or ambiguous GitHub release SHA256 entry for " + jar.name)
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    if digest != entries[0]:
        raise RuntimeError("GitHub release runtime fails its published SHA256 checksum: " + jar.name)
    return digest


def latest_tags(repo):
    rows = json.loads(gh("release", "list", "--repo", repo, "--limit", "100",
                         "--json", "tagName,isDraft,publishedAt"))
    return latest_per_target(rows)


def stage_release(tag, repo, folder):
    minecraft, version, loader = parse_tag(tag)
    jar_name = f"FoundationsPL4-{minecraft}-{version}.jar"
    lib = folder / "build/libs"
    lib.mkdir(parents=True)
    subprocess.run(["gh", "release", "download", tag, "--repo", repo,
                    "--pattern", jar_name, "--pattern", "SHA256SUMS.txt",
                    "--dir", str(lib)], check=True)
    jar = lib / jar_name
    checksums = lib / "SHA256SUMS.txt"
    if not jar.is_file() or not checksums.is_file():
        raise RuntimeError("Missing exact runtime/checksum from immutable tag " + tag)
    digest = validate_checksum(jar, checksums)
    details = json.loads(gh("release", "view", tag, "--repo", repo, "--json", "body,isDraft"))
    if details.get("isDraft"):
        raise RuntimeError("Cannot distribute a draft release")
    notes = folder / "docs/releases"
    notes.mkdir(parents=True)
    (notes / (version + ".md")).write_text(
        details.get("body", "") or f"Foundations PL4 {tag}",
        encoding="utf-8")
    return minecraft, version, loader, digest


def run(targets):
    repo = os.environ["GITHUB_REPOSITORY"]
    if not os.environ.get("CURSEFORGE_PROJECT_ID") or not os.environ.get("CURSEFORGE_API_TOKEN"):
        raise RuntimeError("CurseForge is not connected: repository variable CURSEFORGE_PROJECT_ID or secret CURSEFORGE_API_TOKEN missing")
    cwd = Path.cwd()
    results = {"repository": repo, "timestamp": datetime.now(timezone.utc).isoformat(), "files": []}
    failures = 0
    try:
        for expected_target, tag in targets:
            item = {"minecraft": expected_target, "tag": tag}
            try:
                with tempfile.TemporaryDirectory(prefix="pl4-cf-") as directory:
                    stage = Path(directory)
                    minecraft, version, loader, digest = stage_release(tag, repo, stage)
                    if minecraft != expected_target:
                        raise RuntimeError("GitHub tag and selected target disagree")
                    before = dict(os.environ)
                    try:
                        os.environ.update(RELEASE_TAG=tag, VERSION=version,
                                          MINECRAFT_VERSION=minecraft, MOD_LOADER=loader)
                        os.chdir(stage)
                        receipt = json.loads(gh("release", "view", tag, "--repo", repo, "--json", "assets"))
                        existed = any(asset["name"] == MARKER for asset in receipt["assets"])
                        upload()
                    finally:
                        os.chdir(cwd)
                        os.environ.clear()
                        os.environ.update(before)
                item.update(status="already_submitted" if existed else "accepted_by_upload_api",
                            loader=loader, sha256=digest)
            except (OSError, KeyError, ValueError, RuntimeError, subprocess.CalledProcessError) as exc:
                failures += 1
                item.update(status="failed", reason=str(exc)[:700])
                print(f"FAILED {tag}: {str(exc)[:700]}", file=sys.stderr, flush=True)
            results["files"].append(item)
            print(f"{tag}: {item['status']}", flush=True)
    finally:
        os.chdir(cwd)
        (cwd / "curseforge-sync-report.json").write_text(json.dumps(results, indent=2) + "\n", encoding="utf-8")
    print(f"CurseForge results: {len(targets) - failures} submitted/recorded; {failures} failed", flush=True)
    if failures:
        raise RuntimeError(f"{failures} uploads require review; see curseforge-sync-report.json")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--tag", help="One existing, fully validated release tag")
    modes.add_argument("--backfill", action="store_true",
                       help="Newest GitHub release per Minecraft target")
    args = parser.parse_args()
    repo = os.environ["GITHUB_REPOSITORY"]
    if args.tag:
        target, _, _ = parse_tag(args.tag)
        targets = [(target, args.tag)]
    else:
        targets = latest_tags(repo)
        if not targets:
            raise RuntimeError("No published GitHub releases match supported PL4 tags")
    run(targets)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, KeyError, ValueError, OSError, subprocess.CalledProcessError) as exc:
        raise SystemExit(str(exc)) from None
