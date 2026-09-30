import java.util.*;
import java.math.BigInteger;
import net.foundations.pl4.core.*;

public final class EnergyConversionRegressionTests {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public enum Action {SIMULATE,EXECUTE}
    public interface Strict {
        int getEnergyContainerCount();long extractEnergy(long amount,Action action);long insertEnergy(long amount,Action action);
    }
    public static final class Joules implements Strict {
        public long stored,capacity;public int execute;
        public Joules(long stored,long capacity){this.stored=stored;this.capacity=capacity;}
        public int getEnergyContainerCount(){return 1;}
        public long extractEnergy(long amount,Action action){long n=Math.min(amount,stored);if(action==Action.EXECUTE){stored-=n;execute++;}return n;}
        public long insertEnergy(long amount,Action action){long n=Math.min(amount,capacity-stored);if(action==Action.EXECUTE){stored+=n;execute++;}return amount-n;}
    }
    public enum Side {INPUT,OUTPUT,BLOCKED}
    public interface EU {
        long acceptEnergyFromNetwork(Side side,long voltage,long amperage);long removeEnergy(long n);
        long getEnergyStored();long getEnergyCapacity();boolean inputsEnergy(Side side);boolean outputsEnergy(Side side);
        long getInputVoltage();long getInputAmperage();long getOutputVoltage();long getOutputAmperage();
    }
    public static final class Electric implements EU {
        public long stored=1024,capacity=4096,voltage=32;public int calls;
        public long acceptEnergyFromNetwork(Side side,long v,long a){if(v>voltage)throw new AssertionError("Overvoltage");long used=Math.min(a,(capacity-stored)/v);stored+=used*v;calls++;return used;}
        public long removeEnergy(long n){long taken=Math.min(stored,n);stored-=taken;calls++;return taken;}
        public long getEnergyStored(){return stored;}public long getEnergyCapacity(){return capacity;}
        public boolean inputsEnergy(Side side){return side==Side.INPUT;}public boolean outputsEnergy(Side side){return side==Side.OUTPUT;}
        public long getInputVoltage(){return voltage;}public long getInputAmperage(){return 2;}public long getOutputVoltage(){return 32;}public long getOutputAmperage(){return 2;}
    }
    public static final class Pack {
        final double joules,voltage;Pack(double j,double v){joules=j;voltage=v;}
        public static Pack joulesVoltage(double j,double v){return new Pack(j,v);}public double getJoules(){return joules;}
    }
    public interface ED {
        Pack receivePower(Pack pack,boolean simulate);Pack extractPower(Pack pack,boolean simulate);
        double getVoltage();double getMinimumVoltage();double getMaximumVoltage();double getAmpacity();boolean isEnergyProducer();boolean isEnergyReceiver();
    }
    public static final class Electrodynamic implements ED {
        public double stored=10,capacity=100,voltage=120,ampacity=-1;public double partial=Double.POSITIVE_INFINITY;public int calls;
        public Pack receivePower(Pack p,boolean sim){if(p.voltage!=voltage)throw new AssertionError("Wrong ED voltage");double n=Math.min(p.joules,Math.min(capacity-stored,partial));if(!sim){stored+=n;calls++;}return Pack.joulesVoltage(n,voltage);}
        public Pack extractPower(Pack p,boolean sim){if(p.voltage!=voltage)throw new AssertionError("Wrong ED voltage");double n=Math.min(p.joules,Math.min(stored,partial));if(!sim){stored-=n;calls++;}return Pack.joulesVoltage(n,voltage);}
        public double getVoltage(){return voltage;}public double getMinimumVoltage(){return voltage;}public double getMaximumVoltage(){return voltage;}public double getAmpacity(){return ampacity;}
        public boolean isEnergyProducer(){return true;}public boolean isEnergyReceiver(){return true;}
    }
    static void arithmetic(){
        var rates=new EnergyConversion.Rates(400,4,1000,1);
        check(rates.cost("J")==400000&&rates.cost("EU")==4000000&&rates.cost("ED_J")==1,"Provider-specific native values");
        check(EnergyConversion.credits(5,rates.cost("J"),1000)==2*EnergyConversion.FE,"5 Mek J = 2 FE");
        check(EnergyConversion.credits(1000000,rates.cost("ED_J"),1000)==EnergyConversion.FE,"One ED J = one FE");
        Random random=new Random(0x504C34);
        for(int i=0;i<10000;i++){
            long amount=random.nextLong(1,10000000000L),cost=random.nextLong(1,1000000001L);int efficiency=random.nextInt(1,1001);
            long out=EnergyConversion.outputUnits(amount,cost,efficiency),used=EnergyConversion.consumed(out,cost,efficiency);
            check(used<=amount,"Never mint rounded output");
            long units=EnergyConversion.sourceUnits(amount,cost,efficiency),credits=EnergyConversion.credits(units,cost,efficiency);
            check(credits>=amount,"Whole source quantum covers requested output; remainder stays in escrow");
            check(BigInteger.valueOf(out).multiply(BigInteger.valueOf(cost)).multiply(BigInteger.valueOf(1000)).compareTo(BigInteger.valueOf(amount).multiply(BigInteger.valueOf(efficiency)))<=0,"Reference rational conversion");
        }
        check(EnergyConversion.credits(Long.MAX_VALUE,1000000000000L,1000)==Long.MAX_VALUE,"Saturating overflow");
        check(EnergyConversion.charge(1)==1&&EnergyConversion.charge(EnergyConversion.FE)==1&&EnergyConversion.charge(EnergyConversion.FE+1)==2,"Fractional deliveries conservatively charge caps");
        check(rates.efficiency("J","J")==1000&&new EnergyConversion.Rates(400,4,900,1).efficiency("J","FE")==900,"Loss only on explicit conversion");
    }
    static void adapters()throws Exception {
        var j=new Joules(25,100);var jp=ReflectiveEnergyTransfer.mekanism(Strict.class,Action.class).bind(j,null,32,1024);
        check(jp.extract(5,true)==5&&j.stored==25&&j.execute==0,"Mek simulation is read-only");
        check(jp.extract(5,false)==5&&j.stored==20,"Mek actual extract");
        check(jp.insert(100,true)==80&&j.stored==20,"Mek returns remainder, adapter returns accepted");
        check(jp.insert(100,false)==80&&j.stored==100,"Mek actual insert");
        var eu=new Electric();var ea=ReflectiveEnergyTransfer.gregtech(EU.class,Side.class);var sink=ea.bind(eu,Side.INPUT,32,1024);
        check(sink.insert(200,true)==64&&eu.calls==0,"EU simulation is bounded by amperage without mutating");
        check(sink.insert(200,false)==64&&sink.insert(200,false)==0&&eu.calls==1,"EU amp budget shared across repeated calls in cycle");
        check(ea.bind(eu,Side.INPUT,128,1024).insert(1024,false)==0&&eu.calls==1,"EU overvoltage rejected before provider");
        check(ea.bind(eu,Side.BLOCKED,32,1024).extract(100,false)==0&&eu.calls==1,"EU output side respected");
        var from=ea.bind(eu,Side.OUTPUT,32,1024);check(from.extract(100,true)==64&&eu.calls==1,"EU simulation respects source output voltage/amps");
        check(from.extract(100,false)==64&&from.extract(100,false)==0,"EU extraction budget cannot be bypassed by repeated calls");
        check(ea.bind(eu,Side.OUTPUT,32,1024).extract(100,true)==64,"New cycle resets EU budget");
        var ed=new Electrodynamic();var bridge=ReflectiveElectrodynamicTransfer.resolve(ED.class,Pack.class);var ep=bridge.bind(ed);
        check(ep.extract(250000,true)==250000&&ed.calls==0,"ED fractional simulation");
        check(ep.extract(250000,false)==250000&&ed.stored==9.75,"ED quarter-Joule withdrawal");
        ed.partial=0.0000004;check(ep.insert(1,false)==1,"ED submicro deposits charge up");check(ep.extract(1,false)==0,"ED submicro withdrawals credit down");
        check(ed.stored==9.75,"Conservative fractional operations cannot mint credits");
        ed.partial=Double.POSITIVE_INFINITY;ed.ampacity=1;var limited=bridge.bind(ed);
        check(limited.insert(10000000,true)==6000000&&limited.insert(10000000,false)==6000000&&limited.insert(10000000,false)==0,"ED ampacity uses J/V*20 and shares burst budget");
        ed.voltage=Double.NaN;try{bridge.bind(ed).insert(1,true);throw new AssertionError("Nonfinite voltage accepted");}catch(IllegalStateException expected){checks++;}
        check(ReflectiveElectrodynamicTransfer.microjoules(0.0000004,true)==1&&ReflectiveElectrodynamicTransfer.microjoules(0.0000004,false)==0,"Documented ED precision bound");
    }
    public static void main(String[] args)throws Exception {arithmetic();adapters();System.out.println("PASS energy conversion regression assertions: "+checks);}
}
