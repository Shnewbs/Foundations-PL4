from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
def read(rel): return (R/rel).read_text()
editor=read('src/main/java/net/foundations/pl4/client/DisplayEditorScreen.java')
props=read('src/main/java/net/foundations/pl4/client/DisplayPropertiesScreen.java')
picker=read('src/main/java/net/foundations/pl4/client/DisplayPickerScreen.java')
color=read('src/main/java/net/foundations/pl4/client/DisplayColorPickerScreen.java')
part=read('src/main/java/net/foundations/pl4/client/PartScreen.java')
elems=read('src/main/java/net/foundations/pl4/core/DisplayElements.java')
palette=read('src/main/java/net/foundations/pl4/core/EditorPalette.java')
book=json.loads((R/'src/main/resources/assets/foundations_pl4/guide/en_us.json').read_text())
checks={
 'R14+ version':any(v in read('build.gradle') and v in read('src/main/resources/META-INF/neoforge.mods.toml') for v in ['0.0.1a.R14','0.0.1a.R15','0.0.1a.R16','0.0.1a.R17','0.0.2a.R1']),
 'world help removed':'drawHelp(canvas)' not in editor and 'drawHudHelp(g)' in editor,
 'HUD help editor gated':'if(!editable)return;' in editor and 'side tools stay active' in editor,
 'world side tools stay rendered':'for(int i=0;i<TOOLS.length;i++)' in editor and 'EditorChrome.toolRect(i)' in editor,
 'editor RMB back':'if(button==1){onClose();return true;}' in editor,
 'properties RMB back':'if(button==1){onClose();return true;}' in props,
 'picker RMB back':'if(button==1){onClose();return true;}' in picker,
 'settings RMB back':'if(button==1){if(tab!=0)' in part,
 'visual palette screen':'EditorPalette.PRESETS' in color and 'Use colour' in color and 'Right-click = Back' in color,
 'palette fills hex':'Palette' in props and 'EditorPalette.hex(spec.color())' in props and 'void color(int rgb)' in props,
 'color preview':'colorBox.getValue()' in props and 'g.renderOutline' in props,
 'three column default':'defaultColumns(Type type)' in elems and '?3:1' in elems,
 'columns drive render':'int cols=Math.min(e.columns' in elems and 'cols*Math.max' in elems,
 'columns preview':'cols+" cols"' in props,
 'guide updated':any(v in book['edition'] for v in ['0.0.1a.R14','0.0.1a.R15','0.0.1a.R16','0.0.1a.R17','0.0.2a.R1']) and any('player HUD' in s['body'] for c in book['chapters'] for s in c['sections']) and any('3 columns' in s['body'] for c in book['chapters'] for s in c['sections']),
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items(): print(('PASS ' if v else 'FAIL ')+k)
if failed: raise SystemExit('R14 source guard failed: '+', '.join(failed))
print(f'PASS R14 source guards: {len(checks)} of {len(checks)}.')
