"""Extract only an exact-source, hash-attested 1.21.1 production JAR from a native CI artifact.

This is an offline artifact provenance gate, not a native runtime or RC pass.
Do not substitute neighboring-version, sources, fixture, or historic JARs.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import tempfile
import zipfile

from installed_binary_smoke import validate_binary, digest


def extract(artifact_zip, output, artifact_sha256, source_sha, jar_sha256):
    artifact_zip, output = Path(artifact_zip), Path(output)
    for label, value, size in [
        ("artifact SHA256", artifact_sha256, 64),
        ("source SHA", source_sha, 40),
        ("production JAR SHA256", jar_sha256, 64),
    ]:
        if not re.fullmatch("[0-9a-f]{" + str(size) + "}", value):
            raise ValueError("Invalid immutable " + label)
    if not artifact_zip.is_file() or artifact_zip.is_symlink() or digest(artifact_zip) != artifact_sha256:
        raise ValueError("Retained native GitHub artifact ZIP SHA256 mismatch")
    if output.exists():
        raise FileExistsError("Will not overwrite an existing candidate binary")
    with zipfile.ZipFile(artifact_zip) as z:
        entries = z.infolist()
        names = [entry.filename for entry in entries]
        if (z.testzip() is not None or len(names) != len(set(names)) or
            any(n.startswith("/") or "\\" in n or ".." in n.split("/") for n in names)):
            raise ValueError("Corrupt, duplicated or unsafe native artifact archive")
        sources = [n for n in names if n == "verification-logs/SOURCE_COMMIT.txt"
                   or n.endswith("/verification-logs/SOURCE_COMMIT.txt")]
        if len(sources) != 1:
            raise ValueError("Native artifact lacks exactly one source-commit attestation")
        source_path = sources[0]
        if z.read(source_path).decode("ascii").strip() != source_sha:
            raise ValueError("Native artifact source is not the pinned exact commit")
        prefix = source_path[: -len("verification-logs/SOURCE_COMMIT.txt")]
        libprefix = prefix + "build/libs/"
        candidates = [n for n in names if n.startswith(libprefix)
                      and "/" not in n[len(libprefix):]
                      and n[len(libprefix):].startswith("FoundationsPL4-1.21.1-")
                      and n.endswith(".jar")
                      and not n.endswith(("-sources.jar", "-javadoc.jar", "-native-scenarios.jar"))]
        if len(candidates) != 1:
            raise ValueError("Native artifact production JAR is missing or ambiguous")
        name = candidates[0]
        info = z.getinfo(name)
        if info.file_size <= 0 or info.file_size > 128 * 1024 * 1024:
            raise ValueError("Invalid production JAR length")
        bytes_ = z.read(info)
        if hashlib.sha256(bytes_).hexdigest() != jar_sha256:
            raise ValueError("Pinned native production JAR SHA256 mismatch")
    output.parent.mkdir(parents=True, exist_ok=True)
    tmp = None
    try:
        with tempfile.NamedTemporaryFile(mode="wb", prefix=".pl4-candidate-", suffix=".jar",
                                         dir=output.parent, delete=False) as f:
            tmp = Path(f.name)
            f.write(bytes_)
        validate_binary(tmp, jar_sha256)
        os.replace(tmp, output)
    finally:
        if tmp is not None:
            tmp.unlink(missing_ok=True)
    receipt = {
        "schema": 1, "source_commit": source_sha,
        "artifact_zip_sha256": artifact_sha256,
        "source_marker": source_path, "production_member": name,
        "production_jar_sha256": jar_sha256,
        "output_name": output.name, "output_sha256": digest(output),
        "native_pass_rechecked_here": False,
        "installed_candidate_binary_executed": False,
        "rc_publication_cleared": False
    }
    Path(str(output) + ".provenance.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    return receipt


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--artifact-zip", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--artifact-sha256", required=True)
    p.add_argument("--source-sha", required=True)
    p.add_argument("--jar-sha256", required=True)
    args = p.parse_args()
    print(json.dumps(extract(args.artifact_zip, args.output, args.artifact_sha256,
                             args.source_sha, args.jar_sha256), sort_keys=True))


if __name__ == "__main__":
    main()
