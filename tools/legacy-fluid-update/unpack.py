"""Verify and expand the reviewed source delta used by the two legacy tracks."""
from pathlib import Path, PurePosixPath
import base64, gzip, hashlib, json, sys
root=Path(__file__).resolve().parent
raw=base64.b64decode(''.join((root/('payload.b64.'+str(i))).read_text(encoding='utf-8') for i in range(5)),validate=True)
if hashlib.sha256(raw).hexdigest()!='a86962fcf29306fe5a412af652183028eef440d030ae21826d9a25f4ded18100':
    raise SystemExit('Reviewed source bundle checksum mismatch')
text=gzip.decompress(raw)
if len(text)>1_000_000:raise SystemExit('Source bundle too large')
files=json.loads(text)
if not isinstance(files,dict) or len(files)!=11:raise SystemExit('Invalid bundle')
out=Path(sys.argv[1]).resolve();out.mkdir(parents=True,exist_ok=True)
for name,content in files.items():
    p=PurePosixPath(name)
    if p.is_absolute() or '..' in p.parts or '\\' in name or not isinstance(content,str):raise SystemExit('Invalid source path')
    dest=out.joinpath(*p.parts);dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(content,encoding='utf-8')
print('Verified and expanded 11 reviewed source/tool files; no runtime binaries in the bundle.')
