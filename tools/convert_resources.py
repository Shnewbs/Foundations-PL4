"""Convert pinned PL2 assets/recipes to the Minecraft 1.21.1 resource format.
Usage: python tools/convert_resources.py /path/to/Practical-Logistics-2
"""
from pathlib import Path
import json,re,shutil,sys
root=Path(__file__).resolve().parents[1]
up=Path(sys.argv[1]) if len(sys.argv)>1 else root.parent/'upstream/pl2'
src=up/'src/main/resources/assets/practicallogistics2'
res=root/'src/main/resources'; assets=res/'assets/foundations_pl4'; data=res/'data/foundations_pl4'
def write(path,obj):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,indent=2,ensure_ascii=False)+'\n')
def modern_texture(value):
 return value.replace('foundations_pl4:model/','foundations_pl4:block/model/').replace('foundations_pl4:blocks/','foundations_pl4:block/').replace('foundations_pl4:items/','foundations_pl4:item/')
def lower(obj):
 if isinstance(obj,str): return modern_texture(obj.lower().replace('practicallogistics2:', 'foundations_pl4:')) if ':' in obj else obj
 if isinstance(obj,list):return [lower(x) for x in obj]
 if isinstance(obj,dict):return {k:lower(v) for k,v in obj.items()}
 return obj
# 1.21.1 stitches only block/ and item/ by default. Legacy folders are not discovered.
for old,new in [('blocks','block'),('items','item'),('model','block/model'),('gui','gui'),('logos','logos')]:
 shutil.copytree(src/'textures'/old,assets/'textures'/new,dirs_exist_ok=True)
 legacy=assets/'textures'/old
 if old in ('blocks','items','model') and legacy.exists(): shutil.rmtree(legacy)
for f in (src/'models/block').glob('*.json'):
 d=lower(json.loads(f.read_text()))
 if 'parent' in d:
  parent=d['parent'];d['parent']=parent.replace('foundations_pl4:','foundations_pl4:block/') if ':block/' not in parent else parent
 write(assets/'models/block'/f.name.lower(),d)
kindtext=(root/'src/main/java/net/foundations/pl4/Kind.java').read_text()
kinds=re.findall(r'\("([a-z]+)"\)',kindtext)
rot={'down':{},'up':{'x':180},'north':{'x':90,'y':180},'south':{'x':90},'west':{'x':90,'y':90},'east':{'x':90,'y':270}}
for k in kinds:
 old=lower(json.loads((src/'blockstates'/f'{k}.json').read_text()))
 base=old['defaults']; parent=base['model'].replace('foundations_pl4:','foundations_pl4:block/')
 model={'parent':parent,'textures':base.get('textures',{})}
 if k=='transfernode':model['textures']['0']='foundations_pl4:block/model/transfer_node_passive'
 write(assets/'models/block'/f'part_{k}.json',model)
 write(assets/'blockstates'/f'{k}.json',{'variants':{f'facing={face}':{'model':f'foundations_pl4:block/part_{k}',**({} if k.endswith('cable') else r)} for face,r in rot.items()}})
 write(assets/'models/item'/f'{k}.json',{'parent':f'foundations_pl4:block/part_{k}'})
write(assets/'blockstates/multipart_host.json',{'variants':{'':{'model':'foundations_pl4:block/multipart_host'}}})
write(assets/'models/block/multipart_host.json',{'textures':{'particle':'foundations_pl4:block/data_cable'},'elements':[]})
write(assets/'models/block/sapphireore.json',{'parent':'minecraft:block/cube_all','textures':{'all':'foundations_pl4:block/sapphire_ore'}})
write(assets/'blockstates/sapphireore.json',{'variants':{'':{'model':'foundations_pl4:block/sapphireore'}}})
write(assets/'models/item/sapphireore.json',{'parent':'foundations_pl4:block/sapphireore'})
# R5 uses the baked Java hammer model. r5_assets.generate below installs its empty model carriers.
write(assets/'models/item/hammer.json',{'parent':'minecraft:item/generated','textures':{'layer0':'foundations_pl4:item/forging_hammer_item'}})
itemtex={'sapphire':'sapphire','sapphiredust':'sapphire_dust','stoneplate':'stone_plate','etchedplate':'etched_plate','signallingplate':'signalling_plate','wirelessplate':'wireless_plate','transceiver':'transceiver','entitytransceiver':'entitytransceiver','wirelessstorage':'wireless_monitor','operator':'operator','plguide':'guide'}
for item,texture in itemtex.items():write(assets/'models/item'/f'{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'foundations_pl4:item/'+texture}})
for lang in (src/'lang').glob('*.lang'):
 d={}
 for line in lang.read_text().splitlines():
  if '=' not in line or line.lstrip().startswith('#'):continue
  key,val=line.split('=',1);d[key]=val
  if key.startswith('tile.') and key.endswith('.name'):d['block.foundations_pl4.'+key[5:-5].lower()]=val
  if key.startswith('item.') and key.endswith('.name'):d['item.foundations_pl4.'+key[5:-5].lower()]=val
 for k in kinds:d.setdefault('block.foundations_pl4.'+k,{'datacable':'Data Cable','redstonecable':'Redstone Cable','inforeader':'Info Reader','inventoryreader':'Inventory Reader','fluidreader':'Fluid Reader','energyreader':'Energy Reader','networkreader':'Network Reader'}.get(k,k))
 write(assets/'lang'/f'{lang.stem.lower()}.json',d)
# Parse the original explicit SonarCrafting calls; every original crafting recipe is converted.
text=(up/'src/main/java/sonar/logistics/PL2Crafting.java').read_text()
idmap={'sapphire_ore':'sapphireore','stone_plate':'stoneplate','etched_plate':'etchedplate','sapphire_dust':'sapphiredust','signalling_plate':'signallingplate','wireless_plate':'wirelessplate','data_cable':'datacable','redstone_cable':'redstonecable','info_reader':'inforeader','inventory_reader':'inventoryreader','fluid_reader':'fluidreader','energy_reader':'energyreader','network_reader':'networkreader','large_display_screen':'largedisplayscreen','display_screen':'displayscreen','mini_display':'minidisplay','holographic_display':'holographicdisplay','advanced_holographic_display':'advancedholographicdisplay','redstone_signaller':'redstonesignaller','data_emitter':'dataemitter','data_receiver':'datareceiver','redstone_emitter':'redstoneemitter','redstone_receiver':'redstonereceiver','entity_node':'entitynode','transfer_node':'transfernode','redstone_node':'redstonenode','entity_transceiver':'entitytransceiver','wireless_storage_reader':'wirelessstorage','guide':'plguide'}
tagmap={'gemSapphire':'c:gems/sapphire','dustSapphire':'c:dusts/sapphire','dustRedstone':'c:dusts/redstone','gemDiamond':'c:gems/diamond','ingotIron':'c:ingots/iron','chestWood':'c:chests/wooden','logWood':'minecraft:logs','stickWood':'c:rods/wooden','stone':'c:stones','slabWood':'minecraft:wooden_slabs','enderpearl':'c:ender_pearls'}
def ident(token):
 a,b=token.split('.')
 return ('foundations_pl4:'+idmap.get(b,b)) if a.startswith('PL2') else 'minecraft:'+b.lower()
def ingredient(token):
 if token.startswith('"'):return {'tag':tagmap[token[1:-1]]}
 return {'item':ident(token)}
recipes=0
for line in text.splitlines():
 m=re.search(r'SonarCrafting.add(Shaped|Shapeless)Ore\(PL2Constants.MODID, new ItemStack\(([^,]+), (\d+)\), (.*)\);',line)
 if not m:continue
 typ,out,count,args=m.groups();tokens=re.findall(r'"[^"\n]*"|\x27.\x27|[\w]+\.[\w]+',args)
 output=ident(out);d={'type':'minecraft:crafting_'+typ.lower(),'category':'misc','result':{'id':output,'count':int(count)}}
 if typ=='Shapeless':d['ingredients']=[ingredient(t) for t in tokens]
 else:
  patterns=[]
  while tokens and tokens[0].startswith('"'):patterns.append(tokens.pop(0)[1:-1])
  while patterns and not patterns[0].strip():patterns.pop(0)
  while patterns and not patterns[-1].strip():patterns.pop()
  d['pattern']=patterns;d['key']={tokens[i][1:-1]:ingredient(tokens[i+1]) for i in range(0,len(tokens),2)}
 write(data/'recipe'/f'{output.split(":")[1]}.json',d);recipes+=1
# Tags permit datapack/KubeJS extensions without a hard dependency.
for tag,item in [('gems/sapphire','sapphire'),('dusts/sapphire','sapphiredust'),('ores/sapphire','sapphireore')]:write(res/'data/c/tags/item'/f'{tag}.json',{'replace':False,'values':['foundations_pl4:'+item]})
for tag,values in [('stones',['minecraft:stone']),('chests/wooden',['minecraft:chest','minecraft:trapped_chest'])]:write(res/'data/c/tags/item'/f'{tag}.json',{'replace':False,'values':values})
for name,values in [('mineable/pickaxe',['sapphireore']),('needs_iron_tool',['sapphireore']),('mineable/axe',['hammer'])]:write(res/'data/minecraft/tags/block'/f'{name}.json',{'replace':False,'values':['foundations_pl4:'+i for i in values]})
write(data/'loot_table/blocks/hammer.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'foundations_pl4:hammer'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(data/'loot_table/blocks/sapphireore.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[{'type':'minecraft:item','name':'foundations_pl4:sapphireore','conditions':[{'condition':'minecraft:match_tool','predicate':{'predicates':{'minecraft:enchantments':[{'enchantments':'minecraft:silk_touch','levels':{'min':1}}]}}}]},{'type':'minecraft:item','name':'foundations_pl4:sapphire','functions':[{'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:ore_drops'},{'function':'minecraft:explosion_decay'}]}]}]}]})
write(data/'worldgen/configured_feature/sapphire_ore.json',{'type':'minecraft:ore','config':{'size':6,'discard_chance_on_air_exposure':0,'targets':[{'target':{'predicate_type':'minecraft:tag_match','tag':'minecraft:stone_ore_replaceables'},'state':{'Name':'foundations_pl4:sapphireore'}}]}})
write(data/'worldgen/placed_feature/sapphire_ore.json',{'feature':'foundations_pl4:sapphire_ore','placement':[{'type':'minecraft:count','count':15},{'type':'minecraft:in_square'},{'type':'minecraft:height_range','height':{'type':'minecraft:uniform','min_inclusive':{'absolute':1},'max_inclusive':{'absolute':29}}},{'type':'minecraft:biome'}]})
write(data/'neoforge/biome_modifier/sapphire_ore.json',{'type':'neoforge:add_features','biomes':'#minecraft:is_overworld','features':'foundations_pl4:sapphire_ore','step':'underground_ores'})
print(f'Converted {len(kinds)} component models, {recipes} original recipes, {len(itemtex)} items, four language files, ore generation and loot.')

# Internal single-JAR replacement for the original Sonar Core hammer recipe helper.
for name,tag,result,count in [('sapphire_dust','c:gems/sapphire','sapphiredust',1),('sapphire_ore_dust','c:ores/sapphire','sapphiredust',2),('stone_plate','c:stones','stoneplate',4),('etched_plate','c:gems/diamond','etchedplate',4),('signalling_plate','c:dusts/redstone','signallingplate',4),('wireless_plate','c:ender_pearls','wirelessplate',4)]:
 write(data/'recipe/forging'/f'{name}.json',{'type':'foundations_pl4:forging_hammer','ingredient':{'tag':tag},'input_count':1,'result':{'id':'foundations_pl4:'+result,'count':count},'processing_ticks':100,'cooldown_ticks':200})

# Always restore the R5 wrappers/hammer registrations after importing the pinned upstream assets.
from r5_assets import generate
generate(root)
# R6 supersedes the legacy display-facing wrappers with explicit front/back meshes.
from r6_display_assets import generate as generate_r6_displays
generate_r6_displays(root)

# R7 restores separate paired-reader skins and exposed endpoint leads.
from r7_assets import generate as generate_r7
generate_r7(root)

# R8 applies the Foundations Field Guide item and book sprites after legacy imports.
import runpy
runpy.run_path(str(root / "tools/r8_guide_assets.py"), run_name="__main__")

# R10: explicit item-only centring/transforms must run last, never on block models.
from r10_item_assets import generate as generate_r10_items
generate_r10_items(root)
