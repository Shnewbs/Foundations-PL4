"""Reject deliberate regressions with executable production tests and item-resource checks."""
from pathlib import Path
import tempfile,subprocess,shutil,json
from verify_r10 import check_models
R=Path(__file__).resolve().parents[1];C=R/'src/main/java/net/foundations/pl4/core'
names=['MonitorPresentation','GuideNavigation','GuideBook','GuideLayout','DisplayElements','LayoutTransactions','EditorSelection','DynamicCanvasLayout']
mutations=[
 ('list_covers_custom_editor','MonitorPresentation','return !editing&&mode==','return mode=='),
 ('new_element_hidden_on_other_page','LayoutTransactions','page=element.page();',''),
 ('tab_leaves_unrelated_chapter','GuideNavigation','current!=null&&current.category().equals(category)?current:first(book,category)','current!=null?current:first(book,category)')]
for name,filename,old,new in mutations:
    with tempfile.TemporaryDirectory(prefix='pl4-r10-mutant-') as t:
        p=Path(t);paths=[]
        for cls in names:
            s=(C/f'{cls}.java').read_text()
            if cls==filename:
                assert old in s,name;s=s.replace(old,new,1)
            f=p/f'{cls}.java';f.write_text(s);paths.append(str(f))
        result=subprocess.run(['javac','--release','21','-encoding','UTF-8','-d',str(p),*paths,str(R/'tools/R10RegressionTests.java')],capture_output=True,text=True)
        assert result.returncode==0,result.stderr
        result=subprocess.run(['java','-cp',str(p),'R10RegressionTests'],capture_output=True,text=True)
        assert result.returncode!=0 and 'AssertionError' in result.stderr,(name,result.stdout,result.stderr)
        print('PASS rejected '+name+': '+result.stderr.splitlines()[0])
with tempfile.TemporaryDirectory(prefix='pl4-r10-resource-mutant-') as t:
    p=Path(t);shutil.copytree(R/'src/main/resources/assets/foundations_pl4/models',p/'src/main/resources/assets/foundations_pl4/models')
    (p/'src/main/java/net/foundations/pl4').mkdir(parents=True);shutil.copy2(R/'src/main/java/net/foundations/pl4/Kind.java',p/'src/main/java/net/foundations/pl4/Kind.java')
    f=p/'src/main/resources/assets/foundations_pl4/models/item/largedisplayscreen.json';model=json.loads(f.read_text());model['display']['ground']['scale']=[1,1,1];f.write_text(json.dumps(model))
    try:check_models(p)
    except AssertionError as e:print('PASS rejected full_size_ground_monitor:',e)
    else:raise AssertionError('Full-size ground item escaped resource guard')
print('PASS four retained R10 regressions rejected; R11 supersedes the old centred-header geometry mutation.')
