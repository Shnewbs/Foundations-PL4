package net.foundations.pl4;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PLConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue MEKANISM_TRANSFERS, GREGTECH_TRANSFERS, ELECTRODYNAMICS_TRANSFERS, ENERGY_CONVERSION;
    public static final ModConfigSpec.IntValue FE_PER_1000_J, FE_PER_EU, FE_PER_ED_J, CONVERSION_EFFICIENCY;
    public static final ModConfigSpec.IntValue TICK_RATE, MAX_NETWORK, MAX_ROWS, ITEM_RATE, FLUID_RATE, ENERGY_RATE, ENTITY_RANGE;
    public static final ModConfigSpec.BooleanValue TRANSFERS, WIRELESS, CROSS_DIMENSION, MEKANISM_READS, GREGTECH_READS;
    public static final ModConfigSpec.IntValue MAX_ENERGY_CONTAINERS, NETWORK_ITEM_RATE, NETWORK_FLUID_RATE, NETWORK_ENERGY_RATE;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("network");
        TICK_RATE = b.comment("Network sample/transfer interval in ticks. Never zero.").defineInRange("updateTicks", 20, 1, 1200);
        MAX_NETWORK = b.comment("Maximum loaded host blocks visited in one network. Larger networks stop safely.").defineInRange("maximumHosts", 4096, 16, 65536);
        MAX_ROWS = b.comment("Maximum synchronized display rows per reader.").defineInRange("maximumRows", 128, 8, 256);
        WIRELESS = b.define("wirelessEnabled", true);
        CROSS_DIMENSION = b.define("crossDimensionWireless", true);
        ENTITY_RANGE = b.defineInRange("entityScanRadius", 8, 1, 32);
        b.pop().push("transfer");
        TRANSFERS = b.define("enabled", true);
        ITEM_RATE = b.defineInRange("itemsPerCycle", 64, 1, 4096);
        FLUID_RATE = b.defineInRange("millibucketsPerCycle", 1000, 1, 64000);
        ENERGY_RATE = b.defineInRange("fePerCycle", 10000, 1, 10000000);
        b.push("networkCaps");
        NETWORK_ITEM_RATE=b.comment("Maximum individual items delivered per connected data network per transfer cycle, shared by all Transfer Nodes including escrow retries. 0 disables the shared cap; per-node limits still apply.").defineInRange("itemsPerCycle",0,0,10000000);
        NETWORK_FLUID_RATE=b.comment("Maximum mB delivered per connected data network per transfer cycle. 0 disables the shared cap.").defineInRange("millibucketsPerCycle",0,0,1000000000);
        NETWORK_ENERGY_RATE=b.comment("Maximum FE delivered per connected data network per transfer cycle. Native energy uses FE-equivalent accounting. 0 disables the shared cap.").defineInRange("fePerCycle",0,0,Integer.MAX_VALUE);
        b.pop().pop();
        b.push("energyTransfer");
        MEKANISM_TRANSFERS=b.comment("Enable optional native Mekanism Joule transfer adapter.").define("mekanismJoules",true);
        GREGTECH_TRANSFERS=b.comment("Enable optional GregTech CEu EU transfer adapter with sided voltage/amperage checks.").define("gregtechEU",true);
        ELECTRODYNAMICS_TRANSFERS=b.comment("Enable native Electrodynamics through the optional Voltaic electrodynamicblock capability.").define("electrodynamicsJoules",true);
        FE_PER_ED_J=b.comment("Integer FE value of one Electrodynamics Joule. Independent of Mekanism; default matches Voltaic FE wrapper 1:1.").defineInRange("fePerElectrodynamicsJ",1,1,1000000);
        ENERGY_CONVERSION=b.comment("Allow explicit cross-unit Transfer Node routes. Native same-unit transfers remain available when false.").define("conversionEnabled",true);
        FE_PER_1000_J=b.comment("Integer FE value of 1000 J. Default 400: 2.5 J = 1 FE. Also defines FE-equivalent native transfer caps.").defineInRange("fePer1000J",400,1,1000000);
        FE_PER_EU=b.comment("Integer FE value of one EU; pack policy, not a universal mod ratio.").defineInRange("fePerEU",4,1,1000000);
        CONVERSION_EFFICIENCY=b.comment("Output efficiency per cross-unit node boundary, in thousandths. 1000 is lossless. Native transfers are lossless.").defineInRange("conversionEfficiencyPermille",1000,1,1000);
        b.pop();
        b.push("energyReader");
        MEKANISM_READS=b.comment("Read exposed Mekanism strict energy handlers in native J. Does not transfer or convert energy.").define("mekanismJoules",true);
        GREGTECH_READS=b.comment("Read exposed GregTech CEu energy info/containers in native EU. No voltage conversion or transfer.").define("gregtechEU",true);
        MAX_ENERGY_CONTAINERS=b.comment("Reject providers above this count rather than silently reporting a partial total.").defineInRange("maximumContainersPerHandler",1024,1,4096);
        b.pop();
        SPEC = b.build();
    }
    private PLConfig() {}
}
