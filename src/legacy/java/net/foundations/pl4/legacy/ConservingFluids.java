package net.foundations.pl4.legacy;

/** Java-7-compatible fluid transfer contract. A failed execute call is quarantined, never retried. */
public final class ConservingFluids {
    public interface Port<S> {
        Object identity();
        S drain(int amount, boolean simulate);
        S drain(S requested, boolean simulate);
        int fill(S offered, boolean simulate);
    }
    public interface Escrow<S> {
        S get();
        void set(S value);
        boolean blocked();
        void block(String reason);
        void clearBlock();
    }
    private static void validate(int budget) {
        if (budget < 0 || budget > 16000) throw new IllegalArgumentException("Invalid fluid budget");
    }
    private static int accepted(int value, int offered) {
        if (value < 0 || value > offered) throw new IllegalStateException("Malformed fluid fill result");
        return value;
    }
    public static <S> int flush(ConservingItems.Stacks<S> s, Escrow<S> e, Port<S> sink,
                                int budget, ConservingItems.Work work) {
        validate(budget);
        S pending = e.get();
        if (e.blocked() || s.empty(pending) || sink == null || budget == 0 || !work.take()) return 0;
        int offered = Math.min(s.count(pending), budget);
        int capacity = accepted(sink.fill(s.copy(pending, offered), true), offered);
        if (capacity == 0 || !work.take()) return 0;
        // Record the uncertain stage BEFORE calling foreign code. Exceptions/invalid results leave it set.
        e.block("Fluid insertion interrupted or provider result invalid; inspect before recovery");
        int moved = accepted(sink.fill(s.copy(pending, capacity), false), capacity);
        e.set(s.count(pending) == moved ? null : s.copy(pending, s.count(pending) - moved));
        e.clearBlock();
        return moved;
    }
    public static <S> int move(ConservingItems.Stacks<S> s, Escrow<S> e, Port<S> source,
                               Port<S> sink, int budget, ConservingItems.Work work) {
        validate(budget);
        if (e.blocked() || budget == 0 || sink == null || (source != null && source.identity() == sink.identity())) return 0;
        if (!s.empty(e.get())) return flush(s, e, sink, budget, work);
        if (source == null || !work.take()) return 0;
        S trial = source.drain(budget, true);
        if (s.empty(trial)) return 0;
        if (s.count(trial) < 1 || s.count(trial) > budget) throw new IllegalStateException("Malformed fluid drain simulation");
        if (!work.take()) return 0;
        int amount = accepted(sink.fill(s.copy(trial, s.count(trial)), true), s.count(trial));
        if (amount == 0 || !work.take()) return 0;
        e.block("Fluid extraction interrupted or provider result invalid; inspect before recovery");
        S drained = source.drain(s.copy(trial, amount), false);
        if (s.empty(drained)) { e.clearBlock(); return 0; }
        e.set(s.copy(drained, s.count(drained)));
        if (!s.same(trial, drained) || s.count(drained) < 1 || s.count(drained) > amount) return 0;
        e.clearBlock();
        return flush(s, e, sink, budget, work);
    }
    private ConservingFluids() {}
}
