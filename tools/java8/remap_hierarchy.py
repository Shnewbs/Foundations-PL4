"""Build-only workaround for Forge 32 official Widget.getHeight mapping collision.
The temporary hierarchy JAR is never installed, shaded, released or written into Gradle's cache.
Only byte-identical duplicate getter implementations are removed from this mirror.
"""
from pathlib import Path
import hashlib,json,os,struct,subprocess,sys,tempfile,zipfile
ROOT=Path(__file__).resolve().parents[2]
ENTRY='net/minecraft/client/gui/widget/Widget.class'
def u2(data,offset):return struct.unpack_from('>H',data,offset)[0]
def u4(data,offset):return struct.unpack_from('>I',data,offset)[0]
def parse(data):
    if data[:4]!=b'\xca\xfe\xba\xbe':raise ValueError('Not a class file')
    pool={};pos=10;i=1
    while i<u2(data,8):
        tag=data[pos];pos+=1
        if tag==1:
            length=u2(data,pos);pos+=2;pool[i]=data[pos:pos+length].decode('utf-8',errors='replace');pos+=length
        elif tag in (3,4,9,10,11,12,17,18):pos+=4
        elif tag in (5,6):pos+=8;i+=1
        elif tag in (7,8,16,19,20):pos+=2
        elif tag==15:pos+=3
        else:raise ValueError('Unknown constant-pool tag '+str(tag))
        i+=1
    pos+=6;pos+=2+2*u2(data,pos)
    def member(start):
        pos=start+8;attrs={}
        for _ in range(u2(data,start+6)):
            name=pool[u2(data,pos)];length=u4(data,pos+2);pos+=6
            if pos+length>len(data):raise ValueError('Truncated member attribute')
            attrs[name]=data[pos:pos+length];pos+=length
        return pos,attrs
    fields=u2(data,pos);pos+=2
    for _ in range(fields):pos,_=member(pos)
    count_at=pos;count=u2(data,pos);pos+=2;methods=[]
    for _ in range(count):
        start=pos;pos,attrs=member(pos)
        methods.append((start,pos,u2(data,start),pool[u2(data,start+2)],pool[u2(data,start+4)],attrs))
    return count_at,methods,pos

def code_identity(attrs):
    code=attrs.get('Code')
    if code is None:raise ValueError('Getter implementation missing')
    end=8+u4(code,4)
    end+=2+8*u2(code,end)
    return code[:end]

def normalize(data):
    offset,methods,end=parse(data);seen={};keep=[];removed=0
    for method in methods:
        start,stop,access,name,desc,attrs=method;key=(name,desc)
        if key in seen:
            old=seen[key]
            if key!=('getHeight','()I') or access!=old[2] or code_identity(attrs)!=code_identity(old[5]):
                raise ValueError('Unexpected or conflicting duplicate method '+str(key))
            removed+=1
        else:seen[key]=method;keep.append(data[start:stop])
    return data[:offset]+struct.pack('>H',len(keep))+b''.join(keep)+data[end:],removed

def selftest():
    checks=0
    with tempfile.TemporaryDirectory(prefix='pl4-hierarchy-test-') as name:
        root=Path(name);source=root/'Sample.java'
        source.write_text('public class Sample { public int getHeight(){return 1;} public int getWidth(){return 2;} }')
        subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/javac'),'--release','8',str(source)],check=True)
        raw=(root/'Sample.class').read_bytes();offset,methods,end=parse(raw)
        getter=next(m for m in methods if m[3]=='getHeight');other=next(m for m in methods if m[3]=='getWidth')
        def duplicate(method):return raw[:offset]+struct.pack('>H',len(methods)+1)+raw[offset+2:end]+raw[method[0]:method[1]]+raw[end:]
        assert normalize(raw)==(raw,0);checks+=1
        assert normalize(duplicate(getter))==(raw,1);checks+=1
        try:normalize(duplicate(other))
        except ValueError:checks+=1
        else:raise AssertionError('Unreviewed duplicate accepted')
        for bad in [b'',b'not-a-class',raw[:15]]:
            try:normalize(bad)
            except (ValueError,IndexError,struct.error):checks+=1
            else:raise AssertionError('Truncated class accepted')
    print('PASS',checks,'build-only hierarchy parser checks')
    return checks

def prepare():
    checks=selftest();out=ROOT/'build/java8';out.mkdir(parents=True,exist_ok=True)
    cp=(out/'original-hierarchy-classpath.txt').read_text().split(os.pathsep)
    jars=[Path(p) for p in cp if p.endswith('mapped_official_1.16.1.jar')]
    if len(jars)!=1:raise ValueError('Expected one exact 1.16.1 mapped dependency')
    source=jars[0];destination=out/'native-hierarchy.jar'
    with zipfile.ZipFile(source) as z:
        raw=z.read(ENTRY);clean,removed=normalize(raw)
        if removed!=1:raise ValueError('Expected the single known Forge 32 getter collision')
        with zipfile.ZipFile(destination,'w',zipfile.ZIP_DEFLATED) as mirror:
            for item in z.infolist():mirror.writestr(item,clean if item.filename==ENTRY else z.read(item.filename))
    with zipfile.ZipFile(source) as before,zipfile.ZipFile(destination) as after:
        assert before.namelist()==after.namelist()
        for name in before.namelist():
            if name!=ENTRY and before.read(name)!=after.read(name):raise ValueError('Unrelated hierarchy class changed')
    report={'minecraft':'1.16.1','scope':'Build-only remapper/compatibility-tool hierarchy; never installed or distributed','input_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'mirror_sha256':hashlib.sha256(destination.read_bytes()).hexdigest(),'class':ENTRY,'removed_identical_getters':removed,'parser_tests_passed':checks,'official_cache_changed':False,'installed_runtime_changed':False}
    (out/'hierarchy-check.json').write_text(json.dumps(report,indent=2)+'\n')
    print('PASS isolated build hierarchy collision fix; cache and installed runtime unchanged')
if __name__=='__main__':
    if '--selftest' in sys.argv:selftest()
    else:prepare()
