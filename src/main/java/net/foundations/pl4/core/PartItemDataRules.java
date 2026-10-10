package net.foundations.pl4.core;

/** Item-form persistence policy. Runtime identity must not make otherwise identical drops unique. */
public final class PartItemDataRules {
    // Pre-1.16 NBT writes UUIDs as two long fields. Remove both representations so saved
    // configurations never retain owner/identity, including data migrated from other formats.
    public static final String[] VOLATILE_KEYS={"identity","identityMost","identityLeast","owner","ownerMost","ownerLeast","signal","ticks","layoutRevision"};
    public static boolean needsEscrowPayload(boolean itemPresent,boolean fluidPresent,int energy){return itemPresent||fluidPresent||energy>0;}
    private PartItemDataRules(){}
}
