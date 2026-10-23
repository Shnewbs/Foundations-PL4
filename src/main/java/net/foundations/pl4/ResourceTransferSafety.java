package net.foundations.pl4;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.function.Supplier;
import java.util.function.LongSupplier;

/** Persistent uncertainty fence. Clear only after a validated result is settled in escrow. */
public final class ResourceTransferSafety {
    public static boolean blocked(Part p){return !p.resourceTransferFault.isEmpty()||p.opaqueFluidEscrow!=null;}
    public static void enter(NetworkEngine.Ref owner,String action){
        if(blocked(owner.part()))throw new IllegalStateException("Resource transfer remains quarantined");
        owner.part().resourceTransferFault=action+" uncertain; manual inspection required";owner.host().setChanged();
    }
    public static void completed(NetworkEngine.Ref owner){
        if(owner.part().opaqueFluidEscrow==null)owner.part().resourceTransferFault="";
        owner.host().setChanged();
    }
    public static long amount(long result,long maximum){
        if(result<0||result>maximum)throw new IllegalStateException("Resource provider returned an invalid amount");
        return result;
    }
    public static long mutate(NetworkEngine.Ref owner,String action,long maximum,LongSupplier operation){
        enter(owner,action);return amount(operation.getAsLong(),maximum);
    }
    public static boolean fluidTrial(FluidStack expected,FluidStack actual,int maximum){
        if(actual==null)throw new IllegalStateException("Null fluid extraction");
        amount(actual.getAmount(),maximum);
        if(actual.isEmpty())return false;
        FluidStack a=expected,b=actual;
        if(!(a.getFluid()==b.getFluid()&&java.util.Objects.equals(a.getComponentsPatch(),b.getComponentsPatch())))throw new IllegalStateException("Unexpected fluid variant");
        return true;
    }
    public static FluidStack extractFluid(NetworkEngine.Ref owner,FluidStack request,Supplier<FluidStack> operation){
        FluidStack expected=request.copy();enter(owner,"Fluid extraction");FluidStack actual=operation.get();
        if(actual!=null&&!actual.isEmpty()){owner.part().pendingFluid=actual.copy();owner.host().setChanged();}
        if(!fluidTrial(expected,actual,expected.getAmount())){completed(owner);return FluidStack.EMPTY;}
        completed(owner);return actual.copy();
    }
    public static int insertFluid(NetworkEngine.Ref owner,FluidStack offered,Supplier<Integer> operation){
        int maximum=offered.getAmount();enter(owner,"Fluid insertion");return (int)amount(operation.get(),maximum);
    }
    public static int extractEnergy(NetworkEngine.Ref owner,int maximum,LongSupplier operation){
        enter(owner,"FE extraction");long actual=operation.getAsLong();
        if(actual>0&&actual<=Integer.MAX_VALUE){owner.part().pendingEnergy=(int)actual;owner.host().setChanged();}
        amount(actual,maximum);completed(owner);return (int)actual;
    }
    public static int insertEnergy(NetworkEngine.Ref owner,int maximum,LongSupplier operation){
        return (int)mutate(owner,"FE insertion",maximum,operation);
    }
    private ResourceTransferSafety(){}
}
