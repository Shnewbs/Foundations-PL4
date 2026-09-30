package net.foundations.pl4.core;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Voltaic/Electrodynamics API adapter. Port amounts are microjoules, not Mekanism J. */
public final class ReflectiveElectrodynamicTransfer {
    public static final long MICROJOULES=1000000;
    private final Method receive,extract,pack,joules,voltage,minimum,maximum,ampacity,producer,receiver;
    private ReflectiveElectrodynamicTransfer(Class<?> api,Class<?> transfer)throws ReflectiveOperationException {
        receive=api.getMethod("receivePower",transfer,boolean.class);extract=api.getMethod("extractPower",transfer,boolean.class);
        pack=transfer.getMethod("joulesVoltage",double.class,double.class);joules=transfer.getMethod("getJoules");
        voltage=api.getMethod("getVoltage");minimum=api.getMethod("getMinimumVoltage");maximum=api.getMethod("getMaximumVoltage");ampacity=api.getMethod("getAmpacity");
        producer=api.getMethod("isEnergyProducer");receiver=api.getMethod("isEnergyReceiver");
        if(joules.getReturnType()!=double.class||receive.getReturnType()!=transfer||extract.getReturnType()!=transfer)throw new IllegalArgumentException("Unsupported Voltaic API version");
    }
    public static ReflectiveElectrodynamicTransfer resolve(Class<?> api)throws ReflectiveOperationException {
        return resolve(api,Class.forName("voltaic.prefab.utilities.object.TransferPack",false,api.getClassLoader()));
    }
    public static ReflectiveElectrodynamicTransfer resolve(Class<?> api,Class<?> transfer)throws ReflectiveOperationException {return new ReflectiveElectrodynamicTransfer(api,transfer);}
    private static Object call(Method m,Object handler,Object... args){try{return m.invoke(handler,args);}catch(ReflectiveOperationException e){throw new IllegalStateException("Electrodynamics capability failed",e);}}
    private static double number(Method m,Object handler){double n=((Number)call(m,handler)).doubleValue();if(!Double.isFinite(n))throw new IllegalStateException("Nonfinite Electrodynamics value");return n;}
    public static long microjoules(double value,boolean up){
        if(!Double.isFinite(value)||value<0)throw new IllegalStateException("Invalid Electrodynamics energy response");
        return BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(MICROJOULES)).setScale(0,up?RoundingMode.CEILING:RoundingMode.FLOOR).min(BigDecimal.valueOf(Long.MAX_VALUE)).longValueExact();
    }
    public EnergyConversion.Port bind(Object handler){
        if(handler==null)return null;
        return new EnergyConversion.Port(){
            long extracted,inserted;
            private long move(long amount,boolean simulate,boolean inserting){
                if(amount<=0||!Boolean.TRUE.equals(call(inserting?receiver:producer,handler)))return 0;
                double v=number(voltage,handler),min=number(minimum,handler),max=number(maximum,handler),amps=number(ampacity,handler);
                // Match exact operating voltage: the API's default implementation can claim success
                // without moving anything for undervoltage. Never call it with another voltage.
                if(v!=-1&&v<=0||min!=-1&&v<min||max!=-1&&v>max||amps<0&&amps!=-1)return 0;
                if(amps!=-1){
                    if(v<=0)return 0;
                    // TransferPack.getAmps() = J / V * 20. Bound the burst, not interval-scaled power.
                    long allowance=microjoules(v*amps/20,false);long used=inserting?inserted:extracted;
                    amount=Math.min(amount,Math.max(0,allowance-used));if(amount==0)return 0;
                }
                // Long -> double must never round the request upward.
                double offered=(double)amount/MICROJOULES;
                if(microjoules(offered,true)>amount)offered=Math.nextDown(offered);
                Object request=call(pack,null,offered,v),result=call(inserting?receive:extract,handler,request,simulate);
                if(result==null)throw new IllegalStateException("Null Electrodynamics transfer response");
                double actual=number(joules,result);if(actual<0||actual>offered)throw new IllegalStateException("Electrodynamics response exceeded request");
                // Fractional responses: credit withdrawals down, charge deposits up. This cannot
                // create energy, at the cost of less than one microjoule per actual partial operation.
                long accounted=Math.min(amount,microjoules(actual,inserting));
                if(!simulate){if(inserting)inserted+=accounted;else extracted+=accounted;}
                return accounted;
            }
            public long extract(long amount,boolean simulate){return move(amount,simulate,false);}
            public long insert(long amount,boolean simulate){return move(amount,simulate,true);}
        };
    }
}
