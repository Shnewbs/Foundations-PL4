"""Submit an exact, locally verified PL4 runtime JAR without GitHub Actions.

Dry run by default. Set CURSEFORGE_API_TOKEN and pass --upload to publish.
The receipt is kept beside the JAR so a repeat invocation cannot duplicate it.
"""

import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import tomllib
import zipfile

from upload_curseforge import multipart, release_type, request_json
from publication_policy import is_rc, require_publication


NAME = re.compile(r"FoundationsPL4-(?P<minecraft>(?:1|26)\.\d+(?:\.\d+)?)-(?P<version>[A-Za-z0-9][A-Za-z0-9._-]*)\.jar\Z")

# These are the two independently maintained 26.x NeoForge release tracks.
# mc/26.1 and mc/26.2 are audit-only; 26.4 is a snapshot compatibility watch.
# Add a future target only after official loader availability and owner approval.
SUPPORTED_26X_RELEASE_TARGETS = frozenset(("26.1.2", "26.3"))

# Exact currently published tracks eligible for this modern metadata publisher.
# The other two published tracks (1.6.4 and 1.12.2) use their own legacy
# mcmod.info publishers; 1.13.2 is an unfinished port, not a release target.
# Do not silently accept an adjacent Minecraft patch or a newly named JAR.
SUPPORTED_DIRECT_UPLOAD_TARGETS = frozenset((
    "1.14.4", "1.15.2",
    "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5",
    "1.17.1", "1.18.1", "1.18.2",
    "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4",
    "1.20", "1.20.1", "1.20.2", "1.20.4", "1.21.1",
    "26.1.2", "26.3",
))


def require_supported_direct_target(minecraft):
    """Reject non-ledger versions before a release can reach CurseForge."""
    if minecraft not in SUPPORTED_DIRECT_UPLOAD_TARGETS:
        raise ValueError(
            f"Minecraft {minecraft} is not an approved direct-upload release target; "
            "native validation and owner approval are required")


# Check resource ceilings before ZipFile.testzip() decompresses every member.
# These limits are generous for PL4 runtime JARs, but reject ZIP bombs.
MAX_JAR_ENTRIES = 50_000
MAX_JAR_MEMBER_BYTES = 128 * 1024 * 1024
MAX_JAR_UNCOMPRESSED_BYTES = 512 * 1024 * 1024
MAX_JAR_COMPRESSION_RATIO = 10_000
# Limit the input snapshot itself, not only ZIP members after opening the JAR.
# The multipart body also holds these bytes, so fail before unbounded allocation.
MAX_JAR_COMPRESSED_BYTES = 256 * 1024 * 1024



def require_supported_26x_target(minecraft):
    """Fail closed before upload when a 26.x target is not in the release ledger."""
    if minecraft.startswith("26.") and minecraft not in SUPPORTED_26X_RELEASE_TARGETS:
        raise ValueError(
            f"Minecraft {minecraft} is not an approved exact NeoForge release target; "
            "do not publish a snapshot or audit-only port")




def verify_archive_integrity(archive):
    """Reject ambiguous, unsafe, or corrupt JAR entries before publication."""
    entries = archive.infolist()
    if len(entries) > MAX_JAR_ENTRIES:
        raise ValueError("JAR exceeds archive entry count limit")
    seen = set()
    total_uncompressed = 0
    for entry in entries:
        name = entry.filename
        if name in seen:
            raise ValueError(f"JAR contains duplicate archive entry: {name}")
        seen.add(name)
        if (not name or name.startswith("/") or "\\" in name or "//" in name
                or re.match(r"^[A-Za-z]:", name)
                or any(ord(char) < 32 for char in name)
                or any(part in (".", "..") for part in name.split("/"))):
            raise ValueError(f"JAR contains unsafe archive entry path: {name}")
        if ((entry.external_attr >> 16) & 0o170000) == 0o120000:
            raise ValueError(f"JAR contains symbolic-link archive entry: {name}")
        if entry.file_size > MAX_JAR_MEMBER_BYTES:
            raise ValueError(f"JAR exceeds member size limit: {name}")
        total_uncompressed += entry.file_size
        if total_uncompressed > MAX_JAR_UNCOMPRESSED_BYTES:
            raise ValueError("JAR exceeds total uncompressed size limit")
        if (entry.file_size >= 1024 * 1024 and
                (entry.compress_size == 0 or
                 entry.file_size > entry.compress_size * MAX_JAR_COMPRESSION_RATIO)):
            raise ValueError(f"JAR exceeds compression ratio limit: {name}")
    corrupt = archive.testzip()
    if corrupt is not None:
        raise ValueError(f"JAR contains corrupt archive entry: {corrupt}")



def java_classfile_limit(minecraft):
    """Maximum PL4 bytecode version for the exact target's Java runtime."""
    if minecraft.startswith("26."):
        return 69  # Java 25
    parts = tuple(int(part) for part in minecraft.split("."))
    if parts[1] > 20 or (parts[1] == 20 and len(parts) > 2 and parts[2] >= 5):
        return 65  # Java 21
    if parts[1] >= 18:
        return 61  # Java 17
    if parts[1] == 17:
        return 60  # Java 16
    return 52  # Java 8


def verify_pl4_bytecode(archive, names, minecraft):
    """Validate every class loaded by the target JVM, including bundled helpers.

    A PL4 class must exist, but a shaded class outside net/foundations/pl4/
    can also crash an older Minecraft JVM. Ignore multi-release classes only
    when their Java version is strictly newer than this target's runtime.
    """
    pl4_classes = [name for name in names
                   if name.startswith("net/foundations/pl4/") and name.endswith(".class")]
    if not pl4_classes:
        raise ValueError("JAR contains no compiled PL4 classes")
    maximum = java_classfile_limit(minecraft)
    java_version = maximum - 44
    classes = []
    for name in names:
        if not name.endswith(".class"):
            continue
        if name == "module-info.class" and java_version < 9:
            continue  # Java 8 ignores the Java 9 module descriptor.
        if name.startswith("META-INF/versions/"):
            parts = name.split("/")
            if len(parts) < 4 or not parts[2].isdecimal():
                raise ValueError(f"JAR contains invalid multi-release class path: {name}")
            if int(parts[2]) > java_version:
                continue  # The target JVM cannot select this versioned class.
        classes.append(name)
    for name in sorted(classes):
        with archive.open(name) as stream:
            header = stream.read(10)
        if (len(header) != 10 or header[:4] != bytes.fromhex("cafebabe")
                or int.from_bytes(header[8:10], "big") == 0):
            raise ValueError(f"JAR contains an invalid PL4 classfile header: {name}")
        minor = int.from_bytes(header[4:6], "big")
        major = int.from_bytes(header[6:8], "big")
        if minor != 0 or not 45 <= major <= maximum:
            raise ValueError(
                f"JAR contains incompatible PL4 bytecode for {minecraft}: {name} "
                f"(class {major}.{minor}; maximum {maximum}.0)")


def verify_local_receipt(previous, digest, project, minecraft, version, loader):
    """Validate a local API receipt before suppressing a repeat upload.

    This is not a remote CurseForge lookup or moderation/publication proof.
    A damaged or untrusted receipt requires manual destination reconciliation.
    """
    if not isinstance(previous, dict):
        raise ValueError("Existing CurseForge receipt is not an object; reconcile the destination")
    file_id = previous.get("file_id")
    if isinstance(file_id, bool) or not isinstance(file_id, int) or file_id <= 0:
        raise ValueError("Existing CurseForge receipt has no valid file ID; reconcile the destination")
    if previous.get("upload_status") != "accepted_by_api_not_moderation_verified":
        raise ValueError("Existing CurseForge receipt lacks an API-acceptance status; reconcile the destination")
    if (previous.get("sha256") != digest or str(previous.get("project_id")) != project
            or previous.get("minecraft") != minecraft or previous.get("version") != version
            or previous.get("loader") != loader):
        raise ValueError("Existing receipt differs from the JAR or project; inspect the upload")
    return file_id


def read_jar_snapshot(jar):
    """Read a bounded, immutable snapshot for inspection, hashing and upload."""
    with jar.open("rb") as stream:
        jar_bytes = stream.read(MAX_JAR_COMPRESSED_BYTES + 1)
    if len(jar_bytes) > MAX_JAR_COMPRESSED_BYTES:
        raise ValueError("JAR exceeds compressed archive size limit")
    return jar_bytes


def inspect(jar, notes, jar_bytes=None):
    converted_java8 = jar.name.endswith("-java8.jar")
    normalized_name = jar.name.replace("-java8.jar", ".jar") if converted_java8 else jar.name
    match = NAME.fullmatch(normalized_name)
    if not match or jar.name.endswith("-sources.jar"):
        raise ValueError("Expected an exact-target runtime FoundationsPL4-<minecraft>-<version>.jar")
    minecraft, version = match.group("minecraft", "version")
    require_supported_26x_target(minecraft)
    if minecraft in ("1.16.4", "1.16.5") and not converted_java8:
        raise ValueError("This Forge target requires the converted -java8.jar runtime")
    if minecraft not in ("1.16.4", "1.16.5") and converted_java8:
        raise ValueError("Java 8 conversion is not a release artifact for this target")
    if minecraft in ("1.6.4", "1.12.2"):
        raise ValueError("This legacy target requires separate mcmod.info validation")
    if minecraft == "1.7.10":
        raise ValueError("Minecraft 1.7.10 is excluded")
    require_supported_direct_target(minecraft)
    loader = "NeoForge" if minecraft == "1.21.1" or minecraft.startswith("26.") else "Forge"
    if not notes.is_file() or not notes.read_text(encoding="utf-8").strip():
        raise ValueError("A nonempty, version-specific changelog is required")
    with zipfile.ZipFile(io.BytesIO(jar_bytes) if jar_bytes is not None else jar) as archive:
        verify_archive_integrity(archive)
        names = set(archive.namelist())
        verify_pl4_bytecode(archive, names, minecraft)
        metadata_name = "META-INF/neoforge.mods.toml" if loader == "NeoForge" else "META-INF/mods.toml"
        if metadata_name not in names:
            raise ValueError(f"JAR lacks {metadata_name} for {loader}")
        metadata = tomllib.loads(archive.read(metadata_name).decode("utf-8"))
        mods = metadata.get("mods", [])
        if not isinstance(mods, list) or not all(isinstance(mod, dict) for mod in mods):
            raise ValueError("JAR contains invalid mod metadata")
        pl4_mods = [mod for mod in mods if mod.get("modId") == "foundations_pl4"]
        if len(pl4_mods) != 1 or pl4_mods[0].get("version") != version:
            raise ValueError("JAR mod ID or release version differs from its filename or is duplicated")
        dependency_tables = metadata.get("dependencies", {})
        if not isinstance(dependency_tables, dict):
            raise ValueError("JAR contains invalid dependency metadata")
        dependencies = dependency_tables.get("foundations_pl4", [])
        if not isinstance(dependencies, list) or not all(isinstance(dep, dict) for dep in dependencies):
            raise ValueError("JAR contains invalid dependency metadata")
        minecraft_deps = [dep for dep in dependencies if dep.get("modId") == "minecraft"]
        loader_deps = [dep for dep in dependencies if dep.get("modId") == loader.lower()]
        if len(minecraft_deps) != 1 or len(loader_deps) != 1:
            raise ValueError("JAR must declare exactly one Minecraft and loader dependency")
        if any(dep.get("modId") in ("forge", "neoforge") and dep.get("modId") != loader.lower()
               for dep in dependencies):
            raise ValueError("JAR dependency metadata includes conflicting loaders")
        target = minecraft_deps[0].get("versionRange", "")
        if not isinstance(target, str) or not target.startswith("[" + minecraft) or (
                target[len(minecraft) + 1:len(minecraft) + 2] not in ("]", ",")):
            raise ValueError("JAR metadata does not identify the exact target Minecraft version")
        if loader == "NeoForge" and target != f"[{minecraft}]":
            raise ValueError("NeoForge JAR must declare an exact Minecraft target, not a range")
    return minecraft, version, loader


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("notes", type=Path)
    parser.add_argument("--upload", action="store_true", help="Submit to CurseForge (default: validate only)")
    args = parser.parse_args()
    # Pin one immutable byte snapshot across archive validation, hashing and upload.
    # A build replacing the on-disk JAR mid-publication cannot change uploaded bytes.
    jar_bytes = read_jar_snapshot(args.jar)
    minecraft, version, loader = inspect(args.jar, args.notes, jar_bytes=jar_bytes)
    maturity = release_type(version)  # Reject dev/nightly builds, even when a receipt already exists.
    if is_rc(version):
        status_path = Path("BUILD_STATUS.json")
        if not status_path.is_file():
            raise ValueError("RC publication requires exact-target BUILD_STATUS.json")
        require_publication(json.loads(status_path.read_text(encoding="utf-8")), minecraft, version)
    digest = hashlib.sha256(jar_bytes).hexdigest()
    project = os.environ.get("CURSEFORGE_PROJECT_ID", "1716767")
    if not project.isdecimal() or int(project) <= 0:
        raise ValueError("CURSEFORGE_PROJECT_ID must be a positive integer")
    receipt = args.jar.with_suffix(".curseforge.json")
    if receipt.exists():
        previous = json.loads(receipt.read_text(encoding="utf-8"))
        file_id = verify_local_receipt(previous, digest, project, minecraft, version, loader)
        print(f"Local receipt records API acceptance for CurseForge file {file_id}; "
              "verify its destination status before treating it as published. No upload attempted.")
        return
    metadata = {
        "changelog": args.notes.read_text(encoding="utf-8"),
        "changelogType": "markdown",
        "displayName": f"Foundations PL4 {minecraft} {version} ({loader})",
        "gameVersionNames": [minecraft, loader, "Client", "Server"],
        "releaseType": maturity,
    }
    print(json.dumps({"jar": str(args.jar), "sha256": digest, "project": project,
                      "metadata": {k: v for k, v in metadata.items() if k != "changelog"},
                      "changelog": str(args.notes)}, indent=2))
    if not args.upload:
        return
    token = os.environ.get("CURSEFORGE_API_TOKEN", "")
    if not token:
        raise ValueError("CURSEFORGE_API_TOKEN is required for direct upload")
    data, content_type = multipart(metadata, args.jar, jar_bytes=jar_bytes)
    result = request_json(f"/projects/{project}/upload-file", token, data, content_type)
    file_id = result.get("id")
    if isinstance(file_id, bool) or not isinstance(file_id, int) or file_id <= 0:
        raise RuntimeError("Upload response had no file ID; inspect CurseForge before retrying")
    receipt.write_text(json.dumps({"project_id": project, "file_id": file_id,
                                   "minecraft": minecraft, "version": version, "loader": loader,
                                   "sha256": digest, "upload_status": "accepted_by_api_not_moderation_verified"},
                                  indent=2) + "\n", encoding="utf-8")
    print(f"CurseForge accepted file {file_id}; moderation status is separate")


if __name__ == "__main__":
    main()
