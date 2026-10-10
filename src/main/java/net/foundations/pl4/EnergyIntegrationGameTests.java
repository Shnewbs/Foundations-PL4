package net.foundations.pl4;

import java.util.*;
import net.foundations.pl4.core.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.foundations.pl4.compat.DataComponents;
import net.minecraft.network.chat.ClickEvent;

/** Native server fixtures for the production route engine; optional provider contracts are also
 * exercised by the dependency-free verifier. Injected ports avoid requiring third-party mods. */
@net.minecraftforge.gametest.GameTestHolder(value=FoundationsPL4.ID,namespace=FoundationsPL4.ID)
@net.minecraftforge.gametest.GameTestDontPrefix
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
    @GameTest(template="empty")
    public static void layoutTemplateRoundTripFitAndMalformedInput(GameTestHelper h){
        var e=DisplayElements.create(DisplayElements.Type.TEXT,7,100,100).bounds(new DisplayElements.Rect(50,50,40,40));
        for(int preset=0;preset<3;preset++)h.assertTrue(LayoutTemplate.decode(LayoutTemplate.preset(preset,100,100).encode()).elements().size()==2,"Starter boards round-trip with typed components");
        var template=new LayoutTemplate(100,100,List.of(e));var decoded=LayoutTemplate.decode(template.encode());
        h.assertTrue(decoded.elements().equals(template.elements()),"Versioned JSON preserves pages and text styles");
        var prepared=decoded.prepare(20,20,true,false);
        h.assertTrue(!prepared.get(0).id().equals(e.id())&&prepared.get(0).page()==7&&prepared.get(0).bounds().right()<=20&&prepared.get(0).bounds().bottom()<=20,"Fit imports fresh IDs inside a smaller canvas without losing page identity");
        boolean rejected=false;try{decoded.prepare(20,20,false,false);}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Oversized layouts require explicit fitting");
        rejected=false;try{LayoutTemplate.decode(template.encode().replace("\"schema\":1","\"schema\":2"));}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Unknown schema is rejected");
        rejected=false;try{new LayoutTemplate(100,100,List.of(e,e));}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Duplicate IDs are rejected");
        rejected=false;try{LayoutTemplate.decode("x".repeat(LayoutTemplate.MAX_TEXT+1));}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Oversized clipboard input is rejected before parsing");h.succeed();
    }
    @GameTest(template="empty")
    public static void optionalReaderSystemsPersistAndModelsKeepMultipartGeometry(GameTestHelper h){
        for(String system:List.of("CREATE","AE2")){
            Part p=new Part(Kind.ENERGY_READER,Direction.WEST,OWNER);p.energySystem=system;
            Part restored=Part.load(p.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
            h.assertTrue(restored!=null&&restored.energySystem.equals(system),"Optional reader selection survives save/load");
        }
        h.assertTrue(FoundationsPL4.KINETIC_READER_MODEL.get().kind==Kind.ENERGY_READER&&FoundationsPL4.AE2_READER_MODEL.get().kind==Kind.ENERGY_READER,"Adapter models preserve Energy Reader behavior and placement");h.succeed();
    }
    @GameTest(template="empty")
    public static void pushedForgeEnergyIsSidedSimulatedBoundedAndRevoked(GameTestHelper h){
        var ref=host(h,new BlockPos(2,2,2),Kind.TRANSFER_NODE,Direction.WEST);Part p=ref.part();p.transferMode=2;
        var cap=net.foundations.pl4.compat.PortCapabilities.get(h.getLevel(),net.foundations.pl4.compat.Capabilities.EnergyStorage.BLOCK,ref.host().getBlockPos(),Direction.WEST);
        h.assertTrue(cap!=null&&cap.canReceive()&&!cap.canExtract(),"Pushing generators discover a receive-only FE input");
        h.assertTrue(cap.receiveEnergy(100,true)==100&&p.energyCredits()==0,"Simulation must not alter escrow");
        h.assertTrue(cap.receiveEnergy(100,false)==100&&p.energyCredits()==100*EnergyConversion.FE,"Accepted FE is conserved in persistent escrow");
        h.assertTrue(cap.receiveEnergy(Integer.MAX_VALUE,false)==PLConfig.ENERGY_RATE.get()-100&&cap.receiveEnergy(1,false)==0,"External pushes cannot overfill the node");
        p.energyCredits(0);p.transferMode=1;h.assertTrue(!cap.canReceive()&&cap.receiveEnergy(1,false)==0,"ADD mode rejects input even through cached handlers");
        p.transferMode=2;ref.host().parts.remove(p.slot());ref.host().changed();h.assertTrue(cap.receiveEnergy(1,false)==0,"Removed parts revoke cached input handlers");h.succeed();
    }
    @GameTest(template="empty")
    public static void nativeJoulesRemainJoulesWhenConversionDisabled(GameTestHelper h){
        boolean old=PLConfig.ENERGY_CONVERSION.get();try{PLConfig.ENERGY_CONVERSION.set(false);
            var f=fixture(h,"J","J","J",25,100,false);f.run(h);
            h.assertTrue(f.from.stored==0&&f.to.stored==25&&f.source.part().energyCredits()==0,"Native J must transfer without FE conversion or rounding");h.succeed();
        }finally{PLConfig.ENERGY_CONVERSION.set(old);}
    }
    @GameTest(template="empty")
    public static void joulesToFEHonorsSharedCapAndTopsUpFractionalEscrow(GameTestHelper h){
        int old=PLConfig.NETWORK_ENERGY_RATE.get();try{PLConfig.NETWORK_ENERGY_RATE.set(1);
            var f=fixture(h,"J","FE","FE",25,100,false);f.run(h);
            h.assertTrue(f.from.stored==22&&f.to.stored==1&&f.source.part().energyCredits()==200000,"Three J cover one FE and preserve 0.2 FE in escrow");
            f.run(h);h.assertTrue(f.from.stored==20&&f.to.stored==2&&f.source.part().energyCredits()==0,"Fractional escrow must combine with two new J without stalling or exceeding one FE/cycle");h.succeed();
        }finally{PLConfig.NETWORK_ENERGY_RATE.set(old);}
    }
    @GameTest(template="empty")
    public static void conversionPolicyAndLossCannotBeBypassed(GameTestHelper h){
        boolean old=PLConfig.ENERGY_CONVERSION.get();int loss=PLConfig.CONVERSION_EFFICIENCY.get();
        try{var f=fixture(h,"J","FE","FE",25,100,false);PLConfig.ENERGY_CONVERSION.set(false);f.run(h);
            h.assertTrue(f.from.stored==25&&f.to.stored==0,"Server disabling conversion must prevent extraction");
            PLConfig.ENERGY_CONVERSION.set(true);f.source.part().energyConvert=false;f.run(h);h.assertTrue(f.from.stored==25,"Node must explicitly enable conversion");
            f.source.part().energyConvert=true;PLConfig.CONVERSION_EFFICIENCY.set(900);f.run(h);
            h.assertTrue(f.from.stored==0&&f.to.stored==9&&f.source.part().energyCredits()==0,"25 J at 90 percent must deliver exactly nine FE");h.succeed();
        }finally{PLConfig.ENERGY_CONVERSION.set(old);PLConfig.CONVERSION_EFFICIENCY.set(loss);}
    }
    @GameTest(template="empty")
    public static void partialNativeAcceptanceRetainsAndRetriesEscrow(GameTestHelper h){
        var f=fixture(h,"J","J","J",10,100,false);f.to.actualLimit=3;f.run(h);
        h.assertTrue(f.from.stored==0&&f.to.stored==3&&f.source.part().energyCredits()==2800000,"Partial provider must leave seven J in escrow");
        f.run(h);h.assertTrue(f.to.stored==6&&f.source.part().energyCredits()==1600000&&f.from.withdrawals==1,"Retry must not extract from source twice");h.succeed();
    }
    @GameTest(template="empty")
    public static void addConversionConservesAcrossFractionalRetries(GameTestHelper h){
        int old=PLConfig.NETWORK_ENERGY_RATE.get();try{PLConfig.NETWORK_ENERGY_RATE.set(1);
            var f=fixture(h,"FE","FE","J",10,100,true);
            for(int i=0;i<30;i++){long before=f.to.stored;f.run(h);
                h.assertTrue((10-f.from.stored)*EnergyConversion.FE==f.to.stored*400000+f.sink.part().energyCredits(),"ADD imports and fractions must conserve energy exactly");
                h.assertTrue((f.to.stored-before)*400000<=EnergyConversion.FE,"Shared cap must include ADD and incoming escrow");}
            h.assertTrue(f.from.stored==0&&f.to.stored==25&&f.sink.part().energyCredits()==0,"ADD conversion must eventually drain all fractions");h.succeed();
        }finally{PLConfig.NETWORK_ENERGY_RATE.set(old);}
    }
    @GameTest(template="empty")
    public static void changedConversionRatiosPauseRatherThanRevalueEscrow(GameTestHelper h){
        int old=PLConfig.FE_PER_1000_J.get();try{
            var f=fixture(h,"J","J","J",0,100,false);Part p=f.source.part();p.energyCredits(2000000);p.pendingEnergyUnit="J";p.pendingEnergyJRate=old;p.pendingEnergyEURate=PLConfig.FE_PER_EU.get();p.pendingEnergyEDRate=PLConfig.FE_PER_ED_J.get();
            PLConfig.FE_PER_1000_J.set(old+1);f.run(h);h.assertTrue(f.to.stored==0&&p.energyCredits()==2000000&&p.energyTransferStatus.contains("restore"),"Changed profile cannot mint or discard escrow");
            PLConfig.FE_PER_1000_J.set(old);f.run(h);h.assertTrue(f.to.stored==5&&p.energyCredits()==0,"Restored original ratios resume native escrow");h.succeed();
        }finally{PLConfig.FE_PER_1000_J.set(old);}
    }
    @GameTest(template="empty")
    public static void fractionalEscrowAndRouteSurviveDropsAndRestart(GameTestHelper h){
        Part p=new Part(Kind.TRANSFER_NODE,Direction.WEST,OWNER);p.energyInput="ED_J";p.energyOutput="EU";p.energyConvert=true;p.energyVoltage=128;p.transferMode=2;p.energyCredits(1);p.pendingEnergyUnit="EU";p.pendingEnergyJRate=400;p.pendingEnergyEURate=4;p.pendingEnergyEDRate=1;
        var data=net.foundations.pl4.compat.PortData.get(PartItem.stack(p,h.getLevel().registryAccess()),DataComponents.CUSTOM_DATA);
        h.assertTrue(data!=null,"Sub-FE escrow must never be lost in a normal node drop");
        Part restored=Part.load(data.copyTag().getCompound("pl_part"),h.getLevel().registryAccess());
        h.assertTrue(restored.energyCredits()==1&&restored.pendingEnergy==0&&restored.pendingEnergyUnit.equals("EU")&&restored.pendingEnergyEDRate==1,"Exact credits and profile survive persistence");
        h.assertTrue(restored.energyInput.equals("ED_J")&&restored.energyOutput.equals("EU")&&restored.energyConvert&&restored.energyVoltage==128&&restored.transferMode==2&&!restored.energyRouteEditable(),"Escrow-required routing settings survive breaking/replacing and remain locked");
        Part synced=Part.load(p.save(h.getLevel().registryAccess(),true),h.getLevel().registryAccess());h.assertTrue(synced.energyCredits()==0&&!synced.energyRouteEditable(),"Client receives lock but no escrow amount");
        restored.energyCredits(0);h.assertTrue(restored.energyRouteEditable(),"Draining escrow unlocks route");h.succeed();
    }
    @GameTest(template="empty")
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
    @GameTest(template="empty")
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
    @GameTest(template="empty")
    public static void multipleNativeDriversShareOneNetworkBudget(GameTestHelper h){
        int old=PLConfig.NETWORK_ENERGY_RATE.get();try{PLConfig.NETWORK_ENERGY_RATE.set(5);
            var f=fixture(h,"J","FE","FE",25,100,false);var second=host(h,new BlockPos(3,3,3),Kind.TRANSFER_NODE,Direction.WEST);second.part().transferMode=2;second.part().energyInput="J";second.part().energyOutput="FE";second.part().energyConvert=true;var extra=new Battery(25,25);
            var plan=TransferEngine.prepare(List.of(f.source,second,f.sink));
            NativeEnergyTransfers.run(h.getLevel().getServer(),plan,(ref,unit)->ref==f.source?f.from:ref==second?extra:f.to);
            h.assertTrue(f.to.stored==5&&f.from.stored+extra.stored==37,"Two J exporters share five delivered FE, not five each");
            h.assertTrue(f.source.part().energyCredits()+second.part().energyCredits()==200000,"The 13th withdrawn J's fractional excess stays in escrow");h.succeed();
        }finally{PLConfig.NETWORK_ENERGY_RATE.set(old);}
    }
    @GameTest(template="empty")
    public static void communityMessageContainsOnlyRequestedClickableDestinations(GameTestHelper h){
        var message=CommunityLinks.message();List<ClickEvent> clicks=new ArrayList<>();for(var component:message.getSiblings())if(component.getStyle().getClickEvent()!=null)clicks.add(component.getStyle().getClickEvent());
        h.assertTrue(clicks.size()==2&&clicks.get(0).getAction()==ClickEvent.Action.OPEN_URL&&clicks.get(0).getValue().equals("https://discord.gg/tCTS9xduad")&&clicks.get(1).getValue().equals("https://ko-fi.com/shnewbs"),"Exactly two requested links, no commands or automatic browser opening");
        h.assertTrue(message.getString().equals("[PL4] Discord · Support on Ko-fi"),"One short chat line");h.succeed();
    }
    @GameTest(template="empty")
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
    @GameTest(template="empty")
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


    @GameTest(template="empty")
    public static void readerChannelsStayPinnedAcrossOrderAndSave(GameTestHelper h){
        Part reader=new Part(Kind.INVENTORY_READER,Direction.UP,OWNER);reader.mode="SLOT";reader.index=4;
        var a=new Part.Link("minecraft:overworld",new BlockPos(1,2,3),Direction.NORTH,null,null);
        var b=new Part.Link("minecraft:overworld",new BlockPos(4,5,6),Direction.SOUTH,null,null);
        reader.targetChannel=ReaderChannels.id(b);
        h.assertTrue(ReaderChannels.select(reader,List.of(a,b)).equals(List.of(b))&&ReaderChannels.select(reader,List.of(b,a)).equals(List.of(b)),"Pinned target survives ordering changes");
        h.assertTrue(ReaderChannels.select(reader,List.of(a)).isEmpty(),"Missing pinned target never falls back to another inventory");
        h.assertTrue(!ReaderChannels.id(a).equals(ReaderChannels.id(new Part.Link(a.dimension(),a.pos(),Direction.SOUTH,null,null))),"Sided endpoints remain distinct");
        reader.targetChoices.add(new Part.ReaderChoice(ReaderChannels.id(b),"Machine B","block"));
        Part disk=Part.load(reader.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        Part sync=Part.load(reader.save(h.getLevel().registryAccess(),true),h.getLevel().registryAccess());
        h.assertTrue(disk.targetChannel.equals(reader.targetChannel)&&disk.index==4&&disk.mode.equals("SLOT")&&disk.targetChoices.isEmpty(),"Save keeps independent channel/slot but excludes derived choices");
        h.assertTrue(sync.targetChoices.equals(reader.targetChoices),"Client receives bounded channel choices");
        reader.targetChannel="";reader.mode="CHANNEL";reader.index=2;
        h.assertTrue(ReaderChannels.select(reader,List.of(a,b)).equals(List.of(b)),"Legacy ordinal channels still load");h.succeed();
    }
    @GameTest(template="empty")
    public static void infoProvidersBoundIsolateAndUnregister(GameTestHelper h){
        var target=new Part.Link("minecraft:overworld",h.absolutePos(new BlockPos(1,1,1)),Direction.UP,null,null);
        try(var failing=net.foundations.pl4.api.InfoProviders.register("pl4_test:a_failing",(context,out)->{if(!context.target().equals(target))return;out.add("partial","Partial",1,1,"");throw new IllegalStateException("Expected provider isolation fixture");});
            var good=net.foundations.pl4.api.InfoProviders.register("pl4_test:b_good",(context,out)->{if(!context.target().equals(target))return;out.add("bad","Bad",Double.NaN,0,"");out.add("bad_capacity","Bad",1,-1,"");out.add("health","Machine health",7,10,"");out.add("health","Duplicate",999,0,"");for(int i=0;i<100;i++)out.add("value"+i,"Value",i,100,"");})){
            var rows=net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),target);
            h.assertTrue(rows.stream().anyMatch(r->r.key().equals("x")),"Vanilla keys remain available");
            h.assertTrue(rows.stream().noneMatch(r->r.key().startsWith("pl4_test:a_failing/")),"A failing provider contributes no partial sample");
            h.assertTrue(rows.stream().filter(r->r.key().startsWith("pl4_test:b_good/")).count()==32,"Provider row budget is enforced");
            h.assertTrue(rows.stream().anyMatch(r->r.key().equals("pl4_test:b_good/health")&&r.value()==7),"Namespaced first value wins duplicate key");
            h.assertTrue(rows.stream().noneMatch(r->r.key().endsWith("/bad")||r.key().endsWith("/bad_capacity")),"Invalid numeric samples are rejected");
            boolean duplicate=false;try{net.foundations.pl4.api.InfoProviders.register("pl4_test:b_good",(c,o)->{});}catch(IllegalArgumentException expected){duplicate=true;}h.assertTrue(duplicate,"Duplicate registration cannot replace a provider");
        }
        h.assertTrue(net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),target).stream().noneMatch(r->r.key().startsWith("pl4_test:")),"Closing registrations releases providers");h.succeed();
    }
    @GameTest(template="empty")
    public static void infoReaderUsesProvidersAndTrimsMetricKeys(GameTestHelper h){
        var reader=host(h,new BlockPos(2,2,2),Kind.INFO_READER,Direction.UP);
        var target=new Part.Link("minecraft:overworld",h.absolutePos(new BlockPos(3,2,2)),Direction.UP,null,null);
        reader.part().metric=" x, pl4_test:sample/progress ";
        try(var provider=net.foundations.pl4.api.InfoProviders.register("pl4_test:sample",(context,out)->{if(context.target().equals(target))out.add("progress","Progress",25,100,"%");})){
            var rows=DataSampler.sample(h.getLevel().getServer(),reader,List.of(target),1);
            h.assertTrue(rows.size()==2&&rows.stream().anyMatch(r->r.key().equals("pl4_test:sample/progress")&&r.value()==25),"Production Info Reader samples extensions and trims key filters");
            reader.part().mode="STORAGE";
            h.assertTrue(DataSampler.sample(h.getLevel().getServer(),reader,List.of(target),1).equals(rows),"Storage mode must not replace Info Reader telemetry with an item counter");
            var absent=new Part.Link("minecraft:overworld",target.pos(),Direction.UP,UUID.randomUUID(),null);
            h.assertTrue(net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),absent).isEmpty(),"Unavailable entities never invoke providers");
        }h.succeed();
    }

    @GameTest(template="empty")
    public static void readerPagesSearchAndPinsBeyondSixtyFour(GameTestHelper h){
        Part reader=new Part(Kind.INVENTORY_READER,Direction.UP,OWNER);List<Part.Link> links=new ArrayList<>();
        for(int i=0;i<130;i++)links.add(new Part.Link("minecraft:overworld",h.absolutePos(new BlockPos(i,2,1)),Direction.NORTH,null,null));
        reader.targetChannel=ReaderChannels.id(links.get(129));
        ReaderChannels.refresh(h.getLevel().getServer(),reader,links);
        h.assertTrue(reader.targetChoices.size()==64&&reader.targetCount==130&&!ReaderChannels.label(reader).contains("disconnected"),"Pinned endpoint beyond first page remains identifiable");
        reader.targetPage=2;ReaderChannels.refresh(h.getLevel().getServer(),reader,links);
        h.assertTrue(reader.targetChoices.size()==2&&net.foundations.pl4.compat.PortLists.last(reader.targetChoices).id().equals(reader.targetChannel),"Last page exposes targets beyond original limit");
        h.assertTrue(ReaderChannels.rename(reader," Main Tank "),"Selected channel can be named");reader.targetQuery="MAIN TANK";
        ReaderChannels.refresh(h.getLevel().getServer(),reader,links);
        h.assertTrue(reader.targetPage==0&&reader.targetCount==1&&reader.targetChoices.get(0).name().startsWith("Main Tank"),"Case-insensitive alias search and page clamp");
        var disk=Part.load(reader.save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(disk.channelNames.equals(reader.channelNames)&&disk.targetQuery.equals(reader.targetQuery),"Aliases and query survive saves");
        reader.targetQuery="no matching target";ReaderChannels.refresh(h.getLevel().getServer(),reader,links);
        h.assertTrue(reader.targetChoices.isEmpty()&&!ReaderChannels.label(reader).contains("disconnected")&&ReaderChannels.select(reader,links).equals(List.of(net.foundations.pl4.compat.PortLists.last(links))),"Search does not change the selected sampling endpoint");
        ReaderChannels.refresh(h.getLevel().getServer(),reader,List.of());h.assertTrue(ReaderChannels.label(reader).contains("disconnected"),"Actual endpoint removal is reported");h.succeed();
    }
    @GameTest(template="empty")
    public static void oldProviderHandleCannotRemoveReplacement(GameTestHelper h){
        var target=new Part.Link("minecraft:overworld",h.absolutePos(new BlockPos(1,1,1)),Direction.UP,null,null);
        net.foundations.pl4.api.InfoProviders.Provider callback=(c,out)->{if(c.target().equals(target))out.add("value","Value",42,100,"");};
        var old=net.foundations.pl4.api.InfoProviders.register("pl4_test:reused",callback);old.close();
        try(var current=net.foundations.pl4.api.InfoProviders.register("pl4_test:reused",callback)){
            old.close();
            h.assertTrue(net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),target).stream().anyMatch(r->r.key().equals("pl4_test:reused/value")&&r.value()==42),"Repeated close on old handle cannot remove re-registration of same callback");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void inventorySamplingDeduplicatesVanillaDoubleChest(GameTestHelper h){
        var reader=host(h,new BlockPos(5,2,2),Kind.INVENTORY_READER,Direction.UP);
        var left=h.absolutePos(new BlockPos(2,1,2));var right=left.east();
        var state=net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING,Direction.NORTH);
        h.getLevel().setBlock(left,state.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT),2);
        h.getLevel().setBlock(right,state.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.RIGHT),2);
        ((net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(left)).setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,17));
        ((net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(right)).setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,3));
        var a=new Part.Link("minecraft:overworld",left,Direction.UP,null,null);var b=new Part.Link("minecraft:overworld",right,Direction.NORTH,null,null);
        var rows=DataSampler.sample(h.getLevel().getServer(),reader,List.of(a,b,a),1);
        h.assertTrue(rows.size()==1&&rows.get(0).value()==20,"Both halves and repeated links count one combined inventory");
        reader.part().mode="STORAGE";var storage=DataSampler.sample(h.getLevel().getServer(),reader,List.of(a,b),1).get(0);
        var nativeHandler=net.foundations.pl4.compat.PortCapabilities.get(h.getLevel(),net.foundations.pl4.compat.Capabilities.ItemHandler.BLOCK,left,Direction.UP);
        h.assertTrue(nativeHandler!=null&&nativeHandler.getSlots()==54,"Fixture exposes the whole double chest");
        long expectedCapacity=0;for(int i=0;i<54;i++)expectedCapacity+=nativeHandler.getSlotLimit(i);
        h.assertTrue(storage.value()==20&&storage.capacity()==expectedCapacity&&expectedCapacity>0,"Storage reports the native general capacity once, not a guessed stack size: "+storage.capacity()+" / "+expectedCapacity);h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void liveFurnaceReportsCookingAndFuel(GameTestHelper h){
        BlockPos pos=new BlockPos(2,1,2);h.setBlock(pos,net.minecraft.world.level.block.Blocks.FURNACE);
        var furnace=(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        furnace.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON));
        furnace.setItem(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL));
        h.runAtTickTime(20,()->{
            var target=new Part.Link("minecraft:overworld",h.absolutePos(pos),Direction.UP,null,null);
            var rows=net.foundations.pl4.api.InfoProviders.sample(h.getLevel(),target);
            h.assertTrue(rows.stream().anyMatch(r->r.key().equals("cook_time")&&r.value()>0&&r.capacity()>r.value()),"Actual furnace progress must be live and bounded");
            h.assertTrue(rows.stream().anyMatch(r->r.key().equals("burn_time")&&r.value()>0),"Actual remaining fuel time must be reported");h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void networkDiagnosticsSeparateUnavailableEndpoints(GameTestHelper h){
        var reader=host(h,new BlockPos(2,2,2),Kind.NETWORK_READER,Direction.UP);
        var block=new Part.Link("minecraft:overworld",h.absolutePos(new BlockPos(3,2,2)),Direction.UP,null,null);
        var entity=new Part.Link("minecraft:overworld",block.pos(),Direction.UP,UUID.randomUUID(),null);
        var rows=DataSampler.sample(h.getLevel().getServer(),reader,List.of(block,block,entity),1);
        h.assertTrue(rows.stream().anyMatch(r->r.key().equals("targets")&&r.value()==2)&&rows.stream().anyMatch(r->r.key().equals("available")&&r.value()==1)&&rows.stream().anyMatch(r->r.key().equals("unavailable")&&r.value()==1),"Diagnostics deduplicate exact links and distinguish missing entities");h.succeed();
    }

    @GameTest(template="empty")
    public static void readerEditsRejectForeignOwnersStaleIdsAndForgedTargets(GameTestHelper h){
        var ref=host(h,new BlockPos(2,2,2),Kind.INVENTORY_READER,Direction.UP);var pos=ref.host().getBlockPos();Part part=ref.part();
        var stranger=net.foundations.pl4.compat.TestPlayers.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.fromString("bbbb1111-0000-0000-0000-000000000001"),"PL4-foreign"));
        stranger.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        h.assertTrue(!ref.host().canEdit(stranger),"Fixture uses a foreign non-operator");
        PLPackets.edit(stranger,new PLPackets.Edit(pos,part.slot(),part.identity,"label","forged"));
        h.assertTrue(part.label.isEmpty(),"Foreign owner cannot rename the reader");
        var owner=net.foundations.pl4.compat.TestPlayers.get(h.getLevel(),new com.mojang.authlib.GameProfile(OWNER,"PL4-owner"));
        owner.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);h.assertTrue(ref.host().canEdit(owner),"Fixture owner can edit");
        PLPackets.edit(owner,new PLPackets.Edit(pos,part.slot(),UUID.randomUUID(),"label","stale"));
        h.assertTrue(part.label.isEmpty(),"Replaced part identity rejects old UI edits");
        String forged=UUID.randomUUID().toString();part.targetChoices.add(new Part.ReaderChoice(forged,"Stale entry","block"));
        PLPackets.edit(owner,new PLPackets.Edit(pos,part.slot(),part.identity,"target_channel",forged));
        h.assertTrue(part.targetChannel.isEmpty(),"Cached choices cannot grant access to absent network endpoints");
        NetworkEngine.ensureCurrent(h.getLevel().getServer());long builds=NetworkEngine.topologyBuildCount();
        PLPackets.edit(owner,new PLPackets.Edit(pos,part.slot(),part.identity,"target_query","chest"));
        PLPackets.edit(owner,new PLPackets.Edit(pos,part.slot(),part.identity,"target_page","0"));
        h.assertTrue(part.targetQuery.equals("chest")&&NetworkEngine.topologyBuildCount()==builds,"Reader browsing must not rebuild unchanged network topology");
        owner.setPos(pos.getX()+100,pos.getY(),pos.getZ());
        PLPackets.edit(owner,new PLPackets.Edit(pos,part.slot(),part.identity,"label","remote"));
        h.assertTrue(part.label.isEmpty(),"Remote UI edit is rejected");h.succeed();
    }

    @GameTest(template="empty")
    public static void addItemEscrowSurvivesDestinationException(GameTestHelper h){
        BlockPos fromPos=new BlockPos(1,1,1),toPos=new BlockPos(5,1,1);
        h.setBlock(fromPos,net.minecraft.world.level.block.Blocks.CHEST);
        h.setBlock(toPos,net.minecraft.world.level.block.Blocks.CHEST);
        var from=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(fromPos));
        from.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,17));
        boolean[] fail={true};BlockPos absolute=h.absolutePos(toPos);
        var to=new net.minecraft.world.level.block.entity.ChestBlockEntity(absolute,h.getLevel().getBlockState(absolute)){
            @Override public void setItem(int slot,net.minecraft.world.item.ItemStack stack){
                if(fail[0])throw new IllegalStateException("PL4 expected destination failure");super.setItem(slot,stack);
            }
        };
        h.getLevel().setBlockEntity(to);net.foundations.pl4.compat.PortCapabilities.invalidate(to);
        var source=host(h,new BlockPos(2,1,1),Kind.NODE,Direction.WEST);
        var sink=host(h,new BlockPos(4,1,1),Kind.TRANSFER_NODE,Direction.EAST);sink.part().transferMode=1;
        boolean thrown=false;try{TransferEngine.run(h.getLevel().getServer(),List.of(source,sink));}catch(IllegalStateException expected){
            if(!"PL4 expected destination failure".equals(expected.getMessage()))throw expected;thrown=true;
        }
        h.assertTrue(thrown&&from.getItem(0).isEmpty()&&to.getItem(0).isEmpty()&&sink.part().pendingItem.getCount()==17,"Extracted items remain owned by ADD escrow after a pre-mutation destination failure");
        Part restored=Part.load(sink.part().save(h.getLevel().registryAccess(),false),h.getLevel().registryAccess());
        h.assertTrue(restored!=null&&restored.pendingItem.getCount()==17,"Exception escrow survives persistence");
        fail[0]=false;TransferEngine.run(h.getLevel().getServer(),List.of(source,sink));
        h.assertTrue(to.getItem(0).getCount()==17&&sink.part().pendingItem.isEmpty()&&from.getItem(0).isEmpty(),"Retry delivers escrow exactly once");h.succeed();
    }
    @GameTest(template="empty")
    public static void previewFallbackRespectsTotalBudgetAndStableKeys(GameTestHelper h){
        var samples=new VisualSamples();var last=net.minecraft.world.item.ItemStack.EMPTY;
        for(int i=0;i<256;i++){
            var item=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE);
            net.foundations.pl4.compat.PortData.set(item,DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(i+":"+"x".repeat(1500)));
            samples.item(item);last=item;
        }
        var rows=samples.rows(h.getLevel().registryAccess(),true,256,0);int bytes=0;boolean omitted=false;
        for(var row:rows){
            if(row.previewItem().isEmpty())omitted=true;else{byte[] encoded=VisualSamples.bounded(row.previewItem(),4096);h.assertTrue(encoded!=null,"Each preview fits the individual bound");bytes+=encoded.length;}
            h.assertTrue(row.value()==1&&row.itemId().equals("minecraft:stone"),"Exhausted visual budget preserves numeric and server-side filter data");
        }
        var alone=new VisualSamples();alone.item(last);
        h.assertTrue(rows.size()==256&&bytes<=32768&&omitted,"All fallback previews share the same 32 KiB budget");
        h.assertTrue(rows.stream().map(Part.Row::key).distinct().count()==256&&net.foundations.pl4.compat.PortLists.last(rows).key().equals(alone.rows(h.getLevel().registryAccess(),true,1,0).get(0).key()),"Bounded component keys remain distinct and stable after preview exhaustion");h.succeed();
    }
    @GameTest(template="empty")
    public static void layoutSnapshotRejectsExcessElements(GameTestHelper h){
        var elements=new ArrayList<DisplayElements.Spec>();
        for(int i=0;i<32;i++)elements.add(DisplayElements.create(DisplayElements.Type.TEXT,0,100,100));
        h.assertTrue(ElementJson.decodeList(ElementJson.encodeList(elements)).size()==32,"Full supported snapshot is accepted");
        elements.add(DisplayElements.create(DisplayElements.Type.TEXT,0,100,100));boolean rejected=false;
        try{ElementJson.decodeList(ElementJson.encodeList(elements));}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Oversized element arrays are rejected before individual decoding");h.succeed();
    }
}
