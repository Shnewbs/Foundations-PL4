package net.foundations.pl4.legacy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** PL4-owned energy fallback for 1.6.4. Does not impersonate the absent Forge Energy API. */
public final class LegacyEnergyPlatform {
    public static final String UNIT = "PL4";
    public static long time(LegacyTile node) { return node.worldObj.getTotalWorldTime(); }
    public static ConservingEnergy.Port resolve(final LegacyTile node, final ConservingItems.Work work) {
        if (!node.fluidServer() || node.side<0 || node.side>5) return null;
        final BoundedNetwork.Point p = node.fluidTarget();
        if (!node.worldObj.blockExists(p.x,p.y,p.z)) return null;
        final TileEntity target = node.worldObj.getBlockTileEntity(p.x,p.y,p.z);
        if (target==null || target.isInvalid()) return null;
        if (target instanceof LegacyTile) {
            LegacyTile own=(LegacyTile)target;
            return own.role()==LegacyEnergy.BUFFER && node.owner.equals(own.owner) ? own.energy : null;
        }
        if (!(target instanceof LegacyEnergyAccess) || !work.take()) return null;
        final ConservingEnergy.Port port=((LegacyEnergyAccess)target).pl4EnergyPort(node.side^1);
        if (port==null) return null;
        return new ConservingEnergy.Port() {
            public Object identity() { return port.identity(); }
            private boolean live() { return node.fluidServer() && !target.isInvalid() && node.worldObj.blockExists(p.x,p.y,p.z) && node.worldObj.getBlockTileEntity(p.x,p.y,p.z)==target; }
            public int extract(int n,boolean simulate) { return live()?port.extract(n,simulate):0; }
            public int receive(int n,boolean simulate) { return live()?port.receive(n,simulate):0; }
        };
    }
    public static boolean interact(LegacyTile node,EntityPlayer player,ItemStack held) {
        if (!node.fluidServer() || node.role()!=LegacyEnergy.BUFFER || !node.canEdit(player)) return false;
        if (held==null || held.stackSize<=0) { if(player.isSneaking())node.energy.crank(); return true; }
        if (held.itemID!=LegacyPL4.ENERGY_CELL.itemID) return false;
        if (held.hasTagCompound()) {
            int n=node.energy.restore(held.getTagCompound());
            if(n>0 && !held.getTagCompound().hasKey("PL4Energy")) {
                held.stackSize--;
                if(held.stackSize<=0 && player.getCurrentEquippedItem()==held)player.inventory.setInventorySlotContents(player.inventory.currentItem,null);
            }
            player.inventory.onInventoryChanged();
        }
        return true;
    }
    public static void drop(LegacyTile node) {
        if(!node.fluidServer())return;
        NBTTagCompound data=node.energy.recovery();if(data==null)return;
        ItemStack cell=new ItemStack(LegacyPL4.ENERGY_CELL);cell.setTagCompound(data);
        node.energy.clearRecovered();
        try {
            if(!node.worldObj.spawnEntityInWorld(new EntityItem(node.worldObj,node.xCoord+.5,node.yCoord+.5,node.zCoord+.5,cell))) {
                node.energy.read(data);node.fluidDirty();node.status="Recovery drop rejected by world";
            }
        } catch(RuntimeException error) { node.energy.read(data);node.energy.block("Recovery drop interrupted; inspect before retry");throw error; }
    }
    private LegacyEnergyPlatform(){}
}
