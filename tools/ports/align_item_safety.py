"""Persist item-provider uncertainty and retain quarantine on saves and part drops.

Exact native APIs remain owned by the port. Source drift aborts before writes.
"""
from pathlib import Path
import argparse
import re


def once(source, before, after):
    if source.count(before)!=1:
        raise ValueError(f'Item safety source drift: {before[:100]!r} ({source.count(before)})')
    return source.replace(before,after,1)


def engine(source):
    if 'ItemTransferSafety.extract' in source:
        if source.count('ItemTransferSafety.insert(')!=3:
            raise ValueError('Incomplete item insertion guard')
        return source
    source=once(source,'        Part p=ref.part();String mode=', '        Part p=ref.part();if(ItemTransferSafety.blocked(p)){p.status="Item transfer quarantined; manual inspection required";return;}String mode=')
    begin=source.index('    // ------------------------------------------------------------ items')
    end=source.index('    // ------------------------------------------------------------ fluids',begin)
    prefix,suffix=source[:begin],source[end:]
    source=source[begin:end]
    source=once(source,'        Part p=source.part();boolean hadPending=!p.pendingItem.isEmpty();','        Part p=source.part();if(ItemTransferSafety.blocked(p))return;boolean hadPending=!p.pendingItem.isEmpty();')
    source=once(source,'        Part p=sink.part();IItemHandler to=itemHandler(server,sink);if(to==null)return;','        Part p=sink.part();if(ItemTransferSafety.blocked(p))return;IItemHandler to=itemHandler(server,sink);if(to==null)return;')
    source=once(source,'        Part p=source.part();\n        for(NetworkEngine.Ref sink:sinks){','        Part p=source.part();if(ItemTransferSafety.blocked(p))return;\n        for(NetworkEngine.Ref sink:sinks){')
    source=once(source,'ItemStack trial=from.extractItem(slot,sourceBudget,true);if(trial.isEmpty()', 'ItemStack trial=from.extractItem(slot,sourceBudget,true);if(!ItemTransferSafety.trial(trial,sourceBudget)')
    source=once(source,'ItemStack trial=from.extractItem(slot,limit,true);if(trial.isEmpty()', 'ItemStack trial=from.extractItem(slot,limit,true);if(!ItemTransferSafety.trial(trial,limit)')
    source=once(source,'ItemStack extracted=from.extractItem(slot,Math.min(space,trial.getCount()),false);', 'int request=Math.min(space,trial.getCount());ItemStack extracted=ItemTransferSafety.extract(source,trial,request,()->from.extractItem(slot,request,false));')
    source=once(source,'ItemStack extracted=from.extractItem(slot,accepted,false);','ItemStack extracted=ItemTransferSafety.extract(sink,trial,accepted,()->from.extractItem(slot,accepted,false));')
    # Validate simulated remainders before a mutation can be attempted.
    source=once(source,'remaining-=offered-rest.getCount();','remaining-=offered-ItemTransferSafety.remainder(send,rest);')
    source=once(source,'int accepted=trial.getCount()-rest.getCount();','int accepted=trial.getCount()-ItemTransferSafety.remainder(trial,rest);')
    # Mutating insertions remain quarantined until the caller settles its escrow.
    source=once(source,'ItemStack rest=ItemHandlerHelper.insertItemStacked(to,send,false);','ItemStack rest=ItemTransferSafety.insert(source,send,()->ItemHandlerHelper.insertItemStacked(to,send,false));')
    expression=re.search(r'ItemStack rest=ItemHandlerHelper.insertItemStacked\(to,(.*?),false\);int accepted=offered-rest.getCount\(\);if\(accepted>0\)',source)
    if not expression:raise ValueError('Missing ADD escrow insertion')
    copy=expression.group(1)
    source=source.replace(expression.group(0),'ItemStack send='+copy+';ItemStack rest=ItemTransferSafety.insert(sink,send,()->ItemHandlerHelper.insertItemStacked(to,send,false));int accepted=offered-rest.getCount();if(accepted>0)',1)
    source=once(source,'ItemStack restActual=ItemHandlerHelper.insertItemStacked(to,offered,false);','ItemStack restActual=ItemTransferSafety.insert(sink,offered,()->ItemHandlerHelper.insertItemStacked(to,offered,false));')
    source=once(source,'source.host().setChanged();sink.host().setChanged();}\n        }\n    }\n    private static void pullItems', 'source.host().setChanged();sink.host().setChanged();}\n            ItemTransferSafety.completed(source);\n        }\n    }\n    private static void pullItems')
    source=once(source,'received.add(target(sink));sink.host().setChanged();}}\n            return;', 'received.add(target(sink));sink.host().setChanged();}ItemTransferSafety.completed(sink);}\n            return;')
    source=once(source,'p.pendingItem.shrink(inserted);','p.pendingItem.shrink(inserted);ItemTransferSafety.completed(sink);')
    source=once(source,'if(received.contains(target(source))||pairs.contains(pair(source,sink)))continue;', 'if(ItemTransferSafety.blocked(source.part())||received.contains(target(source))||pairs.contains(pair(source,sink)))continue;')
    return prefix+source+suffix


def part(source, modern_nbt):
    if 'public String itemTransferFault=' in source:return source
    nbt=re.search(r'\b(Compound(?:Tag|NBT))\b',source).group(1)
    source=once(source,'    public ItemStack pendingItem = ItemStack.EMPTY;', '    public String itemTransferFault="";\n    public '+nbt+' opaqueItemEscrow;\n    public int itemEscrowCount;\n    public ItemStack pendingItem = ItemStack.EMPTY;')
    source=once(source,'return pendingItem.isEmpty()&&pendingFluid.isEmpty()&&energyRouteEditable()&&!resourceEscrow;', 'return !ItemTransferSafety.blocked(this)&&pendingItem.isEmpty()&&pendingFluid.isEmpty()&&energyRouteEditable()&&!resourceEscrow;')
    source=once(source,'t.putBoolean("resourceEscrow",!pendingItem.isEmpty()||!pendingFluid.isEmpty()||energyCredits()>0);', 't.putString("itemTransferFault",itemTransferFault);\n        t.putBoolean("resourceEscrow",ItemTransferSafety.blocked(this)||!pendingItem.isEmpty()||!pendingFluid.isEmpty()||energyCredits()>0);')
    saved=re.search(r'            if \(!pendingItem.isEmpty\(\)\) t.put\("pendingItem",(.*?)\);',source)
    if not saved:raise ValueError('Missing native item serialization')
    expression=saved.group(1).replace('pendingItem','itemForSave')
    source=once(source,saved.group(0),'            if(opaqueItemEscrow!=null)t.put("pendingItem",opaqueItemEscrow.copy());\n            else if(!pendingItem.isEmpty()){ItemStack itemForSave=pendingItem.copy();itemForSave.setCount(Math.min(pendingItem.getCount(),64));t.put("pendingItem",'+expression+');}\n            t.putInt("itemEscrowCount",pendingItem.isEmpty()?itemEscrowCount:pendingItem.getCount());')
    loaded=re.search(r'p.pendingItem=(.*?); p.pendingFluid=',source)
    if not loaded:raise ValueError('Missing native item deserialization')
    raw='t.getCompound("pendingItem").orElseGet('+nbt+'::new)' if modern_nbt else 't.getCompound("pendingItem")'
    expression=loaded.group(1)
    if raw not in expression:raise ValueError('Unrecognized compound API for item load')
    expression=expression.replace(raw,'itemPayload')
    string='t.getString("itemTransferFault").orElse("")' if modern_nbt else 't.getString("itemTransferFault")'
    number='t.getInt("itemEscrowCount").orElse(0)' if modern_nbt else 't.getInt("itemEscrowCount")'
    replacement='''p.itemTransferFault=STRING_GET;
        if(p.itemTransferFault.length()>512)p.itemTransferFault=p.itemTransferFault.substring(0,512);
        p.itemEscrowCount=Math.max(0,COUNT_GET);
        NBT_TYPE itemPayload=RAW_GET;
        try{p.pendingItem=ITEM_PARSE;}catch(RuntimeException invalid){p.pendingItem=ItemStack.EMPTY;}
        if(t.contains("pendingItem")&&p.pendingItem.isEmpty()){p.opaqueItemEscrow=itemPayload.copy();p.itemTransferFault="Unresolved saved item escrow; manual inspection required";}
        else if(!p.pendingItem.isEmpty()&&p.itemEscrowCount>0)p.pendingItem.setCount(p.itemEscrowCount);
        p.pendingFluid='''
    for key,value in [('STRING_GET',string),('COUNT_GET',number),('NBT_TYPE',nbt),('RAW_GET',raw),('ITEM_PARSE',expression)]:replacement=replacement.replace(key,value)
    return once(source,loaded.group(0),replacement)


def part_item(source):
    if 'escrow.itemTransferFault=part.itemTransferFault;' in source:return source
    source=once(source,'PartItemDataRules.needsEscrowPayload(!part.pendingItem.isEmpty(),', 'PartItemDataRules.needsEscrowPayload(ItemTransferSafety.blocked(part)||!part.pendingItem.isEmpty(),')
    source=once(source,'escrow.pendingItem=part.pendingItem.copy();','escrow.itemTransferFault=part.itemTransferFault;escrow.itemEscrowCount=part.itemEscrowCount;escrow.opaqueItemEscrow=part.opaqueItemEscrow==null?null:part.opaqueItemEscrow.copy();escrow.pendingItem=part.pendingItem.copy();')
    return source


def helper(engine_source, wireless_source):
    imported=re.search(r'^import ([\w.]+\.ItemStack);',engine_source,re.M).group(0)
    comparator=re.search(r'if\(([\w.]+)\(existing.item,stack\)',wireless_source).group(1)
    return '''package net.foundations.pl4;
IMPORT_ITEM
import java.util.function.Supplier;

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
        if(result.getCount()<0||result.getCount()>offered.getCount()||!SAME_ITEM(offered,result))throw new IllegalStateException("Invalid item remainder");
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
        enter(owner,"Item extraction");ItemStack actual=operation.get();
        if(actual==null)throw new IllegalStateException("Null actual item extraction");
        if(actual.isEmpty()){completed(owner);return ItemStack.EMPTY;}
        owner.part().pendingItem=actual.copy();owner.part().itemEscrowCount=actual.getCount();owner.host().setChanged();
        if(actual.getCount()<1||actual.getCount()>maximum||!SAME_ITEM(expected,actual))throw new IllegalStateException("Unexpected actual item extraction; escrow quarantined");
        completed(owner);return actual.copy();
    }
    public static ItemStack insert(NetworkEngine.Ref owner,ItemStack offered,Supplier<ItemStack> operation){
        ItemStack expected=offered.copy();enter(owner,"Item insertion");ItemStack result=operation.get();remainder(expected,result);return result;
    }
    private ItemTransferSafety(){}
}
'''.replace('IMPORT_ITEM',imported).replace('SAME_ITEM',comparator)


def fixtures(source):
    if 'private static void itemQuarantineChecks(' in source:return source
    # Extend an existing native scenario so historical fixture IDs/counts stay stable.
    source=once(source,'"Normal Node must feed an ADD Transfer Node");h.succeed();','"Normal Node must feed an ADD Transfer Node");itemQuarantineChecks(ref(add,Direction.EAST));h.succeed();')
    saved=re.search(r'^(\s*)var restored=Part.load\(source.save\(.*$',source,re.M)
    if not saved:raise ValueError('Missing native Part save/restore scenario')
    line=saved.group(0)
    source=once(source,line,'\n        source.itemTransferFault="Interrupted item insertion";source.pendingItem=new ItemStack(Items.DIAMOND,4096);\n'+line+'\n        if(!ItemTransferSafety.blocked(restored)||restored.pendingItem.getCount()!=4096||restored.routeEditable())throw new IllegalStateException("Item quarantine/count must survive native save reload");')
    checks='''
    private static void itemQuarantineChecks(NetworkEngine.Ref owner){
        Part p=owner.part();p.pendingItem=new ItemStack(Items.DIAMOND,8);int[] calls={0};
        boolean failed=false;
        try{ItemTransferSafety.insert(owner,p.pendingItem.copy(),()->{calls[0]++;throw new IllegalStateException("provider after mutation");});}catch(IllegalStateException expected){failed=true;}
        if(!failed||!ItemTransferSafety.blocked(p)||calls[0]!=1)throw new IllegalStateException("Uncertain insertion must persist quarantine");
        try{ItemTransferSafety.insert(owner,p.pendingItem.copy(),()->{calls[0]++;return ItemStack.EMPTY;});throw new AssertionError("Quarantine was replayed");}catch(IllegalStateException expected){}
        if(calls[0]!=1)throw new IllegalStateException("Quarantined provider must not be called again");
        p.itemTransferFault="";p.pendingItem=ItemStack.EMPTY;
        try{ItemTransferSafety.extract(owner,new ItemStack(Items.DIAMOND,8),8,()->new ItemStack(Items.EMERALD,8));throw new AssertionError("Wrong variant accepted");}catch(IllegalStateException expected){}
        if(!ItemTransferSafety.blocked(p)||p.pendingItem.getItem()!=Items.EMERALD||p.pendingItem.getCount()!=8)throw new IllegalStateException("Unexpected extraction retained without forwarding");
        p.itemTransferFault="";p.pendingItem=new ItemStack(Items.DIAMOND,8);
        try{ItemTransferSafety.insert(owner,p.pendingItem.copy(),()->new ItemStack(Items.EMERALD,1));throw new AssertionError("Malformed remainder accepted");}catch(IllegalStateException expected){}
        if(!ItemTransferSafety.blocked(p))throw new IllegalStateException("Invalid actual remainder must stay quarantined");
        p.itemTransferFault="";
        ItemStack refused=ItemTransferSafety.insert(owner,p.pendingItem.copy(),()->p.pendingItem.copy());
        if(refused.getCount()!=8)throw new IllegalStateException("Healthy refusal loses escrow");ItemTransferSafety.completed(owner);
        if(ItemTransferSafety.blocked(p)||p.pendingItem.getCount()!=8)throw new IllegalStateException("Healthy refusal must allow a later retry");
        p.pendingItem=ItemStack.EMPTY;p.itemEscrowCount=0;
    }
'''
    return source.rsplit('}',1)[0]+checks+'}\n'


def exception_fixture(source):
    if 'Quarantined escrow cannot be replayed after an uncertain mutation' in source:return source
    source=once(source,'if(fail[0])throw new IllegalStateException("PL4 expected destination failure");super.setItem(slot,stack);', 'super.setItem(slot,stack);if(fail[0])throw new IllegalStateException("PL4 expected destination failure");')
    source=once(source,'thrown&&from.getItem(0).isEmpty()&&to.getItem(0).isEmpty()&&sink.part().pendingItem.getCount()==17', 'thrown&&from.getItem(0).isEmpty()&&to.getItem(0).getCount()==17&&sink.part().pendingItem.getCount()==17&&ItemTransferSafety.blocked(sink.part())')
    source=once(source,'Extracted items remain owned by ADD escrow after a pre-mutation destination failure', 'A post-mutation destination failure leaves an uncertain, quarantined transaction')
    source=once(source,'restored!=null&&restored.pendingItem.getCount()==17', 'restored!=null&&restored.pendingItem.getCount()==17&&ItemTransferSafety.blocked(restored)&&!restored.routeEditable()')
    source=once(source,'to.getItem(0).getCount()==17&&sink.part().pendingItem.isEmpty()&&from.getItem(0).isEmpty(),"Retry delivers escrow exactly once"', 'to.getItem(0).getCount()==17&&sink.part().pendingItem.getCount()==17&&ItemTransferSafety.blocked(sink.part())&&from.getItem(0).isEmpty(),"Quarantined escrow cannot be replayed after an uncertain mutation"')
    return source


def edits(root):
    if (root/'src/legacy').exists():raise ValueError('Modern item migration cannot edit a legacy runtime')
    java=root/'src/main/java/net/foundations/pl4'
    status=__import__('json').loads((root/'BUILD_STATUS.json').read_text())
    engine_path=java/'TransferEngine.java';old_engine=engine_path.read_text()
    result={engine_path:engine(old_engine),java/'Part.java':part((java/'Part.java').read_text(),status['minecraft'].startswith('26.')),
            java/'PartItem.java':part_item((java/'PartItem.java').read_text()),
            java/'ItemTransferSafety.java':helper(old_engine,(java/'WirelessStorage.java').read_text())}
    choices=[root/'src/portTest/java/net/foundations/pl4/R16GameTests.java',java/'R16GameTests.java']
    path=next((p for p in choices if p.exists()),None)
    if path is None:raise ValueError('Missing native R16 fixtures')
    result[path]=fixtures(path.read_text())
    exception_path=path.with_name('EnergyIntegrationGameTests.java')
    result[exception_path]=exception_fixture(exception_path.read_text())
    return result


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('root',type=Path);args=parser.parse_args()
    for path,content in edits(args.root.resolve()).items():path.parent.mkdir(parents=True,exist_ok=True);path.write_text(content)


if __name__=='__main__':main()
