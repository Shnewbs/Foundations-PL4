package net.foundations.pl4.legacy;

/** Integer energy transport. Mutating foreign calls are quarantined until their result is known. */
public final class ConservingEnergy {
    public static final int MAX_OPERATION = 100000;
    public interface Port {
        Object identity();
        int extract(int maximum, boolean simulate);
        int receive(int maximum, boolean simulate);
    }
    public interface Escrow {
        int get();
        void set(int amount);
        boolean blocked();
        void block(String reason);
        void clearBlock();
    }
    private static void budget(int amount) {
        if (amount < 0 || amount > MAX_OPERATION) throw new IllegalArgumentException("Invalid energy budget");
    }
    private static int result(int value, int maximum) {
        if (value < 0 || value > maximum) throw new IllegalStateException("Energy provider returned an invalid amount");
        return value;
    }
    public static int flush(Escrow escrow, Port sink, int limit, ConservingItems.Work work) {
        budget(limit);
        if (escrow.blocked() || escrow.get() <= 0 || sink == null || limit == 0 || !work.take()) return 0;
        int offered = Math.min(escrow.get(), limit);
        int accepted = result(sink.receive(offered, true), offered);
        if (accepted == 0 || !work.take()) return 0;
        escrow.block("Energy insertion interrupted or result invalid; manual inspection required");
        int moved = result(sink.receive(accepted, false), accepted);
        escrow.set(escrow.get() - moved);
        escrow.clearBlock();
        return moved;
    }
    public static int move(Escrow escrow, Port source, Port sink, int limit, ConservingItems.Work work) {
        budget(limit);
        if (escrow.blocked() || limit == 0 || sink == null
                || source != null && source.identity() == sink.identity()) return 0;
        if (escrow.get() > 0) return flush(escrow, sink, limit, work);
        if (source == null || !work.take()) return 0;
        int available = result(source.extract(limit, true), limit);
        if (available == 0 || !work.take()) return 0;
        int accepted = result(sink.receive(available, true), available);
        if (accepted == 0 || !work.take()) return 0;
        escrow.block("Energy extraction interrupted or result invalid; manual inspection required");
        int extracted = source.extract(accepted, false);
        // Preserve even an oversized positive report, but do not forward or replay it.
        if (extracted > 0) escrow.set(extracted);
        result(extracted, accepted);
        escrow.clearBlock();
        return extracted == 0 ? 0 : flush(escrow, sink, limit, work);
    }
    private ConservingEnergy() {}
}
