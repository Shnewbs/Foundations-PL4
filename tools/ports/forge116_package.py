"""Finish the native 1.16.5 build identity and isolate its non-shipping scenario harness."""
from pathlib import Path
import os,json,re,shutil
W=Path(os.environ['PL4_PORT_WORK']).resolve();R=W/'1.16.5';J=R/'src/main/java/net/foundations/pl4'
p=R/'build.gradle';s=p.read_text().replace('1.18.2','1.16.5').replace('40.3.12','36.2.42').replace('1.18.3','1.17')
s=s.replace('minecraft {','''sourceSets {
    portTest {
        compileClasspath += sourceSets.main.output + sourceSets.main.compileClasspath
        runtimeClasspath += sourceSets.main.output + sourceSets.main.runtimeClasspath
    }
}
configurations {
    portTestImplementation.extendsFrom implementation
    portTestRuntimeOnly.extendsFrom runtimeOnly
}
minecraft {''',1)
s=re.sub(r"        gameTestServer \{[^\n]+\}\n",'',s)
s=s.replace("server { args '--nogui' }","""server {
            args '--nogui'
            if (project.hasProperty('portScenarios')) {
                workingDirectory project.file('run-scenarios')
                property 'foundations_pl4.portScenarioServer','true'
                mods { foundations_pl4_porttests { source sourceSets.portTest } }
            }
        }""")
s=s.replace("if (entry.name.endsWith('.jar'))", "if (entry.name.contains('compat/scenarios/') || entry.name.endsWith('GameTests.class')) throw new GradleException('Test fixture leaked into runtime: '+entry.name)\n                if (entry.name.endsWith('.jar'))")
s += "\nif (project.hasProperty('portScenarios')) tasks.named('runServer') { dependsOn tasks.named('portTestClasses') }\n"
p.write_text(s)
p=R/'src/main/resources/META-INF/mods.toml';s=p.read_text().replace('1.18.2','1.16.5').replace('1.18.3','1.17').replace('40.3.12','36.2.42').replace('[40,)','[36,)').replace(',41)',',37)');p.write_text(s)
p=R/'src/main/resources/pack.mcmeta';p.write_text(json.dumps({'pack':{'pack_format':6,'description':'Foundations PL4 Forge 1.16.5 resources'}})+'\n')
for relative in ['data/foundations_pl4/worldgen','data/foundations_pl4/forge/biome_modifier']:
 path=R/'src/main/resources'/relative
 if path.exists():shutil.rmtree(path)
(J/'PortWorldgen.java').write_text('''package net.foundations.pl4;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.placement.*;
import net.minecraftforge.event.world.BiomeLoadingEvent;
/** Native world generation; this target cannot consume newer biome-modifier JSON. */
public final class PortWorldgen {
 private static ConfiguredFeature<?,?> sapphire;
 public static void register(){
  sapphire=Registry.register(WorldGenRegistries.CONFIGURED_FEATURE,FoundationsPL4.id("sapphire_ore"),Feature.ORE.configured(new OreFeatureConfig(OreFeatureConfig.FillerBlockType.NATURAL_STONE,FoundationsPL4.ORE.get().defaultBlockState(),6))
    .decorated(Placement.RANGE.configured(new TopSolidRangeConfig(1,0,30))).squared().count(15));
 }
 public static void biome(BiomeLoadingEvent event){
  if(sapphire!=null&&event.getCategory()!=Biome.Category.NETHER&&event.getCategory()!=Biome.Category.THEEND)
    event.getGeneration().getFeatures(GenerationStage.Decoration.UNDERGROUND_ORES).add(()->sapphire);
 }
 private PortWorldgen(){}
}
''')
p=J/'FoundationsPL4.java';s=p.read_text().replace('PLPackets.register(new net.foundations.pl4.compat.RegisterPayloadHandlersEvent());','PLPackets.register(new net.foundations.pl4.compat.RegisterPayloadHandlersEvent());\n            e.enqueueWork(PortWorldgen::register);').replace('MinecraftForge.EVENT_BUS.addListener(NetworkEngine::tick);','MinecraftForge.EVENT_BUS.addListener(NetworkEngine::tick);\n        MinecraftForge.EVENT_BUS.addListener(PortWorldgen::biome);')
s=s.replace('.requiresCorrectToolForDrops()', '.requiresCorrectToolForDrops().harvestTool(net.minecraftforge.common.ToolType.PICKAXE).harvestLevel(2)');p.write_text(s)
p=J/'compat/RegisterPayloadHandlersEvent.java';p.write_text(p.read_text().replace('forge-1.18.2-','forge-1.16.5-'))
p=R/'tools/run_port_checks.py';s=p.read_text().replace("status['minecraft']=='1.18.2'","status['minecraft']=='1.16.5'").replace("['pack_format']==8","['pack_format']==6")
s=s.replace("registered=re.findall", "fixtures=ROOT/'src/portTest/java/net/foundations/pl4'\n    registered=re.findall")
s=s.replace("(source/'PLGameTests.java')","(fixtures/'PLGameTests.java')").replace("(source/(name+'.java'))","(fixtures/(name+'.java'))")
s=s.replace("status['expected_native_tests']","status['expected_native_scenarios']")
s=s.replace("assert 'getFirst'", "assert 'getFirst'")
p.write_text(s)
p=R/'BUILD_STATUS.json';status=json.loads(p.read_text());status.update(minecraft='1.16.5',java=17,loader_version='36.2.42',port_status='NATIVE_SCENARIO_VALIDATION_REQUIRED',payload_protocol='forge-1.16.5-5',expected_native_scenarios=191,test_harness='PL4 isolated dedicated-server scenarios; not modern GameTests');status.pop('expected_native_tests',None);p.write_text(json.dumps(status,indent=2)+'\n')
for name in ['docs/FIELD_GUIDE.md','src/main/resources/assets/foundations_pl4/guide/en_us.json']:
 p=R/name;p.write_text(p.read_text().replace('1.18.2','1.16.5'))
(R/'README.md').write_text('''# Foundations PL4 — Minecraft 1.16.5 / Forge

Native development port **0.2a-port.1**, **Forge 36.2.42**, **Java 17 required**. This PL4 port is not compatible with Java 8.

Retains the cable, reader, storage, transfer/escrow, ownership and display rules; native TileEntity persistence/ticking, sided Forge capabilities, SimpleChannel packets, legacy model rendering and biome-event sapphire generation implement this target's platform behavior.

`bash gradlew build` produces the runtime and sources. The separate `src/portTest` module adapts all 191 existing behavior fixtures to a disposable dedicated server, because Minecraft 1.16.5 lacks modern GameTests. It is never shipped in the runtime JAR. Run only with `bash gradlew runServer -PportScenarios` in an isolated checkout and accept the EULA in `run-scenarios` before using that development harness.

**Further API testing is still required.** Native scenario results, client visuals, installed optional providers, multiplayer and performance acceptance are separate. All planned optional-API fallback features are not yet complete. Back up worlds and use matching client/server versions; worlds and JARs are not interchangeable between Minecraft targets.
''')
(R/'docs/releases/0.2a-port.1.md').write_text('''# Foundations PL4 0.2a-port.1 — Forge 1.16.5

Java 17 required; Forge 36.2.42. This is a native source port, not a renamed newer JAR.

Native tile entity save/load and update packets, server ticking, Forge capability lifecycle, matching-version network channels, old-model rendering, GUI and tag adapters, reload-safe recipe lookup, and native sapphire ore generation preserve the PL4 core behavior.

The runtime excludes the dedicated-server scenario harness. All 191 existing fixtures have target-native equivalents in `src/portTest`; only an actual successful server run proves execution. The automated release gate requires a report with all 191 passed and none failed.

**Further API testing is still required.** Installed optional mods, real client graphics, multiplayer, sustained profiling, and the complete optional-fallback milestone remain pending. No foreign power system is advertised using invented conversion rules.
''')
resources=R/'src/portTest/resources/META-INF';resources.mkdir(parents=True,exist_ok=True)
(resources/'mods.toml').write_text('''modLoader="javafml"
loaderVersion="[36,)"
license="MIT"
[[mods]]
modId="foundations_pl4_porttests"
version="1"
displayName="PL4 isolated native scenarios (development only)"
[[dependencies.foundations_pl4_porttests]]
modId="foundations_pl4"
mandatory=true
versionRange="[0,)"
ordering="AFTER"
side="SERVER"
''')
print('Configured native 1.16.5 build, world generation and isolated scenario module')
