package net.foundations.pl4.core;

import java.util.*;

/** Deterministic layout donor choice. No world, packet, or client references are retained here. */
public final class CanvasContinuity {
    public record Candidate(String id, long revision, boolean configured, boolean previousController, boolean topLeft) {
        public Candidate { Objects.requireNonNull(id); if (revision < 0) throw new IllegalArgumentException("Negative revision"); }
    }
    public static String donor(Collection<Candidate> candidates) {
        return candidates.stream().max(Comparator.comparingLong(Candidate::revision)
            .thenComparing(Candidate::configured).thenComparing(Candidate::previousController)
            .thenComparing(Candidate::topLeft).thenComparing(Candidate::id, Comparator.reverseOrder()))
            .orElseThrow(() -> new IllegalArgumentException("Empty canvas")).id();
    }
    public static long next(long greatest) {
        if (greatest < 0 || greatest == Long.MAX_VALUE) throw new IllegalStateException("Invalid layout revision");
        return greatest+1;
    }
    private CanvasContinuity() {}
}
