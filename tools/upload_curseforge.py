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
TAG_PATTERN = re.compile(r"(?:(?:mc(?P<minecraft>(?:1|26)\.\d+(?:\.\d+)?))-)?v(?P<version>[A-Za-z0-9][A-Za-z0-9._-]*)\Z")


def parse_tag(tag):
    """Derive the exact Minecraft target from a published PL4 release tag."""
    match = TAG_PATTERN.fullmatch(tag)
    if match is None:
        raise ValueError("Unsupported Foundations PL4 release tag")
    minecraft, version = match.group("minecraft") or "1.21.1", match.group("version")
    if minecraft == "1.7.10":
        raise ValueError("Minecraft 1.7.10 is explicitly excluded from PL4")
    loader = "NeoForge" if minecraft == "1.21.1" or minecraft.startswith("26.") else "Forge"
    return minecraft, version, loader


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
    """Never label an alpha port or preview as a stable CurseForge release."""
    text = version.lower()
    if re.search(r"beta|(?:^|[.\-])rc\d*(?:[.\-]|$)|\db(?:[.\-]|$)", text):
        return "beta"
    if re.search(r"alpha|preview|experimental|(?:^|[.\-])port(?:[.\-]|$)|\da(?:[.\-]|$)", text):
        return "alpha"
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
    tag, repo = (os.environ[k] for k in ("RELEASE_TAG", "GITHUB_REPOSITORY"))
    minecraft, version, loader = parse_tag(tag)
    if (os.environ.get("VERSION", version) != version
            or os.environ.get("MINECRAFT_VERSION", minecraft) != minecraft
            or os.environ.get("MOD_LOADER", loader) != loader):
        raise RuntimeError("Release version, Minecraft target, or loader differs from its immutable tag.")
    jar = Path(f"build/libs/FoundationsPL4-{minecraft}-{version}.jar")
    if not jar.is_file() or not jar.stat().st_size:
        raise RuntimeError("Exact validated runtime JAR is missing: " + str(jar))
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    release = json.loads(gh("release", "view", tag, "--repo", repo, "--json", "assets,isDraft"))
    if release.get("isDraft"):
        raise RuntimeError("Draft releases cannot be submitted to CurseForge.")
    if not any(a["name"] == jar.name for a in release["assets"]):
        raise RuntimeError("The exact target JAR is absent from the published GitHub release.")
    with tempfile.TemporaryDirectory() as directory:
        marker = Path(directory) / MARKER
        if any(a["name"] == MARKER for a in release["assets"]):
            gh("release", "download", tag, "--repo", repo, "--pattern", MARKER, "--dir", directory)
            prior = json.loads(marker.read_text())
            if (str(prior.get("project_id")) != project or prior.get("sha256") != digest
                    or prior.get("version", version) != version
                    or prior.get("minecraft", minecraft) != minecraft
                    or prior.get("loader", loader) != loader):
                raise RuntimeError("Existing CurseForge receipt differs; refusing a duplicate or overwrite.")
            print(f"CurseForge file {prior['file_id']} already submitted for {minecraft} {version}; skipped.")
            return
        # The official upload API resolves supported version names directly.
        notes = Path(f"docs/releases/{version}.md")
        changelog = notes.read_text(encoding="utf-8") if notes.is_file() else f"Foundations PL4 {version}. See the matching GitHub Release for changes."
        game_versions = [minecraft, loader, "Client", "Server"]
        maturity = release_type(version)
        metadata = {"changelog": changelog, "changelogType": "markdown",
                    "displayName": f"Foundations PL4 {minecraft} {version} ({loader})",
                    "gameVersionNames": game_versions, "releaseType": maturity}
        data, content_type = multipart(metadata, jar)
        response = request_json(f"/projects/{project}/upload-file", token, data, content_type)
        file_id = response.get("id")
        if not isinstance(file_id, int) or isinstance(file_id, bool) or file_id <= 0:
            raise RuntimeError("CurseForge did not return a file ID; check the project before retrying.")
        print(f"CurseForge accepted file {file_id}; publication remains subject to CurseForge approval.")
        marker.write_text(json.dumps({
            "project_id": project, "file_id": file_id,
            "minecraft": minecraft, "loader": loader, "tag": tag,
            "version": version, "sha256": digest,
            "game_version_names": game_versions, "release_type": maturity,
            "upload_status": "accepted_by_api_not_moderation_verified"
        }, indent=2) + "\n")
        try:
            gh("release", "upload", tag, str(marker), "--repo", repo)
        except subprocess.CalledProcessError:
            raise RuntimeError(f"CurseForge file {file_id} uploaded, but its GitHub receipt could not be saved. Check that file before retrying.") from None


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, KeyError, ValueError, OSError, subprocess.CalledProcessError) as exc:
        raise SystemExit(str(exc)) from None
