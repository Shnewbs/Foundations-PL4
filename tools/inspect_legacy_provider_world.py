"""Independently inspect registered restart-fixture providers in a retained native world ZIP."""
import argparse,gzip,hashlib,io,json,struct,zipfile,zlib
from pathlib import Path

PROVIDER_ID = 'foundations_pl4_restarttests:persisted_provider'

def nbt(raw):
    stream=io.BytesIO(raw)
    def read(size):
        value=stream.read(size)
        if len(value)!=size:raise ValueError('Truncated NBT')
        return value
    def number(fmt):return struct.unpack('>'+fmt,read(struct.calcsize(fmt)))[0]
    def text():return read(number('H')).decode('utf-8')
    def length():
        n=number('i')
        if not 0<=n<=1000000:raise ValueError('Unbounded NBT collection')
        return n
    def payload(kind,depth=0):
        if depth>64:raise ValueError('Unbounded NBT depth')
        if kind in {1,2,3,4,5,6}:return number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[kind])
        if kind==7:return read(length())
        if kind==8:return text()
        if kind==9:
            child=number('B');return [payload(child,depth+1) for _ in range(length())]
        if kind==10:
            result={}
            while True:
                child=number('B')
                if child==0:return result
                key=text()
                if key in result:raise ValueError('Duplicate NBT key')
                result[key]=payload(child,depth+1)
        if kind in {11,12}:return [number('i' if kind==11 else 'q') for _ in range(length())]
        raise ValueError('Unsupported NBT type')
    if number('B')!=10:raise ValueError('Expected compound NBT root')
    text();return payload(10)

def inspect(archive,phase_report):
    providers=[]
    with zipfile.ZipFile(archive) as saved:
        for name in saved.namelist():
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
                for tile in nbt(raw).get('Level',{}).get('TileEntities',[]):
                    if tile.get('id')==PROVIDER_ID:
                        providers.append(dict(provider_id=tile['ProviderId'],owner=tile['ProviderOwner'],outcome='extract' if tile['FaultExtract'] else 'insert',
                                              mutation_calls=tile['MutationCalls'],removed_units=tile['RemovedUnits'],accepted_units=tile['AcceptedUnits'],
                                              inventory_units=sum(item['Count'] for item in tile.get('Items',[])),fault_extract=tile['FaultExtract'],fault_insert=tile['FaultInsert'],position=[tile[k] for k in ['x','y','z']]))
    expected=phase_report['provider_outcomes']
    if len(providers)!=2 or len({p['provider_id'] for p in providers})!=2:raise ValueError('Expected two persisted registered providers')
    rows={p['provider_id']:p for p in providers}
    for outcome in expected:
        row=rows.get(outcome['provider_id'])
        if row is None or any(row[k]!=outcome[k] for k in ['owner','outcome','mutation_calls','removed_units','accepted_units']):raise ValueError('Raw provider private state differs from native report')
        extraction=outcome['outcome']=='extract'
        if row['inventory_units']!=(outcome['source_units'] if extraction else outcome['destination_units']) or (row['fault_extract'],row['fault_insert'])!=((1,0) if extraction else (0,1)):raise ValueError('Raw provider inventory/fault mode differs')
    return dict(world_archive_sha256=hashlib.sha256(Path(archive).read_bytes()).hexdigest(),registered_provider_id=PROVIDER_ID,providers=sorted(providers,key=lambda p:p['outcome']),result='PASS')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('world_zip',type=Path);parser.add_argument('phase_json',type=Path);args=parser.parse_args()
    print(json.dumps(inspect(args.world_zip,json.loads(args.phase_json.read_text())),indent=2))
