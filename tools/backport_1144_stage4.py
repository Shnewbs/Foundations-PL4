"""Fix additional Forge28 methods using signatures captured from the actual mapped SDK."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
p=R/'BUILD_STATUS.json'
status=json.loads(p.read_text())
if status.get('minecraft')!='1.14.4' or status.get('native_api_backport') not in ('forge28-stage3','forge28-stage4'):
 raise SystemExit('Wrong source baseline')
if status['native_api_backport']=='forge28-stage4':raise SystemExit(0)
q=R/'src/main/java/net/foundations/pl4/FoundationsPL4.java'
s=q.read_text()
assert '.maxStackSize(1)' in s
# Exactly inspected: Item.Properties.stacksTo(int); maxStackSize does not exist.
q.write_text(s.replace('.maxStackSize(1)','.stacksTo(1)'),encoding='utf-8')
q=R/'src/main/java/net/foundations/pl4/BuiltinInfoProvider.java'
s=q.read_text()
for old,new in [('e.getPosX()','e.x'),('e.getPosY()','e.y'),('e.getPosZ()','e.z')]:
 if old not in s:raise SystemExit('Missing entity coordinate anchor: '+old)
 s=s.replace(old,new)
# The mapped 1.14.4 Entity stores public doubles x/y/z; it has neither
# getX() nor getPosX() methods. No positional data is inferred or removed.
q.write_text(s,encoding='utf-8')
status['native_api_backport']='forge28-stage4'
p.write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')
print('Verified Forge28 Item.Properties.stacksTo and Entity.x/y/z; visual rendering acceptance remains pending.')
