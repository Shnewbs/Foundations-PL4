"""Package the source port with the checked official Forge developer toolchain."""
from pathlib import Path
import os,json,re,shutil
W=Path(os.environ['PL4_PORT_WORK']).resolve();R=W/'1.20.1';M=W/'toolchains/1.20.1/mdk';J=R/'src/main/java/net/foundations/pl4'
for n in ['gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties','settings.gradle']:
 p=R/n;p.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(M/n,p)
(R/'build.gradle').write_text('''plugins {
    id 'java-library'
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
}
version = '0.2a-port.1'
group = 'net.foundations.pl4'
base { archivesName = 'FoundationsPL4-1.20.1' }
java { toolchain.languageVersion = JavaLanguageVersion.of(17); withSourcesJar() }
minecraft {
    mappings channel: 'official', version: '1.20.1'
    copyIdeResources = true
    runs {
        configureEach {
            workingDirectory project.file('run')
            property 'forge.logging.console.level', 'info'
            mods { foundations_pl4 { source sourceSets.main } }
        }
        client { }
        server { args '--nogui' }
        gameTestServer { property 'forge.enabledGameTestNamespaces', 'foundations_pl4' }
    }
}
repositories { mavenCentral() }
dependencies { minecraft 'net.minecraftforge:forge:1.20.1-47.4.26' }
tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8'; options.release=17; options.compilerArgs += ['-Xmaxerrs','1000'] }
tasks.withType(AbstractArchiveTask).configureEach { preserveFileTimestamps=false; reproducibleFileOrder=true }
processResources {
    inputs.property 'version',project.version
    filesMatching('META-INF/mods.toml') { expand 'version':project.version }
}
jar {
    from('LICENSE'); from('LICENSE-SonarCore'); from('NOTICE')
    manifest { attributes('Implementation-Version':project.version) }
    finalizedBy 'reobfJar'
}
tasks.register('verifyPortRules',Exec) { commandLine 'python3','tools/run_port_checks.py' }
tasks.named('check') { dependsOn 'verifyPortRules' }
tasks.register('verifyPortJar') {
    dependsOn 'reobfJar'
    doLast {
        def archive=tasks.jar.archiveFile.get().asFile
        new java.util.zip.ZipFile(archive).withCloseable { zip ->
            def metadata=zip.getInputStream(zip.getEntry('META-INF/mods.toml')).getText('UTF-8')
            if (!metadata.contains('version="'+project.version+'"') || !metadata.contains('versionRange="[1.20.1,1.20.2)"')) throw new GradleException('Incorrect Forge target metadata')
            zip.entries().each { entry ->
                if (entry.name.endsWith('.jar')) throw new GradleException('Unexpected bundled mod: '+entry.name)
                if (entry.name.endsWith('.class')) {
                    def content=new String(zip.getInputStream(entry).bytes,java.nio.charset.StandardCharsets.ISO_8859_1)
                    for (invalid in ['sonar/core/','mcmultipart/','net/neoforged/']) if (content.contains(invalid)) throw new GradleException('Foreign-loader dependency in '+entry.name)
                }
            }
        }
        logger.lifecycle('PASS standalone Forge 1.20.1 artifact identity and dependencies')
    }
}
tasks.named('build') { dependsOn 'verifyPortJar' }
''')
meta='''modLoader="javafml"
loaderVersion="[47,)"
license="MIT"
[[mods]]
modId="foundations_pl4"
version="${version}"
displayName="Foundations PL4"
authors="Foundations; original Practical Logistics and Sonar Core by Ollie Lansdell (SonarSonic)"
description="Standalone Practical Logistics. Forge port development build; further API testing is required."
[[dependencies.foundations_pl4]]
modId="forge"
mandatory=true
versionRange="[47.4.26,48)"
ordering="NONE"
side="BOTH"
[[dependencies.foundations_pl4]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1,1.20.2)"
ordering="NONE"
side="BOTH"
'''
(R/'src/main/resources/META-INF/neoforge.mods.toml').unlink()
(R/'src/main/resources/META-INF/mods.toml').write_text(meta)
res=R/'src/main/resources';(res/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':15,'description':'Foundations PL4 Forge 1.20.1 resources'}})+'\n')
for path in sorted((res/'data').rglob('*'),key=lambda p:len(p.parts),reverse=True):
 if path.is_dir() and path.name in {'recipe','loot_table','structure','item','block','neoforge'}:
  if path.name in {'item','block'} and path.parent.name!='tags':continue
  new={'recipe':'recipes','loot_table':'loot_tables','structure':'structures','item':'items','block':'blocks','neoforge':'forge'}[path.name]
  path.rename(path.with_name(new))
for p in (res/'data').rglob('*.json'):
 o=json.loads(p.read_text());t=o.get('type','')
 if t in {'minecraft:crafting_shaped','minecraft:crafting_shapeless'}:
  if isinstance(o.get('result'),dict) and 'id' in o['result']:o['result']['item']=o['result'].pop('id')
 if t in {'minecraft:smelting','minecraft:blasting','minecraft:smoking','minecraft:campfire_cooking'} and isinstance(o.get('result'),dict):o['result']=o['result'].get('id',o['result'].get('item'))
 if t.startswith('neoforge:'):o['type']=t.replace('neoforge:','forge:',1)
 p.write_text(json.dumps(o,indent=2)+'\n')
forge=res/'data/forge/tags/items';forge.mkdir(parents=True,exist_ok=True)
for p in (res/'data/c/tags/items').rglob('*.json'):
 out=forge/p.relative_to(res/'data/c/tags/items');out.parent.mkdir(parents=True,exist_ok=True);out.write_text(p.read_text())
g=res/'assets/foundations_pl4/guide/en_us.json';book=json.loads(g.read_text());old=book['edition'];book['edition']='Minecraft 1.20.1 / Forge / Foundations PL4 0.2a-port.1';g.write_text(json.dumps(book,ensure_ascii=False,indent=2)+'\n')
p=R/'docs/FIELD_GUIDE.md';p.write_text(p.read_text().replace(old,book['edition']))
for p in (R/'tools').glob('*.java'):
 s=p.read_text().replace('Math.clamp(','net.foundations.pl4.compat.PortMath.clamp(').replace('.getFirst()','.get(0)').replace('"21"','"17"').replace('Java 21','Java 17').replace('cells.removeLast()','net.foundations.pl4.compat.PortLists.removeLast(cells)');p.write_text(s)
status=json.loads((R/'BUILD_STATUS.json').read_text());status.update(version='0.2a-port.1',minecraft='1.20.1',java=17,loader='Forge',loader_version='47.4.26',release_channel='alpha',port_status='NATIVE_VALIDATION_REQUIRED',payload_protocol='forge-1.20.1-5',further_api_testing_required=True,client_visual_acceptance='PENDING',installed_optional_provider_acceptance='PENDING',live_multiplayer_acceptance='PENDING')
status['release_notes']='docs/releases/0.2a-port.1.md';(R/'BUILD_STATUS.json').write_text(json.dumps(status,indent=2)+'\n')
(R/'README.md').write_text('''# Foundations PL4 — Minecraft 1.20.1 / Forge

Development port **0.2a-port.1**, Forge **47.4.26**, Java **17**.

Native Forge source port, not a renamed NeoForge JAR. Retains multipart cables, readers, displays, network item storage, transfer escrow, ownership checks and the built-in field guide. Forge SimpleChannel networking and sided capabilities replace NeoForge interfaces. Native NBT preserves item and fluid variants on this Minecraft generation.

Build with `bash gradlew build runGameTestServer`. Java compilation is not client, multiplayer or installed-provider acceptance. **Further API testing is still required.** See BUILD_STATUS.json and docs/releases/0.2a-port.1.md.

Back up worlds; use matching client/server builds. Worlds and JARs are not interchangeable between Minecraft versions. Unavailable third-party power APIs remain disabled rather than using guessed units or conversion ratios.
''')
(R/'BUILD.bat').write_text('@echo off\r\ncall gradlew.bat --no-daemon build\r\nexit /b %errorlevel%\r\n')
(R/'docs/releases/0.2a-port.1.md').write_text('''# Forge 1.20.1 port, 0.2a-port.1

Native Forge networking, sided capabilities and lifecycle invalidation; Java 17 common rules; NBT persistence and component-to-tag variant handling; native recipe JSON/network serialization; pre-1.21 resource paths; item-use routing, rendering and GUI background adapters.

Retains the 191 native behavior fixtures with target-native API calls. Pure-JVM regressions still exercise the production rules. Native test completion is recorded by CI, not inferred from the fixture count. Actual optional-provider, real-client, multiplayer and performance acceptance remain pending. **Further API testing is still required.**

Optional alternatives: PL4's built-in guide, display editor, reader inspection, server configuration and native item/fluid/FE transfer have no external viewer, tooltip or script dependency. The full recipe-browser fallback and foreign-power provider acceptance are not complete; these existing features are not every planned fallback.
''')
shutil.copy2(Path(__file__).with_name('run_port_checks.py'),R/'tools/run_port_checks.py')
print('Packaged native Forge resources and build configuration')
