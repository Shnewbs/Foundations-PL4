package net.foundations.pl4.core;

import java.math.BigInteger;
import java.util.Set;

/** Integer accounting: one credit is 1/1000000 FE. Native amounts never use floating point. */
public final class EnergyConversion {
    public static final long FE=1000000;
    public interface Port {
        long extract(long amount,boolean simulate);
        long insert(long amount,boolean simulate);
    }
    public record Rates(int fePer1000J,int fePerEU,int efficiencyPermille,int fePerElectrodynamicsJ) {
        public Rates {
            if(fePer1000J<1||fePer1000J>1000000||fePerEU<1||fePerEU>1000000||efficiencyPermille<1||efficiencyPermille>1000||fePerElectrodynamicsJ<1||fePerElectrodynamicsJ>1000000)throw new IllegalArgumentException("Energy conversion profile outside limits");
        }
        public long cost(String unit){return switch(unit){case "FE"->FE;case "J"->(long)fePer1000J*1000;case "ED_J"->fePerElectrodynamicsJ;case "EU"->Math.multiplyExact(FE,fePerEU);default->throw new IllegalArgumentException("Unknown energy unit");};}
        public int efficiency(String input,String output){return input.equals(output)?1000:efficiencyPermille;}
    }
    public static String unit(String value){return Set.of("FE","J","EU","ED_J").contains(value)?value:"FE";}
    public static long credits(long units,long unitCost,int efficiency){return floor(units,Math.multiplyExact(unitCost,efficiency),1000);}
    public static long extractable(long creditLimit,long unitCost){return Math.max(0,creditLimit)/unitCost;}
    /** Smallest whole native withdrawal covering simulated room; excess stays in escrow. */
    public static long sourceUnits(long room,long unitCost,int efficiency){return ceil(room,1000,Math.multiplyExact(unitCost,efficiency));}
    public static long outputUnits(long credits,long unitCost,int efficiency){return floor(credits,efficiency,Math.multiplyExact(unitCost,1000));}
    public static long consumed(long deliveredUnits,long unitCost,int efficiency){return ceil(deliveredUnits,Math.multiplyExact(unitCost,1000),efficiency);}
    public static int charge(long credits){return (int)Math.min(Integer.MAX_VALUE,ceil(credits,1,FE));}
    public static long deliver(Port port,long credits,long unitCost,int efficiency,boolean simulate){
        long offered=outputUnits(credits,unitCost,efficiency);if(offered==0)return 0;
        long inserted=port.insert(offered,simulate);if(inserted<0||inserted>offered)throw new IllegalStateException("Energy provider violated insertion contract");
        return consumed(inserted,unitCost,efficiency);
    }
    private static long floor(long amount,long numerator,long denominator){return divide(amount,numerator,denominator,false);}
    private static long ceil(long amount,long numerator,long denominator){return divide(amount,numerator,denominator,true);}
    private static long divide(long amount,long numerator,long denominator,boolean up){
        if(amount<0||numerator<0||denominator<=0)throw new IllegalArgumentException("Invalid energy arithmetic");
        if(numerator==0||amount==0)return 0;
        if(amount<=Long.MAX_VALUE/numerator){long product=amount*numerator;return product/denominator+(up&&product%denominator!=0?1:0);}
        BigInteger value=BigInteger.valueOf(amount).multiply(BigInteger.valueOf(numerator));
        if(up&&value.signum()>0)value=value.add(BigInteger.valueOf(denominator-1));
        return value.divide(BigInteger.valueOf(denominator)).min(BigInteger.valueOf(Long.MAX_VALUE)).longValueExact();
    }
    private EnergyConversion(){}
}
