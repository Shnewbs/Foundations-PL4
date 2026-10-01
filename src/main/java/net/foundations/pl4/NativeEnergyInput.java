package net.foundations.pl4;

import java.lang.reflect.Proxy;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.foundations.pl4.core.EnergyConversion;
import net.foundations.pl4.core.TransferRules;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Receive GregTech's push-only sources into the same persistent, bounded conversion escrow. */
final class NativeEnergyInput {
    @SuppressWarnings("unchecked")
    static void register(RegisterCapabilitiesEvent event){
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,FoundationsPL4.HOST_ENTITY.get(),(host,side)->{
            if(side==null)return null;
            Part part=host.parts.get(side.ordinal());
            if(part==null||part.kind!=Kind.TRANSFER_NODE)return null;
            return new IEnergyStorage(){
                public int receiveEnergy(int amount,boolean simulate){return acceptFE(host,part,side,amount,simulate);}
                public int extractEnergy(int amount,boolean simulate){return 0;}
                public int getEnergyStored(){return EnergyConversion.charge(part.energyCredits());}
                public int getMaxEnergyStored(){return PLConfig.ENERGY_RATE.get();}
                public boolean canExtract(){return false;}
                public boolean canReceive(){return eligible(host,part,side,"FE");}
            };
        });
        for(var capability:BlockCapability.getAll())if(capability.name().toString().equals("gtceu:energy_container")&&capability.contextClass()==Direction.class){
            var cap=(BlockCapability<Object,Direction>)capability;
            event.registerBlockEntity(cap,FoundationsPL4.HOST_ENTITY.get(),(host,side)->{
                if(side==null)return null;
                Part part=host.parts.get(side.ordinal());
                if(part==null||part.kind!=Kind.TRANSFER_NODE)return null;
                return Proxy.newProxyInstance(cap.typeClass().getClassLoader(),new Class<?>[]{cap.typeClass()},(proxy,method,args)->switch(method.getName()){
                    case "inputsEnergy" -> eligible(host,part,side)&&args[0]==side;
                    case "outputsEnergy" -> false;
                    case "getInputVoltage" -> eligible(host,part,side)?(long)part.energyVoltage:0L;
                    case "getInputAmperage" -> eligible(host,part,side)?(long)PLConfig.ENERGY_RATE.get():0L;
                    case "getEnergyStored" -> 0L;
                    case "getEnergyCapacity","getEnergyCanBeInserted" -> eligible(host,part,side)?(long)PLConfig.ENERGY_RATE.get():0L;
                    case "acceptEnergyFromNetwork" -> accept(host,part,side,(Direction)args[0],(long)args[1],(long)args[2]);
                    case "changeEnergy","addEnergy","removeEnergy","getOutputVoltage","getOutputAmperage","getInputPerSec","getOutputPerSec" -> 0L;
                    case "supportsBigIntEnergyValues","isOneProbeHidden" -> false;
                    case "toString" -> "PL4 EU input";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy==args[0];
                    default -> method.isDefault()?java.lang.reflect.InvocationHandler.invokeDefault(proxy,method,args):null;
                });
            });
        }
    }
    private static boolean eligible(HostEntity host,Part p,Direction side){return eligible(host,p,side,"EU");}
    private static boolean eligible(HostEntity host,Part p,Direction side,String input){
        if(host.getLevel()==null||host.getLevel().isClientSide||host.isRemoved()||host.parts.get(side.ordinal())!=p)return false;
        if(!p.energy||!TransferRules.drivesRemove(p.transferMode)||!p.energyInput.equals(input)||!EnergyPorts.enabled(input)||!EnergyPorts.enabled(p.energyOutput))return false;
        if(!p.energyOutput.equals(input)&&(!p.energyConvert||!PLConfig.ENERGY_CONVERSION.get()))return false;
        var rates=EnergyPorts.rates();
        return p.energyCredits()==0||p.pendingEnergyUnit.equals(p.energyOutput)&&p.pendingEnergyJRate==rates.fePer1000J()&&p.pendingEnergyEURate==rates.fePerEU()&&p.pendingEnergyEDRate==rates.fePerElectrodynamicsJ();
    }
    static int acceptFE(HostEntity host,Part p,Direction side,int amount,boolean simulate){
        if(amount<=0||!eligible(host,p,side,"FE"))return 0;
        var rates=EnergyPorts.rates();
        long packet=EnergyConversion.credits(1,EnergyConversion.FE,rates.efficiency("FE",p.energyOutput));
        long capacity=(long)PLConfig.ENERGY_RATE.get()*EnergyConversion.FE;
        int accepted=(int)Math.min(amount,Math.min(PLConfig.ENERGY_RATE.get(),Math.max(0,capacity-p.energyCredits())/packet));
        if(accepted>0&&!simulate){
            p.energyCredits(p.energyCredits()+packet*accepted);p.pendingEnergyUnit=p.energyOutput;
            p.pendingEnergyJRate=rates.fePer1000J();p.pendingEnergyEURate=rates.fePerEU();p.pendingEnergyEDRate=rates.fePerElectrodynamicsJ();host.setChanged();
        }
        return accepted;
    }
    static long accept(HostEntity host,Part p,Direction attached,Direction side,long voltage,long amps){
        if(side!=attached||voltage<1||voltage>p.energyVoltage||amps<1||!eligible(host,p,attached))return 0;
        var rates=EnergyPorts.rates();long cost=rates.cost("EU");
        // Bound raw incoming EU before conversion loss, and never bypass the per-node buffer cap.
        long packet=EnergyConversion.credits(voltage,cost,rates.efficiency("EU",p.energyOutput));
        long capacity=(long)PLConfig.ENERGY_RATE.get()*EnergyConversion.FE;
        long allowed=Math.min(amps,Math.min(capacity/cost/voltage,packet==0?0:Math.max(0,capacity-p.energyCredits())/packet));
        if(allowed<=0)return 0;
        p.energyCredits(p.energyCredits()+packet*allowed);p.pendingEnergyUnit=p.energyOutput;
        p.pendingEnergyJRate=rates.fePer1000J();p.pendingEnergyEURate=rates.fePerEU();p.pendingEnergyEDRate=rates.fePerElectrodynamicsJ();host.setChanged();
        return allowed;
    }
    private NativeEnergyInput(){}
}
