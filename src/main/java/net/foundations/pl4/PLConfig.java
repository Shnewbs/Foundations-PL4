package net.foundations.pl4;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PLConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue TICK_RATE, MAX_NETWORK, MAX_ROWS, ITEM_RATE, FLUID_RATE, ENERGY_RATE, ENTITY_RANGE;
    public static final ModConfigSpec.BooleanValue TRANSFERS, WIRELESS, CROSS_DIMENSION, MEKANISM_READS, GREGTECH_READS;
    public static final ModConfigSpec.IntValue MAX_ENERGY_CONTAINERS;
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
