"""Inspect real Forge38/Minecraft 1.18 GameTest symbols from the mapped JAR."""
from pathlib import Path
import subprocess
R=Path(__file__).resolve().parents[1]
classes=[
 "net.minecraft.gametest.framework.GameTestRunner",
 "net.minecraft.gametest.framework.GameTestRegistry",
 "net.minecraft.gametest.framework.GameTestBatchRunner",
 "net.minecraft.gametest.framework.GameTestInfo",
 "net.minecraft.gametest.framework.GameTestHelper",
 "net.minecraft.gametest.framework.GameTest",
 "net.minecraft.gametest.framework.GameTestListener",
 "net.minecraft.gametest.framework.TestCommand",
 "net.minecraft.gametest.framework.GameTestServer",
 "net.minecraft.gametest.framework.GlobalTestReporter",
 "net.minecraft.gametest.framework.TestReporter",
 "net.minecraft.gametest.framework.MultipleTestTracker",
 "net.minecraft.gametest.framework.TestFunction",
 "net.minecraft.gametest.framework.GameTestTicker",
 "net.minecraft.commands.Commands",
 "net.minecraft.server.level.ServerLevel",
 "net.minecraft.server.MinecraftServer",
 "net.minecraftforge.event.RegisterCommandsEvent",
 "net.minecraftforge.event.TickEvent$ServerTickEvent"]
jars=[p for root in [R/'.gradle',Path.home()/'.gradle'] if root.exists()
      for p in root.rglob('*.jar')
      if '1.18-38.0.17' in p.name and 'mapped_' in p.name
      and not any(v in p.name for v in ('sources','javadoc'))]
if not jars:raise SystemExit('No pinned Forge38 mapped class JAR; native API cannot be inferred')
jar=sorted(jars,key=lambda p:len(str(p)))[0]
lines=['Mapped Forge38 class JAR: '+str(jar)]
for clazz in classes:
 try:
  result=subprocess.run(['javap','-classpath',str(jar),'-public',clazz],
       capture_output=True,text=True,timeout=20)
  lines.append('\nCLASS '+clazz+' exit='+str(result.returncode)+'\n'+(result.stdout or result.stderr)[:15000])
 except (OSError,subprocess.TimeoutExpired) as err:
  lines.append(clazz+' failed: '+str(err))
out=R/'verification-logs/forge38-native-gametest-signatures.txt'
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text('\n'.join(lines),encoding='utf-8')
print('Saved exact Forge38 native GameTest signatures; no gameplay acceptance inferred')
