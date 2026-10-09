"""Adapt the entire feature implementation and retain 193 scenarios on Forge 37's APIs."""
from pathlib import Path
import json, re, subprocess, sys
ROOT=Path(__file__).resolve().parents[1]
J=ROOT/'src/main/java/net/foundations/pl4'
T=ROOT/'src/portTest/java/net/foundations/pl4'
status=json.loads((ROOT/'BUILD_STATUS.json').read_text())
if status['minecraft']!='1.17.1':raise SystemExit('Wrong target')
if status.get('dedicated_scenario_port'):raise SystemExit(0)

def edit(rel, old, new):
    p=ROOT/rel;s=p.read_text()
    if old not in s:raise ValueError('Missing anchor: '+rel+' '+old[:60])
    p.write_text(s.replace(old,new))

for p in J.rglob('*.java'):
    s=p.read_text()
    s=s.replace('net.minecraftforge.network.','net.minecraftforge.fmllegacy.network.')
    s=s.replace('net.minecraftforge.registries.RegistryObject','net.minecraftforge.fmllegacy.RegistryObject')
    s=s.replace('net.minecraftforge.server.ServerLifecycleHooks','net.minecraftforge.fmllegacy.server.ServerLifecycleHooks')
    s=s.replace('net.minecraftforge.event.server.ServerStoppedEvent','net.minecraftforge.fmlserverevents.FMLServerStoppedEvent')
    s=re.sub(r'\bServerStoppedEvent\b','FMLServerStoppedEvent',s)
    s=s.replace('com.google.gson.JsonParser.parseString(', 'new com.google.gson.JsonParser().parse(')
    s=s.replace('JsonParser.parseString(', 'new JsonParser().parse(')
    p.write_text(s)

p=J/'FoundationsPL4.java';s=p.read_text().replace('import net.minecraftforge.registries.*;','import net.minecraftforge.registries.*;\nimport net.minecraftforge.fmllegacy.RegistryObject;')
for key,native in [('BLOCK','BLOCKS'),('ITEM','ITEMS'),('BLOCK_ENTITY_TYPE','TILE_ENTITIES'),('MENU','CONTAINERS')]:
    s=s.replace('DeferredRegister.create(Registries.'+key+',ID)','DeferredRegister.create(ForgeRegistries.'+native+',ID)')
s=s.replace('        bus.addListener(PLGameTests::register);\n','')
p.write_text(s)

for name,var in [('HostEntity','t'),('HammerEntity','tag')]:
    p=J/(name+'.java');s=p.read_text().replace('super.saveAdditional('+var+')','super.save('+var+')')
    s=s.replace('return ClientboundBlockEntityDataPacket.create(this);','return new ClientboundBlockEntityDataPacket(worldPosition,0,getUpdateTag());')
    s=s.replace('@Override protected void saveAdditional(CompoundTag tag){saveAdditional(tag,null);}',
        '@Override public CompoundTag save(CompoundTag tag){saveAdditional(tag,null);return tag;}')
    p.write_text(s)
for name in ['HostBlock','HammerSpaceBlock']:
    p=J/(name+'.java');p.write_text(p.read_text().replace('getCloneItemStack(','getPickBlock(').replace('onDestroyedByPlayer(','removedByPlayer('))
edit('src/main/java/net/foundations/pl4/BuiltinInfoProvider.java','furnace.saveWithoutMetadata()','furnace.save(new net.minecraft.nbt.CompoundTag())')
edit('src/main/java/net/foundations/pl4/client/GuideButton.java','isHoveredOrFocused()','(isHovered||isFocused())')
edit('src/main/java/net/foundations/pl4/client/DisplayCanvas.java','.color(color)', '.color((color>>>16)&255,(color>>>8)&255,color&255,(color>>>24)&255)')
edit('src/main/java/net/foundations/pl4/core/ForgingRecipe.java','GsonHelper.getAsJsonObject(json,"result").deepCopy()',
     'new com.google.gson.JsonParser().parse(GsonHelper.getAsJsonObject(json,"result").toString()).getAsJsonObject()')

# Keep the complete behavior suite in a dedicated native-server module.
T.mkdir(parents=True,exist_ok=True)
for p in J.glob('*GameTests.java'):
    s=p.read_text().replace('net.minecraft.gametest.framework.','net.foundations.pl4.compat.scenarios.')
    s=s.replace('net.minecraftforge.event.RegisterGameTestsEvent','net.foundations.pl4.compat.scenarios.RegisterGameTestsEvent')
    s=s.replace('net.minecraftforge.gametest.PrefixGameTestTemplate','net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate')
    s=s.replace('net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,h.getLevel().registryAccess())','com.mojang.serialization.JsonOps.INSTANCE')
    (T/p.name).write_text(s);p.unlink()

native_imports={
    'net.minecraft.world.server.ServerWorld':'net.minecraft.server.level.ServerLevel',
    'net.minecraft.util.math.BlockPos':'net.minecraft.core.BlockPos',
    'net.minecraft.util.math.AxisAlignedBB':'net.minecraft.world.phys.AABB',
    'net.minecraft.block.':'net.minecraft.world.level.block.',
    'net.minecraft.tileentity.TileEntity':'net.minecraft.world.level.block.entity.BlockEntity',
    'net.minecraft.entity.player.PlayerEntity':'net.minecraft.world.entity.player.Player',
    'net.minecraft.entity.Entity':'net.minecraft.world.entity.Entity',
    'net.minecraftforge.fml.event.server.FMLServerStartedEvent':'net.minecraftforge.fmlserverevents.FMLServerStartedEvent'
}
for name in ['BeforeBatch','GameTest','GameTestHelper','PrefixGameTestTemplate','RegisterGameTestsEvent','PortScenarioMod']:
    original='src/portTest/java/net/foundations/pl4/compat/scenarios/'+name+'.java'
    s=subprocess.check_output(['git','show','7c284703f0a4bf9f61c59c1824e193e7064d6108:'+original],cwd=ROOT,text=True)
    for old,new in native_imports.items():s=s.replace(old,new)
    for old,new in [('ServerWorld','ServerLevel'),('AxisAlignedBB','AABB'),('TileEntity','BlockEntity'),('PlayerEntity','Player')]:s=re.sub(r'\b'+old+r'\b',new,s)
    s=s.replace('entity.remove()','entity.discard()').replace('1.16.5','1.17.1').replace('191','193')
    p=T/'compat/scenarios'/(name+'.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
p=ROOT/'src/portTest/resources/META-INF/mods.toml';p.parent.mkdir(parents=True,exist_ok=True)
p.write_text('''modLoader="javafml"
loaderVersion="[37,38)"
license="MIT"
[[mods]]
modId="foundations_pl4_porttests"
version="1"
displayName="PL4 isolated 1.17.1 scenarios (development only)"
[[dependencies.foundations_pl4_porttests]]
modId="foundations_pl4"
mandatory=true
versionRange="[0,)"
ordering="AFTER"
side="SERVER"
''')

build=ROOT/'build.gradle';s=build.read_text()
setup='''sourceSets {
    portTest {
        compileClasspath += sourceSets.main.output + sourceSets.main.compileClasspath
        runtimeClasspath += sourceSets.main.output + sourceSets.main.runtimeClasspath
    }
}
configurations { portTestImplementation.extendsFrom implementation; portTestRuntimeOnly.extendsFrom runtimeOnly }
'''
s=s.replace('minecraft {\n',setup+'minecraft {\n',1)
s=s.replace("server { args '--nogui' }",'''server {
            args '--nogui'
            if(project.hasProperty('portScenarios')) {
                workingDirectory project.file('run-scenarios')
                property 'foundations_pl4.portScenarioServer','true'
                mods { foundations_pl4_porttests { source sourceSets.portTest } }
            }
        }''')
s=re.sub(r"        gameTestServer \{[^\n]+\}\n",'',s)
s=s.replace("['compileJava','compileTestJava']", "['compileJava','compileTestJava','compilePortTestJava']")
s=s.replace("if (entry.name.endsWith('.jar'))", "if(entry.name.endsWith('GameTests.class') || entry.name.contains('compat/scenarios/')) throw new GradleException('Leaked scenario class: '+entry.name)\n                if (entry.name.endsWith('.jar'))")
s+="\nif(project.hasProperty('portScenarios')) tasks.matching { it.name=='runServer' }.configureEach { dependsOn tasks.named('portTestClasses') }\n"
build.write_text(s)
p=ROOT/'tools/run_port_checks.py';s=p.read_text().replace("registered=re.findall(r'e\\.register\\((\\w+)\\.class\\)',(source/'PLGameTests.java').read_text())", "fixtures=ROOT/'src/portTest/java/net/foundations/pl4'\n    registered=re.findall(r'e\\.register\\((\\w+)\\.class\\)',(fixtures/'PLGameTests.java').read_text())")
s=s.replace("(source/(name+'.java')).read_text().count('@GameTest(')","(fixtures/(name+'.java')).read_text().count('@GameTest(')")
p.write_text(s)
count=sum(p.read_text().count('@GameTest(') for p in T.glob('*GameTests.java'))
if count!=193:raise SystemExit('Scenario count drift: '+str(count))
status.update(dedicated_scenario_port=True,expected_native_scenarios=193,test_harness='PL4 isolated native server scenarios; not modern GameTests',port_status='NATIVE_VALIDATION_PENDING')
(ROOT/'BUILD_STATUS.json').write_text(json.dumps(status,indent=2)+'\n')
for rel in ['README.md','docs/releases/0.2a-port.1.md']:
    p=ROOT/rel;s=p.read_text().replace('193-test','193-scenario').replace('native tests','native scenarios').replace('runGameTestServer','runServer -PportScenarios')
    s+='\nThe 193 scenarios execute in a separate isolated dedicated-server harness because this target predates Forge\'s modern GameTest registration hook. The runtime JAR excludes all scenario classes.\n'
    p.write_text(s)
print('Applied exact Forge 37 runtime adapters and preserved all 193 dedicated-server scenarios.')
