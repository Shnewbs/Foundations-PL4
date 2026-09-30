package net.foundations.pl4.core;

import java.lang.reflect.*;

/** Optional API bridge. Resolves ONLY the documented read methods once, using the public
 * capability interface rather than implementation classes. No classpath dependency on other mods,
 * no NBT guessing, no insertion/extraction/setter calls, and no cached world/handler references.
 */
public final class ReflectiveEnergyAccess {
    private final String provider,unit;
    private final Method stored,capacity,count,info,hidden;
    private ReflectiveEnergyAccess(String provider,String unit,Method stored,Method capacity,Method count,Method info,Method hidden){
        this.provider=provider;this.unit=unit;this.stored=stored;this.capacity=capacity;this.count=count;this.info=info;this.hidden=hidden;
    }
    public static ReflectiveEnergyAccess mekanism(Class<?> api)throws ReflectiveOperationException{
        return new ReflectiveEnergyAccess("mekanism","J",api.getMethod("getEnergy",int.class),api.getMethod("getMaxEnergy",int.class),api.getMethod("getEnergyContainerCount"),null,null);
    }
    public static ReflectiveEnergyAccess gregtech(Class<?> api)throws ReflectiveOperationException{
        return new ReflectiveEnergyAccess("gtceu","EU",api.getMethod("getEnergyStored"),api.getMethod("getEnergyCapacity"),null,null,null);
    }
    public static ReflectiveEnergyAccess gregtechInfo(Class<?> api)throws ReflectiveOperationException{
        Method info=api.getMethod("getEnergyInfo");Class<?> result=info.getReturnType();
        return new ReflectiveEnergyAccess("gtceu_info","EU",result.getMethod("stored"),result.getMethod("capacity"),null,info,api.getMethod("isOneProbeHidden"));
    }
    public EnergyValues.Reading read(Object handler,int maximumContainers)throws ReflectiveOperationException{
        if(handler==null)return null;
        if(hidden!=null&&Boolean.TRUE.equals(hidden.invoke(handler)))return null;
        Object value=info==null?handler:info.invoke(handler);if(value==null)return null;
        double amount=0,limit=0;
        if(count!=null){
            Object raw=count.invoke(handler);if(!(raw instanceof Number number))throw new IllegalArgumentException("Nonnumeric container count");
            long n=number.longValue();if(n<0||n>maximumContainers)throw new IllegalArgumentException("Energy container count exceeds limit");if(n==0)return null;
            for(int i=0;i<n;i++){amount=EnergyValues.add(amount,number(stored.invoke(value,i)));limit=EnergyValues.add(limit,number(capacity.invoke(value,i)));}
        }else{amount=number(stored.invoke(value));limit=number(capacity.invoke(value));}
        return new EnergyValues.Reading(provider,unit,amount,limit);
    }
    private static double number(Object value){
        if(!(value instanceof Number n))throw new IllegalArgumentException("Energy API returned a nonnumeric value");
        double result=n.doubleValue();if(!Double.isFinite(result)||result<0)throw new IllegalArgumentException("Energy API returned invalid telemetry");return result;
    }
}
