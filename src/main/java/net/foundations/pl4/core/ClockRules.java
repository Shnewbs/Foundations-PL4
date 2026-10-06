package net.foundations.pl4.core;

/** Quantized server clock. A zero pulse preserves the legacy one-sample pulse. */
public final class ClockRules {
    public static long period(double requested,int sample){return Math.max(sample*2L,Math.min(24000,(long)requested));}
    public static boolean high(long ticks,double requested,int sample,int pulse,int phase,boolean paused){
        if(paused)return false;
        long period=period(requested,sample),width=Math.clamp(pulse==0?sample:pulse,1,period);
        long position=Math.floorMod(Math.floorMod(ticks,period)+Math.floorMod(phase,period),period);
        return position<width;
    }
    private ClockRules(){}
}
