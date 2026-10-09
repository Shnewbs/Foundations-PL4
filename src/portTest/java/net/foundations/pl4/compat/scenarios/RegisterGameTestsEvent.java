package net.foundations.pl4.compat.scenarios;
public final class RegisterGameTestsEvent {
 private final java.util.List<Class<?>> fixtures=new java.util.ArrayList<>();
 public void register(Class<?> type){if(fixtures.contains(type))throw new IllegalStateException("Duplicate fixture");fixtures.add(type);}
 public java.util.List<Class<?>> fixtures(){return java.util.List.copyOf(fixtures);}
}
