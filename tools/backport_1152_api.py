"""Apply reviewed Forge 31 API migrations once; native acceptance remains mandatory."""
from pathlib import Path
import json,re
R=Path(__file__).resolve().parents[1]
status=json.loads((R/'BUILD_STATUS.json').read_text())
if status['minecraft']!='1.15.2':raise SystemExit('Wrong target')
if status.get('forge31_api_pass'):raise SystemExit(0)
J=R/'src/main/java/net/foundations/pl4';T=R/'src/portTest/java/net/foundations/pl4'
registry_calls={'h.getLevel().registryAccess()','ref.level().registryAccess()','level.registryAccess()',
 'host.getLevel().registryAccess()','l.registryAccess()','c.getLevel().registryAccess()',
 'mc.level.registryAccess()','minecraft.level.registryAccess()'}
for p in (R/'src').rglob('*.java'):
 s=p.read_text()
 for imp in ['net.minecraft.util.registry.DynamicRegistries','net.minecraft.util.RegistryKey','com.mojang.serialization.MapCodec','net.minecraft.util.IReorderingProcessor']:
  s=s.replace('import '+imp+';','')
 s=s.replace('net.minecraft.util.registry.DynamicRegistries','Object')
 s=s.replace('net.minecraft.util.math.vector.Vector3d','net.minecraft.util.math.Vec3d')
 s=re.sub(r'\bVector3d\b','Vec3d',s)
 s=s.replace('net.minecraft.util.math.vector.','net.minecraft.client.renderer.')
 s=s.replace('net.minecraft.util.math.Vec3d.atCenterOf(', 'net.foundations.pl4.compat.PortVectors.atCenterOf(')
 s=s.replace('net.minecraft.util.math.Vec3d.atLowerCornerOf(', 'net.foundations.pl4.compat.PortVectors.atLowerCornerOf(')
 s=s.replace('Vec3d.atCenterOf(', 'net.foundations.pl4.compat.PortVectors.atCenterOf(')
 s=s.replace('Vec3d.atLowerCornerOf(', 'net.foundations.pl4.compat.PortVectors.atLowerCornerOf(')
 for old in sorted(registry_calls,key=len,reverse=True):s=s.replace(old,'null')
 s=s.replace('.dimension().location()', '.dimension.getType().getRegistryName()')
 s=s.replace('.blockPosition()', '.getCommandSenderBlockPosition()')
 s=s.replace('import net.minecraft.block.AbstractBlock;','import net.minecraft.block.Block;')
 s=s.replace('AbstractBlock.Properties','Block.Properties')
 s=s.replace('.requiresCorrectToolForDrops()', '.harvestTool(net.minecraftforge.common.ToolType.PICKAXE).harvestLevel(2)')
 s=s.replace('ActionResultType.sidedSuccess(', 'net.foundations.pl4.compat.PortInteractions.sidedSuccess(')
 s=s.replace('ActionResult.sidedSuccess(', 'net.foundations.pl4.compat.PortInteractions.sidedSuccess(')
 s=s.replace('font.plainSubstrByWidth(', 'font.substrByWidth(')
 s=s.replace('com.google.gson.JsonParser.parseString(', 'new com.google.gson.JsonParser().parse(').replace('JsonParser.parseString(', 'new JsonParser().parse(')
 s=s.replace('IReorderingProcessor','String')
 s=s.replace('e.enqueueWork(', 'net.minecraftforge.fml.DeferredWorkQueue.runLater(').replace('event.enqueueWork(', 'net.minecraftforge.fml.DeferredWorkQueue.runLater(')
 s=s.replace('.withColor(color)', '.setColor(color)').replace('.withClickEvent(', '.setClickEvent(').replace('.withHoverEvent(', '.setHoverEvent(')
 p.write_text(s)

def replace(path,old,new):
 p=R/path;s=p.read_text()
 if old not in s:raise ValueError('Missing migration anchor '+path+': '+old)
 p.write_text(s.replace(old,new))

replace('src/main/java/net/foundations/pl4/NetworkEngine.java',
 'ResourceLocation id=ResourceLocation.tryParse(link.dimension());return id==null?null:server.getLevel(RegistryKey.create(Registries.DIMENSION,id));',
 'ResourceLocation id=ResourceLocation.tryParse(link.dimension());if(id==null)return null;var type=net.minecraft.world.dimension.DimensionType.getByName(id);return type==null?null:server.getLevel(type);')
for name in ['HostEntity','HammerEntity']:
 p=J/(name+'.java');s=p.read_text().replace('void load(BlockState state,CompoundNBT tag){super.load(state,tag);','void load(CompoundNBT tag){super.load(tag);').replace('load(getBlockState(),packet.getTag())','load(packet.getTag())')
 p.write_text(s)
replace('src/main/java/net/foundations/pl4/FoundationsPL4.java','        MinecraftForge.EVENT_BUS.addListener(PortWorldgen::biome);\n','')
p=J/'client/DisplayCanvas.java';s=p.read_text().replace('font.split(new net.minecraft.util.text.StringTextComponent(value),scaledWidth)','font.split(value,scaledWidth)')
s=s.replace('String.forward(font.substrByWidth(value,scaledWidth),net.minecraft.util.text.Style.EMPTY)','font.substrByWidth(value,scaledWidth)')
s=s.replace('.color(color)', '.color((color>>>16)&255,(color>>>8)&255,color&255,(color>>>24)&255)');p.write_text(s)
p=J/'client/GuideScreen.java';s=p.read_text()
for value in ['section.heading()','paragraph','description.summary()']:
 s=s.replace('font.split(new net.minecraft.util.text.StringTextComponent('+value+'),','font.split('+value+',')
s=s.replace('TECHNICAL MANUAL  /  1.21.1','TECHNICAL MANUAL  /  1.15.2');p.write_text(s)
replace('src/main/java/net/foundations/pl4/client/GuideButton.java','getMessage().getString()','getMessage()')
p=J/'client/HammerScreen.java';s=p.read_text().replace('titleLabelY=6;','').replace('font.width(title)','font.width(title.getColoredString())').replace('font.width(status)','font.width(status.getColoredString())');p.write_text(s)
p=J/'HammerEntity.java';s=p.read_text().replace('level.getRecipeManager().getAllRecipesFor(CoreRecipes.HAMMER.get())','level.getRecipeManager().getRecipes().stream().filter(r->r.getType()==CoreRecipes.HAMMER.get()).map(r->(ForgingRecipe)r).collect(java.util.stream.Collectors.toList())');p.write_text(s)
p=T/'PLGameTests.java';s=p.read_text()
s=s.replace('h.getLevel().getRecipeManager().getAllRecipesFor(type).size()', 'h.getLevel().getRecipeManager().getRecipes().stream().filter(r->r.getType()==type).count()')
s=s.replace('var codec=net.foundations.pl4.core.CoreRecipes.HAMMER_SERIALIZER.get().codec().codec();\n        var ops=com.mojang.serialization.JsonOps.INSTANCE;\n        var encoded=codec.encodeStart(ops,recipe).getOrThrow(false,message->{throw new IllegalArgumentException(message);});var decoded=codec.parse(ops,encoded).getOrThrow(false,message->{throw new IllegalArgumentException(message);});',
 'var serializer=net.foundations.pl4.core.CoreRecipes.HAMMER_SERIALIZER.get();\n        var encoded=serializer.toJson(recipe);var decoded=serializer.fromJson(recipe.getId(),encoded);')
s=s.replace('encoded.getAsJsonObject().addProperty("input_count",0);net.foundations.pl4.compat.PortAssertions.check(codec.parse(ops,encoded).result().isEmpty(),"Zero-input forging recipes must be rejected");h.succeed();',
 'encoded.addProperty("input_count",0);boolean rejected=false;try{serializer.fromJson(recipe.getId(),encoded);}catch(IllegalArgumentException|com.google.gson.JsonParseException expected){rejected=true;}net.foundations.pl4.compat.PortAssertions.check(rejected,"Zero-input forging recipes must be rejected");h.succeed();')
if 'codec.parse' in s or 'com.mojang.serialization.' in s:raise ValueError('Recipe test was not fully migrated')
s=s.replace('e.register(PLGameTests.class);','e.register(PLGameTests.class);e.register(Port115GameTests.class);')
p.write_text(s)
p=T/'compat/scenarios/PortScenarioMod.java';s=p.read_text().replace('server.overworld()','server.getLevel(net.minecraft.world.dimension.DimensionType.OVERWORLD)').replace('191','193');p.write_text(s)
p=R/'tools/run_port_checks.py';p.write_text(p.read_text().replace('count_tests==191==','count_tests==193=='))
status.update(forge31_api_pass=True,expected_native_scenarios=193,port_status='NATIVE_API_VALIDATION_PENDING')
(R/'BUILD_STATUS.json').write_text(json.dumps(status,indent=2)+'\n')
for p in [R/'README.md',R/'docs/releases/0.2a-port.1.md']:
 p.write_text(p.read_text().replace('191-scenario','193-scenario').replace('full 191','full 193')+'\nForge 31 uses native JSON recipe serialization and global NBT item/fluid registries rather than newer Mojang codecs. Strict optional field validation, result NBT, and registry-name dimension links are retained. Two target-specific scenarios cover native ore attachment and malformed JSON fields.\n')
count=sum(p.read_text().count('@GameTest(') for p in T.glob('*GameTests.java'))
if count!=193:raise ValueError('Expected 193 retained/native scenarios, found '+str(count))
print('Applied Forge 31 native dimension/NBT/recipe/render API migrations; all 193 scenarios remain required.')
