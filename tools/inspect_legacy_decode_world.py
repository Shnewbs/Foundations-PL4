"""Read retained native region and saved-journal NBT independently of fixture JSON."""
import gzip,hashlib,zipfile,zlib
from pathlib import Path
from inspect_legacy_provider_world import nbt

def validate(tiles,journal,phase,mc,suite):
    owner='fae1b877-c6b4-4538-a26a-630190b54205' if mc=='1.12.2' else 'pl4owner'
    at={(t['x'],t['y'],t['z']):t for t in tiles}
    if (80,200,32) in at:raise ValueError('Removed host still saved')
    live=at[(64,200,32)];chest=at[(85,200,32)]
    items=[i for i in chest.get('Items',[]) if i.get('Slot')==0]
    if len(items)!=1 or items[0].get('Count')!=1:raise ValueError('Missing unique sealed receipt')
    if mc=='1.12.2' and items[0].get('id')!='foundations_pl4:legacy_item_cell':raise ValueError('Wrong saved receipt item')
    rows=phase['decode_outcomes'];tokens=[]
    compounds={'decode-compound','decode-owner','decode-dimension','decode-scope','decode-rewrite','decode-live'}
    if suite not in compounds|{'decode-raw','decode-missing'}:raise ValueError('Unknown decode world suite')
    def rejected(x,token):
        result=dict(Token=71 if suite=='decode-compound' else token,Owner=owner,Dimension=0,Scope='legacy-unknown',HostX=x,HostY=200,HostZ=32,RejectedPayload=dict(Bytes=bytes([4,5,6])))
        if suite=='decode-owner':result['Owner']=71
        if suite=='decode-dimension':result['Dimension']='0'
        if suite=='decode-scope':result['Scope']=dict(Marker=71,Bytes=bytes([7,8,9]))
        if suite in {'decode-rewrite','decode-live'}:result.update(Owner='rewritten-owner',Dimension=7,HostX=x+128)
        return result
    for x,tag,row in [(64,live,rows[0]),(80,items[0]['tag'],rows[1])]:
        evidence=tag['ItemLegacyEvidence'];token=evidence['OriginalToken'];tokens.append(token)
        expected=dict(Fault='Original decode fixture uncertainty',ObservedOwner=owner,ObservedDimension=0,HostX=x,HostY=200,HostZ=32,OriginalToken=token,MalformedContext='first-raw-payload',OriginalPayload=dict(Sentinel=12345,Bytes=bytes([1,2,3])))
        if evidence!=expected or token!=row['original_token']:raise ValueError('Raw original evidence or token changed')
        if type(tag.get('ItemEscrowCount')) is not int or tag['ItemEscrowCount']!=8 or not tag.get('ItemTransferFault'):raise ValueError('Raw uncertainty/claim missing')
        if suite in compounds:
            if tag.get('ItemJournalContext')!=rejected(x,token):raise ValueError('Rejected compound changed')
        elif suite=='decode-missing':
            if 'ItemJournalContext' in tag:raise ValueError('Missing context was synthesized')
        elif x==64:
            if type(tag.get('ItemJournalContext')) is not int or tag['ItemJournalContext']!=71:raise ValueError('Raw integer context changed')
        elif type(tag.get('ItemJournalContext')) is not str or tag['ItemJournalContext']!='current-raw-context':raise ValueError('Raw string context changed')
    opaque=journal['Opaque'] if mc=='1.6.4' else journal
    if opaque.get('ExistingLegacyEvidence')!=live['ItemLegacyEvidence']:raise ValueError('Opaque original evidence changed')
    if suite in compounds:
        if opaque.get('RejectedContext')!=rejected(64,tokens[0]):raise ValueError('Opaque compound rejection changed')
    elif suite=='decode-missing':
        if opaque.get('MissingContext')!=1 or 'RejectedContext' in opaque:raise ValueError('Raw missing-context barrier changed')
    elif type(opaque.get('RejectedContext')) is not int or opaque['RejectedContext']!=71:raise ValueError('Raw rejection barrier changed')
    prior=opaque['PreviousJournal'];entries=prior['Entries']
    if prior.get('Schema')!=2 or len(entries)!=2 or sorted(r['Token'] for r in entries)!=sorted(tokens):raise ValueError('Raw prior journal tokens changed')
    if any(r.get('Owner')!=owner or r.get('Scope')!='legacy-unknown' or 'X' in r or 'Phase' in r for r in entries):raise ValueError('Raw prior route was invented')
    if suite == 'decode-live':
        if live.get('Owner' if mc=='1.12.2' else 'OwnerName') != 'live-owner' or (64,198,32) in at:
            raise ValueError('Live owner rewrite or disposable alias removal changed')
        for x in [64,80]:
            stock=at[(x,199,32)];items=stock.get('Items',[])
            if len(items)!=1 or items[0].get('Slot')!=0 or type(items[0].get('Count')) is not int or items[0]['Count']!=32:
                raise ValueError('Physical native source inventory changed')
            if items[0].get('id') != ('minecraft:iron_ingot' if mc=='1.12.2' else 265):
                raise ValueError('Physical native source item changed')
    return dict(result='PASS',suite=suite,retained_host=[64,200,32],removed_host=[80,200,32],receipt_chest=[85,200,32],original_tokens=tokens,sealed_claims=[8,8],prior_journal_entries=2,original_evidence_and_raw_context='UNCHANGED',barrier='OPAQUE_DIMENSION_WIDE')

def inspect(archive,phase,mc,suite):
    tiles=[];journal=None
    with zipfile.ZipFile(archive) as saved:
        for name in saved.namelist():
            if name.endswith('/data/foundations_pl4_wireless_uncertainty.dat'):
                if journal is not None:raise ValueError('Ambiguous journal')
                journal=nbt(gzip.decompress(saved.read(name)))['data']
            if '/region/' not in name or not name.endswith('.mca'):continue
            region=saved.read(name)
            if len(region)<8192:raise ValueError('Truncated region')
            for slot in range(1024):
                location=int.from_bytes(region[slot*4:slot*4+4],'big');offset=(location>>8)*4096
                if not offset:continue
                if offset<8192 or offset+5>len(region):raise ValueError('Invalid region offset')
                size=int.from_bytes(region[offset:offset+4],'big');kind=region[offset+4]
                if size<1 or offset+4+size>len(region):raise ValueError('Invalid region record')
                compressed=region[offset+5:offset+4+size]
                raw=gzip.decompress(compressed) if kind==1 else zlib.decompress(compressed) if kind==2 else None
                if raw is None:raise ValueError('Unsupported region compression')
                tiles.extend(nbt(raw).get('Level',{}).get('TileEntities',[]))
    if journal is None:raise ValueError('No saved uncertainty journal')
    result=validate(tiles,journal,phase,mc,suite);result['world_archive_sha256']=hashlib.sha256(Path(archive).read_bytes()).hexdigest();return result
