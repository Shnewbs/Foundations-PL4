package net.foundations.pl4.legacy;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;

/** Shared native energy storage and transport; no foreign unit conversion is performed. */
public final class LegacyEnergy {
    public static final int CAPACITY = 100000;
    public static final int EXPORT = 6, IMPORT = 7, BUFFER = 8;
    public static final class State implements ConservingEnergy.Escrow, ConservingEnergy.Port {
        private final LegacyTile tile;
        private int stored, pending;
        private String fault = "";
        private NBTTagCompound opaque;
        private long lastCrank = Long.MIN_VALUE;
        public State(LegacyTile tile) { this.tile = tile; }
        public Object identity() { return this; }
        public int get() { return pending; }
        public int stored() { return stored; }
        public void set(int amount) {
            if (amount < 0) throw new IllegalArgumentException("Negative energy escrow");
            pending = amount; tile.fluidDirty();
        }
        public boolean blocked() { return opaque != null || !fault.isEmpty(); }
        public void block(String reason) { fault = reason; tile.fluidDirty(); }
        public void clearBlock() { if (opaque == null) fault = ""; tile.fluidDirty(); }
        private boolean usable() {
            return tile.fluidServer() && tile.role() == BUFFER && !blocked()
                    && LegacyPL4.enabled && LegacyPL4.energyEnabled && !tile.fluidPowered();
        }
        public int receive(int maximum, boolean simulate) {
            if (maximum <= 0 || !usable()) return 0;
            int n = Math.min(maximum, CAPACITY - stored);
            if (!simulate && n > 0) { stored += n; tile.fluidDirty(); }
            return n;
        }
        public int extract(int maximum, boolean simulate) {
            if (maximum <= 0 || !usable()) return 0;
            int n = Math.min(maximum, stored);
            if (!simulate && n > 0) { stored -= n; tile.fluidDirty(); }
            return n;
        }
        /** Built-in manual generator: bounded per buffer, server-side, and persisted across reload. */
        public int crank() {
            if (!usable() || LegacyPL4.manualEnergy <= 0) return 0;
            long now = LegacyEnergyPlatform.time(tile);
            if (lastCrank != Long.MIN_VALUE && now >= lastCrank && now - lastCrank < 10) return 0;
            int n = receive(LegacyPL4.manualEnergy, false);
            if (n > 0) { lastCrank = now; tile.fluidDirty(); }
            return n;
        }
        private static boolean type(NBTTagCompound data, String name, int id) {
            return data.hasKey(name) && data.getTag(name).getId() == id;
        }
        private static boolean valid(NBTTagCompound data) {
            return type(data,"Schema",3) && data.getInteger("Schema") == 1
                    && type(data,"Unit",8) && LegacyEnergyPlatform.UNIT.equals(data.getString("Unit"))
                    && type(data,"Stored",3) && type(data,"Escrow",3)
                    && data.getInteger("Stored") >= 0 && data.getInteger("Stored") <= CAPACITY
                    && data.getInteger("Escrow") >= 0 && data.getInteger("Escrow") <= CAPACITY
                    && (!data.hasKey("Fault") || type(data,"Fault",8))
                    && (!data.hasKey("LastCrank") || type(data,"LastCrank",4));
        }
        public void write(NBTTagCompound out) {
            if (opaque != null) { out.setTag("PL4Energy", opaque.copy()); return; }
            NBTTagCompound data = new NBTTagCompound();
            data.setInteger("Schema",1); data.setString("Unit",LegacyEnergyPlatform.UNIT);
            data.setInteger("Stored",stored); data.setInteger("Escrow",pending);
            if (!fault.isEmpty()) data.setString("Fault",fault);
            data.setLong("LastCrank",lastCrank); out.setTag("PL4Energy",data);
        }
        public void read(NBTTagCompound in) {
            stored = pending = 0; fault = ""; opaque = null; lastCrank = Long.MIN_VALUE;
            if (!in.hasKey("PL4Energy")) return;
            // A malformed outer tag is retained inside an explicitly blocked diagnostic envelope.
            if (!type(in,"PL4Energy",10)) {
                opaque = new NBTTagCompound(); opaque.setTag("Unresolved",in.getTag("PL4Energy").copy());
                fault = "Malformed energy data retained"; return;
            }
            NBTTagCompound data = in.getCompoundTag("PL4Energy");
            if (!valid(data)) { opaque = (NBTTagCompound)data.copy(); fault = "Unresolved energy data retained"; return; }
            stored = data.getInteger("Stored"); pending = data.getInteger("Escrow"); fault = data.getString("Fault");
            if (data.hasKey("LastCrank")) lastCrank = data.getLong("LastCrank");
        }
        public String diagnostic() {
            return stored + "/" + CAPACITY + " " + LegacyEnergyPlatform.UNIT + " | Escrow " + pending
                    + (blocked() ? " | QUARANTINED: " + (opaque != null ? "unresolved data" : fault) : "");
        }
        public NBTTagCompound recovery() {
            if (stored == 0 && pending == 0 && !blocked()) return null;
            NBTTagCompound data = new NBTTagCompound(); write(data); return data;
        }
        public void clearRecovered() { stored = pending = 0; fault = ""; opaque = null; tile.fluidDirty(); }
        /** Recovery is only into our own synchronous storage, never an uncertain foreign provider. */
        public int restore(NBTTagCompound cell) {
            if (cell == null || !type(cell,"PL4Energy",10)) return 0;
            NBTTagCompound data = cell.getCompoundTag("PL4Energy");
            if (!valid(data) || !data.getString("Fault").isEmpty()) return 0;
            int a = data.getInteger("Stored"), b = data.getInteger("Escrow");
            int n = receive((int)Math.min(CAPACITY,(long)a+b),false);
            if (n > 0) {
                int fromEscrow = Math.min(b,n); b -= fromEscrow; a -= n-fromEscrow;
                if (a == 0 && b == 0) cell.removeTag("PL4Energy");
                else { data.setInteger("Stored",a); data.setInteger("Escrow",b); }
            }
            return n;
        }
    }
    public static int transfer(LegacyTile node) {
        if (!node.fluidServer() || node.role() != EXPORT || node.owner.isEmpty() || !LegacyPL4.enabled
                || !LegacyPL4.energyEnabled || node.fluidPowered() || node.energy.blocked()) return 0;
        BoundedNetwork.Plan plan = node.network();
        if (plan.overflow) { node.status = "Network exceeds configured host limit"; return 0; }
        ConservingItems.Work work = new ConservingItems.Work(64);
        ConservingEnergy.Port source = node.energy.get() > 0 ? null : LegacyEnergyPlatform.resolve(node,work);
        if (source == null && node.energy.get() == 0) { node.status = "No loaded sided energy source"; return 0; }
        Set<BoundedNetwork.Point> seen = new HashSet<BoundedNetwork.Point>(); seen.add(node.fluidTarget());
        Set<Object> identities = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object,Boolean>());
        if (source != null) identities.add(source.identity());
        int moved = 0;
        for (BoundedNetwork.Point p : plan.nodes) {
            if (!work.available()) break;
            LegacyTile sink = node.fluidNode(p);
            if (sink == null || sink.role() != IMPORT || !node.owner.equals(sink.owner)
                    || sink.fluidPowered() || !seen.add(sink.fluidTarget())) continue;
            ConservingEnergy.Port dest = LegacyEnergyPlatform.resolve(sink,work);
            if (dest == null || !identities.add(dest.identity())) continue;
            moved += ConservingEnergy.move(node.energy,source,dest,LegacyPL4.energyRate-moved,work);
            if (moved >= LegacyPL4.energyRate || node.energy.blocked()) break;
        }
        node.status = moved > 0 ? "Moved " + moved + " " + LegacyEnergyPlatform.UNIT : "Waiting for energy or receiving space";
        return moved;
    }
    public static String describe(LegacyTile node) {
        return "PL4 " + (node.role()==EXPORT ? "Energy Export" : node.role()==IMPORT ? "Energy Import" : "Energy Buffer")
                + " | Side " + node.side + " | " + node.energy.diagnostic() + " | " + node.status;
    }
    private LegacyEnergy() {}
}
