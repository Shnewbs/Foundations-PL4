"""Backport native 1.19.3/Forge44 registry, GUI and rendering APIs.

No production features or test cases are removed. The Forge44-native registries
and creative mode callback replace the removed 1.19.2 static interfaces; the
editor retains a real projected pose using JOML, not an estimated transform.
"""
from pathlib import Path
import json
import re

R=Path(__file__).resolve().parents[1];J=R/'src/main/java/net/foundations/pl4'
p=R/'BUILD_STATUS.json';status=json.loads(p.read_text())
if status.get('minecraft')!='1.19.3' or status.get('loader_version')!='44.1.23':
    raise SystemExit('Wrong Minecraft/Forge source target')
if status.get('native_api_backport')=='forge44-stage1':raise SystemExit(0)

for path in J.rglob('*.java'):
    s=path.read_text(encoding='utf-8')
    # Static registry instances moved from Registry to BuiltInRegistries in 1.19.3.
    for entry in ('ITEM','FLUID','BLOCK'):
        s=s.replace('net.minecraft.core.Registry.'+entry,
            'net.minecraft.core.registries.BuiltInRegistries.'+entry)
        s=re.sub(r'(?<![\w.])Registry\.'+entry+r'\b',
            'net.minecraft.core.registries.BuiltInRegistries.'+entry,s)
    if 'import com.mojang.math.Vector3f;' in s:
        s=s.replace('import com.mojang.math.Vector3f;\n','')
    for axis in ('X','Y','Z'):
        s=s.replace('Vector3f.'+axis+'P.rotationDegrees(',
            'net.foundations.pl4.compat.AxisRotation.'+axis.lower()+'p(')
    if 'import net.foundations.pl4.compat.Button;' in s:
        s=s.replace('Button.builder(', 'Button.pl4Builder(')
    if s!=path.read_text(encoding='utf-8'):
        path.write_text(s,encoding='utf-8')

registries=J/'compat/Registries.java'
s=registries.read_text(encoding='utf-8')
if 'BLOCK_REGISTRY' not in s:raise SystemExit('Expected Forge43 registry bridge')
s=re.sub(r'net\.minecraft\.core\.Registry\.(\w+)_REGISTRY',
   lambda m:'net.minecraft.core.registries.Registries.'+m.group(1),s)
registries.write_text(s,encoding='utf-8')

mod=J/'FoundationsPL4.java'
s=mod.read_text(encoding='utf-8')
old='''    public static final CreativeModeTab TAB=new CreativeModeTab("foundations_pl4"){
        @Override public ItemStack makeIcon(){return new ItemStack(item("sapphire"));}
        @Override public void fillItemList(net.minecraft.core.NonNullList<ItemStack> items){ITEMS.getEntries().forEach(entry->items.add(new ItemStack(entry.get())));}
    };
'''
if s.count(old)!=1:raise SystemExit('Unexpected creative tab registration baseline')
s=s.replace(old,'')
anchor='BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus);'
if s.count(anchor)!=1:raise SystemExit('Missing Forge44 registration event anchor')
s=s.replace(anchor,anchor+'''
        bus.addListener((net.minecraftforge.event.CreativeModeTabEvent.Register event) ->
            event.registerCreativeModeTab(id("items"), builder -> builder
                .title(Component.literal("Foundations PL4"))
                .icon(() -> new ItemStack(item("sapphire")))
                .displayItems((parameters, output) ->
                    ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))));
''')
mod.write_text(s,encoding='utf-8')

button=J/'compat/Button.java';s=button.read_text(encoding='utf-8')
old='super(x,y,width,height,message,b->press.onPress((Button)b));'
new='super(x,y,width,height,message,b->press.onPress((Button)b),net.minecraft.client.gui.components.Button.DEFAULT_NARRATION);'
if old not in s:raise SystemExit('Unexpected Forge43 button ctor')
s=s.replace(old,new).replace('public static Builder builder(Component message,OnPress press)', 'public static Builder pl4Builder(Component message,OnPress press)')
s=s.replace('public int getX(){return x;} public int getY(){return y;} public void setX(int value){x=value;} public void setY(int value){y=value;}','')
button.write_text('\n'.join(line.rstrip() for line in s.splitlines())+'\n',encoding='utf-8')
edit=J/'compat/EditBox.java';s=edit.read_text(encoding='utf-8')
old='public int getX(){return x;}public int getY(){return y;}public void setX(int value){x=value;}public void setY(int value){y=value;}'
if old not in s:raise SystemExit('Unexpected Forge43 EditBox accessors')
edit.write_text('\n'.join(line.rstrip() for line in s.replace(old,'').splitlines())+'\n',encoding='utf-8')

editor=J/'client/DisplayEditorScreen.java';s=editor.read_text(encoding='utf-8')
s=s.replace('import com.mojang.math.Matrix4f;','import org.joml.Matrix4f;')
s=s.replace('matrix.multiply(', 'matrix.mul(')
s=s.replace('matrix.store(buffer);','matrix.get(buffer);')
editor.write_text(s,encoding='utf-8')

axis=J/'compat/AxisRotation.java'
axis.write_text('''package net.foundations.pl4.compat;
import org.joml.Quaternionf;
/** Forge44 JOML replacements for the former Minecraft axis quaternions. */
public final class AxisRotation {
    public static Quaternionf xp(float deg){return new Quaternionf().rotationX((float)Math.toRadians(deg));}
    public static Quaternionf yp(float deg){return new Quaternionf().rotationY((float)Math.toRadians(deg));}
    public static Quaternionf zp(float deg){return new Quaternionf().rotationZ((float)Math.toRadians(deg));}
    private AxisRotation(){}
}
''',encoding='utf-8')
status.update(native_api_backport='forge44-stage1',port_status='FORGE44_NATIVE_API_VALIDATION',
    native_compilation='PENDING',release_published=False,further_api_testing_required=True)
p.write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')
print('Applied Forge44 registries, creative tab, JOML axes/editor and GUI control adapters; 191 native tests remain mandatory')
