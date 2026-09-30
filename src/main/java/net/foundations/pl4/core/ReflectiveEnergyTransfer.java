package net.foundations.pl4.core;

import java.lang.reflect.Method;

/** Optional public capability API bridge, resolved once; bound ports live for one network cycle. */
public final class ReflectiveEnergyTransfer {
    private final String unit;
    private final Method insert,extract,count,stored,capacity,inputs,outputs,inputV,inputA,outputV,outputA;
    private final Object simulate,execute;
    private ReflectiveEnergyTransfer(String unit,Method insert,Method extract,Method count,Method stored,Method capacity,Method inputs,Method outputs,Method inputV,Method inputA,Method outputV,Method outputA,Object simulate,Object execute){
        this.unit=unit;this.insert=insert;this.extract=extract;this.count=count;this.stored=stored;this.capacity=capacity;this.inputs=inputs;this.outputs=outputs;this.inputV=inputV;this.inputA=inputA;this.outputV=outputV;this.outputA=outputA;this.simulate=simulate;this.execute=execute;
    }
    public static ReflectiveEnergyTransfer mekanism(Class<?> api)throws ReflectiveOperationException {
        Class<?> action=Class.forName("mekanism.api.Action",false,api.getClassLoader());return mekanism(api,action);
    }
    /** Explicit action interface also permits dependency-free contract fixtures. */
    public static ReflectiveEnergyTransfer mekanism(Class<?> api,Class<?> action)throws ReflectiveOperationException {
        Object sim=action.getField("SIMULATE").get(null),exec=action.getField("EXECUTE").get(null);
        Method insert=api.getMethod("insertEnergy",long.class,action),extract=api.getMethod("extractEnergy",long.class,action);
        requireLong(insert);requireLong(extract);
        return new ReflectiveEnergyTransfer("J",insert,extract,api.getMethod("getEnergyContainerCount"),null,null,null,null,null,null,null,null,sim,exec);
    }
    public static ReflectiveEnergyTransfer gregtech(Class<?> api,Class<?> direction)throws ReflectiveOperationException {
        Method insert=api.getMethod("acceptEnergyFromNetwork",direction,long.class,long.class),extract=api.getMethod("removeEnergy",long.class);
        requireLong(insert);requireLong(extract);
        return new ReflectiveEnergyTransfer("EU",insert,extract,null,api.getMethod("getEnergyStored"),api.getMethod("getEnergyCapacity"),api.getMethod("inputsEnergy",direction),api.getMethod("outputsEnergy",direction),api.getMethod("getInputVoltage"),api.getMethod("getInputAmperage"),api.getMethod("getOutputVoltage"),api.getMethod("getOutputAmperage"),null,null);
    }
    private static void requireLong(Method method){if(method.getReturnType()!=long.class)throw new IllegalArgumentException("Unsupported native energy API version");}
    private static Object call(Method method,Object handler,Object... args){
        try{return method.invoke(handler,args);}catch(ReflectiveOperationException e){throw new IllegalStateException("Native energy capability failed",e);}
    }
    private static long number(Method method,Object handler){Object value=call(method,handler);if(!(value instanceof Number n)||n.longValue()<0)throw new IllegalStateException("Negative/nonnumeric native energy value");return n.longValue();}
    private static long checked(long value,long offered){if(value<0||value>offered)throw new IllegalStateException("Native energy provider exceeded request");return value;}
    public EnergyConversion.Port bind(Object handler,Object side,long voltage,int maximumContainers){
        if(handler==null)return null;
        if(unit.equals("J")){
            long n=number(count,handler);if(n==0||n>maximumContainers)return null;
            return new EnergyConversion.Port(){
                public long extract(long amount,boolean sim){return checked(((Number)call(extract,handler,amount,sim?simulate:execute)).longValue(),amount);}
                public long insert(long amount,boolean sim){long rest=checked(((Number)call(insert,handler,amount,sim?simulate:execute)).longValue(),amount);return amount-rest;}
            };
        }
        if(voltage<1)throw new IllegalArgumentException("Invalid EU voltage");
        return new EnergyConversion.Port(){
            long extracted,insertedAmps;
            public long extract(long amount,boolean sim){
                if(amount<=0||!Boolean.TRUE.equals(call(outputs,handler,side)))return 0;
                long v=number(outputV,handler),a=number(outputA,handler);
                long allowance=v==0||a==0?0:(a>Long.MAX_VALUE/v?Long.MAX_VALUE:a*v);
                long offered=Math.min(amount,Math.min(number(stored,handler),Math.max(0,allowance-extracted)));
                if(sim)return offered;
                long actual=checked(((Number)call(extract,handler,offered)).longValue(),offered);extracted+=actual;return actual;
            }
            public long insert(long amount,boolean sim){
                if(amount<voltage||!Boolean.TRUE.equals(call(inputs,handler,side))||voltage>number(inputV,handler))return 0;
                long room=Math.max(0,number(capacity,handler)-number(stored,handler));
                long amps=Math.min(amount/voltage,Math.min(room/voltage,Math.max(0,number(inputA,handler)-insertedAmps)));
                if(amps==0)return 0;if(sim)return amps*voltage;
                long used=checked(((Number)call(insert,handler,side,voltage,amps)).longValue(),amps);insertedAmps+=used;return used*voltage;
            }
        };
    }
}
