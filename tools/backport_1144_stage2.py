"""Correct confirmed Forge28 method-name differences without dropping gameplay logic."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
s=R/'BUILD_STATUS.json';status=json.loads(s.read_text())
if status.get('minecraft')!='1.14.4':raise SystemExit('Wrong target')
if status.get('native_api_backport')=='forge28-stage2':raise SystemExit(0)
if status.get('native_api_backport')!='forge28-stage1':raise SystemExit('Stage 1 must run first')
p=R/'src/main/java/net/foundations/pl4/FoundationsPL4.java'
t=p.read_text()
assert 'Block.Properties.create(' in t
# Mojang method name in the actual mapped Forge28 SDK is 'of', not MCP 'create'.
p.write_text(t.replace('Block.Properties.create(','Block.Properties.of('))
p=R/'src/main/java/net/foundations/pl4/compat/TagKey.java'
t=p.read_text()
old='(net.minecraft.tags.ItemTags.getCollection().get(location)!=null&&net.minecraft.tags.ItemTags.getCollection().get(location).contains((net.minecraft.item.Item)value))'
assert old in t,'Unexpected tag loader'
p.write_text(t.replace(old,'net.minecraft.tags.ItemTags.getAllTags().getTagOrEmpty(location).contains((net.minecraft.item.Item)value)'))
p=R/'src/main/java/net/foundations/pl4/BuiltinInfoProvider.java'
t=p.read_text()
assert 'e.getX()' in t and 'e.getY()' in t and 'e.getZ()' in t
for old,new in [('e.getX()','e.getPosX()'),('e.getY()','e.getPosY()'),('e.getZ()','e.getPosZ()')]:t=t.replace(old,new)
p.write_text(t)
status['native_api_backport']='forge28-stage2'
s.write_text(json.dumps(status,indent=2)+'\n')
print('Forge28 stage2 applied: exact Block.Properties.of, modern item tag access, legacy entity position methods')
