"""Set the reviewed 1.19.2/1.18.2 source tree's own build and resource identity."""
from pathlib import Path
import json,os,sys
W=Path(os.environ['PL4_PORT_WORK']).resolve()
TARGETS={'1.19.2':('43.5.2','43','44','1.19.3',9),'1.18.2':('40.3.12','40','41','1.18.3',8)}
mc=sys.argv[1]
if mc not in TARGETS:raise SystemExit('Unsupported target')
forge,major,next_major,next_mc,pack=TARGETS[mc];R=W/mc
p=R/'build.gradle';s=p.read_text();s=s.replace('1.20.1',mc).replace('47.4.26',forge).replace('1.20.2',next_mc);p.write_text(s)
p=R/'src/main/resources/META-INF/mods.toml';s=p.read_text().replace('1.20.1',mc).replace('1.20.2',next_mc).replace('47.4.26',forge).replace('loaderVersion="[47,)"','loaderVersion="['+major+',)"').replace(',48)',','+next_major+')');p.write_text(s)
p=R/'src/main/resources/pack.mcmeta';p.write_text(json.dumps({'pack':{'pack_format':pack,'description':'Foundations PL4 Forge '+mc+' resources'}})+'\n')
p=R/'src/main/java/net/foundations/pl4/compat/RegisterPayloadHandlersEvent.java';p.write_text(p.read_text().replace('forge-1.20.1-','forge-'+mc+'-'))
p=R/'BUILD_STATUS.json';status=json.loads(p.read_text());status.update(minecraft=mc,loader_version=forge,port_status='NATIVE_VALIDATION_REQUIRED',payload_protocol='forge-'+mc+'-5',expected_native_tests=191);p.write_text(json.dumps(status,indent=2)+'\n')
for name in ['README.md','docs/FIELD_GUIDE.md','docs/releases/0.2a-port.1.md','src/main/resources/assets/foundations_pl4/guide/en_us.json']:
 p=R/name;p.write_text(p.read_text().replace('1.20.1',mc).replace('47.4.26',forge))
p=R/'tools/run_port_checks.py';p.write_text(p.read_text().replace("status['minecraft']=='1.20.1'","status['minecraft']=='"+mc+"'").replace("['pack_format']==15","['pack_format']=="+str(pack)))
(R/'release-version.txt').write_text('0.2a-port.1\n')
print('Configured Minecraft '+mc+', Forge '+forge+', Java 17, resource format '+str(pack))
