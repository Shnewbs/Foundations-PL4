package net.foundations.pl4.legacy;
/** Native item subclass; recovery data is held on each item stack. */
public final class LegacyEnergyCell extends net.minecraft.item.Item {
    public LegacyEnergyCell(int id) {
        super(id);
        setUnlocalizedName(LegacyPL4.ID+".legacy_energy_cell");
        setTextureName("redstone");
        setMaxStackSize(1);
    }
}
