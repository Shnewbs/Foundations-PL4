"""Forge-port regression gate; original JVM suites execute the production rule classes."""
from pathlib import Path
import ast,json,re,subprocess,sys,tempfile
ROOT=Path(__file__).resolve().parents[1]
def run(*args):
    print('+ '+' '.join(map(str,args)),flush=True)
    subprocess.run(list(map(str,args)),cwd=ROOT,check=True)
def main():
    runner=(ROOT/'tools/run_offline_checks.py').read_text()
    names=re.search(r"classes=\[core/\(n\+'\.java'\) for n in (\[.*?\])\]",runner).group(1)
    core=ROOT/'src/main/java/net/foundations/pl4/core'
    sources=[core/(name+'.java') for name in ast.literal_eval(names)]
    names=re.findall(r"run\('java','-cp',tmp,'([^']+)'\)",runner)
    tests=[ROOT/'tools'/(name+'.java') for name in names]+[ROOT/'tools/VerifyJavaSyntax.java']
    compat=core.parent/'compat'
    with tempfile.TemporaryDirectory(prefix='pl4-forge-rules-') as tmp:
        run('javac','--release','17','-encoding','UTF-8','-d',tmp,*sources,core.parent/'Kind.java',compat/'PortMath.java',compat/'PortLists.java',*tests)
        for name in names:run('java','-cp',tmp,name)
        run('java','-cp',tmp,'VerifyJavaSyntax',ROOT)
    resources=ROOT/'src/main/resources';count=0
    for path in resources.rglob('*.json'):json.loads(path.read_text());count+=1
    status=json.loads((ROOT/'BUILD_STATUS.json').read_text())
    assert status['minecraft']=='1.16.3' and status['java']==8 and status['build_java']==17
    assert (resources/'META-INF/mods.toml').is_file()
    assert not (resources/'META-INF/neoforge.mods.toml').exists()
    assert json.loads((resources/'pack.mcmeta').read_text())['pack']['pack_format']==6
    assert (resources/'data/foundations_pl4/structures/empty.nbt').is_file()
    for path in (resources/'data').rglob('*.json'):
        value=json.loads(path.read_text())
        assert not value.get('type','').startswith('neoforge:'),path
        if value.get('type') in {'minecraft:crafting_shaped','minecraft:crafting_shapeless'}:assert 'item' in value['result'] and 'id' not in value['result'],path
    source=core.parent
    fixtures=ROOT/'src/portTest/java/net/foundations/pl4'
    registered=re.findall(r'e\.register\((\w+)\.class\)',(fixtures/'PLGameTests.java').read_text())
    count_tests=sum((fixtures/(name+'.java')).read_text().count('@GameTest(') for name in registered)
    assert count_tests==193==status['expected_native_scenarios'],count_tests
    packet=(source/'compat/RegisterPayloadHandlersEvent.java').read_text()
    assert 'NetworkDirection.PLAY_TO_SERVER' in packet and 'NetworkDirection.PLAY_TO_CLIENT' in packet
    assert 'version::equals,version::equals' in packet and 'setPacketHandled(true)' in packet
    caps=(source/'compat/PortCapabilities.java').read_text()
    assert 'hasChunkAt' in caps and 'addListener' in caps and 'LazyOptional::invalidate' in caps
    models=resources/'assets/foundations_pl4/models'
    for path in models.rglob('*.json'):
        model=json.loads(path.read_text())
        parent=model.get('parent','')
        if parent.startswith('foundations_pl4:'):assert (models/(parent.split(':',1)[1]+'.json')).is_file(),path
        for texture in model.get('textures',{}).values():
            if texture.startswith('foundations_pl4:'):assert (resources/'assets/foundations_pl4/textures'/(texture.split(':',1)[1]+'.png')).is_file(),(path,texture)
    print(f'PASS Forge resource/identity/lifecycle gates: {count} JSON resources, {count_tests} native fixtures registered (not executed here)')
if __name__=='__main__':
    try:main()
    except (OSError,ValueError,AssertionError,subprocess.CalledProcessError) as error:
        print('PORT CHECK FAILED:',error,file=sys.stderr);sys.exit(1)
