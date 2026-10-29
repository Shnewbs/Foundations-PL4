"""Exact 26.3 beta RC release preflight, packaging and independent policy audit."""
import hashlib, json, os, re, subprocess, sys, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def eligible(status,event,ref,sha,head):
    version=status.get("version")
    if status.get("minecraft")!="26.3" or not isinstance(version,str):
        raise ValueError("Wrong target or version")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{1,99}",version):
        raise ValueError("Invalid version")
    if re.search(r"(?:^|[._-])(dev|nightly|snapshot)(?:$|[._-])",version,re.I):
        raise ValueError("Development source")
    if not re.search(r"(?:^|[._-])rc(?:[._-]?\d+)?(?:$|[._-])",version,re.I):
        raise ValueError("Only RC beta is publishable")
    if status.get("release_channel")!="beta" or status.get("rc_publication_cleared") is not True:
        raise ValueError("Exact-source RC clearance absent")
    if event!="push" or ref!="refs/tags/mc26.3-v"+version:
        raise ValueError("Wrong exact target/version tag or event")
    if not isinstance(sha,str) or not re.fullmatch(r"[0-9a-f]{40}",sha) or head!=sha:
        raise ValueError("Immutable tag source SHA mismatch")
    return version
def paths(v):
    d=ROOT/"build/libs"
    return [d/f"FoundationsPL4-26.3-{v}.jar",d/f"FoundationsPL4-26.3-{v}-sources.jar",
            d/f"FoundationsPL4-26.3-{v}-source.zip"]
def verify(v):
    expected={}
    for p in paths(v):
        if not p.is_file(): raise ValueError("Missing exact-target file "+p.name)
        with zipfile.ZipFile(p) as z:
            if z.testzip(): raise ValueError("Invalid ZIP CRC: "+p.name)
            if p.name.endswith(".jar") and not p.name.endswith("-sources.jar"):
                m=z.read("META-INF/neoforge.mods.toml").decode()
                if 'modId="foundations_pl4"' not in m or f'version="{v}"' not in m:
                    raise ValueError("Wrong native JAR metadata")
        expected[p.name]=hashlib.sha256(p.read_bytes()).hexdigest()
    actual={}
    for line in (ROOT/"build/libs/SHA256SUMS.txt").read_text(encoding="ascii").splitlines():
        digest,sep,name=line.partition("  ")
        if not sep or not re.fullmatch("[0-9a-f]{64}",digest) or name in actual:
            raise ValueError("Invalid checksum manifest entry")
        actual[name]=digest
    if expected!=actual: raise ValueError("Neighbor, extra or incorrect checksum asset")
def audit():
    w=ROOT/".github/workflows"
    r=(w/"release.yml").read_text()
    p=(w/"port-26.yml").read_text()
    if "mc26.3-v*" not in r or "release_263_policy.py preflight" not in r or "--verify-tag" not in r:
        raise ValueError("Missing exact-source tag and immutable release policy")
    if "gh release upload" in r or "--clobber" in r or "upload_curseforge.py" in r:
        raise ValueError("Mutable or branch-local CurseForge publication")
    if "  publish:" in p or "contents: write" in p:
        raise ValueError("Native matrix must not publish")
    for n in ("curseforge-26.yml","publish-port-26.yml","curseforge.yml"):
        declarations=(w/n).read_text().split("      - name: Confirm safe" if n=="curseforge.yml" else "      - name: Audit",1)[0]
        if "contents: read" not in declarations or any(x in declarations for x in
            ("contents: write","workflow_dispatch:","workflow_run:","secrets.")):
            raise ValueError("Unsafe historical publisher "+n)
def main():
    command=sys.argv[1]
    if command=="audit":
        audit();print("PASS retired 26.3 publishers and immutable exact target");return
    status=json.loads((ROOT/"BUILD_STATUS.json").read_text())
    head=subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
    v=eligible(status,os.getenv("GITHUB_EVENT_NAME"),os.getenv("GITHUB_REF"),os.getenv("GITHUB_SHA"),head)
    if command=="preflight":
        print("PASS exact 26.3 RC source/tag",head);return
    if command=="package":
        source=paths(v)[2];source.parent.mkdir(parents=True,exist_ok=True)
        subprocess.run(["git","archive","--format=zip","--output="+str(source),"HEAD"],cwd=ROOT,check=True)
        entries=paths(v)
        if not all(p.is_file() for p in entries): raise ValueError("Missing tested runtime/source artifacts")
        (ROOT/"build/libs/SHA256SUMS.txt").write_text(
            "".join(hashlib.sha256(p.read_bytes()).hexdigest()+"  "+p.name+"\n" for p in entries))
    if command in ("package","verify"): verify(v);print("PASS verified exact-target ZIP/JAR/checksum identities")
    else: raise ValueError("Unknown command")
if __name__=="__main__":
    try: main()
    except (ValueError,OSError,IndexError,zipfile.BadZipFile,KeyError,subprocess.CalledProcessError) as e:
        raise SystemExit("RC publication blocked: "+str(e)) from None
