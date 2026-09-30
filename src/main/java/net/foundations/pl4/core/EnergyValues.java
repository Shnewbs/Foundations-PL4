package net.foundations.pl4.core;

import java.util.*;

/** Read-only telemetry: native units are never converted, added together, or sent to TransferEngine. */
public final class EnergyValues {
    public record Reading(String provider,String unit,double stored,double capacity){
        public Reading {
            if(provider==null||provider.isBlank()||!Set.of("FE","EU","J").contains(unit))throw new IllegalArgumentException("Unknown energy unit/provider");
            if(!Double.isFinite(stored)||!Double.isFinite(capacity)||stored<0||capacity<0)throw new IllegalArgumentException("Invalid energy telemetry");
        }
    }
    public static String system(String value){return Set.of("AUTO","FE","EU","J").contains(value)?value:"AUTO";}
    public static boolean accepts(String requested,String unit){return system(requested).equals("AUTO")||requested.equals(unit);}
    public static double add(double a,double b){double result=a+b;if(!Double.isFinite(result))throw new IllegalArgumentException("Energy total exceeds telemetry range");return result;}
    public static String totalKey(String unit){return unit.equals("FE")?"storage":"storage:"+unit.toLowerCase(Locale.ROOT);}
    public static Map<String,Reading> totals(Collection<Reading> readings){
        Map<String,Reading> result=new TreeMap<>();
        for(Reading reading:readings){Reading old=result.get(reading.unit);result.put(reading.unit,old==null?new Reading("total",reading.unit,reading.stored,reading.capacity):new Reading("total",reading.unit,add(old.stored,reading.stored),add(old.capacity,reading.capacity)));}
        return Collections.unmodifiableMap(result);
    }
    private EnergyValues(){}
}
