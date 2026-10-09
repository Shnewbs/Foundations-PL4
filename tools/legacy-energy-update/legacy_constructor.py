"""Use a native Item subclass for the protected 1.6.4 Item constructor."""
from pathlib import Path
import sys
if sys.argv[2]!='1.6.4':raise SystemExit(0)
root=Path(sys.argv[1])/'src/legacy/java/net/foundations/pl4/legacy'
p=root/'LegacyPL4.java';text=p.read_text()
old='ENERGY_CELL=new Item(energyCell).setUnlocalizedName(ID+".legacy_energy_cell").setTextureName("redstone").setMaxStackSize(1);'
new='ENERGY_CELL=new LegacyEnergyCell(energyCell);'
if old in text:
    if text.count(old)!=1:raise SystemExit('Unexpected legacy item registration')
    p.write_text(text.replace(old,new))
elif new not in text:raise SystemExit('Unexpected energy cell registration')
(root/'LegacyEnergyCell.java').write_text('package net.foundations.pl4.legacy;\n/** Native item subclass; recovery data is held on each item stack. */\npublic final class LegacyEnergyCell extends net.minecraft.item.Item {\n    public LegacyEnergyCell(int id) {\n        super(id);\n        setUnlocalizedName(LegacyPL4.ID+".legacy_energy_cell");\n        setTextureName("redstone");\n        setMaxStackSize(1);\n    }\n}\n')
