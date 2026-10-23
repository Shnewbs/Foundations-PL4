package net.foundations.pl4;
import net.minecraft.item.ItemStack;
import java.util.function.Function;

/** Player handoffs use the same persistent uncertainty fence as network transfers. */
final class WirelessItemTransfers {
    static boolean available(Part part){return !ItemTransferSafety.blocked(part)&&!ResourceTransferSafety.blocked(part)&&part.pendingItem.isEmpty();}
    static int deposit(NetworkEngine.Ref owner,ItemStack source,ItemStack offered,Function<ItemStack,ItemStack> operation){
        if(!available(owner.part()))throw new IllegalStateException("Wireless node escrow is busy or quarantined");
        int count=ItemTransferSafety.remainder(source,offered);if(count<=0)throw new IllegalStateException("Empty wireless deposit");
        owner.part().pendingItem=offered.copy();owner.part().itemEscrowCount=count;owner.host().setChanged();
        // Remove spendable player items BEFORE a provider may mutate and then throw.
        source.shrink(count);
        ItemStack remainder=ItemTransferSafety.insert(owner,offered,()->operation.apply(offered.copy()));
        source.grow(remainder.getCount());owner.part().pendingItem=ItemStack.EMPTY;ItemTransferSafety.completed(owner);
        return count-remainder.getCount();
    }
    static int deliver(NetworkEngine.Ref owner,Function<ItemStack,ItemStack> operation){
        ItemStack offered=owner.part().pendingItem.copy();int count=offered.getCount();
        if(count<=0)return 0;
        ItemStack remainder=ItemTransferSafety.insert(owner,offered,()->operation.apply(offered.copy()));
        owner.part().pendingItem=remainder.copy();ItemTransferSafety.completed(owner);
        if(!remainder.isEmpty()){owner.part().itemTransferFault="Wireless player delivery incomplete; manual inspection required";owner.host().setChanged();}
        return count-remainder.getCount();
    }
    private WirelessItemTransfers(){}
}
