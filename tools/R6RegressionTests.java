import java.util.*;
import java.math.BigInteger;
import net.foundations.pl4.core.*;

/** These fixtures model documented public method signatures. They are NOT Mekanism/GT runtime tests. */
public final class R6RegressionTests {
    private static int checks;
    private static void check(boolean good,String message){checks++;if(!good)throw new AssertionError(message);}
    private static void close(double a,double b,String message){check(Math.abs(a-b)<0.000001,message+": "+a+" / "+b);}
    private interface Throwing {void run()throws Exception;}
    private static void rejects(Throwing work){boolean rejected=false;try{work.run();}catch(Exception e){rejected=true;}check(rejected,"Invalid telemetry must be rejected");}
    public interface MekAPI {int getEnergyContainerCount();long getEnergy(int i);long getMaxEnergy(int i);void setEnergy(int i,long amount);}
    private static final class MekFixture implements MekAPI {
        int reads;public int getEnergyContainerCount(){return 2;}
        public long getEnergy(int i){reads++;return i==0?5_000_000_000L:42;}
        public long getMaxEnergy(int i){reads++;return 10_000_000_000L;}
        public void setEnergy(int i,long n){throw new AssertionError("Reader must never mutate energy");}
    }
    public interface GTAPI {long getEnergyStored();long getEnergyCapacity();long changeEnergy(long amount);}
    private static final class GTFixture implements GTAPI {
        public long getEnergyStored(){return 5_000_000_000L;}
        public long getEnergyCapacity(){return 10_000_000_000L;}
        public long changeEnergy(long amount){throw new AssertionError("Reader must never transfer energy");}
    }
    public record Info(BigInteger capacity,BigInteger stored){}
    public interface GTInfoAPI {Info getEnergyInfo();boolean isOneProbeHidden();}
    public static void main(String[] args)throws Exception{
        for(int mount=0;mount<6;mount++)for(boolean outward:new boolean[]{false,true}){
            var f=DisplayFacing.frame(mount,outward);
            check(f.right().cross(f.up()).equals(f.normal()),"Right x up must be FRONT, not mirrored");
            check(f.right().dot(f.up())==0&&f.up().dot(f.normal())==0&&f.right().dot(f.normal())==0,"Orthonormal display basis");
            check(DisplayFacing.direction(f.normal())==DisplayFacing.front(mount,outward),"Layout/renderer front must agree");
            double x=Math.toRadians(f.rotationX()),y=Math.toRadians(f.rotationY());
            // Match the renderer's X-then-Y basis (only one is nonzero).
            double[][] r={{Math.cos(y),0,-Math.sin(y)},{Math.sin(y)*Math.sin(x),Math.cos(x),Math.cos(y)*Math.sin(x)},{Math.sin(y)*Math.cos(x),-Math.sin(x),Math.cos(y)*Math.cos(x)}};
            var v=List.of(f.right(),f.up(),f.normal());
            for(int axis=0;axis<3;axis++){close(r[axis][0],v.get(axis).x(),"Rotation X component");close(r[axis][1],v.get(axis).y(),"Rotation Y component");close(r[axis][2],v.get(axis).z(),"Rotation Z component");}
            // Glyph plane must be beyond the selected skin, never on the reverse side or embedded.
            close(DisplayFacing.planeOffset(outward),outward?0.5005625:0.4370,"Text surface offset");
            var reverse=DisplayFacing.frame(mount,!outward);
            check(reverse.normal().equals(f.normal().negate()),"Flip reverses viewing side");
            for(int width=1;width<=16;width++)for(int height=1;height<=16;height++){
                // Screen-positive rows/columns agree with world vectors for all rectangular extents.
                int dx=f.right().x()*(width-1)-f.up().x()*(height-1);
                int dy=f.right().y()*(width-1)-f.up().y()*(height-1);
                int dz=f.right().z()*(width-1)-f.up().z()*(height-1);
                var delta=new DisplayFacing.Vector(dx,dy,dz);
                check(delta.dot(f.right())==width-1&&delta.dot(f.up())==-(height-1)&&delta.dot(f.normal())==0,"Rectangle must lie on front plane with unmirrored columns");
            }
        }
        rejects(()->DisplayFacing.frame(6,true));
        var mek=new MekFixture();var access=ReflectiveEnergyAccess.mekanism(MekAPI.class);
        var reading=access.read(mek,1024);check(reading.unit().equals("J"),"Mekanism unit must be J");close(reading.stored(),5_000_000_042D,"No int truncation");close(reading.capacity(),20_000_000_000D,"Sum containers once");check(mek.reads==4,"One read of each storage/capacity");
        check(access.read(null,1024)==null,"Missing handler is not zero stored");rejects(()->access.read(mek,1));
        var gt=ReflectiveEnergyAccess.gregtech(GTAPI.class).read(new GTFixture(),1024);check(gt.unit().equals("EU"),"GT native unit");close(gt.stored(),5_000_000_000D,"GT long capacity retained");
        var infoAccess=ReflectiveEnergyAccess.gregtechInfo(GTInfoAPI.class);
        var big=infoAccess.read(new GTInfoAPI(){public Info getEnergyInfo(){return new Info(new BigInteger("100000000000000000000"),new BigInteger("50000000000000000000"));}public boolean isOneProbeHidden(){return false;}},1024);
        close(big.stored(),5e19,"BigInteger telemetry uses finite double, not signed-long overflow");
        check(infoAccess.read(new GTInfoAPI(){public Info getEnergyInfo(){throw new AssertionError("Hidden provider must not be read");}public boolean isOneProbeHidden(){return true;}},1024)==null,"Respect hidden information flag");
        Map<String,EnergyValues.Reading> totals=EnergyValues.totals(List.of(reading,gt,new EnergyValues.Reading("fe","FE",100,200),new EnergyValues.Reading("other","FE",30,50)));
        check(totals.size()==3,"FE/EU/J must never be combined");close(totals.get("FE").stored(),130,"FE total");close(totals.get("J").stored(),reading.stored(),"J total");
        check(EnergyValues.totalKey("FE").equals("storage"),"Legacy FE layout key retained");check(EnergyValues.totalKey("EU").equals("storage:eu"),"Native stable key");
        for(String req:List.of("AUTO","FE","EU","J"))for(String unit:List.of("FE","EU","J"))check(EnergyValues.accepts(req,unit)==(req.equals("AUTO")||req.equals(unit)),"Provider system selection");
        for(double bad:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY})rejects(()->new EnergyValues.Reading("fixture","EU",bad,100));
        rejects(()->new EnergyValues.Reading("fixture","RF",1,2));rejects(()->EnergyValues.add(Double.MAX_VALUE,Double.MAX_VALUE));rejects(()->ReflectiveEnergyAccess.gregtech(String.class));
        check(EnergyValues.totals(List.of()).isEmpty(),"Unsupported target must not manufacture Storage 0 FE");
        System.out.println("PASS R6 production rules/accessors: "+checks+" assertions (12 viewing orientations, rectangular axes, unit isolation, native long/BigInteger reads, invalid/hidden/missing providers).");
        System.out.println("Scope: dependency-free methods and synthetic API fixtures, NOT Minecraft rendering or live mod integration.");
    }
}
