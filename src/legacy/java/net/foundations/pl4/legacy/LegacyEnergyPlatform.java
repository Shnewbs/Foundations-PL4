package net.foundations.pl4.legacy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/** Native Forge Energy only. A denied face is never retried as unsided or as a different API. */
public final class LegacyEnergyPlatform {
    public static final String UNIT = "FE";
    public static long time(LegacyTile node) { return node.getWorld().getTotalWorldTime(); }
    public static ConservingEnergy.Port resolve(final LegacyTile node, final ConservingItems.Work work) {
        if (!node.fluidServer() || node.side < 0 || node.side > 5 || !node.getWorld().isBlockLoaded(node.target(),false)) return null;
        final TileEntity target = node.getWorld().getTileEntity(node.target());
        if (target == null || target.isInvalid()) return null;
        if (target instanceof LegacyTile) {
            LegacyTile own = (LegacyTile)target;
            return own.role()==LegacyEnergy.BUFFER && node.owner.equals(own.owner) ? own.energy : null;
        }
        final EnumFacing face = EnumFacing.getFront(node.side).getOpposite();
        if (!work.take() || !target.hasCapability(CapabilityEnergy.ENERGY,face) || !work.take()) return null;
        final IEnergyStorage handler = target.getCapability(CapabilityEnergy.ENERGY,face);
        if (handler == null) return null;
        return new ConservingEnergy.Port() {
            public Object identity() { return handler; }
            private boolean live() {
                return node.fluidServer() && !target.isInvalid() && node.getWorld().isBlockLoaded(node.target(),false)
                        && node.getWorld().getTileEntity(node.target())==target;
            }
            public int extract(int n, boolean simulate) {
                // Core charges one call for canExtract; the budget also charges the extract invocation.
                return live() && handler.canExtract() && work.take() ? handler.extractEnergy(n,simulate) : 0;
            }
            public int receive(int n, boolean simulate) {
                return live() && handler.canReceive() && work.take() ? handler.receiveEnergy(n,simulate) : 0;
            }
        };
    }
    public static IEnergyStorage nativeBuffer(final LegacyTile node) {
        return new IEnergyStorage() {
            public int receiveEnergy(int n,boolean simulate) { return node.energy.receive(n,simulate); }
            public int extractEnergy(int n,boolean simulate) { return node.energy.extract(n,simulate); }
            public int getEnergyStored() { return node.energy.stored(); }
            public int getMaxEnergyStored() { return LegacyEnergy.CAPACITY; }
            public boolean canExtract() { return node.fluidServer() && !node.energy.blocked() && LegacyPL4.enabled && LegacyPL4.energyEnabled && !node.fluidPowered(); }
            public boolean canReceive() { return canExtract(); }
        };
    }
    public static boolean interact(LegacyTile node,EntityPlayer player,ItemStack held) {
        if (!node.fluidServer() || node.role()!=LegacyEnergy.BUFFER || !node.canEdit(player)) return false;
        if (held.isEmpty()) {
            if (player.isSneaking()) node.energy.crank();
            return true;
        }
        if (held.getItem()!=LegacyPL4.ENERGY_CELL) return false;
        if (held.hasTagCompound()) {
            int n = node.energy.restore(held.getTagCompound());
            if (n>0 && !held.getTagCompound().hasKey("PL4Energy")) held.shrink(1);
            player.inventory.markDirty();
        }
        return true;
    }
    public static void drop(LegacyTile node) {
        if (!node.fluidServer()) return;
        NBTTagCompound data = node.energy.recovery(); if (data==null) return;
        ItemStack cell = new ItemStack(LegacyPL4.ENERGY_CELL); cell.setTagCompound(data);
        BlockPos p = node.getPos();
        // Detach first so reentrant removal callbacks cannot issue the same recovery twice.
        node.energy.clearRecovered();
        try {
            if (!node.getWorld().spawnEntity(new EntityItem(node.getWorld(),p.getX()+.5,p.getY()+.5,p.getZ()+.5,cell))) {
                node.energy.read(data); node.fluidDirty(); node.status="Recovery drop rejected by world";
            }
        } catch (RuntimeException error) { node.energy.read(data); node.energy.block("Recovery drop interrupted; inspect before retry"); throw error; }
    }
    private LegacyEnergyPlatform() {}
}
