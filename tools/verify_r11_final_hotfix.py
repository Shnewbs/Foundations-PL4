from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
editor=(ROOT/'src/main/java/net/foundations/pl4/client/DisplayEditorScreen.java').read_text()
painter=(ROOT/'src/main/java/net/foundations/pl4/client/DisplayPainter.java').read_text()
canvas=(ROOT/'src/main/java/net/foundations/pl4/client/DisplayCanvas.java').read_text()
elements=(ROOT/'src/main/java/net/foundations/pl4/core/DisplayElements.java').read_text()
chrome=(ROOT/'src/main/java/net/foundations/pl4/core/EditorChrome.java').read_text()
checks={
    'shared toolbar geometry core':'TOOLBAR_X=2,TOOLBAR_Y=2,TOOLBAR_W=14,TOOLBAR_H=12,TOOLBAR_STEP=13' in chrome,
    'render uses shared toolbar rectangles':'EditorChrome.toolRect(i)' in editor,
    'hover/click uses same hit helper':'EditorChrome.toolAt(p.x(),p.y(),TOOLS.length)' in editor,
    'old outside-screen toolbar hitbox removed':'p.x()>=-17&&p.x()<-3' not in editor,
    'toolbar background border glyph separated':'0xE6080B0D,5)' in editor and 'canvas.outline(r,accent,6)' in editor and 'false,7)' in editor,
    'filled element geometry uses explicit non-surface planes':'box.color(),box.layer()' in painter and 'true,1)' in elements and 'true,2)' in elements,
    'normal element text remains above pictures/backgrounds':'text.overlay()?4:3' in painter,
    'item/block picture plane at layer two':'depth(2)' in canvas,
    'fluid picture plane at layer two':'color,depth(2)' in canvas,
    'automatic text remains lifted from panel':'part.color,false,3)' in painter,
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items(): print(('PASS ' if v else 'FAIL ')+k)
if failed: raise SystemExit('R11-R13 stabilization guard failed: '+', '.join(failed))
print(f'R11-R13 stabilization guards PASS ({len(checks)} checks).')
