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


def extract(artifact_zip, output, artifact_sha256, source_sha, jar_sha256, artifact_metadata=None):
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
    # Github's main verification artifact does not embed SOURCE_COMMIT.txt.
    # In this case require the authentic GitHub REST artifact metadata, fetched
    # in the same workflow run (not a user-provided label or nearby-version JAR).
    authenticated_source = None
    artifact_id = None
    if artifact_metadata is not None:
        metadata = json.loads(Path(artifact_metadata).read_text())
        workflow = metadata.get("workflow_run", {})
        artifact_id = metadata.get("id")
        if (type(artifact_id) is not int or artifact_id <= 0 or
                metadata.get("expired") is not False or
                metadata.get("digest") != "sha256:" + artifact_sha256 or
                metadata.get("archive_download_url") !=
                    "https://api.github.com/repos/Shnewbs/Foundations-PL4/actions/artifacts/" + str(artifact_id) + "/zip" or
                type(workflow.get("id")) is not int or workflow["id"] <= 0 or
                workflow.get("repository_id") != 1396808859 or
                workflow.get("head_repository_id") != 1396808859 or
                workflow.get("head_branch") != "main" or
                workflow.get("head_sha") != source_sha or
                not str(metadata.get("name", "")).startswith("verification-" + source_sha + "-")):
            raise ValueError("Native artifact GitHub source/digest metadata is missing, stale or mismatched")
        authenticated_source = source_sha
    with zipfile.ZipFile(artifact_zip) as z:
        entries = z.infolist()
        names = [entry.filename for entry in entries]
        if (z.testzip() is not None or len(names) != len(set(names)) or
            any(n.startswith("/") or "\\" in n or ".." in n.split("/") for n in names)):
            raise ValueError("Corrupt, duplicated or unsafe native artifact archive")
        sources = [n for n in names if n == "verification-logs/SOURCE_COMMIT.txt"
                   or n.endswith("/verification-logs/SOURCE_COMMIT.txt")]
        if len(sources) > 1 or (not sources and authenticated_source is None):
            raise ValueError("Native artifact lacks exactly one independent source attestation")
        source_path = sources[0] if sources else None
        if source_path is not None and z.read(source_path).decode("ascii").strip() != source_sha:
            raise ValueError("Embedded native artifact source is not the pinned exact commit")
        prefix = source_path[: -len("verification-logs/SOURCE_COMMIT.txt")] if source_path else ""
        libprefix = prefix + "build/libs/"
        # This exact retained aa9b439 1.21.1 candidate requires its real native
        # GameTest log, not just a plausible checksum/zip from a failed job.
        native_path = prefix + "verification-logs/native.log"
        if native_path not in names:
            raise ValueError("Pinned native candidate has no actual JVM test log")
        native_raw = z.read(native_path)
        native_log = native_raw.decode("utf-8", errors="replace")
        if (not re.search(r"={6,} 194 GAME TESTS COMPLETE IN [0-9.]+ s", native_log) or
            "All 194 required tests passed :)" not in native_log or
            "BUILD SUCCESSFUL" not in native_log):
            raise ValueError("Pinned 1.21.1 native GameTest log lacks exact 194/194 success markers")
        native_log_sha256 = hashlib.sha256(native_raw).hexdigest()
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
        "source_marker": source_path, "artifact_github_metadata_id": artifact_id,
        "native_log_member": native_path, "native_log_sha256": native_log_sha256,
        "native_log_success_count": "194/194",
        "source_attestation": "github-validated-artifact-metadata" if source_path is None else "embedded-source-marker",
        "production_member": name,
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
    p.add_argument("--artifact-metadata", type=Path, help="Required if archive lacks embedded source marker; obtain from authenticated gh api")
    args = p.parse_args()
    print(json.dumps(extract(args.artifact_zip, args.output, args.artifact_sha256,
                             args.source_sha, args.jar_sha256, args.artifact_metadata), sort_keys=True))


if __name__ == "__main__":
    main()
