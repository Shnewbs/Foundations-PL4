package net.foundations.pl4.compat;
/** Identical assertion semantics for native GameTestHelper versions without assertTrue. */
public final class PortAssertions {
 public static void check(boolean value,String message){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
 private PortAssertions(){}
}
