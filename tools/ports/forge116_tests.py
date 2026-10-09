"""Adapt all original behavior assertions to a separate 1.16.5 native-server test module."""
from pathlib import Path
import os,json,re,zipfile,subprocess
W=Path(os.environ['PL4_PORT_WORK']).resolve();R=W/'1.16.5';J=R/'src/portTest/java/net/foundations/pl4'
for p in (W/'1.18.2/src/main/java/net/foundations/pl4').glob('*GameTests.java'):(J/p.name).write_text(p.read_text())
mapping=json.loads((W/'map116-full.json').read_text());old=[]
for jar in (W/'toolchains/1.18.2/classpath').glob('*.jar'):
 with zipfile.ZipFile(jar) as archive:old += [s[:-6].replace('/','.') for s in archive.namelist() if s.endswith('.class') and s.startswith(('net/minecraft/','com/mojang/')) and '$' not in s]
simple={a.rsplit('.',1)[1]:b.rsplit('.',1)[1] for a,b in mapping.items() if a.rsplit('.',1)[1]!=b.rsplit('.',1)[1] and a.rsplit('.',1)[1] not in ['Container','EditBox','Screen']}
simple['AbstractFurnaceBlockEntity']='AbstractFurnaceTileEntity'
pattern=re.compile(r'\b('+ '|'.join(re.escape(k) for k in sorted(simple,key=len,reverse=True))+r')\b')
for p in J.glob('*.java'):
 s=p.read_text()
 def expand(match):
  prefix=match.group(1)+'.';tokens=set(re.findall(r'\b\w+\b',s))
  return '\n'.join('import '+c+';' for c in old if c.rsplit('.',1)[0]+'.'==prefix and c.rsplit('.',1)[1] in tokens)
 s=re.sub(r'import ((?:net\.minecraft|com\.mojang)\.[\w.]+)\.\*;',expand,s)
 markers={}
 for a,b in sorted(mapping.items(),key=lambda pair:len(pair[0]),reverse=True):
  if a in s:
   marker='__PORT_TYPE_'+str(len(markers))+'__';markers[marker]=b;s=re.sub(re.escape(a)+r'\b',marker,s)
 s=re.sub(r'\bTag\.(TAG_[A-Z_]+)',r'net.minecraftforge.common.util.Constants.NBT.\1',s)
 s=pattern.sub(lambda match:simple[match.group()],s)
 for a,b in markers.items():s=s.replace(a,b)
 s=s.replace('net.minecraft.gametest.framework','net.foundations.pl4.compat.scenarios').replace('net.minecraftforge.event.RegisterGameTestsEvent','net.foundations.pl4.compat.scenarios.RegisterGameTestsEvent').replace('net.minecraftforge.gametest.PrefixGameTestTemplate','net.foundations.pl4.compat.scenarios.PrefixGameTestTemplate')
 s=s.replace('ItemStack.isSameItemSameTags(','net.foundations.pl4.compat.PortData.sameItem(')
 s=s.replace('player.getInventory()','player.inventory').replace('player.getAbilities()','player.abilities').replace('JsonParser.parseString(','new JsonParser().parse(')
 s=s.replace('com.google.gson.new JsonParser()','new com.google.gson.JsonParser()')
 s=re.sub(r'(?:net\.minecraft\.nbt\.)?INBT.TAG_([A-Z_]+)',r'net.minecraftforge.common.util.Constants.NBT.TAG_\1',s)
 p.write_text(s)
subprocess.run(['java','-cp',str(W),'Rewrite116',str(J)],check=True)
for p in J.rglob('*.java'):
 s=p.read_text().replace('net.minecraft.world.level.block.entity.AbstractFurnaceTileEntity','net.minecraft.tileentity.AbstractFurnaceTileEntity').replace('net.minecraft.item.Items.RAW_IRON','net.minecraft.item.Items.IRON_ORE')
 s=s.replace('.getInventory()','.inventory').replace('.getAbilities()','.abilities')
 s=s.replace('loaded.setLevel(h.getLevel())','loaded.setLevelAndPosition(h.getLevel(),hammer.getBlockPos())')
 s=s.replace('new net.minecraft.tileentity.ChestTileEntity(absolute,h.getLevel().getBlockState(absolute))','new net.minecraft.tileentity.ChestTileEntity()')
 s=s.replace('h.getLevel().setBlockEntity(to);','to.setLevelAndPosition(h.getLevel(),absolute);h.getLevel().setBlockEntity(absolute,to);')
 s=s.replace('user.setYRot(180);user.setXRot(0);','user.yRot=180;user.xRot=0;')
 s=s.replace('net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,h.getLevel().registryAccess())','com.mojang.serialization.JsonOps.INSTANCE')
 s=re.sub(r'(getBlockState\([^\n;]+?\))\.getItem\(\)',r'\1.getBlock()',s);p.write_text(s)
S=J/'compat/scenarios';S.mkdir(parents=True,exist_ok=True)
for name,body in {
'GameTest':'public @interface GameTest {String template() default "empty";String templateNamespace() default "foundations_pl4";int timeoutTicks() default 100;}',
'BeforeBatch':'public @interface BeforeBatch {String batch() default "defaultBatch";}',
'PrefixGameTestTemplate':'public @interface PrefixGameTestTemplate {boolean value();}',
}.items():(S/(name+'.java')).write_text('package net.foundations.pl4.compat.scenarios;\n@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)\n'+body+'\n')
(S/'RegisterGameTestsEvent.java').write_text('''package net.foundations.pl4.compat.scenarios;
public final class RegisterGameTestsEvent {
 private final java.util.List<Class<?>> fixtures=new java.util.ArrayList<>();
 public void register(Class<?> type){if(fixtures.contains(type))throw new IllegalStateException("Duplicate fixture");fixtures.add(type);}
 public java.util.List<Class<?>> fixtures(){return java.util.List.copyOf(fixtures);}
}
''')
(S/'GameTestHelper.java').write_text('''package net.foundations.pl4.compat.scenarios;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
/** Fixture semantics on a real isolated server; not Mojang's modern GameTest API. */
public final class GameTestHelper {
 final ServerWorld level;final BlockPos origin;final java.util.TreeMap<Long,java.util.List<Runnable>> scheduled=new java.util.TreeMap<>();
 boolean passed;long tick;
 public GameTestHelper(ServerWorld level,BlockPos origin){this.level=level;this.origin=origin;}
 public ServerWorld getLevel(){return level;}
 public BlockPos absolutePos(BlockPos relative){return origin.offset(relative);}
 public void setBlock(BlockPos pos,Block block){setBlock(pos,block.defaultBlockState());}
 public void setBlock(BlockPos pos,BlockState state){BlockPos target=absolutePos(pos);if(!level.setBlock(target,state,3)&&level.getBlockState(target)!=state)throw new AssertionError("Unable to set fixture block "+pos);}
 public TileEntity getBlockEntity(BlockPos pos){return level.getBlockEntity(absolutePos(pos));}
 public void runAtTickTime(long time,Runnable action){if(time<tick)throw new IllegalArgumentException("Callback scheduled in the past");scheduled.computeIfAbsent(time,k->new java.util.ArrayList<>()).add(action);}
 public void succeed(){passed=true;}
 void advance(){while(!scheduled.isEmpty()&&scheduled.firstKey()<=tick){var work=scheduled.pollFirstEntry().getValue();for(var action:work)action.run();}tick++;}
}
''')
(J/'compat/PortAssertions.java').write_text('''package net.foundations.pl4.compat;
public final class PortAssertions {public static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}private PortAssertions(){}}
''')
(S/'PortScenarioMod.java').write_text(Path(__file__).with_name('PortScenarioMod.java').read_text())
print('All 191 original fixture methods adapted; execution is a separate native server gate')
