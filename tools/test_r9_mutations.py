"""Reintroduce functional bugs into production classes and require the R9 tests to reject them."""
from pathlib import Path
import tempfile,subprocess
R=Path(__file__).resolve().parents[1];C=R/'src/main/java/net/foundations/pl4/core'
mutations=[
 ('block_becomes_item','DisplayElements.java','e.type==Type.BLOCK));','false));'),
 ('stale_edit_accepted','LayoutTransactions.java','if(expected!=before.revision)','if(false)'),
 ('counter_over_editor','DisplayElements.java','Math.clamp(layer,0,8)*.01/16.0','(layer==3?1:Math.clamp(layer,0,8))*.01/16.0'),
 ('wrong_cursor_origin','DisplayPicking.java','2*mouseX/width-1','2*mouseX/width')]
for name,filename,old,new in mutations:
 with tempfile.TemporaryDirectory(prefix='pl4-r9-mutant-') as tmp:
  p=Path(tmp);files=[]
  for n in ['DisplayElements','LayoutTransactions','EditorSelection','DisplayPicking']:
   path=C/(n+'.java');s=path.read_text()
   if path.name==filename:
    assert old in s,(name,old);s=s.replace(old,new,1)
   out=p/path.name;out.write_text(s);files.append(out)
  test=p/'R9RegressionTests.java';test.write_text((R/'tools/R9RegressionTests.java').read_text());files.append(test)
  c=subprocess.run(['javac','--release','21','-encoding','UTF-8','-d',str(p),*map(str,files)],capture_output=True,text=True);assert c.returncode==0,c.stderr
  run=subprocess.run(['java','-cp',str(p),'R9RegressionTests'],capture_output=True,text=True);assert run.returncode!=0 and 'AssertionError' in run.stderr,(name,run.stdout,run.stderr)
  print('PASS rejected '+name+': '+run.stderr.splitlines()[0])
print('PASS four deliberate R9 production regressions rejected. No Minecraft/native renderer execution.')
