"""Bounded, read-only observations from an offline copy of an exact legacy save ZIP.

This collects evidence for review. It cannot authorize, reconcile, redeem, replay,
mint items or clear a fence. Saved ownership labels are not authorization proof.
"""
import argparse,hashlib,io,json,re,zipfile,zlib
from pathlib import Path,PurePosixPath
from inspect_legacy_provider_world import nbt

LIMITS=dict(archive_bytes=128*1024*1024,selected_bytes=32*1024*1024,chunk_bytes=2*1024*1024,chunks=128,tiles=4096,results=64,inventory_slots=256,journal_records=4096)
HASH_FORMAT='pl4-parsed-value-v2'

def digest_value(value):
    def safe(v):
        if isinstance(v,bytes):return ['bytes',v.hex()]
        if isinstance(v,dict):return ['compound',[[k,safe(x)] for k,x in sorted(v.items())]]
        if isinstance(v,list):return ['list',[safe(x) for x in v]]
        if type(v) is float:return ['float',v.hex()]
        if type(v) in {str,int,bool,type(None)}:return [type(v).__name__,v]
        raise ValueError('Unsupported observed value type')
    return hashlib.sha256(json.dumps([HASH_FORMAT,safe(value)],separators=(',',':')).encode()).hexdigest()

def record_errors(row,schema=2):
    errors=[];scope=row.get('Scope','');legacy=scope=='legacy-unknown'
    token=row.get('Token')
    if not isinstance(token,str) or not re.fullmatch(r'[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}',token):errors.append('invalid token')
    if not isinstance(scope,str) or scope not in {'','legacy-unknown'}:errors.append('invalid scope')
    owner=row.get('Owner')
    if not isinstance(owner,str) or len(owner)>128 or not legacy and not owner:errors.append('invalid owner')
    if legacy and (schema!=2 or not isinstance(row.get('Evidence'),dict)):errors.append('invalid legacy evidence/schema')
    keys=('HostX','HostY','HostZ') if legacy else ('X','Y','Z')
    if any(type(row.get(k)) is not int or not -2147483648<=row[k]<=2147483647 for k in keys):errors.append('invalid coordinates')
    elif not 0<=row[keys[1]]<=255:errors.append('invalid height')
    return errors

def inflate(raw,kind,limit):
    if kind not in {1,2}:raise ValueError('Unsupported region compression')
    stream=zlib.decompressobj(31 if kind==1 else 15)
    value=stream.decompress(raw,limit+1)
    if len(value)>limit or stream.unconsumed_tail or stream.unused_data or not stream.eof:raise ValueError('Compressed NBT exceeds bound or is truncated')
    return value

def journal_records(journal,limit):
    records=[];opaque=False;depth=0
    if journal is not None and not isinstance(journal,dict):raise ValueError('Malformed journal compound')
    while isinstance(journal,dict):
        if depth>=8:raise ValueError('Journal envelope depth exceeds bound')
        if journal.get('Schema')==-1 and isinstance(journal.get('Opaque'),dict):
            opaque=True;journal=journal['Opaque'];depth+=1;continue
        known=type(journal.get('Schema')) is int and journal['Schema'] in {1,2}
        if not known:opaque=True
        if known and 'Entries' not in journal:raise ValueError('Missing saved journal entries')
        entries=journal.get('Entries',[]) if known else []
        if not isinstance(entries,list) or len(records)+len(entries)>limit:raise ValueError('Journal record bound exceeded')
        if any(not isinstance(r,dict) for r in entries):raise ValueError('Noncompound journal entry')
        if any(record_errors(r,journal['Schema']) for r in entries):opaque=True
        records.extend(entries)
        if 'PreviousJournal' not in journal:break
        if not isinstance(journal['PreviousJournal'],dict):raise ValueError('Malformed previous journal')
        opaque=True;journal=journal['PreviousJournal'];depth+=1
    tokens=[r.get('Token') for r in records if isinstance(r.get('Token'),str)]
    if len(set(tokens))!=len(tokens):opaque=True
    return records,opaque

def observe(tiles,journal,token,owner,dimension,inventories,limits,mc='1.12.2'):
    records,opaque=journal_records(journal,limits['journal_records'])
    matches=[];faults=[];inventory_rows=[];receipt_rows=[];truncated=False
    def add(rows,row):
        nonlocal truncated
        if len(rows)>=limits['results']:truncated=True
        else:rows.append(row)
    def label(value):return value[:128] if isinstance(value,str) else value if type(value) is int else None
    for r in records:
        if r.get('Token')!=token:continue
        errors=record_errors(r);ambiguous=sum(row.get('Token')==token for row in records)>1
        known=not opaque and not errors and not ambiguous and r.get('Scope','')==''
        add(matches,dict(token=token,saved_owner=label(r.get('Owner')),owner_label_matches_query=r.get('Owner')==owner,original_target=[r[k] for k in ['X','Y','Z']] if known else None,observed_host=[r.get(k) if type(r.get(k)) is int else None for k in ['HostX','HostY','HostZ']] if r.get('Scope')=='legacy-unknown' else None,record_validation_errors=errors,duplicate_token_observation=ambiguous,original_target_withheld=not known,record_sha256=digest_value(r),route_claim='saved observation only; opaque/invalid/duplicate records grant no original target or reconciliation clearance'))
    for tile in tiles:
        position=[label(tile.get(k)) for k in ['x','y','z']]
        if 'ItemTransferFault' in tile or 'ItemLegacyEvidence' in tile:
            context=tile.get('ItemJournalContext');evidence=tile.get('ItemLegacyEvidence')
            linked=isinstance(context,dict) and context.get('Token')==token
            # An occurrence in opaque evidence is preserved as an observation, never upgraded into a trusted route.
            occurrence=isinstance(evidence,dict) and (evidence.get('OriginalToken')==token or isinstance(evidence.get('PreviousContext'),dict) and evidence['PreviousContext'].get('Token')==token)
            if linked or occurrence:
                add(faults,dict(position=position,saved_owner=label(tile.get('OwnerName' if mc=='1.6.4' else 'Owner')),current_context_token_matches=linked,preserved_evidence_token_occurrence=occurrence,context_state='absent' if 'ItemJournalContext' not in tile else 'compound' if isinstance(context,dict) else 'raw-scalar',sealed_claim_count=label(tile.get('ItemEscrowCount')),tile_sha256=digest_value(tile),original_route_inferred=False))
        items=tile.get('Items',[])
        if not isinstance(items,list):continue
        if len(items)>limits['inventory_slots']:raise ValueError('Saved inventory slot bound exceeded')
        for item in items:
            tag=item.get('tag',{}) if isinstance(item,dict) else {}
            if not isinstance(tag,dict) or 'ItemTransferFault' not in tag and 'ItemLegacyEvidence' not in tag:continue
            ctx=tag.get('ItemJournalContext');ev=tag.get('ItemLegacyEvidence')
            occurrence=isinstance(ev,dict) and (ev.get('OriginalToken')==token or isinstance(ev.get('PreviousContext'),dict) and ev['PreviousContext'].get('Token')==token)
            if isinstance(ctx,dict) and ctx.get('Token')==token or occurrence or tuple(position) in inventories:
                add(receipt_rows,dict(container=position,slot=label(item.get('Slot')),saved_item_id=label(item.get('id')),saved_stack_count=label(item.get('Count')),sealed_claim_count=label(tag.get('ItemEscrowCount')),current_context_token_matches=isinstance(ctx,dict) and ctx.get('Token')==token,preserved_evidence_token_occurrence=occurrence,receipt_sha256=digest_value(item),spendable_claim=False,original_route_inferred=False))
        if tuple(position) in inventories:
            add(inventory_rows,dict(position=position,saved_tile_id=label(tile.get('id')),saved_provider_id=label(tile.get('ProviderId')),saved_provider_owner_label=label(tile.get('ProviderOwner')),tile_sha256=digest_value(tile),slots=[dict(slot=label(i.get('Slot')),saved_item_id=label(i.get('id')),saved_count=label(i.get('Count')),saved_stack_sha256=digest_value(i)) for i in items if isinstance(i,dict)],identity_or_alias_proven=False,authorization_proven=False,spendable_total_inferred=False))
    return dict(query=dict(token=token,owner_label=owner,dimension=dimension),value_hash_format=HASH_FORMAT,journal_present=journal is not None,opaque_journal=opaque,matching_saved_records=matches,observed_fault_tiles=faults,observed_receipts=receipt_rows,explicit_inventory_observations=inventory_rows,result_limit_reached=truncated,reconciliation_cleared=False,original_route_inferred=False,scope='Offline saved observations only; missing tokens are not clearance. No replay, redemption, minting, inferred conservation or fence clearing. Provider labels are not identity, alias, authorization or durability proof. Value hashes describe parsed values, not original encoded NBT tag widths; compare only matching hash formats.')

def collect(path,mc,token,owner,dimension=0,inventories=(),limits=None):
    limits=dict(LIMITS if limits is None else limits)
    if mc not in {'1.12.2','1.6.4'}:raise ValueError('Exact supported legacy Minecraft identity required')
    if not re.fullmatch(r'[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}',token):raise ValueError('Canonical token required')
    if not isinstance(owner,str) or not 1<=len(owner)<=128:raise ValueError('Explicit bounded owner label required')
    if type(dimension) is not int or not -2147483648<=dimension<=2147483647:raise ValueError('Invalid dimension')
    inventories=set(inventories)
    if len(inventories)>8 or any(len(p)!=3 or any(type(n) is not int for n in p) for p in inventories):raise ValueError('At most eight explicit inventory coordinates')
    path=Path(path)
    if path.stat().st_size>limits['archive_bytes']:raise ValueError('Archive byte bound exceeded')
    archive_bytes=path.read_bytes()
    if len(archive_bytes)>limits['archive_bytes']:raise ValueError('Archive byte bound exceeded')
    source_hash=hashlib.sha256(archive_bytes).hexdigest();tiles=[];journal=None;chunks=0;selected=0;complete=True;reason=None
    with zipfile.ZipFile(io.BytesIO(archive_bytes)) as saved:
        names=saved.namelist()
        if len(names)>10000 or len(set(names))!=len(names) or any(PurePosixPath(n).is_absolute() or '..' in PurePosixPath(n).parts for n in names):raise ValueError('Ambiguous or unbounded archive members')
        roots=[n[:-len('level.dat')] for n in names if n=='level.dat' or n.endswith('/level.dat')]
        if len(roots)!=1:raise ValueError('Exactly one copied world root required')
        prefix=roots[0]+('' if dimension==0 else 'DIM'+str(dimension)+'/')
        journal_name=prefix+'data/foundations_pl4_wireless_uncertainty.dat'
        if journal_name in names:
            info=saved.getinfo(journal_name)
            if info.file_size>limits['chunk_bytes']:raise ValueError('Journal byte bound exceeded')
            journal=nbt(inflate(saved.read(info),1,limits['chunk_bytes']))['data']
        # Prioritize explicit observation coordinates and saved matching record coordinates.
        # Host coordinates remain observed hosts; this never promotes them to original targets.
        preferred=set(inventories)
        for row in journal_records(journal,limits['journal_records'])[0]:
            if row.get('Token')!=token:continue
            for keys in [('X','Y','Z'),('HostX','HostY','HostZ')]:
                if all(type(row.get(k)) is int for k in keys):preferred.add(tuple(row[k] for k in keys))
        preferred_chunks={(x>>4,z>>4) for x,y,z in preferred}
        preferred_regions={(x>>5,z>>5) for x,z in preferred_chunks}
        regions=sorted(n for n in names if n.startswith(prefix+'region/') and '/' not in n[len(prefix+'region/'):] and n.endswith('.mca'))
        def region_coords(name):
            m=re.fullmatch(r'r\.(-?\d+)\.(-?\d+)\.mca',PurePosixPath(name).name)
            if not m:raise ValueError('Unexpected region filename')
            return tuple(map(int,m.groups()))
        regions.sort(key=lambda name:(region_coords(name) not in preferred_regions,name))
        preferred_records=[];other_records=[];available_chunks=0
        for name in regions:
            info=saved.getinfo(name)
            if selected+info.file_size>limits['selected_bytes']:complete=False;reason='selected region byte bound';break
            selected+=info.file_size;region=saved.read(info)
            if len(region)<8192:raise ValueError('Truncated region')
            rx,rz=region_coords(name)
            for slot in range(1024):
                loc=int.from_bytes(region[slot*4:slot*4+4],'big');off=(loc>>8)*4096
                if not off:continue
                available_chunks+=1
                queue=preferred_records if (rx*32+slot%32,rz*32+slot//32) in preferred_chunks else other_records
                if len(queue)<=limits['chunks']:queue.append((region,off))
        if available_chunks>limits['chunks']:complete=False;reason=reason or 'chunk bound'
        for region,off in (preferred_records+other_records)[:limits['chunks']]:
            if off<8192 or off+5>len(region):raise ValueError('Invalid region offset')
            size=int.from_bytes(region[off:off+4],'big')
            if size<1 or off+size+4>len(region):raise ValueError('Invalid region record')
            chunk=nbt(inflate(region[off+5:off+4+size],region[off+4],limits['chunk_bytes']));chunks+=1
            rows=chunk.get('Level',{}).get('TileEntities',[])
            if not isinstance(rows,list):raise ValueError('Malformed saved tiles')
            if len(tiles)+len(rows)>limits['tiles']:complete=False;reason='tile bound';break
            tiles.extend(rows)
    result=observe(tiles,journal,token,owner,dimension,inventories,limits,mc)
    result.update(minecraft_label=mc,minecraft_identity_proven_by_save=False,source_archive_sha256=source_hash,journal_value_sha256=digest_value(journal) if journal is not None else None,selected_dimension_scan_complete=complete and not result['result_limit_reached'],scan_stop_reason=reason,scanned_chunks=chunks,scanned_tiles=len(tiles),selected_region_bytes=selected,limits=limits)
    return result

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('copied_world_zip',type=Path);p.add_argument('--minecraft',required=True,choices=['1.12.2','1.6.4']);p.add_argument('--token',required=True);p.add_argument('--owner',required=True);p.add_argument('--dimension',type=int,default=0);p.add_argument('--inventory',action='append',default=[],help='Explicit saved coordinate x,y,z; at most eight');a=p.parse_args()
    points=[tuple(map(int,v.split(','))) for v in a.inventory]
    print(json.dumps(collect(a.copied_world_zip,a.minecraft,a.token,a.owner,a.dimension,points),indent=2))
