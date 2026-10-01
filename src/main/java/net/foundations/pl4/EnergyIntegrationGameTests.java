package net.foundations.pl4;

import java.util.*;
import net.foundations.pl4.core.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native server fixtures for the production route engine; optional provider contracts are also
 * exercised by the dependency-free verifier. Injected ports avoid requiring third-party mods. */
@PrefixGameTestTemplate(false)
public final class EnergyIntegrationGameTests {
    private static final UUID OWNER=UUID.fromString("aaaa0000-0000-0000-0000-000000000019");
    private static final class Battery implements EnergyConversion.Port {
        long stored,capacity,actualLimit=Long.MAX_VALUE;int withdrawals;
        Battery(long amount,long capacity){stored=amount;this.capacity=capacity;}
        public long extract(long amount,boolean simulate){long n=Math.min(amount,stored);if(!simulate){stored-=n;withdrawals++;}return n;}
        public long insert(long amount,boolean simulate){long n=Math.min(amount,capacity-stored);if(!simulate){n=Math.min(n,actualLimit);stored+=n;}return n;}
    }
    private record Fixture(NetworkEngine.Ref source,NetworkEngine.Ref sink,TransferEngine.Plan plan,Battery from,Battery to) {
        void run(GameTestHelper h){NativeEnergyTransfers.run(h.getLevel().getServer(),plan,(ref,unit)->ref==source?from:to);}
    }
    private static NetworkEngine.Ref host(GameTestHelper h,BlockPos pos,Kind kind,Direction face){
        h.setBlock(pos,FoundationsPL4.HOST.get());var host=(HostEntity)h.getBlockEntity(pos);var part=new Part(kind,face,OWNER);host.parts.put(part.slot(),part);host.changed();return new NetworkEngine.Ref(host,part);
    }
    private static Fixture fixture(GameTestHelper h,String input,String wire,String output,long amount,long capacity,boolean add){
        var source=host(h,new BlockPos(2,2,2),add?Kind.NODE:Kind.TRANSFER_NODE,Direction.WEST);
        var sink=host(h,new BlockPos(5,2,2),add||!output.equals("FE")||!wire.equals("FE")?Kind.TRANSFER_NODE:Kind.NODE,Direction.EAST);
        source.part().energyInput=input;source.part().energyOutput=wire;source.part().energyConvert=!input.equals(wire);source.part().transferMode=add?0:2;
        sink.part().energyInput=wire;sink.part().energyOutput=output;sink.part().energyConvert=!wire.equals(output);sink.part().transferMode=1;
        return new Fixture(source,sink,TransferEngine.prepare(List.of(source,sink)),new Battery(amount,amount),new Battery(0,capacity));
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void optionalReaderSystemsPersistAndModelsKeepMultipartGeometry(GameTestHelper h){
        for(String system:List.of("CREATE","AE2")){
            Part p=new Part(Kind.ENERGY_READER,Direction.WEST,OWNER);p.energySystem=system;
            Part restored=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
            h.assertTrue(restored!=null&&restored.energySystem.equals(system),"Optional reader selection survives save/load");
        }
        h.assertTrue(FoundationsPL4.KINETIC_READER_MODEL.get().kind==Kind.ENERGY_READER&&FoundationsPL4.AE2_READER_MODEL.get().kind==Kind.ENERGY_READER,"Adapter models preserve Energy Reader behavior and placement");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void pushedForgeEnergyIsSidedSimulatedBoundedAndRevoked(GameTestHelper h){
        var ref=host(h,new BlockPos(2,2,2),Kind.TRANSFER_NODE,Direction.WEST);Part p=ref.part();p.transferMode=2;
        var cap=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,ref.host().getBlockPos(),Direction.WEST);
        h.assertTrue(cap!=null&&cap.canReceive()&&!cap.canExtract(),"Pushing generators discover a receive-only FE input");
        h.assertTrue(cap.receiveEnergy(100,true)==100&&p.energyCredits()==0,"Simulation must not alter escrow");
        h.assertTrue(cap.receiveEnergy(100,false)==100&&p.energyCredits()==100*EnergyConversion.FE,"Accepted FE is conserved in persistent escrow");
        h.assertTrue(cap.receiveEnergy(Integer.MAX_VALUE,false)==PLConfig.ENERGY_RATE.get()-100&&cap.receiveEnergy(1,false)==0,"External pushes cannot overfill the node");
        p.energyCredits(0);p.transferMode=1;h.assertTrue(!cap.canReceive()&&cap.receiveEnergy(1,false)==0,"ADD mode rejects input even through cached handlers");
        p.transferMode=2;ref.host().parts.remove(p.slot());ref.host().changed();h.assertTrue(cap.receiveEnergy(1,false)==0,"Removed parts revoke cached input handlers");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void nativeJoulesRemainJoulesWhenConversionDisabled(GameTestHelper h){
        boolean old=PLConfig.ENERGY_CONVERSION.get();try{PLConfig.ENERGY_CONVERSION.set(false);
            var f=fixture(h,"J","J","J",25,100,false);f.run(h);
            h.assertTrue(f.from.stored==0&&f.to.stored==25&&f.source.part().energyCredits()==0,"Native J must transfer without FE conversion or rounding");h.succeed();
        }finally{PLConfig.ENERGY_CONVERSION.set(old);}
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void joulesToFEHonorsSharedCapAndTopsUpFractionalEscrow(GameTestHelper h){
        int old=PLConfig.NETWORK_ENERGY_RATE.get();try{PLConfig.NETWORK_ENERGY_RATE.set(1);
            var f=fixture(h,"J","FE","FE",25,100,false);f.run(h);
            h.assertTrue(f.from.stored==22&&f.to.stored==1&&f.source.part().energyCredits()==200000,"Three J cover one FE and preserve 0.2 FE in escrow");
            f.run(h);h.assertTrue(f.from.stored==20&&f.to.stored==2&&f.source.part().energyCredits()==0,"Fractional escrow must combine with two new J without stalling or exceeding one FE/cycle");h.succeed();
        }finally{PLConfig.NETWORK_ENERGY_RATE.set(old);}
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void conversionPolicyAndLossCannotBeBypassed(GameTestHelper h){
        boolean old=PLConfig.ENERGY_CONVERSION.get();int loss=PLConfig.CONVERSION_EFFICIENCY.get();
        try{var f=fixture(h,"J","FE","FE",25,100,false);PLConfig.ENERGY_CONVERSION.set(false);f.run(h);
            h.assertTrue(f.from.stored==25&&f.to.stored==0,"Server disabling conversion must prevent extraction");
            PLConfig.ENERGY_CONVERSION.set(true);f.source.part().energyConvert=false;f.run(h);h.assertTrue(f.from.stored==25,"Node must explicitly enable conversion");
            f.source.part().energyConvert=true;PLConfig.CONVERSION_EFFICIENCY.set(900);f.run(h);
            h.assertTrue(f.from.stored==0&&f.to.stored==9&&f.source.part().energyCredits()==0,"25 J at 90 percent must deliver exactly nine FE");h.succeed();
        }finally{PLConfig.ENERGY_CONVERSION.set(old);PLConfig.CONVERSION_EFFICIENCY.set(loss);}
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void partialNativeAcceptanceRetainsAndRetriesEscrow(GameTestHelper h){
        var f=fixture(h,"J","J","J",10,100,false);f.to.actualLimit=3;f.run(h);
        h.assertTrue(f.from.stored==0&&f.to.stored==3&&f.source.part().energyCredits()==2800000,"Partial provider must leave seven J in escrow");
        f.run(h);h.assertTrue(f.to.stored==6&&f.source.part().energyCredits()==1600000&&f.from.withdrawals==1,"Retry must not extract from source twice");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void addConversionConservesAcrossFractionalRetries(GameTestHelper h){
        int old=PLConfig.NETWORK_ENERGY_RATE.get();try{PLConfig.NETWORK_ENERGY_RATE.set(1);
            var f=fixture(h,"FE","FE","J",10,100,true);
            for(int i=0;i<30;i++){long before=f.to.stored;f.run(h);
                h.assertTrue((10-f.from.stored)*EnergyConversion.FE==f.to.stored*400000+f.sink.part().energyCredits(),"ADD imports and fractions must conserve energy exactly");
                h.assertTrue((f.to.stored-before)*400000<=EnergyConversion.FE,"Shared cap must include ADD and incoming escrow");}
            h.assertTrue(f.from.stored==0&&f.to.stored==25&&f.sink.part().energyCredits()==0,"ADD conversion must eventually drain all fractions");h.succeed();
        }finally{PLConfig.NETWORK_ENERGY_RATE.set(old);}
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void changedConversionRatiosPauseRatherThanRevalueEscrow(GameTestHelper h){
        int old=PLConfig.FE_PER_1000_J.get();try{
            var f=fixture(h,"J","J","J",0,100,false);Part p=f.source.part();p.energyCredits(2000000);p.pendingEnergyUnit="J";p.pendingEnergyJRate=old;p.pendingEnergyEURate=PLConfig.FE_PER_EU.get();p.pendingEnergyEDRate=PLConfig.FE_PER_ED_J.get();
            PLConfig.FE_PER_1000_J.set(old+1);f.run(h);h.assertTrue(f.to.stored==0&&p.energyCredits()==2000000&&p.energyTransferStatus.contains("restore"),"Changed profile cannot mint or discard escrow");
            PLConfig.FE_PER_1000_J.set(old);f.run(h);h.assertTrue(f.to.stored==5&&p.energyCredits()==0,"Restored original ratios resume native escrow");h.succeed();
        }finally{PLConfig.FE_PER_1000_J.set(old);}
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void fractionalEscrowAndRouteSurviveDropsAndRestart(GameTestHelper h){
        Part p=new Part(Kind.TRANSFER_NODE,Direction.WEST,OWNER);p.energyInput="ED_J";p.energyOutput="EU";p.energyConvert=true;p.energyVoltage=128;p.transferMode=2;p.energyCredits(1);p.pendingEnergyUnit="EU";p.pendingEnergyJRate=400;p.pendingEnergyEURate=4;p.pendingEnergyEDRate=1;
        var data=PartItem.stack(p,h.getLevel().registryAccess()).get(DataComponents.CUSTOM_DATA);
        h.assertTrue(data!=null,"Sub-FE escrow must never be lost in a normal node drop");
        Part restored=Part.load(data.copyTag().getCompound("pl_part"),h.getLevel().registryAccess());
        h.assertTrue(restored.energyCredits()==1&&restored.pendingEnergy==0&&restored.pendingEnergyUnit.equals("EU")&&restored.pendingEnergyEDRate==1,"Exact credits and profile survive persistence");
        h.assertTrue(restored.energyInput.equals("ED_J")&&restored.energyOutput.equals("EU")&&restored.energyConvert&&restored.energyVoltage==128&&restored.transferMode==2&&!restored.energyRouteEditable(),"Escrow-required routing settings survive breaking/replacing and remain locked");
        Part synced=Part.load(p.save(h.getLevel().registryAccess(),true),h.getLevel().registryAccess());h.assertTrue(synced.energyCredits()==0&&!synced.energyRouteEditable(),"Client receives lock but no escrow amount");
        restored.energyCredits(0);h.assertTrue(restored.energyRouteEditable(),"Draining escrow unlocks route");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void electrodynamicsNativeFractionsAndIndependentRates(GameTestHelper h){
        var f=fixture(h,"ED_J","ED_J","ED_J",3250000,10000000,false);f.run(h);
        h.assertTrue(f.from.stored==0&&f.to.stored==3250000,"3.25 Electrodynamics J must remain exact in native microjoule transport");
        var rates=EnergyPorts.rates();h.assertTrue(rates.cost("ED_J")==1&&rates.cost("J")==400000,"Electrodynamics and Mekanism units must use distinct adapters and pack ratios");h.succeed();
    }
    public interface EUFixture {
        long acceptEnergyFromNetwork(Direction side,long voltage,long amperage);long removeEnergy(long n);
        long getEnergyStored();long getEnergyCapacity();boolean inputsEnergy(Direction side);boolean outputsEnergy(Direction side);
        long getInputVoltage();long getInputAmperage();long getOutputVoltage();long getOutputAmperage();
    }
    public static final class EUBattery implements EUFixture {
        long stored;int actualCalls;
        public long acceptEnergyFromNetwork(Direction side,long voltage,long amperage){if(voltage>32)throw new AssertionError("Unsafe overvoltage");stored+=voltage*amperage;actualCalls++;return amperage;}
        public long removeEnergy(long n){long result=Math.min(n,stored);stored-=result;return result;}
        public long getEnergyStored(){return stored;}public long getEnergyCapacity(){return 4096;}
        public boolean inputsEnergy(Direction side){return side==Direction.WEST;}public boolean outputsEnergy(Direction side){return side==Direction.EAST;}
        public long getInputVoltage(){return 32;}public long getInputAmperage(){return 2;}public long getOutputVoltage(){return 32;}public long getOutputAmperage(){return 2;}
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void feToEUUsesWholeSafePacketsAndSharedCap(GameTestHelper h)throws Exception {
        int old=PLConfig.NETWORK_ENERGY_RATE.get();try{
            var f=fixture(h,"FE","FE","EU",500,1000,true);var eu=new EUBattery();var adapter=ReflectiveEnergyTransfer.gregtech(EUFixture.class,Direction.class);
            PLConfig.NETWORK_ENERGY_RATE.set(100);
            NativeEnergyTransfers.run(h.getLevel().getServer(),f.plan,(ref,unit)->ref==f.source?f.from:adapter.bind(eu,Direction.WEST,f.sink.part().energyVoltage,1024));
            h.assertTrue(eu.stored==0&&f.from.stored==500&&eu.actualCalls==0,"Cap below one 32 V packet (128 FE) must not withdraw or call the receiver");
            PLConfig.NETWORK_ENERGY_RATE.set(128);
            NativeEnergyTransfers.run(h.getLevel().getServer(),f.plan,(ref,unit)->ref==f.source?f.from:adapter.bind(eu,Direction.WEST,f.sink.part().energyVoltage,1024));
            h.assertTrue(eu.stored==32&&f.from.stored==372&&eu.actualCalls==1,"One 32 EU packet costs exactly 128 FE and one amp");
            f.sink.part().energyVoltage=128;
            NativeEnergyTransfers.run(h.getLevel().getServer(),f.plan,(ref,unit)->ref==f.source?f.from:adapter.bind(eu,Direction.WEST,f.sink.part().energyVoltage,1024));
            h.assertTrue(eu.stored==32&&f.from.stored==372&&eu.actualCalls==1,"Node voltage above machine rating must fail closed before extraction");h.succeed();
        }finally{PLConfig.NETWORK_ENERGY_RATE.set(old);}
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void multipleNativeDriversShareOneNetworkBudget(GameTestHelper h){
        int old=PLConfig.NETWORK_ENERGY_RATE.get();try{PLConfig.NETWORK_ENERGY_RATE.set(5);
            var f=fixture(h,"J","FE","FE",25,100,false);var second=host(h,new BlockPos(3,3,3),Kind.TRANSFER_NODE,Direction.WEST);second.part().transferMode=2;second.part().energyInput="J";second.part().energyOutput="FE";second.part().energyConvert=true;var extra=new Battery(25,25);
            var plan=TransferEngine.prepare(List.of(f.source,second,f.sink));
            NativeEnergyTransfers.run(h.getLevel().getServer(),plan,(ref,unit)->ref==f.source?f.from:ref==second?extra:f.to);
            h.assertTrue(f.to.stored==5&&f.from.stored+extra.stored==37,"Two J exporters share five delivered FE, not five each");
            h.assertTrue(f.source.part().energyCredits()+second.part().energyCredits()==200000,"The 13th withdrawn J's fractional excess stays in escrow");h.succeed();
        }finally{PLConfig.NETWORK_ENERGY_RATE.set(old);}
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void communityMessageContainsOnlyRequestedClickableDestinations(GameTestHelper h){
        var message=CommunityLinks.message();List<ClickEvent> clicks=new ArrayList<>();for(var component:message.getSiblings())if(component.getStyle().getClickEvent()!=null)clicks.add(component.getStyle().getClickEvent());
        h.assertTrue(clicks.size()==2&&clicks.get(0).getAction()==ClickEvent.Action.OPEN_URL&&clicks.get(0).getValue().equals("https://discord.gg/tCTS9xduad")&&clicks.get(1).getValue().equals("https://ko-fi.com/shnewbs"),"Exactly two requested links, no commands or automatic browser opening");
        h.assertTrue(message.getString().equals("[PL4] Discord · Support on Ko-fi"),"One short chat line");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void pushOnlyEUFeedsConversionEscrow(GameTestHelper h){
        int oldRate=PLConfig.ENERGY_RATE.get(),oldCap=PLConfig.NETWORK_ENERGY_RATE.get();
        try{PLConfig.ENERGY_RATE.set(256);PLConfig.NETWORK_ENERGY_RATE.set(128);
            var f=fixture(h,"EU","FE","FE",0,1000,false);Part p=f.source.part();
            long accepted=NativeEnergyInput.accept(f.source.host(),p,Direction.WEST,Direction.WEST,32,10);
            h.assertTrue(accepted==2&&p.energyCredits()==256*EnergyConversion.FE,"Push-only source fills bounded converted escrow without extraction");
            h.assertTrue(NativeEnergyInput.accept(f.source.host(),p,Direction.WEST,Direction.WEST,32,1)==0,"Full buffer rejects packets");
            f.run(h);h.assertTrue(f.to.stored==128&&p.energyCredits()==128*EnergyConversion.FE,"Shared network cap applies to pushed EU delivery");
            f.run(h);h.assertTrue(f.to.stored==256&&p.energyCredits()==0&&f.from.withdrawals==0,"Pushed packets drain exactly once without pulling source storage");h.succeed();
        }finally{PLConfig.ENERGY_RATE.set(oldRate);PLConfig.NETWORK_ENERGY_RATE.set(oldCap);}
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID)
    public static void pushedEURespectsSideVoltageAndPolicy(GameTestHelper h){
        boolean conversion=PLConfig.ENERGY_CONVERSION.get();
        try{var f=fixture(h,"EU","FE","FE",0,1000,false);Part p=f.source.part();
            h.assertTrue(NativeEnergyInput.accept(f.source.host(),p,Direction.WEST,Direction.EAST,32,1)==0,"Wrong side rejects input");
            h.assertTrue(NativeEnergyInput.accept(f.source.host(),p,Direction.WEST,Direction.WEST,128,1)==0,"Overvoltage rejected without accepting packet");
            PLConfig.ENERGY_CONVERSION.set(false);h.assertTrue(NativeEnergyInput.accept(f.source.host(),p,Direction.WEST,Direction.WEST,32,1)==0,"Server conversion policy enforced on push");
            PLConfig.ENERGY_CONVERSION.set(true);h.assertTrue(NativeEnergyInput.accept(f.source.host(),p,Direction.WEST,Direction.WEST,32,1)==1,"Configured input accepts safe packet");
            p.pendingEnergyEURate++;h.assertTrue(NativeEnergyInput.accept(f.source.host(),p,Direction.WEST,Direction.WEST,32,1)==0,"Ratio mismatch prevents mixing escrow profiles");
            h.assertTrue(p.energyCredits()==128*EnergyConversion.FE,"Rejected packets never change escrow");h.succeed();
        }finally{PLConfig.ENERGY_CONVERSION.set(conversion);}
    }

}
