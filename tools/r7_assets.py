"""Reader-with-display wrappers and unscaled endpoint lead meshes for R7.
Original textures/meshes remain untouched. Run after r5_assets.py and r6_display_assets.py.
"""
from pathlib import Path
import json,copy
ROOT=Path(__file__).resolve().parents[1]
ROT={'down':{},'up':{'x':180},'north':{'x':90,'y':180},'south':{'x':90},'west':{'x':90,'y':90},'east':{'x':90,'y':270}}

def generate(root=ROOT):
 a=root/'src/main/resources/assets/foundations_pl4';j=root/'src/main/java/net/foundations/pl4'
 def write(path,value):path.write_text(json.dumps(value,indent=2)+'\n')
 for reader in ['inforeader','inventoryreader','fluidreader','energyreader','networkreader']:
  source=json.loads((a/'models/block'/f'part_{reader}.json').read_text())
  source['parent']='foundations_pl4:block/readerwithdisplay'
  write(a/'models/block'/f'part_{reader}_with_display.json',source)
  write(a/'blockstates'/f'{reader}.json',{'variants':{
   f'facing={f},has_display={str(attached).lower()}':{'model':f'foundations_pl4:block/part_{reader}'+('_with_display' if attached else ''),**rot}
   for attached in [False,True] for f,rot in ROT.items()}})
 # Preserve the cable UV strip's two-pixel cross-section. No stretched centre-cube geometry.
 for material,texture in [('data','cable_types'),('redstone_off','redstone_cable_off'),('redstone_on','redstone_cable_on')]:
  for key,depth in [('1',1),('15',1.5),('2',2),('3',3),('4',4),('6',6)]:
   name=f'cable_model_{material}_lead_{key}'
   faces={f:{'texture':'#0','uv':[4,0,8,min(16,(16-depth))]}for f in ['north','south','east','west']}
   faces.update({f:{'texture':'#0','uv':[11,3,13,5]}for f in ['up','down']})
   write(a/'models/block'/f'{name}.json',{'textures':{'0':f'foundations_pl4:block/model/{texture}','particle':'foundations_pl4:block/data_cable'},'elements':[{'from':[7,depth,7],'to':[9,16,9],'faces':faces}]})
   write(a/'blockstates'/f'{name}.json',{'variants':{f'facing={f}':{'model':f'foundations_pl4:block/{name}',**rot}for f,rot in ROT.items()}})
 # R5 generated raw shape rotation is reused by the paired/lead shape layer.
 p=j/'PartShapes.java';s=p.read_text().replace('private static VoxelShape rotate(', 'static VoxelShape rotate(');p.write_text(s)
 print('R7 assets: five paired-reader wrappers, 60 reader states and 18 endpoint-lead models. Original PNGs unchanged.')
if __name__=='__main__':generate()
