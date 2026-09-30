from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
D=(R/'src/main/java/net/foundations/pl4/core/DynamicCanvasLayout.java').read_text()
H=(R/'src/main/java/net/foundations/pl4/client/HostRenderer.java').read_text()
E=(R/'src/main/java/net/foundations/pl4/client/DisplayEditorScreen.java').read_text()
P=(R/'src/main/java/net/foundations/pl4/Part.java').read_text()
N=(R/'src/main/java/net/foundations/pl4/DisplayNetworks.java').read_text()
assert 'PIXELS_PER_BLOCK=180' in D and 'migrate' in D
assert 'DynamicCanvasLayout.large' in H and 'logicalW' in H and 'logicalH' in H
assert 'spaceW()' in E and 'spaceH()' in E and 'EditorChrome.toolRect(i)' in E
assert 'layoutWidth' in P and 'layoutHeight' in P
assert 'DynamicCanvasLayout.migrate' in N
b=json.loads((R/'src/main/resources/assets/foundations_pl4/guide/en_us.json').read_text());assert 'Foundations PL4' in b['edition'] and '1.21.1' in b['edition']
print('PASS R11 source wiring: dynamic whole-board canvas, proportional migration, editor bounds and guide edition.')
