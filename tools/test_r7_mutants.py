"""Prove the R7 rules reject specific regressions. Mutates temporary copies, never shipped production source."""
from pathlib import Path
import tempfile,subprocess
R=Path(__file__).resolve().parents[1]
planner=R/'src/main/java/net/foundations/pl4/core/MultipartTopology.java'
original=planner.read_text()
mutants={
 'R6-style shared display/ordinary slots':('kind.display()?DISPLAY_BASE+face:face','kind.display()?face:face'),
 'VISUAL port accidentally unions machine networks':('exports.add(new Export(reader.id,cable.id));','edges.add(new Edge(reader.id,cable.id));exports.add(new Export(reader.id,cable.id));'),
 'Free cable lead spans an unplaced endpoint cell':('            // A mounted endpoint', '            else { Host adjacent=hosts.get(device.cell.offset(device.face^1)); if(adjacent!=null&&adjacent.cable()!=null){edges.add(new Edge(adjacent.cable().id,device.id));leads.add(device.id);} }\n            // A mounted endpoint'),
}
for label,(before,after) in mutants.items():
 assert before in original,label
 with tempfile.TemporaryDirectory(prefix='pl4-r7-mutant-')as tmp:
  tmp=Path(tmp);source=tmp/'MultipartTopology.java';source.write_text(original.replace(before,after));out=tmp/'classes'
  cmd=['javac','--release','21','-d',str(out),str(source),str(R/'src/main/java/net/foundations/pl4/Kind.java'),str(R/'src/main/java/net/foundations/pl4/core/ConnectionRules.java'),str(R/'src/main/java/net/foundations/pl4/core/DisjointSets.java'),str(R/'tools/R7RegressionTests.java')]
  subprocess.run(cmd,check=True,capture_output=True)
  result=subprocess.run(['java','-cp',str(out),'R7RegressionTests'],capture_output=True,text=True)
  assert result.returncode!=0,'Regression escaped: '+label
  print('PASS regression rejected:',label,'|',result.stderr.splitlines()[0])
