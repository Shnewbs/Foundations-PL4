"""Reintroduce three specific rule regressions; the actual R8 tests must reject each mutated production class."""
from pathlib import Path
import subprocess,tempfile,shutil
R=Path(__file__).resolve().parents[1];C=R/'src/main/java/net/foundations/pl4/core'
mutants=[
 ('perpendicular_extension','DisplayPlacement.java','return OptionalInt.of(hitFace);','return OptionalInt.of(mount);'),
 ('mirrored_hologram_back','HologramProjection.java','if(side<0)frame=DisplayFacing.facing(front^1);','if(side<0)frame=DisplayFacing.facing(front);'),
 ('older_layout_wins','CanvasContinuity.java','Comparator.comparingLong(Candidate::revision)','Comparator.comparingLong(Candidate::revision).reversed()')]
for name,file,old,new in mutants:
 with tempfile.TemporaryDirectory(prefix='pl4-r8-mutation-') as tmp:
  d=Path(tmp);sources=[]
  for cls in ['DisplayLayout','DisplayFacing','DisplayPlacement','HologramProjection','CanvasContinuity','GuideLayout','GuideBook']:
   src=C/(cls+'.java');text=src.read_text()
   if src.name==file:
    assert old in text,(name,old);text=text.replace(old,new,1)
   out=d/src.name;out.write_text(text);sources.append(out)
  shutil.copy2(R/'tools/R8RegressionTests.java',d/'R8RegressionTests.java');sources.append(d/'R8RegressionTests.java')
  compile=subprocess.run(['javac','--release','21','-encoding','UTF-8','-d',str(d),*map(str,sources)],capture_output=True,text=True)
  assert compile.returncode==0,compile.stderr
  run=subprocess.run(['java','-cp',str(d),'R8RegressionTests'],capture_output=True,text=True)
  assert run.returncode!=0 and 'AssertionError' in run.stderr,(name,run.stdout,run.stderr)
  print('PASS rejected '+name+': '+run.stderr.splitlines()[0])
print('PASS three deliberate R8 rule regressions rejected by executable tests (not source substring checks).')
