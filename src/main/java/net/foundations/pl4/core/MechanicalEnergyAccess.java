package net.foundations.pl4.core;

import java.lang.reflect.*;

/** Optional, read-only API access. Resolution checks signatures before sampling; no power setters are called. */
public final class MechanicalEnergyAccess {
    public record Kinetic(double rpm,double theoretical,double stress,double capacity,boolean overstressed){}
    public record Grid(Object identity,double stored,double capacity,double usage,double injection,boolean powered){}
    private static double number(Object value){
        if(!(value instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalArgumentException("Invalid provider telemetry");
        return n.doubleValue();
    }
    private static double positive(Object value){double n=number(value);if(n<0)throw new IllegalArgumentException("Negative provider telemetry");return n;}
    public static final class Create {
        private final Class<?> type;private final Method speed,theoretical,overstressed;private final Field stress,capacity;
        public Create(Class<?> type)throws ReflectiveOperationException{
            this.type=type;speed=type.getMethod("getSpeed");theoretical=type.getMethod("getTheoreticalSpeed");overstressed=type.getMethod("isOverStressed");
            stress=type.getDeclaredField("stress");capacity=type.getDeclaredField("capacity");
            if(speed.getReturnType()!=float.class||theoretical.getReturnType()!=float.class||overstressed.getReturnType()!=boolean.class||stress.getType()!=float.class||capacity.getType()!=float.class)throw new NoSuchMethodException("Unsupported Create kinetics API");
            if(!stress.trySetAccessible()||!capacity.trySetAccessible())throw new IllegalAccessException("Create kinetics telemetry inaccessible");
        }
        public Kinetic read(Object target)throws ReflectiveOperationException{
            if(!type.isInstance(target))return null;
            return new Kinetic(number(speed.invoke(target)),number(theoretical.invoke(target)),positive(stress.get(target)),positive(capacity.get(target)),(boolean)overstressed.invoke(target));
        }
    }
    public static final class AE2 {
        private final Class<?> type;private final Method node,grid,energy,stored,capacity,usage,injection,powered;
        public AE2(Class<?> type,Class<?> direction)throws ReflectiveOperationException{
            this.type=type;node=type.getMethod("getGridNode",direction);grid=node.getReturnType().getMethod("getGrid");energy=grid.getReturnType().getMethod("getEnergyService");
            Class<?> service=energy.getReturnType();stored=service.getMethod("getStoredPower");capacity=service.getMethod("getMaxStoredPower");usage=service.getMethod("getAvgPowerUsage");injection=service.getMethod("getAvgPowerInjection");powered=service.getMethod("isNetworkPowered");
            for(Method m:new Method[]{stored,capacity,usage,injection})if(m.getReturnType()!=double.class)throw new NoSuchMethodException("Unsupported AE2 energy API");
            if(powered.getReturnType()!=boolean.class)throw new NoSuchMethodException("Unsupported AE2 power state API");
        }
        public Grid read(Object target,Object side)throws ReflectiveOperationException{
            if(!type.isInstance(target))return null;Object n=node.invoke(target,side);if(n==null)return null;Object g=grid.invoke(n);if(g==null)return null;Object e=energy.invoke(g);if(e==null)return null;
            return new Grid(g,positive(stored.invoke(e)),positive(capacity.invoke(e)),positive(usage.invoke(e)),positive(injection.invoke(e)),(boolean)powered.invoke(e));
        }
    }
    private MechanicalEnergyAccess(){}
}
