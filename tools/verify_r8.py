"""Resource/content and source-wiring guards. These do not substitute for native Minecraft tests."""
from pathlib import Path
import json,re,struct
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/foundations_pl4';J=R/'src/main/java/net/foundations/pl4'
b=json.loads((A/'guide/en_us.json').read_text())
assert b['schema']==1 and b['title']=='Foundations PL4 Field Guide'
assert '1.21.1' in b['edition'] and 'NeoForge' in b['edition'] and 'Foundations PL4' in b['edition']
ids=[c['id'] for c in b['chapters']]
assert len(ids)==len(set(ids)) and {'start', 'hammer', 'tutorial_hammer', 'troubleshooting', 'cables', 'compact', 'tutorial_graphics', 'holograms', 'panels', 'tutorial_inventory', 'large', 'tutorial_energy', 'wireless', 'information', 'configuration', 'transfer', 'materials', 'fluids', 'remote', 'guide_controls', 'kubejs', 'redstone', 'status', 'tutorial_hologram', 'inventory', 'expansion', 'energy', 'nodes'}.difference({'expansion'}).union({'tutorial_expansion'}).issubset(set(ids))
categories={};words=0
for c in b['chapters']:
 assert re.fullmatch('[a-z0-9_]{1,64}',c['id']) and len(c['title'])<=100
 assert c['category'] in {'start','network','display','reference'} and 1<=len(c['sections'])<=32
 categories[c['category']]=categories.get(c['category'],0)+1
 assert c['icon'].startswith('foundations_pl4:') and (A/'models/item'/f"{c['icon'].split(':')[1]}.json").is_file(),c['icon']
 for section in c['sections']:
  assert 0<len(section['heading'])<=100 and 0<len(section['body'])<=8192
  assert not re.search(r'[\x00-\x08\x0b\x0c\x0e-\x1f§]',section['body'])
  words+=len(section['body'].split())
assert len(categories)==4
for name,border in [('cover',7),('page',4)]:
 raw=(A/f'textures/gui/sprites/field_guide/{name}.png').read_bytes()
 assert raw[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',raw[16:24])==(256,256)
 meta=json.loads((A/f'textures/gui/sprites/field_guide/{name}.png.mcmeta').read_text())
 assert meta['gui']['scaling']=={'type':'nine_slice','width':256,'height':256,'border':border}
 assert meta['texture']['blur'] is False
assert json.loads((A/'models/item/plguide.json').read_text())['textures']['layer0']=='foundations_pl4:item/field_guide'
assert json.loads((A/'lang/en_us.json').read_text())['item.foundations_pl4.plguide']=='Foundations PL4 Field Guide'
item=(J/'PartItem.java').read_text();net=(J/'DisplayNetworks.java').read_text();rend=(J/'client/HostRenderer.java').read_text();guide=(J/'client/GuideScreen.java').read_text();packets=(J/'PLPackets.java').read_text()
assert 'DisplayPlacement.extension' in item and 'DisplayNetworks.canExtendAt(l,pos,clicked,player)' in item
assert item.index('DisplayNetworks.canEditCanvas')<item.index('l.setBlock')<item.index('host.parts.put')
assert 'placementPart(c,face,outward,extend)' in item and item.index('placementPart(c,face,outward,extend)')<item.index('attachToReader&&!HostBlock.canAdd')
assert 'CanvasContinuity.donor' in net and 'applyDisplaySettings(settings,revision)' in net and 'tile.host().setChanged()' in net
flip=net[net.index('public static boolean flip('):]
assert flip.index('canFlipInto')<flip.index('tile.part().displayOutward=outward')
assert flip.index('var settings=root.part().displaySettings()')<flip.index('NetworkEngine.invalidate')
assert 'HologramProjection.forCamera' in rend and 'HologramProjection.baseYaw' in rend
assert 'HologramProjection.baseYaw' in (J/'MultipartShapes.java').read_text()
assert 'registrar("5")' in packets and 'case "hologram_view"' in packets and 'Shapes.joinIsNotEmpty' in packets
assert 'GuideResources.load' in guide and 'GuideLayout.fit(width,height)' in guide and 'I18n.get(' not in guide
assert 'pose().scale' not in guide and 'scale(' not in guide
for method in ['mouseScrolled','mouseDragged','mouseClicked','mouseReleased','keyPressed','removed','wrapBody','beginDrag','scrollbar']:
 assert method+'(' in guide,method
assert 'g.enableScissor' in guide and 'finally{g.disableScissor();}' in guide
assert 'GuideResources.save(preferences)' in guide and 'icons.computeIfAbsent' in guide
resources=(J/'client/GuideResources.java').read_text()
assert '1_048_577' in resources and 'ATOMIC_MOVE' in resources and '16_384' in resources and 'Files.deleteIfExists(temp)' in resources
print(f'PASS R8 guide: {len(b["chapters"])} chapters, {words} body words, four sections {categories}; book sprites, item and resource limits.')
print('PASS R8 source wiring: preflighted extension/flip, orientation-before-collision, mirrored persisted settings, projector frame/shape, native-scale two-pane book and scroll lifecycle.')
print('Scope: JSON, assets and source wiring only; NOT graphical, native API, Windows or gameplay acceptance.')
