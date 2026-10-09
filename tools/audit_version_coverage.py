"""Inventory final Minecraft release gaps; an artifact/branch is not a compatibility certification."""
from __future__ import annotations
import argparse, datetime as dt, hashlib, json, re
from pathlib import Path
import xml.etree.ElementTree as ET

UPSTREAM = frozenset({'1.7.10','1.9.4','1.10','1.10.1','1.10.2','1.12','1.12.1','1.12.2'})
EXCLUDED = frozenset({'1.7.10'})
VERSION = re.compile(r'\d+\.\d+(?:\.\d+)?')

def key(version: str) -> tuple[int, ...]:
    parts=tuple(map(int,version.split('.')))
    return parts+(0,)*(3-len(parts))

def pages(value: list) -> list:
    if not isinstance(value,list):raise ValueError('Expected a GitHub collection')
    return [entry for page in value for entry in page] if value and all(isinstance(page,list) for page in value) else value

def inventory(manifest: dict, forge_xml: str, branches: list, releases: list, as_of: dt.datetime) -> list[dict]:
    if '<!DOCTYPE' in forge_xml.upper() or '<!ENTITY' in forge_xml.upper():raise ValueError('Unexpected XML declarations')
    forge=set()
    for item in ET.fromstring(forge_xml).findall('./versioning/versions/version'):
        match=re.match(r'^(\d+\.\d+(?:\.\d+)?)-',item.text or '')
        if match:forge.add(match.group(1))
    refs={b['name'] for b in pages(branches)}
    published={}
    for release in sorted(pages(releases),key=lambda r:r.get('published_at') or '',reverse=True):
        if release.get('draft') or not release.get('published_at'):continue
        if dt.datetime.fromisoformat(release['published_at'].replace('Z','+00:00'))>as_of:continue
        for asset in release.get('assets',[]):
            name=asset.get('name','')
            if any(marker in name.lower() for marker in ['sources','scenarios','javadoc']):continue
            match=re.fullmatch(r'FoundationsPL4-(\d+\.\d+(?:\.\d+)?)-.+\.jar',name)
            if match and asset.get('state')=='uploaded':
                published.setdefault(match.group(1),{'tag':release['tag_name'],'runtime_asset':name,'prerelease':bool(release.get('prerelease'))})
    result=[];seen=set()
    for release in manifest['versions']:
        version=release['id']
        if release['type']!='release' or not VERSION.fullmatch(version) or key(version)<key('1.6.4'):continue
        when=dt.datetime.fromisoformat(release['releaseTime'].replace('Z','+00:00'))
        if when>as_of:continue
        if version in seen:raise ValueError('Duplicate final Minecraft release')
        seen.add(version);record=published.get(version)
        status='EXCLUDED_BY_USER' if version in EXCLUDED else 'PL4_ARTIFACT_PRESENT' if record else 'UPSTREAM_LISTED' if version in UPSTREAM else 'MISSING_PL4_BUILD'
        result.append({'minecraft':version,'upstream_listed':version in UPSTREAM,'coverage':status,
                       'branch_present':'mc/'+version in refs,'forge_artifacts_listed':version in forge,
                       'latest_published_asset':record,'api_runtime_visual_acceptance':'SEE_TARGET_EVIDENCE_NOT_INFERRED',
                       'experimental':version=='1.6.4'})
    return sorted(result,key=lambda r:key(r['minecraft']))

def main() -> None:
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ['minecraft','forge','branches','releases','output']:parser.add_argument('--'+name,type=Path,required=True)
    args=parser.parse_args();names=['minecraft','forge','branches','releases']
    if any(getattr(args,n).stat().st_size>16_000_000 for n in names):parser.error('Input exceeds size bound')
    inputs={n:getattr(args,n).read_bytes() for n in names};now=dt.datetime.now(dt.timezone.utc)
    records=inventory(json.loads(inputs['minecraft']),inputs['forge'].decode(),json.loads(inputs['branches']),json.loads(inputs['releases']),now)
    if not {'1.6.4','1.16.4','1.16.5','1.18.2'}.issubset({r['minecraft'] for r in records}):raise ValueError('Official release inventory is incomplete')
    data={'as_of':now.isoformat(),'scope':'Final numerical Java Edition releases from 1.6.4; 1.7.10 excluded; future entries and snapshots excluded',
          'notice':'Artifact presence is not native, installed API, visual, multiplayer or performance acceptance. Forge absence does not imply no other loader.',
          'upstream_evidence':['https://www.curseforge.com/minecraft/mc-mods/practical-logistics/files/all','https://www.curseforge.com/minecraft/mc-mods/practical-logistics-2/files/all'],
          'input_sha256':{n:hashlib.sha256(raw).hexdigest() for n,raw in inputs.items()},'versions':records}
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(data,indent=2)+'\n')
    missing=[r['minecraft'] for r in records if r['coverage']=='MISSING_PL4_BUILD']
    print('Inventoried',len(records),'final releases; unbuilt upstream gaps:',len(missing));print(', '.join(missing))

if __name__=='__main__':main()
