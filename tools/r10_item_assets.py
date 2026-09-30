"""Item-only, centred presentations for the 23 PL4 parts.
Never changes block model geometry, collision, blockstates, textures or world scale.
Uses the pinned mesh/UVs, bakes only a centring translation into an ITEM child and
supplies every 1.21.1 display context explicitly (no legacy unit-scale fallbacks).
Run after r5_assets/r6_display_assets/r7_assets when regenerating resources.
"""
from pathlib import Path
import copy, json, math, re, sys
CONTEXTS=('gui','ground','fixed','head','firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand')
ROOT=Path(__file__).resolve().parents[1]

def rotate(point,axis,angle,origin):
    p=[point[i]-origin[i] for i in range(3)]
    a=math.radians(angle);c,s=math.cos(a),math.sin(a)
    if axis=='x':p=[p[0],p[1]*c-p[2]*s,p[1]*s+p[2]*c]
    elif axis=='y':p=[p[0]*c+p[2]*s,p[1],-p[0]*s+p[2]*c]
    elif axis=='z':p=[p[0]*c-p[1]*s,p[0]*s+p[1]*c,p[2]]
    return [p[i]+origin[i] for i in range(3)]

def corners(elements):
    out=[]
    for e in elements:
        for x in (e['from'][0],e['to'][0]):
            for y in (e['from'][1],e['to'][1]):
                for z in (e['from'][2],e['to'][2]):
                    p=[x,y,z]
                    if 'rotation' in e:
                        r=e['rotation'];p=rotate(p,r['axis'],r['angle'],r['origin'])
                        if r.get('rescale'):
                            for i,axis in enumerate('xyz'):
                                if axis!=r['axis']:p[i]=r['origin'][i]+(p[i]-r['origin'][i])/math.cos(math.radians(r['angle']))
                    out.append(p)
    return out

def resolve(models,identifier,seen=None):
    seen=set() if seen is None else seen
    if identifier in seen:raise ValueError('Cyclic model '+identifier)
    seen.add(identifier)
    if ':' not in identifier or identifier.startswith('minecraft:'):return {}
    ns,path=identifier.split(':',1)
    if ns!='foundations_pl4':raise ValueError('Unexpected parent '+identifier)
    value=json.loads((models/(path+'.json')).read_text())
    base=resolve(models,value['parent'],seen) if 'parent' in value else {}
    result=copy.deepcopy(base)
    for k,v in value.items():
        if k=='textures':result[k]={**result.get(k,{}),**v}
        elif k!='parent':result[k]=copy.deepcopy(v)
    return result

def kind_names(root):
    source=(root/'src/main/java/net/foundations/pl4/Kind.java').read_text()
    return re.findall(r'\("([a-z]+)"\)',source)

def context_rotation(name,profile,legacy):
    if profile=='panel':
        if name in ('gui','fixed'):return [-90,0,0]
        if name=='ground':return [180,0,0]
        if name=='head':return [-90,0,0]
        if name.startswith('firstperson'):return [-80,0,-15]
        return [-75,0,0]
    if name=='gui':return legacy.get('gui',{}).get('rotation',[30,225,0])
    if name=='fixed':return [-30,180,0] if profile=='projector' else [0,180,0]
    if name in ('ground','head'):return [0,0,0]
    if name.startswith('thirdperson'):return [75,45,0]
    return [0,45,0]

def generate(root=ROOT):
    models=root/'src/main/resources/assets/foundations_pl4/models';manifest={}
    for name in kind_names(root):
        model=resolve(models,'foundations_pl4:block/part_'+name)
        elements=copy.deepcopy(model['elements']);pts=corners(elements)
        lo=[min(p[i] for p in pts) for i in range(3)];hi=[max(p[i] for p in pts) for i in range(3)]
        centre=[(lo[i]+hi[i])/2 for i in range(3)];shift=[8-centre[i] for i in range(3)]
        for e in elements:
            for key in ('from','to'):
                e[key]=[round(e[key][i]+shift[i],7) for i in range(3)]
            if 'rotation' in e:e['rotation']['origin']=[round(e['rotation']['origin'][i]+shift[i],7) for i in range(3)]
        for p in corners(elements):
            if any(not math.isfinite(c) or c < -16 or c>32 for c in p):raise ValueError('Item geometry out of bounds: '+name)
        profile='panel' if name in ('displayscreen','largedisplayscreen','minidisplay') else ('projector' if 'holographic' in name else 'component')
        display={};detail={}
        for ctx in CONTEXTS:
            rot=context_rotation(ctx,profile,model.get('display',{}))
            pts=corners(elements)
            for axis,angle in reversed(tuple(zip('xyz',rot))):pts=[rotate(p,axis,angle,[8,8,8]) for p in pts]
            span=max(max(p[i] for p in pts)-min(p[i] for p in pts) for i in range(3))
            # Envelope sizes in model units: distinct from scale factors on small parts.
            target=13 if ctx=='gui' else 8 if ctx=='fixed' else 5.6 if ctx=='head' else 4 if ctx=='ground' else 5.2 if ctx.startswith('firstperson') else 5.0
            scale=round(min(3.0,target/max(span,.001)),6)
            transform={'rotation':rot,'translation':[0,2 if ctx=='ground' else 1.5 if ctx.startswith('thirdperson') else 0,0], 'scale':[scale]*3}
            display[ctx]=transform;detail[ctx]={'maximum_extent_model_units':round(span*scale,6),'target_model_units':target}
        textures=model.get('textures',{})
        item={'parent':'minecraft:block/block','gui_light':'front' if profile=='panel' else 'side','ambientocclusion':False,'textures':textures,'elements':elements,'display':display}
        if 'render_type' in model:item['render_type']=model['render_type']
        path=models/'item'/f'{name}.json';path.write_text(json.dumps(item,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
        manifest[name]={'profile':profile,'source':'foundations_pl4:block/part_'+name,'centring_translation':shift,'contexts':detail}
    path=root/'docs/R10_ITEM_PRESENTATIONS.json';path.write_text(json.dumps(manifest,indent=2)+'\n')
    return manifest

if __name__=='__main__':
    result=generate(Path(sys.argv[1]).resolve() if len(sys.argv)>1 else ROOT)
    print(f'Generated {len(result)} centred ITEM models, eight explicit contexts each. Placed models untouched.')
