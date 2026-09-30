from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
def read(rel): return (R/rel).read_text()
holo=read('src/main/java/net/foundations/pl4/core/HologramProjection.java')
host=read('src/main/java/net/foundations/pl4/client/HostRenderer.java')
book=json.loads((R/'src/main/resources/assets/foundations_pl4/guide/en_us.json').read_text())
checks={
 'R15+ version':any(v in read('build.gradle') and v in read('src/main/resources/META-INF/neoforge.mods.toml') for v in ['0.0.1a.R15','0.0.1a.R16']),
 'normal clearance': 'NORMAL_PROJECTION_CLEARANCE = .90' in holo,
 'advanced clearance': 'ADVANCED_PROJECTION_CLEARANCE = 1.20' in holo,
 'surface-relative centre': 'MOUNT_SURFACE_OFFSET-clearance(advanced)' in holo,
 'camera does not move plane': 'var origin=centre(mount,advanced)' in holo and 'return new Projection(origin,frame)' in holo,
 'renderer uses projection origin': 'HologramProjection.forCamera' in host and 'projection.centre().x()' in host,
 'guide R15+ edition':any(v in book['edition'] for v in ['0.0.1a.R15','0.0.1a.R16']),
 'natural first tutorial':any(s['heading']=='1  /  WHAT YOU NEED' and 'keep it simple' in s['body'] for c in book['chapters'] if c['id']=='tutorial_inventory' for s in c['sections']),
 'hologram guide clearance':any('0.90 blocks' in s['body'] and '1.20' in s['body'] for c in book['chapters'] if c['id'] in {'tutorial_hologram','holograms'} for s in c['sections']),
 'tutorial count retained':sum(1 for c in book['chapters'] if c['id'].startswith('tutorial_'))==6,
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items():print(('PASS ' if v else 'FAIL ')+k)
if failed:raise SystemExit('R15 source guard failed: '+', '.join(failed))
print(f'PASS R15 source guards: {len(checks)} of {len(checks)}.')
