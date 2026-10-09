"""Correct native capability casts discovered by the actual 1.12.2 compiler."""
from pathlib import Path
import sys
root=Path(sys.argv[1]); target=sys.argv[2]
if target!='1.12.2':raise SystemExit(0)
base='net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY'
for rel,old,new in [
 ('src/legacy/java/net/foundations/pl4/legacy/LegacyTile.java','return cap.cast(nativeFluids);','return '+base+'.cast(nativeFluids);'),
 ('src/legacyTest/java/net/foundations/pl4/legacy/tests/NativeScenarios.java','hasCapability(c,side)?c.cast(this):null','hasCapability(c,side)?'+base+'.cast(this):null'),
]:
 path=root/rel; text=path.read_text(encoding='utf-8')
 if new in text and old not in text:continue
 if text.count(old)!=1:raise SystemExit('Unexpected native source: '+rel)
 path.write_text(text.replace(old,new),encoding='utf-8')
 print('Fixed native capability generic cast: '+rel)
