from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
elements=(ROOT/'src/main/java/net/foundations/pl4/core/DisplayElements.java').read_text()
painter=(ROOT/'src/main/java/net/foundations/pl4/client/DisplayPainter.java').read_text()
canvas=(ROOT/'src/main/java/net/foundations/pl4/client/DisplayCanvas.java').read_text()
editor=(ROOT/'src/main/java/net/foundations/pl4/client/DisplayEditorScreen.java').read_text()
checks={
 'explicit box plane':'record Box(Rect rect,int color,boolean filled,int layer)' in elements,
 'bar base plane one':'new Box(b,0xFF333333,true,1)' in elements,
 'bar fill plane two':'0xFF000000|e.color,true,2' in elements,
 'bar frame plane three':'new Box(b,0xFFB0B0B0,false,3)' in elements,
 'shallow content/editor plane budget':'Math.clamp(layer,0,8)*.01/16.0' in elements,
 'painter consumes explicit box plane':'box.color(),box.layer()' in painter,
 'normal text plane three':'text.overlay()?4:3' in painter,
 'automatic rules off physical surface':'0xFF637A7E,1' in painter and '0xFF465456,1' in painter,
 'all element planes get order bias':'layer>0&&layer<5' in canvas,
 'editor chrome starts above content':editor.count(',5)')>=2 and ',6)' in editor and ',7)' in editor,
 'no old box inference':'box.color(),1);else canvas.outline' not in painter,
}
failed=[name for name,ok in checks.items() if not ok]
if failed: raise SystemExit('R12 source guard failed: '+', '.join(failed))
print(f'PASS R12 source guards: {len(checks)} of {len(checks)}.')
