"""Regenerate R5 model wrappers, joined-screen variants and model-matched collision shapes.
Uses the retained PL2 JSON meshes, not placeholder cubes. Does not download anything.
Run after the upstream resource converter, or directly with `python tools/r5_assets.py`.
"""
from pathlib import Path
import json, re, itertools, math, copy

ROOT=Path(__file__).resolve().parents[1]

def generate(root: Path = ROOT):
    java=root/'src/main/java/net/foundations/pl4'
    assets=root/'src/main/resources/assets/foundations_pl4'
    a=assets
    rotations={'down':{},'up':{'x':180},'north':{'x':90,'y':180},'south':{'x':90},'west':{'x':90,'y':90},'east':{'x':90,'y':270}}
    for material,texture in [('data','cable_types'),('redstone_off','redstone_cable_off'),('redstone_on','redstone_cable_on')]:
     for typ,parent in [('cable','cableconnector'),('internal','cableconnector_internal'),('half','cableconnector_half'),('centre','cablecentre')]:
      name=f'cable_model_{material}_{typ}'
      (assets/'models/block'/f'{name}.json').write_text(json.dumps({'parent':f'foundations_pl4:block/{parent}','textures':{'0':f'foundations_pl4:block/model/{texture}','particle':'foundations_pl4:block/data_cable'}},indent=2)+'\n')
      (assets/'blockstates'/f'{name}.json').write_text(json.dumps({'variants':{f'facing={face}':{'model':f'foundations_pl4:block/{name}',**(rot if typ!='centre' else {})}for face,rot in rotations.items()}},indent=2)+'\n')
    # Exact model box extents (including antenna spans), precomputed once for all six directions.
    kindids=re.findall(r'\b([A-Z_]+)\("([a-z]+)"\)',(java/'Kind.java').read_text())
    models={f.stem:json.loads(f.read_text())for f in (assets/'models/block').glob('*.json')}
    def resolve(name):
     m=models[name]
     return m.get('elements',resolve(m['parent'].split('/')[-1]) if m.get('parent','').startswith('foundations_pl4:') else [])
    lines=['package net.foundations.pl4;', '', 'import java.util.EnumMap;', 'import net.minecraft.core.Direction;', 'import net.minecraft.world.level.block.Block;', 'import net.minecraft.world.phys.shapes.*;', 'import net.foundations.pl4.core.ConnectionRules;', '', '/** Static model-matched part shapes, generated from the retained PL2 JSON elements. */', 'public final class PartShapes {', '    private static final EnumMap<Kind,VoxelShape[]> SHAPES = new EnumMap<>(Kind.class);', '    public static final VoxelShape CENTRE = Block.box(6,6,6,10,10,10);', '    private static final VoxelShape[][] ARMS = new VoxelShape[4][6];', '    static {']
    for name,id in kindids:
     if 'CABLE' in name:
      lines.append(f'        add(Kind.{name}, new double[][]{{{{6,6,6,10,10,10}}}});');continue
     boxes=[]
     for e in resolve('part_'+id):
      coords=list(itertools.product(*zip(e['from'],e['to'])))
      if 'rotation' in e:
       r=e['rotation'];axis='xyz'.index(r['axis']);a=math.radians(r['angle']);origin=r['origin'];tmp=[]
       for v in coords:
        w=[v[i]-origin[i] for i in range(3)];i,j=[k for k in range(3)if k!=axis];u=w[i];vv=w[j];w[i]=u*math.cos(a)-vv*math.sin(a);w[j]=u*math.sin(a)+vv*math.cos(a);tmp.append([w[k]+origin[k]for k in range(3)])
       coords=tmp
      b=[max(0,min(v[i]for v in coords))for i in range(3)]+[min(16,max(v[i]for v in coords))for i in range(3)]
      if all(b[i+3]>b[i]for i in range(3)): boxes.append(b)
     fmt=lambda n: ('%.6f'%n).rstrip('0').rstrip('.') if n else '0'
     lines.append('        add(Kind.'+name+', new double[][]{'+','.join('{'+','.join(map(fmt,b))+'}' for b in boxes)+'});')
    lines+=['''        for (int type=1; type<=3; type++) for (Direction face:Direction.values())
                ARMS[type][face.ordinal()]=rotate(new double[]{7,ConnectionRules.armStart(type),7,9,6,9},face);
        }
        private PartShapes() {}
        private static void add(Kind kind,double[][] boxes) {
            VoxelShape[] directions=new VoxelShape[6];
            for(Direction face:Direction.values()) {
                VoxelShape shape=Shapes.empty();
                for(double[] box:boxes) shape=Shapes.or(shape,rotate(box,face));
                directions[face.ordinal()]=shape.optimize();
            }
            SHAPES.put(kind,directions);
        }
        public static VoxelShape part(Part part) { return SHAPES.get(part.kind)[part.face.ordinal()]; }
        public static VoxelShape arm(int type,Direction face) { return type<1||type>3?Shapes.empty():ARMS[type][face.ordinal()]; }
        private static VoxelShape rotate(double[] b,Direction face) {
            double[] lo={16,16,16},hi={0,0,0};
            for(int a=0;a<8;a++) {
                double x=b[(a&1)==0?0:3],y=b[(a&2)==0?1:4],z=b[(a&4)==0?2:5];
                double[] v=switch(face) {
                    case DOWN->new double[]{x,y,z}; case UP->new double[]{x,16-y,16-z};
                    case NORTH->new double[]{16-x,z,y}; case SOUTH->new double[]{x,z,16-y};
                    case WEST->new double[]{y,z,x}; case EAST->new double[]{16-y,z,16-x};
                };
                for(int i=0;i<3;i++){lo[i]=Math.min(lo[i],v[i]);hi[i]=Math.max(hi[i],v[i]);}
            }
            return Block.box(lo[0],lo[1],lo[2],hi[0],hi[1],hi[2]);
        }
    }''']
    (java/'PartShapes.java').write_text('\n'.join(lines)+'\n')
    textures=['none','one_w','one_e','opposite_2','one_n','two_w','two_n','three_s','one_s','two_s','two_e','three_n','opposite_1','three_e','three_w','all']
    base=json.loads((a/'models/block/largedisplayscreen.json').read_text())
    for mask,tex in enumerate(textures):
     for plane in ('flat','wall'):
      model=copy.deepcopy(base);model.pop('display',None)
      model['textures']['front']=f'foundations_pl4:block/model/large_screen/large_display_{tex}'
      model['textures']['back']=f'foundations_pl4:block/model/large_screen_back/large_display_{tex}'
      model['textures']['particle']='foundations_pl4:block/data_cable'
      faces=model['elements'][0]['faces'];faces['up']['texture']='#front';faces['down']['texture']='#back'
      if plane=='wall': faces['up']['rotation']=180;faces['down']['rotation']=180
      (a/f'models/block/large_joined_{mask}_{plane}.json').write_text(json.dumps(model,indent=2)+'\n')
    rotations={'down':{},'up':{'x':180},'north':{'x':90,'y':180},'south':{'x':90},'west':{'x':90,'y':90},'east':{'x':90,'y':270}}
    variants={}
    for direction,rot in rotations.items():
     for mask in range(16):variants[f'connections={mask},facing={direction}']={'model':f'foundations_pl4:block/large_joined_{mask}_{"flat" if direction in ("down","up") else "wall"}',**rot}
    (a/'blockstates/large_display_model.json').write_text(json.dumps({'variants':variants},indent=2)+'\n')
    def write(path, data):
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    for name in ('hammer','hammer_air'):
        write(assets/f'models/block/{name}.json',{'textures':{'particle':'foundations_pl4:block/hammer_break'},'elements':[]})
    write(assets/'blockstates/hammer.json',{'variants':{f'facing={d}':{'model':'foundations_pl4:block/hammer'} for d in ('north','east','south','west')}})
    write(assets/'blockstates/hammer_air.json',{'variants':{f'offset={i}':{'model':'foundations_pl4:block/hammer_air'} for i in (1,2)}})
    write(root/'src/main/resources/data/foundations_pl4/loot_table/blocks/hammer_air.json',{'type':'minecraft:block','pools':[]})
    tag=root/'src/main/resources/data/minecraft/tags/block/mineable/axe.json'
    axe=json.loads(tag.read_text(encoding='utf-8'));value='foundations_pl4:hammer_air'
    if value not in axe['values']:axe['values'].append(value)
    write(tag,axe)
    language=assets/'lang/en_us.json'
    data=json.loads(language.read_text(encoding='utf-8'))
    data.update({f'gui.foundations_pl4.hammer.{k}':v for k,v in {
        'idle':'Insert a forging ingredient','working':'Forging','cooldown':'Hammer returning',
        'output_full':'Output blocked','headroom':'Needs two clear blocks above','no_recipe':'No matching forging recipe'}.items()})
    write(language,data)
    print('Regenerated R5 cable meshes, 96 joined-display variants, hammer model carriers, and 23-kind collision shapes.')

if __name__=='__main__':generate()
