"""Fail-closed byte provenance for the immutable 1.21.1 published-beta baseline.

This is deliberately *not* proof that the downloaded binary was installed or
executed. The two-client source-compiled migration remains a separate gate.
"""
import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path

REPO = "Shnewbs/Foundations-PL4"
TAG = "v0.2b.0"
SOURCE = "7c7c07ff5ae0bce4990bf2f0a739841161ce8f8c"
RELEASE_ID = 416116341
ASSET = "FoundationsPL4-1.21.1-0.2b.0.jar"
EXPECTED_SHA256 = "7fa14351e1bbf039846afba040e4b66ce16b98db64103f1b33fc8ce14df0c229"
EXPECTED_SIZE = 1244008


def sha256_file(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as f:
        for part in iter(lambda: f.read(1024 * 1024), b""):
            h.update(part)
    return h.hexdigest()


def verify(release, tag_ref, jar_path):
    """Reject swapped tags, release records, missing/duplicate assets and bad JARs."""
    if (not isinstance(tag_ref, dict) or tag_ref.get("ref") != "refs/tags/" + TAG or
        not isinstance(tag_ref.get("object"), dict) or
        tag_ref["object"].get("type") != "commit" or
        tag_ref["object"].get("sha") != SOURCE):
        raise ValueError("Released-beta tag is not the pinned immutable commit")
    if (not isinstance(release, dict) or type(release.get("id")) is not int or
        release["id"] != RELEASE_ID or release.get("tag_name") != TAG or
        release.get("draft") is not False or release.get("prerelease") is not True or
        not release.get("published_at") or
        release.get("html_url") != "https://github.com/" + REPO + "/releases/tag/" + TAG):
        raise ValueError("Released-beta record is missing, replaced or ineligible")
    assets = release.get("assets")
    if not isinstance(assets, list):
        raise ValueError("Released-beta asset manifest missing")
    matches = [a for a in assets if isinstance(a, dict) and a.get("name") == ASSET]
    if len(matches) != 1:
        raise ValueError("Expected exactly one matching published production JAR")
    asset = matches[0]
    if (type(asset.get("id")) is not int or asset["id"] <= 0 or
        asset.get("state") != "uploaded" or asset.get("digest") != "sha256:" + EXPECTED_SHA256 or
        type(asset.get("size")) is not int or asset["size"] != EXPECTED_SIZE):
        raise ValueError("Published asset metadata is not pinned to the accepted binary bytes")
    jar_path = Path(jar_path)
    if jar_path.name != ASSET or not jar_path.is_file() or jar_path.stat().st_size != EXPECTED_SIZE:
        raise ValueError("Exact published-beta JAR missing or has unexpected length")
    actual = sha256_file(jar_path)
    if actual != EXPECTED_SHA256:
        raise ValueError("Downloaded published-beta JAR SHA256 mismatch")
    try:
        with zipfile.ZipFile(jar_path) as archive:
            names = archive.namelist()
            if (len(names) != len(set(names)) or
                any(name.startswith("/") or ".." in name.split("/") for name in names)):
                raise ValueError("Unsafe or duplicated production JAR member")
            if archive.testzip() is not None:
                raise ValueError("Production JAR has a corrupt member CRC")
            classes = [name for name in names if name.startswith("net/foundations/pl4/") and name.endswith(".class")]
            if not classes or "META-INF/neoforge.mods.toml" not in names:
                raise ValueError("Published JAR lacks PL4 bytecode or NeoForge metadata")
            metadata = archive.read("META-INF/neoforge.mods.toml").decode("utf-8")
            for field in ('modId="foundations_pl4"', 'version="0.2b.0"',
                          'modId="minecraft"', 'versionRange="[1.21.1]"'):
                if field not in metadata:
                    raise ValueError("Incorrect published JAR mod identity, version or Minecraft target: " + field)
            entry_count = len(names)
            class_count = len(classes)
    except (OSError, zipfile.BadZipFile, UnicodeError) as exc:
        raise ValueError("Published production JAR cannot be validated") from exc
    return {
        "schema": 1,
        "scope": "Immutable release metadata and downloaded 1.21.1 beta production JAR bytes only",
        "repository": REPO, "tag": TAG, "release_id": RELEASE_ID,
        "source_commit": SOURCE, "asset_id": asset["id"], "asset_name": ASSET,
        "asset_sha256": actual, "asset_bytes": EXPECTED_SIZE,
        "jar_entries": entry_count, "pl4_class_entries": class_count,
        "installed_binary_executed": False,
        "client_multiplayer_migration_accepted": False,
        "rc_publication_cleared": False
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--release-json", required=True, type=Path)
    parser.add_argument("--tag-ref-json", required=True, type=Path)
    parser.add_argument("--jar", required=True, type=Path)
    parser.add_argument("--receipt", required=True, type=Path)
    args = parser.parse_args()
    args.receipt.unlink(missing_ok=True)
    receipt = verify(json.loads(args.release_json.read_text()),
                     json.loads(args.tag_ref_json.read_text()), args.jar)
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    args.receipt.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print("PASS: immutable beta release metadata and actual JAR bytes verified; binary execution remains untested.")


if __name__ == "__main__":
    main()
