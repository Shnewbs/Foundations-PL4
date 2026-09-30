"""Generate explicit six-face/two-front display meshes. No runtime JSON rotation ambiguity.
Original PNGs and source item models are left intact. New front coordinates match DisplayFacing.
Run AFTER tools/r5_assets.py when regenerating resources from upstream.
"""
from pathlib import Path
import json,copy,itertools
ROOT=Path(__file__).resolve().parents[1]
DIRS=('down','up','north','south','west','east')
VECTORS=((0,-1,0),(0,1,0),(0,0,-1),(0,0,1),(-1,0,0),(1,0,0))
TEXTURES=['none','one_w','one_e','opposite_2','one_n','two_w','two_n','three_s','one_s','two_s','two_e','three_n','opposite_1','three_e','three_w','all']
def transform(v,mount):
 x,y,z=v
 return ((x,y,z),(x,16-y,16-z),(16-x,z,y),(x,z,16-y),(y,z,x),(16-y,z,16-x))[mount]
def normal(face,mount):
 p=transform((8,8,8),mount);q=transform(tuple(8+n for n in VECTORS[face]),mount)
 return DIRS[VECTORS.index(tuple(q[i]-p[i] for i in range(3)))]
def back_mask(mask,front):
 # Back-view left/right exchange for walls; floor/ceiling exchange top/bottom.
 return ((mask&12)|((mask&1)<<1)|((mask&2)>>1)) if front>=2 else ((mask&3)|((mask&4)<<1)|((mask&8)>>1))
def mesh(base,mount,outward,mask=None):
 model=copy.deepcopy(base);model.pop('display',None)
 model['textures']['particle']='foundations_pl4:block/data_cable'
 front=mount if outward else mount^1
 for element in model['elements']:
  vertices=[transform(v,mount) for v in itertools.product(*zip(element['from'],element['to']))]
  element['from']=[min(v[i] for v in vertices) for i in range(3)];element['to']=[max(v[i] for v in vertices) for i in range(3)]
  original=element['faces'];faces={normal(DIRS.index(direction),mount):copy.deepcopy(data) for direction,data in original.items()}
  # The original display front is the model's DOWN face. Only the chosen front gets it.
  frontface=copy.deepcopy(original['down']);backface=copy.deepcopy(original['up'])
  frontface.pop('rotation',None);backface.pop('rotation',None)
  if mask is not None:
   model['textures']['front']=f'foundations_pl4:block/model/large_screen/large_display_{TEXTURES[mask]}'
   model['textures']['back']=f'foundations_pl4:block/model/large_screen_back/large_display_{TEXTURES[back_mask(mask,front)]}'
   frontface['texture']='#front';backface['texture']='#back'
  faces[DIRS[front]]=frontface;faces[DIRS[front^1]]=backface;element['faces']=faces
 return model

def generate(root=ROOT):
 assets=root/'src/main/resources/assets/foundations_pl4';models=assets/'models/block'
 def save(path,data):path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
 for kind in ('displayscreen','minidisplay','large_display_model'):
  base=json.loads((models/('largedisplayscreen.json' if kind=='large_display_model' else kind+'.json')).read_text())
  variants={}
  for mount,direction in enumerate(DIRS):
   for outward in (False,True):
    for mask in range(16) if kind=='large_display_model' else (None,):
     name=f'r6_{kind}_{direction}_{"out" if outward else "in"}'+(f'_{mask}' if mask is not None else '')
     save(models/(name+'.json'),mesh(base,mount,outward,mask))
     key=f'facing={direction},front_outward={str(outward).lower()}'+(f',connections={mask}' if mask is not None else '')
     variants[key]={'model':'foundations_pl4:block/'+name}
  save(assets/'blockstates'/(kind+'.json'),{'variants':variants})
 print('Generated 216 explicit front/back model variants (normal, mini, joined large); original textures unchanged.')
if __name__=='__main__':generate()
