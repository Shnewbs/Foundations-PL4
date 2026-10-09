package net.foundations.pl4.compat;
public final class PortLists {
 public static <T> T last(java.util.List<T> list){if(list.isEmpty())throw new java.util.NoSuchElementException();return list.get(list.size()-1);}
 public static <T> T removeLast(java.util.List<T> list){if(list.isEmpty())throw new java.util.NoSuchElementException();return list.remove(list.size()-1);}
 public static <T> java.util.List<T> reversed(java.util.List<T> list){var copy=new java.util.ArrayList<>(list);java.util.Collections.reverse(copy);return copy;}
 private PortLists(){}
}
