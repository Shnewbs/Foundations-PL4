package net.foundations.pl4.legacy;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

/** Shared native fluid behavior; platform adapters resolve only the requested loaded side. */
public final class LegacyFluids {
    public static final int CAPACITY = 16000;
    public static final ConservingItems.Stacks<FluidStack> STACKS = new ConservingItems.Stacks<FluidStack>() {
        public boolean empty(FluidStack value) { return value == null || value.amount == 0; }
        public int count(FluidStack value) { return value == null ? 0 : value.amount; }
        public boolean same(FluidStack a, FluidStack b) { return a != null && b != null && a.isFluidEqual(b); }
        public FluidStack copy(FluidStack value, int count) {
            if (count == 0) return null;
            FluidStack copy = value.copy(); copy.amount = count; return copy;
        }
    };
    public static final class State implements ConservingFluids.Escrow<FluidStack>, ConservingFluids.Port<FluidStack> {
        private final LegacyTile tile;
        private FluidStack pending, stored;
        private NBTTagCompound unknownPending, unknownStored;
        private String fault = "";
        public State(LegacyTile tile) { this.tile = tile; }
        public Object identity() { return this; }
        public FluidStack get() { return pending; }
        public void set(FluidStack value) { pending = value == null ? null : value.copy(); unknownPending = null; tile.fluidDirty(); }
        public boolean blocked() { return !fault.isEmpty() || unknownPending != null; }
        public void block(String reason) { fault = reason; tile.fluidDirty(); }
        public void clearBlock() { fault = ""; tile.fluidDirty(); }
        public int storedAmount() { return stored == null ? 0 : stored.amount; }
        public FluidStack contents() { return stored == null ? null : stored.copy(); }
        public String diagnostic() {
            return "Tank " + storedAmount() + "/" + CAPACITY + " mB | Escrow " + STACKS.count(pending)
                    + " mB" + (unknownPending != null || unknownStored != null ? " | Unresolved fluid retained" : "")
                    + (fault.isEmpty() ? "" : " | QUARANTINED: " + fault);
        }
        public int fill(FluidStack offered, boolean simulate) {
            if (tile.role() != 5 || tile.isInvalid() || blocked() || unknownStored != null || offered == null || offered.amount <= 0) return 0;
            if (stored != null && !stored.isFluidEqual(offered)) return 0;
            int n = Math.min(offered.amount, Math.max(0, CAPACITY - storedAmount()));
            if (!simulate && n > 0) { stored = STACKS.copy(offered, storedAmount() + n); tile.fluidDirty(); }
            return n;
        }
        public FluidStack drain(int amount, boolean simulate) {
            if (tile.role() != 5 || tile.isInvalid() || blocked() || stored == null || amount <= 0) return null;
            int n = Math.min(amount, stored.amount); FluidStack result = STACKS.copy(stored, n);
            if (!simulate) { stored = STACKS.copy(stored, stored.amount - n); tile.fluidDirty(); }
            return result;
        }
        public FluidStack drain(FluidStack requested, boolean simulate) {
            return requested != null && stored != null && stored.isFluidEqual(requested) ? drain(requested.amount, simulate) : null;
        }
        private static NBTTagCompound copy(NBTTagCompound tag) { return (NBTTagCompound)tag.copy(); }
        private static void writeFluid(NBTTagCompound out, String key, FluidStack value, NBTTagCompound unresolved) {
            if (unresolved != null) out.setTag(key, copy(unresolved));
            else if (value != null) out.setTag(key, value.writeToNBT(new NBTTagCompound()));
            else out.removeTag(key);
        }
        private static FluidStack decode(NBTTagCompound tag) {
            try { FluidStack value = FluidStack.loadFluidStackFromNBT(tag); return value != null && value.amount > 0 ? value : null; }
            catch (RuntimeException ignored) { return null; }
        }
        public void write(NBTTagCompound tag) {
            writeFluid(tag, "FluidEscrow", pending, unknownPending); writeFluid(tag, "FluidTank", stored, unknownStored);
            if (!fault.isEmpty()) tag.setString("FluidFault", fault); else tag.removeTag("FluidFault");
        }
        public void read(NBTTagCompound tag) {
            pending = tag.hasKey("FluidEscrow") ? decode(tag.getCompoundTag("FluidEscrow")) : null;
            stored = tag.hasKey("FluidTank") ? decode(tag.getCompoundTag("FluidTank")) : null;
            unknownPending = tag.hasKey("FluidEscrow") && pending == null ? copy(tag.getCompoundTag("FluidEscrow")) : null;
            unknownStored = tag.hasKey("FluidTank") && stored == null ? copy(tag.getCompoundTag("FluidTank")) : null;
            fault = tag.getString("FluidFault");
            if (pending != null && pending.amount > CAPACITY) fault = "Oversized provider extraction; manual inspection required";
        }
        /** Recovery items hold opaque NBT, including unknown fluid identities and uncertainty flags. */
        public java.util.List<NBTTagCompound> recovery() {
            java.util.List<NBTTagCompound> cells = new java.util.ArrayList<NBTTagCompound>();
            for (int i = 0; i < 2; i++) {
                FluidStack value = i == 0 ? pending : stored; NBTTagCompound raw = i == 0 ? unknownPending : unknownStored;
                if (value == null && raw == null) continue;
                NBTTagCompound cell = new NBTTagCompound();
                cell.setTag("Contents", raw != null ? copy(raw) : value.writeToNBT(new NBTTagCompound()));
                if (!fault.isEmpty()) cell.setString("Fault", fault);
                cells.add(cell);
            }
            if (cells.isEmpty() && !fault.isEmpty()) { NBTTagCompound cell = new NBTTagCompound(); cell.setString("Fault", fault); cells.add(cell); }
            return cells;
        }
        public void clearRecovered() { pending = null; stored = null; unknownPending = null; unknownStored = null; fault = ""; tile.fluidDirty(); }
        /** Only a healthy, resolvable cell can restore into the built-in tank; no forced recovery. */
        public int restore(NBTTagCompound cell) {
            if (cell == null || !cell.getString("Fault").isEmpty() || !cell.hasKey("Contents")) return 0;
            FluidStack offered = decode(cell.getCompoundTag("Contents")); if (offered == null) return 0;
            int n = fill(offered, false);
            if (n > 0) {
                if (n == offered.amount) cell.removeTag("Contents");
                else cell.setTag("Contents", STACKS.copy(offered, offered.amount - n).writeToNBT(new NBTTagCompound()));
            }
            return n;
        }
    }
    public static int transfer(LegacyTile node) {
        if (!node.fluidServer() || node.role() != 3 || node.owner.isEmpty() || !LegacyPL4.enabled
                || !LegacyPL4.fluidsEnabled || node.fluidPowered() || node.fluid.blocked()) return 0;
        BoundedNetwork.Plan plan = node.network();
        if (plan.overflow) { node.status = "Network exceeds configured host limit"; return 0; }
        ConservingFluids.Port<FluidStack> source = node.fluids();
        if (source == null && node.fluid.get() == null) { node.status = "No loaded sided fluid source"; return 0; }
        ConservingItems.Work work = new ConservingItems.Work(64);
        Set<BoundedNetwork.Point> seen = new HashSet<BoundedNetwork.Point>(); seen.add(node.fluidTarget());
        Set<Object> providers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object,Boolean>());
        if (source != null) providers.add(source.identity());
        int moved = 0;
        for (BoundedNetwork.Point p : plan.nodes) {
            LegacyTile sink = node.fluidNode(p);
            if (sink == null || sink.role() != 4 || !node.owner.equals(sink.owner) || sink.fluidPowered()) continue;
            if (!seen.add(sink.fluidTarget())) continue;
            ConservingFluids.Port<FluidStack> destination = sink.fluids();
            if (destination == null || !providers.add(destination.identity())) continue;
            moved += ConservingFluids.move(STACKS, node.fluid, source, destination, LegacyPL4.fluidRate - moved, work);
            if (moved >= LegacyPL4.fluidRate || !work.available() || node.fluid.blocked()) break;
        }
        node.status = moved > 0 ? "Moved " + moved + " mB" : "Waiting for fluid or receiving space";
        return moved;
    }
    public static String describe(LegacyTile node) {
        return "PL4 " + (node.role() == 3 ? "Fluid Export" : node.role() == 4 ? "Fluid Import" : "Tank")
                + " | Side " + node.side + " | " + node.fluid.diagnostic() + " | " + node.status;
    }
    private LegacyFluids() {}
}
