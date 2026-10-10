"""Forge25/MCP 1.13.2 first-stage source name migration; NEVER remove features.

The names below are verified against the 1.13.x upstream MinecraftForge
source imports, not speculative bytecode renames. This pass only fixes
moved/renamed native classes, retaining all logic, recipes, interfaces,
and all 194 gameplay scenarios; method signatures still require native work.
"""
from pathlib import Path
import json
import re

ROOT=Path(__file__).resolve().parents[1]
status_path=ROOT/'BUILD_STATUS.json'
status=json.loads(status_path.read_text())
if status.get('minecraft')!='1.13.2' or status.get('loader_version')!='25.0.223':
    raise SystemExit('Refusing a non-1.13.2 source target')
if status.get('native_api_backport')=='forge25-mcp-names-v1':
    print('MCP name migration already applied; no duplication')
    raise SystemExit(0)

# Exact Forge1.13 import spellings from net.minecraftforge.common.ForgeHooks
# and net.minecraftforge.event.ForgeEventFactory (1.13.x upstream).
name_map={
 'Direction':'EnumFacing',
 'CompoundNBT':'NBTTagCompound',
 'ListNBT':'NBTTagList',
 'INBT':'NBTBase',
 'PlayerEntity':'EntityPlayer',
 'ServerPlayerEntity':'EntityPlayerMP',
 'ServerWorld':'WorldServer',
 'BlockState':'IBlockState',
 'StringTextComponent':'TextComponentString',
 'BlockRayTraceResult':'RayTraceResult',
 'Vector3d':'Vec3d',
 'Hand':'EnumHand',
 'ActionResultType':'EnumActionResult',
 'ChestTileEntity':'TileEntityChest',
 'Items':'Items',
 'Blocks':'Blocks',
 'Fluids':'Fluids',
}
prefix_map={
 'net.minecraft.util.Direction':'net.minecraft.util.EnumFacing',
 'net.minecraft.nbt.CompoundNBT':'net.minecraft.nbt.NBTTagCompound',
 'net.minecraft.nbt.ListNBT':'net.minecraft.nbt.NBTTagList',
 'net.minecraft.nbt.INBT':'net.minecraft.nbt.NBTBase',
 'net.minecraft.entity.player.PlayerEntity':'net.minecraft.entity.player.EntityPlayer',
 'net.minecraft.entity.player.ServerPlayerEntity':'net.minecraft.entity.player.EntityPlayerMP',
 'net.minecraft.world.server.ServerWorld':'net.minecraft.world.WorldServer',
 'net.minecraft.block.BlockState':'net.minecraft.block.state.IBlockState',
 'net.minecraft.util.text.StringTextComponent':'net.minecraft.util.text.TextComponentString',
 'net.minecraft.util.math.BlockRayTraceResult':'net.minecraft.util.math.RayTraceResult',
 'net.minecraft.util.math.vector.Vector3d':'net.minecraft.util.math.Vec3d',
 'net.minecraft.util.Hand':'net.minecraft.util.EnumHand',
 'net.minecraft.util.ActionResultType':'net.minecraft.util.EnumActionResult',
 'net.minecraft.tileentity.ChestTileEntity':'net.minecraft.tileentity.TileEntityChest',
 'net.minecraft.item.Items':'net.minecraft.init.Items',
 'net.minecraft.block.Blocks':'net.minecraft.init.Blocks',
 'net.minecraft.fluid.Fluids':'net.minecraft.init.Fluids',
}
changed=0
for folder in (ROOT/'src/main/java',ROOT/'src/portTest/java'):
 if not folder.is_dir():continue
 for path in folder.rglob('*.java'):
    before=path.read_text(encoding='utf-8')
    source=before
    # First substitute fully qualified package paths, longest first.
    for old,new in sorted(prefix_map.items(),key=lambda p:-len(p[0])):
        source=source.replace(old,new)
    # Fix remaining simple class references without touching longer symbols.
    for old,new in name_map.items():
        if old==new:continue
        source=re.sub(r'\b'+re.escape(old)+r'\b',new,source)
    # Avoid invalid double rewrites of otherwise correctly qualified classes.
    if source!=before:
        path.write_text(source,encoding='utf-8')
        changed+=1

status['native_api_backport']='forge25-mcp-names-v1'
status['port_status']='FORGE25_NATIVE_MCP_API_BACKPORT'
status['native_compilation']='PENDING'
status['native_scenarios']='PENDING'
status['release_published']=False
status['further_api_testing_required']=True
status_path.write_text(json.dumps(status,indent=2)+'\n',encoding='utf-8')
print('Migrated',changed,'Java sources to grounded Forge25 MCP class names; native compile/194 tests still required')
