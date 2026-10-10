"""Record actual cached mapped Forge28 JVM signatures, not guessed API names."""
from pathlib import Path
import subprocess, os, glob, json
R=Path(__file__).resolve().parents[1]
dest=R/'verification-logs/forge28-mapped-api.txt'
dest.parent.mkdir(exist_ok=True)
candidates=sorted(glob.glob(str(Path.home()/'.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.14.4-28.2.26_mapped_*/*.jar')))
candidates=[c for c in candidates if not any(x in c for x in ('-sources','-javadoc'))]
if not candidates:
 dest.write_text('Mapped Forge28 JAR missing; build could not prepare Java classpath.\n')
 print('No mapped JAR available, see compile output')
 raise SystemExit(0)
jar=next((p for p in candidates if p.endswith('/forge-1.14.4-28.2.26_mapped_official_1.14.4.jar')),candidates[0])
classes=['net.minecraft.world.gen.feature.Feature','net.minecraft.world.gen.feature.ConfiguredFeature','net.minecraft.world.gen.placement.Placement','net.minecraft.world.gen.placement.ConfiguredPlacement','net.minecraft.tags.TagCollection','net.minecraft.tags.ItemTags','net.minecraft.tags.FluidTags','net.minecraft.block.Block$Properties','net.minecraft.client.renderer.tileentity.TileEntityRenderer','net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher','net.minecraft.util.ActionResultType']
out=['Exact mapped JAR: '+jar]
for name in classes:
 try:
  p=subprocess.run(['javap','-classpath',jar,'-public',name],capture_output=True,text=True,timeout=20)
  out.append('\nCLASS '+name+' exit='+str(p.returncode)+'\n'+(p.stdout or p.stderr)[:12000])
 except (OSError,subprocess.TimeoutExpired) as err:out.append(name+' unavailable: '+str(err))
dest.write_text('\n'.join(out),encoding='utf-8')
print('Saved '+str(len(classes))+' cached mapped Forge28 signatures to '+str(dest.relative_to(R)))
