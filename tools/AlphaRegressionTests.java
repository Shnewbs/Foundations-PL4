import java.util.*;
import net.foundations.pl4.core.SignalRules;
public final class AlphaRegressionTests {
    private static int checks;
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("Alpha check "+checks);}
    public static void main(String[] args){
        for(String operator:SignalRules.OPERATORS){check(!SignalRules.compare(null,1,operator));check(!SignalRules.compare(Double.NaN,1,operator));check(!SignalRules.compare(Double.POSITIVE_INFINITY,1,operator));}
        check(SignalRules.compare(-0.0,0,"="));check(!SignalRules.compare(0.0,0,"!="));check(!SignalRules.compare(1.0,1,"invalid"));
        var a=new SignalRules.Statement(UUID.randomUUID(),"reader","key",">=",5);
        var b=new SignalRules.Statement(UUID.randomUUID(),"other","key","<",3);
        for(int strength=0;strength<=15;strength++)for(int av=0;av<10;av++)for(int bv=0;bv<10;bv++){
            final int x=av,y=bv;
            check(SignalRules.signal(List.of(a,b),true,strength,s->s==a?(double)x:(double)y)==(av>=5&&bv<3?strength:0));
            check(SignalRules.signal(List.of(a,b),false,strength,s->s==a?(double)x:(double)y)==(av>=5||bv<3?strength:0));
        }
        check(SignalRules.signal(List.of(),true,15,s->1.0)==0);
        check(SignalRules.signal(Collections.nCopies(17,a),false,15,s->10.0)==0);
        check(SignalRules.signal(List.of(a),true,30,s->10.0)==15);
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,1e16}){boolean rejected=false;try{new SignalRules.Statement(UUID.randomUUID(),"","",">",bad);}catch(IllegalArgumentException expected){rejected=true;}check(rejected);}
        System.out.println("PASS 0.1a automation: "+checks+" assertions");
    }
}
