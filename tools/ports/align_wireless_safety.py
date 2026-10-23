"""Stage Wireless Storage player mutations in persistent node escrow before foreign calls."""
from pathlib import Path
import re
from align_item_safety import once


def helper(item_helper):
    imported=re.search(r'^import [\w.]+\.ItemStack;',item_helper,re.M).group(0)
    return '''package net.foundations.pl4;
IMPORT_ITEM
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
'''.replace('IMPORT_ITEM',imported)


def storage(source):
    if 'WirelessItemTransfers.deposit' in source:return source
    source=once(source,'private record Target(Part node,IItemHandler inventory,Part.Link link){}','private record Target(Part node,IItemHandler inventory,Part.Link link,NetworkEngine.Ref ref){}')
    source=once(source,'if(ref.host().isRemoved()||','if(!WirelessItemTransfers.available(ref.part())||ref.host().isRemoved()||')
    source=once(source,'new Target(ref.part(),inventory,link)','new Target(ref.part(),inventory,link,ref)')
    source=once(source,'        int page=session.page();','        try{\n        int page=session.page();')
    comparison=re.search(r'if\(trial.isEmpty\(\)\|\|!(.*?)\(expected.item,trial\)\)continue;',source)
    if not comparison:raise ValueError('Unknown wireless variant predicate')
    source=once(source,comparison.group(0),'if(!ItemTransferSafety.trial(trial,remaining)||!'+comparison.group(1)+'(expected.item,trial))continue;')
    source=once(source,'ItemStack extracted=target.inventory().extractItem(source.slot(),Math.min(remaining,trial.getCount()),false);','int requested=Math.min(remaining,trial.getCount());ItemStack extracted=ItemTransferSafety.extract(target.ref(),expected.item,requested,()->target.inventory().extractItem(source.slot(),requested,false));')
    delivery=re.search(r'if\(extracted.isEmpty\(\)\)continue;int count=extracted.getCount\(\);(player(?:\.inventory|\.getInventory\(\)))\.add\(extracted\);if\(!extracted.isEmpty\(\)\)player.drop\(extracted,false(?:,net\.minecraft\.util\.Prediction.SERVER_ONLY)?\);moved\+=count;remaining-=count;',source)
    if not delivery:raise ValueError('Unknown native player inventory handoff')
    inventory=delivery.group(1)
    source=once(source,delivery.group(0),'if(extracted.isEmpty())continue;int count=WirelessItemTransfers.deliver(target.ref(),stackToPlayer->{'+inventory+'.add(stackToPlayer);return stackToPlayer;});moved+=count;remaining-=extracted.getCount();')
    source=once(source,'int capacity=offered.getCount()-simulated.getCount();','int capacity=offered.getCount()-ItemTransferSafety.remainder(offered,simulated);')
    mutation=re.search(r'ItemStack sent=(.*?);int count=sent.getCount\(\);ItemStack remainder=target.inventory\(\).insertItem\(slot,sent,false\);\s*int accepted=(?:Math|net\.foundations\.pl4\.compat\.PortMath).clamp\(count-remainder.getCount\(\),0,count\);offhand.shrink\(accepted\);moved\+=accepted;',source)
    if not mutation:raise ValueError('Unknown wireless offhand insertion')
    source=once(source,mutation.group(0),'ItemStack sent='+mutation.group(1)+';final int destinationSlot=slot;int accepted=WirelessItemTransfers.deposit(target.ref(),offhand,sent,value->target.inventory().insertItem(destinationSlot,value,false));moved+=accepted;')
    anchor='        send(player,session.hand(),held,session.binding(),targets,page,query,sort,true,status);'
    source=once(source,anchor,anchor+'''\n        }catch(RuntimeException failure){
            org.slf4j.LoggerFactory.getLogger("FoundationsPL4").error("Wireless Storage provider failed; inspect node escrow before recovery",failure);
            sendClosed(player,"Storage provider failed; inspect node escrow before recovery.");
        }finally{INVENTORY.setChanged();player.inventoryMenu.broadcastChanges();}'''.replace('INVENTORY',inventory))
    return source


def network(source):
    if 'Item storage paused; inspect node escrow' in source:return source
    return once(source,'if(p.kind.cable()||p.kind==Kind.NODE||p.kind==Kind.ARRAY)p.status=connectionStatus;','if(p.kind.cable()||p.kind==Kind.ARRAY)p.status=connectionStatus;\n            if(p.kind==Kind.NODE)p.status=WirelessItemTransfers.available(p)?connectionStatus:"Item storage paused; inspect node escrow";')


def fixtures(source):
    if 'private static void wirelessHandoffQuarantine(' in source:return source
    call='throw new IllegalStateException("Healthy endpoint resumes without rebuilding the plan");'
    source=once(source,call,call+'wirelessHandoffQuarantine(ref(node,Direction.WEST));')
    checks='''
    private static void wirelessHandoffQuarantine(NetworkEngine.Ref owner){
        Part p=owner.part();ItemStack hand=new ItemStack(Items.DIAMOND,19);int[] actual={0};
        try{WirelessItemTransfers.deposit(owner,hand,new ItemStack(Items.DIAMOND,7),sent->{actual[0]+=sent.getCount();throw new IllegalStateException("Destination mutated before failure");});throw new AssertionError("Missing deposit failure");}catch(IllegalStateException expected){}
        if(hand.getCount()!=12||actual[0]!=7||p.pendingItem.getCount()!=7||!ItemTransferSafety.blocked(p))throw new IllegalStateException("Uncertain deposit must remove spendable offhand items and quarantine the node");
        try{WirelessItemTransfers.deposit(owner,hand,new ItemStack(Items.DIAMOND,7),sent->{actual[0]+=sent.getCount();return ItemStack.EMPTY;});throw new AssertionError("Replayed wireless deposit");}catch(IllegalStateException expected){}
        if(actual[0]!=7)throw new IllegalStateException("Quarantined wireless deposit cannot replay");
        p.itemTransferFault="";p.pendingItem=ItemStack.EMPTY;p.itemEscrowCount=0;
        int refused=WirelessItemTransfers.deposit(owner,hand,new ItemStack(Items.DIAMOND,7),sent->sent);
        if(refused!=0||hand.getCount()!=12||!p.pendingItem.isEmpty()||ItemTransferSafety.blocked(p))throw new IllegalStateException("Healthy wireless refusal must return the staged hand items");
        ItemStack fullHand=new ItemStack(Items.DIAMOND,7);
        if(WirelessItemTransfers.deposit(owner,fullHand,fullHand.copy(),sent->sent)!=0||fullHand.getCount()!=7||fullHand.getItem()!=Items.DIAMOND)throw new IllegalStateException("Complete offhand staging must restore a healthy refusal");
        int moved=WirelessItemTransfers.deposit(owner,hand,new ItemStack(Items.DIAMOND,7),sent->ItemStack.EMPTY);
        if(moved!=7||hand.getCount()!=5||!p.pendingItem.isEmpty())throw new IllegalStateException("Valid wireless deposit settles exactly once");
        p.pendingItem=new ItemStack(Items.DIAMOND,8);
        try{WirelessItemTransfers.deliver(owner,sent->{actual[0]+=sent.getCount();throw new IllegalStateException("Player inventory mutated before failure");});throw new AssertionError("Missing delivery failure");}catch(IllegalStateException expected){}
        if(!ItemTransferSafety.blocked(p)||p.pendingItem.getCount()!=8||actual[0]!=15)throw new IllegalStateException("Uncertain player delivery must remain fenced");
        p.itemTransferFault="";p.pendingItem=ItemStack.EMPTY;p.itemEscrowCount=0;
    }
'''
    return source.rsplit('}',1)[0]+checks+'}\n'


def edits(root,prepared=None):
    if (root/'src/legacy').exists():raise ValueError('Excluded legacy runtime')
    java=root/'src/main/java/net/foundations/pl4'
    path=next(p for p in [root/'src/portTest/java/net/foundations/pl4/R16GameTests.java',java/'R16GameTests.java'] if p.exists())
    return {java/'WirelessItemTransfers.java':helper((java/'ItemTransferSafety.java').read_text()),java/'WirelessStorage.java':storage((java/'WirelessStorage.java').read_text()),java/'NetworkEngine.java':network((java/'NetworkEngine.java').read_text()),path:fixtures((prepared or {}).get(path,path.read_text()))}
