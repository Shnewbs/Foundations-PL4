"""Submit an exact, locally verified PL4 runtime JAR without GitHub Actions.

Dry run by default. Set CURSEFORGE_API_TOKEN and pass --upload to publish.
The receipt is kept beside the JAR so a repeat invocation cannot duplicate it.
"""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import tomllib
import zipfile

from upload_curseforge import multipart, release_type, request_json


NAME = re.compile(r"FoundationsPL4-(?P<minecraft>(?:1|26)\.\d+(?:\.\d+)?)-(?P<version>[A-Za-z0-9][A-Za-z0-9._-]*)\.jar\Z")


def inspect(jar, notes):
    converted_java8 = jar.name.endswith("-java8.jar")
    normalized_name = jar.name.replace("-java8.jar", ".jar") if converted_java8 else jar.name
    match = NAME.fullmatch(normalized_name)
    if not match or jar.name.endswith("-sources.jar"):
        raise ValueError("Expected an exact-target runtime FoundationsPL4-<minecraft>-<version>.jar")
    minecraft, version = match.group("minecraft", "version")
    if minecraft in ("1.16.4", "1.16.5") and not converted_java8:
        raise ValueError("This Forge target requires the converted -java8.jar runtime")
    if minecraft not in ("1.16.4", "1.16.5") and converted_java8:
        raise ValueError("Java 8 conversion is not a release artifact for this target")
    if minecraft in ("1.6.4", "1.12.2"):
        raise ValueError("This legacy target requires separate mcmod.info validation")
    if minecraft == "1.7.10":
        raise ValueError("Minecraft 1.7.10 is excluded")
    loader = "NeoForge" if minecraft == "1.21.1" or minecraft.startswith("26.") else "Forge"
    if not notes.is_file() or not notes.read_text(encoding="utf-8").strip():
        raise ValueError("A nonempty, version-specific changelog is required")
    with zipfile.ZipFile(jar) as archive:
        names = set(archive.namelist())
        if not any(n.startswith("net/foundations/pl4/") and n.endswith(".class") for n in names):
            raise ValueError("JAR contains no compiled PL4 classes")
        metadata_name = "META-INF/neoforge.mods.toml" if loader == "NeoForge" else "META-INF/mods.toml"
        if metadata_name not in names:
            raise ValueError(f"JAR lacks {metadata_name} for {loader}")
        metadata = tomllib.loads(archive.read(metadata_name).decode("utf-8"))
        mods = metadata.get("mods", [])
        if not any(mod.get("modId") == "foundations_pl4" and mod.get("version") == version
                   for mod in mods):
            raise ValueError("JAR mod ID or release version differs from its filename")
        dependencies = metadata.get("dependencies", {}).get("foundations_pl4", [])
        target = next((dep.get("versionRange", "") for dep in dependencies
                       if dep.get("modId") == "minecraft"), "")
        if not target.startswith("[" + minecraft) or target[len(minecraft) + 1:len(minecraft) + 2] not in ("]", ","):
            raise ValueError("JAR metadata does not identify the exact target Minecraft version")
        if not any(dep.get("modId") == loader.lower() for dep in dependencies):
            raise ValueError("JAR dependency metadata does not match its loader")
    return minecraft, version, loader


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("notes", type=Path)
    parser.add_argument("--upload", action="store_true", help="Submit to CurseForge (default: validate only)")
    args = parser.parse_args()
    minecraft, version, loader = inspect(args.jar, args.notes)
    digest = hashlib.sha256(args.jar.read_bytes()).hexdigest()
    project = os.environ.get("CURSEFORGE_PROJECT_ID", "1716767")
    if not project.isdecimal() or int(project) <= 0:
        raise ValueError("CURSEFORGE_PROJECT_ID must be a positive integer")
    receipt = args.jar.with_suffix(".curseforge.json")
    if receipt.exists():
        previous = json.loads(receipt.read_text(encoding="utf-8"))
        if (previous.get("sha256") != digest or str(previous.get("project_id")) != project
                or previous.get("minecraft") != minecraft or previous.get("version") != version
                or previous.get("loader") != loader):
            raise ValueError("Existing receipt differs from the JAR or project; inspect the upload")
        print(f"Already submitted as CurseForge file {previous['file_id']}; no upload attempted")
        return
    metadata = {
        "changelog": args.notes.read_text(encoding="utf-8"),
        "changelogType": "markdown",
        "displayName": f"Foundations PL4 {minecraft} {version} ({loader})",
        "gameVersionNames": [minecraft, loader, "Client", "Server"],
        "releaseType": release_type(version),
    }
    print(json.dumps({"jar": str(args.jar), "sha256": digest, "project": project,
                      "metadata": {k: v for k, v in metadata.items() if k != "changelog"},
                      "changelog": str(args.notes)}, indent=2))
    if not args.upload:
        return
    token = os.environ.get("CURSEFORGE_API_TOKEN", "")
    if not token:
        raise ValueError("CURSEFORGE_API_TOKEN is required for direct upload")
    data, content_type = multipart(metadata, args.jar)
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
