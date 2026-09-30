"""Upload the tested runtime JAR using the official CurseForge Upload API.

Configured only through environment variables. A release asset records successful
uploads so rerunning the same tagged build does not create duplicate files.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import urllib.error
import urllib.request
import uuid

API = "https://minecraft.curseforge.com/api"
MARKER = "curseforge-upload.json"


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise RuntimeError("CurseForge redirected the authenticated request; stopped without forwarding credentials.")


def request_json(path, token, data=None, content_type=None):
    headers = {"X-Api-Token": token, "Accept": "application/json"}
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(API + path, data=data, headers=headers)
    try:
        with urllib.request.build_opener(NoRedirect).open(req, timeout=120) as response:
            return json.load(response)
    except urllib.error.HTTPError as exc:
        detail = ""
        try:
            error = json.loads(exc.read(8192))
            if isinstance(error, dict):
                message = error.get("errorMessage", error.get("message", ""))
                if isinstance(message, str):
                    detail = " " + message.replace(token, "[redacted]").replace("\n", " ")[:400]
        except (ValueError, OSError):
            pass
        raise RuntimeError(f"CurseForge {path} returned HTTP {exc.code}.{detail} Check project permissions and metadata before retrying.") from None
    except urllib.error.URLError:
        raise RuntimeError("CurseForge connection failed. Check the project file list before retrying an upload; the request may have succeeded.") from None


def release_type(version):
    if re.search(r"alpha|\da(?:\.|$)", version, re.I):
        return "alpha"
    if re.search(r"beta|rc|\db(?:\.|$)", version, re.I):
        return "beta"
    return "release"


def multipart(metadata, jar):
    boundary = "pl4-" + uuid.uuid4().hex
    part = b"--" + boundary.encode()
    body = part + b'\r\nContent-Disposition: form-data; name="metadata"\r\nContent-Type: application/json\r\n\r\n'
    body += json.dumps(metadata).encode() + b"\r\n"
    body += part + f'\r\nContent-Disposition: form-data; name="file"; filename="{jar.name}"\r\nContent-Type: application/java-archive\r\n\r\n'.encode()
    body += jar.read_bytes() + b"\r\n" + part + b"--\r\n"
    return body, "multipart/form-data; boundary=" + boundary


def gh(*args):
    return subprocess.check_output(["gh", *args], text=True)


def main():
    project = os.environ.get("CURSEFORGE_PROJECT_ID", "").strip()
    token = os.environ.get("CURSEFORGE_API_TOKEN", "")
    if not project and not token:
        print("CurseForge upload skipped: project ID and API token are not configured.")
        return
    if not project.isdecimal() or int(project) <= 0 or not token:
        raise RuntimeError("Set a numeric CURSEFORGE_PROJECT_ID repository variable and CURSEFORGE_API_TOKEN secret.")
    version, tag, repo = (os.environ[k] for k in ("VERSION", "RELEASE_TAG", "GITHUB_REPOSITORY"))
    if not re.fullmatch(r"[A-Za-z0-9._-]+", version) or tag != "v" + version:
        raise RuntimeError("Invalid release version/tag.")
    jar = Path(f"build/libs/FoundationsPL4-1.21.1-{version}.jar")
    if not jar.is_file():
        raise RuntimeError("The tested runtime JAR is missing.")
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    release = json.loads(gh("release", "view", tag, "--repo", repo, "--json", "assets"))
    with tempfile.TemporaryDirectory() as directory:
        marker = Path(directory) / MARKER
        if any(a["name"] == MARKER for a in release["assets"]):
            gh("release", "download", tag, "--repo", repo, "--pattern", MARKER, "--dir", directory)
            prior = json.loads(marker.read_text())
            if prior.get("project_id") != project or prior.get("sha256") != digest:
                raise RuntimeError("The existing CurseForge upload record differs. Do not overwrite or duplicate this version.")
            print(f"CurseForge file {prior['file_id']} already uploaded; skipped.")
            return
        # The official upload API resolves supported version names directly.
        notes = Path(f"docs/releases/{version}.md")
        changelog = notes.read_text(encoding="utf-8") if notes.is_file() else f"Foundations PL4 {version}. See the matching GitHub Release for changes."
        metadata = {"changelog": changelog, "changelogType": "markdown", "displayName": f"Foundations PL4 {version}", "gameVersionNames": ["1.21.1", "NeoForge"], "releaseType": release_type(version)}
        data, content_type = multipart(metadata, jar)
        response = request_json(f"/projects/{project}/upload-file", token, data, content_type)
        file_id = response.get("id")
        if not isinstance(file_id, int) or isinstance(file_id, bool) or file_id <= 0:
            raise RuntimeError("CurseForge did not return a file ID; check the project before retrying.")
        print(f"CurseForge accepted file {file_id}; publication remains subject to CurseForge approval.")
        marker.write_text(json.dumps({"project_id": project, "file_id": file_id, "version": version, "sha256": digest}, indent=2) + "\n")
        try:
            gh("release", "upload", tag, str(marker), "--repo", repo)
        except subprocess.CalledProcessError:
            raise RuntimeError(f"CurseForge file {file_id} uploaded, but its GitHub receipt could not be saved. Check that file before retrying.") from None


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, KeyError, ValueError, OSError, subprocess.CalledProcessError) as exc:
        raise SystemExit(str(exc)) from None
