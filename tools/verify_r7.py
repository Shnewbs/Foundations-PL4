"""R7 integration guards and geometry math. Not native Minecraft compilation/model baking."""
from pathlib import Path
import json,itertools
R=Path(__file__).resolve().parents[1];J=R/'src/main/java/net/foundations/pl4';A=R/'src/main/resources/assets/foundations_pl4'
readers=['inforeader','inventoryreader','fluidreader','energyreader','networkreader']
for name in readers:
 states=json.loads((A/'blockstates'/f'{name}.json').read_text())['variants'];assert len(states)==12
 for attached in [False,True]:
  for face in ['down','up','north','south','west','east']:
   state=states[f'facing={face},has_display={str(attached).lower()}'];ref=state['model'].split(':')[1]
   wrapper=json.loads((A/'models'/(ref+'.json')).read_text())
   assert wrapper['parent'].endswith('/readerwithdisplay' if attached else '/reader')
# Base-down boxes are rotated by the same orthogonal mappings as PartShapes. No overlap in any direction.
model=json.loads((A/'models/block/readerwithdisplay.json').read_text())
readerboxes=[x['from']+x['to']for x in model['elements']]
def rotate(b,face):
 vs=[]
 for x,y,z in itertools.product((b[0],b[3]),(b[1],b[4]),(b[2],b[5])):
  vs.append([(x,y,z),(x,16-y,16-z),(16-x,z,y),(x,z,16-y),(y,z,x),(16-y,z,16-x)][face])
 return [min(v[i]for v in vs)for i in range(3)]+[max(v[i]for v in vs)for i in range(3)]
def overlap(a,b):return all(min(a[i+3],b[i+3])-max(a[i],b[i])>1e-9 for i in range(3))
for face in range(6):
 for p in [[0,0,4,16,1,12],[4,0,4,12,1,12],[0,0,0,16,1,16]]:
  assert all(not overlap(rotate(p,face),rotate(b,face))for b in readerboxes)
for material in ['data','redstone_off','redstone_on']:
 for key,depth in [('1',1),('15',1.5),('2',2),('3',3),('4',4),('6',6)]:
  m=json.loads((A/'models/block'/f'cable_model_{material}_lead_{key}.json').read_text())['elements'][0]
  assert m['from']==[7,depth,7] and m['to']==[9,16,9]
host=(J/'HostEntity.java').read_text();packets=(J/'PLPackets.java').read_text();net=(J/'NetworkEngine.java').read_text();renderer=(J/'client/HostRenderer.java').read_text();placement=(J/'PartItem.java').read_text()
assert 'MultipartTopology.SLOT_COUNT' in host and 't.putInt("schema",2)' in host
assert 'MultipartTopology.SLOT_COUNT' in packets and 'event.registrar("4")' in packets  # R9 typed snapshots/edits require matching clients.
assert 'MultipartTopology.plan(nodes)' in net and 'topology.network()' in net
assert 'sets.union' not in net[net.index('for(var export:topology.visual())'):net.index('Map<Part,List<Ref>> visible')]
assert 'DisplayNetworks.rebuild(refs,visible)' in net and 'DisplayNetworks.sample();' in net
assert '.getBlockEntity(' not in renderer and 'readerHasDisplay' in renderer and 'externalLead(part)' in renderer
assert 'attachToReader&&!HostBlock.canAdd(host,candidate)' in placement
assert 'player.isShiftKeyDown()' in (J/'HostBlock.java').read_text() and 'interactionTarget' in host
assert 'level.mayInteract(player,adjacent)' in (J/'ToolItem.java').read_text()
assert 'mayInteract(player,packet.pos)' in packets and '!p.identity.equals(packet.identity)' in packets and 'if(!h.canEdit(player))return;' in packets
assert 'R7GameTests.class' in (J/'PLGameTests.java').read_text()
print('PASS R7 wiring guards: separate-slot persistence/protocol, native planner use, visual edge isolation, paired-reader rendering/placement, covered-reader access and protection checks.')
print('PASS 60 paired-reader model states, three panel sizes across six geometry rotations, and 18 unscaled endpoint-lead meshes. Not native render acceptance.')
