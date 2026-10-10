"""Prepare explicit full-feature 1.16 patch ports from verified 1.16.4 source.
Only source/metadata are adapted here; native and installed scenarios gate release.
"""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
TARGETS={'1.16.1':('32.0.108',5),'1.16.2':('33.0.61',6),'1.16.3':('34.1.42',6)}
def prepare(root):
    request=json.loads((root/'PORT_TARGET.json').read_text());target=request['minecraft']
    if target not in TARGETS:raise ValueError('Unreviewed target')
    forge,pack=TARGETS[target];major=int(forge.split('.')[0]);status_path=root/'BUILD_STATUS.json'
    status=json.loads(status_path.read_text())
    if status['minecraft']==target:
        if status['loader_version']!=forge:raise ValueError('Target loader drift')
        return
    if status['minecraft']!='1.16.4':raise ValueError('Expected reviewed 1.16.4 base')
    changes={};paths=[root/'build.gradle',root/'docs/FIELD_GUIDE.md',root/'tools/run_port_checks.py']
    for directory in ['src/main','src/portTest','tools/java8']:
        paths.extend(p for p in (root/directory).rglob('*') if p.suffix in {'.java','.py','.gradle','.json','.toml','.mcmeta'})
    for path in paths:
        before=path.read_text(encoding='utf-8');after=before.replace('1.16.4',target).replace('35.1.37',forge)
        if path.suffix=='.toml':
            after=after.replace('[35,)',f'[{major},{major+1})').replace('[35,36)',f'[{major},{major+1})').replace(f'[{forge},36)',f'[{forge},{major+1})')
        if path.name=='run_port_checks.py':after=after.replace("['pack_format']==6","['pack_format']=="+str(pack))
        if path.name=='pack.mcmeta':
            data=json.loads(after);data['pack']['pack_format']=pack;after=json.dumps(data,indent=2)+'\n'
        if before!=after:changes[path]=after
    clean={k:status[k] for k in ['project','version','release_channel','host_save_schema','loader','further_api_testing_required','full_pl2_parity','acceptance_matrix','release_notes']}
    clean.update(minecraft=target,loader_version=forge,java=8,build_java=17,payload_protocol='forge-'+target+'-5',source_baseline=request['source_baseline'],port_status='NATIVE_ACCEPTANCE_PENDING',expected_native_scenarios=193,installed_runtime_acceptance='PENDING',installed_optional_provider_acceptance='PENDING',client_visual_acceptance='PENDING',live_multiplayer_acceptance='PENDING',runtime_profile='Java 8; explicit ModLauncher 8.1.3 precedence; no runtime agent',stock_forge_runtime_compatible=False,release_gate=['production rule suites','native compile and reobfuscation','Java 8 archive isolation','193 installed native scenarios','production-only server boot','exact source and checksums'])
    changes[status_path]=json.dumps(clean,indent=2)+'\n'
    notes=f'''# Foundations PL4 {target} 0.2a-port.1

Full-feature source port derived from the verified 1.16.4 track, not a renamed JAR or a transport-only preview.

Includes multipart cable/reader/display behavior, hologram and display editing, physical-network Wireless Storage with search/sorting and transfers, sided item/fluid/FE routing with saved buffers, ownership, configuration, recipes and the built-in field guide. Foreign energy telemetry is not universal energy conversion.

## Exact runtime

Minecraft **{target}**, Forge **{forge}**, **Java 8**. Java 17 is the build JDK only. Build-time conversion supplies required relocated Java compatibility code; no runtime Java agent is needed. JvmDowngrader source and license notices accompany the release.

This old Forge track uses an explicit **ModLauncher 8.1.3 launch profile**. This is NOT a stock-launcher compatibility claim. Install the exact Forge server, then run the supplied `forge35_profile.py --server SERVER_DIRECTORY --java PATH_TO_JAVA8 --launch`. The helper verifies the pinned launcher hash and uses classpath precedence without replacing the Forge JAR or its bundled libraries. Client launcher setup and graphical acceptance are still pending; do not assume this server helper configures clients.

## Release gates

Independent target compilation/reobfuscation, original production-rule suites, all 193 required native server scenarios on the exact installed runtime, Java 8 bytecode/archive checks, and a separate production-only startup without the test mod. Check the attached runtime summary for actual results. Source status before CI remains PENDING; this is not proof of a passing run.

Clients and servers must use matching exact-target versions. Back up worlds and do not downgrade existing worlds. No cross-version world migration is promised.

**Further API testing is still required.** Real-client visuals, installed third-party APIs, multiplayer and sustained modpack performance remain unverified. Core guide/inspection/native storage and transfer fallbacks are retained, but this is not full optional-API or scripting parity.
'''
    changes[root/'docs/releases/0.2a-port.1.md']=notes;changes[root/'README.md']=notes
    changes[root/'docs/JAVA17_RUNTIME.md']=f'''# {target} build/runtime separation

This target compiles modern source with Java 17 and converts the mod/fixtures to Java 8 before Forge reobfuscation. Do not launch it on Java 17 merely because Java 17 builds it.

Use Forge {forge}, Java 8 and the explicit ModLauncher 8.1.3 profile supplied in `tools/java8/forge35_profile.py`. The profile filename is historical; its contents validate the exact {target} target. It does not patch Forge or supply a client launcher profile.

Installed scenarios and production-only startup must pass before publication. Native pass status is in attached CI summaries, not inferred from compilation. Further API testing is still required.
'''
    for path,content in changes.items():
        path.parent.mkdir(parents=True,exist_ok=True);path.write_text(content,encoding='utf-8')
    print('Prepared full-feature source for',target,forge,'; installed acceptance PENDING')
if __name__=='__main__':prepare(ROOT)
