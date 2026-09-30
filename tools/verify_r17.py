from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
def read(rel): return (ROOT/rel).read_text()

projection=read('src/main/java/net/foundations/pl4/core/HologramProjection.java')
renderer=read('src/main/java/net/foundations/pl4/client/HostRenderer.java')
book=json.loads((ROOT/'src/main/resources/assets/foundations_pl4/guide/en_us.json').read_text())
checks={
    'R17 version in build and mod metadata':'0.0.1a.R17' in read('build.gradle') and '0.0.1a.R17' in read('src/main/resources/META-INF/neoforge.mods.toml'),
    'normal emitter bar anchor':'modelPoint(mount,yaw,8,1,10.5)' in projection,
    'advanced panel emitter anchors':'modelPoint(mount,yaw,5,2,8)' in projection and 'modelPoint(mount,yaw,8,2,11)' in projection,
    'projection clearance starts at emitter':'emitterAnchor.x()+normal.x()*distance' in projection and 'clearance(advanced)' in projection,
    'wall and floor projection rises vertically':'DisplayFacing.facing(mount==1?0:1).normal()' in projection,
    'floor and ceiling use centred advanced emitter':'modelPoint(mount,yaw,8,4,8)' in projection,
    'renderer uses view-aware anchor':'centre(p.face.ordinal(),p.hologramView' in renderer,
    'guide edition updated':'0.0.1a.R17' in book['edition'],
    'guide describes emitter-relative clearance':any('from the emitter hardware' in section['body'] for chapter in book['chapters'] for section in chapter['sections']),
}
failed=[name for name,passed in checks.items() if not passed]
for name,passed in checks.items(): print(('PASS ' if passed else 'FAIL ')+name)
if failed: raise SystemExit('R17 source guard failed: '+', '.join(failed))
print(f'PASS R17 source guards: {len(checks)} of {len(checks)}.')
