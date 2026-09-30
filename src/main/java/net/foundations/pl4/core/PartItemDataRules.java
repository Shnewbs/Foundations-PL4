package net.foundations.pl4.core;

/** Item-form persistence policy. Runtime identity/state must never make otherwise identical drops unique. */
public final class PartItemDataRules {
    public static final String[] VOLATILE_KEYS={"identity","owner","signal","ticks","layoutRevision"};
    public static boolean needsEscrowPayload(boolean itemPresent,boolean fluidPresent,int energy){return itemPresent||fluidPresent||energy>0;}
    private PartItemDataRules(){}
}
