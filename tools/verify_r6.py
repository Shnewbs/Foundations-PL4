"""R6 source/asset guards; geometric JSON checks are not Minecraft baking or screenshot validation."""
from pathlib import Path
import json,subprocess,tempfile,copy
from r6_display_assets import mesh,DIRS,VECTORS,back_mask
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/foundations_pl4';J=ROOT/'src/main/java/net/foundations/pl4'
checks=0
for kind in ('displayscreen','minidisplay','large_display_model'):
 variants=json.loads((A/'blockstates'/(kind+'.json')).read_text())['variants']
 assert len(variants)==(192 if kind=='large_display_model' else 12)
 for key,value in variants.items():
  properties=dict(t.split('=') for t in key.split(','));mount=DIRS.index(properties['facing']);out=properties['front_outward']=='true';front=mount if out else mount^1
  path=A/'models'/(value['model'].split(':')[1]+'.json');model=json.loads(path.read_text());element=model['elements'][0]
  assert not any(k in value for k in ('x','y','uvlock'))
  frontface=element['faces'][DIRS[front]];backface=element['faces'][DIRS[front^1]]
  if kind=='large_display_model':
   assert frontface['texture']=='#front' and backface['texture']=='#back'
   mask=int(properties['connections']);from r6_display_assets import TEXTURES
   assert model['textures']['back'].endswith(TEXTURES[back_mask(mask,front)])
  else:
   # Original top row is the illuminated front, second row the back, never interchangeable.
   assert frontface['uv'][1]==0 and backface['uv'][1]==6
  # Selected face normal points in the same direction as the font; plane lies beyond selected skin.
  axis=next(i for i,n in enumerate(VECTORS[mount]) if n);sign=VECTORS[mount][axis]
  offset=0.5005625 if out else 0.4370;text=0.5+sign*offset
  skin=(element['to'][axis] if VECTORS[front][axis]>0 else element['from'][axis])/16
  assert (text-skin)*VECTORS[front][axis]>0,(kind,key,text,skin)
  checks+=1
renderer=(J/'client/HostRenderer.java').read_text();assert 'DisplayFacing.frame' in renderer and 'displayOutward' in renderer
assert 'getMainCamera().getPosition()' in renderer and 'frame.normal()' in renderer
assert 'DisplayNetworks.right(p)' in renderer
net=(J/'DisplayNetworks.java').read_text();assert 'boolean outward' in net and 'newRoot.part().applyDisplaySettings(settings,revision)' in net and 'canFlipInto(player,tiles,outward)' in net and 'mayInteract' in net
sampler=(J/'DataSampler.java').read_text();assert 'EnergyReader.sample' in sampler and 'Capabilities.EnergyStorage' not in sampler
energy=(J/'EnergyReader.java').read_text();assert 'level.hasChunkAt(key.pos)' in energy and 'getCapability(probe.capability,pos,side)' in energy
assert 'getCapability(probe.capability,pos,null)' not in energy
assert 'if(result!=null)return result' in energy and 'if(!sides.contains(link.side()))' in energy
for illegal in ('insertEnergy(', 'extractEnergy(', 'changeEnergy(', 'setEnergy(', 'saveWithoutMetadata('):
 assert illegal not in energy and illegal not in (J/'core/ReflectiveEnergyAccess.java').read_text()
assert 'if(r.part.kind!=Kind.ENERGY_READER)r.part.status=' in (J/'NetworkEngine.java').read_text()
assert 'EnergyReader.clear()' in (J/'NetworkEngine.java').read_text()
# Execute sample JS using a recipe-event stub; validate captured JSON against known codec limits.
# This is NOT KubeJS/Rhino execution or a native recipe-reload test.
example=ROOT/'examples/kubejs/server_scripts/pl4_recipes.js.example'
with tempfile.TemporaryDirectory() as tmp:
 harness=Path(tmp)/'example-test.js'
 harness.write_text('const capture={};global.ServerEvents={recipes:f=>f({remove:x=>capture.remove=x,custom:x=>{capture.recipe=x;return{id:id=>capture.id=id}}})};\n'+example.read_text()+'\nconsole.log(JSON.stringify(capture));')
 result=json.loads(subprocess.check_output(['node',str(harness)],text=True))
recipe=result['recipe'];assert result['remove']['id']=='foundations_pl4:forging/stone_plate'
assert recipe['type']=='foundations_pl4:forging_hammer' and recipe['result']['id']=='foundations_pl4:stoneplate'
assert 1<=recipe['input_count']<=64 and 1<=recipe['processing_ticks']<=72000 and 0<=recipe['cooldown_ticks']<=72000
assert 'KubeJSPlugin' not in ''.join(f.read_text() for f in J.rglob('*.java')) # No false custom-event/DSL claim.
print(f'PASS {checks} model front/plane variants, screen basis wiring, read-only/sided energy source guards, and JS example with a stub event.')
print('NOT native rendering, live mod probes, or KubeJS runtime validation.')
