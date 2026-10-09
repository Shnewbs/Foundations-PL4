"""Expand the checksum-pinned, reviewed energy source update; contains no binaries."""
from pathlib import Path, PurePosixPath
import base64, gzip, hashlib, json, sys
root=Path(__file__).resolve().parent
raw=base64.b64decode(''.join((root/('payload.b64.'+str(i))).read_text(encoding='utf-8') for i in range(4)),validate=True)
if hashlib.sha256(raw).hexdigest()!='f3cd6dd78841af6a4813a14a3eaede2385f72418aeb2238fd5eaa201ee8a4725':
    raise SystemExit('Reviewed source bundle checksum mismatch')
text=gzip.decompress(raw)
if len(text)>1_000_000:raise SystemExit('Source bundle too large')
files=json.loads(text)
if not isinstance(files,dict) or len(files)!=12:raise SystemExit('Invalid bundle')
out=Path(sys.argv[1]).resolve();out.mkdir(parents=True,exist_ok=True)
for name,content in files.items():
    p=PurePosixPath(name)
    if p.is_absolute() or '..' in p.parts or '\\' in name or not isinstance(content,str):raise SystemExit('Invalid source path')
    dest=out.joinpath(*p.parts);dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(content,encoding='utf-8')
print('Verified and expanded 12 reviewed source/tool files; native validation remains required.')
