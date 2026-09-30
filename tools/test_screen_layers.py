"""Exercise the JDK source-order guard; not Minecraft client or gameplay tests."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parent.parent
CLIENT = Path('src/main/java/net/foundations/pl4/client')


class ScreenLayerGuardTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.compiled = tempfile.TemporaryDirectory(prefix='pl4-layer-verifier-')
        subprocess.run(['javac', '-encoding', 'UTF-8', '--release', '21', '-d',
                        cls.compiled.name, str(ROOT / 'tools/VerifyScreenLayers.java')],
                       check=True, capture_output=True, text=True)

    @classmethod
    def tearDownClass(cls):
        cls.compiled.cleanup()

    def setUp(self):
        self.workspace = tempfile.TemporaryDirectory(prefix='pl4-layer-case-')
        self.root = Path(self.workspace.name)
        (self.root / CLIENT).mkdir(parents=True)
        for name in ('PartScreen.java', 'GuideScreen.java'):
            shutil.copy2(ROOT / CLIENT / name, self.root / CLIENT / name)

    def tearDown(self):
        self.workspace.cleanup()

    def change(self, name, old, new):
        path = self.root / CLIENT / name
        text = path.read_text(encoding='utf-8')
        self.assertEqual(text.count(old), 1, f'Ambiguous mutation: {old}')
        path.write_text(text.replace(old, new), encoding='utf-8')

    def verify(self, accepted, reason=''):
        result = subprocess.run(['java', '-cp', self.compiled.name,
                                 'VerifyScreenLayers', str(self.root)],
                                capture_output=True, text=True)
        self.assertEqual(result.returncode == 0, accepted, result.stdout + result.stderr)
        if reason:
            self.assertIn(reason, result.stderr)

    def test_current_screens_pass(self):
        self.verify(True)

    def test_part_content_before_background_rejected(self):
        self.change('PartScreen.java', 'super.renderBackground(g,mx,my,partial);',
                    'g.fill(0,0,1,1,0);super.renderBackground(g,mx,my,partial);')
        self.verify(False, 'must begin with super.renderBackground')

    def test_guide_content_before_background_rejected(self):
        self.change('GuideScreen.java', 'super.renderBackground(g,x,y,partial);',
                    'g.fill(0,0,1,1,0);super.renderBackground(g,x,y,partial);')
        self.verify(False, 'must begin with super.renderBackground')

    def test_repeated_blur_rejected(self):
        self.change('PartScreen.java', 'super.renderBackground(g,mx,my,partial);',
                    'super.renderBackground(g,mx,my,partial);super.renderBackground(g,mx,my,partial);')
        self.verify(False, 'exactly once')

    def test_recursive_widget_pass_rejected(self):
        self.change('PartScreen.java', 'hoveredRowTooltip=null;',
                    'hoveredRowTooltip=null;super.render(g,mx,my,partial);')
        self.verify(False, 'Late or repeated')

    def test_tooltip_in_background_rejected(self):
        self.change('PartScreen.java', 'hoveredRowTooltip=null;',
                    'hoveredRowTooltip=null;g.renderTooltip(font,title,mx,my);')
        self.verify(False, 'Tooltips must render after widgets')

    def test_stale_tooltip_rejected(self):
        self.change('PartScreen.java', 'hoveredRowTooltip=null;', '')
        self.verify(False, 'Clear hoveredRowTooltip')

    def test_guide_render_override_rejected(self):
        self.change('GuideScreen.java', '    @Override public boolean mouseScrolled',
                    '    public void render(GuiGraphics g,int x,int y,float p){super.render(g,x,y,p);}\n'
                    '    @Override public boolean mouseScrolled')
        self.verify(False, 'must inherit Screen.render')

    def test_missing_row_tooltip_rejected(self):
        self.change('PartScreen.java',
                    'if(hoveredRowTooltip!=null)g.renderTooltip(font,hoveredRowTooltip,mx,my);', '')
        self.verify(False, 'Missing post-widget row tooltip')

    def test_content_before_widgets_in_render_rejected(self):
        self.change('PartScreen.java', 'super.render(g,mx,my,partial);',
                    'g.fill(0,0,1,1,0);super.render(g,mx,my,partial);')
        self.verify(False, 'must begin with super.render(')

    def test_missing_background_hook_rejected(self):
        self.change('GuideScreen.java', 'public void renderBackground(', 'public void legacyRender(')
        self.verify(False, 'Missing renderBackground hook')


if __name__ == '__main__':
    unittest.main(verbosity=2)
