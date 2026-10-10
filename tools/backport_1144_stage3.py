"""Use javap-confirmed Forge 28 blocks/tags and constructor-based 1.14 worldgen."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1];status_path=R/'BUILD_STATUS.json'
status=json.loads(status_path.read_text())
if status.get('minecraft')!='1.14.4' or status.get('native_api_backport') not in ('forge28-stage2','forge28-stage3','forge28-stage4'):
 raise SystemExit('Refusing the wrong Forge28 baseline')
if status['native_api_backport'] in ('forge28-stage3','forge28-stage4'):raise SystemExit(0)
p=R/'src/main/java/net/foundations/pl4/FoundationsPL4.java'
s=p.read_text()
assert '.notSolid()' in s,'Unexpected property shape'
# Exact mapped SDK: Block.Properties has of, noCollission, dynamicShape, strength.
# It does not have notSolid, doesNotBlockMovement or hardnessAndResistance.
s=s.replace('.notSolid()','').replace('.doesNotBlockMovement()','.noCollission()')
s=s.replace('.hardnessAndResistance(','.strength(')
s=s.replace('.strength(.5F,25))','.strength(.5F,25).dynamicShape())')
p.write_text(s,encoding='utf-8')
p=R/'src/main/java/net/foundations/pl4/compat/TagKey.java'
s=p.read_text()
old='(net.minecraft.tags.FluidTags.getCollection().get(location)!=null&&net.minecraft.tags.FluidTags.getCollection().get(location).contains((net.minecraft.fluid.Fluid)value))'
assert old in s
s=s.replace(old,'net.minecraft.tags.FluidTags.getCollection().getTagOrEmpty(location).contains((net.minecraft.fluid.Fluid)value)')
p.write_text(s,encoding='utf-8')
p=R/'src/main/java/net/foundations/pl4/PortWorldgen.java'
s=p.read_text()
old='ore=Feature.ORE.withConfiguration(new OreFeatureConfig(OreFeatureConfig.FillerBlockType.NATURAL_STONE,FoundationsPL4.ORE.get().defaultBlockState(),6));'
new='ore=new ConfiguredFeature<>(Feature.ORE,new OreFeatureConfig(OreFeatureConfig.FillerBlockType.NATURAL_STONE,FoundationsPL4.ORE.get().defaultBlockState(),6));'
assert old in s
s=s.replace(old,new)
old='sapphire=ore.withPlacement(Placement.COUNT_RANGE.configure(new CountRangeConfig(15,1,0,30)));'
new='sapphire=new ConfiguredFeature<>(Feature.DECORATED,new DecoratedFeatureConfig(ore,new ConfiguredPlacement<>(Placement.COUNT_RANGE,new CountRangeConfig(15,1,0,30))));'
assert old in s
s=s.replace(old,new)
p.write_text(s,encoding='utf-8')
status['native_api_backport']='forge28-stage3';status['native_compilation']='PENDING';status_path.write_text(json.dumps(status,indent=2)+'\n')
print('Applied SDK-inspected Forge28 block, tag and constructor-driven ore generation changes; native build validation pending.')
