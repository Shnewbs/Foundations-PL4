"""One-way exact-target source preparation; preserves the complete native behavior suite."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
status=json.loads((R/'BUILD_STATUS.json').read_text())
if status['minecraft']=='1.17.1':
    print('Exact 1.17.1 source is already prepared; do not rewrite subsequent fixes.')
    raise SystemExit(0)
if status['minecraft']!='1.18.2':raise SystemExit('Wrong source baseline')
for name in ['build.gradle','tools/run_port_checks.py','docs/FIELD_GUIDE.md','src/main/resources/assets/foundations_pl4/guide/en_us.json','src/main/java/net/foundations/pl4/compat/RegisterPayloadHandlersEvent.java']:
 p=R/name;s=p.read_text().replace('1.18.2','1.17.1').replace('0.2a-port.2','0.2a-port.1')
 if name=='build.gradle':s=s.replace('40.3.12','37.1.1').replace('[1.17.1,1.18.3)','[1.17.1]')
 if name=='tools/run_port_checks.py':s=s.replace("['pack_format']==8","['pack_format']==7")
 p.write_text(s)
status.update(minecraft='1.17.1',version='0.2a-port.1',loader_version='37.1.1',payload_protocol='forge-1.17.1-5',expected_native_tests=193,port_status='NATIVE_BUILD_AND_TESTS_PENDING',release_notes='docs/releases/0.2a-port.1.md',installed_runtime_acceptance='PENDING',further_api_testing_required=True)
(R/'BUILD_STATUS.json').write_text(json.dumps(status,indent=2)+'\n')
p=R/'src/main/resources/pack.mcmeta';data=json.loads(p.read_text());data['pack']['pack_format']=7;p.write_text(json.dumps(data,indent=2)+'\n')
p=R/'src/main/resources/META-INF/mods.toml';s=p.read_text().replace('loaderVersion="[40,)"','loaderVersion="[37,38)"').replace('[40.3.12,41)','[37.1.1,38)').replace('[1.18.2,1.18.3)','[1.17.1]');p.write_text(s)
for name in ['DataSampler.java','TransferFilters.java']:
 p=R/'src/main/java/net/foundations/pl4'/name;s=p.read_text().replace('import net.minecraft.tags.TagKey;','import net.foundations.pl4.compat.TagKey;').replace('stack.getFluid().is(tag)','tag.contains(stack.getFluid())').replace('stack.is(tag)','tag.contains(stack.getItem())');p.write_text(s)
print('Prepared full-feature 1.17.1 source, native tag lookup and exact resources. Acceptance is pending.')
