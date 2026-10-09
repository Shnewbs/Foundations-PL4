package net.foundations.pl4.compat;
public final class PortAssertions {public static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}private PortAssertions(){}}
