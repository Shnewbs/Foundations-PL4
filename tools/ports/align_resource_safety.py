"""Fence uncertain foreign fluid/energy mutations in each exact modern runtime."""
from pathlib import Path
import argparse
import json
import re
from align_item_safety import once


def helper(root):
    java=root/'src/main/java/net/foundations/pl4'
    samples=(java/'VisualSamples.java').read_text()
    imported=re.search(r'^import ([\w.]+\.FluidStack);',samples,re.M).group(0)
    patch=re.search(r'new Key\(stack.getFluid\(\),(.*?)\);Sum',samples).group(1)
    same='a.getFluid()==b.getFluid()&&java.util.Objects.equals('+patch.replace('stack','a')+','+patch.replace('stack','b')+')'
    return '''package net.foundations.pl4;
IMPORT_FLUID
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
        if(!(SAME_FLUID))throw new IllegalStateException("Unexpected fluid variant");
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
'''.replace('IMPORT_FLUID',imported).replace('SAME_FLUID',same)


def part(source,modern):
    if 'public String resourceTransferFault=' in source:return source
    nbt=re.search(r'\b(Compound(?:Tag|NBT))\b',source).group(1)
    source=once(source,'    public String itemTransferFault="";','    public String resourceTransferFault="";\n    public '+nbt+' opaqueFluidEscrow;\n    public String itemTransferFault="";')
    source=once(source,'return !ItemTransferSafety.blocked(this)&&','return !ResourceTransferSafety.blocked(this)&&!ItemTransferSafety.blocked(this)&&')
    source=once(source,'return energyCredits()==0&&!energyEscrow;','return !ResourceTransferSafety.blocked(this)&&energyCredits()==0&&!energyEscrow;')
    source=once(source,'t.putString("itemTransferFault",itemTransferFault);','t.putString("resourceTransferFault",resourceTransferFault);\n        t.putString("itemTransferFault",itemTransferFault);')
    source=once(source,'t.putBoolean("resourceEscrow",ItemTransferSafety.blocked(this)||','t.putBoolean("resourceEscrow",ResourceTransferSafety.blocked(this)||ItemTransferSafety.blocked(this)||')
    source=once(source,'            if (!pendingFluid.isEmpty()) t.put("pendingFluid",','            if(opaqueFluidEscrow!=null)t.put("pendingFluid",opaqueFluidEscrow.copy());\n            else if (!pendingFluid.isEmpty()) t.put("pendingFluid",')
    get='t.getString("resourceTransferFault")'+('.orElse("")' if modern else '')
    source=once(source,'        p.itemTransferFault=','        p.resourceTransferFault='+get+';\n        if(p.resourceTransferFault.length()>512)p.resourceTransferFault=p.resourceTransferFault.substring(0,512);\n        p.itemTransferFault=')
    load=re.search(r'p.pendingFluid=(.*?); p.pendingEnergy=',source)
    raw='t.getCompound("pendingFluid")'+('.orElseGet('+nbt+'::new)' if modern else '')
    if raw not in load.group(1):raise ValueError('Unrecognized native fluid NBT parse')
    replacement=nbt+' fluidPayload='+raw+';\n        try{p.pendingFluid='+load.group(1).replace(raw,'fluidPayload')+';}catch(RuntimeException invalid){p.pendingFluid=FluidStack.EMPTY;}\n        if(t.contains("pendingFluid")&&p.pendingFluid.isEmpty()){p.opaqueFluidEscrow=fluidPayload.copy();p.resourceTransferFault="Unresolved saved fluid escrow; manual inspection required";}\n        p.pendingEnergy='
    return once(source,load.group(0),replacement)


def part_item(source):
    if 'escrow.resourceTransferFault=part.resourceTransferFault;' in source:return source
    source=once(source,'PartItemDataRules.needsEscrowPayload(ItemTransferSafety.blocked(part)||','PartItemDataRules.needsEscrowPayload(ResourceTransferSafety.blocked(part)||ItemTransferSafety.blocked(part)||')
    return once(source,'escrow.itemTransferFault=part.itemTransferFault;','escrow.resourceTransferFault=part.resourceTransferFault;escrow.opaqueFluidEscrow=part.opaqueFluidEscrow==null?null:part.opaqueFluidEscrow.copy();escrow.itemTransferFault=part.itemTransferFault;')


def cycle_fence(source):
    if 'private static List<NetworkEngine.Ref> available(' in source:return source
    source=once(source,'List<NetworkEngine.Ref> endpoints=plan.endpoints(),transfer=plan.nextDrivers();','List<NetworkEngine.Ref> endpoints=available(plan.endpoints()),transfer=available(plan.nextDrivers());')
    source=once(source,'return !ResourceTransferSafety.blocked(source.part())&&!ResourceTransferSafety.blocked(sink.part())&&source!=sink&&','return source!=sink&&')
    helper='''    /** Healthy cycles reuse their immutable lists. A rare fault filters once, before any routing. */
    private static List<NetworkEngine.Ref> available(List<NetworkEngine.Ref> refs){
        for(NetworkEngine.Ref ref:refs)if(ResourceTransferSafety.blocked(ref.part())){
            List<NetworkEngine.Ref> result=new ArrayList<>();
            for(NetworkEngine.Ref candidate:refs){
                if(ResourceTransferSafety.blocked(candidate.part()))candidate.part().status="Resource transfer quarantined; manual inspection required";
                else result.add(candidate);
            }
            return result;
        }
        return refs;
    }
'''
    return once(source,'    private static boolean hasPending(Part p)',helper+'    private static boolean hasPending(Part p)')


def engine(source):
    if 'ResourceTransferSafety.extractFluid' in source:
        if source.count('ResourceTransferSafety.insertFluid(')!=3 or source.count('ResourceTransferSafety.insertEnergy(')!=3:raise ValueError('Incomplete resource guards')
        return cycle_fence(source)
    source=once(source,'return source!=sink&&','return !ResourceTransferSafety.blocked(source.part())&&!ResourceTransferSafety.blocked(sink.part())&&source!=sink&&')
    source=once(source,'Part p=ref.part();if(ItemTransferSafety.blocked(p))','Part p=ref.part();if(ResourceTransferSafety.blocked(p)){p.status="Resource transfer quarantined; manual inspection required";return;}if(ItemTransferSafety.blocked(p))')
    start=source.index('    // ------------------------------------------------------------ fluids')
    end=source.index('    private static void updateStatus',start)
    before,section,after=source[:start],source[start:end],source[end:]
    # Every driving entry and pending retry is fenced before resolving a provider.
    section=section.replace('Part p=source.part();','Part p=source.part();if(ResourceTransferSafety.blocked(p))return;')
    section=section.replace('Part p=sink.part();','Part p=sink.part();if(ResourceTransferSafety.blocked(p))return;')
    section=section.replace('if(received.contains(target(source))||pairs.contains(pair(source,sink))','if(ResourceTransferSafety.blocked(source.part())||received.contains(target(source))||pairs.contains(pair(source,sink))')
    section=section.replace('FluidStack trial=from.drain(request,IFluidHandler.FluidAction.SIMULATE);if(trial.isEmpty())','FluidStack trial=from.drain(request,IFluidHandler.FluidAction.SIMULATE);if(!ResourceTransferSafety.fluidTrial(request,trial,request.getAmount()))')
    # Request is reassigned during simulation; a final snapshot is needed for the callback.
    old='FluidStack extracted=from.drain(request,IFluidHandler.FluidAction.EXECUTE);'
    section=once(section,old,'FluidStack mutationRequest=request.copy();FluidStack extracted=ResourceTransferSafety.extractFluid(source,mutationRequest,()->from.drain(mutationRequest,IFluidHandler.FluidAction.EXECUTE));') if section.count(old)==1 else section
    if old in section:
        first=section.index(old);section=section[:first]+section[first:].replace(old,'FluidStack mutationRequest=request.copy();FluidStack extracted=ResourceTransferSafety.extractFluid(source,mutationRequest,()->from.drain(mutationRequest,IFluidHandler.FluidAction.EXECUTE));',1)
        section=once(section,old,'FluidStack mutationRequest=request.copy();FluidStack extracted=ResourceTransferSafety.extractFluid(sink,mutationRequest,()->from.drain(mutationRequest,IFluidHandler.FluidAction.EXECUTE));')
    clamp=r'(?:Math|net\.foundations\.pl4\.compat\.PortMath)\.clamp'
    # A malformed simulated amount aborts before withdrawal; actual uncertainty stays fenced.
    section=re.sub(clamp+r'\(to.fill\((send|trial.copy\(\)),IFluidHandler.FluidAction.SIMULATE\),0,(send|trial).getAmount\(\)\)',r'(int)ResourceTransferSafety.amount(to.fill(\1,IFluidHandler.FluidAction.SIMULATE),\2.getAmount())',section)
    pattern=clamp+r'\(to.fill\((send|offered),IFluidHandler.FluidAction.EXECUTE\),0,\1.getAmount\(\)\)'
    matches=list(re.finditer(pattern,section))
    if len(matches)!=3:raise ValueError('Expected three actual fluid insertions')
    owners=iter(['source','sink','sink'])
    section=re.sub(pattern,lambda m:'ResourceTransferSafety.insertFluid('+next(owners)+','+m.group(1)+',()->to.fill('+m.group(1)+',IFluidHandler.FluidAction.EXECUTE))',section)
    # Clear only after pendingFluid is reduced, including healthy refusal (zero inserted).
    section=section.replace('source.host().setChanged();sink.host().setChanged();}}','source.host().setChanged();sink.host().setChanged();}ResourceTransferSafety.completed(source);}')
    section=section.replace('received.add(target(sink));sink.host().setChanged();}return;}','received.add(target(sink));sink.host().setChanged();}ResourceTransferSafety.completed(sink);return;}')
    section=once(section,'p.pendingFluid.shrink(inserted);','p.pendingFluid.shrink(inserted);ResourceTransferSafety.completed(sink);')
    section=once(section,'int trial=from.extractEnergy(sourceBudget,true);','int trial=(int)ResourceTransferSafety.amount(from.extractEnergy(sourceBudget,true),sourceBudget);')
    section=once(section,'int trial=from.extractEnergy(limit,true);','int trial=(int)ResourceTransferSafety.amount(from.extractEnergy(limit,true),limit);')
    section=once(section,'int extracted=Math.max(0,from.extractEnergy(Math.min(trial,space),false));','int request=Math.min(trial,space);int extracted=ResourceTransferSafety.extractEnergy(source,request,()->from.extractEnergy(request,false));')
    section=once(section,'int extracted=Math.max(0,from.extractEnergy(accepted,false));','int extracted=ResourceTransferSafety.extractEnergy(sink,accepted,()->from.extractEnergy(accepted,false));')
    section=re.sub(clamp+r'\(to.receiveEnergy\((offered|trial),true\),0,\1\)',r'(int)ResourceTransferSafety.amount(to.receiveEnergy(\1,true),\1)',section)
    pattern=clamp+r'\(to.receiveEnergy\(offered,false\),0,offered\)'
    if len(list(re.finditer(pattern,section)))!=3:raise ValueError('Expected three FE insertions')
    owners=iter(['source','sink','sink'])
    section=re.sub(pattern,lambda m:'ResourceTransferSafety.insertEnergy('+next(owners)+',offered,()->to.receiveEnergy(offered,false))',section)
    section=once(section,'p.pendingEnergy=extracted-inserted;','p.pendingEnergy=extracted-inserted;ResourceTransferSafety.completed(sink);')
    return cycle_fence(before+section+after)


def native_energy(source):
    if 'ResourceTransferSafety.mutate' in source:return source
    source=once(source,'return (!driver(r)||p.energy)&&','return !ResourceTransferSafety.blocked(p)&&(!driver(r)||p.energy)&&')
    source=once(source,'if(!p.energy)continue;','if(!p.energy)continue;\n            if(ResourceTransferSafety.blocked(p)){p.energyTransferStatus="Energy transfer quarantined; manual inspection required";continue;}')
    source=once(source,'p.pendingEnergyEDRate=rates.fePerElectrodynamicsJ();holder.host().setChanged();','p.pendingEnergyEDRate=rates.fePerElectrodynamicsJ();ResourceTransferSafety.completed(holder);holder.host().setChanged();')
    source=once(source,'extract(NetworkEngine.Ref source,long room,boolean simulate)','extract(NetworkEngine.Ref source,NetworkEngine.Ref holder,long room,boolean simulate)')
    source=once(source,'long actual=from.extract(units,simulate);','final long requested=units;long actual=simulate?from.extract(requested,true):ResourceTransferSafety.mutate(holder,"Native energy extraction",requested,()->from.extract(requested,false));\n        if(!simulate&&actual==0)ResourceTransferSafety.completed(holder);')
    source=once(source,'deliver(NetworkEngine.Ref sink,long available,boolean simulate)','deliver(NetworkEngine.Ref sink,NetworkEngine.Ref holder,long available,boolean simulate)')
    source=once(source,'return EnergyConversion.deliver(to,Math.min(available,limit(sink)),rates.cost(output(sink)),efficiency(sink),simulate);','long maximum=Math.min(available,limit(sink));\n        long accepted=simulate?EnergyConversion.deliver(to,maximum,rates.cost(output(sink)),efficiency(sink),true):ResourceTransferSafety.mutate(holder,"Native energy insertion",maximum,()->EnergyConversion.deliver(to,maximum,rates.cost(output(sink)),efficiency(sink),false));\n        if(!simulate&&accepted==0)ResourceTransferSafety.completed(holder);return accepted;')
    source=source.replace('extract(source,extractionLimit(source),true)','extract(source,source,extractionLimit(source),true)')
    source=once(source,'extract(source,room,false)','extract(source,source,room,false)') if source.count('extract(source,room,false)')==1 else source.replace('extract(source,room,false)','extract(source,source,room,false)',1)
    source=once(source,'extract(source,room,false)','extract(source,sink,room,false)')
    source=source.replace('deliver(sink,remaining,true)','deliver(sink,source,remaining,true)').replace('deliver(sink,combined,true)','deliver(sink,sink,combined,true)')
    # First actual call is a REMOVE flush; the two later calls belong to the ADD holder.
    source=source.replace('deliver(sink,p.energyCredits(),false)','deliver(sink,source,p.energyCredits(),false)',1)
    source=source.replace('deliver(sink,p.energyCredits(),false)','deliver(sink,sink,p.energyCredits(),false)')
    return source


def fixtures(source):
    if 'private static void resourceQuarantineChecks(' in source:return source
    water=re.search(r'var water=new ([\w.]+FluidStack)\(([\w.]+Fluids).WATER,1000\);',source)
    if not water:raise ValueError('Missing target-native fluid fixture types')
    fluid,fluids=water.groups()
    source=once(source,'itemQuarantineChecks(ref(add,Direction.EAST));','itemQuarantineChecks(ref(add,Direction.EAST));resourceQuarantineChecks(ref(add,Direction.EAST));')
    source=once(source,'source.itemTransferFault="Interrupted item insertion";','source.resourceTransferFault="Interrupted fluid insertion";source.pendingFluid=new '+fluid+'('+fluids+'.WATER,725);\n        source.itemTransferFault="Interrupted item insertion";')
    check='throw new IllegalStateException("Item quarantine/count must survive native save reload");'
    source=once(source,check,check+'\n        if(!ResourceTransferSafety.blocked(restored)||restored.pendingFluid.getAmount()!=725||restored.energyRouteEditable())throw new IllegalStateException("Resource quarantine survives native reload");')
    checks='''
    private static void resourceQuarantineChecks(NetworkEngine.Ref owner){
        Part p=owner.part();p.pendingFluid=new FLUID(FLUIDS.WATER,8);int[] calls={0};
        try{ResourceTransferSafety.insertFluid(owner,p.pendingFluid.copy(),()->{calls[0]++;throw new IllegalStateException("fluid provider mutated");});throw new AssertionError("Missing fluid failure");}catch(IllegalStateException expected){}
        if(!ResourceTransferSafety.blocked(p)||p.routeEditable())throw new IllegalStateException("Uncertain fluid insertion must remain fenced");
        try{ResourceTransferSafety.insertFluid(owner,p.pendingFluid.copy(),()->{calls[0]++;return 8;});throw new AssertionError("Replayed fluid");}catch(IllegalStateException expected){}
        if(calls[0]!=1)throw new IllegalStateException("Quarantined fluid callback replayed");
        p.resourceTransferFault="";int refused=ResourceTransferSafety.insertFluid(owner,p.pendingFluid.copy(),()->0);ResourceTransferSafety.completed(owner);
        if(refused!=0||ResourceTransferSafety.blocked(p)||p.pendingFluid.getAmount()!=8)throw new IllegalStateException("Healthy fluid refusal must be retryable");
        try{ResourceTransferSafety.extractFluid(owner,p.pendingFluid.copy(),()->new FLUID(FLUIDS.LAVA,8));throw new AssertionError("Wrong fluid accepted");}catch(IllegalStateException expected){}
        if(!ResourceTransferSafety.blocked(p)||p.pendingFluid.getFluid()!=FLUIDS.LAVA)throw new IllegalStateException("Unexpected fluid variant must be retained but not forwarded");
        p.resourceTransferFault="";
        try{ResourceTransferSafety.insertFluid(owner,p.pendingFluid.copy(),()->9);throw new AssertionError("Over-reported fluid insertion accepted");}catch(IllegalStateException expected){}
        if(!ResourceTransferSafety.blocked(p))throw new IllegalStateException("Invalid fluid insertion stays quarantined");
        p.resourceTransferFault="";p.pendingFluid=FLUID.EMPTY;p.pendingEnergy=8;
        try{ResourceTransferSafety.insertEnergy(owner,8,()->{calls[0]++;throw new IllegalStateException("FE provider mutated");});throw new AssertionError("Missing FE failure");}catch(IllegalStateException expected){}
        try{ResourceTransferSafety.insertEnergy(owner,8,()->{calls[0]++;return 8;});throw new AssertionError("Replayed FE");}catch(IllegalStateException expected){}
        if(calls[0]!=2||!ResourceTransferSafety.blocked(p))throw new IllegalStateException("Uncertain FE must not replay");
        p.resourceTransferFault="";
        try{ResourceTransferSafety.extractEnergy(owner,8,()->9);throw new AssertionError("Over-extraction accepted");}catch(IllegalStateException expected){}
        if(!ResourceTransferSafety.blocked(p)||p.pendingEnergy!=9)throw new IllegalStateException("Over-extracted FE remains quarantined");
        p.resourceTransferFault="";p.pendingEnergy=0;
        if(ResourceTransferSafety.insertEnergy(owner,8,()->0)!=0)throw new IllegalStateException("Refusal changed amount");ResourceTransferSafety.completed(owner);
        if(ResourceTransferSafety.blocked(p))throw new IllegalStateException("Healthy FE refusal must be retryable");
    }
'''.replace('FLUIDS',fluids).replace('FLUID',fluid)
    return source.rsplit('}',1)[0]+checks+'}\n'


def native_fixtures(source):
    if 'private static void nativeMutationQuarantine(' in source:return source
    source=once(source,'"Native J must transfer without FE conversion or rounding");h.succeed();','"Native J must transfer without FE conversion or rounding");nativeMutationQuarantine(h);h.succeed();')
    checks='''
    private static void nativeMutationQuarantine(GameTestHelper h){
        for(boolean add:new boolean[]{false,true}){
            var f=fixture(h,"FE","FE","FE",10,100,add);var holder=add?f.sink:f.source;
            EnergyConversion.Port broken=new EnergyConversion.Port(){
                public long extract(long amount,boolean simulate){return 0;}
                public long insert(long amount,boolean simulate){long inserted=f.to.insert(amount,simulate);if(!simulate)throw new IllegalStateException("PL4 expected native post-mutation failure");return inserted;}
            };
            NativeEnergyTransfers.run(h.getLevel().getServer(),f.plan,(ref,unit)->ref==f.source?f.from:broken);
            h.assertTrue(f.from.stored==0&&f.to.stored==10&&holder.part().energyCredits()==10*EnergyConversion.FE&&ResourceTransferSafety.blocked(holder.part()),"Native post-mutation insertion retains an uncertainty fence on the driving holder");
            f.run(h);h.assertTrue(f.to.stored==10&&holder.part().energyCredits()==10*EnergyConversion.FE,"Native uncertain insertion cannot replay");
            var g=fixture(h,"FE","FE","FE",10,100,add);var extractionHolder=add?g.sink:g.source;
            EnergyConversion.Port brokenSource=new EnergyConversion.Port(){
                public long extract(long amount,boolean simulate){long extracted=g.from.extract(amount,simulate);if(!simulate)throw new IllegalStateException("PL4 expected native extraction failure");return extracted;}
                public long insert(long amount,boolean simulate){return 0;}
            };
            NativeEnergyTransfers.run(h.getLevel().getServer(),g.plan,(ref,unit)->ref==g.source?brokenSource:g.to);
            h.assertTrue(g.from.stored==0&&g.to.stored==0&&ResourceTransferSafety.blocked(extractionHolder.part())&&!extractionHolder.part().routeEditable(),"Interrupted native withdrawal fences even an empty escrow");
            g.run(h);h.assertTrue(g.from.withdrawals==1&&g.to.stored==0,"Native uncertain extraction cannot be retried");
        }
    }
'''
    if 'net.foundations.pl4.compat.PortAssertions.check(' in source:
        checks=checks.replace('h.assertTrue(','net.foundations.pl4.compat.PortAssertions.check(')
    return source.rsplit('}',1)[0]+checks+'}\n'


def cycle_fixture(source):
    if 'Cycle fence must exclude a quarantined driver' in source:return source
    checks='''resourceQuarantineChecks(ref(add,Direction.EAST));
        from.setItem(0,new ItemStack(Items.DIAMOND,5));part(add,Direction.EAST).resourceTransferFault="Interrupted provider mutation";
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        if(from.getItem(0).getCount()!=5||to.getItem(0).getCount()!=17)throw new IllegalStateException("Cycle fence must exclude a quarantined driver");
        part(add,Direction.EAST).resourceTransferFault="";part(node,Direction.WEST).resourceTransferFault="Unresolved passive endpoint";
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        if(from.getItem(0).getCount()!=5||to.getItem(0).getCount()!=17)throw new IllegalStateException("Cycle fence must exclude a quarantined passive endpoint");
        part(node,Direction.WEST).resourceTransferFault="";
        TransferEngine.run(h.getLevel().getServer(),List.of(ref(node,Direction.WEST),ref(add,Direction.EAST)));
        if(!from.getItem(0).isEmpty()||to.getItem(0).getCount()!=22)throw new IllegalStateException("Healthy endpoint resumes without rebuilding the plan");'''
    return once(source,'resourceQuarantineChecks(ref(add,Direction.EAST));',checks)


def drop_fixtures(source):
    if 'Empty escrow does not discard an uncertain mutation fence' in source:return source
    if 'Resource quarantine survives canonical item payload' not in source:
        source=once(source,'p.pendingEnergy=1200;','p.pendingEnergy=1200;p.resourceTransferFault="Interrupted resource mutation";')
        check='"Escrow must survive canonical item payload");'
        assertion='net.foundations.pl4.compat.PortAssertions.check' if 'net.foundations.pl4.compat.PortAssertions.check(' in source else 'h.assertTrue'
        source=once(source,check,check+assertion+'(ResourceTransferSafety.blocked(restored)&&!restored.routeEditable(),"Resource quarantine survives canonical item payload");')
    data=re.search(r'var data=(.*?);[\w.]+\(data!=null&&data.contains\("pl_part"\),"Escrow-bearing',source).group(1)
    loaded=re.search(r'Part restored=(.*?);[\w.]+\(restored.pendingItem.getCount\(\)==17',source).group(1)
    stack=re.search(r'ItemStack stack=(PartItem.stack\(p,.*?\));var data=',source).group(1)
    checks='''
        p.pendingItem=ItemStack.EMPTY;p.pendingFluid=FluidStack.EMPTY;p.energyCredits(0);
        ItemStack faultStack=STACK;var faultData=DATA;
        h.assertTrue(faultData!=null,"Fault-only drop must retain a payload");
        Part faultRestored=LOAD;
        h.assertTrue(ResourceTransferSafety.blocked(faultRestored)&&!faultRestored.routeEditable(),"Empty escrow does not discard an uncertain mutation fence");'''
    checks=checks.replace('STACK',stack).replace('DATA',data.replace('stack','faultStack')).replace('LOAD',loaded.replace('data','faultData'))
    if 'net.foundations.pl4.compat.PortAssertions.check(' in source:
        checks=checks.replace('h.assertTrue(','net.foundations.pl4.compat.PortAssertions.check(')
    return once(source,'"Resource quarantine survives canonical item payload");','"Resource quarantine survives canonical item payload");'+checks)


def edits(root):
    if (root/'src/legacy').exists():raise ValueError('Excluded legacy runtime')
    java=root/'src/main/java/net/foundations/pl4'
    status=json.loads((root/'BUILD_STATUS.json').read_text())
    result={java/'ResourceTransferSafety.java':helper(root),java/'Part.java':part((java/'Part.java').read_text(),status['minecraft'].startswith('26.')),
            java/'PartItem.java':part_item((java/'PartItem.java').read_text()),java/'TransferEngine.java':engine((java/'TransferEngine.java').read_text()),
            java/'NativeEnergyTransfers.java':native_energy((java/'NativeEnergyTransfers.java').read_text())}
    path=java/'NativeEnergyInput.java'
    text=path.read_text()
    if 'ResourceTransferSafety.blocked(p)' not in text:
        text=once(text,'return p.energyCredits()==0||','return !ResourceTransferSafety.blocked(p)&&(p.energyCredits()==0||')
        text=once(text,'p.pendingEnergyEDRate==rates.fePerElectrodynamicsJ();','p.pendingEnergyEDRate==rates.fePerElectrodynamicsJ());')
    result[path]=text
    fixture_path=next(p for p in [root/'src/portTest/java/net/foundations/pl4/R16GameTests.java',java/'R16GameTests.java'] if p.exists())
    result[fixture_path]=cycle_fixture(fixtures(fixture_path.read_text()))
    path=fixture_path.with_name('EnergyIntegrationGameTests.java');result[path]=native_fixtures(path.read_text())
    path=fixture_path.with_name('R13GameTests.java');result[path]=drop_fixtures(path.read_text())
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('root',type=Path);args=parser.parse_args()
    for path,content in edits(args.root.resolve()).items():path.write_text(content)
