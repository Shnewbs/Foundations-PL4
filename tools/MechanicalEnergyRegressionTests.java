import net.foundations.pl4.core.MechanicalEnergyAccess;

public final class MechanicalEnergyRegressionTests {
    public static class Kinetic {
        protected float stress=40,capacity=100;public int mutations;
        public float getSpeed(){return -32;}public float getTheoreticalSpeed(){return -64;}public boolean isOverStressed(){return true;}
        public void setSpeed(float v){mutations++;}
    }
    public enum Side{WEST,EAST}
    public interface Host{Node getGridNode(Side side);}
    public interface Node{Grid getGrid();}
    public interface Grid{Service getEnergyService();}
    public interface Service{double getStoredPower();double getMaxStoredPower();double getAvgPowerUsage();double getAvgPowerInjection();boolean isNetworkPowered();}
    public static final class Energy implements Service{
        double stored=125;public double getStoredPower(){return stored;}public double getMaxStoredPower(){return 1000;}public double getAvgPowerUsage(){return 2;}public double getAvgPowerInjection(){return 3;}public boolean isNetworkPowered(){return true;}
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Kinetic k=new Kinetic();var create=new MechanicalEnergyAccess.Create(Kinetic.class);var c=create.read(k);
        check(c.rpm()==-32&&c.theoretical()==-64&&c.stress()==40&&c.capacity()==100&&c.overstressed(),"Signed RPM and stopped overstressed networks retain theoretical speed and network SU");
        check(k.mutations==0&&k.stress==40&&k.capacity==100,"Sampling cannot modify a kinetic network");check(create.read(new Object())==null,"Unrelated machines are unsupported");
        k.stress=Float.NaN;try{create.read(k);throw new AssertionError("NaN accepted");}catch(IllegalArgumentException expected){}
        try{new MechanicalEnergyAccess.Create(String.class);throw new AssertionError("Unsupported API accepted");}catch(ReflectiveOperationException expected){}
        Energy service=new Energy();Grid grid=()->service;Node node=()->grid;Host host=side->side==Side.WEST?node:null;
        var ae=new MechanicalEnergyAccess.AE2(Host.class,Side.class);var v=ae.read(host,Side.WEST);
        check(v.identity()==grid&&v.stored()==125&&v.capacity()==1000&&v.usage()==2&&v.injection()==3&&v.powered(),"AE2 native units and stable grid identity");
        check(ae.read(host,Side.EAST)==null&&ae.read(new Object(),Side.WEST)==null,"Disconnected and unrelated grid endpoints fail closed");
        service.stored=-1;try{ae.read(host,Side.WEST);throw new AssertionError("Negative energy accepted");}catch(IllegalArgumentException expected){}
        System.out.println("PASS Create/AE2 read-only API contracts, unsupported targets and malformed telemetry");
    }
}
