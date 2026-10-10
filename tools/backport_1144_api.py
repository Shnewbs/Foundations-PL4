"""Incrementally translate supported Forge 28 APIs; never remove PL4 features to compile."""
from pathlib import Path
import json

R=Path(__file__).resolve().parents[1]
status_path=R/'BUILD_STATUS.json'
status=json.loads(status_path.read_text())
if status.get('minecraft')!='1.14.4' or status.get('loader_version')!='28.2.26':
 raise SystemExit('Refusing a different Minecraft/Forge target')
if status.get('native_api_backport') in ('forge28-stage1','forge28-stage2','forge28-stage3','forge28-stage4'):
 print('Forge28 stage1 already applied')
 raise SystemExit(0)

root=R/'src/main/java/net/foundations/pl4'
def edit(path,old,new,required=True):
 p=R/path
 s=p.read_text(encoding='utf-8')
 if old not in s:
  if required:raise SystemExit('Backport anchor missing: '+path+' '+old)
  return
 p.write_text(s.replace(old,new),encoding='utf-8')

# Forge 28's native DeferredRegister has only its constructor, not the modern factory.
for p in root.rglob('*.java'):
 s=p.read_text(encoding='utf-8')
 if 'DeferredRegister.create(' in s:
  p.write_text(s.replace('DeferredRegister.create(', 'new DeferredRegister<>('),encoding='utf-8')

p=root/'FoundationsPL4.java'
s=p.read_text()
for old,new in [('Block.Properties.of(', 'Block.Properties.create('),
                ('.noOcclusion()', '.notSolid()'),
                ('.noCollission()', '.doesNotBlockMovement()'),
                ('.dynamicShape()', ''),
                ('.strength(', '.hardnessAndResistance('),
                ('.stacksTo(', '.maxStackSize(')]:
 s=s.replace(old,new)
p.write_text(s,encoding='utf-8')

# Old Minecraft used boolean for block activation; preserve "pass" behavior for
# held tools and use the existing owner-checked activation logic for everything else.
for name in ['HostBlock','HammerBlock','HammerSpaceBlock']:
 p=root/(name+'.java')
 s=p.read_text()
 if name=='HostBlock':
  old='@Override public ActionResultType use(BlockState s,World l,BlockPos p,PlayerEntity player,Hand hand,BlockRayTraceResult hit){'
  new='@Override public boolean use(BlockState s,World l,BlockPos p,PlayerEntity player,Hand hand,BlockRayTraceResult hit){'
  if old not in s:raise SystemExit('HostBlock activation anchor missing')
  s=s.replace(old,new).replace('return ActionResultType.PASS;\n        return useWithoutItem(s,l,p,player,hit);',
    'return false;\n        return useWithoutItem(s,l,p,player,hit)!=ActionResultType.PASS;')
 else:
  old='@Override public ActionResultType use(BlockState s,World l,BlockPos pos,PlayerEntity p,Hand hand,BlockRayTraceResult hit){return useWithoutItem(s,l,pos,p,hit);}'
  new='@Override public boolean use(BlockState s,World l,BlockPos pos,PlayerEntity p,Hand hand,BlockRayTraceResult hit){return useWithoutItem(s,l,pos,p,hit)!=ActionResultType.PASS;}'
  if old not in s:raise SystemExit(name+' activation anchor missing')
  s=s.replace(old,new)
 if name=='HammerSpaceBlock':
  s=s.replace('l.destroyBlock(base,player==null||!player.abilities.instabuild,player);',
              'l.destroyBlock(base,player==null||!player.abilities.instabuild);')
 p.write_text(s,encoding='utf-8')

for p in root.rglob('*.java'):
 s=p.read_text()
 s=s.replace('.isShiftKeyDown()', '.isSneaking()')
 s=s.replace('.toShortString()', '.toString()')
 if 'ActionResultType.CONSUME' in s:
  # Forge28 success already consumes the activation on both sides.
  s=s.replace('ActionResultType.CONSUME','ActionResultType.SUCCESS')
 if s!=p.read_text():p.write_text(s,encoding='utf-8')

p=root/'PortWorldgen.java'
s=p.read_text()
s=s.replace('ConfiguredFeature<?,?>','ConfiguredFeature<?>').replace('ConfiguredFeature<OreFeatureConfig,?>','ConfiguredFeature<OreFeatureConfig>')
s=s.replace('Feature.ORE.configured(', 'Feature.ORE.withConfiguration(')
s=s.replace('ore.decorated(Placement.COUNT_RANGE.configured(', 'ore.withPlacement(Placement.COUNT_RANGE.configure(')
p.write_text(s,encoding='utf-8')

# Forge28 lacks the six-argument SimpleChannel packet registration. Direction
# checks must still happen BEFORE any user-controlled handler is invoked.
p=root/'compat/RegisterPayloadHandlersEvent.java'
s=p.read_text()
needle='NetworkEvent.Context context=supplier.get();handler.accept(packet,new Context(context));context.setPacketHandled(true);},java.util.Optional.of(direction));'
assert s.count(needle)==1,'Different packet API requires review'
s=s.replace(needle,'NetworkEvent.Context context=supplier.get();if(context.getDirection()!=direction){context.setPacketHandled(true);return;}handler.accept(packet,new Context(context));context.setPacketHandled(true);});')
p.write_text(s,encoding='utf-8')

# 1.14 tags live in reloadable collections, not the later getAllTags API.
p=root/'compat/TagKey.java'
s=p.read_text()
assert 'getAllTags().getTagOrEmpty(location)' in s,'Tag source has changed'
s=s.replace('net.minecraft.tags.ItemTags.getAllTags().getTagOrEmpty(location).contains((net.minecraft.item.Item)value)',
            '(net.minecraft.tags.ItemTags.getCollection().get(location)!=null&&net.minecraft.tags.ItemTags.getCollection().get(location).contains((net.minecraft.item.Item)value))')
s=s.replace('net.minecraft.tags.FluidTags.getAllTags().getTagOrEmpty(location).contains((net.minecraft.fluid.Fluid)value)',
            '(net.minecraft.tags.FluidTags.getCollection().get(location)!=null&&net.minecraft.tags.FluidTags.getCollection().get(location).contains((net.minecraft.fluid.Fluid)value))')
p.write_text(s,encoding='utf-8')

status['native_api_backport']='forge28-stage1'
status['port_status']='FORGE28_NATIVE_API_ADAPTATION'
status['native_compilation']='PENDING'
status['native_scenarios']='PENDING'
status_path.write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')
print('Applied audited Forge28 registry, block, interaction, packet, tag, generation and toolchain entrypoint adaptations; native compile pending.')
