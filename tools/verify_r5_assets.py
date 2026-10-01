"""Offline source-resource validation. This does not bake Minecraft models or render a GUI."""
from pathlib import Path
import json,re,hashlib,sys
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources';ASSETS=RES/'assets/foundations_pl4';JAVA=ROOT/'src/main/java/net/foundations/pl4'
VANILLA={'minecraft:block/block':{},'minecraft:block/cube_all':{'textures':{'particle':'#all'},'required':['#all']},'minecraft:item/generated':{'generated':True},'minecraft:item/handheld':{'generated':True}}
models={'foundations_pl4:'+f.relative_to(ASSETS/'models').as_posix()[:-5]:json.loads(f.read_text()) for f in (ASSETS/'models').rglob('*.json')}
roots=set();sprites=set()
def qualify(value):return value if ':' in value else 'minecraft:'+value
def gather(value):
    if isinstance(value,dict):
        if isinstance(value.get('model'),str):roots.add(qualify(value['model']))
        for v in value.values():gather(v)
    elif isinstance(value,list):
        for v in value:gather(v)
def resolve(name,chain=()):
    assert name not in chain,('parent cycle',name)
    m=models.get(name,VANILLA.get(name));assert m is not None,('missing model',name)
    parent=resolve(qualify(m['parent']),chain+(name,)) if 'parent' in m else {}
    return {**parent,**m,'textures':{**parent.get('textures',{}),**m.get('textures',{})}}
def sprite(value):
    value=qualify(value);namespace,path=value.split(':',1)
    assert namespace=='foundations_pl4' and path.startswith(('block/','item/')),('atlas path',value)
    png=RES/f'assets/{namespace}/textures/{path}.png'
    assert png.is_file() and png.read_bytes()[:8]==b'\x89PNG\r\n\x1a\n',('missing/invalid PNG',value)
    sprites.add(value)
for f in (ASSETS/'blockstates').glob('*.json'):gather(json.loads(f.read_text()))
roots.update(k for k in models if k.startswith('foundations_pl4:item/'))
for name,model in models.items():
    for value in model.get('textures',{}).values():
        if not value.startswith('#'):sprite(value)
    resolve(name)
for name in roots:
    model=resolve(name);refs=set(model.get('required',[]));textures=model['textures']
    if model.get('generated'):
        assert 'layer0' in textures,('missing generated texture',name)
        refs.update('#'+k for k in textures if k.startswith('layer'))
    else:
        assert 'particle' in textures,('missing particle',name)
        refs.add('#particle')
        for element in model.get('elements',[]):
            for face in element.get('faces',{}).values():refs.add(face['texture'])
    for reference in refs:
        seen=set()
        while reference.startswith('#'):
            assert reference not in seen,('texture cycle',name);seen.add(reference)
            reference=textures[reference[1:]]
        sprite(reference)
# R5-specific resources and source-wiring regressions.
assert len(json.loads((ASSETS/'blockstates/large_display_model.json').read_text())['variants'])==192
assert len(list((ASSETS/'blockstates').glob('cable_model_*.json')))==30
# Cable mesh states must keep the DOWN-authored arm aligned with the topology face.
arm_rotations={
    'down':{},
    'up':{'x':180},
    'north':{'x':90,'y':180},
    'south':{'x':90},
    'west':{'x':90,'y':90},
    'east':{'x':90,'y':270},
}
for material in ['data','redstone_off','redstone_on']:
    for connector in ['cable','internal','half']:
        states=json.loads((ASSETS/'blockstates'/f'cable_model_{material}_{connector}.json').read_text())['variants']
        assert len(states)==6,('cable arm direction count',material,connector)
        for face,rotation in arm_rotations.items():
            state=states[f'facing={face}']
            assert {key:value for key,value in state.items() if key in ('x','y')}==rotation,('cable arm rotation',material,connector,face)
    centre=json.loads((ASSETS/'blockstates'/f'cable_model_{material}_centre.json').read_text())['variants']
    assert len(centre)==6 and all('x' not in state and 'y' not in state for state in centre.values()),('cable centre rotation',material)
    for depth in ['1','15','2','3','4','6']:
        states=json.loads((ASSETS/'blockstates'/f'cable_model_{material}_lead_{depth}.json').read_text())['variants']
        assert len(states)==6,('cable lead direction count',material,depth)
        for face,rotation in arm_rotations.items():
            state=states[f'facing={face}']
            assert {key:value for key,value in state.items() if key in ('x','y')}==rotation,('cable lead rotation',material,depth,face)
assert json.loads((ASSETS/'models/block/hammer.json').read_text())['elements']==[]
assert json.loads((RES/'data/foundations_pl4/loot_table/blocks/hammer_air.json').read_text())['pools']==[]
for f in ['gui/hammer.png','block/model/forging_hammer_stone.png']:assert (ASSETS/'textures'/f).is_file()
assert '.dynamicShape()' in (JAVA/'FoundationsPL4.java').read_text()
assert 'InteractionResult.PASS : InteractionResult.TRY_WITH_EMPTY_HAND' in (JAVA/'HostBlock.java').read_text()
renderer=(JAVA/'client/HostRenderer.java').read_text()
assert 'host.connection(d)' in renderer and 'pose.scale(d.getAxis()' not in renderer
assert '.getBlockEntity(' not in renderer,'Renderer must not rediscover topology per frame'
assert 'cachedGroups' in (JAVA/'NetworkEngine.java').read_text()
assert 'DisplayNetworks.sample();' in (JAVA/'NetworkEngine.java').read_text()
assert 'addDataSlots(data)' in (JAVA/'HammerMenu.java').read_text()
assert 'HAMMER_MENU.get(),HammerScreen::new' in (JAVA/'client/PLClient.java').read_text()
assert 'context.bakeLayer(HammerModel.LAYER)' in (JAVA/'client/HammerRenderer.java').read_text()
assert 'root.render(pose,vertices,light,overlay)' in (JAVA/'client/HammerModel.java').read_text()
screen=(JAVA/'client/HammerScreen.java').read_text()
body=screen[screen.index('public void extractRenderState('):]
assert body.count('super.extractRenderState(')==1 and 'extractBackground(' not in body
assert body.index('super.extractRenderState(')<body.index('setTooltipForNextFrame(')
# All JSON syntax, not native codec/schema acceptance.
json_files=list(RES.rglob('*.json'))
for f in json_files:json.loads(f.read_text(encoding='utf-8'))
print(f'PASS offline resource references: {len(models)} models, {len(roots)} roots, {len(sprites)} atlas sprites; {len(json_files)} JSON files parsed.')
print('PASS cable rendering states: all materials, arm kinds, endpoint leads, and six facing rotations.')
print('PASS R5 source wiring guards: dynamic host shapes, click routing, cached cable renderer, menu/model registration, shared-canvas sampling, hammer tooltip order.')
print('Scope: source assets/wiring only. No Minecraft baking, rendering, Java API type-checking or gameplay execution.')
