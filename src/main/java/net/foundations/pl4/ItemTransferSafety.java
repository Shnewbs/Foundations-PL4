package net.foundations.pl4;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

/** Write-ahead uncertainty fences for foreign item mutations. Never clears an interrupted call automatically. */
public final class ItemTransferSafety {
    public static boolean blocked(Part p){return p.opaqueItemEscrow!=null||!p.itemTransferFault.isEmpty();}
    public static boolean trial(ItemStack result,int maximum){
        if(result==null||result.getCount()<0||result.getCount()>maximum)throw new IllegalStateException("Invalid simulated item extraction");
        return !result.isEmpty();
    }
    public static int remainder(ItemStack offered,ItemStack result){
        if(result==null)throw new IllegalStateException("Null item remainder");
        if(result.isEmpty())return 0;
        if(result.getCount()<0||result.getCount()>offered.getCount()||!ItemStack.isSameItemSameComponents(offered,result))throw new IllegalStateException("Invalid item remainder");
        return result.getCount();
    }
    private static void enter(NetworkEngine.Ref owner,String action){
        if(blocked(owner.part()))throw new IllegalStateException("Item transfer remains quarantined");
        owner.part().itemTransferFault=action+" uncertain; manual inspection required";owner.host().setChanged();
    }
    public static void completed(NetworkEngine.Ref owner){
        if(owner.part().opaqueItemEscrow==null)owner.part().itemTransferFault="";
        if(owner.part().pendingItem.isEmpty())owner.part().itemEscrowCount=0;
        owner.host().setChanged();
    }
    public static ItemStack extract(NetworkEngine.Ref owner,ItemStack expected,int maximum,Supplier<ItemStack> operation){
        return extract(owner,expected,maximum,()->true,operation);
    }
    /** The live route is checked both before and after the foreign provider callback. */
    public static ItemStack extract(NetworkEngine.Ref owner,ItemStack expected,int maximum,BooleanSupplier authorized,Supplier<ItemStack> operation){
        if(!authorized.getAsBoolean())return ItemStack.EMPTY;
        enter(owner,"Item extraction");ItemStack actual=operation.get();
        if(actual==null)throw new IllegalStateException("Null actual item extraction");
        // Record what the foreign provider actually removed before considering a revoked route.
        if(!actual.isEmpty()){
            owner.part().pendingItem=actual.copy();owner.part().itemEscrowCount=actual.getCount();owner.host().setChanged();
        }
        if(!authorized.getAsBoolean())throw new IllegalStateException("Item extraction route revoked during provider callback; escrow quarantined");
        if(actual.isEmpty()){completed(owner);return ItemStack.EMPTY;}
        if(actual.getCount()<1||actual.getCount()>maximum||!ItemStack.isSameItemSameComponents(expected,actual))throw new IllegalStateException("Unexpected actual item extraction; escrow quarantined");
        completed(owner);return actual.copy();
    }
    public static ItemStack insert(NetworkEngine.Ref owner,ItemStack offered,Supplier<ItemStack> operation){
        return insert(owner,offered,()->true,operation);
    }
    public static ItemStack insert(NetworkEngine.Ref owner,ItemStack offered,BooleanSupplier authorized,Supplier<ItemStack> operation){
        if(!authorized.getAsBoolean())return offered.copy();
        ItemStack expected=offered.copy();enter(owner,"Item insertion");
        ItemStack result=operation.get();remainder(expected,result);
        // A successful-looking return is not authorization if the callback changed ownership,
        // attachment, dimension or loaded target. Keep the entire escrow fenced for review.
        if(!authorized.getAsBoolean())throw new IllegalStateException("Item insertion route revoked during provider callback; escrow quarantined");
        return result;
    }
    private ItemTransferSafety(){}
}
